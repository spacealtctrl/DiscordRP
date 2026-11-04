package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverUrlsTest {
    private val group = "https://coverartarchive.org/release-group/87bfd05d-7895-41ae-a1df-4773e9610033"
    private val release = "https://coverartarchive.org/release/545f7071-1fee-485d-8e0a-9f036708c3f2"

    @Test
    fun `a sized archive cover falls back to the original`() {
        assertEquals(listOf("$group/front-500", "$group/front"), CoverUrls.displayCandidates("$group/front-500"))
        assertEquals(listOf("$release/front-250", "$release/front"), CoverUrls.displayCandidates("$release/front-250"))
    }

    @Test
    fun `other covers have no fallback`() {
        val deezer = "https://cdn-images.dzcdn.net/images/cover/4ea391102f4dd619d13d924c635cf8cd/1000x1000-000000-80-0-0.jpg"
        assertEquals(listOf(deezer), CoverUrls.displayCandidates(deezer))
        assertEquals(listOf("$group/front"), CoverUrls.displayCandidates("$group/front"))
        assertEquals(listOf("https://evil.test/front-500"), CoverUrls.displayCandidates("https://evil.test/front-500"))
    }

    @Test
    fun `discord thumbnails are asked for small`() {
        assertEquals(
            "https://media.discordapp.net/external/abc/https/coverartarchive.org/x/front?width=64&height=64",
            DiscordMedia.thumbnail("mp:external/abc/https/coverartarchive.org/x/front"),
        )
        assertNull(DiscordMedia.thumbnail("not-a-media-ref"))
    }
}
