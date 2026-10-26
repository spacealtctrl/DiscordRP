package net.spacealtctrl.discordrp.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import net.spacealtctrl.discordrp.log.AndroidAppLog
import net.spacealtctrl.discordrp.settings.Stash

class StartOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in WAKE_ACTIONS) return

        val stash = Stash.of(context)
        if (!stash.startOnBoot || !stash.signedIn) return

        try {
            context.startForegroundService(Intent(context, BridgeService::class.java))
            AndroidAppLog.info(TAG, "Bridge relaunched after ${intent.action}")
        } catch (e: Exception) {
            val vetoed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            AndroidAppLog.warn(
                TAG,
                if (vetoed) "System vetoed the relaunch"
                else "Relaunch failed: ${e::class.java.simpleName}",
            )
        }
    }

    private companion object {
        const val TAG = "BootWake"

        val WAKE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
