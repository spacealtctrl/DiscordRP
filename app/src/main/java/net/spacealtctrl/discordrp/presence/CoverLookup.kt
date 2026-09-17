package net.spacealtctrl.discordrp.presence

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
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

sealed interface CoverResult {
    data class Found(val url: String) : CoverResult

    data object Missing : CoverResult

    data object Unavailable : CoverResult
}

@Singleton
class CoverLookup @Inject constructor(
    private val http: HttpClient,
    private val log: AppLog,
) {
    private val brainzGate = Mutex()

    @Volatile
    private var lastBrainzCall = 0L

    suspend fun cover(artist: String?, album: String?): CoverResult {
        val title = tidy(album) ?: return CoverResult.Missing
        val candidates = when (val search = releaseGroups(tidy(artist), title)) {
            null -> return CoverResult.Unavailable
            else -> search
        }
        if (candidates.isEmpty()) {
            log.warn(TAG, "No release group for \"$title\"")
            return CoverResult.Missing
        }
        var unsure = false
        for (groupId in candidates) {
            when (hasFront(groupId)) {
                true -> return CoverResult.Found(frontUrl(groupId))
                false -> Unit
                null -> unsure = true
            }
        }
        if (unsure) return CoverResult.Unavailable
        log.warn(TAG, "No cover art on file for \"$title\"")
        return CoverResult.Missing
    }

    private suspend fun releaseGroups(artist: String?, album: String): List<String>? {
        val query = buildString {
            append("releasegroup:\"").append(album).append('"')
            if (!artist.isNullOrBlank()) append(" AND artist:\"").append(artist).append('"')
        }
        repeat(BRAINZ_ATTEMPTS) { attempt ->
            val found = try {
                politeBrainzPause()
                val response = http.get {
                    url("$MUSICBRAINZ/release-group/")
                    header("User-Agent", AGENT)
                    url.parameters.append("query", query)
                    url.parameters.append("fmt", "json")
                    url.parameters.append("limit", "5")
                }
                if (response.isSuccess()) {
                    response.body<BrainzSearch>().groups
                        .filter { it.score >= MIN_SCORE && it.id != null }
                        .sortedByDescending { it.score }
                        .mapNotNull { it.id }
                } else {
                    log.warn(TAG, "MusicBrainz answered ${response.status.value}")
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn(TAG, "MusicBrainz request failed: ${e::class.java.simpleName}")
                null
            }
            if (found != null) return found
            if (attempt < BRAINZ_ATTEMPTS - 1) delay(BRAINZ_RETRY_MS + attempt * BRAINZ_RETRY_STEP_MS)
        }
        log.warn(TAG, "MusicBrainz stayed unavailable for \"$album\"")
        return null
    }

    private suspend fun hasFront(groupId: String): Boolean? = try {
        val response = http.head {
            url(frontUrl(groupId))
            header("User-Agent", AGENT)
        }
        when {
            response.isSuccess() -> true
            response.status.value == 404 -> false
            else -> null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn(TAG, "Cover Art Archive check failed: ${e::class.java.simpleName}")
        null
    }

    private suspend fun politeBrainzPause() = brainzGate.withLock {
        val since = System.currentTimeMillis() - lastBrainzCall
        if (since in 0 until BRAINZ_INTERVAL_MS) delay(BRAINZ_INTERVAL_MS - since)
        lastBrainzCall = System.currentTimeMillis()
    }

    private fun HttpResponse.isSuccess(): Boolean = status.value in 200..299

    private fun frontUrl(groupId: String): String =
        "$COVER_ART_ARCHIVE/release-group/$groupId/front-500"

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
        const val BRAINZ_ATTEMPTS = 6
        const val BRAINZ_RETRY_MS = 1_500L
        const val BRAINZ_RETRY_STEP_MS = 500L
        const val MIN_SCORE = 70

        val EDITION_SUFFIX = Regex("""[\[(][^\[\]()]*\b(edition|deluxe|remaster\w*|expanded|anniversary|bonus|explicit|version|reissue)\b[^\[\]()]*[])]""", RegexOption.IGNORE_CASE)
        val FORMAT_SUFFIX = Regex("""\s+-\s+(single|ep)$""", RegexOption.IGNORE_CASE)
        val QUOTES = Regex("""["\\]""")
        val WHITESPACE = Regex("""\s+""")
    }
}
