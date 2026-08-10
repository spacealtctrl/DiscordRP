package net.spacealtctrl.discordrp.presence.tidy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceTrustTest {
    @Test
    fun `video front ends are treated as untrusted`() {
        assertEquals(SourceTrust.VIDEO, SourceKind.trustOf("org.schabi.newpipe"))
        assertEquals(SourceTrust.VIDEO, SourceKind.trustOf("com.google.android.youtube"))
        assertEquals(SourceTrust.VIDEO, SourceKind.trustOf("com.github.libretube"))
        assertEquals(SourceTrust.VIDEO, SourceKind.trustOf("org.mozilla.fenix"))
    }

    @Test
    fun `tagged players are left alone`() {
        assertEquals(SourceTrust.TAGGED, SourceKind.trustOf("app.symfonik.music.player"))
        assertEquals(SourceTrust.TAGGED, SourceKind.trustOf("com.spotify.music"))
        assertEquals(SourceTrust.TAGGED, SourceKind.trustOf("org.oxycblt.auxio"))
        assertEquals(SourceTrust.TAGGED, SourceKind.trustOf(null))
    }

    @Test
    fun `an album naming the app is junk`() {
        assertTrue(SourceKind.isJunkAlbum("NewPipe", "NewPipe"))
        assertTrue(SourceKind.isJunkAlbum("YouTube", "YouTube"))
        assertTrue(SourceKind.isJunkAlbum("Unknown album", "NewPipe"))
        assertTrue(SourceKind.isJunkAlbum(null, "NewPipe"))
        assertTrue(SourceKind.isJunkAlbum("  ", "NewPipe"))
    }

    @Test
    fun `singles and eponymous records are kept`() {
        assertFalse(SourceKind.isJunkAlbum("Blinding Lights", "NewPipe"))
        assertFalse(SourceKind.isJunkAlbum("Metallica", "NewPipe"))
    }

    @Test
    fun `a real album is kept`() {
        assertFalse(SourceKind.isJunkAlbum("OK Computer", "NewPipe"))
        assertFalse(SourceKind.isJunkAlbum("War Psalms", "Symfonium"))
    }

    @Test
    fun `label and handle channels are junk artists`() {
        assertTrue(SourceKind.isJunkArtist("TheWeekndVEVO", "NewPipe"))
        assertTrue(SourceKind.isJunkArtist("@somechannel", "NewPipe"))
        assertTrue(SourceKind.isJunkArtist("some_channel", "NewPipe"))
        assertTrue(SourceKind.isJunkArtist("channel1234", "NewPipe"))
        assertTrue(SourceKind.isJunkArtist("@music_uploads", "NewPipe"))
        assertTrue(SourceKind.isJunkArtist("NewPipe", "NewPipe"))
        assertTrue(SourceKind.isJunkArtist(null, "NewPipe"))
    }

    @Test
    fun `a topic channel is not junk because the artist is in it`() {
        assertFalse(SourceKind.isJunkArtist("Radiohead - Topic", "NewPipe"))
    }

    @Test
    fun `ordinary one word artist names are not mistaken for handles`() {
        assertFalse(SourceKind.isJunkArtist("Radiohead", "NewPipe"))
        assertFalse(SourceKind.isJunkArtist("Metallica", "NewPipe"))
        assertFalse(SourceKind.isJunkArtist("Beyonce", "NewPipe"))
        assertFalse(SourceKind.isJunkArtist("Slipknot", "NewPipe"))
    }

    @Test
    fun `bands with numbers in their name are not mistaken for handles`() {
        assertFalse(SourceKind.isJunkArtist("Blink182", "NewPipe"))
        assertFalse(SourceKind.isJunkArtist("Sum41", "NewPipe"))
        assertFalse(SourceKind.isJunkArtist("Matchbox20", "NewPipe"))
        assertFalse(SourceKind.isJunkArtist("U2", "NewPipe"))
    }
}
