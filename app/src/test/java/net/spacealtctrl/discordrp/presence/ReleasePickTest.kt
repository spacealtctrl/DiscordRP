package net.spacealtctrl.discordrp.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleasePickTest {
    private val cd = "11111111-1111-4111-8111-111111111111"
    private val digital = "22222222-2222-4222-8222-222222222222"
    private val promo = "33333333-3333-4333-8333-333333333333"
    private val later = "44444444-4444-4444-8444-444444444444"

    private fun release(id: String, status: String?, date: String?, vararg formats: String) =
        BrainzReleaseItem(id = id, status = status, date = date, media = formats.map { BrainzMedium(it) })

    @Test
    fun `official digital release wins`() {
        val picked = ReleasePick.pick(
            listOf(
                release(cd, "Official", "2026-05-15", "CD"),
                release(promo, "Promotion", "2026-05-01", "Digital Media"),
                release(digital, "Official", "2026-05-15", "Digital Media"),
            ),
        )
        assertEquals(digital, picked)
    }

    @Test
    fun `earliest official release when none is digital`() {
        val picked = ReleasePick.pick(
            listOf(release(later, "Official", "2027-01-01", "CD"), release(cd, "Official", "2026-05-15", "CD")),
        )
        assertEquals(cd, picked)
    }

    @Test
    fun `any release when none is official`() {
        assertEquals(promo, ReleasePick.pick(listOf(release(promo, "Promotion", null))))
    }

    @Test
    fun `nothing usable gives nothing`() {
        assertNull(ReleasePick.pick(emptyList()))
        assertNull(ReleasePick.pick(listOf(release("not-an-id", "Official", "2026-05-15", "Digital Media"))))
    }
}
