package net.spacealtctrl.discordrp.ui.screens.login

import java.net.URI

internal object LoginWebPolicy {
    private const val DEFAULT_HTTPS_PORT = 443

    fun isDiscordUrl(raw: String?): Boolean {
        val uri = raw?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != DEFAULT_HTTPS_PORT) return false
        val host = uri.host?.lowercase() ?: return false
        return host == "discord.com"
    }

    fun isDiscordAppUrl(raw: String?): Boolean {
        val uri = raw?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        return isDiscordUrl(raw) && uri.path?.trimEnd('/') == "/app"
    }
}
