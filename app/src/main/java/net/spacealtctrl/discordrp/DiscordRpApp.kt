package net.spacealtctrl.discordrp

import android.app.Application
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp
import net.spacealtctrl.discordrp.settings.Stash
import net.spacealtctrl.discordrp.ui.crash.CrashGuard

@HiltAndroidApp
class DiscordRpApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashGuard.install(this)
        Stash.of(this).purgeStaleArtCaches()
        dropRetiredNotificationChannels()
    }

    private fun dropRetiredNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        listOf("discordrp.notification", "discordrp.messages").forEach {
            runCatching { manager.deleteNotificationChannel(it) }
        }
    }
}
