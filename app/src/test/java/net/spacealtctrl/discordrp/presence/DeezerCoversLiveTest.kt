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
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class DeezerCoversLiveTest {
    @Before
    fun onlyWhenAsked() = assumeTrue(System.getenv("DISCORDRP_LIVE_COVERS") == "1")

    private val deezer = DeezerCovers(
        HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) { requestTimeoutMillis = 30_000 }
        },
        SilentLog,
    )

    private suspend fun assertFound(request: CoverRequest) {
        val result = deezer.cover(request)
        assertTrue("$request -> $result", result is CoverResult.Found)
    }

    @Test
    fun `single MusicBrainz does not have`() = runBlocking {
        assertFound(CoverRequest(artist = "WSTR", album = "Footprints (Acoustic)", title = "Footprints (Acoustic)"))
    }

    @Test
    fun `album details carry what MusicBrainz needs`() = runBlocking {
        val (album, tracks) = requireNotNull(deezer.albumDetails(143466912))
        assertEquals("790692286560", album.upc)
        assertEquals(1, tracks.size)
        assertTrue((deezer.image(requireNotNull(album.coverXl))?.size ?: 0) > 10_000)
    }

    @Test
    fun `apple music single naming`() = runBlocking {
        assertFound(CoverRequest(artist = "Good Hangs", album = "Anthem Emo, Pt. 1 - Single", title = "Anthem Emo"))
    }

    @Test
    fun `YouTube Music video without an album`() = runBlocking {
        assertFound(CoverRequest(artist = "Dead by April", title = "Collapsing"))
    }

    @Test
    fun `duet credited as one string`() = runBlocking {
        assertFound(CoverRequest(artist = "Jamie T & Leif Vollebekk", album = "Ghosts (100 Days of Morning)", title = "Tallahassee"))
    }
}
