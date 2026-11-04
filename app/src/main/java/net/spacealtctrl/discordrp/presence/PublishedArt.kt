package net.spacealtctrl.discordrp.presence

import androidx.compose.runtime.Immutable

@Immutable
data class PublishedArt(
    val large: String? = null,
    val small: String? = null,
    val coverKey: String? = null,
    val missingGroupId: String? = null,
    val deezerAlbumId: Long? = null,
    val albumMissing: Boolean = false,
    val retrySoon: Boolean = false,
)

object DiscordMedia {
    private const val PROXY = "https://media.discordapp.net/"

    fun url(ref: String?): String? = ref
        ?.takeIf { it.startsWith("mp:") }
        ?.removePrefix("mp:")
        ?.takeIf { it.isNotBlank() }
        ?.let { PROXY + it }

    fun thumbnail(ref: String?): String? = url(ref)?.let { "$it?width=64&height=64" }
}

object CoverOffer {
    private const val SQUARE_TOLERANCE = 0.1f

    fun canAdd(album: String?, width: Int, height: Int, missingGroupId: String?): Boolean =
        !album.isNullOrBlank() && CoverQueries.isMbid(missingGroupId) && isSquare(width, height)

    private fun isSquare(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val ratio = width.toFloat() / height
        return ratio in (1f - SQUARE_TOLERANCE)..(1f + SQUARE_TOLERANCE)
    }

    fun canAdd(track: NowPlaying, art: PublishedArt?): Boolean {
        val artwork = track.artwork ?: return false
        return canAdd(track.album, artwork.width, artwork.height, art?.missingGroupId)
    }

    fun canAddAlbum(album: String?, albumMissing: Boolean, deezerAlbumId: Long?): Boolean =
        !album.isNullOrBlank() && albumMissing && deezerAlbumId != null && deezerAlbumId > 0

    fun canAddAlbum(track: NowPlaying, art: PublishedArt?): Boolean = canAddAlbum(
        album = track.album,
        albumMissing = art?.albumMissing == true && art.missingGroupId == null,
        deezerAlbumId = art?.deezerAlbumId,
    )
}
