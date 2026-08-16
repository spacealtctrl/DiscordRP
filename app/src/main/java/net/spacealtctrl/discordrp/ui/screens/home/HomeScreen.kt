package net.spacealtctrl.discordrp.ui.screens.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.presence.NowPlaying
import net.spacealtctrl.discordrp.ui.kit.AppScreen
import net.spacealtctrl.discordrp.ui.kit.AvatarBadge
import net.spacealtctrl.discordrp.ui.kit.CalloutBanner
import net.spacealtctrl.discordrp.ui.kit.CalloutTone
import net.spacealtctrl.discordrp.ui.kit.EqualizerBars
import net.spacealtctrl.discordrp.ui.kit.GlowCard
import net.spacealtctrl.discordrp.ui.kit.StatusRibbon
import net.spacealtctrl.discordrp.ui.kit.HeroArt
import net.spacealtctrl.discordrp.ui.kit.MoodSheet
import net.spacealtctrl.discordrp.ui.kit.PageTitle
import net.spacealtctrl.discordrp.ui.kit.SectionHeader
import net.spacealtctrl.discordrp.ui.kit.ThumbSwitch
import net.spacealtctrl.discordrp.ui.kit.TrackTimeline
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

@Composable
fun HomeScreen(
    notificationAccess: State<Boolean>,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var moodSheetOpen by remember { mutableStateOf(false) }

    AppScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Look.gaps.gutter,
                end = Look.gaps.gutter,
                bottom = Look.gaps.vast,
            ),
        ) {
            item(key = "title") {
                PageTitle(
                    eyebrow = stringResource(R.string.app_name),
                    title = state.displayName
                        ?.let { stringResource(R.string.home_greeting, it) }
                        ?: stringResource(R.string.home_greeting_anonymous),
                    modifier = settleIntoPlace(),
                    trailing = {
                        AvatarBadge(
                            avatarUrl = state.avatarUrl,
                            mood = state.mood,
                            mobile = state.mobileBadge,
                            onClick = { moodSheetOpen = true },
                        )
                    },
                )
            }

            state.updateVersion?.let { version ->
                item(key = "update-banner") {
                    CalloutBanner(
                        title = stringResource(R.string.update_banner_title),
                        body = stringResource(R.string.update_banner_body, version),
                        icon = Icons.Rounded.SystemUpdate,
                        actionLabel = stringResource(R.string.update_banner_action),
                        onAction = viewModel::installUpdate,
                        modifier = settleIntoPlace().padding(bottom = Look.gaps.room),
                    )
                }
            }

            if (!notificationAccess.value) {
                item(key = "notice-banner") {
                    CalloutBanner(
                        title = stringResource(R.string.access_banner_title),
                        body = stringResource(R.string.access_banner_body),
                        tone = CalloutTone.Warning,
                        icon = Icons.Rounded.Warning,
                        actionLabel = stringResource(R.string.access_banner_action),
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        },
                        modifier = settleIntoPlace().padding(bottom = Look.gaps.room),
                    )
                }
            }

            item(key = "bridge") {
                BridgeCard(
                    state = state,
                    enabled = state.signedIn && notificationAccess.value,
                    onToggle = viewModel::toggleBridge,
                    modifier = settleIntoPlace(),
                )
            }

            item(key = "now-playing") {
                Column(settleIntoPlace()) {
                    SectionHeader(stringResource(R.string.home_now_playing_header))
                    NowPlayingCard(state = state)
                }
            }

            item(key = "autostart") {
                Column(settleIntoPlace()) {
                    SectionHeader(stringResource(R.string.home_general_header))
                    AutostartCard(
                        on = state.startOnBoot,
                        onToggle = { viewModel.setStartOnBoot(!state.startOnBoot) },
                    )
                }
            }
        }
    }

    if (moodSheetOpen) {
        MoodSheet(
            current = state.mood,
            onPick = { mood ->
                viewModel.pickMood(mood)
                moodSheetOpen = false
            },
            onDismiss = { moodSheetOpen = false },
        )
    }
}

private fun LazyItemScope.settleIntoPlace(): Modifier = Modifier.animateItem(
    fadeInSpec = tween(Pace.EASY, easing = Pace.settle),
    placementSpec = Pace.glidespring(IntOffset.VisibilityThreshold),
    fadeOutSpec = tween(Pace.QUICK, easing = Pace.dart),
)

@Composable
private fun BridgeCard(
    state: HomeUiState,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bodyRes = when {
        state.running && state.linked -> R.string.bridge_card_on_body
        state.running -> R.string.bridge_card_linking_body
        else -> R.string.bridge_card_off_body
    }

    GlowCard(
        modifier = modifier,
        glowing = state.running,
        onClick = if (enabled) onToggle else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = state.running to bodyRes,
                transitionSpec = { Pace.fadeThrough() },
                modifier = Modifier.weight(1f),
                label = "bridgeLabel",
            ) { (running, body) ->
                Column {
                    Text(
                        text = stringResource(
                            if (running) R.string.bridge_card_on_title
                            else R.string.bridge_card_off_title
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Look.palette.ink,
                    )
                    Text(
                        text = stringResource(body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Look.palette.inkFaint,
                        modifier = Modifier.padding(top = Look.gaps.tight),
                    )
                }
            }
            Spacer(Modifier.width(Look.gaps.room))
            ThumbSwitch(checked = state.running, enabled = enabled)
        }
    }
}

@Composable
private fun AutostartCard(on: Boolean, onToggle: () -> Unit) {
    val powerTint by animateColorAsState(
        targetValue = if (on) Look.palette.accent else Look.palette.inkFaint,
        animationSpec = tween(Pace.EASY, easing = Pace.settle),
        label = "powerTint",
    )

    GlowCard(glowing = on, onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = null,
                tint = powerTint,
                modifier = Modifier
                    .padding(end = Look.gaps.room)
                    .size(22.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_autostart_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Look.palette.ink,
                )
                Text(
                    text = stringResource(R.string.home_autostart_blurb),
                    style = MaterialTheme.typography.bodySmall,
                    color = Look.palette.inkFaint,
                    modifier = Modifier.padding(top = Look.gaps.hair),
                )
            }
            Spacer(Modifier.width(Look.gaps.room))
            ThumbSwitch(checked = on)
        }
    }
}

@Composable
private fun NowPlayingCard(state: HomeUiState) {
    val track = state.snapshot.track

    GlowCard(glowing = track != null) {
        AnimatedContent(
            targetState = track,
            transitionSpec = {
                val trackToTrack = initialState != null && targetState != null
                val enter =
                    if (trackToTrack) {
                        fadeIn(tween(Pace.EASY, easing = Pace.settle)) +
                            slideInHorizontally(Pace.glidespring(IntOffset.VisibilityThreshold)) { it / 12 }
                    } else {
                        fadeIn(tween(Pace.EASY, easing = Pace.settle)) +
                            slideInVertically(Pace.pop(IntOffset.VisibilityThreshold)) { it / 14 }
                    }
                val exit =
                    if (trackToTrack) {
                        fadeOut(tween(Pace.QUICK, easing = Pace.dart)) +
                            slideOutHorizontally(tween(Pace.QUICK, easing = Pace.dart)) { -it / 16 }
                    } else {
                        fadeOut(tween(Pace.QUICK, easing = Pace.dart))
                    }
                (enter togetherWith exit).using(
                    SizeTransform(clip = false) { _, _ ->
                        Pace.glidespring(IntSize.VisibilityThreshold)
                    }
                )
            },
            contentKey = { it?.let { t -> t.appPackage + t.title } },
            label = "nowPlaying",
        ) { current ->
            if (current == null) {
                IdleHero(running = state.running, sharing = state.presenceEnabled)
            } else {
                TrackHero(
                    track = current,
                    showGlyph = state.showPlaybackGlyph,
                    showAppIcon = state.showAppIcon,
                )
            }
        }
    }
}

@Composable
private fun TrackHero(track: NowPlaying, showGlyph: Boolean, showAppIcon: Boolean) {
    val context = LocalContext.current
    val appIcon = remember(track.appPackage, showAppIcon) {
        if (!showAppIcon) null
        else runCatching { context.packageManager.getApplicationIcon(track.appPackage) }.getOrNull()
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EqualizerBars(playing = track.playing)
            Text(
                text = stringResource(R.string.hero_listening_on, track.appLabel),
                style = MaterialTheme.typography.labelSmall,
                color = Look.palette.accent,
                modifier = Modifier.padding(start = Look.gaps.snug),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(Look.gaps.room))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroArt(track = track, appIcon = appIcon, showGlyph = showGlyph)
            Column(Modifier.padding(start = Look.gaps.room)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Look.palette.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                track.artist?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Look.palette.inkFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Look.gaps.hair),
                    )
                }
                track.album?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Look.palette.inkGhost,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        TrackTimeline(startMillis = track.startedAtMillis, endMillis = track.endsAtMillis)
    }
}

@Composable
private fun IdleHero(running: Boolean, sharing: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = Look.gaps.open),
        ) {
            StatusRibbon(running = running, sharing = sharing)
            AnimatedContent(
                targetState = running to sharing,
                transitionSpec = { Pace.fadeThrough() },
                label = "idleCopy",
            ) { (isRunning, isSharing) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(
                            when {
                                !isRunning -> R.string.hero_idle_off_title
                                !isSharing -> R.string.hero_idle_private_title
                                else -> R.string.hero_idle_title
                            }
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = Look.palette.ink,
                        modifier = Modifier.padding(top = Look.gaps.cozy),
                    )
                    Text(
                        text = stringResource(
                            when {
                                !isRunning -> R.string.hero_idle_off_body
                                !isSharing -> R.string.hero_idle_private_body
                                else -> R.string.hero_idle_body
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Look.palette.inkGhost,
                        modifier = Modifier.padding(top = Look.gaps.tight),
                    )
                }
            }
        }
    }
}
