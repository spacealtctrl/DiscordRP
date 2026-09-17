package net.spacealtctrl.discordrp.ui.screens.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.discord.DiscordApi
import net.spacealtctrl.discordrp.discord.GuildSummary
import net.spacealtctrl.discordrp.settings.AlertScope
import net.spacealtctrl.discordrp.settings.Stash
import net.spacealtctrl.discordrp.ui.kit.AppScreen
import net.spacealtctrl.discordrp.ui.kit.Cluster
import net.spacealtctrl.discordrp.ui.kit.PageTitle
import net.spacealtctrl.discordrp.ui.kit.PickRow
import net.spacealtctrl.discordrp.ui.kit.RowRule
import net.spacealtctrl.discordrp.ui.kit.SectionHeader
import net.spacealtctrl.discordrp.ui.kit.TapRow
import net.spacealtctrl.discordrp.ui.kit.ToggleRow
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace
import javax.inject.Inject

data class AlertsUiState(
    val enabled: Boolean = true,
    val scope: AlertScope = AlertScope.EVERYTHING,
    val skipBots: Boolean = false,
    val clearReadElsewhere: Boolean = false,
    val mutedGuilds: Set<String> = emptySet(),
)

sealed interface ServerListState {
    data object Loading : ServerListState

    data object Failed : ServerListState

    data class Ready(val guilds: List<GuildSummary>) : ServerListState
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val stash: Stash,
    private val api: DiscordApi,
) : ViewModel() {
    private val _servers = MutableStateFlow<ServerListState>(ServerListState.Loading)
    val servers: StateFlow<ServerListState> = _servers.asStateFlow()
    val state: StateFlow<AlertsUiState> = stash.revisions
        .map {
            AlertsUiState(
                enabled = stash.alertsEnabled,
                scope = stash.alertScope,
                skipBots = stash.skipBots,
                clearReadElsewhere = stash.clearReadElsewhere,
                mutedGuilds = stash.mutedGuilds,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    fun setEnabled(on: Boolean) {
        stash.alertsEnabled = on
    }

    fun setScope(scope: AlertScope) {
        stash.alertScope = scope
    }

    fun setSkipBots(on: Boolean) {
        stash.skipBots = on
    }

    fun setClearReadElsewhere(on: Boolean) {
        stash.clearReadElsewhere = on
    }

    fun loadServers() {
        if (_servers.value is ServerListState.Ready) return
        _servers.value = ServerListState.Loading
        viewModelScope.launch {
            _servers.value = api.myGuilds().fold(
                onSuccess = { guilds ->
                    ServerListState.Ready(
                        guilds.filter { it.id != null }
                            .sortedBy { it.name.orEmpty().lowercase() },
                    )
                },
                onFailure = { ServerListState.Failed },
            )
        }
    }

    fun toggleMutedGuild(guildId: String) {
        stash.toggleMutedGuild(guildId)
    }
}

@Composable
fun AlertsScreen(viewModel: AlertsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val servers by viewModel.servers.collectAsState()
    var pickingServers by remember { mutableStateOf(false) }

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
                    title = stringResource(R.string.alerts_title),
                    subtitle = stringResource(R.string.alerts_subtitle),
                )
            }

            item {
                Cluster {
                    ToggleRow(
                        title = stringResource(R.string.alerts_master_toggle),
                        icon = Icons.Rounded.NotificationsActive,
                        checked = state.enabled,
                        onToggle = { viewModel.setEnabled(!state.enabled) },
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = state.enabled,
                    enter = Pace.reveal,
                    exit = Pace.conceal,
                ) {
                    Column {
                        SectionHeader(stringResource(R.string.alerts_scope_header))
                        Cluster {
                            AlertScope.entries.forEachIndexed { index, scope ->
                                PickRow(
                                    title = stringResource(scope.labelRes()),
                                    subtitle = stringResource(scope.blurbRes()),
                                    icon = scope.icon(),
                                    selected = state.scope == scope,
                                    onPick = { viewModel.setScope(scope) },
                                )
                                if (scope != AlertScope.DMS_ONLY) {
                                    AnimatedVisibility(
                                        visible = state.scope == scope,
                                        enter = Pace.reveal,
                                        exit = Pace.conceal,
                                    ) {
                                        Column {
                                            RowRule()
                                            TapRow(
                                                title = stringResource(
                                                    R.string.alerts_muted_button,
                                                ),
                                                subtitle = if (state.mutedGuilds.isEmpty()) {
                                                    stringResource(R.string.alerts_muted_none)
                                                } else {
                                                    stringResource(
                                                        R.string.alerts_muted_count,
                                                        state.mutedGuilds.size,
                                                    )
                                                },
                                                icon = Icons.Rounded.NotificationsOff,
                                                onClick = {
                                                    viewModel.loadServers()
                                                    pickingServers = true
                                                },
                                            )
                                        }
                                    }
                                }
                                if (index != AlertScope.entries.lastIndex) RowRule(inset = false)
                            }
                        }

                        SectionHeader(stringResource(R.string.alerts_filters_header))
                        Cluster {
                            ToggleRow(
                                title = stringResource(R.string.alerts_skip_bots),
                                icon = Icons.Rounded.SmartToy,
                                checked = state.skipBots,
                                onToggle = { viewModel.setSkipBots(!state.skipBots) },
                            )
                            RowRule()
                            ToggleRow(
                                title = stringResource(R.string.alerts_clear_read),
                                subtitle = stringResource(R.string.alerts_clear_read_blurb),
                                icon = Icons.Rounded.DoneAll,
                                checked = state.clearReadElsewhere,
                                onToggle = {
                                    viewModel.setClearReadElsewhere(!state.clearReadElsewhere)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickingServers) {
        MutedServersSheet(
            state = servers,
            muted = state.mutedGuilds,
            onToggle = viewModel::toggleMutedGuild,
            onDismiss = { pickingServers = false },
        )
    }
}

private fun AlertScope.labelRes(): Int = when (this) {
    AlertScope.EVERYTHING -> R.string.scope_everything
    AlertScope.MENTIONS_AND_DMS -> R.string.scope_mentions
    AlertScope.DMS_ONLY -> R.string.scope_dms
}

private fun AlertScope.blurbRes(): Int = when (this) {
    AlertScope.EVERYTHING -> R.string.scope_everything_blurb
    AlertScope.MENTIONS_AND_DMS -> R.string.scope_mentions_blurb
    AlertScope.DMS_ONLY -> R.string.scope_dms_blurb
}

private fun AlertScope.icon(): ImageVector = when (this) {
    AlertScope.EVERYTHING -> Icons.AutoMirrored.Rounded.Chat
    AlertScope.MENTIONS_AND_DMS -> Icons.Rounded.AlternateEmail
    AlertScope.DMS_ONLY -> Icons.Rounded.Person
}

