package net.spacealtctrl.discordrp.ui.nav

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

enum class Dock(val route: String, val icon: ImageVector, val labelRes: Int) {
    HOME("home", Icons.Rounded.Home, R.string.tab_home),
    ALERTS("alerts", Icons.Rounded.Notifications, R.string.tab_alerts),
    PRESENCE("presence", Icons.Rounded.MusicNote, R.string.tab_presence),
    YOU("you", Icons.Rounded.Person, R.string.tab_you),
}

@Composable
fun TabDock(
    current: Dock?,
    onPick: (Dock) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Look.palette.abyss)
            .navigationBarsPadding()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Dock.entries.forEach { dock ->
            DockItem(
                dock = dock,
                selected = dock == current,
                onPick = { onPick(dock) },
            )
        }
    }
}

@Composable
private fun DockItem(
    dock: Dock,
    selected: Boolean,
    onPick: () -> Unit,
) {
    val accent = Look.palette.accent
    val inkGhost = Look.palette.inkGhost
    val transition = updateTransition(targetState = selected, label = "dockSelect")
    val tint by transition.animateColor(
        transitionSpec = { tween(Pace.EASY, easing = Pace.settle) },
        label = "dockTint",
    ) { picked -> if (picked) accent else inkGhost }
    val bounce by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                spring(dampingRatio = 0.45f, stiffness = 1500f)
            } else {
                spring(dampingRatio = 1f, stiffness = 1500f)
            }
        },
        label = "dockBounce",
    ) { picked -> if (picked) 1.12f else 1f }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val squish by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = if (isPressed) Pace.press() else Pace.snap(),
        label = "dockSquish",
    )

    Column(
        Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPick,
            )
            .padding(horizontal = Look.gaps.room, vertical = Look.gaps.tight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = dock.icon,
            contentDescription = stringResource(dock.labelRes),
            tint = tint,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    val scale = bounce * squish
                    scaleX = scale
                    scaleY = scale
                },
        )
        Text(
            text = stringResource(dock.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
