package net.spacealtctrl.discordrp.update

import java.net.URI

/**
 * Updates are distributed from the project's GitHub releases. The downloaded
 * APK must also carry the SHA-256 digest published with the asset.
 */
internal object UpdatePolicy {
    const val REPO = "spacealtctrl/DiscordRP"
    const val FEED_URL = "https://api.github.com/repos/$REPO/releases/latest"

    private const val ASSET_PREFIX = "/$REPO/releases/download/"
    private const val DEFAULT_HTTPS_PORT = 443
    private val SHA256 = Regex("[0-9a-f]{64}")

    // GitHub answers release-asset downloads with a redirect to a signed,
    // short-lived object URL. Both hosts are first-party.
    private val REDIRECT_HOSTS = setOf(
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
    )

    fun isTrustedApkUrl(raw: String?): Boolean {
        val uri = raw?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        if (!"github.com".equals(uri.host, ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != DEFAULT_HTTPS_PORT) return false
        if (uri.rawQuery != null || uri.fragment != null) return false
        return uri.path?.startsWith(ASSET_PREFIX) == true
    }

    /**
     * Rechecked at download time. [resolved] is the URL the HTTP client actually
     * ends up on after redirects; a redirect may only land on GitHub's signed
     * asset host, and a direct answer must still be the trusted asset URL.
     */
    fun isTrustedDownloadUrl(initial: String?, resolved: String?): Boolean {
        if (!isTrustedApkUrl(initial)) return false
        val uri = resolved?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != DEFAULT_HTTPS_PORT) return false
        if (uri.fragment != null) return false
        return when (uri.host?.lowercase()) {
            "github.com" -> isTrustedApkUrl(resolved)
            in REDIRECT_HOSTS -> true
            else -> false
        }
    }

    fun sha256(raw: String?): String? {
        val value = raw?.removePrefix("sha256:")?.trim()?.lowercase() ?: return null
        return value.takeIf { SHA256.matches(it) }
    }
}
