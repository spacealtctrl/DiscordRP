package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BridgeFeedTest {
    private val song = NowPlaying(
        title = "Anthem Emo",
        artist = "Good Hangs",
        album = "Anthem Emo, Pt. 1",
        appLabel = "Symfonium",
        appPackage = "app.symfonik.music.player",
    )
    private val art = PublishedArt(large = "mp:external/a/https/x", small = "mp:external/b/https/y")

    @Before
    fun reset() = BridgeFeed.serviceStopped()

    @Test
    fun `art follows the song it was published for`() {
        BridgeFeed.trackChanged(song)
        BridgeFeed.artPublished(song, art)
        assertEquals(art, BridgeFeed.snapshot.value.art)
    }

    @Test
    fun `the same song playing on keeps its art`() {
        BridgeFeed.trackChanged(song)
        BridgeFeed.artPublished(song, art)
        BridgeFeed.trackChanged(song.copy(playing = true, startedAtMillis = 5L))
        assertEquals(art, BridgeFeed.snapshot.value.art)
    }

    @Test
    fun `a new song drops the old art`() {
        BridgeFeed.trackChanged(song)
        BridgeFeed.artPublished(song, art)
        BridgeFeed.trackChanged(song.copy(title = "Next One"))
        assertNull(BridgeFeed.snapshot.value.art)
    }

    @Test
    fun `art for a song that already ended is ignored`() {
        BridgeFeed.trackChanged(song.copy(title = "Next One"))
        BridgeFeed.artPublished(song, art)
        assertNull(BridgeFeed.snapshot.value.art)
    }

    @Test
    fun `nothing playing clears the art`() {
        BridgeFeed.trackChanged(song)
        BridgeFeed.artPublished(song, art)
        BridgeFeed.trackChanged(null)
        assertNull(BridgeFeed.snapshot.value.art)
    }
}
