package net.spacealtctrl.discordrp.log

import android.util.Log
import net.spacealtctrl.discordrp.BuildConfig

interface AppLog {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String)
}

object AndroidAppLog : AppLog {
    private const val PREFIX = "DiscordRP/"

    override fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(PREFIX + tag, message)
    }

    override fun info(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(PREFIX + tag, message)
    }

    override fun warn(tag: String, message: String) {
        Log.w(PREFIX + tag, message)
    }

    override fun error(tag: String, message: String) {
        Log.e(PREFIX + tag, message)
    }
}

object SilentLog : AppLog {
    override fun debug(tag: String, message: String) = Unit
    override fun info(tag: String, message: String) = Unit
    override fun warn(tag: String, message: String) = Unit
    override fun error(tag: String, message: String) = Unit
}
