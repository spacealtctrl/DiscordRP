package net.spacealtctrl.discordrp.presence

import java.text.Normalizer

internal object CoverNames {
    fun album(value: String?): String? = tidy(
        value?.replace(FORMAT_SUFFIX, "")?.replace(FEATURING, "")?.replace(EDITION_SUFFIX, ""),
    )

    fun title(value: String?): String? = tidy(value?.replace(FEATURING, ""))

    fun sameAlbum(have: String?, want: String?): Boolean {
        val wanted = albumKeys(want)
        return albumKeys(have).any(wanted::contains)
    }

    fun credits(vararg values: String?): List<String> = values
        .flatMap { value ->
            val whole = tidy(value) ?: return@flatMap emptyList()
            listOf(whole) + whole.split(CREDIT_BREAK).mapNotNull(::tidy)
        }
        .distinctBy(::key)

    fun key(value: String?): String = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFKC)
        .lowercase()
        .replace(NON_WORD, " ")
        .trim()

    private fun albumKeys(value: String?): Set<String> {
        val whole = key(album(value)).takeIf(String::isNotEmpty) ?: return emptySet()
        return setOf(whole, whole.removeSuffix(EP_TAIL))
    }

    private fun tidy(value: String?): String? = value
        ?.replace(QUOTES, " ")
        ?.replace(WHITESPACE, " ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private val FEATURING = Regex(
        """\s*[\[(]\s*(feat\.?|ft\.?|featuring|with)\s[^\[\]()]*[])]""",
        RegexOption.IGNORE_CASE,
    )
    private val EDITION_SUFFIX = Regex(
        """[\[(][^\[\]()]*\b(edition|deluxe|remaster\w*|expanded|anniversary|bonus|explicit|version|reissue)\b[^\[\]()]*[])]""",
        RegexOption.IGNORE_CASE,
    )
    private const val EP_TAIL = " ep"
    private val FORMAT_SUFFIX = Regex("""\s*-\s+(single|ep)$""", RegexOption.IGNORE_CASE)
    private val CREDIT_BREAK = Regex(
        """\s*(?:,|、|;|＆|&|\s/\s|\s(?:feat\.?|ft\.?|featuring|with|x|×|vs\.?)\s)\s*""",
        RegexOption.IGNORE_CASE,
    )
    private val QUOTES = Regex("""["\\]""")
    private val WHITESPACE = Regex("""\s+""")
    private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")
}
