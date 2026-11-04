package net.spacealtctrl.discordrp.ui.screens.cover

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.spacealtctrl.discordrp.presence.ArtRegistry
import net.spacealtctrl.discordrp.presence.BridgeFeed
import net.spacealtctrl.discordrp.presence.CoverLookup
import net.spacealtctrl.discordrp.presence.CoverNames
import net.spacealtctrl.discordrp.presence.CoverOffer
import net.spacealtctrl.discordrp.presence.DeezerCovers
import net.spacealtctrl.discordrp.presence.DeezerPicks
import net.spacealtctrl.discordrp.presence.NowPlaying
import net.spacealtctrl.discordrp.presence.PublishedArt
import net.spacealtctrl.discordrp.service.BridgeService
import javax.inject.Inject

sealed interface AddAlbumStage {
    data object Preparing : AddAlbumStage

    data class Ready(val seedBody: String, val coverScript: String) : AddAlbumStage

    data object Failed : AddAlbumStage

    data object Sent : AddAlbumStage
}

@HiltViewModel
class AddAlbumViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val coverLookup: CoverLookup,
    private val deezerCovers: DeezerCovers,
    private val artRegistry: ArtRegistry,
) : ViewModel() {
    val track: NowPlaying? = BridgeFeed.snapshot.value.track
    private val art: PublishedArt? = BridgeFeed.snapshot.value.art

    private val _stage = MutableStateFlow<AddAlbumStage>(AddAlbumStage.Preparing)
    val stage: StateFlow<AddAlbumStage> = _stage.asStateFlow()

    private var newGroup: Deferred<String?>? = null

    init {
        viewModelScope.launch { _stage.value = prepare() }
    }

    fun releaseAdded(releaseId: String) {
        if (newGroup != null) return
        newGroup = viewModelScope.async(Dispatchers.IO) { coverLookup.groupOfRelease(releaseId) }
    }

    fun uploaded() {
        if (_stage.value == AddAlbumStage.Sent) return
        _stage.value = AddAlbumStage.Sent
        val key = art?.coverKey ?: return
        viewModelScope.launch {
            val group = newGroup?.await() ?: return@launch
            artRegistry.noteNewRelease(key, group)
            if (BridgeService.alive.value) {
                appContext.startService(BridgeService.recheckArtIntent(appContext, key))
            }
        }
    }

    private suspend fun prepare(): AddAlbumStage = withContext(Dispatchers.IO) {
        track?.takeIf { CoverOffer.canAddAlbum(it, art) } ?: return@withContext AddAlbumStage.Failed
        art?.deezerAlbumId?.let { fromDeezer(it) } ?: AddAlbumStage.Failed
    }

    private suspend fun fromDeezer(albumId: Long): AddAlbumStage.Ready? {
        val (album, tracks) = deezerCovers.albumDetails(albumId) ?: return null
        val coverUrl = DeezerPicks.cover(album.coverXl, album.coverBig) ?: return null
        val image = deezerCovers.image(coverUrl) ?: return null
        val script = CoverAttach.script(Base64.encodeToString(image, Base64.NO_WRAP)) ?: return null
        val artistMbid = album.artist?.name?.let { soleArtist(it) }
        return AddAlbumStage.Ready(ReleaseSeed.body(ReleaseSeed.fields(album, tracks, artistMbid)), script)
    }

    private suspend fun soleArtist(name: String): String? =
        if (CoverNames.credits(name).size == 1) coverLookup.artistFor(name) else null
}
