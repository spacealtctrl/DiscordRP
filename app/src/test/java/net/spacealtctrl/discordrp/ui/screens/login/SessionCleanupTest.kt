package net.spacealtctrl.discordrp.ui.screens.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCleanupTest {
    private val ops = FakeOps()
    private var timeout: (() -> Unit)? = null
    private val cleanup = SessionCleanup(ops) { timeout = it }

    @Test
    fun waitsForTheBlankDocumentBeforeWiping() {
        var cleaned = 0
        cleanup.start { cleaned++ }

        assertEquals(listOf("stop", "blank"), ops.calls)
        assertNull(ops.cookieCallback)

        cleanup.onPageFinished("https://discord.com/app")
        assertEquals(listOf("stop", "blank"), ops.calls)

        cleanup.onPageFinished(SessionCleanup.BLANK_URL)
        assertEquals(listOf("stop", "blank", "storage", "cookies"), ops.calls)
        assertEquals(0, cleaned)

        ops.cookieCallback!!.invoke()
        assertEquals(listOf("stop", "blank", "storage", "cookies", "flush", "cache"), ops.calls)
        assertEquals(1, cleaned)
    }

    @Test
    fun releaseDefersDestructionUntilCookiesAreGone() {
        cleanup.start()
        cleanup.onPageFinished(SessionCleanup.BLANK_URL)
        cleanup.release()

        assertEquals(false, ops.calls.contains("destroy"))

        ops.cookieCallback!!.invoke()
        assertEquals(listOf("stop", "blank", "storage", "cookies", "flush", "cache", "destroy"), ops.calls)

        cleanup.release()
        assertEquals(1, ops.calls.count { it == "destroy" })
    }

    @Test
    fun bothPathsShareOneCleanup() {
        var cleaned = 0
        cleanup.start { cleaned++ }
        cleanup.release()
        cleanup.start()
        cleanup.onPageFinished(SessionCleanup.BLANK_URL)
        cleanup.onPageFinished(SessionCleanup.BLANK_URL)

        assertEquals(1, ops.calls.count { it == "stop" })
        assertEquals(1, ops.calls.count { it == "storage" })
        assertEquals(1, ops.calls.count { it == "cookies" })

        ops.cookieCallback!!.invoke()
        assertEquals(1, cleaned)
        assertEquals(1, ops.calls.count { it == "destroy" })
    }

    @Test
    fun timeoutFailsClosedInsteadOfWipingALivePage() {
        var cleaned = 0
        cleanup.start { cleaned++ }
        timeout!!.invoke()

        assertEquals(listOf("stop", "blank", "detach", "destroy", "storage", "cookies"), ops.calls)
        assertTrue(cleanup.isCancelled)

        ops.cookieCallback!!.invoke()
        assertEquals(
            listOf("stop", "blank", "detach", "destroy", "storage", "cookies", "flush"),
            ops.calls,
        )
        assertEquals(0, cleaned)

        // A late blank commit or capture result must not restart the normal path.
        cleanup.onPageFinished(SessionCleanup.BLANK_URL)
        cleanup.start { cleaned++ }
        assertEquals(
            listOf("stop", "blank", "detach", "destroy", "storage", "cookies", "flush"),
            ops.calls,
        )
        assertEquals(0, cleaned)
    }

    @Test
    fun lateCaptureResultAfterCancelIsNotDelivered() {
        var cleaned = 0
        cleanup.start { cleaned++ }
        cleanup.cancel()
        cleanup.onPageFinished(SessionCleanup.BLANK_URL)

        ops.cookieCallback!!.invoke()
        assertTrue(cleanup.isCancelled)

        // The pending evaluateJavascript callback returns after cleanup finished.
        cleanup.start { cleaned++ }
        assertEquals(0, cleaned)
    }

    @Test
    fun cancellingWithoutACaptureStillCleansUp() {
        cleanup.release()
        assertEquals(listOf("stop", "blank"), ops.calls)

        cleanup.onPageFinished(SessionCleanup.BLANK_URL)
        assertEquals(listOf("stop", "blank", "storage", "cookies"), ops.calls)

        ops.cookieCallback!!.invoke()
        assertEquals("destroy", ops.calls.last())
    }

    private class FakeOps : SessionOps {
        val calls = mutableListOf<String>()
        var cookieCallback: (() -> Unit)? = null

        override fun stopPage() {
            calls += "stop"
        }

        override fun loadBlank() {
            calls += "blank"
        }

        override fun detach() {
            calls += "detach"
        }

        override fun clearStorage() {
            calls += "storage"
        }

        override fun removeCookies(onDone: () -> Unit) {
            calls += "cookies"
            cookieCallback = onDone
        }

        override fun flushCookies() {
            calls += "flush"
        }

        override fun clearCache() {
            calls += "cache"
        }

        override fun destroyPage() {
            calls += "destroy"
        }
    }
}
