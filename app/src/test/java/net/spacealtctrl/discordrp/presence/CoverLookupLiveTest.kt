package net.spacealtctrl.discordrp.presence

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.spacealtctrl.discordrp.log.SilentLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class CoverLookupLiveTest {
    @Before
    fun onlyWhenAsked() = assumeTrue(System.getenv("DISCORDRP_LIVE_COVERS") == "1")

    private val lookup = CoverLookup(
        HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
        },
        SilentLog,
    )

    private suspend fun assertFound(request: CoverRequest) {
        val result = lookup.cover(request)
        assertTrue("$request -> $result", result is CoverResult.Found)
    }

    @Test
    fun `YouTube Music video without an album`() = runBlocking {
        assertFound(CoverRequest(artist = "Dead by April", title = "Collapsing"))
    }

    @Test
    fun `album with a featured artist in the title`() = runBlocking {
        assertFound(CoverRequest(artist = "RADWIMPS", album = "天気の子", title = "グランドエスケープ (feat. 三浦透子)"))
    }

    @Test
    fun `classical album with localized performers`() = runBlocking {
        assertFound(
            CoverRequest(
                artist = "皇家音乐厅管弦乐团 & 里卡多・夏伊",
                album = "Shostakovich: the Jazz Album",
                title = "Jazz Suite No. 2: 6. Waltz II",
                durationMillis = 225_000,
            ),
        )
    }

    @Test
    fun `apple music single naming`() = runBlocking {
        assertFound(CoverRequest(artist = "Neuro-sama", album = "Colorful Array - Single", title = "Colorful Array"))
    }

    @Test
    fun `duet credited as one string`() = runBlocking {
        assertFound(CoverRequest(artist = "Jamie T & Leif Vollebekk", album = "Ghosts (100 Days of Morning)", title = "Tallahassee"))
    }

    @Test
    fun `cover added with DiscordRP is found and its release is the upload target`() = runBlocking {
        assertFound(CoverRequest(artist = "Good Hangs", album = "Anthem Emo, Pt. 1 - Single", title = "Anthem Emo"))
        assertEquals("545f7071-1fee-485d-8e0a-9f036708c3f2", lookup.uploadRelease("87bfd05d-7895-41ae-a1df-4773e9610033"))
    }

    @Test
    fun `EP missing from MusicBrainz takes no cover from a b-side and asks to be added`() = runBlocking {
        val result = lookup.cover(
            CoverRequest(
                artist = "Good Hangs",
                album = "Cover Up Tattoo EP",
                title = "Cover Up Tattoo (EP Version)",
                durationMillis = 124_548,
            ),
        )
        assertEquals(CoverResult.Missing(groupId = null, albumMissing = true), result)
    }

    @Test
    fun `existing artist and release group are found for the editor`() = runBlocking {
        assertEquals("60dcd6fa-c279-40de-894e-431337409905", lookup.artistFor("WSTR"))
        assertEquals("87bfd05d-7895-41ae-a1df-4773e9610033", lookup.groupOfRelease("545f7071-1fee-485d-8e0a-9f036708c3f2"))
    }

    @Test
    fun `english names for a japanese release never ask to add a duplicate album`() = runBlocking {
        val result = lookup.cover(
            CoverRequest(
                artist = "Hachi",
                album = "DUNE (feat. Hatsune Miku) - Single",
                title = "DUNE (feat. Hatsune Miku)",
                durationMillis = 238_000,
            ),
        )
        assertFalse("$result", result is CoverResult.Missing && result.albumMissing)
    }
}
