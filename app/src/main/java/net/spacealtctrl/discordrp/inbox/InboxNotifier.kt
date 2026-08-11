package net.spacealtctrl.discordrp.inbox

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.gateway.ChatMessage
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.settings.AlertScope
import net.spacealtctrl.discordrp.settings.Stash
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
    private val directory: ChannelDirectory,
    private val stash: Stash,
    private val log: AppLog,
) {
    private val faces = FaceFetcher(context)

    init {
        ensureChannel()
    }

    suspend fun deliver(message: ChatMessage) {
        if (!stash.alertsEnabled) return
        val channelId = message.channelId ?: return
        runCatching {
            if (wanted(message)) post(channelId, message)
        }.onFailure {
            log.error(TAG, "Notification failed: ${it::class.java.simpleName}")
        }
    }

    fun conversationSeen(channelId: String) {
        ConversationLog.forget(channelId)
        if (stash.clearReadElsewhere) {
            notificationManager.cancel(noticeId(channelId))
        }
    }

    fun conversationDismissed(channelId: String) {
        ConversationLog.forget(channelId)
    }

    fun reset() {
        ConversationLog.forgetAll()
    }

    private suspend fun wanted(message: ChatMessage): Boolean {
        if (stash.skipBots && message.sender?.bot == true) return false
        return when (stash.alertScope) {
            AlertScope.EVERYTHING -> true
            AlertScope.DMS_ONLY -> message.isDm
            AlertScope.MENTIONS_AND_DMS -> message.isDm || concernsMe(message)
        }
    }

    private suspend fun concernsMe(message: ChatMessage): Boolean {
        val selfId = stash.selfId.takeIf { it.isNotBlank() } ?: return false
        val roles = if (message.roleMentions.isEmpty() || message.guildId == null) {
            emptySet()
        } else {
            runCatching { directory.myRoles(message.guildId) }.getOrDefault(emptySet())
        }
        return message.pings(selfId, roles)
    }

    private suspend fun post(channelId: String, message: ChatMessage) {
        val title = runCatching {
            directory.conversationTitle(channelId, message.guildId)
        }.getOrNull()

        val thread = ConversationLog.record(
            channelId,
            ConversationLog.Line(
                senderName = message.sender?.displayName() ?: "DiscordRP",
                senderId = message.sender?.id,
                portraitUrl = message.sender?.portraitUrl(),
                body = message.digest(),
                at = System.currentTimeMillis(),
            ),
        )

        val portraits: Map<String, IconCompat?> = thread
            .mapNotNull { it.portraitUrl }
            .distinct()
            .associateWith { faces.face(it) }

        val style = NotificationCompat.MessagingStyle(ME)
        thread.forEach { line ->
            val speaker = if (line.fromMe) null else personOf(line, portraits[line.portraitUrl])
            style.addMessage(line.body, line.at, speaker)
        }
        if (!message.isDm) {
            style.isGroupConversation = true
            style.conversationTitle = title ?: message.sender?.displayName()
        }

        val newest = thread.last()
        val speaker = personOf(newest, portraits[newest.portraitUrl])
        publishShortcut(channelId, message, speaker, title)

        val replyTo = if (message.isDm) speaker.name else title ?: speaker.name

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_discord_logo)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(channelId)
            .setLocusId(LocusIdCompat(channelId))
            .addPerson(speaker)
            .setWhen(newest.at)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(openDiscordIntent(message))
            .setDeleteIntent(dismissIntent(channelId))
            .addExtras(Bundle().apply { putString(EXTRA_CHANNEL_ID, channelId) })
            .addAction(replyAction(channelId, message.id, replyTo?.toString()))
            .apply { message.id?.let { addAction(markReadAction(channelId, it)) } }
            .build()

        log.debug(TAG, "Posting notification for a conversation")
        notificationManager.notify(noticeId(channelId), notification)
    }

    fun replySent(channelId: String, text: String) {
        ConversationLog.record(
            channelId,
            ConversationLog.Line(
                senderName = "You",
                senderId = stash.selfId.takeIf { it.isNotBlank() },
                portraitUrl = null,
                body = text,
                at = System.currentTimeMillis(),
                fromMe = true,
            ),
        )
        val active = activeNotification(channelId) ?: return
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(active) ?: return
        style.addMessage(text, System.currentTimeMillis(), null as Person?)
        notificationManager.notify(
            noticeId(channelId),
            NotificationCompat.Builder(context, active).setStyle(style).build(),
        )
    }

    fun replyFailed(channelId: String) {
        val active = activeNotification(channelId) ?: return
        notificationManager.notify(
            noticeId(channelId),
            NotificationCompat.Builder(context, active)
                .setSubText(context.getString(R.string.inbox_reply_failed))
                .build(),
        )
    }

    fun conversationRead(channelId: String) {
        ConversationLog.forget(channelId)
        notificationManager.cancel(noticeId(channelId))
    }

    private fun activeNotification(channelId: String) = runCatching {
        notificationManager.activeNotifications
            .firstOrNull { it.id == noticeId(channelId) }
            ?.notification
    }.getOrNull()

    private fun replyAction(
        channelId: String,
        messageId: String?,
        replyTo: String?,
    ): NotificationCompat.Action {
        val label = context.getString(R.string.inbox_reply)
        val remoteInput = RemoteInput.Builder(InboxReplyReceiver.KEY_REPLY)
            .setLabel(replyTo?.let { context.getString(R.string.inbox_reply_to, it) } ?: label)
            .build()
        val intent = Intent(context, InboxReplyReceiver::class.java)
            .setAction(InboxReplyReceiver.ACTION_REPLY)
            .putExtra(InboxReplyReceiver.EXTRA_CHANNEL_ID, channelId)
            .putExtra(InboxReplyReceiver.EXTRA_MESSAGE_ID, messageId)
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pending = PendingIntent.getBroadcast(
            context,
            channelId.hashCode() * 31 + 1,
            intent,
            mutable or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Action.Builder(R.drawable.ic_discord_logo, label, pending)
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setAllowGeneratedReplies(true)
            .setShowsUserInterface(false)
            .build()
    }

    private fun markReadAction(channelId: String, messageId: String): NotificationCompat.Action {
        val intent = Intent(context, InboxReplyReceiver::class.java)
            .setAction(InboxReplyReceiver.ACTION_MARK_READ)
            .putExtra(InboxReplyReceiver.EXTRA_CHANNEL_ID, channelId)
            .putExtra(InboxReplyReceiver.EXTRA_MESSAGE_ID, messageId)
        val pending = PendingIntent.getBroadcast(
            context,
            channelId.hashCode() * 31 + 2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_discord_logo,
            context.getString(R.string.inbox_mark_read),
            pending,
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }

    private fun personOf(line: ConversationLog.Line, portrait: IconCompat?): Person =
        Person.Builder()
            .setName(line.senderName)
            .setKey(line.senderId ?: line.senderName)
            .setIcon(portrait)
            .setBot(false)
            .build()

    private fun publishShortcut(
        channelId: String,
        message: ChatMessage,
        speaker: Person,
        title: String?,
    ) {
        runCatching {
            val label = if (message.isDm) {
                speaker.name?.toString().orEmpty()
            } else {
                title ?: speaker.name?.toString().orEmpty()
            }
            val shortcut = ShortcutInfoCompat.Builder(context, channelId)
                .setShortLabel(label.ifBlank { "DiscordRP" })
                .setLongLived(true)
                .setIntent(discordIntent(message).setAction(Intent.ACTION_VIEW))
                .setPerson(speaker)
                .setLocusId(LocusIdCompat(channelId))
                .apply { speaker.icon?.let { setIcon(it) } }
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        }.onFailure {
            log.warn(TAG, "Shortcut publish failed: ${it::class.java.simpleName}")
        }
    }

    private fun discordIntent(message: ChatMessage): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(message.deepLink()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .apply { installedDiscordPackage()?.let { setPackage(it) } }

    private fun openDiscordIntent(message: ChatMessage): PendingIntent =
        PendingIntent.getActivity(
            context,
            message.channelId.hashCode(),
            discordIntent(message),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun dismissIntent(channelId: String): PendingIntent {
        val intent = Intent(context, InboxDismissReceiver::class.java)
            .setAction(InboxDismissReceiver.ACTION)
            .putExtra(InboxDismissReceiver.EXTRA_CHANNEL_ID, channelId)
        return PendingIntent.getBroadcast(
            context,
            channelId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun installedDiscordPackage(): String? = DISCORD_CLIENTS.firstOrNull { pkg ->
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Direct messages and mentions from Discord"
            setShowBadge(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "InboxNotifier"
        const val CHANNEL_ID = "bridge.inbox"
        const val EXTRA_CHANNEL_ID = "conversation_channel_id"

        private val ME: Person = Person.Builder().setName("You").build()

        private val DISCORD_CLIENTS = listOf(
            "com.discord",
            "com.discord.canary",
            "com.discord.ptb",
            "com.aliucord",
        )

        fun noticeId(channelId: String): Int = channelId.hashCode()
    }
}
