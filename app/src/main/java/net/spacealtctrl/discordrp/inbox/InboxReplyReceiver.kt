package net.spacealtctrl.discordrp.inbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.spacealtctrl.discordrp.discord.DiscordApi
import net.spacealtctrl.discordrp.log.AppLog

class InboxReplyReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun api(): DiscordApi
        fun notifier(): InboxNotifier
        fun log(): AppLog
    }

    override fun onReceive(context: Context, intent: Intent) {
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        val deps = EntryPointAccessors.fromApplication(
            context.applicationContext,
            Deps::class.java,
        )

        when (intent.action) {
            ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_REPLY)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                if (text.isEmpty()) {
                    deps.notifier().replyFailed(channelId)
                    return
                }
                finishLater {
                    val sent = deps.api().sendMessage(channelId, text)
                    if (sent.isSuccess) {
                        deps.notifier().replySent(channelId, text)
                        messageId?.let { deps.api().markRead(channelId, it) }
                    } else {
                        deps.log().warn(TAG, "Reply refused: ${sent.exceptionOrNull()?.message}")
                        deps.notifier().replyFailed(channelId)
                    }
                }
            }

            ACTION_MARK_READ -> {
                if (messageId == null) return
                deps.notifier().conversationRead(channelId)
                finishLater { deps.api().markRead(channelId, messageId) }
            }
        }
    }

    private fun finishLater(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "InboxReplyReceiver"

        const val ACTION_REPLY = "net.spacealtctrl.discordrp.action.REPLY"
        const val ACTION_MARK_READ = "net.spacealtctrl.discordrp.action.MARK_READ"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val KEY_REPLY = "reply_text"
    }
}
