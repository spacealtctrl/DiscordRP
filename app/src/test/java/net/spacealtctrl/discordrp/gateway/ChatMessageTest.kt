package net.spacealtctrl.discordrp.gateway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageTest {
    private val selfId = "1111"
    private val other = Sender(id = "2222", username = "bob", globalName = "Bob")

    private fun message(
        guildId: String? = null,
        mentions: List<Sender> = emptyList(),
        roleMentions: List<String> = emptyList(),
        mentionsEveryone: Boolean = false,
        quoted: QuotedMessage? = null,
        text: String? = "hi",
        attachments: List<Attachment> = emptyList(),
    ) = ChatMessage(
        id = "9",
        channelId = "42",
        guildId = guildId,
        text = text,
        sender = other,
        mentions = mentions,
        roleMentions = roleMentions,
        mentionsEveryone = mentionsEveryone,
        quoted = quoted,
        attachments = attachments,
    )

    @Test
    fun `messages without a guild are dms`() {
        assertTrue(message().isDm)
        assertFalse(message(guildId = "7").isDm)
    }

    @Test
    fun `own messages are recognised`() {
        val mine = message().copy(sender = Sender(id = selfId))
        assertTrue(mine.sentBy(selfId))
        assertFalse(message().sentBy(selfId))
        assertFalse(message().sentBy(null))
    }

    @Test
    fun `direct mention pings`() {
        val m = message(guildId = "7", mentions = listOf(Sender(id = selfId)))
        assertTrue(m.pings(selfId))
    }

    @Test
    fun `everyone mention pings`() {
        assertTrue(message(guildId = "7", mentionsEveryone = true).pings(selfId))
    }

    @Test
    fun `role mention pings only for held roles`() {
        val m = message(guildId = "7", roleMentions = listOf("55"))
        assertTrue(m.pings(selfId, heldRoles = setOf("55")))
        assertTrue(m.pings(selfId, heldRoles = setOf("12", "55")))
        assertFalse(m.pings(selfId, heldRoles = setOf("99")))
        assertFalse(m.pings(selfId))
    }

    @Test
    fun `reply to own message pings`() {
        val m = message(
            guildId = "7",
            quoted = QuotedMessage(id = "1", sender = Sender(id = selfId)),
        )
        assertTrue(m.pings(selfId))
    }

    @Test
    fun `plain guild message does not ping`() {
        assertFalse(message(guildId = "7").pings(selfId))
        assertFalse(message(guildId = "7").pings(null))
    }

    @Test
    fun `digest falls back to attachment wording`() {
        assertEquals("hi", message().digest())
        assertEquals(
            "Sent an attachment",
            message(text = "", attachments = listOf(Attachment(id = "1"))).digest(),
        )
        assertEquals(
            "Sent 2 attachments",
            message(
                text = null,
                attachments = listOf(Attachment(id = "1"), Attachment(id = "2")),
            ).digest(),
        )
        assertEquals("New message", message(text = null).digest())
    }

    @Test
    fun `deep link targets the right scope`() {
        assertEquals("https://discord.com/channels/@me/42/9", message().deepLink())
        assertEquals("https://discord.com/channels/7/42/9", message(guildId = "7").deepLink())
    }

    @Test
    fun `display name prefers the global name`() {
        assertEquals("Bob", message().sender?.displayName())
        assertEquals("bob", other.copy(globalName = null).displayName())
        assertEquals("DiscordRP", Sender().displayName())
    }

    @Test
    fun `portrait url handles animated and missing avatars`() {
        val animated = other.copy(avatar = "a_beef")
        assertTrue(animated.portraitUrl().endsWith(".gif?size=128"))
        val plain = other.copy(avatar = "beef")
        assertTrue(plain.portraitUrl().endsWith(".png?size=128"))
        val stock = other.copy(avatar = null)
        assertTrue(stock.portraitUrl().startsWith("https://cdn.discordapp.com/embed/avatars/"))
    }
}
