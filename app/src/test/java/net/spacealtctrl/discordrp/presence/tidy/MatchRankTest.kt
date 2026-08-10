package net.spacealtctrl.discordrp.presence.tidy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchRankTest {
    private fun album(
        title: String,
        primary: String? = "Album",
        secondary: List<String> = emptyList(),
        status: String? = "Official",
        date: String? = "1997-05-21",
        groupId: String? = null,
    ) = CandidateRelease(title, groupId, primary, secondary, status, date)

    @Test
    fun `nonsense is refused however confident the catalogue sounds`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Nobody at All",
                    artist = "FM",
                    relevance = 100,
                    releases = listOf(album("City of Fear")),
                ),
            ),
            wantTitle = "I bought a \$1 house and this happened",
            wantArtist = "SomeVlogger",
            wantDurationMs = 1_320_000,
        )
        assertNull(facts)
    }

    @Test
    fun `a matching song is accepted`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    relevance = 100,
                    durationMs = 200_046,
                    releases = listOf(album("After Hours", date = "2020-03-20")),
                ),
            ),
            wantTitle = "Blinding Lights",
            wantArtist = "The Weeknd",
            wantDurationMs = 262_000,
        )
        assertNotNull(facts)
        assertEquals("Blinding Lights", facts?.title)
        assertEquals("The Weeknd", facts?.artist)
        assertEquals("After Hours", facts?.album)
        assertEquals(AlbumQuality.CLEAN, facts?.albumQuality)
    }

    @Test
    fun `the album is taken across every verified answer, not just the top one`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Karma Police",
                    artist = "Radiohead",
                    relevance = 100,
                    durationMs = 264_000,
                    releases = listOf(
                        album(
                            "2001-08-01: Hutchinson Field",
                            secondary = listOf("Live"),
                            status = "Bootleg",
                            date = null,
                        ),
                    ),
                ),
                Candidate(
                    title = "Karma Police",
                    artist = "Radiohead",
                    relevance = 100,
                    durationMs = 261_000,
                    releases = listOf(album("OK Computer", groupId = "rg-ok-computer")),
                ),
            ),
            wantTitle = "Karma Police",
            wantArtist = "Radiohead",
            wantDurationMs = 264_000,
        )
        assertEquals("OK Computer", facts?.album)
        assertEquals("rg-ok-computer", facts?.releaseGroupId)
    }

    @Test
    fun `a compilation loses to a studio album`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "STAY",
                    artist = "The Kid LAROI & Justin Bieber",
                    relevance = 100,
                    releases = listOf(
                        album("Die ultimative Chartshow", secondary = listOf("Compilation")),
                        album("F*CK LOVE 3: OVER YOU", date = "2021-07-23"),
                    ),
                ),
            ),
            wantTitle = "STAY",
            wantArtist = "The Kid LAROI, Justin Bieber",
            wantDurationMs = 141_000,
        )
        assertEquals("F*CK LOVE 3: OVER YOU", facts?.album)
    }

    @Test
    fun `the album the player supplied is preferred when it verifies`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Karma Police",
                    artist = "Radiohead",
                    relevance = 100,
                    releases = listOf(
                        album("Hail to the Thief", date = "2003-06-09"),
                        album("OK Computer", date = "1997-05-21"),
                    ),
                ),
            ),
            wantTitle = "Karma Police",
            wantArtist = "Radiohead",
            wantDurationMs = null,
            albumHint = "OK Computer",
        )
        assertEquals("OK Computer", facts?.album)
    }

    @Test
    fun `an exact match outranks a remix of the same name`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Stay (Leotrix remix)",
                    artist = "The Kid LAROI",
                    relevance = 100,
                    releases = listOf(album("Stay (Leotrix remix)", primary = "Single")),
                ),
                Candidate(
                    title = "Stay",
                    artist = "The Kid LAROI",
                    relevance = 100,
                    releases = listOf(album("F*CK LOVE")),
                ),
            ),
            wantTitle = "Stay",
            wantArtist = "The Kid LAROI",
            wantDurationMs = null,
        )
        assertEquals("Stay", facts?.title)
    }

    @Test
    fun `a duration disagreement orders candidates but never rejects one`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Levels (Skrillex remix)",
                    artist = "Avicii",
                    relevance = 100,
                    durationMs = 279_000,
                    releases = listOf(album("Levels", primary = "Single")),
                ),
            ),
            wantTitle = "Levels (Skrillex Remix)",
            wantArtist = "Avicii",
            wantDurationMs = 330_000,
        )
        assertNotNull(facts)
        assertEquals("Levels (Skrillex remix)", facts?.title)
    }

    @Test
    fun `the closer duration wins between otherwise equal answers`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Creep",
                    artist = "Radiohead",
                    relevance = 100,
                    durationMs = 500_000,
                    releases = listOf(album("Live at Somewhere", secondary = listOf("Live"))),
                ),
                Candidate(
                    title = "Creep",
                    artist = "Radiohead",
                    relevance = 100,
                    durationMs = 238_000,
                    releases = listOf(album("Pablo Honey", date = "1993-02-22")),
                ),
            ),
            wantTitle = "Creep",
            wantArtist = "Radiohead",
            wantDurationMs = 239_000,
        )
        assertEquals("Pablo Honey", facts?.album)
    }

    @Test
    fun `a wrong artist is refused even when the title is exact`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Creep",
                    artist = "Tears for Fears",
                    relevance = 100,
                    releases = listOf(album("Some Covers Record")),
                ),
            ),
            wantTitle = "Creep",
            wantArtist = "Radiohead",
            wantDurationMs = null,
        )
        assertNull(facts)
    }

    @Test
    fun `bootleg only answers report that no clean album was found`() {
        val facts = MatchRank.resolve(
            candidates = listOf(
                Candidate(
                    title = "Karma Police",
                    artist = "Radiohead",
                    relevance = 100,
                    releases = listOf(
                        album("Some Bootleg", secondary = listOf("Live"), status = "Bootleg", date = null),
                    ),
                ),
            ),
            wantTitle = "Karma Police",
            wantArtist = "Radiohead",
            wantDurationMs = null,
        )
        assertNotNull(facts)
        assertTrue(facts?.albumQuality != AlbumQuality.CLEAN)
    }

    @Test
    fun `an empty catalogue answer resolves to nothing`() {
        assertNull(MatchRank.resolve(emptyList(), "Anything", "Anyone", null))
    }
}
