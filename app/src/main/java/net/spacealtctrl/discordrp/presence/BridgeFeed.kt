package net.spacealtctrl.discordrp.presence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object BridgeFeed {
    private val _snapshot = MutableStateFlow(BridgeSnapshot())
    val snapshot: StateFlow<BridgeSnapshot> = _snapshot.asStateFlow()

    fun trackChanged(track: NowPlaying?) {
        _snapshot.update { it.copy(track = track) }
    }

    fun linkChanged(linked: Boolean) {
        _snapshot.update { it.copy(linked = linked) }
    }

    fun serviceStopped() {
        _snapshot.value = BridgeSnapshot()
    }
}
