package net.spacealtctrl.discordrp.gateway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("content") val text: String? = null,
    @SerialName("author") val sender: Sender? = null,
    val mentions: List<Sender> = emptyList(),
    @SerialName("mention_roles") val roleMentions: List<String> = emptyList(),
    @SerialName("mention_everyone") val mentionsEveryone: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    @SerialName("referenced_message") val quoted: QuotedMessage? = null,
) {
    val isDm: Boolean get() = guildId == null

    fun sentBy(userId: String?): Boolean =
        userId != null && sender?.id == userId

    fun pings(userId: String?, heldRoles: Set<String> = emptySet()): Boolean = when {
        userId == null -> false
        mentions.any { it.id == userId } -> true
        mentionsEveryone -> true
        quoted?.sender?.id == userId -> true
        roleMentions.any { it in heldRoles } -> true
        else -> false
    }

    fun digest(): String = when {
        !text.isNullOrBlank() -> text
        attachments.size == 1 -> "Sent an attachment"
        attachments.size > 1 -> "Sent ${attachments.size} attachments"
        else -> "New message"
    }

    fun deepLink(): String = if (isDm) {
        "https://discord.com/channels/@me/$channelId/$id"
    } else {
        "https://discord.com/channels/$guildId/$channelId/$id"
    }
}

@Serializable
data class Sender(
    val id: String? = null,
    val username: String? = null,
    @SerialName("global_name") val globalName: String? = null,
    val avatar: String? = null,
    val bot: Boolean? = null,
) {
    fun displayName(): String =
        globalName?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: "DiscordRP"

    fun portraitUrl(size: Int = 128): String {
        val userId = id ?: return stockPortrait(null)
        val hash = avatar ?: return stockPortrait(userId)
        val ext = if (hash.startsWith("a_")) "gif" else "png"
        return "https://cdn.discordapp.com/avatars/$userId/$hash.$ext?size=$size"
    }

    private fun stockPortrait(userId: String?): String {
        val index = userId?.toLongOrNull()?.let { ((it shr 22) % 6).toInt() } ?: 0
        return "https://cdn.discordapp.com/embed/avatars/$index.png"
    }
}

@Serializable
data class Attachment(
    val id: String? = null,
    val filename: String? = null,
)

@Serializable
data class QuotedMessage(
    val id: String? = null,
    @SerialName("author") val sender: Sender? = null,
)
