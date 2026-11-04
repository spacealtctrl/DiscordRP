package net.spacealtctrl.discordrp.gateway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GatewayEndpointTest {
    @Test
    fun acceptsDiscordGatewayHostsAndAddsTheProtocolQuery() {
        assertEquals(
            "wss://gateway.discord.gg/?v=10&encoding=json",
            GatewayEndpoint.resumeUrl("wss://gateway.discord.gg"),
        )
        assertEquals(
            "wss://gateway.discord.gg/?v=10&encoding=json",
            GatewayEndpoint.resumeUrl("wss://gateway.discord.gg/"),
        )
        assertEquals(
            "wss://gateway-us-east1-b.discord.gg/?v=10&encoding=json",
            GatewayEndpoint.resumeUrl("wss://gateway-us-east1-b.discord.gg"),
        )
        assertEquals(
            "wss://gateway.discord.gg/?v=10&encoding=json",
            GatewayEndpoint.resumeUrl("wss://GATEWAY.DISCORD.GG:443"),
        )
    }

    @Test
    fun rejectsEndpointsThatCouldReceiveTheCredential() {
        assertNull(GatewayEndpoint.resumeUrl(null))
        assertNull(GatewayEndpoint.resumeUrl(""))
        assertNull(GatewayEndpoint.resumeUrl("ws://gateway.discord.gg"))
        assertNull(GatewayEndpoint.resumeUrl("https://gateway.discord.gg"))
        assertNull(GatewayEndpoint.resumeUrl("wss://evil.test"))
        assertNull(GatewayEndpoint.resumeUrl("wss://discord.gg"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway.discord.gg.evil.test"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway.discord.gg.evil.discord.gg"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway.attacker.discord.gg"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gatewayevil.discord.gg"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway-.discord.gg"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway.discord.gg."))
        assertNull(GatewayEndpoint.resumeUrl("wss://user@gateway.discord.gg"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway.discord.gg:8443"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway.discord.gg/socket"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway.discord.gg?v=10"))
        assertNull(GatewayEndpoint.resumeUrl("wss://gateway.discord.gg/#fragment"))
    }
}
