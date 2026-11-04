package net.spacealtctrl.discordrp.presence

internal object ReleasePick {
    private const val DIGITAL = "Digital Media"
    private const val OFFICIAL = "Official"
    private const val UNDATED = "9999"

    fun pick(releases: List<BrainzReleaseItem>): String? {
        val valid = releases
            .filter { CoverQueries.isMbid(it.id) }
            .sortedBy { it.date?.takeIf(String::isNotBlank) ?: UNDATED }
        val official = valid.filter { it.status.equals(OFFICIAL, ignoreCase = true) }
        val digital = official.firstOrNull { release -> release.media.any { it.format == DIGITAL } }
        return (digital ?: official.firstOrNull() ?: valid.firstOrNull())?.id
    }
}
