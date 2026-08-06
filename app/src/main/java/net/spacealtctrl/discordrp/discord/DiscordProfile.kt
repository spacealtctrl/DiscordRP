package net.spacealtctrl.discordrp.discord

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val CDN = "https://cdn.discordapp.com"

@Serializable
@Immutable
data class DiscordProfile(
    val id: String? = null,
    val username: String? = null,
    @SerialName("global_name") val globalName: String? = null,
    val avatar: String? = null,
    val banner: String? = null,
    @SerialName("banner_color") val bannerColor: String? = null,
    @SerialName("accent_color") val accentColor: Int? = null,
    val bio: String? = null,
    val nitro: Boolean? = null,
) {
    fun displayName(): String =
        globalName?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: "Unknown"

    fun avatarUrl(size: Int = 256): String? {
        val userId = id ?: return null
        val hash = avatar?.takeIf { it.isNotBlank() } ?: return null
        val ext = if (hash.startsWith("a_")) "gif" else "png"
        return "$CDN/avatars/$userId/$hash.$ext?size=$size"
    }

    fun bannerUrl(): String? {
        val userId = id ?: return null
        val hash = banner?.takeIf { it.isNotBlank() } ?: return null
        val ext = if (hash.startsWith("a_")) "gif" else "png"
        return "$CDN/banners/$userId/$hash.$ext?size=480"
    }

    fun joinedAtMillis(): Long? =
        id?.toLongOrNull()?.let { (it shr 22) + DISCORD_EPOCH_MS }

    companion object {
        const val DISCORD_EPOCH_MS = 1_420_070_400_000L
    }
}
