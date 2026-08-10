package net.spacealtctrl.discordrp.presence.tidy

import java.text.Normalizer

object TextCanon {
    private val COMBINING = Regex("""\p{Mn}+""")
    private val ELISION = Regex("""['’‘`´]""")
    private val PUNCTUATION = Regex("""[^\p{L}\p{N}\s]""")
    private val WHITESPACE = Regex("""\s+""")
    private val FEATURE = Regex(
        """\s*[(\[]?\s*\b(?:feat|ft|featuring)\b\.?\s+.*$""",
        RegexOption.IGNORE_CASE,
    )

    fun fold(value: String?): String {
        if (value.isNullOrBlank()) return ""
        var text = Normalizer.normalize(value, Normalizer.Form.NFKD)
        text = COMBINING.replace(text, "")
        text = text.lowercase()
        text = text.replace('＆', '&').replace("&", " and ")
        text = FEATURE.replace(text, " ")
        text = ELISION.replace(text, "")
        text = PUNCTUATION.replace(text, " ")
        text = WHITESPACE.replace(text, " ").trim()
        return text.removePrefix("the ")
    }

    fun similarity(left: String?, right: String?): Double {
        val a = fold(left)
        val b = fold(right)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        return maxOf(bigramDice(a, b), tokenDice(a, b))
    }

    fun sameText(left: String?, right: String?): Boolean {
        val a = fold(left)
        return a.isNotEmpty() && a == fold(right)
    }

    private fun bigramDice(a: String, b: String): Double {
        val left = bigrams(a)
        val right = bigrams(b)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val shared = left.count { it in right }
        return 2.0 * shared / (left.size + right.size)
    }

    private fun bigrams(value: String): Set<String> {
        val packed = value.replace(" ", "")
        if (packed.length < 2) return setOfNotNull(packed.takeIf { it.isNotEmpty() })
        return (0 until packed.length - 1).mapTo(LinkedHashSet()) { packed.substring(it, it + 2) }
    }

    private fun tokenDice(a: String, b: String): Double {
        val left = a.split(' ').filter { it.isNotEmpty() }.toSet()
        val right = b.split(' ').filter { it.isNotEmpty() }.toSet()
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val shared = left.count { it in right }
        return 2.0 * shared / (left.size + right.size)
    }
}
