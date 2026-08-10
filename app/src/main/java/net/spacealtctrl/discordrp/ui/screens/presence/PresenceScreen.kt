package net.spacealtctrl.discordrp.ui.screens.presence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.settings.Mood
import net.spacealtctrl.discordrp.ui.kit.AppScreen
import net.spacealtctrl.discordrp.ui.kit.Cluster
import net.spacealtctrl.discordrp.ui.kit.EmptyCard
import net.spacealtctrl.discordrp.ui.kit.MoodDot
import net.spacealtctrl.discordrp.ui.kit.PageTitle
import net.spacealtctrl.discordrp.ui.kit.PickRow
import net.spacealtctrl.discordrp.ui.kit.RowRule
import net.spacealtctrl.discordrp.ui.kit.SearchBox
import net.spacealtctrl.discordrp.ui.kit.SectionHeader
import net.spacealtctrl.discordrp.ui.kit.ShimmerBlock
import net.spacealtctrl.discordrp.ui.kit.ThumbSwitch
import net.spacealtctrl.discordrp.ui.kit.ToggleRow
import net.spacealtctrl.discordrp.ui.kit.moodLabel
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

@Composable
fun PresenceScreen(viewModel: PresenceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()

    AppScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Look.gaps.gutter,
                end = Look.gaps.gutter,
                bottom = Look.gaps.vast,
            ),
        ) {
            item {
                PageTitle(
                    title = stringResource(R.string.presence_title),
                    subtitle = stringResource(R.string.presence_subtitle),
                )
            }

            item {
                Cluster {
                    ToggleRow(
                        title = stringResource(R.string.presence_master_toggle),
                        subtitle = stringResource(R.string.presence_master_blurb),
                        icon = Icons.Rounded.GraphicEq,
                        checked = state.sharing,
                        onToggle = { viewModel.setSharing(!state.sharing) },
                    )
                }
            }

            item {
                SectionHeader(stringResource(R.string.presence_mood_header))
                Cluster {
                    Mood.entries.forEachIndexed { index, mood ->
                        PickRow(
                            title = moodLabel(mood),
                            leading = { MoodDot(mood, size = 16.dp) },
                            selected = state.mood == mood,
                            onPick = { viewModel.pickMood(mood) },
                        )
                        if (index != Mood.entries.lastIndex) RowRule(inset = false)
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.presence_device_header))
                Cluster {
                    ToggleRow(
                        title = stringResource(R.string.presence_mobile_toggle),
                        subtitle = stringResource(R.string.presence_mobile_blurb),
                        icon = Icons.Rounded.PhoneAndroid,
                        checked = state.mobileBadge,
                        onToggle = { viewModel.setMobileBadge(!state.mobileBadge) },
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = state.sharing,
                    enter = fadeIn(tween(Pace.QUICK)) +
                        expandVertically(tween(Pace.EASY, easing = Pace.settle)),
                    exit = fadeOut(tween(Pace.BLINK)) +
                        shrinkVertically(tween(Pace.QUICK, easing = Pace.dart)),
                ) {
                    Column {
                        SectionHeader(stringResource(R.string.presence_facets_header))
                        Cluster {
                            ShareFacet.entries.forEachIndexed { index, facet ->
                                ToggleRow(
                                    title = stringResource(facet.labelRes()),
                                    icon = facet.icon(),
                                    checked = state.facets[facet] == true,
                                    onToggle = { viewModel.flipFacet(facet) },
                                )
                                if (index != ShareFacet.entries.lastIndex) RowRule()
                            }
                        }
                    }
                }
            }

            if (state.sharing) {
                item {
                    SectionHeader(stringResource(R.string.presence_details_header))
                    Cluster {
                        ToggleRow(
                            title = stringResource(R.string.presence_tidy_toggle),
                            subtitle = stringResource(R.string.presence_tidy_blurb),
                            icon = Icons.Rounded.AutoFixHigh,
                            checked = state.tidyMetadata,
                            onToggle = { viewModel.setTidyMetadata(!state.tidyMetadata) },
                        )
                        AnimatedVisibility(
                            visible = state.tidyMetadata,
                            enter = fadeIn(tween(Pace.QUICK)) +
                                expandVertically(tween(Pace.EASY, easing = Pace.settle)),
                            exit = fadeOut(tween(Pace.BLINK)) +
                                shrinkVertically(tween(Pace.QUICK, easing = Pace.dart)),
                        ) {
                            Column {
                                RowRule()
                                ToggleRow(
                                    title = stringResource(R.string.presence_deezer_toggle),
                                    subtitle = stringResource(R.string.presence_deezer_blurb),
                                    icon = Icons.Rounded.Public,
                                    checked = state.deezerFallback,
                                    onToggle = { viewModel.setDeezerFallback(!state.deezerFallback) },
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        stringResource(R.string.presence_players_header),
                        trailing = {
                            Text(
                                text = state.chosenCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Look.palette.inkGhost,
                            )
                        },
                    )
                    SearchBox(
                        value = query,
                        onValueChange = { viewModel.query.value = it },
                        placeholder = stringResource(R.string.presence_players_search),
                        modifier = Modifier.padding(bottom = Look.gaps.room),
                    )
                }

                when {
                    state.loadingPlayers -> item {
                        Column {
                            repeat(5) {
                                ShimmerBlock(
                                    Modifier
                                        .padding(vertical = Look.gaps.snug)
                                        .fillMaxWidth(),
                                    height = 44.dp,
                                )
                            }
                        }
                    }
                    state.players.isEmpty() -> item {
                        EmptyCard(
                            title = stringResource(R.string.presence_players_empty_title),
                            body = stringResource(R.string.presence_players_empty_body),
                        )
                    }
                    else -> items(
                        count = state.players.size,
                        key = { state.players[it].packageName },
                    ) { index ->
                        PlayerRow(
                            player = state.players[index],
                            onToggle = viewModel::flipPlayer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    player: PlayerChoice,
    onToggle: (String) -> Unit,
) {
    val context = LocalContext.current
    val icon = remember(player.packageName) {
        runCatching { context.packageManager.getApplicationIcon(player.packageName) }.getOrNull()
    }
    Cluster(Modifier.padding(bottom = Look.gaps.snug)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onToggle(player.packageName) }
                .padding(horizontal = Look.gaps.room, vertical = Look.gaps.cozy),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = Look.gaps.room)
                    .size(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = player.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Look.palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = player.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Look.palette.inkGhost,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ThumbSwitch(checked = player.chosen)
        }
    }
}

private fun ShareFacet.labelRes(): Int = when (this) {
    ShareFacet.ARTIST -> R.string.facet_artist
    ShareFacet.ALBUM -> R.string.facet_album
    ShareFacet.TIMELINE -> R.string.facet_timeline
    ShareFacet.PLAYBACK_GLYPH -> R.string.facet_playback_glyph
    ShareFacet.APP_ICON -> R.string.facet_app_icon
    ShareFacet.HIDE_PAUSED -> R.string.facet_hide_paused
    ShareFacet.SONG_AS_TITLE -> R.string.facet_song_as_title
    ShareFacet.ARTIST_AS_TITLE -> R.string.facet_artist_as_title
}

private fun ShareFacet.icon(): ImageVector = when (this) {
    ShareFacet.ARTIST -> Icons.Rounded.MusicNote
    ShareFacet.ALBUM -> Icons.Rounded.Album
    ShareFacet.TIMELINE -> Icons.Rounded.Schedule
    ShareFacet.PLAYBACK_GLYPH -> Icons.Rounded.PlayCircle
    ShareFacet.APP_ICON -> Icons.Rounded.Apps
    ShareFacet.HIDE_PAUSED -> Icons.Rounded.PauseCircle
    ShareFacet.SONG_AS_TITLE -> Icons.Rounded.Title
    ShareFacet.ARTIST_AS_TITLE -> Icons.Rounded.Title
}
