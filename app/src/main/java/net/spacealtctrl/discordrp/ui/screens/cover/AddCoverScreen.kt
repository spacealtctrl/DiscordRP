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
fun AddCoverScreen(
    onBack: () -> Unit,
    viewModel: AddCoverViewModel = hiltViewModel(),
) {
    val stage by viewModel.stage.collectAsState()
    val context = LocalContext.current
    val sentMessage = stringResource(R.string.cover_add_sent)

    LaunchedEffect(stage) {
        if (stage == AddCoverStage.Sent) {
            Toast.makeText(context, sentMessage, Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    AppScreen {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = Look.gaps.gutter)) {
                PageTitle(
                    title = stringResource(R.string.cover_add_title),
                    subtitle = viewModel.track?.let { listOfNotNull(it.album, it.artist).joinToString(" - ") },
                    leading = {
                        RoundIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            onClick = onBack,
                        )
                    },
                )
                Text(
                    text = stringResource(R.string.cover_add_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = Look.palette.inkFaint,
                    modifier = Modifier.padding(bottom = Look.gaps.room),
                )
            }
            when (val current = stage) {
                AddCoverStage.Preparing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ArcSpinner()
                }
                AddCoverStage.Failed -> Text(
                    text = stringResource(R.string.cover_add_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Look.palette.inkFaint,
                    modifier = Modifier.padding(horizontal = Look.gaps.gutter),
                )
                is AddCoverStage.Ready -> UploadPage(
                    ready = current,
                    onUploaded = viewModel::uploaded,
                    modifier = Modifier.fillMaxSize(),
                )
                AddCoverStage.Sent -> Unit
            }
        }
    }
}

@Composable
private fun UploadPage(
    ready: AddCoverStage.Ready,
    onUploaded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MusicBrainzWeb(
        startUrl = ready.pageUrl,
        modifier = modifier,
        onPageFinished = { view, url ->
            when {
                CoverWebPolicy.isUploaded(url, ready.releaseId) -> onUploaded()
                CoverWebPolicy.isUploadForm(url, ready.releaseId) -> view.evaluateJavascript(ready.script, null)
            }
        },
    )
}
