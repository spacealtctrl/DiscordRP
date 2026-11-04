package net.spacealtctrl.discordrp.ui.screens.login

internal object TokenShape {
    private val SHAPE = Regex("[A-Za-z0-9._-]{20,200}")

    fun isPlausible(raw: String?): Boolean {
        val value = raw?.trim()?.removeSurrounding("\"")?.trim() ?: return false
        return SHAPE.matches(value)
    }
}
