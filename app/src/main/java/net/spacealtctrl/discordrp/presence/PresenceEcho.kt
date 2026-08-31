package net.spacealtctrl.discordrp.presence

import net.spacealtctrl.discordrp.gateway.ActivityCard
import net.spacealtctrl.discordrp.gateway.PresenceUpdate
import kotlin.math.abs

object PresenceEcho {
    const val TIMELINE_TOLERANCE_MS = 5_000L

    fun isEcho(
        previous: PresenceUpdate?,
        next: PresenceUpdate,
        toleranceMs: Long = TIMELINE_TOLERANCE_MS,
    ): Boolean {
        if (previous == null) return false
        if (previous.status != next.status || previous.afk != next.afk) return false
        if (previous.activities.size != next.activities.size) return false
        return previous.activities.zip(next.activities).all { (before, after) ->
            sameCard(before, after, toleranceMs)
        }
    }

    private fun sameCard(before: ActivityCard, after: ActivityCard, toleranceMs: Long): Boolean =
        before.name == after.name &&
            before.type == after.type &&
            before.details == after.details &&
            before.state == after.state &&
            before.assets == after.assets &&
            before.applicationId == after.applicationId &&
            nearlyEqual(before.timestamps?.start, after.timestamps?.start, toleranceMs) &&
            nearlyEqual(before.timestamps?.end, after.timestamps?.end, toleranceMs)

    private fun nearlyEqual(before: Long?, after: Long?, toleranceMs: Long): Boolean = when {
        before == null || after == null -> before == after
        else -> abs(before - after) <= toleranceMs
    }
}
