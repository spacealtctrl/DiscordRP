package net.spacealtctrl.discordrp.settings

enum class CoverSource(val stored: String) {
    MUSICBRAINZ("musicbrainz"),
    MUSICBRAINZ_DEEZER("musicbrainz_deezer");

    val deezerBackup: Boolean get() = this == MUSICBRAINZ_DEEZER

    companion object {
        fun fromStored(value: String?): CoverSource =
            entries.firstOrNull { it.stored == value } ?: MUSICBRAINZ
    }
}
