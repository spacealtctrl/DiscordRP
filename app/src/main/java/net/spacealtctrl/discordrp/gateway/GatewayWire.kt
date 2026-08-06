package net.spacealtctrl.discordrp.gateway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

internal object Op {
    const val EVENT = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val PRESENCE = 3
    const val RESUME = 6
    const val RECONNECT = 7
    const val INVALID_SESSION = 9
    const val HELLO = 10
    const val HEARTBEAT_ACK = 11
}

@Serializable
internal data class Envelope(
    val op: Int? = null,
    val s: Int? = null,
    val t: String? = null,
    val d: JsonElement? = null,
)

@Serializable
internal data class HelloData(
    @SerialName("heartbeat_interval") val heartbeatInterval: Long,
)

@Serializable
internal data class ReadyData(
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("resume_gateway_url") val resumeUrl: String? = null,
    val user: SessionUser? = null,
)

@Serializable
data class SessionUser(
    val id: String? = null,
    val bio: String? = null,
    @SerialName("premium_type") val premiumType: Int? = null,
)

@Serializable
internal data class IdentifyRequest(
    val token: String,
    val capabilities: Int = 65,
    val compress: Boolean = false,
    @SerialName("large_threshold") val largeThreshold: Int = 100,
    val properties: ClientStamp,
)

@Serializable
internal data class ClientStamp(
    val os: String,
    val browser: String,
    val device: String,
) {
    companion object {
        val HANDSET = ClientStamp(os = "Android", browser = "Discord Android", device = "android")
        val WORKSTATION = ClientStamp(os = "Windows", browser = "Discord Client", device = "desktop")

        fun forMobileBadge(wanted: Boolean) = if (wanted) HANDSET else WORKSTATION
    }
}

@Serializable
internal data class ResumeRequest(
    val token: String,
    @SerialName("session_id") val sessionId: String,
    val seq: Int,
)

@Serializable
internal data class ReadReceipt(
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("message_id") val messageId: String? = null,
)
