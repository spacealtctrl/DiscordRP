package net.spacealtctrl.discordrp.presence

import java.net.URI

internal data class DeezerHit(val cover: String, val albumId: Long?)

internal object DeezerPicks {
    private val CDN_HOST = Regex("^[a-z0-9-]+\\.dzcdn\\.net$")
    private const val DEFAULT_HTTPS_PORT = 443

    fun album(albums: List<DeezerAlbum>, album: String, credits: List<String>): DeezerHit? {
        val match = albums.firstOrNull {
            CoverNames.sameAlbum(it.title, album) && artistMatches(it.artist?.name, credits)
        } ?: return null
        return cover(match.coverXl, match.coverBig)?.let { DeezerHit(it, match.id) }
    }

    fun track(tracks: List<DeezerTrack>, title: String, album: String?, credits: List<String>): DeezerHit? {
        val titleKey = CoverNames.key(title)
        val matches = tracks.filter {
            CoverNames.key(CoverNames.title(it.title)) == titleKey && artistMatches(it.artist?.name, credits)
        }
        val best = if (album != null) {
            matches.firstOrNull { CoverNames.sameAlbum(it.album?.title, album) }
        } else {
            matches.firstOrNull { CoverNames.key(CoverNames.album(it.album?.title)) == titleKey } ?: matches.firstOrNull()
        }
        val ref = best?.album ?: return null
        return cover(ref.coverXl, ref.coverBig)?.let { DeezerHit(it, ref.id) }
    }

    fun isDeezerImage(url: String?): Boolean {
        val uri = url?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != DEFAULT_HTTPS_PORT) return false
        return CDN_HOST.matches(uri.host?.lowercase().orEmpty())
    }

    fun cover(vararg urls: String?): String? = urls.firstOrNull(::isDeezerImage)

    private fun artistMatches(name: String?, credits: List<String>): Boolean {
        val have = CoverNames.key(name)
        if (have.isEmpty()) return false
        return credits.any { credit ->
            val want = CoverNames.key(credit)
            want.isNotEmpty() && (want == have || " $want ".contains(" $have ") || " $have ".contains(" $want "))
        }
    }
}
