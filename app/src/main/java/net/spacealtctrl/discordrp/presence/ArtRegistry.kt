package net.spacealtctrl.discordrp.presence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.DisplayMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import net.spacealtctrl.discordrp.discord.ApiRefusal
import net.spacealtctrl.discordrp.discord.DiscordApi
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.settings.Stash
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ArtSource {
    data class Remote(val url: String, val cacheKey: String) : ArtSource

    data class Cover(
        val cacheKey: String,
        val artist: String? = null,
        val albumArtist: String? = null,
        val album: String? = null,
        val title: String? = null,
        val durationMillis: Long? = null,
    ) : ArtSource

    data class AppIcon(val packageName: String) : ArtSource
}

@Singleton
class ArtRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DiscordApi,
    private val coverLookup: CoverLookup,
    private val deezerCovers: DeezerCovers,
    private val stash: Stash,
    private val log: AppLog,
) {
    private val unavailableAt = ConcurrentHashMap<String, Long>()
    private val unavailableStreak = ConcurrentHashMap<String, Int>()
    private val uploadCheckedAt = ConcurrentHashMap<String, Long>()
    private val coverGeneration = AtomicInteger()

    suspend fun resolve(source: ArtSource?): String? = when (source) {
        null -> null
        is ArtSource.Remote -> register(source.url, source.cacheKey, coverGeneration.get())
        is ArtSource.Cover -> resolveCover(source)
        is ArtSource.AppIcon -> resolveAppIcon(source)
    }

    private suspend fun resolveCover(cover: ArtSource.Cover): String? {
        val generation = coverGeneration.get()
        val cached = stash.coverAssets[cover.cacheKey]
        if (!stash.powerMode.fetchesRemoteArt) return cached
        uploadedGroup(cover.cacheKey)?.let { group ->
            checkUpload(cover.cacheKey, group, generation)?.let { return it }
        }
        if (cached != null) return cached
        if (unavailableIsFresh(cover.cacheKey)) return null
        val request = CoverRequest(
            artist = cover.artist,
            albumArtist = cover.albumArtist,
            album = cover.album,
            title = cover.title,
            durationMillis = cover.durationMillis,
        )
        if (missIsFresh(cover.cacheKey)) {
            if (deezerMissIsFresh(cover.cacheKey)) return null
            return backupFor(request, cover.cacheKey, cover.cacheKey in stash.coverAlbumMisses, generation)
        }
        val result = coverLookup.cover(request)
        if (isStale(generation)) return null
        return when (result) {
            is CoverResult.Found ->
                register(result.url, cover.cacheKey, generation)?.also { forgetMiss(cover.cacheKey) }
                    ?: markUnavailable(cover.cacheKey, generation)
            is CoverResult.Missing -> {
                rememberMiss(cover.cacheKey, result)
                backupFor(request, cover.cacheKey, result.albumMissing, generation)
            }
            CoverResult.Unavailable -> markUnavailable(cover.cacheKey, generation)
        }
    }

    private suspend fun backupFor(
        request: CoverRequest,
        cacheKey: String,
        albumMissing: Boolean,
        generation: Int,
    ): String? {
        if (!stash.coverSource.deezerBackup) return null
        val backup = deezerCovers.cover(request)
        if (isStale(generation)) return null
        return when (backup) {
            is CoverResult.Found -> {
                val reference = store(backup.url, cacheKey, generation, verify = false)
                    ?: return markUnavailable(cacheKey, generation)
                stash.coverDeezerAssets = stash.coverDeezerAssets + cacheKey
                backup.deezerAlbumId?.takeIf { albumMissing }?.let { album ->
                    stash.coverDeezerAlbums = stash.coverDeezerAlbums + (cacheKey to album.toString())
                }
                reference
            }
            is CoverResult.Missing -> {
                stash.coverDeezerMisses = stash.coverDeezerMisses + (cacheKey to System.currentTimeMillis())
                null
            }
            CoverResult.Unavailable -> markUnavailable(cacheKey, generation)
        }
    }

    private fun markUnavailable(cacheKey: String, generation: Int): String? {
        if (!isStale(generation)) {
            unavailableAt[cacheKey] = System.currentTimeMillis()
            unavailableStreak.merge(cacheKey, 1) { streak, failure -> streak + failure }
        }
        return null
    }

    private fun isStale(generation: Int): Boolean = coverGeneration.get() != generation

    fun coverSourceChanged() {
        coverGeneration.incrementAndGet()
        unavailableAt.clear()
        unavailableStreak.clear()
        stash.coverDeezerMisses = emptyMap()
        if (!stash.coverSource.deezerBackup) stash.dropDeezerCovers()
    }

    private fun deezerMissIsFresh(cacheKey: String, now: Long = System.currentTimeMillis()): Boolean {
        val missedAt = stash.coverDeezerMisses[cacheKey] ?: return false
        return now - missedAt in 0 until MISS_RETRY_MS
    }

    private fun unavailableIsFresh(cacheKey: String, now: Long = System.currentTimeMillis()): Boolean {
        val failedAt = unavailableAt[cacheKey] ?: return false
        return now - failedAt in 0 until UNAVAILABLE_RETRY_MS
    }

    private fun missIsFresh(cacheKey: String, now: Long = System.currentTimeMillis()): Boolean {
        val missedAt = stash.coverMisses[cacheKey] ?: return false
        return now - missedAt in 0 until MISS_RETRY_MS
    }

    fun missingGroupFor(cacheKey: String): String? =
        stash.coverMissGroups[cacheKey]?.takeIf { uploadedGroup(cacheKey) == null }

    fun deezerAlbumFor(cacheKey: String): Long? =
        stash.coverDeezerAlbums[cacheKey]?.toLongOrNull()?.takeIf { albumMissingFor(cacheKey) }

    fun albumMissingFor(cacheKey: String): Boolean =
        cacheKey in stash.coverAlbumMisses && stash.coverMissGroups[cacheKey] == null

    fun noteNewRelease(cacheKey: String, groupId: String) {
        stash.coverMissGroups = stash.coverMissGroups + (cacheKey to groupId)
        stash.coverDeezerAlbums = stash.coverDeezerAlbums - cacheKey
        stash.coverAlbumMisses = stash.coverAlbumMisses - cacheKey
    }

    fun coverPending(cacheKey: String): Boolean =
        (unavailableIsFresh(cacheKey) && unavailableStreak.getOrDefault(cacheKey, 0) < UNAVAILABLE_RETRIES) ||
            uploadedGroup(cacheKey) != null

    fun noteUpload(cacheKey: String) {
        if (stash.coverMissGroups[cacheKey] == null) return
        stash.coverUploads = stash.coverUploads + (cacheKey to System.currentTimeMillis())
        uploadCheckedAt.remove(cacheKey)
    }

    private suspend fun checkUpload(cacheKey: String, groupId: String, generation: Int): String? {
        val now = System.currentTimeMillis()
        val lastCheck = uploadCheckedAt[cacheKey]
        if (lastCheck != null && now - lastCheck in 0 until UPLOAD_RECHECK_MS) return null
        uploadCheckedAt[cacheKey] = now
        val found = coverLookup.frontFor(groupId) as? CoverResult.Found ?: return null
        val reference = store(found.url, cacheKey, generation) ?: return null
        forgetMiss(cacheKey)
        return reference
    }

    private fun uploadedGroup(cacheKey: String, now: Long = System.currentTimeMillis()): String? {
        val uploadedAt = stash.coverUploads[cacheKey] ?: return null
        if (now - uploadedAt !in 0 until UPLOAD_WATCH_MS) return null
        return stash.coverMissGroups[cacheKey]
    }

    private fun rememberMiss(cacheKey: String, miss: CoverResult.Missing) {
        stash.coverMisses = stash.coverMisses + (cacheKey to System.currentTimeMillis())
        stash.coverMissGroups = if (miss.groupId != null) {
            stash.coverMissGroups + (cacheKey to miss.groupId)
        } else {
            stash.coverMissGroups - cacheKey
        }
        stash.coverAlbumMisses = if (miss.albumMissing) {
            stash.coverAlbumMisses + cacheKey
        } else {
            stash.coverAlbumMisses - cacheKey
        }
    }

    private fun forgetMiss(cacheKey: String) {
        val tracked = cacheKey in stash.coverMisses ||
            cacheKey in stash.coverMissGroups ||
            cacheKey in stash.coverUploads ||
            cacheKey in stash.coverDeezerAlbums ||
            cacheKey in stash.coverAlbumMisses ||
            cacheKey in stash.coverDeezerAssets ||
            cacheKey in stash.coverDeezerMisses
        if (!tracked) return
        stash.coverMisses = stash.coverMisses - cacheKey
        stash.coverMissGroups = stash.coverMissGroups - cacheKey
        stash.coverUploads = stash.coverUploads - cacheKey
        stash.coverDeezerAlbums = stash.coverDeezerAlbums - cacheKey
        stash.coverAlbumMisses = stash.coverAlbumMisses - cacheKey
        stash.coverDeezerAssets = stash.coverDeezerAssets - cacheKey
        stash.coverDeezerMisses = stash.coverDeezerMisses - cacheKey
        uploadCheckedAt.remove(cacheKey)
    }

    private suspend fun resolveAppIcon(icon: ArtSource.AppIcon): String? {
        stash.appIconAssets[icon.packageName]?.let { return it.takeIf(String::isNotBlank) }
        if (!stash.powerMode.fetchesRemoteArt) return null
        val name = HostedIcons.nameFor(icon.packageName)
        val result = api.hostedAssetFor("$ICON_BASE/$name.png?v=$ICON_REVISION")
        val reference = result.getOrNull()
        when {
            reference != null ->
                stash.appIconAssets = stash.appIconAssets + (icon.packageName to reference)
            result.exceptionOrNull() is ApiRefusal -> {
                log.warn(TAG, "No hosted icon for ${icon.packageName}")
                stash.appIconAssets = stash.appIconAssets + (icon.packageName to "")
            }
        }
        return reference
    }

    private suspend fun register(url: String, cacheKey: String, generation: Int): String? {
        stash.coverAssets[cacheKey]?.let { return it }
        return store(url, cacheKey, generation)
    }

    private suspend fun store(url: String, cacheKey: String, generation: Int, verify: Boolean = true): String? {
        if (!stash.powerMode.fetchesRemoteArt) return null
        for (candidate in CoverUrls.displayCandidates(url)) {
            val reference = api.hostedAssetFor(candidate)
                .onFailure { log.warn(TAG, "Discord refused $candidate: ${it.message}") }
                .getOrNull() ?: continue
            if (verify && api.mediaShows(reference) == false) {
                log.warn(TAG, "Discord cannot show $candidate")
                continue
            }
            if (isStale(generation)) return null
            stash.coverAssets = stash.coverAssets + (cacheKey to reference)
            unavailableStreak.remove(cacheKey)
            return reference
        }
        return null
    }

    fun iconBitmap(packageName: String): Bitmap? = runCatching {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        val drawable = runCatching {
            pm.getResourcesForApplication(info)
                .getDrawableForDensity(info.icon, DisplayMetrics.DENSITY_XXXHIGH, null)
        }.getOrNull() ?: pm.getApplicationIcon(info)

        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }.getOrNull()

    private companion object {
        const val TAG = "ArtRegistry"

        const val ICON_BASE =
            "https://raw.githubusercontent.com/spacealtctrl/DiscordRP/main/assets/icons"
        const val ICON_REVISION = 2

        const val MISS_RETRY_MS = 5 * 60 * 1000L
        const val UNAVAILABLE_RETRY_MS = 2 * 60 * 1000L
        const val UNAVAILABLE_RETRIES = 3
        const val UPLOAD_RECHECK_MS = 150 * 1000L
        const val UPLOAD_WATCH_MS = 60 * 60 * 1000L
    }
}
