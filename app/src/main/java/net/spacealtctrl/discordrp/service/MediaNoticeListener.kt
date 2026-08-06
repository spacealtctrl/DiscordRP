package net.spacealtctrl.discordrp.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import net.spacealtctrl.discordrp.inbox.ConversationLog
import net.spacealtctrl.discordrp.inbox.InboxNotifier

class MediaNoticeListener : NotificationListenerService() {
    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        super.onNotificationRemoved(sbn, rankingMap, reason)
        val removed = sbn ?: return
        if (removed.packageName != packageName) return

        removed.notification?.extras
            ?.getString(InboxNotifier.EXTRA_CHANNEL_ID)
            ?.let { ConversationLog.forget(it) }
    }
}
