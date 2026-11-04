package net.spacealtctrl.discordrp.presence

internal object CoverQueries {
    const val LENGTH_SLACK_MS = 1_000L

    fun album(album: String, artists: List<String>): String =
        "releasegroup:${phrase(album)}" + artistClause(artists)

    fun song(title: String, artists: List<String>): String =
        "recording:${phrase(title)}" + artistClause(artists)

    fun freeText(value: String): String = value
        .replace(SYNTAX, " ")
        .replace(WHITESPACE, " ")
        .trim()

    fun artists(names: List<String>): String = names.joinToString(" OR ") { name ->
        val quoted = phrase(name)
        "artist:$quoted OR alias:$quoted OR sortname:$quoted"
    }

    fun songBy(title: String, artistIds: List<String>): String? =
        aridClause(artistIds)?.let { "recording:${phrase(title)} AND $it" }

    fun lengthBy(durationMillis: Long, artistIds: List<String>): String? =
        aridClause(artistIds)?.let {
            "$it AND dur:[${durationMillis - LENGTH_SLACK_MS} TO ${durationMillis + LENGTH_SLACK_MS}]"
        }

    fun isMbid(value: String?): Boolean = value != null && MBID.matches(value)

    private fun artistClause(artists: List<String>): String =
        if (artists.isEmpty()) {
            ""
        } else {
            artists.joinToString(" OR ", prefix = " AND (", postfix = ")") { "artist:${phrase(it)}" }
        }

    private fun aridClause(artistIds: List<String>): String? = artistIds
        .filter(::isMbid)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" OR ", prefix = "arid:(", postfix = ")")

    private fun phrase(value: String): String =
        "\"" + value.replace(QUOTES, " ").replace(WHITESPACE, " ").trim() + "\""

    private val MBID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    private val QUOTES = Regex("""["\\]""")
    private val SYNTAX = Regex("""[+\-!(){}\[\]^"~*?:\\/&|]""")
    private val WHITESPACE = Regex("""\s+""")
}
