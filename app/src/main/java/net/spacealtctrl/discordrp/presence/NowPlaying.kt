package net.spacealtctrl.discordrp.presence

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class NowPlaying(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val appLabel: String,
    val appPackage: String,
    val artwork: Bitmap? = null,
    val playing: Boolean = false,
    val stopped: Boolean = false,
    val startedAtMillis: Long? = null,
    val endsAtMillis: Long? = null,
    val durationMillis: Long? = null,
    val releaseGroupId: String? = null,
    val coverUrl: String? = null,
) {
    val hasTimeline: Boolean get() = startedAtMillis != null
}

@Immutable
data class BridgeSnapshot(
    val linked: Boolean = false,
    val track: NowPlaying? = null,
)
