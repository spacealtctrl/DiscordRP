package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityLinesTest {
    private fun arrange(songAsTitle: Boolean = false, artistAsTitle: Boolean = false) =
        PresenceComposer.arrangeLines(
            title = "Karry On",
            artist = "Morning Glory",
            album = "War Psalms",
            appLabel = "Symfonium",
            songAsTitle = songAsTitle,
            artistAsTitle = artistAsTitle,
        )

    @Test
    fun `default layout headlines the app`() {
        val lines = arrange()
        assertEquals("Symfonium", lines.name)
        assertEquals("Karry On", lines.details)
        assertEquals("Morning Glory", lines.state)
        assertEquals("War Psalms", lines.largeText)
    }

    @Test
    fun `song layout headlines the track`() {
        val lines = arrange(songAsTitle = true)
        assertEquals("Karry On", lines.name)
        assertEquals("Morning Glory", lines.details)
        assertEquals("War Psalms", lines.state)
        assertEquals("Symfonium", lines.largeText)
    }

    @Test
    fun `artist layout headlines the artist`() {
        val lines = arrange(artistAsTitle = true)
        assertEquals("Morning Glory", lines.name)
        assertEquals("Karry On", lines.details)
        assertEquals("Morning Glory", lines.state)
        assertEquals("War Psalms", lines.largeText)
    }

    @Test
    fun `artist layout wins when both are set`() {
        val lines = arrange(songAsTitle = true, artistAsTitle = true)
        assertEquals("Morning Glory", lines.name)
    }

    @Test
    fun `artist layout falls back to the track when artist is missing`() {
        val lines = PresenceComposer.arrangeLines(
            title = "Karry On",
            artist = null,
            album = null,
            appLabel = "Symfonium",
            songAsTitle = false,
            artistAsTitle = true,
        )
        assertEquals("Karry On", lines.name)
        assertEquals("", lines.state)
        assertEquals("Symfonium", lines.largeText)
    }
}
