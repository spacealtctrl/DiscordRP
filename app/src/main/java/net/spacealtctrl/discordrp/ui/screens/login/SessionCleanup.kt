package net.spacealtctrl.discordrp.ui.screens.login

import java.util.concurrent.atomic.AtomicBoolean

/**
 * The operations a login WebView needs for teardown, split out so the ordering
 * can be exercised without an Android runtime.
 */
internal interface SessionOps {
    fun stopPage()

    fun loadBlank()

    /** Detaches the view; [destroyPage] refuses to run while it is attached. */
    fun detach()

    fun clearStorage()

    /** Must call [onDone] only after the cookie store is actually gone. */
    fun removeCookies(onDone: () -> Unit)

    fun flushCookies()

    fun clearCache()

    fun destroyPage()
}

/**
 * Tears a login WebView down in an order that cannot leak the document back:
 * stop loading, replace the document with about:blank, wait until that
 * navigation has finished (the old document is gone once it commits), wipe
 * storage, and only after [SessionOps.removeCookies] reports completion flush
 * the cookie store and consider the session cleaned.
 *
 * If the blank navigation never commits, the timeout path fails closed instead
 * of pretending the commit happened: the view is detached and destroyed (which
 * stops the still-live document) and only then are storage and cookies wiped.
 * That path never delivers a capture callback.
 *
 * Both the successful capture path and the cancel path go through the same
 * instance, so the work happens once. Destruction is deferred until cleanup is
 * complete even when the composable is released first.
 */
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

    /** True once capture is cancelled or the fail-closed timeout has run. */
    val isCancelled: Boolean get() = cancelled

    fun start(onCleaned: (() -> Unit)? = null) {
        // A capture callback arriving after cancellation must never be accepted;
        // the token may have been read from a document that was never torn down.
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

    /** Called for every page finish; only the blank document starts cleanup. */
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

    /**
     * The blank document never committed, so the previous document may still be
     * executing and could write the token back after a wipe. Destroying the view
     * stops it; only then is storage safe to clear.
     */
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
