package net.spacealtctrl.discordrp.ui.screens.login

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenShapeTest {
    @Test
    fun acceptsDiscordStyleTokens() {
        assertTrue(TokenShape.isPlausible("A".repeat(24) + "." + "B".repeat(6) + "." + "C".repeat(38)))
        assertTrue(TokenShape.isPlausible("mfa." + "A".repeat(84)))
        assertTrue(TokenShape.isPlausible("\"" + "a".repeat(40) + "\""))
        assertTrue(TokenShape.isPlausible("  " + "a".repeat(40) + "  "))
    }

    @Test
    fun rejectsObviousGarbage() {
        assertFalse(TokenShape.isPlausible(null))
        assertFalse(TokenShape.isPlausible(""))
        assertFalse(TokenShape.isPlausible("short"))
        assertFalse(TokenShape.isPlausible("token with spaces and enough length"))
        assertFalse(TokenShape.isPlausible("A".repeat(201)))
        assertFalse(TokenShape.isPlausible("line\nbreak"))
    }
}
