package net.spacealtctrl.discordrp.inbox

internal object ConversationLog {
    data class Line(
        val senderName: String,
        val senderId: String?,
        val portraitUrl: String?,
        val body: String,
        val at: Long,
        val fromMe: Boolean = false,
    )

    private const val LINES_PER_CONVERSATION = 8
    private const val CONVERSATION_CAP = 30

    private val threads = object : LinkedHashMap<String, List<Line>>() {
        override fun removeEldestEntry(eldest: Map.Entry<String, List<Line>>) =
            size > CONVERSATION_CAP
    }

    @Synchronized
    fun record(channelId: String, line: Line): List<Line> {
        val updated = ((threads[channelId] ?: emptyList()) + line).takeLast(LINES_PER_CONVERSATION)
        threads[channelId] = updated
        return updated
    }

    @Synchronized
    fun forget(channelId: String) {
        threads.remove(channelId)
    }

    @Synchronized
    fun forgetAll() {
        threads.clear()
    }
}
