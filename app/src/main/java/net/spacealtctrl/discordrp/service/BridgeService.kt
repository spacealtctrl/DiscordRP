package net.spacealtctrl.discordrp.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.net.ConnectivityManager
import android.net.Network
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.gateway.Gateway
import net.spacealtctrl.discordrp.gateway.GatewayEngine
import net.spacealtctrl.discordrp.gateway.GatewayEvent
import net.spacealtctrl.discordrp.inbox.InboxNotifier
import net.spacealtctrl.discordrp.log.AndroidAppLog
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.presence.ArtRegistry
import net.spacealtctrl.discordrp.presence.BridgeFeed
import net.spacealtctrl.discordrp.presence.NowPlaying
import net.spacealtctrl.discordrp.presence.PlaybackScanner
import net.spacealtctrl.discordrp.presence.PresenceComposer
import net.spacealtctrl.discordrp.settings.Stash
import javax.inject.Inject

@AndroidEntryPoint
class BridgeService : Service() {
    @Inject lateinit var stash: Stash
    @Inject lateinit var scanner: PlaybackScanner
    @Inject lateinit var composer: PresenceComposer
    @Inject lateinit var artRegistry: ArtRegistry
    @Inject lateinit var notifier: InboxNotifier
    @Inject lateinit var log: AppLog

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var gateway: Gateway? = null
    private var refreshJob: Job? = null
    private var redialJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var netCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile private var lastUpAt = 0L
    @Volatile private var lastNetwork: Network? = null

    private lateinit var sessionManager: MediaSessionManager
    private var watchedController: MediaController? = null
    private var sessionsListenerAttached = false

    override fun onCreate() {
        super.onCreate()
        ensureStatusChannel()
        goForeground(statusNotification(track = null))

        if (!stash.signedIn) {
            log.error(TAG, "No account on file; the bridge cannot start")
            stopSelf()
            return
        }

        _alive.value = true
        lastUpAt = System.currentTimeMillis()
        stash.purgeStaleArtCaches()
        holdWakeLock()
        openGateway()
        watchMediaSessions()
        watchNetwork()
        startCheckupTicker()
        scheduleWatchdogAlarm()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_REFRESH -> scheduleRefresh(delayMs = 0)
            ACTION_CHECKUP -> checkup()
            ACTION_RESTART -> scope.launch {
                log.info(TAG, "Restart action: redialing the gateway")
                gateway?.redial()
                refresh()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (::sessionManager.isInitialized && sessionsListenerAttached) {
            runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsWatcher) }
            sessionsListenerAttached = false
        }
        netCallback?.let { cb ->
            runCatching { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(cb) }
            netCallback = null
        }
        getSystemService(AlarmManager::class.java)?.cancel(watchdogIntent())
        watchedController?.unregisterCallback(controllerWatcher)
        _alive.value = false
        BridgeFeed.serviceStopped()
        notifier.reset()
        refreshJob?.cancel()
        redialJob?.cancel()
        scope.cancel()
        gateway?.shutdown()
        gateway = null
        dropWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun openGateway() {
        val engine = GatewayEngine(
            token = stash.authToken,
            mobileBadge = stash.mobileBadge,
            fallbackSelfId = stash.selfId.takeIf { it.isNotBlank() },
            log = AndroidAppLog,
        )
        gateway = engine

        scope.launch {
            engine.events.collect { event ->
                when (event) {
                    is GatewayEvent.MessageArrived -> scope.launch {
                        notifier.deliver(event.message)
                    }
                    is GatewayEvent.ChannelSeen -> notifier.conversationSeen(event.channelId)
                    is GatewayEvent.AuthRejected -> onAuthRejected()
                    is GatewayEvent.SessionReady -> Unit
                }
            }
        }
        scope.launch {
            engine.linked.collect { up ->
                if (up) lastUpAt = System.currentTimeMillis()
                BridgeFeed.linkChanged(up)
                if (up) scheduleRefresh(delayMs = 0)
            }
        }

        log.info(TAG, "Opening the gateway for presence and push")
        engine.start()
    }

    private fun onAuthRejected() {
        log.error(TAG, "Discord no longer accepts the stored login; stopping")
        notificationManager().notify(
            STATUS_NOTICE_ID,
            statusBuilder()
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.bridge_sign_in_again))
                .build(),
        )
        stopSelf()
    }

    private fun watchMediaSessions() {
        sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(this, MediaNoticeListener::class.java)
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsWatcher, component)
            sessionsListenerAttached = true
            onSessionsChanged(sessionManager.getActiveSessions(component), settled = true)
        } catch (e: SecurityException) {
            log.error(TAG, "Notification access missing; presence stays off")
        }
    }

    private fun watchNetwork() {
        val connectivity = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val changed = lastNetwork != null && lastNetwork != network
                lastNetwork = network
                val engine = gateway ?: return
                val downForMs = System.currentTimeMillis() - lastUpAt
                if (changed || (!engine.linked.value && downForMs > NETWORK_REDIAL_GRACE_MS)) {
                    log.info(TAG, "Network changed or returned; redialing the gateway")
                    redialJob?.cancel()
                    redialJob = scope.launch {
                        delay(NETWORK_SETTLE_MS)
                        engine.redial()
                    }
                }
            }

            override fun onLost(network: Network) {
                if (lastNetwork == network) lastNetwork = null
            }
        }
        try {
            connectivity.registerDefaultNetworkCallback(callback)
            netCallback = callback
        } catch (e: Exception) {
            log.warn(TAG, "Network watching unavailable: ${e::class.java.simpleName}")
        }
    }

    private fun startCheckupTicker() {
        scope.launch {
            while (true) {
                delay(CHECKUP_INTERVAL_MS)
                checkup()
            }
        }
    }

    private fun checkup() {
        holdWakeLock()
        scheduleWatchdogAlarm()
        val engine = gateway ?: return
        if (engine.linked.value) return
        val downForMs = System.currentTimeMillis() - lastUpAt
        if (downForMs > STUCK_GATEWAY_MS) {
            log.warn(TAG, "Gateway down for ${downForMs}ms; forcing a redial")
            scope.launch { engine.redial() }
        }
    }

    private fun watchdogIntent(): PendingIntent =
        PendingIntent.getForegroundService(
            this, 3,
            Intent(this, BridgeService::class.java).setAction(ACTION_CHECKUP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun scheduleWatchdogAlarm() {
        getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + WATCHDOG_INTERVAL_MS,
            watchdogIntent(),
        )
    }

    private val sessionsWatcher =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            onSessionsChanged(controllers, settled = false)
        }

    private fun onSessionsChanged(controllers: List<MediaController>?, settled: Boolean) {
        log.debug(TAG, "Active media sessions changed")
        rewatchController(controllers)
        scheduleRefresh(delayMs = if (settled) 0 else SESSIONS_SETTLE_MS)
    }

    private fun rewatchController(controllers: List<MediaController>?) {
        watchedController?.unregisterCallback(controllerWatcher)
        watchedController = controllers?.firstOrNull()
        watchedController?.registerCallback(controllerWatcher, mainHandler)
    }

    private val controllerWatcher = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) =
            scheduleRefresh(delayMs = PLAYBACK_SETTLE_MS)

        override fun onMetadataChanged(metadata: MediaMetadata?) =
            scheduleRefresh(delayMs = PLAYBACK_SETTLE_MS)

        override fun onSessionDestroyed() = scheduleRefresh(delayMs = 0)
    }

    private fun scheduleRefresh(delayMs: Long) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            refresh()
        }
    }

    private suspend fun refresh() {
        val track = scanner.currentTrack()
        notificationManager().notify(STATUS_NOTICE_ID, statusNotification(track))

        if (!stash.presenceEnabled || track == null) {
            if (track == null) log.debug(TAG, "Nothing playing; clearing the activity")
            BridgeFeed.trackChanged(null)
            gateway?.publish(composer.quietUpdate())
            return
        }

        BridgeFeed.trackChanged(track)
        gateway?.publish(composer.listeningUpdate(track))
    }

    private fun statusNotification(track: NowPlaying?): Notification {
        val builder = statusBuilder()
        if (track == null) {
            builder
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.bridge_idle))
        } else {
            builder
                .setContentTitle(track.title)
                .setContentText(track.artist ?: track.appLabel)
            val art = track.artwork ?: artRegistry.iconBitmap(track.appPackage)
            art?.let { builder.setLargeIcon(it) }
        }
        return builder.build()
    }

    private fun statusBuilder(): NotificationCompat.Builder {
        val restart = PendingIntent.getService(
            this, 1,
            Intent(this, BridgeService::class.java).setAction(ACTION_RESTART),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val exit = PendingIntent.getService(
            this, 2,
            Intent(this, BridgeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bridge_glyph)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(0, getString(R.string.bridge_action_restart), restart)
            .addAction(0, getString(R.string.bridge_action_exit), exit)
    }

    private fun goForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                STATUS_NOTICE_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(STATUS_NOTICE_ID, notification)
        }
    }

    private fun ensureStatusChannel() {
        val channel = NotificationChannel(
            STATUS_CHANNEL_ID,
            getString(R.string.bridge_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = getString(R.string.bridge_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    private fun holdWakeLock() {
        if (wakeLock == null) {
            val power = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
                .apply { setReferenceCounted(false) }
        }
        wakeLock?.acquire(WAKELOCK_LEASE_MS)
    }

    private fun dropWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    companion object {
        private const val TAG = "BridgeService"

        const val ACTION_STOP = "net.spacealtctrl.discordrp.bridge.STOP"
        const val ACTION_RESTART = "net.spacealtctrl.discordrp.bridge.RESTART"
        const val ACTION_REFRESH = "net.spacealtctrl.discordrp.bridge.REFRESH"
        const val ACTION_CHECKUP = "net.spacealtctrl.discordrp.bridge.CHECKUP"

        const val STATUS_CHANNEL_ID = "bridge.status.quiet"
        private const val STATUS_NOTICE_ID = 41_002

        private const val WAKELOCK_TAG = "discordrp:bridge"
        private const val WAKELOCK_LEASE_MS = 50 * 60 * 1000L
        private const val SESSIONS_SETTLE_MS = 1_500L
        private const val PLAYBACK_SETTLE_MS = 1_000L

        private const val CHECKUP_INTERVAL_MS = 15 * 60 * 1000L
        private const val WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L
        private const val STUCK_GATEWAY_MS = 2 * 60 * 1000L
        private const val NETWORK_SETTLE_MS = 3_000L
        private const val NETWORK_REDIAL_GRACE_MS = 10_000L

        private val _alive = MutableStateFlow(false)

        val alive: StateFlow<Boolean> = _alive.asStateFlow()

        fun refreshIntent(context: Context): Intent =
            Intent(context, BridgeService::class.java).setAction(ACTION_REFRESH)
    }
}
