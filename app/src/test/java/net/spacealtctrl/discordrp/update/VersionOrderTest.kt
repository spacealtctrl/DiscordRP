package net.spacealtctrl.discordrp.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionOrderTest {
    @Test
    fun `newer patch wins`() {
        assertTrue(VersionOrder.isNewer("1.0.1", "1.0.0"))
    }

    @Test
    fun `newer minor wins`() {
        assertTrue(VersionOrder.isNewer("1.1.0", "1.0.9"))
    }

    @Test
    fun `newer major wins`() {
        assertTrue(VersionOrder.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun `same version is not newer`() {
        assertFalse(VersionOrder.isNewer("1.1.0", "1.1.0"))
    }

    @Test
    fun `older version is not newer`() {
        assertFalse(VersionOrder.isNewer("1.0.0", "1.1.0"))
    }

    @Test
    fun `leading v prefix is ignored`() {
        assertTrue(VersionOrder.isNewer("v1.2.0", "1.1.0"))
        assertFalse(VersionOrder.isNewer("v1.1.0", "v1.1.0"))
    }

    @Test
    fun `missing segments count as zero`() {
        assertTrue(VersionOrder.isNewer("1.1", "1.0.9"))
        assertFalse(VersionOrder.isNewer("1.1", "1.1.0"))
    }

    @Test
    fun `prerelease suffix is stripped`() {
        assertTrue(VersionOrder.isNewer("1.2.0-beta", "1.1.0"))
    }
}
