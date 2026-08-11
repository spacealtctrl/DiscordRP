package net.spacealtctrl.discordrp.inbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConversationLogTest {
    private fun line(body: String, fromMe: Boolean = false) = ConversationLog.Line(
        senderName = if (fromMe) "You" else "Someone",
        senderId = if (fromMe) "self" else "them",
        portraitUrl = null,
        body = body,
        at = 0L,
        fromMe = fromMe,
    )

    @Before
    fun clear() {
        ConversationLog.forgetAll()
    }

    @Test
    fun `a thread keeps its order`() {
        ConversationLog.record("c1", line("first"))
        ConversationLog.record("c1", line("second"))
        val thread = ConversationLog.record("c1", line("third"))
        assertEquals(listOf("first", "second", "third"), thread.map { it.body })
    }

    @Test
    fun `a reply is marked as ours and the rest are not`() {
        ConversationLog.record("c1", line("theirs"))
        val thread = ConversationLog.record("c1", line("mine", fromMe = true))
        assertFalse(thread.first().fromMe)
        assertTrue(thread.last().fromMe)
    }

    @Test
    fun `a long thread keeps only the newest lines`() {
        repeat(12) { ConversationLog.record("c1", line("line $it")) }
        val thread = ConversationLog.record("c1", line("newest"))
        assertEquals(8, thread.size)
        assertEquals("newest", thread.last().body)
        assertTrue(thread.none { it.body == "line 0" })
    }

    @Test
    fun `threads are kept apart`() {
        ConversationLog.record("c1", line("one"))
        val other = ConversationLog.record("c2", line("two"))
        assertEquals(listOf("two"), other.map { it.body })
    }

    @Test
    fun `forgetting a thread starts it over`() {
        ConversationLog.record("c1", line("old"))
        ConversationLog.forget("c1")
        val thread = ConversationLog.record("c1", line("new"))
        assertEquals(listOf("new"), thread.map { it.body })
    }
}
