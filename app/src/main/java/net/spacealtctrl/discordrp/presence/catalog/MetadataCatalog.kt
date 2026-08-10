package net.spacealtctrl.discordrp.presence.catalog

import net.spacealtctrl.discordrp.presence.tidy.TrackFacts

data class LookupAsk(
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
)

interface MetadataCatalog {
    val label: String

    suspend fun lookup(ask: LookupAsk): TrackFacts?
}
