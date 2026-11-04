package net.spacealtctrl.discordrp.presence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object BridgeFeed {
    private val _snapshot = MutableStateFlow(BridgeSnapshot())
    val snapshot: StateFlow<BridgeSnapshot> = _snapshot.asStateFlow()

    fun trackChanged(track: NowPlaying?) {
        _snapshot.update { current ->
            val keepArt = track != null && track.isSameSong(current.track)
            current.copy(track = track, art = if (keepArt) current.art else null)
        }
    }

    fun artPublished(track: NowPlaying, art: PublishedArt) {
        _snapshot.update { current ->
            if (track.isSameSong(current.track)) current.copy(art = art) else current
        }
    }

    fun artForgotten() {
        _snapshot.update { it.copy(art = null) }
    }

    fun linkChanged(linked: Boolean) {
        _snapshot.update { it.copy(linked = linked) }
    }

    fun serviceStopped() {
        _snapshot.value = BridgeSnapshot()
    }
}
