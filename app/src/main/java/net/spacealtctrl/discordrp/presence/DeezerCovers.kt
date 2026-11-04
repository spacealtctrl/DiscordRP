package net.spacealtctrl.discordrp.presence

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.spacealtctrl.discordrp.log.AppLog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class DeezerCovers @Inject constructor(
    private val http: HttpClient,
    private val log: AppLog,
) {
    suspend fun cover(request: CoverRequest): CoverResult {
        val album = CoverNames.album(request.album)
        val title = CoverNames.title(request.title)
        val credits = CoverNames.credits(request.albumArtist, request.artist)
        if ((album == null && title == null) || credits.isEmpty()) return CoverResult.Missing()
        val lead = credits.first()

        var unsure = false
        if (album != null) {
            val albums = search<DeezerAlbumSearch>("search/album", "$lead $album")
            if (albums == null) {
                unsure = true
            } else {
                DeezerPicks.album(albums.data, album, credits)?.let { return CoverResult.Found(it.cover, it.albumId) }
            }
        }
        if (title != null) {
            val tracks = search<DeezerTrackSearch>("search", "$lead $title")
            if (tracks == null) {
                unsure = true
            } else {
                DeezerPicks.track(tracks.data, title, album, credits)?.let { return CoverResult.Found(it.cover, it.albumId) }
            }
        }
        if (unsure) return CoverResult.Unavailable
        log.warn(TAG, "No Deezer cover for \"${album ?: title}\"")
        return CoverResult.Missing()
    }

    internal suspend fun albumDetails(albumId: Long): Pair<DeezerAlbumDetail, List<DeezerAlbumTrack>>? {
        if (albumId <= 0) return null
        val album = fetch<DeezerAlbumDetail>("album/$albumId", emptyList()) ?: return null
        val tracks = fetch<DeezerTrackList>("album/$albumId/tracks", listOf("limit" to TRACK_LIMIT.toString()))
            ?: return null
        return album to tracks.data
    }

    suspend fun image(imageUrl: String): ByteArray? {
        if (!DeezerPicks.isDeezerImage(imageUrl)) return null
        return withContext(Dispatchers.IO) {
            val connection = try {
                URL(imageUrl).openConnection() as HttpsURLConnection
            } catch (e: IOException) {
                log.warn(TAG, "Deezer image download failed: ${e::class.java.simpleName}")
                return@withContext null
            }
            try {
                connection.connectTimeout = TIMEOUT_MS.toInt()
                connection.readTimeout = TIMEOUT_MS.toInt()
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", AGENT)
                if (connection.responseCode !in 200..299) return@withContext null
                connection.inputStream.use(::readCapped)
            } catch (e: IOException) {
                log.warn(TAG, "Deezer image download failed: ${e::class.java.simpleName}")
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun readCapped(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(IMAGE_CHUNK_BYTES)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_IMAGE_BYTES) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray().takeIf { it.isNotEmpty() }
    }

    private suspend inline fun <reified T : DeezerReply> search(path: String, query: String): T? =
        fetch(path, listOf("q" to query, "limit" to LIMIT.toString()))

    private suspend inline fun <reified T : DeezerReply> fetch(path: String, params: List<Pair<String, String>>): T? {
        repeat(ATTEMPTS) { attempt ->
            val reply: T? = try {
                val response = http.get {
                    url("$DEEZER/$path")
                    header("User-Agent", AGENT)
                    params.forEach { (name, value) -> url.parameters.append(name, value) }
                    timeout { requestTimeoutMillis = TIMEOUT_MS }
                }
                if (response.status.value in 200..299) {
                    response.body<T>().takeIf { it.error == null }
                        ?: null.also { log.warn(TAG, "Deezer refused the search") }
                } else {
                    log.warn(TAG, "Deezer answered ${response.status.value}")
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn(TAG, "Deezer request failed: ${e::class.java.simpleName}")
                null
            }
            if (reply != null) return reply
            if (attempt < ATTEMPTS - 1) delay(RETRY_MS)
        }
        return null
    }

    private companion object {
        const val TAG = "DeezerCovers"
        const val DEEZER = "https://api.deezer.com"
        const val AGENT = "DiscordRP/2.0 ( https://github.com/spacealtctrl/DiscordRP )"
        const val LIMIT = 10
        const val TRACK_LIMIT = 200
        const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
        const val IMAGE_CHUNK_BYTES = 16 * 1024
        const val ATTEMPTS = 2
        const val RETRY_MS = 2_000L
        const val TIMEOUT_MS = 15_000L
    }
}
