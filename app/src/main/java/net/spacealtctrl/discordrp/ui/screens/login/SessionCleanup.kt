package net.spacealtctrl.discordrp.ui.screens.login

import java.util.concurrent.atomic.AtomicBoolean

internal interface SessionOps {
    fun stopPage()

    fun loadBlank()

    fun detach()

    fun clearStorage()

    fun removeCookies(onDone: () -> Unit)

    fun flushCookies()

    fun clearCache()

    fun destroyPage()
}

internal class SessionCleanup(
    private val ops: SessionOps,
    private val scheduleTimeout: (action: () -> Unit) -> Unit,
) {
    private val started = AtomicBoolean(false)
    private var clearing = false
    private var complete = false
    private var destroyWhenComplete = false
    private var destroyed = false
    private var cancelled = false
    private var onCleaned: (() -> Unit)? = null

    val isCancelled: Boolean get() = cancelled

    fun start(onCleaned: (() -> Unit)? = null) {
        if (cancelled) return
        if (onCleaned != null) this.onCleaned = onCleaned
        if (complete) {
            deliver()
            return
        }
        begin()
    }

    fun cancel() {
        cancelled = true
        onCleaned = null
        release()
    }

    fun release() {
        destroyWhenComplete = true
        if (complete) {
            destroy()
            return
        }
        begin()
    }

    fun onPageFinished(url: String) {
        if (url == BLANK_URL) clear()
    }

    private fun begin() {
        if (!started.compareAndSet(false, true)) return
        ops.stopPage()
        ops.loadBlank()
        scheduleTimeout { failClosed() }
    }

    private fun clear() {
        if (clearing || complete) return
        clearing = true
        ops.clearStorage()
        ops.removeCookies {
            if (complete) return@removeCookies
            ops.flushCookies()
            complete = true
            ops.clearCache()
            deliver()
            if (destroyWhenComplete) destroy()
        }
    }

    private fun failClosed() {
        if (complete || clearing) return
        clearing = true
        cancelled = true
        onCleaned = null
        ops.detach()
        destroy()
        ops.clearStorage()
        ops.removeCookies {
            if (complete) return@removeCookies
            ops.flushCookies()
            complete = true
        }
    }

    private fun deliver() {
        if (cancelled) return
        val callback = onCleaned
        onCleaned = null
        callback?.invoke()
    }

    private fun destroy() {
        if (destroyed) return
        destroyed = true
        ops.destroyPage()
    }

    companion object {
        const val BLANK_URL = "about:blank"
    }
}
