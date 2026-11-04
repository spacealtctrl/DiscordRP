package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverPicksTest {
    private fun group(id: String, title: String, score: Int = 100, credit: String = "Artist") =
        BrainzGroup(id = id, title = title, score = score, credit = listOf(BrainzCredit(name = credit)))

    private fun recording(
        title: String,
        vararg groups: Pair<String, String>,
        score: Int = 100,
        length: Long? = null,
        credit: String = "Artist",
    ) = BrainzRecording(
        title = title,
        score = score,
        length = length,
        releases = groups.map { (id, name) -> BrainzRelease(group = BrainzGroupRef(id = id, title = name)) },
        credit = listOf(BrainzCredit(name = credit)),
    )

    private val shown = listOf("Artist")

    @Test
    fun `album groups keep only the same album and drop weak scores`() {
        val picks = CoverPicks.groups(
            listOf(group("long", "天気の子 complete version", 88), group("exact", "天気の子"), group("weak", "天気の子", 40)),
            "天気の子",
        )
        assertEquals(listOf("exact"), picks)
    }

    @Test
    fun `another album that contains the title never lends its cover`() {
        val groups = listOf(group("live", "Hope (Live)"), group("hits", "Hope: Greatest Hits", 95))
        assertEquals(emptyList<String>(), CoverPicks.groups(groups, "Hope"))
        assertEquals(listOf("deluxe"), CoverPicks.groups(listOf(group("deluxe", "Hope (Deluxe Edition)")), "Hope"))
    }

    @Test
    fun `only an exact album title is a place to add a cover`() {
        val groups = listOf(group("long", "天気の子 complete version", 88), group("exact", "天気の子"))
        assertEquals("exact", CoverPicks.exactGroup(groups, "天気の子"))
        assertEquals(null, CoverPicks.exactGroup(listOf(group("long", "天気の子 complete version", 88)), "天気の子"))
        assertEquals(null, CoverPicks.exactGroup(listOf(group("weak", "天気の子", 40)), "天気の子"))
    }

    @Test
    fun `songs on a named album only give that album`() {
        val picks = CoverPicks.songs(
            listOf(recording("Collapsing", "comp" to "Hits 2018", "single" to "Collapsing", "lp" to "Worlds Collide")),
            title = "Collapsing",
            album = "Worlds Collide",
            credits = shown,
        )
        assertEquals(listOf("lp"), picks)
    }

    @Test
    fun `a b-side never lends its single's cover to another album`() {
        val bSide = recording("Cover Up Tattoo", "friends" to "I Can't Make Friends (Like I Used To)", credit = "Good Hangs")
        val credits = listOf("Good Hangs")
        assertEquals(emptyList<String>(), CoverPicks.songs(listOf(bSide), "Cover Up Tattoo", "Cover Up Tattoo EP", credits))
        assertEquals(
            emptyList<String>(),
            CoverPicks.byLength(listOf(bSide.copy(length = 124_548)), 124_000, "Cover Up Tattoo EP", credits),
        )
    }

    @Test
    fun `an EP named with or without EP is the same album`() {
        val ep = recording("Cover Up Tattoo", "ep" to "Cover Up Tattoo", credit = "Good Hangs")
        assertEquals(listOf("ep"), CoverPicks.songs(listOf(ep), "Cover Up Tattoo", "Cover Up Tattoo EP", listOf("Good Hangs")))
        assertEquals(listOf("ep"), CoverPicks.groups(listOf(group("ep", "Cover Up Tattoo")), "Cover Up Tattoo EP"))
        assertEquals("ep", CoverPicks.exactGroup(listOf(group("ep", "Cover Up Tattoo")), "Cover Up Tattoo EP"))
    }

    @Test
    fun `a translated storefront may still match by length`() {
        val dune = recording("砂の惑星", "single" to "砂の惑星", length = 239_000, credit = "ハチ")
        assertEquals(listOf("single"), CoverPicks.byLength(listOf(dune), 238_600, "DUNE", listOf("Hachi")))
        assertEquals(emptyList<String>(), CoverPicks.byLength(listOf(dune.copy(credit = listOf(BrainzCredit(name = "Hachi")))), 238_600, "DUNE", listOf("Hachi")))
    }

    @Test
    fun `songs without an album prefer the single`() {
        val picks = CoverPicks.songs(
            listOf(recording("Collapsing", "comp" to "Hits 2018", "single" to "Collapsing")),
            title = "Collapsing",
            album = null,
            credits = shown,
        )
        assertEquals(listOf("single", "comp"), picks)
    }

    @Test
    fun `songs with another title or a weak score are ignored`() {
        val picks = CoverPicks.songs(
            listOf(
                recording("Collapsing (Orchestral Version)", "orch" to "Collapsing (Orchestral Version)"),
                recording("Collapsing", "weak" to "Collapsing", score = 50),
            ),
            title = "Collapsing",
            album = null,
            credits = shown,
        )
        assertEquals(emptyList<String>(), picks)
    }

    @Test
    fun `featured credits in MusicBrainz titles still match`() {
        val picks = CoverPicks.songs(
            listOf(recording("グランドエスケープ (feat. 三浦透子)", "ost" to "天気の子")),
            title = "グランドエスケープ",
            album = "天気の子",
            credits = shown,
        )
        assertEquals(listOf("ost"), picks)
    }

    @Test
    fun `only album names that can hold a composer are searched that way`() {
        assertEquals(true, CoverPicks.mayNameComposer("Shostakovich: the Jazz Album"))
        assertEquals(true, CoverPicks.mayNameComposer("Bach - Goldberg Variations"))
        assertEquals(false, CoverPicks.mayNameComposer("Cover Up Tattoo EP"))
        assertEquals(false, CoverPicks.mayNameComposer("Anthem Emo, Pt. 1"))
    }

    @Test
    fun `composer form needs the credit inside the album name`() {
        val jazz = group("jazz", "The Jazz Album", credit = "Shostakovich")
        assertEquals(listOf("jazz"), CoverPicks.composerForm(listOf(jazz), "Shostakovich: the Jazz Album"))

        val dune = group("dune", "Dune", credit = "Klaus Schulze")
        assertEquals(emptyList<String>(), CoverPicks.composerForm(listOf(dune), "DUNE"))
        val weak = group("weak", "The Jazz Album", score = 70, credit = "Shostakovich")
        assertEquals(emptyList<String>(), CoverPicks.composerForm(listOf(weak), "Shostakovich: the Jazz Album"))
    }

    @Test
    fun `artists match on name, alias or sort name exactly`() {
        val hachi = BrainzArtist(id = "8e893bac", name = "ハチ", sortName = "Hachi", aliases = listOf(BrainzAlias("hachi")))
        val upper = BrainzArtist(id = "60cc2a61", name = "HACHI", sortName = "HACHI")
        val near = BrainzArtist(id = "c2929a43", name = "Hachioji", sortName = "Hachioji")
        assertEquals(listOf("8e893bac", "60cc2a61"), CoverPicks.artistIds(listOf(hachi, upper, near), listOf("Hachi")))
    }

    @Test
    fun `an artist known by another name may hold the album elsewhere`() {
        val hachi = BrainzArtist(id = "8e893bac", name = "ハチ", sortName = "Hachi")
        val hangs = BrainzArtist(id = "744", name = "Good Hangs", sortName = "Good Hangs")
        assertEquals(true, CoverPicks.knownByOtherName(listOf(hachi), listOf("Hachi")))
        assertEquals(false, CoverPicks.knownByOtherName(listOf(hangs), listOf("Good Hangs")))
        assertEquals(false, CoverPicks.knownByOtherName(emptyList(), listOf("Good Hangs")))
    }

    @Test
    fun `length match needs exactly one song within a second`() {
        val dune = recording("砂の惑星", "single" to "砂の惑星", length = 239_000)
        val umbrella = recording("WORLD'S END UMBRELLA", "game" to "Project DIVA", length = 238_093)
        val far = recording("Other", "far" to "Far", length = 250_000)

        assertEquals(listOf("single"), CoverPicks.byLength(listOf(dune, far), 238_600, null, shown))
        assertEquals(emptyList<String>(), CoverPicks.byLength(listOf(dune, umbrella), 238_600, null, shown))
        assertEquals(emptyList<String>(), CoverPicks.byLength(listOf(far), 238_600, null, shown))
    }
}
