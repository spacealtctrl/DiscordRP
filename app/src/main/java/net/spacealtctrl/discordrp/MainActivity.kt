package net.spacealtctrl.discordrp

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import net.spacealtctrl.discordrp.ui.nav.AppNavHost
import net.spacealtctrl.discordrp.ui.theme.DiscordRpTheme
import net.spacealtctrl.discordrp.update.UpdateHub
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var updateHub: UpdateHub

    private val notificationAccess = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        notificationAccess.value = hasNotificationAccess()
        updateHub.autoCheck()

        setContent {
            DiscordRpTheme {
                AppNavHost(notificationAccess = notificationAccess)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationAccess.value = hasNotificationAccess()
    }

    private fun Context.hasNotificationAccess(): Boolean {
        val granted = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return granted?.contains(packageName) == true
    }
}
