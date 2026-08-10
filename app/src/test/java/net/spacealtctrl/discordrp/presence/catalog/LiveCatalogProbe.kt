package net.spacealtctrl.discordrp.presence.catalog

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.presence.tidy.AlbumQuality
import net.spacealtctrl.discordrp.presence.tidy.TitleScrub
import org.junit.Ignore
import org.junit.Test

@Ignore("hits the live MusicBrainz and Deezer services")
class LiveCatalogProbe {
    private val printer = object : AppLog {
        override fun debug(tag: String, message: String) = println("  D $tag $message")
        override fun info(tag: String, message: String) = println("  I $tag $message")
        override fun warn(tag: String, message: String) = println("  W $tag $message")
        override fun error(tag: String, message: String) = println("  E $tag $message")
    }

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

    private val gate = RateGate()
    private val brainz = BrainzCatalog(http, gate, printer)
    private val deezer = DeezerCatalog(http, gate, printer)

    private data class Case(
        val name: String,
        val title: String,
        val artist: String?,
        val album: String?,
        val durationMs: Long?,
        val expectMatch: Boolean,
    )

    @Test
    fun `report what the catalogues answer`() = runBlocking {
        val cases = listOf(
            Case(
                "the reported NewPipe failure",
                "The Weeknd - Blinding Lights (Official Video)",
                "TheWeekndVEVO", "NewPipe", 262_000, true,
            ),
            Case("topic channel", "Karma Police", "Radiohead - Topic", "OK Computer", 264_000, true),
            Case("topic channel, junk album", "Karma Police", "Radiohead - Topic", "NewPipe", 264_000, true),
            Case(
                "lyric video with a feature",
                "The Kid LAROI, Justin Bieber - STAY (Official Video)",
                "TheKidLAROIVEVO", "NewPipe", 141_000, true,
            ),
            Case(
                "japanese music video",
                "YOASOBI「アイドル」Official Music Video",
                "Ayase / YOASOBI", "NewPipe", 214_000, true,
            ),
            Case("a real remix", "Avicii - Levels (Skrillex Remix)", "AviciiOfficialVEVO", "NewPipe", 330_000, true),
            Case(
                "a live performance",
                "Radiohead - Creep (Live at Glastonbury 2003)",
                "RadioheadTV", "NewPipe", 290_000, true,
            ),
            Case("a vlog", "I bought a \$1 house and this happened", "SomeVlogger", "NewPipe", 1_320_000, false),
            Case("a tech talk", "Rust vs Go in 2025 - which should you learn?", "CodeChannel", "NewPipe", 980_000, false),
        )

        var wrong = 0
        for (case in cases) {
            val scrubbed = TitleScrub.scrub(case.title, case.artist, case.album, "NewPipe")
            val ask = LookupAsk(scrubbed.title, scrubbed.artist, scrubbed.album, case.durationMs)
            println("\n=== ${case.name}")
            println("  scrubbed: title=${scrubbed.title} artist=${scrubbed.artist} album=${scrubbed.album}")

            val open = if (scrubbed.worthLookup) brainz.lookup(ask) else null
            val fallback =
                if (scrubbed.worthLookup && open?.albumQuality != AlbumQuality.CLEAN) {
                    deezer.lookup(ask)
                } else {
                    null
                }
            val chosen = open ?: fallback

            println("  musicbrainz: ${open?.let { "${it.title} / ${it.artist} / ${it.album} [${it.albumQuality}]" }}")
            println("  deezer:      ${fallback?.let { "${it.title} / ${it.artist} / ${it.album}" }}")

            val matched = chosen != null
            if (matched != case.expectMatch) {
                wrong++
                println("  MISMATCH: expected match=${case.expectMatch}, got $matched")
            }
        }
        println("\nmismatches: $wrong")
    }
}
