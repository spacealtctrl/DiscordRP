package net.spacealtctrl.discordrp.presence

internal object CoverUrls {
    private val ARCHIVE_SIZED = Regex(
        "^(https://coverartarchive\\.org/(?:release|release-group)/[0-9a-f-]{36}/front)-(?:250|500|1200)$",
    )

    fun displayCandidates(url: String): List<String> {
        val original = ARCHIVE_SIZED.find(url)?.groupValues?.get(1) ?: return listOf(url)
        return listOf(url, original)
    }
}
