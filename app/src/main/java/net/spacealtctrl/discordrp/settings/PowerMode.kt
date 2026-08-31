package net.spacealtctrl.discordrp.settings

enum class PowerMode(val stored: String) {
    SAVER("saver"),
    BALANCED("balanced"),
    ALWAYS_ON("always_on");

    val holdsCpuAwake: Boolean get() = this == ALWAYS_ON

    val fetchesRemoteArt: Boolean get() = this != SAVER

    val watchdogIntervalMs: Long
        get() = when (this) {
            SAVER -> 60 * 60 * 1000L
            BALANCED -> 15 * 60 * 1000L
            ALWAYS_ON -> 15 * 60 * 1000L
        }

    val playbackSettleMs: Long
        get() = when (this) {
            SAVER -> 5_000L
            BALANCED -> 1_000L
            ALWAYS_ON -> 1_000L
        }

    companion object {
        fun fromStored(value: String?): PowerMode =
            entries.firstOrNull { it.stored == value } ?: ALWAYS_ON
    }
}
