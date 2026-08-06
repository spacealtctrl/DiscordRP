package net.spacealtctrl.discordrp.inbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InboxDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return
        ConversationLog.forget(channelId)
    }

    companion object {
        const val ACTION = "net.spacealtctrl.discordrp.action.CONVERSATION_DISMISSED"
        const val EXTRA_CHANNEL_ID = "channel_id"
    }
}
