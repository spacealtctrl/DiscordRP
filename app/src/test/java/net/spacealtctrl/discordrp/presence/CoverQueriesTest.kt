package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverQueriesTest {
    private val hachi = "8e893bac-144c-4a04-a9d7-9068759a7eed"

    @Test
    fun `album query pins the title and takes any credited artist`() {
        assertEquals(
            "releasegroup:\"天気の子\" AND (artist:\"RADWIMPS\")",
            CoverQueries.album("天気の子", listOf("RADWIMPS")),
        )
        assertEquals(
            "releasegroup:\"Ghosts\" AND (artist:\"Jamie T\" OR artist:\"Leif Vollebekk\")",
            CoverQueries.album("Ghosts", listOf("Jamie T", "Leif Vollebekk")),
        )
        assertEquals("releasegroup:\"天気の子\"", CoverQueries.album("天気の子", emptyList()))
    }

    @Test
    fun `song query pins the recording title`() {
        assertEquals(
            "recording:\"Collapsing\" AND (artist:\"Dead by April\")",
            CoverQueries.song("Collapsing", listOf("Dead by April")),
        )
    }

    @Test
    fun `phrases cannot break out of their quotes`() {
        assertEquals(
            "releasegroup:\"A B\" AND (artist:\"C D\")",
            CoverQueries.album("A \"B\"", listOf("C\\ D")),
        )
    }

    @Test
    fun `free text drops query syntax`() {
        assertEquals("Shostakovich the Jazz Album", CoverQueries.freeText("Shostakovich: the Jazz Album"))
        assertEquals("AC DC Back in Black", CoverQueries.freeText("AC/DC - Back in Black"))
    }

    @Test
    fun `artist lookup covers names, aliases and sort names`() {
        assertEquals(
            "artist:\"Hachi\" OR alias:\"Hachi\" OR sortname:\"Hachi\"",
            CoverQueries.artists(listOf("Hachi")),
        )
    }

    @Test
    fun `artist scoped queries only carry real MBIDs`() {
        assertEquals(
            "recording:\"DUNE\" AND arid:($hachi)",
            CoverQueries.songBy("DUNE", listOf(hachi, "not-an-id")),
        )
        assertEquals(
            "arid:($hachi) AND dur:[237000 TO 239000]",
            CoverQueries.lengthBy(238_000L, listOf(hachi)),
        )
        assertNull(CoverQueries.songBy("DUNE", listOf("not-an-id")))
        assertNull(CoverQueries.lengthBy(238_000L, emptyList()))
    }
}
