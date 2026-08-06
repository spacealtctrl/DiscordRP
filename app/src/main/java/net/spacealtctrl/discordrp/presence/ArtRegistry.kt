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
import javax.inject.Inject

sealed interface ArtSource {
    data class Remote(val url: String, val cacheKey: String) : ArtSource

    data class Cover(
        val cacheKey: String,
        val artist: String? = null,
        val album: String? = null,
    ) : ArtSource

    data class AppIcon(val packageName: String) : ArtSource
}

class ArtRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DiscordApi,
    private val coverLookup: CoverLookup,
    private val stash: Stash,
    private val log: AppLog,
) {
    suspend fun resolve(source: ArtSource?): String? = when (source) {
        null -> null
        is ArtSource.Remote -> register(source.url, source.cacheKey)
        is ArtSource.Cover -> resolveCover(source)
        is ArtSource.AppIcon -> resolveAppIcon(source)
    }

    private suspend fun resolveCover(cover: ArtSource.Cover): String? {
        stash.coverAssets[cover.cacheKey]?.let { return it }
        val url = coverLookup.coverUrl(cover.artist, cover.album) ?: return null
        return register(url, cover.cacheKey)
    }

    private suspend fun resolveAppIcon(icon: ArtSource.AppIcon): String? {
        stash.appIconAssets[icon.packageName]?.let { return it.takeIf(String::isNotBlank) }
        val result = api.hostedAssetFor("$ICON_BASE/${icon.packageName}.png")
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

    private suspend fun register(url: String, cacheKey: String): String? {
        stash.coverAssets[cacheKey]?.let { return it }
        val reference = api.hostedAssetFor(url)
            .onFailure { log.warn(TAG, "Discord refused $url: ${it.message}") }
            .getOrNull() ?: return null
        stash.coverAssets = stash.coverAssets + (cacheKey to reference)
        return reference
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
    }
}
