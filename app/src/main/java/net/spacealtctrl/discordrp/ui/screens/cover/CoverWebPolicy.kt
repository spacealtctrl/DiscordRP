package net.spacealtctrl.discordrp.ui.screens.cover

import java.net.URI

internal object CoverWebPolicy {
    private const val MUSICBRAINZ = "musicbrainz.org"
    private const val METABRAINZ = "metabrainz.org"
    private const val DEFAULT_HTTPS_PORT = 443

    fun addCoverUrl(releaseId: String): String = "https://$MUSICBRAINZ/release/$releaseId/add-cover-art"

    fun isAllowed(raw: String?): Boolean {
        val uri = parse(raw) ?: return false
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != DEFAULT_HTTPS_PORT) return false
        val host = uri.host?.lowercase() ?: return false
        return host == MUSICBRAINZ || host == METABRAINZ || host.endsWith(".$METABRAINZ")
    }

    fun isUploadForm(raw: String?, releaseId: String): Boolean =
        onMusicBrainz(raw) && path(raw) == "/release/$releaseId/add-cover-art"

    fun isReleaseEditor(raw: String?): Boolean =
        onMusicBrainz(raw) && path(raw) == "/release/add"

    fun isUploaded(raw: String?, releaseId: String): Boolean =
        onMusicBrainz(raw) && path(raw) == "/release/$releaseId/cover-art"

    private fun onMusicBrainz(raw: String?): Boolean =
        isAllowed(raw) && parse(raw)?.host.equals(MUSICBRAINZ, ignoreCase = true)

    private fun path(raw: String?): String? = parse(raw)?.path?.trimEnd('/')

    private fun parse(raw: String?): URI? = raw?.let { runCatching { URI(it) }.getOrNull() }
}
