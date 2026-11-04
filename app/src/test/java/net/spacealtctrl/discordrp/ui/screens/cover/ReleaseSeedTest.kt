package net.spacealtctrl.discordrp.ui.screens.cover

import net.spacealtctrl.discordrp.presence.DeezerAlbumDetail
import net.spacealtctrl.discordrp.presence.DeezerAlbumTrack
import net.spacealtctrl.discordrp.presence.DeezerArtist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseSeedTest {
    private val wstr = "60dcd6fa-c279-40de-894e-431337409905"
    private val footprints = DeezerAlbumDetail(
        title = "Footprints (Acoustic)",
        upc = "790692286560",
        releaseDate = "2016-10-27",
        recordType = "single",
        label = "Hopeless Records",
        link = "https://www.deezer.com/album/143466912",
        artist = DeezerArtist("WSTR"),
    )
    private val track = DeezerAlbumTrack(title = "Footprints (Acoustic)", duration = 179, trackPosition = 1, diskNumber = 1, artist = DeezerArtist("WSTR"))

    private fun fields(
        album: DeezerAlbumDetail = footprints,
        tracks: List<DeezerAlbumTrack> = listOf(track),
        mbid: String? = wstr,
    ) = ReleaseSeed.fields(album, tracks, mbid)

    private fun List<Pair<String, String>>.value(name: String) = firstOrNull { it.first == name }?.second

    @Test
    fun `a single is seeded with everything Deezer knows`() {
        val seed = fields()
        assertEquals("Footprints (Acoustic)", seed.value("name"))
        assertEquals("WSTR", seed.value("artist_credit.names.0.name"))
        assertEquals(wstr, seed.value("artist_credit.names.0.mbid"))
        assertEquals("single", seed.value("type"))
        assertEquals("official", seed.value("status"))
        assertEquals("790692286560", seed.value("barcode"))
        assertEquals("2016", seed.value("events.0.date.year"))
        assertEquals("10", seed.value("events.0.date.month"))
        assertEquals("27", seed.value("events.0.date.day"))
        assertEquals("XW", seed.value("events.0.country"))
        assertEquals("Hopeless Records", seed.value("labels.0.name"))
        assertEquals("Digital Media", seed.value("mediums.0.format"))
        assertEquals("Footprints (Acoustic)", seed.value("mediums.0.track.0.name"))
        assertEquals("1", seed.value("mediums.0.track.0.number"))
        assertEquals("179000", seed.value("mediums.0.track.0.length"))
        assertEquals("https://www.deezer.com/album/143466912", seed.value("urls.0.url"))
        assertEquals(ReleaseSeed.ADDED_MARKER, seed.value("redirect_uri"))
        assertEquals(
            "Imported from https://www.deezer.com/album/143466912 with DiscordRP: https://github.com/spacealtctrl/DiscordRP",
            seed.value("edit_note"),
        )
    }

    @Test
    fun `an unknown artist is credited by name for the editor to link`() {
        val seed = fields(mbid = null)
        assertNull(seed.value("artist_credit.names.0.mbid"))
        assertEquals("WSTR", seed.value("artist_credit.names.0.artist.name"))
    }

    @Test
    fun `discs and track order follow Deezer`() {
        val tracks = listOf(
            DeezerAlbumTrack(title = "B2", duration = 60, trackPosition = 2, diskNumber = 2, artist = DeezerArtist("WSTR")),
            DeezerAlbumTrack(title = "A1", duration = 61, trackPosition = 1, diskNumber = 1, artist = DeezerArtist("WSTR")),
            DeezerAlbumTrack(title = "B1", duration = 62, trackPosition = 1, diskNumber = 2, artist = DeezerArtist("WSTR")),
        )
        val seed = fields(album = footprints.copy(recordType = "album"), tracks = tracks)
        assertEquals("A1", seed.value("mediums.0.track.0.name"))
        assertEquals("B1", seed.value("mediums.1.track.0.name"))
        assertEquals("B2", seed.value("mediums.1.track.1.name"))
        assertEquals("Digital Media", seed.value("mediums.1.format"))
    }

    @Test
    fun `compilations and guest artists are kept`() {
        val guest = DeezerAlbumTrack(title = "Duet", duration = 100, trackPosition = 1, diskNumber = 1, artist = DeezerArtist("Someone Else"))
        val seed = fields(album = footprints.copy(recordType = "compile"), tracks = listOf(guest))
        assertEquals(listOf("album", "compilation"), seed.filter { it.first == "type" }.map { it.second })
        assertEquals("Someone Else", seed.value("mediums.0.track.0.artist_credit.names.0.name"))
        assertNull(fields().value("mediums.0.track.0.artist_credit.names.0.name"))
    }

    @Test
    fun `missing or junk details are left out`() {
        val seed = fields(album = footprints.copy(upc = "n/a", releaseDate = "0000-00-00", label = " ", link = "https://evil.test/album/1"))
        assertNull(seed.value("barcode"))
        assertNull(seed.value("events.0.date.year"))
        assertNull(seed.value("labels.0.name"))
        assertNull(seed.value("urls.0.url"))
        assertFalse(seed.any { it.second.contains("evil.test") })
        assertEquals("Imported from Deezer with DiscordRP: https://github.com/spacealtctrl/DiscordRP", seed.value("edit_note"))
    }

    @Test
    fun `the new release is read back from the redirect`() {
        val id = "545f7071-1fee-485d-8e0a-9f036708c3f2"
        assertEquals(id, ReleaseSeed.addedRelease("${ReleaseSeed.ADDED_MARKER}?release_mbid=$id"))
        assertNull(ReleaseSeed.addedRelease("${ReleaseSeed.ADDED_MARKER}?release_mbid=nope"))
        assertNull(ReleaseSeed.addedRelease("https://musicbrainz.org/release/$id"))
        assertNull(ReleaseSeed.addedRelease("https://evil.test/discordrp-release-added?release_mbid=$id"))
    }

    @Test
    fun `the form body is url encoded`() {
        assertEquals("name=A+%26+B&type=single", ReleaseSeed.body(listOf("name" to "A & B", "type" to "single")))
    }
}
