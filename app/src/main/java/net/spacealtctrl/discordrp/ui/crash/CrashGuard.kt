package net.spacealtctrl.discordrp.ui.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashGuard {
    fun install(context: Context) {
        val appContext = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler { _, error ->
            runCatching {
                val intent = Intent(appContext, CrashActivity::class.java)
                    .putExtra(CrashActivity.EXTRA_REPORT, describe(appContext, error))
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                appContext.startActivity(intent)
            }
            exitProcess(1)
        }
    }

    private fun describe(context: Context, error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
        return """
            |DiscordRP crash report
            |Device: ${Build.MANUFACTURER} ${Build.MODEL}
            |Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            |App version: $version
            |
            |$trace
        """.trimMargin().take(REPORT_CAP)
    }

    private const val REPORT_CAP = 120_000
}
