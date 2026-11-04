package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishedArtTest {
    private val group = "87bfd05d-7895-41ae-a1df-4773e9610033"

    @Test
    fun `discord media references become proxy links`() {
        assertEquals(
            "https://media.discordapp.net/external/abc/https/coverartarchive.org/release-group/x/front-500",
            DiscordMedia.url("mp:external/abc/https/coverartarchive.org/release-group/x/front-500"),
        )
        assertEquals(
            "https://media.discordapp.net/external/h/%3Fv%3D2/https/raw.githubusercontent.com/a.png",
            DiscordMedia.url("mp:external/h/%3Fv%3D2/https/raw.githubusercontent.com/a.png"),
        )
    }

    @Test
    fun `anything else has no proxy link`() {
        assertNull(DiscordMedia.url(null))
        assertNull(DiscordMedia.url("mp:"))
        assertNull(DiscordMedia.url("1446527242577641472"))
        assertNull(DiscordMedia.url("https://evil.test/a.png"))
    }

    @Test
    fun `adding a cover needs an album, a known release and square art`() {
        assertTrue(CoverOffer.canAdd(album = "Anthem Emo, Pt. 1", width = 512, height = 512, missingGroupId = group))
        assertTrue(CoverOffer.canAdd(album = "Anthem Emo, Pt. 1", width = 600, height = 580, missingGroupId = group))
    }

    @Test
    fun `video thumbnails, unknown releases and missing albums are not offered`() {
        assertFalse(CoverOffer.canAdd(album = "Collapsing", width = 1280, height = 720, missingGroupId = group))
        assertFalse(CoverOffer.canAdd(album = "Anthem Emo, Pt. 1", width = 512, height = 512, missingGroupId = null))
        assertFalse(CoverOffer.canAdd(album = "Anthem Emo, Pt. 1", width = 512, height = 512, missingGroupId = "nope"))
        assertFalse(CoverOffer.canAdd(album = null, width = 512, height = 512, missingGroupId = group))
        assertFalse(CoverOffer.canAdd(album = " ", width = 512, height = 512, missingGroupId = group))
        assertFalse(CoverOffer.canAdd(album = "Anthem Emo, Pt. 1", width = 0, height = 0, missingGroupId = group))
    }

    @Test
    fun `an album MusicBrainz lacks is added only with Deezer's details`() {
        assertTrue(CoverOffer.canAddAlbum("Footprints (Acoustic)", albumMissing = true, deezerAlbumId = 143466912))
        assertFalse(CoverOffer.canAddAlbum("Cover Up Tattoo EP", albumMissing = true, deezerAlbumId = null))
    }

    @Test
    fun `adding an album needs MusicBrainz to lack it`() {
        assertFalse(CoverOffer.canAddAlbum("Cover Up Tattoo EP", albumMissing = false, deezerAlbumId = 143466912))
        assertFalse(CoverOffer.canAddAlbum(null, albumMissing = true, deezerAlbumId = 143466912))
        assertFalse(CoverOffer.canAddAlbum(" ", albumMissing = true, deezerAlbumId = 143466912))
        assertFalse(CoverOffer.canAddAlbum("Cover Up Tattoo EP", albumMissing = true, deezerAlbumId = 0))
    }
}
