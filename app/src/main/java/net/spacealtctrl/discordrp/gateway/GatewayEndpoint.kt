package net.spacealtctrl.discordrp.gateway

import java.net.URI

/**
 * Resume endpoints arrive over the gateway itself, so they are validated
 * before a credential-bearing reconnect can use them.
 */
internal object GatewayEndpoint {
    const val DEFAULT_URL = "wss://gateway.discord.gg/?v=10&encoding=json"

    private const val QUERY = "?v=10&encoding=json"
    private const val DEFAULT_WSS_PORT = 443

    fun resumeUrl(raw: String?): String? {
        val uri = raw?.let { runCatching { URI(it) }.getOrNull() } ?: return null
        if (!"wss".equals(uri.scheme, ignoreCase = true)) return null
        if (uri.userInfo != null) return null
        if (uri.port != -1 && uri.port != DEFAULT_WSS_PORT) return null
        if (uri.rawQuery != null || uri.fragment != null) return null

        val path = uri.path.orEmpty()
        if (path.isNotEmpty() && path != "/") return null

        val host = uri.host?.lowercase() ?: return null
        if (!isGatewayHost(host)) return null
        return "wss://$host/$QUERY"
    }

    private fun isGatewayHost(host: String): Boolean {
        // Anchored to Discord's own three-label hosts: gateway.discord.gg or a
        // sharded name such as gateway-us-east1-b.discord.gg. Suffix checks
        // alone would also accept crafted names beneath discord.gg.
        val labels = host.split('.')
        if (labels.size != 3) return false
        if (labels[1] != "discord" || labels[2] != "gg") return false
        return labels[0] == "gateway" || SHARDED_LABEL.matches(labels[0])
    }

    private val SHARDED_LABEL = Regex("gateway-[a-z0-9]+(?:-[a-z0-9]+)*")
}
