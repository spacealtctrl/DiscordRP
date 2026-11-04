package net.spacealtctrl.discordrp.presence

internal object HostedIcons {
    private const val YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"
    private const val YOUTUBE = "com.google.android.youtube"

    fun nameFor(packageName: String): String = when {
        packageName.endsWith(".android.apps.youtube.music") -> YOUTUBE_MUSIC
        packageName.endsWith(".android.youtube") -> YOUTUBE
        else -> packageName
    }
}
