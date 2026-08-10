package net.spacealtctrl.discordrp.presence.tidy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextCanonTest {
    @Test
    fun `folding ignores case, accents and punctuation`() {
        assertEquals(TextCanon.fold("Beyoncé"), TextCanon.fold("BEYONCE"))
        assertEquals(TextCanon.fold("Sigur Rós"), TextCanon.fold("sigur ros"))
        assertEquals(TextCanon.fold("Don't Stop!"), TextCanon.fold("dont stop"))
    }

    @Test
    fun `folding ignores a leading the and spells out an ampersand`() {
        assertEquals(TextCanon.fold("The Weeknd"), TextCanon.fold("Weeknd"))
        assertEquals(TextCanon.fold("Simon & Garfunkel"), TextCanon.fold("Simon and Garfunkel"))
    }

    @Test
    fun `folding drops a featured artist so credits still line up`() {
        assertEquals(TextCanon.fold("Stay (feat. Justin Bieber)"), TextCanon.fold("Stay"))
        assertEquals(TextCanon.fold("Song ft. Someone"), TextCanon.fold("Song"))
    }

    @Test
    fun `the word with is left alone because it is ordinary English`() {
        assertTrue(TextCanon.fold("With or Without You").isNotEmpty())
        assertEquals("with or without you", TextCanon.fold("With or Without You"))
        assertTrue(TextCanon.similarity("Stay With Me", "Stay With You") < 1.0)
        assertTrue(TextCanon.similarity("Stay With Me", "Stay") < 1.0)
    }

    @Test
    fun `full width characters fold to their plain forms`() {
        assertEquals(TextCanon.fold("ＩＤＯＬ"), TextCanon.fold("IDOL"))
    }

    @Test
    fun `identical text scores one`() {
        assertEquals(1.0, TextCanon.similarity("Blinding Lights", "blinding lights"), 0.0001)
    }

    @Test
    fun `extra words cost something so a remix cannot look perfect`() {
        val exact = TextCanon.similarity("Stay", "Stay")
        val remix = TextCanon.similarity("Stay", "Stay (Leotrix remix)")
        assertTrue("a remix must score below an exact match", remix < exact)
        assertTrue("a remix must not look like a confident match", remix < 0.8)
    }

    @Test
    fun `unrelated text scores low`() {
        assertTrue(TextCanon.similarity("Karma Police", "Rust vs Go in 2025") < 0.4)
        assertTrue(TextCanon.similarity("Radiohead", "SomeVlogger") < 0.4)
    }

    @Test
    fun `spelling drift still clears the artist bar`() {
        assertTrue(TextCanon.similarity("The Weeknd", "The Weekend") > 0.7)
    }

    @Test
    fun `blank text never matches anything`() {
        assertEquals(0.0, TextCanon.similarity("", "anything"), 0.0001)
        assertEquals(0.0, TextCanon.similarity(null, null), 0.0001)
    }

    @Test
    fun `scripts without spaces still compare`() {
        assertEquals(1.0, TextCanon.similarity("アイドル", "アイドル"), 0.0001)
        assertTrue(TextCanon.similarity("アイドル", "夜に駆ける") < 0.4)
    }
}
