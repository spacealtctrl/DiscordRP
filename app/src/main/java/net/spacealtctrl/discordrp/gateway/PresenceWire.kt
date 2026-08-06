package net.spacealtctrl.discordrp.gateway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresenceUpdate(
    val status: String,
    val since: Long = 0L,
    val afk: Boolean = true,
    val activities: List<ActivityCard> = emptyList(),
)

@Serializable
data class ActivityCard(
    val name: String,
    val type: Int = 2,
    val details: String? = null,
    val state: String? = null,
    val timestamps: TimeSpan? = null,
    val assets: ArtRefs? = null,
    @SerialName("application_id") val applicationId: String? = null,
) {
    companion object {
        fun clip(value: String?): String? =
            value?.takeIf { it.isNotBlank() }?.take(128)
    }
}

@Serializable
data class TimeSpan(
    val start: Long? = null,
    val end: Long? = null,
)

@Serializable
data class ArtRefs(
    @SerialName("large_image") val largeImage: String? = null,
    @SerialName("small_image") val smallImage: String? = null,
    @SerialName("large_text") val largeText: String? = null,
    @SerialName("small_text") val smallText: String? = null,
)
