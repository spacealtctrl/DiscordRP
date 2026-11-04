package net.spacealtctrl.discordrp.ui.screens.cover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.spacealtctrl.discordrp.presence.BridgeFeed
import net.spacealtctrl.discordrp.presence.CoverLookup
import net.spacealtctrl.discordrp.presence.CoverOffer
import net.spacealtctrl.discordrp.presence.NowPlaying
import net.spacealtctrl.discordrp.presence.PublishedArt
import net.spacealtctrl.discordrp.service.BridgeService
import javax.inject.Inject

sealed interface AddCoverStage {
    data object Preparing : AddCoverStage

    data class Ready(val pageUrl: String, val releaseId: String, val script: String) : AddCoverStage

    data object Failed : AddCoverStage

    data object Sent : AddCoverStage
}

@HiltViewModel
class AddCoverViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val coverLookup: CoverLookup,
) : ViewModel() {
    val track: NowPlaying? = BridgeFeed.snapshot.value.track
    private val art: PublishedArt? = BridgeFeed.snapshot.value.art

    private val _stage = MutableStateFlow<AddCoverStage>(AddCoverStage.Preparing)
    val stage: StateFlow<AddCoverStage> = _stage.asStateFlow()

    init {
        viewModelScope.launch { _stage.value = prepare() }
    }

    fun uploaded() {
        if (_stage.value == AddCoverStage.Sent) return
        _stage.value = AddCoverStage.Sent
        val key = art?.coverKey ?: return
        if (BridgeService.alive.value) {
            appContext.startService(BridgeService.recheckArtIntent(appContext, key))
        }
    }

    private suspend fun prepare(): AddCoverStage = withContext(Dispatchers.IO) {
        val song = track ?: return@withContext AddCoverStage.Failed
        val artwork = song.artwork ?: return@withContext AddCoverStage.Failed
        val groupId = art?.missingGroupId?.takeIf { CoverOffer.canAdd(song, art) }
            ?: return@withContext AddCoverStage.Failed
        val releaseId = coverLookup.uploadRelease(groupId) ?: return@withContext AddCoverStage.Failed
        val script = CoverAttach.script(CoverJpeg.base64(artwork)) ?: return@withContext AddCoverStage.Failed
        AddCoverStage.Ready(CoverWebPolicy.addCoverUrl(releaseId), releaseId, script)
    }
}
