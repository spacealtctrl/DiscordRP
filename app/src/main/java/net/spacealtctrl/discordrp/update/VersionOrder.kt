package net.spacealtctrl.discordrp.update

object VersionOrder {
    fun isNewer(candidate: String, installed: String): Boolean =
        compare(candidate, installed) > 0

    private fun compare(a: String, b: String): Int {
        val left = parts(a)
        val right = parts(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    private fun parts(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .substringBefore('-')
            .split('.')
            .mapNotNull { it.toIntOrNull() }
}
