package net.spacealtctrl.discordrp.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerModeTest {
    @Test
    fun `unset storage keeps delivery guaranteed`() {
        assertEquals(PowerMode.ALWAYS_ON, PowerMode.fromStored(null))
        assertEquals(PowerMode.ALWAYS_ON, PowerMode.fromStored("nonsense"))
    }

    @Test
    fun `stored values round trip`() {
        PowerMode.entries.forEach { mode ->
            assertEquals(mode, PowerMode.fromStored(mode.stored))
        }
    }

    @Test
    fun `only always on pins the cpu awake`() {
        assertTrue(PowerMode.ALWAYS_ON.holdsCpuAwake)
        assertFalse(PowerMode.BALANCED.holdsCpuAwake)
        assertFalse(PowerMode.SAVER.holdsCpuAwake)
    }

    @Test
    fun `only saver stops reaching out for art`() {
        assertTrue(PowerMode.ALWAYS_ON.fetchesRemoteArt)
        assertTrue(PowerMode.BALANCED.fetchesRemoteArt)
        assertFalse(PowerMode.SAVER.fetchesRemoteArt)
    }

    @Test
    fun `thriftier modes check in less often and settle for longer`() {
        assertTrue(PowerMode.SAVER.watchdogIntervalMs > PowerMode.BALANCED.watchdogIntervalMs)
        assertTrue(PowerMode.SAVER.playbackSettleMs > PowerMode.BALANCED.playbackSettleMs)
    }
}
