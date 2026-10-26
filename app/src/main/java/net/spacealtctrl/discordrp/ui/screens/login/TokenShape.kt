package net.spacealtctrl.discordrp.ui.screens.login

/**
 * Discord tokens are opaque, but they are always a compact run of URL-safe
 * characters. Rejecting obvious garbage before a handshake keeps pasted text
 * from being sent to Discord or stored.
 */
internal object TokenShape {
    private val SHAPE = Regex("[A-Za-z0-9._-]{20,200}")

    fun isPlausible(raw: String?): Boolean {
        val value = raw?.trim()?.removeSurrounding("\"")?.trim() ?: return false
        return SHAPE.matches(value)
    }
}
