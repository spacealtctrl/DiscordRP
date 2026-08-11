package net.spacealtctrl.discordrp.presence

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.spacealtctrl.discordrp.log.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class BrainzSearch(
    @SerialName("release-groups") val groups: List<BrainzGroup> = emptyList(),
)

@Serializable
internal data class BrainzGroup(
    val id: String? = null,
    val title: String? = null,
    val score: Int = 0,
)

@Singleton
class CoverLookup @Inject constructor(
    private val http: HttpClient,
    private val log: AppLog,
) {
    private val brainzGate = Mutex()

    @Volatile
    private var lastBrainzCall = 0L

    suspend fun coverUrl(artist: String?, album: String?): String? {
        val title = tidy(album) ?: return null
        val performer = tidy(artist)
        val groupId = releaseGroupId(performer, title)
        if (groupId == null) {
            log.warn(TAG, "No release group for \"$title\"")
            return null
        }
        return "$COVER_ART_ARCHIVE/release-group/$groupId/front"
    }

    private suspend fun releaseGroupId(artist: String?, album: String?): String? {
        val query = buildString {
            append("releasegroup:\"").append(album).append('"')
            if (!artist.isNullOrBlank()) append(" AND artist:\"").append(artist).append('"')
        }
        repeat(BRAINZ_ATTEMPTS) { attempt ->
            val outcome = runCatching {
                politeBrainzPause()
                val search: BrainzSearch = http.get {
                    url("$MUSICBRAINZ/release-group/")
                    header("User-Agent", AGENT)
                    url.parameters.append("query", query)
                    url.parameters.append("fmt", "json")
                    url.parameters.append("limit", "5")
                }.body()
                search.groups.maxByOrNull { it.score }
                    ?.takeIf { it.score >= MIN_SCORE }
                    ?.id
            }
            outcome.getOrNull()?.let { return it }
            outcome.exceptionOrNull()?.let {
                if (it is CancellationException) throw it
                log.warn(TAG, "MusicBrainz attempt ${attempt + 1} failed: ${it::class.java.simpleName}")
                if (attempt < BRAINZ_ATTEMPTS - 1) delay(BRAINZ_RETRY_MS)
            } ?: return null
        }
        return null
    }

    private suspend fun politeBrainzPause() = brainzGate.withLock {
        val since = System.currentTimeMillis() - lastBrainzCall
        if (since in 0 until BRAINZ_INTERVAL_MS) delay(BRAINZ_INTERVAL_MS - since)
        lastBrainzCall = System.currentTimeMillis()
    }

    private fun tidy(value: String?): String? = value
        ?.replace(EDITION_SUFFIX, "")
        ?.replace(FORMAT_SUFFIX, "")
        ?.replace(QUOTES, " ")
        ?.replace(WHITESPACE, " ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private companion object {
        const val TAG = "CoverLookup"
        const val MUSICBRAINZ = "https://musicbrainz.org/ws/2"
        const val COVER_ART_ARCHIVE = "https://coverartarchive.org"
        const val AGENT = "DiscordRP/2.0 ( https://github.com/spacealtctrl/DiscordRP )"

        const val BRAINZ_INTERVAL_MS = 1_100L
        const val BRAINZ_ATTEMPTS = 3
        const val BRAINZ_RETRY_MS = 1_500L
        const val MIN_SCORE = 70

        val EDITION_SUFFIX = Regex("""[\[(][^\[\]()]*\b(edition|deluxe|remaster\w*|expanded|anniversary|bonus|explicit|version|reissue)\b[^\[\]()]*[])]""", RegexOption.IGNORE_CASE)
        val FORMAT_SUFFIX = Regex("""\s+-\s+(single|ep)$""", RegexOption.IGNORE_CASE)
        val QUOTES = Regex("""["\\]""")
        val WHITESPACE = Regex("""\s+""")
    }
}
