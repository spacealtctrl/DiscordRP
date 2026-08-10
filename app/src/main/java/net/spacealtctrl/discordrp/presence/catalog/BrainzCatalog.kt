package net.spacealtctrl.discordrp.presence.catalog

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import kotlinx.coroutines.CancellationException
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.presence.tidy.MatchRank
import net.spacealtctrl.discordrp.presence.tidy.TitleScrub
import net.spacealtctrl.discordrp.presence.tidy.TrackFacts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrainzCatalog @Inject constructor(
    private val http: HttpClient,
    private val gate: RateGate,
    private val log: AppLog,
) : MetadataCatalog {
    override val label = "MusicBrainz"

    private data class Rung(val query: String, val titleForScoring: String)

    override suspend fun lookup(ask: LookupAsk): TrackFacts? {
        for (rung in ladder(ask)) {
            val recordings = search(rung.query) ?: continue
            if (recordings.isEmpty()) continue
            val facts = MatchRank.resolve(
                candidates = recordings.mapNotNull { it.toCandidate() },
                wantTitle = rung.titleForScoring,
                wantArtist = ask.artist,
                wantDurationMs = ask.durationMs,
                albumHint = ask.album,
            )
            if (facts != null) return facts
        }
        return null
    }

    private fun ladder(ask: LookupAsk): List<Rung> {
        val title = ask.title
        val artist = ask.artist?.takeIf { it.isNotBlank() }
        val album = ask.album?.takeIf { it.isNotBlank() }
        val bare = TitleScrub.stripTrailingBracket(title)

        val rungs = mutableListOf<Rung>()
        if (artist != null && album != null) {
            rungs += Rung(
                "recording:${phrase(title)} AND artist:${phrase(artist)} AND release:${phrase(album)}",
                title,
            )
        }
        if (artist != null) {
            rungs += Rung("recording:${phrase(title)} AND artist:${phrase(artist)}", title)
            rungs += Rung("recording:${phrase(title)} AND artistname:${phrase(artist)}", title)
        }
        rungs += Rung("recording:${phrase(title)}", title)
        rungs += Rung(listOfNotNull(artist, title).joinToString(" "), title)
        if (!bare.equals(title, ignoreCase = true)) {
            if (artist != null) {
                rungs += Rung("recording:${phrase(bare)} AND artist:${phrase(artist)}", bare)
            }
            rungs += Rung("recording:${phrase(bare)}", bare)
        }
        return rungs
    }

    private suspend fun search(query: String): List<BrainzRecording>? = try {
        gate.await(RateGate.MUSICBRAINZ, RateGate.MUSICBRAINZ_INTERVAL_MS)
        val response: BrainzRecordingSearch = http.get {
            url("$MUSICBRAINZ/recording")
            header("User-Agent", AGENT)
            url.parameters.append("query", query)
            url.parameters.append("fmt", "json")
            url.parameters.append("limit", RESULT_LIMIT.toString())
        }.body()
        response.recordings
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        log.warn(TAG, "Recording search failed: ${failure::class.java.simpleName}")
        null
    }

    private fun phrase(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val TAG = "BrainzCatalog"
        const val MUSICBRAINZ = "https://musicbrainz.org/ws/2"
        const val AGENT = "DiscordRP/2.0 ( https://github.com/spacealtctrl/DiscordRP )"

        const val RESULT_LIMIT = 25
    }
}
