package net.spacealtctrl.discordrp.ui.screens.you

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.spacealtctrl.discordrp.R
import androidx.compose.material.icons.rounded.SystemUpdate
import net.spacealtctrl.discordrp.BuildConfig
import net.spacealtctrl.discordrp.discord.ProfileFetch
import net.spacealtctrl.discordrp.update.AppUpdate
import net.spacealtctrl.discordrp.update.UpdateState
import net.spacealtctrl.discordrp.ui.kit.AccentButton
import net.spacealtctrl.discordrp.ui.kit.AppScreen
import net.spacealtctrl.discordrp.ui.kit.PageTitle
import net.spacealtctrl.discordrp.ui.kit.QuietButton
import net.spacealtctrl.discordrp.ui.kit.SectionHeader
import net.spacealtctrl.discordrp.ui.kit.ShimmerBlock
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

private const val REPO_URL = "https://github.com/spacealtctrl/DiscordRP"

@Composable
fun YouScreen(
    onSignIn: () -> Unit,
    viewModel: YouViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val updateState by viewModel.updates.collectAsState()
    var confirmingSignOut by remember { mutableStateOf(false) }
    var awaitingCheck by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.updateOutcomes.collect { outcome ->
            if (!awaitingCheck) return@collect
            awaitingCheck = false
            when (outcome) {
                is UpdateState.UpToDate ->
                    checkResult = context.getString(R.string.update_dialog_current)
                is UpdateState.Failed ->
                    checkResult = context.getString(R.string.update_dialog_failed)
                is UpdateState.Available -> {
                    awaitingCheck = true
                    viewModel.installUpdate(outcome.update)
                }
                else -> Unit
            }
        }
    }

    AppScreen {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    PaddingValues(
                        start = Look.gaps.gutter,
                        end = Look.gaps.gutter,
                        bottom = Look.gaps.vast,
                    )
                ),
        ) {
            PageTitle(
                title = stringResource(R.string.you_title),
                subtitle = if (state.signedIn) {
                    stringResource(R.string.you_subtitle)
                } else {
                    null
                },
            )

            AnimatedContent(
                targetState = state.signedIn,
                transitionSpec = {
                    val swap = if (targetState) {
                        (
                            fadeIn(tween(Pace.EASY, delayMillis = Pace.BLINK, easing = Pace.settle)) +
                                slideInVertically(Pace.glidespring(IntOffset.VisibilityThreshold)) { it / 20 }
                            )
                            .togetherWith(fadeOut(tween(Pace.BLINK, easing = Pace.dart)))
                    } else {
                        fadeIn(tween(Pace.EASY, delayMillis = Pace.BLINK, easing = Pace.settle))
                            .togetherWith(
                                fadeOut(tween(Pace.QUICK, easing = Pace.dart)) +
                                    slideOutVertically(tween(Pace.QUICK, easing = Pace.dart)) { it / 12 }
                            )
                    }
                    swap.using(
                        SizeTransform(clip = false) { _, _ ->
                            Pace.glidespring(IntSize.VisibilityThreshold)
                        }
                    )
                },
                label = "signedInSwap",
            ) { signedIn ->
                if (signedIn) {
                    ProfileSection(state = state)
                } else {
                    SignedOutHero(onSignIn = onSignIn)
                }
            }

            if (state.signedIn) {
                SectionHeader(stringResource(R.string.you_app_header))
                UpdateBlock(
                    state = updateState,
                    onCheck = {
                        awaitingCheck = true
                        viewModel.checkForUpdates()
                    },
                    onInstall = viewModel::installUpdate,
                )

                SectionHeader(stringResource(R.string.you_session_header))
                QuietButton(
                    text = stringResource(R.string.you_sign_out),
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    destructive = true,
                    onClick = { confirmingSignOut = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.you_sign_out_blurb),
                    style = MaterialTheme.typography.bodySmall,
                    color = Look.palette.inkGhost,
                    modifier = Modifier.padding(
                        top = Look.gaps.cozy,
                        start = Look.gaps.tight,
                        end = Look.gaps.tight,
                    ),
                )
            }

            Text(
                text = stringResource(R.string.you_footer_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium.copy(
                    textDecoration = TextDecoration.Underline,
                ),
                color = Look.palette.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Look.gaps.vast)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))
                        )
                    }
                    .padding(vertical = Look.gaps.snug),
            )
        }
    }

    checkResult?.let { body ->
        AlertDialog(
            onDismissRequest = { checkResult = null },
            containerColor = Look.palette.panel,
            title = {
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    color = Look.palette.ink,
                )
            },
            text = { Text(text = body, color = Look.palette.inkFaint) },
            confirmButton = {
                TextButton(onClick = { checkResult = null }) {
                    Text(
                        text = stringResource(R.string.common_ok),
                        color = Look.palette.accent,
                    )
                }
            },
        )
    }

    if (confirmingSignOut) {
        AlertDialog(
            onDismissRequest = { confirmingSignOut = false },
            containerColor = Look.palette.panel,
            title = {
                Text(
                    text = stringResource(R.string.you_sign_out_confirm_title),
                    color = Look.palette.ink,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.you_sign_out_confirm_body),
                    color = Look.palette.inkFaint,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::signOut) {
                    Text(
                        text = stringResource(R.string.you_sign_out),
                        color = Look.palette.danger,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingSignOut = false }) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = Look.palette.inkFaint,
                    )
                }
            },
        )
    }
}

@Composable
private fun ProfileSection(state: YouUiState) {
    AnimatedContent(
        targetState = state.fetch,
        transitionSpec = { Pace.fadeThrough() },
        contentKey = { it::class },
        label = "profileFetch",
    ) { fetch ->
        when (fetch) {
            is ProfileFetch.Loading -> ProfileSkeleton()
            is ProfileFetch.Fresh -> ProfileCard(
                profile = fetch.profile,
                track = state.snapshot.track,
                mood = state.mood,
                mobile = state.mobileBadge,
                showGlyph = state.showPlaybackGlyph,
                showAppIcon = state.showAppIcon,
                cardTitle = state.cardTitle,
            )
            is ProfileFetch.Stale -> Column {
                ProfileCard(
                    profile = fetch.profile,
                    track = state.snapshot.track,
                    mood = state.mood,
                    mobile = state.mobileBadge,
                    showGlyph = state.showPlaybackGlyph,
                    showAppIcon = state.showAppIcon,
                    cardTitle = state.cardTitle,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Look.gaps.room),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = Look.palette.inkGhost,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.you_offline_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Look.palette.inkGhost,
                        modifier = Modifier.padding(start = Look.gaps.snug),
                    )
                }
            }
        }
    }
}

@Composable
private fun SignedOutHero(onSignIn: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Look.gaps.vast),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Look.palette.panel),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_discord_logo),
                contentDescription = null,
                tint = Look.palette.accent,
                modifier = Modifier.size(44.dp),
            )
        }
        Text(
            text = stringResource(R.string.you_signed_out_title),
            style = MaterialTheme.typography.titleLarge,
            color = Look.palette.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Look.gaps.open),
        )
        Text(
            text = stringResource(R.string.you_signed_out_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Look.palette.inkGhost,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Look.gaps.snug),
        )
        AccentButton(
            text = stringResource(R.string.login_cta),
            onClick = onSignIn,
            modifier = Modifier.padding(top = Look.gaps.wide),
        )
    }
}

@Composable
private fun ProfileSkeleton() {
    Column {
        ShimmerBlock(
            Modifier.fillMaxWidth(),
            height = 118.dp,
            shape = Look.corners.card,
        )
        ShimmerBlock(
            Modifier
                .padding(top = Look.gaps.room)
                .fillMaxWidth(0.5f),
            height = 24.dp,
        )
        ShimmerBlock(
            Modifier
                .padding(top = Look.gaps.snug)
                .fillMaxWidth(0.3f),
            height = 14.dp,
        )
    }
}

@Composable
private fun UpdateBlock(
    state: UpdateState,
    onCheck: () -> Unit,
    onInstall: (AppUpdate) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = { Pace.fadeThrough() },
        contentKey = { it::class },
        label = "updateAction",
    ) { update ->
        when (update) {
            is UpdateState.Available -> AccentButton(
                text = stringResource(R.string.update_action, update.update.version),
                icon = Icons.Rounded.SystemUpdate,
                onClick = { onInstall(update.update) },
                modifier = Modifier.fillMaxWidth(),
            )
            is UpdateState.Downloading -> QuietButton(
                text = stringResource(R.string.update_downloading),
                icon = Icons.Rounded.SystemUpdate,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            else -> QuietButton(
                text = stringResource(R.string.you_check_updates),
                icon = Icons.Rounded.SystemUpdate,
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
