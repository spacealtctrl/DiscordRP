package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import net.spacealtctrl.discordrp.ui.theme.Look

@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Look.palette.canvas),
    ) {
        content()
    }
}

@Composable
fun PageTitle(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = Look.gaps.open, bottom = Look.gaps.open),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading?.let {
                Box(Modifier.padding(end = Look.gaps.cozy)) { it() }
            }
            Column(Modifier.weight(1f)) {
                eyebrow?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Look.palette.accent,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Look.palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing?.invoke()
        }
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = Look.palette.inkFaint,
                modifier = Modifier.padding(top = Look.gaps.snug),
            )
        }
    }
}

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = Look.gaps.open, bottom = Look.gaps.snug, start = Look.gaps.tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Look.palette.inkGhost,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}
