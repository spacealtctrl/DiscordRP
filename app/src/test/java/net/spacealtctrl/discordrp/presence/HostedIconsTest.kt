package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Test

class HostedIconsTest {
    @Test
    fun `patched YouTube Music builds use the official icon`() {
        for (pkg in listOf(
            "app.morphe.android.apps.youtube.music",
            "app.revanced.android.apps.youtube.music",
            "app.rvx.android.apps.youtube.music",
        )) {
            assertEquals(pkg, "com.google.android.apps.youtube.music", HostedIcons.nameFor(pkg))
        }
    }

    @Test
    fun `patched YouTube builds use the official icon`() {
        assertEquals("com.google.android.youtube", HostedIcons.nameFor("app.revanced.android.youtube"))
        assertEquals("com.google.android.youtube", HostedIcons.nameFor("app.morphe.android.youtube"))
    }

    @Test
    fun `other players keep their own icon`() {
        for (pkg in listOf(
            "com.google.android.apps.youtube.music",
            "com.google.android.youtube.tv",
            "com.google.android.apps.youtube.kids",
            "app.symfonik.music.player",
            "com.apple.android.music",
        )) {
            assertEquals(pkg, HostedIcons.nameFor(pkg))
        }
    }
}
