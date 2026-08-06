package net.spacealtctrl.discordrp.settings

enum class Mood(val wire: String) {
    ONLINE("online"),
    IDLE("idle"),
    DND("dnd"),
    INVISIBLE("invisible");

    companion object {
        fun fromWire(value: String?): Mood =
            entries.firstOrNull { it.wire == value } ?: ONLINE
    }
}

enum class AlertScope(val stored: String) {
    EVERYTHING("everything"),
    MENTIONS_AND_DMS("mentions"),
    DMS_ONLY("dms");

    companion object {
        fun fromStored(value: String?): AlertScope =
            entries.firstOrNull { it.stored == value } ?: EVERYTHING
    }
}
