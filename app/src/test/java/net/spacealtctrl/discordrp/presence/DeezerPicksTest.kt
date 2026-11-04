package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeezerPicksTest {
    private val acoustic = "https://cdn-images.dzcdn.net/images/cover/aaa/1000x1000-000000-80-0-0.jpg"
    private val original = "https://cdn-images.dzcdn.net/images/cover/bbb/1000x1000-000000-80-0-0.jpg"

    private fun album(title: String, artist: String, cover: String? = acoustic) =
        DeezerAlbum(title = title, artist = DeezerArtist(artist), coverXl = cover)

    private fun track(title: String, artist: String, albumTitle: String, cover: String? = acoustic) =
        DeezerTrack(title = title, artist = DeezerArtist(artist), album = DeezerAlbumRef(title = albumTitle, coverXl = cover))

    @Test
    fun `album needs the same title and artist`() {
        val albums = listOf(album("Footprints", "WSTR", original), album("Footprints (Acoustic)", "WSTR", acoustic))
        assertEquals(acoustic, DeezerPicks.album(albums, "Footprints (Acoustic)", listOf("WSTR"))?.cover)
        assertNull(DeezerPicks.album(listOf(album("Footprints", "WSTR")), "Footprints (Acoustic)", listOf("WSTR")))
        assertNull(DeezerPicks.album(listOf(album("Footprints (Acoustic)", "Someone Else")), "Footprints (Acoustic)", listOf("WSTR")))
    }

    @Test
    fun `apple style single naming still matches`() {
        val albums = listOf(album("Anthem Emo, Pt. 1", "Good Hangs"))
        assertEquals(acoustic, DeezerPicks.album(albums, CoverNames.album("Anthem Emo, Pt. 1 - Single")!!, listOf("Good Hangs"))?.cover)
    }

    @Test
    fun `featured and duet credits still match`() {
        val credits = CoverNames.credits("Jamie T & Leif Vollebekk")
        assertEquals(acoustic, DeezerPicks.album(listOf(album("Ghosts", "Jamie T")), "Ghosts", credits)?.cover)
    }

    @Test
    fun `song without an album takes the single`() {
        val tracks = listOf(
            track("Collapsing (Orchestral Version)", "Dead by April", "Collapsing (Orchestral Version)", original),
            track("Collapsing", "Dead by April", "Hits 2018", original),
            track("Collapsing", "Dead by April", "Collapsing", acoustic),
        )
        assertEquals(acoustic, DeezerPicks.track(tracks, "Collapsing", null, listOf("Dead by April"))?.cover)
    }

    @Test
    fun `EP labels do not stop an album match`() {
        assertEquals(acoustic, DeezerPicks.album(listOf(album("Cover Up Tattoo", "Good Hangs")), "Cover Up Tattoo EP", listOf("Good Hangs"))?.cover)
    }

    @Test
    fun `song on a known album only takes that album`() {
        val tracks = listOf(track("Footprints", "WSTR", "Red, Green or Inbetween", original))
        assertNull(DeezerPicks.track(tracks, "Footprints", "Footprints (Acoustic)", listOf("WSTR")))
        assertEquals(original, DeezerPicks.track(tracks, "Footprints", "Red, Green or Inbetween", listOf("WSTR"))?.cover)
    }

    @Test
    fun `the Deezer album is remembered with the cover`() {
        val albums = listOf(DeezerAlbum(id = 143466912, title = "Footprints (Acoustic)", artist = DeezerArtist("WSTR"), coverXl = acoustic))
        assertEquals(DeezerHit(acoustic, 143466912), DeezerPicks.album(albums, "Footprints (Acoustic)", listOf("WSTR")))
        val tracks = listOf(DeezerTrack(title = "Collapsing", artist = DeezerArtist("Dead by April"), album = DeezerAlbumRef(id = 7, title = "Collapsing", coverXl = acoustic)))
        assertEquals(DeezerHit(acoustic, 7), DeezerPicks.track(tracks, "Collapsing", null, listOf("Dead by April")))
    }

    @Test
    fun `covers must come from Deezer's image servers`() {
        assertTrue(DeezerPicks.isDeezerImage(acoustic))
        assertTrue(DeezerPicks.isDeezerImage("https://e-cdns-images.dzcdn.net/images/cover/x/1000x1000.jpg"))
        assertFalse(DeezerPicks.isDeezerImage("http://cdn-images.dzcdn.net/images/cover/x/1000x1000.jpg"))
        assertFalse(DeezerPicks.isDeezerImage("https://evil.test/images/cover/x.jpg"))
        assertFalse(DeezerPicks.isDeezerImage("https://dzcdn.net.evil.test/x.jpg"))
        assertFalse(DeezerPicks.isDeezerImage("https://user@cdn-images.dzcdn.net/x.jpg"))
        assertFalse(DeezerPicks.isDeezerImage(null))
        assertNull(DeezerPicks.album(listOf(album("Ghosts", "Jamie T", "https://evil.test/x.jpg")), "Ghosts", listOf("Jamie T")))
    }
}
