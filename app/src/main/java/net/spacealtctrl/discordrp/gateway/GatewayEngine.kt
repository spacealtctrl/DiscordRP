package net.spacealtctrl.discordrp.gateway

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.log.SilentLog
import kotlin.random.Random

class GatewayEngine(
    private val token: String,
    private val mobileBadge: Boolean,
    private val autoRedial: Boolean = true,
    fallbackSelfId: String? = null,
    private val log: AppLog = SilentLog,
) : Gateway {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = HttpClient(CIO) {
        install(WebSockets)
        install(io.ktor.client.plugins.HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
    }

    private val _events = MutableSharedFlow<GatewayEvent>(
        replay = 8,
        extraBufferCapacity = 56,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<GatewayEvent> = _events.asSharedFlow()

    private val _linked = MutableStateFlow(false)
    override val linked: StateFlow<Boolean> = _linked.asStateFlow()

    @Volatile private var socket: DefaultClientWebSocketSession? = null
    @Volatile private var loop: Job? = null
    @Volatile private var pacemaker: Job? = null
    @Volatile private var retired = false

    @Volatile private var seq = 0
    @Volatile private var sessionId: String? = null
    @Volatile private var resumeUrl: String? = null
    @Volatile private var selfId: String? = fallbackSelfId

    @Volatile private var beatAcked = true
    @Volatile private var sessionUp = false
    @Volatile private var dialedAt = 0L

    override fun start() {
        if (retired) {
            log.warn(TAG, "start() after shutdown, ignoring")
            return
        }
        if (loop?.isActive == true) return
        loop = scope.launch { redialLoop() }
    }

    override suspend fun redial() {
        if (retired) return
        log.info(TAG, "Redial requested")
        forgetSession()
        val open = socket
        if (open == null) {
            start()
        } else {
            runCatching { open.close(CloseReason(APP_CLOSE_CODE, "redial")) }
        }
    }

    override suspend fun publish(update: PresenceUpdate) {
        val ready = withTimeoutOrNull(PUBLISH_WAIT_MS) { linked.first { it } }
        if (ready == null) {
            log.warn(TAG, "Session never came up; presence update dropped")
            return
        }
        transmit(Op.PRESENCE, GatewayCodec.encode(update))
    }

    override fun shutdown() {
        retired = true
        _linked.value = false
        pacemaker?.cancel()
        val open = socket
        socket = null
        loop = null
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { open?.close(CloseReason(CloseReason.Codes.NORMAL, "bye")) }
            runCatching { http.close() }
            log.info(TAG, "Gateway retired")
        }
        scope.cancel()
    }

    private suspend fun redialLoop() {
        var attempt = 0
        while (scope.isActive && !retired) {
            val drop = dialOnce()
            if (retired || !scope.isActive) return

            log.warn(TAG, "Dropped: code=${drop.code} reason=${drop.reason}")

            if (drop.code in UNRECOVERABLE_CODES) {
                log.error(TAG, "Close code ${drop.code} is terminal; reporting auth failure")
                _events.tryEmit(GatewayEvent.AuthRejected)
                return
            }
            if (!autoRedial) {
                log.info(TAG, "Single-shot session; not redialing")
                return
            }
            if (drop.code in FRESH_SESSION_CODES) {
                log.info(TAG, "Session not resumable; starting clean")
                forgetSession()
            }

            val heldUp = sessionUp && System.currentTimeMillis() - dialedAt > STEADY_SESSION_MS
            attempt = if (heldUp) 1 else attempt + 1
            if (attempt >= RESUME_PATIENCE && resumeUrl != null) {
                log.warn(TAG, "Resume endpoint keeps failing; abandoning it")
                forgetSession()
            }

            val napMs = backoff(attempt)
            log.info(TAG, "Redialing in ${napMs}ms (attempt $attempt)")
            delay(napMs)
        }
    }

    private data class Drop(val code: Int?, val reason: String?)

    private suspend fun dialOnce(): Drop {
        sessionUp = false
        dialedAt = System.currentTimeMillis()
        val url = resumeUrl ?: GATEWAY_URL

        var session: DefaultClientWebSocketSession? = null
        try {
            log.info(TAG, "Dialing $url")
            session = withTimeoutOrNull(DIAL_TIMEOUT_MS) { http.webSocketSession(url) }
                ?: run {
                    log.error(TAG, "Dial timed out")
                    return Drop(null, "dial timeout")
                }
            socket = session
            beatAcked = true
            for (frame in session.incoming) {
                if (frame is Frame.Text) receive(frame.readText())
            }
        } catch (e: CancellationException) {
            runCatching { session?.cancel() }
            throw e
        } catch (e: Exception) {
            log.error(TAG, "Socket error: ${e::class.java.simpleName}")
        }

        val closeReason = withTimeoutOrNull(CLOSE_REASON_WAIT_MS) {
            try {
                session?.closeReason?.await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

        pacemaker?.cancel()
        pacemaker = null
        _linked.value = false
        socket = null
        runCatching { session?.cancel() }

        return Drop(closeReason?.code?.toInt(), closeReason?.message)
    }

    private suspend fun receive(text: String) {
        val frame = GatewayCodec.parse(text) ?: run {
            log.error(TAG, "Unparseable frame dropped")
            return
        }
        frame.seq?.let { seq = it }
        log.debug(TAG, "<- op=${frame.op} t=${frame.event} s=${frame.seq}")

        when (frame.op) {
            Op.HELLO -> greet(frame)
            Op.HEARTBEAT -> sendBeat()
            Op.HEARTBEAT_ACK -> beatAcked = true
            Op.RECONNECT -> socket?.close(CloseReason(APP_CLOSE_CODE, "server asked to reconnect"))
            Op.INVALID_SESSION -> {
                log.info(TAG, "Session invalidated; identifying fresh")
                forgetSession()
                delay(REIDENTIFY_NAP_MS)
                announceSelf()
            }
            Op.EVENT -> dispatch(frame)
        }
    }

    private suspend fun greet(frame: GatewayCodec.Inbound) {
        if (seq > 0 && !sessionId.isNullOrBlank()) resumeSession() else announceSelf()
        val interval = GatewayCodec.read<HelloData>(frame)?.heartbeatInterval ?: return
        log.info(TAG, "Heartbeat every ${interval}ms")
        startPacemaker(interval)
    }

    private fun dispatch(frame: GatewayCodec.Inbound) {
        when (frame.event) {
            "READY" -> {
                val ready = GatewayCodec.read<ReadyData>(frame) ?: return
                sessionId = ready.sessionId
                resumeUrl = ready.resumeUrl?.let { "$it/?v=10&encoding=json" }
                ready.user?.id?.let { selfId = it }
                sessionUp = true
                _linked.value = true
                log.info(TAG, "Session ready (resume endpoint stored)")
                _events.tryEmit(GatewayEvent.SessionReady(ready.user))
            }
            "RESUMED" -> {
                sessionUp = true
                _linked.value = true
                log.info(TAG, "Session resumed")
            }
            "MESSAGE_CREATE" -> {
                val message = GatewayCodec.read<ChatMessage>(frame) ?: return
                if (message.sender?.id == null || message.sentBy(selfId)) return
                _events.tryEmit(GatewayEvent.MessageArrived(message))
            }
            "MESSAGE_ACK" -> {
                val channelId = GatewayCodec.read<ReadReceipt>(frame)?.channelId ?: return
                _events.tryEmit(GatewayEvent.ChannelSeen(channelId))
            }
        }
    }

    private suspend fun announceSelf() {
        log.info(TAG, "-> identify")
        transmit(
            Op.IDENTIFY,
            GatewayCodec.encode(
                IdentifyRequest(token = token, properties = ClientStamp.forMobileBadge(mobileBadge))
            ),
        )
    }

    private suspend fun resumeSession() {
        log.info(TAG, "-> resume seq=$seq")
        transmit(
            Op.RESUME,
            GatewayCodec.encode(
                ResumeRequest(token = token, sessionId = sessionId.orEmpty(), seq = seq)
            ),
        )
    }

    private suspend fun sendBeat() {
        log.debug(TAG, "-> heartbeat seq=$seq")
        transmit(Op.HEARTBEAT, GatewayCodec.sequenceValue(seq))
    }

    private suspend fun transmit(op: Int, data: kotlinx.serialization.json.JsonElement) {
        val open = socket ?: return
        if (!open.isActive) return
        try {
            open.send(Frame.Text(GatewayCodec.outbound(op, data)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(TAG, "Send failed for op=$op: ${e::class.java.simpleName}")
        }
    }

    private fun startPacemaker(intervalMs: Long) {
        pacemaker?.cancel()
        pacemaker = scope.launch {
            delay((intervalMs * Random.nextDouble()).toLong())
            while (isActive) {
                if (!beatAcked) {
                    log.warn(TAG, "Heartbeat went unanswered; dropping the socket")
                    runCatching {
                        socket?.close(CloseReason(APP_CLOSE_CODE, "heartbeat unanswered"))
                    }
                    return@launch
                }
                beatAcked = false
                sendBeat()
                delay(intervalMs)
            }
        }
    }

    private fun forgetSession() {
        sessionId = null
        resumeUrl = null
        seq = 0
    }

    private fun backoff(attempt: Int): Long {
        val exponential = FLOOR_BACKOFF_MS shl (attempt - 1).coerceIn(0, 6)
        return exponential.coerceAtMost(CEILING_BACKOFF_MS) + Random.nextLong(0, JITTER_MS)
    }

    private companion object {
        const val TAG = "Gateway"
        const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"

        const val APP_CLOSE_CODE: Short = 4000

        const val FLOOR_BACKOFF_MS = 1_000L
        const val CEILING_BACKOFF_MS = 60_000L
        const val JITTER_MS = 500L
        const val REIDENTIFY_NAP_MS = 2_000L
        const val PUBLISH_WAIT_MS = 30_000L
        const val CONNECT_TIMEOUT_MS = 30_000L
        const val DIAL_TIMEOUT_MS = 45_000L
        const val CLOSE_REASON_WAIT_MS = 2_000L
        const val STEADY_SESSION_MS = 60_000L
        const val RESUME_PATIENCE = 3

        val UNRECOVERABLE_CODES = setOf(4004, 4010, 4011, 4012, 4013, 4014)

        val FRESH_SESSION_CODES = setOf(4003, 4007, 4009)
    }
}
