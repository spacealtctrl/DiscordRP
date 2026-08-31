package net.spacealtctrl.discordrp.presence

import net.spacealtctrl.discordrp.gateway.ActivityCard
import net.spacealtctrl.discordrp.gateway.ArtRefs
import net.spacealtctrl.discordrp.gateway.PresenceUpdate
import net.spacealtctrl.discordrp.gateway.TimeSpan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceEchoTest {
    private fun update(
        status: String = "online",
        name: String = "Symfonium",
        details: String? = "Karry On",
        state: String? = "Morning Glory",
        start: Long? = 1_000_000L,
        end: Long? = 1_240_000L,
        assets: ArtRefs? = ArtRefs(largeImage = "mp:external/cover"),
    ) = PresenceUpdate(
        status = status,
        activities = listOf(
            ActivityCard(
                name = name,
                details = details,
                state = state,
                timestamps = TimeSpan(start = start, end = end),
                assets = assets,
            ),
        ),
    )

    @Test
    fun `first update is never an echo`() {
        assertFalse(PresenceEcho.isEcho(null, update()))
    }

    @Test
    fun `identical update is an echo`() {
        assertTrue(PresenceEcho.isEcho(update(), update()))
    }

    @Test
    fun `timestamp jitter within tolerance is still an echo`() {
        assertTrue(PresenceEcho.isEcho(update(start = 1_000_000L), update(start = 1_000_120L)))
    }

    @Test
    fun `a seek past the tolerance is not an echo`() {
        assertFalse(PresenceEcho.isEcho(update(start = 1_000_000L), update(start = 1_090_000L)))
    }

    @Test
    fun `a new track is not an echo`() {
        assertFalse(PresenceEcho.isEcho(update(details = "Karry On"), update(details = "War Psalms")))
    }

    @Test
    fun `a mood change is not an echo`() {
        assertFalse(PresenceEcho.isEcho(update(status = "online"), update(status = "dnd")))
    }

    @Test
    fun `artwork arriving late is not an echo`() {
        assertFalse(PresenceEcho.isEcho(update(assets = null), update()))
    }

    @Test
    fun `clearing the activity is not an echo`() {
        val quiet = PresenceUpdate(status = "online")
        assertFalse(PresenceEcho.isEcho(update(), quiet))
        assertTrue(PresenceEcho.isEcho(quiet, quiet))
    }

    @Test
    fun `a missing timestamp never matches a present one`() {
        assertFalse(PresenceEcho.isEcho(update(start = null), update(start = 1_000_000L)))
        assertTrue(PresenceEcho.isEcho(update(start = null), update(start = null)))
    }
}
