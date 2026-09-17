package net.spacealtctrl.discordrp.ui.screens.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.discord.GuildSummary
import net.spacealtctrl.discordrp.ui.kit.ArcSpinner
import net.spacealtctrl.discordrp.ui.kit.RowRule
import net.spacealtctrl.discordrp.ui.kit.SearchBox
import net.spacealtctrl.discordrp.ui.kit.TapRow
import net.spacealtctrl.discordrp.ui.theme.Look

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutedServersSheet(
    state: ServerListState,
    muted: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Look.palette.panel,
        contentColor = Look.palette.ink,
    ) {
        Column(Modifier.padding(bottom = Look.gaps.vast)) {
            Text(
                text = stringResource(R.string.alerts_muted_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Look.palette.ink,
                modifier = Modifier.padding(horizontal = Look.gaps.open),
            )
            Text(
                text = stringResource(R.string.alerts_muted_blurb),
                style = MaterialTheme.typography.bodySmall,
                color = Look.palette.inkFaint,
                modifier = Modifier.padding(
                    start = Look.gaps.open,
                    end = Look.gaps.open,
                    top = Look.gaps.hair,
                    bottom = Look.gaps.room,
                ),
            )

            when (state) {
                is ServerListState.Loading -> Notice(R.string.alerts_muted_loading, spinning = true)
                is ServerListState.Failed -> Notice(R.string.alerts_muted_failed)
                is ServerListState.Ready -> {
                    SearchBox(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = stringResource(R.string.alerts_muted_search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Look.gaps.open),
                    )
                    val shown = state.guilds.filter { it.matches(query) }
                    if (shown.isEmpty()) {
                        Notice(R.string.alerts_muted_empty)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            contentPadding = PaddingValues(top = Look.gaps.room),
                        ) {
                            itemsIndexed(shown, key = { _, it -> it.id.orEmpty() }) { index, guild ->
                                val id = guild.id ?: return@itemsIndexed
                                TapRow(
                                    title = guild.label(),
                                    onClick = { onToggle(id) },
                                    trailing = {
                                        Checkbox(
                                            checked = id in muted,
                                            onCheckedChange = { onToggle(id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Look.palette.accent,
                                                uncheckedColor = Look.palette.inkFaint,
                                            ),
                                        )
                                    },
                                )
                                if (index != shown.lastIndex) RowRule()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(textRes: Int, spinning: Boolean = false) {
    Column(Modifier.padding(Look.gaps.open)) {
        if (spinning) ArcSpinner()
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            color = Look.palette.inkFaint,
        )
    }
}

private fun GuildSummary.label(): String = name?.takeIf { it.isNotBlank() } ?: id.orEmpty()

private fun GuildSummary.matches(query: String): Boolean =
    query.isBlank() || label().contains(query.trim(), ignoreCase = true)
