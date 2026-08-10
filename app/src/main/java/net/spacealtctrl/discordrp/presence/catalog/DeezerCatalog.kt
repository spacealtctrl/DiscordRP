package net.spacealtctrl.discordrp.presence.catalog

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.presence.tidy.Candidate
import net.spacealtctrl.discordrp.presence.tidy.CandidateRelease
import net.spacealtctrl.discordrp.presence.tidy.MatchRank
import net.spacealtctrl.discordrp.presence.tidy.TitleScrub
import net.spacealtctrl.discordrp.presence.tidy.TrackFacts
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class DeezerSearch(
    val data: List<DeezerTrack> = emptyList(),
)

@Serializable
internal data class DeezerTrack(
    val title: String? = null,
    @SerialName("title_short") val titleShort: String? = null,
    val duration: Long = 0,
    val artist: DeezerArtist? = null,
    val album: DeezerAlbum? = null,
)

@Serializable
internal data class DeezerArtist(val name: String? = null)

@Serializable
internal data class DeezerAlbum(
    val title: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
)

@Singleton
class DeezerCatalog @Inject constructor(
    private val http: HttpClient,
    private val gate: RateGate,
    private val log: AppLog,
) : MetadataCatalog {
    override val label = "Deezer"

    private data class Rung(val query: String, val titleForScoring: String)

    override suspend fun lookup(ask: LookupAsk): TrackFacts? {
        for (rung in ladder(ask)) {
            val tracks = search(rung.query) ?: continue
            if (tracks.isEmpty()) continue
            val facts = MatchRank.resolve(
                candidates = tracks.mapNotNull { it.toCandidate() },
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
        val artist = ask.artist?.takeIf { it.isNotBlank() }
        val bare = TitleScrub.stripTrailingBracket(ask.title)
        val shortened = !bare.equals(ask.title, ignoreCase = true)
        val rungs = mutableListOf<Rung>()
        if (artist != null) {
            rungs += Rung("artist:${phrase(artist)} track:${phrase(ask.title)}", ask.title)
            if (shortened) {
                rungs += Rung("artist:${phrase(artist)} track:${phrase(bare)}", bare)
            }
            rungs += Rung("$artist ${ask.title}", ask.title)
            if (shortened) {
                rungs += Rung("$artist $bare", bare)
            }
        }
        rungs += Rung(ask.title, ask.title)
        if (shortened) {
            rungs += Rung(bare, bare)
        }
        return rungs
    }

    private suspend fun search(query: String): List<DeezerTrack>? = try {
        gate.await(RateGate.DEEZER, RateGate.DEEZER_INTERVAL_MS)
        val response: DeezerSearch = http.get {
            url(SEARCH_URL)
            url.parameters.append("q", query)
            url.parameters.append("limit", RESULT_LIMIT.toString())
        }.body()
        response.data
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        log.warn(TAG, "Track search failed: ${failure::class.java.simpleName}")
        null
    }

    private fun phrase(value: String): String = "\"" + value.replace("\"", " ").trim() + "\""

    private fun DeezerTrack.toCandidate(): Candidate? {
        val name = title?.takeIf { it.isNotBlank() }
            ?: titleShort?.takeIf { it.isNotBlank() }
            ?: return null
        val albumName = album?.title?.takeIf { it.isNotBlank() }
        return Candidate(
            title = name,
            artist = artist?.name.orEmpty(),
            relevance = 100,
            durationMs = duration.takeIf { it > 0 }?.times(1_000),
            releases = listOfNotNull(
                albumName?.let {
                    CandidateRelease(
                        title = it,
                        primaryType = "Album",
                        status = "Official",
                        coverUrl = album?.coverXl ?: album?.coverBig,
                    )
                },
            ),
        )
    }

    private companion object {
        const val TAG = "DeezerCatalog"
        const val SEARCH_URL = "https://api.deezer.com/search"
        const val RESULT_LIMIT = 10
    }
}
