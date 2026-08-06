package net.spacealtctrl.discordrp.ui.screens.you

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.spacealtctrl.discordrp.discord.ProfileFetch
import net.spacealtctrl.discordrp.discord.ProfileRepository
import net.spacealtctrl.discordrp.presence.BridgeSnapshot
import net.spacealtctrl.discordrp.presence.BridgeFeed
import net.spacealtctrl.discordrp.presence.PresenceComposer
import net.spacealtctrl.discordrp.settings.Mood
import net.spacealtctrl.discordrp.settings.Stash
import net.spacealtctrl.discordrp.update.AppUpdate
import net.spacealtctrl.discordrp.update.UpdateHub
import net.spacealtctrl.discordrp.update.UpdateState
import javax.inject.Inject

data class YouUiState(
    val signedIn: Boolean = false,
    val fetch: ProfileFetch = ProfileFetch.Loading,
    val snapshot: BridgeSnapshot = BridgeSnapshot(),
    val mood: Mood = Mood.ONLINE,
    val mobileBadge: Boolean = true,
    val showPlaybackGlyph: Boolean = false,
    val showAppIcon: Boolean = true,
    val cardTitle: String? = null,
)

@HiltViewModel
class YouViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val stash: Stash,
    private val updateHub: UpdateHub,
    profileRepository: ProfileRepository,
) : ViewModel() {
    val updates: StateFlow<UpdateState> = updateHub.state

    val updateOutcomes = updateHub.outcomes

    fun checkForUpdates() = updateHub.check()

    fun installUpdate(update: AppUpdate) = updateHub.install(update)

    val state: StateFlow<YouUiState> = combine(
        profileRepository.profile(),
        BridgeFeed.snapshot,
        stash.revisions,
    ) { fetch, snapshot, _ ->
        YouUiState(
            signedIn = stash.signedIn,
            fetch = fetch,
            snapshot = snapshot,
            mood = stash.mood,
            mobileBadge = stash.mobileBadge,
            showPlaybackGlyph = stash.showPlaybackGlyph,
            showAppIcon = stash.showAppIcon,
            cardTitle = snapshot.track?.let { PresenceComposer.cardName(it, stash) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YouUiState())

    fun signOut() {
        appContext.getSystemService(ActivityManager::class.java)
            ?.clearApplicationUserData()
    }
}
