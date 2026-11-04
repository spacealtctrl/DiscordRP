package net.spacealtctrl.discordrp.presence

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.spacealtctrl.discordrp.log.AppLog
import javax.inject.Inject
import javax.inject.Singleton

data class CoverRequest(
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
    val title: String? = null,
    val durationMillis: Long? = null,
)

sealed interface CoverResult {
    data class Found(val url: String, val deezerAlbumId: Long? = null) : CoverResult

    data class Missing(val groupId: String? = null, val albumMissing: Boolean = false) : CoverResult

    data object Unavailable : CoverResult
}

private data class Candidates(
    val groupIds: List<String>,
    val uploadTarget: String? = null,
    val mayBeElsewhere: Boolean = false,
)

private typealias GroupSearch = suspend () -> Candidates?

@Singleton
class CoverLookup @Inject constructor(
    private val http: HttpClient,
    private val log: AppLog,
) {
    private val brainzGate = Mutex()
    private val artCheck: HttpClient by lazy { http.config { followRedirects = false } }

    @Volatile
    private var lastBrainzCall = 0L

    suspend fun cover(request: CoverRequest): CoverResult {
        val album = CoverNames.album(request.album)
        val title = CoverNames.title(request.title)
        if (album == null && title == null) return CoverResult.Missing()
        val credits = CoverNames.credits(request.albumArtist, request.artist)

        var unsure = false
        var uploadTarget: String? = null
        var albumSeen = false
        for (search in searches(album, title, credits, request.durationMillis)) {
            val candidates = search() ?: return CoverResult.Unavailable
            uploadTarget = uploadTarget ?: candidates.uploadTarget
            albumSeen = albumSeen || candidates.groupIds.isNotEmpty() || candidates.mayBeElsewhere
            when (val check = firstFront(candidates.groupIds)) {
                is FrontCheck.Found -> return CoverResult.Found(frontUrl(check.groupId))
                FrontCheck.Unsure -> unsure = true
                FrontCheck.None -> Unit
            }
        }
        if (unsure) return CoverResult.Unavailable
        log.warn(TAG, "No cover art on file for \"${album ?: title}\"")
        return CoverResult.Missing(
            groupId = uploadTarget?.takeIf(CoverQueries::isMbid),
            albumMissing = album != null && !albumSeen,
        )
    }

    suspend fun frontFor(groupId: String): CoverResult {
        if (!CoverQueries.isMbid(groupId)) return CoverResult.Missing()
        return when (hasFront(groupId)) {
            true -> CoverResult.Found(frontUrl(groupId))
            false -> CoverResult.Missing(groupId)
            null -> CoverResult.Unavailable
        }
    }

    suspend fun artistFor(name: String): String? {
        val credit = CoverNames.credits(name).firstOrNull() ?: return null
        val found = brainz("artist", CoverQueries.artists(listOf(credit)), BrainzArtistSearch()) ?: return null
        return CoverPicks.artistIds(found.artists, listOf(credit)).singleOrNull()
    }

    suspend fun groupOfRelease(releaseId: String): String? {
        if (!CoverQueries.isMbid(releaseId)) return null
        val params = listOf("inc" to "release-groups")
        val release = brainzGet("release/$releaseId", params, BrainzReleaseLookup()) ?: return null
        return release.group?.id?.takeIf(CoverQueries::isMbid)
    }

    suspend fun uploadRelease(groupId: String): String? {
        if (!CoverQueries.isMbid(groupId)) return null
        val params = listOf("release-group" to groupId, "inc" to "media", "limit" to RELEASE_LIMIT.toString())
        val browse = brainzGet("release", params, BrainzReleaseBrowse()) ?: return null
        return ReleasePick.pick(browse.releases)
    }

    private fun searches(
        album: String?,
        title: String?,
        credits: List<String>,
        durationMillis: Long?,
    ): List<GroupSearch> = buildList {
        if (album != null) add { albumGroups(album, credits) }
        if (title != null && credits.isNotEmpty()) add { songGroups(title, album, credits) }
        if (album != null && CoverPicks.mayNameComposer(album)) add { composerGroups(album) }
        if (credits.isNotEmpty()) add { aliasGroups(title, album, credits, durationMillis) }
    }

    private suspend fun albumGroups(album: String, credits: List<String>): Candidates? =
        brainz("release-group", CoverQueries.album(album, credits), BrainzGroupSearch())
            ?.let { Candidates(CoverPicks.groups(it.groups, album), CoverPicks.exactGroup(it.groups, album)) }

    private suspend fun songGroups(title: String, album: String?, credits: List<String>): Candidates? =
        brainz("recording", CoverQueries.song(title, credits), BrainzRecordingSearch())
            ?.let { Candidates(CoverPicks.songs(it.recordings, title, album, credits)) }

    private suspend fun composerGroups(album: String): Candidates? {
        val text = CoverQueries.freeText(album).takeIf { it.isNotBlank() } ?: return Candidates(emptyList())
        return brainz("release-group", text, BrainzGroupSearch())
            ?.let { CoverPicks.composerForm(it.groups, album) }
            ?.let { Candidates(it, it.firstOrNull()) }
    }

    private suspend fun aliasGroups(
        title: String?,
        album: String?,
        credits: List<String>,
        durationMillis: Long?,
    ): Candidates? {
        val found = brainz("artist", CoverQueries.artists(credits), BrainzArtistSearch()) ?: return null
        val ids = CoverPicks.artistIds(found.artists, credits)
        val none = Candidates(emptyList(), mayBeElsewhere = CoverPicks.knownByOtherName(found.artists, credits))
        if (!none.mayBeElsewhere) return none
        val byTitle = title?.let { CoverQueries.songBy(it, ids) }
        if (title != null && byTitle != null) {
            val songs = brainz("recording", byTitle, BrainzRecordingSearch()) ?: return null
            CoverPicks.songs(songs.recordings, title, album, credits).takeIf { it.isNotEmpty() }?.let { return Candidates(it) }
        }
        val length = durationMillis?.takeIf { it > 0 } ?: return none
        val byLength = CoverQueries.lengthBy(length, ids) ?: return none
        return brainz("recording", byLength, BrainzRecordingSearch())
            ?.let { none.copy(groupIds = CoverPicks.byLength(it.recordings, length, album, credits)) }
    }

    private suspend fun firstFront(groups: List<String>): FrontCheck {
        var unsure = false
        for (groupId in groups.filter(CoverQueries::isMbid).take(MAX_GROUP_CHECKS)) {
            when (hasFront(groupId)) {
                true -> return FrontCheck.Found(groupId)
                null -> unsure = true
                false -> Unit
            }
        }
        return if (unsure) FrontCheck.Unsure else FrontCheck.None
    }

    private suspend fun hasFront(groupId: String): Boolean? = try {
        val response = artCheck.head {
            url(frontUrl(groupId))
            header("User-Agent", AGENT)
            timeout { requestTimeoutMillis = ART_CHECK_TIMEOUT_MS }
        }
        when (response.status.value) {
            in 200..399 -> true
            404 -> false
            else -> null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn(TAG, "Cover Art Archive check failed: ${e::class.java.simpleName}")
        null
    }

    private suspend inline fun <reified T> brainz(entity: String, query: String, empty: T): T? =
        brainzGet("$entity/", listOf("query" to query, "limit" to BRAINZ_LIMIT.toString()), empty)

    private suspend inline fun <reified T> brainzGet(
        path: String,
        params: List<Pair<String, String>>,
        empty: T,
    ): T? {
        repeat(BRAINZ_ATTEMPTS) { attempt ->
            val found: T? = try {
                politeBrainzPause()
                val response = http.get {
                    url("$MUSICBRAINZ/$path")
                    header("User-Agent", AGENT)
                    params.forEach { (name, value) -> url.parameters.append(name, value) }
                    url.parameters.append("fmt", "json")
                }
                when (val status = response.status.value) {
                    in 200..299 -> response.body<T>()
                    400, 404 -> return empty
                    else -> {
                        log.warn(TAG, "MusicBrainz answered $status")
                        null
                    }
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
        log.warn(TAG, "MusicBrainz stayed unavailable for $path")
        return null
    }

    private suspend fun politeBrainzPause() = brainzGate.withLock {
        val since = System.currentTimeMillis() - lastBrainzCall
        if (since in 0 until BRAINZ_INTERVAL_MS) delay(BRAINZ_INTERVAL_MS - since)
        lastBrainzCall = System.currentTimeMillis()
    }

    private fun frontUrl(groupId: String): String =
        "$COVER_ART_ARCHIVE/release-group/$groupId/front-500"

    private sealed interface FrontCheck {
        data class Found(val groupId: String) : FrontCheck

        data object Unsure : FrontCheck

        data object None : FrontCheck
    }

    private companion object {
        const val TAG = "CoverLookup"
        const val MUSICBRAINZ = "https://musicbrainz.org/ws/2"
        const val COVER_ART_ARCHIVE = "https://coverartarchive.org"
        const val AGENT = "DiscordRP/2.0 ( https://github.com/spacealtctrl/DiscordRP )"

        const val BRAINZ_INTERVAL_MS = 1_100L
        const val BRAINZ_ATTEMPTS = 6
        const val BRAINZ_RETRY_MS = 1_500L
        const val BRAINZ_RETRY_STEP_MS = 500L
        const val BRAINZ_LIMIT = 10
        const val RELEASE_LIMIT = 25
        const val MAX_GROUP_CHECKS = 6
        const val ART_CHECK_TIMEOUT_MS = 10_000L
    }
}
