package net.spacealtctrl.discordrp.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverSourceTest {
    @Test
    fun `MusicBrainz alone is the default`() {
        assertEquals(CoverSource.MUSICBRAINZ, CoverSource.fromStored(null))
        assertEquals(CoverSource.MUSICBRAINZ, CoverSource.fromStored("deezer"))
    }

    @Test
    fun `stored choices come back`() {
        for (source in CoverSource.entries) assertEquals(source, CoverSource.fromStored(source.stored))
    }

    @Test
    fun `only the second choice asks Deezer`() {
        assertFalse(CoverSource.MUSICBRAINZ.deezerBackup)
        assertTrue(CoverSource.MUSICBRAINZ_DEEZER.deezerBackup)
    }
}
