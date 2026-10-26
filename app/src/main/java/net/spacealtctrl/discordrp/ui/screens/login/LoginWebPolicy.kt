package net.spacealtctrl.discordrp.ui.screens.login

import java.net.URI

/**
 * Trust boundary for the embedded Discord login page. Only canonical HTTPS
 * Discord origins can load in the login WebView, and only /app may trigger
 * token extraction.
 */
internal object LoginWebPolicy {
    private const val DEFAULT_HTTPS_PORT = 443

    fun isDiscordUrl(raw: String?): Boolean {
        val uri = raw?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != DEFAULT_HTTPS_PORT) return false
        val host = uri.host?.lowercase() ?: return false
        // Only the origin that serves the login application. Legacy
        // discordapp.com links 301 to the same place, and neither the login page nor
        // extraction needs any other Discord origin.
        return host == "discord.com"
    }

    fun isDiscordAppUrl(raw: String?): Boolean {
        val uri = raw?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        return isDiscordUrl(raw) && uri.path?.trimEnd('/') == "/app"
    }
}
