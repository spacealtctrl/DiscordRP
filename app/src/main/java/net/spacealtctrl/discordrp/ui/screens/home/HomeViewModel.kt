package net.spacealtctrl.discordrp.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.spacealtctrl.discordrp.discord.ProfileRepository
import net.spacealtctrl.discordrp.presence.BridgeSnapshot
import net.spacealtctrl.discordrp.service.BridgeService
import net.spacealtctrl.discordrp.settings.Mood
import net.spacealtctrl.discordrp.settings.Stash
import net.spacealtctrl.discordrp.update.UpdateHub
import net.spacealtctrl.discordrp.update.UpdateState
import javax.inject.Inject

data class HomeUiState(
    val running: Boolean = false,
    val linked: Boolean = false,
    val snapshot: BridgeSnapshot = BridgeSnapshot(),
    val mood: Mood = Mood.ONLINE,
    val mobileBadge: Boolean = true,
    val showPlaybackGlyph: Boolean = false,
    val showAppIcon: Boolean = true,
    val presenceEnabled: Boolean = true,
    val signedIn: Boolean = false,
    val startOnBoot: Boolean = false,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val updateVersion: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val stash: Stash,
    private val updateHub: UpdateHub,
    profileRepository: ProfileRepository,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        BridgeService.alive,
        net.spacealtctrl.discordrp.presence.BridgeFeed.snapshot,
        stash.revisions,
        updateHub.state,
    ) { running, snapshot, _, updateState ->
        val profile = profileRepository.cached()
        HomeUiState(
            running = running,
            linked = snapshot.linked,
            snapshot = snapshot,
            mood = stash.mood,
            mobileBadge = stash.mobileBadge,
            showPlaybackGlyph = stash.showPlaybackGlyph,
            showAppIcon = stash.showAppIcon,
            presenceEnabled = stash.presenceEnabled,
            signedIn = stash.signedIn,
            startOnBoot = stash.startOnBoot,
            displayName = profile?.displayName(),
            avatarUrl = profile?.avatarUrl(160),
            updateVersion = (updateState as? UpdateState.Available)?.update?.version,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleBridge() {
        val intent = Intent(appContext, BridgeService::class.java)
        if (BridgeService.alive.value) {
            appContext.stopService(intent)
        } else {
            ContextCompat.startForegroundService(appContext, intent)
        }
    }

    fun pickMood(mood: Mood) {
        stash.mood = mood
        nudgeBridge()
    }

    fun setStartOnBoot(on: Boolean) {
        stash.startOnBoot = on
    }

    fun installUpdate() {
        (updateHub.state.value as? UpdateState.Available)?.let { updateHub.install(it.update) }
    }

    private fun nudgeBridge() {
        if (BridgeService.alive.value) {
            appContext.startService(BridgeService.refreshIntent(appContext))
        }
    }
}
