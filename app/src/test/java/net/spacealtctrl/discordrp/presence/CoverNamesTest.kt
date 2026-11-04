package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverNamesTest {
    @Test
    fun `album drops featuring, single and edition tags`() {
        assertEquals("DUNE", CoverNames.album("DUNE (feat. Hatsune Miku) - Single"))
        assertEquals("Colorful Array", CoverNames.album("Colorful Array - Single"))
        assertEquals("Monsters", CoverNames.album("Monsters - EP"))
        assertEquals("Bad Vibrations", CoverNames.album("Bad Vibrations (Deluxe Edition)"))
        assertEquals("天気の子", CoverNames.album("天気の子"))
        assertEquals("Song A", CoverNames.album("Song \"A\""))
    }

    @Test
    fun `blank album is no album`() {
        assertNull(CoverNames.album(null))
        assertNull(CoverNames.album("  "))
        assertNull(CoverNames.album(" - Single"))
    }

    @Test
    fun `title drops featuring but keeps versions`() {
        assertEquals("グランドエスケープ", CoverNames.title("グランドエスケープ (feat. 三浦透子)"))
        assertEquals("Song", CoverNames.title("Song [ft. Someone]"))
        assertEquals("Collapsing (Orchestral Version)", CoverNames.title("Collapsing (Orchestral Version)"))
        assertNull(CoverNames.title(" "))
    }

    @Test
    fun `credits keep the whole credit then each artist`() {
        assertEquals(
            listOf("Jamie T & Leif Vollebekk", "Jamie T", "Leif Vollebekk"),
            CoverNames.credits("Jamie T & Leif Vollebekk"),
        )
        assertEquals(
            listOf("皇家音乐厅管弦乐团 & 里卡多・夏伊", "皇家音乐厅管弦乐团", "里卡多・夏伊"),
            CoverNames.credits("皇家音乐厅管弦乐团 & 里卡多・夏伊"),
        )
        assertEquals(listOf("A feat. B", "A", "B"), CoverNames.credits("A feat. B"))
        assertEquals(listOf("RADWIMPS"), CoverNames.credits("RADWIMPS"))
        assertEquals(listOf("Lil Nas X"), CoverNames.credits("Lil Nas X"))
        assertEquals(emptyList<String>(), CoverNames.credits(null))
    }

    @Test
    fun `credits merge several fields without repeats`() {
        assertEquals(
            listOf("Hachi", "Hachi feat. Hatsune Miku", "Hatsune Miku"),
            CoverNames.credits("Hachi", "Hachi feat. Hatsune Miku", "hachi"),
        )
    }

    @Test
    fun `keys ignore case, punctuation and width`() {
        assertEquals(CoverNames.key("shostakovich the jazz album"), CoverNames.key("Shostakovich: The Jazz Album"))
        assertEquals("里卡多 夏伊", CoverNames.key("里卡多・夏伊"))
        assertEquals("abc", CoverNames.key("ＡＢＣ"))
        assertEquals("", CoverNames.key(null))
    }

    @Test
    fun `same album ignores single and EP labels`() {
        assertTrue(CoverNames.sameAlbum("Cover Up Tattoo", "Cover Up Tattoo EP"))
        assertTrue(CoverNames.sameAlbum("Cover Up Tattoo - EP", "Cover Up Tattoo"))
        assertTrue(CoverNames.sameAlbum("Anthem Emo, Pt. 1", "Anthem Emo, Pt. 1 - Single"))
        assertFalse(CoverNames.sameAlbum("I Can't Make Friends (Like I Used To)", "Cover Up Tattoo EP"))
        assertFalse(CoverNames.sameAlbum(null, "Cover Up Tattoo"))
        assertFalse(CoverNames.sameAlbum("", ""))
    }
}
