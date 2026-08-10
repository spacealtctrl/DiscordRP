package net.spacealtctrl.discordrp.presence.tidy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleScrubTest {
    @Test
    fun `the reported NewPipe failure comes out clean`() {
        val scrubbed = TitleScrub.scrub(
            title = "The Weeknd - Blinding Lights (Official Video)",
            artist = "TheWeekndVEVO",
            album = "NewPipe",
            appLabel = "NewPipe",
        )
        assertEquals("Blinding Lights", scrubbed.title)
        assertEquals("The Weeknd", scrubbed.artist)
        assertNull(scrubbed.album)
        assertTrue(scrubbed.worthLookup)
    }

    @Test
    fun `topic channels name the artist`() {
        val scrubbed = TitleScrub.scrub(
            title = "Karma Police",
            artist = "Radiohead - Topic",
            album = "OK Computer",
            appLabel = "NewPipe",
        )
        assertEquals("Karma Police", scrubbed.title)
        assertEquals("Radiohead", scrubbed.artist)
        assertEquals("OK Computer", scrubbed.album)
    }

    @Test
    fun `a redundant artist prefix is removed from the title`() {
        val scrubbed = TitleScrub.scrub(
            title = "Radiohead - Karma Police [Official Music Video]",
            artist = "Radiohead",
            album = null,
            appLabel = "NewPipe",
        )
        assertEquals("Karma Police", scrubbed.title)
        assertEquals("Radiohead", scrubbed.artist)
    }

    @Test
    fun `a reversed title is put back in order`() {
        val scrubbed = TitleScrub.scrub(
            title = "Blinding Lights - The Weeknd",
            artist = "The Weeknd",
            album = null,
            appLabel = "NewPipe",
        )
        assertEquals("Blinding Lights", scrubbed.title)
        assertEquals("The Weeknd", scrubbed.artist)
    }

    @Test
    fun `a tagged artist stops the title being split at the wrong place`() {
        val scrubbed = TitleScrub.scrub(
            title = "Some Song - Live Version",
            artist = "Real Artist",
            album = "Real Album",
            appLabel = "Symfonium",
        )
        assertEquals("Some Song - Live Version", scrubbed.title)
        assertEquals("Real Artist", scrubbed.artist)
        assertEquals("Real Album", scrubbed.album)
    }

    @Test
    fun `genuine qualifiers survive scrubbing`() {
        assertEquals("Levels (Skrillex Remix)", TitleScrub.scrubTitle("Levels (Skrillex Remix)"))
        assertEquals("Creep (Acoustic)", TitleScrub.scrubTitle("Creep (Acoustic)"))
        assertEquals(
            "Creep (Live at Glastonbury 2003)",
            TitleScrub.scrubTitle("Creep (Live at Glastonbury 2003) [Official Video]"),
        )
    }

    @Test
    fun `decoration is stripped in its many shapes`() {
        assertEquals("Song", TitleScrub.scrubTitle("Song (Official Music Video)"))
        assertEquals("Song", TitleScrub.scrubTitle("Song [Official Audio]"))
        assertEquals("Song", TitleScrub.scrubTitle("Song | Lyrics"))
        assertEquals("Song", TitleScrub.scrubTitle("Song - Official Video"))
        assertEquals("Song", TitleScrub.scrubTitle("01. Song"))
        assertEquals("Song", TitleScrub.scrubTitle("Song (4K)"))
        assertEquals("Song", TitleScrub.scrubTitle("Song 【MV】"))
    }

    @Test
    fun `the japanese upload shape splits on its brackets`() {
        val scrubbed = TitleScrub.scrub(
            title = "YOASOBI「アイドル」Official Music Video",
            artist = "Ayase / YOASOBI",
            album = "NewPipe",
            appLabel = "NewPipe",
        )
        assertEquals("アイドル", scrubbed.title)
        assertEquals("YOASOBI", scrubbed.artist)
    }

    @Test
    fun `uploads that are not one song are not looked up`() {
        val album = TitleScrub.scrub(
            title = "Pink Floyd - The Dark Side of the Moon (Full Album)",
            artist = "PinkFloydVEVO",
            album = "NewPipe",
            appLabel = "NewPipe",
        )
        assertFalse(album.worthLookup)

        val stream = TitleScrub.scrub(
            title = "lofi hip hop radio 24/7 - beats to relax to",
            artist = "Lofi Girl",
            album = "NewPipe",
            appLabel = "NewPipe",
        )
        assertFalse(stream.worthLookup)
    }

    @Test
    fun `ordinary words that happen to be decoration survive`() {
        assertEquals("Make It Official", TitleScrub.scrubTitle("Make It Official"))
        assertEquals("Nothing but Lyrics", TitleScrub.scrubTitle("Nothing but Lyrics"))
        assertEquals("Sonic HD", TitleScrub.scrubTitle("Sonic HD"))
        assertEquals("La Letra", TitleScrub.scrubTitle("La Letra"))
    }

    @Test
    fun `numeric song titles are not read as track numbers`() {
        val song = TitleScrub.scrub(
            title = "22 - Taylor Swift",
            artist = "TaylorSwiftVEVO",
            album = null,
            appLabel = "NewPipe",
        )
        assertEquals("22", song.title)
        assertEquals("Taylor Swift", song.artist)
    }

    @Test
    fun `the earliest separator splits the title`() {
        val scrubbed = TitleScrub.scrub(
            title = "Metallica | Master of Puppets - Live",
            artist = "@metallicaTV",
            album = null,
            appLabel = "NewPipe",
        )
        assertEquals("Metallica", scrubbed.artist)
        assertEquals("Master of Puppets - Live", scrubbed.title)
    }

    @Test
    fun `a topic channel outranks a bracketed title`() {
        val scrubbed = TitleScrub.scrub(
            title = "TVアニメ「進撃の巨人」オープニングテーマ",
            artist = "Linked Horizon - Topic",
            album = "自由への進撃",
            appLabel = "NewPipe",
        )
        assertEquals("Linked Horizon", scrubbed.artist)
    }

    @Test
    fun `an artist that just repeats the title is ignored`() {
        val scrubbed = TitleScrub.scrub(
            title = "Queen - Bohemian Rhapsody",
            artist = "Queen - Bohemian Rhapsody",
            album = "NewPipe",
            appLabel = "NewPipe",
        )
        assertEquals("Bohemian Rhapsody", scrubbed.title)
        assertEquals("Queen", scrubbed.artist)
    }

    @Test
    fun `a label channel still yields an artist when the title has none`() {
        val scrubbed = TitleScrub.scrub(
            title = "Blinding Lights",
            artist = "TheWeekndVEVO",
            album = null,
            appLabel = "NewPipe",
        )
        assertEquals("Blinding Lights", scrubbed.title)
        assertEquals("TheWeeknd", scrubbed.artist)
    }

    @Test
    fun `a single keeps its album`() {
        val scrubbed = TitleScrub.scrub(
            title = "Blinding Lights",
            artist = "The Weeknd - Topic",
            album = "Blinding Lights",
            appLabel = "NewPipe",
        )
        assertEquals("Blinding Lights", scrubbed.album)
    }

    @Test
    fun `real songs are not mistaken for whole album uploads`() {
        assertTrue(TitleScrub.scrub("Drake - Nonstop", "DrakeVEVO", null, "NewPipe").worthLookup)
        assertTrue(TitleScrub.scrub("Non-Stop", "HamiltonVEVO", null, "NewPipe").worthLookup)
    }

    @Test
    fun `a blank result never replaces the original title`() {
        assertEquals("Official Video", TitleScrub.scrubTitle("Official Video"))
        val scrubbed = TitleScrub.scrub("(Official Video)", null, null, "NewPipe")
        assertTrue(scrubbed.title.isNotBlank())
    }

    @Test
    fun `trailing brackets come off only for the fallback query`() {
        assertEquals("Creep", TitleScrub.stripTrailingBracket("Creep (Live at Glastonbury 2003)"))
        assertEquals("Levels", TitleScrub.stripTrailingBracket("Levels (Skrillex Remix)"))
        assertEquals("Song", TitleScrub.stripTrailingBracket("Song"))
    }

    @Test
    fun `channel names are turned into artist names`() {
        assertEquals("Radiohead", TitleScrub.cleanArtist("Radiohead - Topic"))
        assertEquals("TheWeeknd", TitleScrub.cleanArtist("TheWeekndVEVO"))
        assertEquals("Morning Glory", TitleScrub.cleanArtist("Morning Glory"))
        assertNull(TitleScrub.cleanArtist("   "))
    }
}
