package net.spacealtctrl.discordrp.ui.screens.cover

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.ui.kit.AppScreen
import net.spacealtctrl.discordrp.ui.kit.ArcSpinner
import net.spacealtctrl.discordrp.ui.kit.PageTitle
import net.spacealtctrl.discordrp.ui.kit.RoundIconButton
import net.spacealtctrl.discordrp.ui.theme.Look

@Composable
fun AddAlbumScreen(
    onBack: () -> Unit,
    viewModel: AddAlbumViewModel = hiltViewModel(),
) {
    val stage by viewModel.stage.collectAsState()
    val context = LocalContext.current
    val sentMessage = stringResource(R.string.album_add_sent)

    LaunchedEffect(stage) {
        if (stage == AddAlbumStage.Sent) {
            Toast.makeText(context, sentMessage, Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    AppScreen {
        Column(Modifier.fillMaxSize()) {
            PageTitle(
                title = stringResource(R.string.album_add_title),
                modifier = Modifier.padding(horizontal = Look.gaps.gutter),
                subtitle = viewModel.track?.let { listOfNotNull(it.album, it.artist).joinToString(" - ") },
                leading = {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        onClick = onBack,
                    )
                },
            )
            when (val current = stage) {
                AddAlbumStage.Preparing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ArcSpinner()
                }
                AddAlbumStage.Failed -> Text(
                    text = stringResource(R.string.album_add_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Look.palette.inkFaint,
                    modifier = Modifier.padding(horizontal = Look.gaps.gutter),
                )
                is AddAlbumStage.Ready -> ReleasePages(
                    ready = current,
                    onReleaseAdded = viewModel::releaseAdded,
                    onUploaded = viewModel::uploaded,
                    modifier = Modifier.fillMaxSize(),
                )
                AddAlbumStage.Sent -> Unit
            }
        }
    }
}

private class ReleaseProgress {
    var seeded = false
    var releaseId: String? = null
}

@Composable
private fun ReleasePages(
    ready: AddAlbumStage.Ready,
    onReleaseAdded: (String) -> Unit,
    onUploaded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { ReleaseProgress() }
    MusicBrainzWeb(
        startUrl = ReleaseSeed.ADD_URL,
        modifier = modifier,
        onIntercept = { view, url ->
            val releaseId = ReleaseSeed.addedRelease(url) ?: return@MusicBrainzWeb false
            progress.releaseId = releaseId
            onReleaseAdded(releaseId)
            view.loadUrl(CoverWebPolicy.addCoverUrl(releaseId))
            true
        },
        onPageFinished = { view, url ->
            val releaseId = progress.releaseId
            when {
                releaseId == null && !progress.seeded && CoverWebPolicy.isReleaseEditor(url) -> {
                    progress.seeded = true
                    view.postUrl(ReleaseSeed.ADD_URL, ready.seedBody.toByteArray(Charsets.UTF_8))
                }
                releaseId != null && CoverWebPolicy.isUploaded(url, releaseId) -> onUploaded()
                releaseId != null && CoverWebPolicy.isUploadForm(url, releaseId) ->
                    view.evaluateJavascript(ready.coverScript, null)
            }
        },
    )
}
