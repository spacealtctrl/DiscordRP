package net.spacealtctrl.discordrp.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import net.spacealtctrl.discordrp.settings.Stash

class StartOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in WAKE_ACTIONS) return

        val stash = Stash.of(context)
        if (!stash.startOnBoot || !stash.signedIn) return

        try {
            context.startForegroundService(Intent(context, BridgeService::class.java))
            Log.i(TAG, "Bridge relaunched after ${intent.action}")
        } catch (e: Exception) {
            val vetoed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            Log.w(TAG, if (vetoed) "System vetoed the relaunch" else "Relaunch failed", e)
        }
    }

    private companion object {
        const val TAG = "DiscordRP/BootWake"

        val WAKE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
