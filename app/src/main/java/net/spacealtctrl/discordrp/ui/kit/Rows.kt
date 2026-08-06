package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

@Composable
private fun RowShell(
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 58.dp)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = Look.gaps.room, vertical = Look.gaps.cozy),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun RowScope.RowLabels(
    title: String,
    subtitle: String?,
    dimmed: Boolean = false,
) {
    Column(Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (dimmed) Look.palette.inkGhost else Look.palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Look.palette.inkGhost,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LeadIcon(icon: ImageVector?, tint: Color) {
    icon?.let {
        Icon(
            imageVector = it,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(end = Look.gaps.room)
                .size(22.dp),
        )
    }
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    RowShell(onClick = onToggle, enabled = enabled) {
        LeadIcon(icon, if (enabled) Look.palette.inkFaint else Look.palette.inkGhost)
        RowLabels(title, subtitle, dimmed = !enabled)
        ThumbSwitch(checked = checked, enabled = enabled)
    }
}

@Composable
fun TapRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    RowShell(onClick = onClick) {
        LeadIcon(icon, Look.palette.inkFaint)
        RowLabels(title, subtitle)
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Look.palette.inkGhost,
            )
        }
    }
}

@Composable
fun PickRow(
    title: String,
    selected: Boolean,
    onPick: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    RowShell(onClick = onPick) {
        leading?.let {
            Box(Modifier.padding(end = Look.gaps.room)) { it() }
        }
        LeadIcon(icon, Look.palette.inkFaint)
        RowLabels(title, subtitle)
        RadioBead(selected)
    }
}

@Composable
private fun RadioBead(selected: Boolean) {
    val ringColor by animateColorAsState(
        targetValue = if (selected) Look.palette.accent else Look.palette.hover,
        animationSpec = tween(Pace.QUICK, easing = Pace.settle),
    )
    val dotColor by animateColorAsState(
        targetValue = if (selected) Color.White else Look.palette.panel,
        animationSpec = tween(Pace.QUICK, easing = Pace.settle),
    )
    val dotSize by animateDpAsState(
        targetValue = if (selected) 8.dp else 16.dp,
        animationSpec = Pace.snap(),
    )
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(ringColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

@Composable
fun StepRow(
    index: Int,
    title: String,
    subtitle: String?,
    done: Boolean,
    onClick: () -> Unit,
) {
    RowShell(onClick = onClick) {
        val wash by animateColorAsState(
            targetValue = if (done) Look.palette.positiveWash else Look.palette.hover,
            animationSpec = tween(Pace.EASY, easing = Pace.settle),
        )
        Box(
            Modifier
                .padding(end = Look.gaps.room)
                .size(28.dp)
                .clip(CircleShape)
                .background(wash),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = done,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    val arrive = if (targetState) Pace.pop<Float>() else Pace.snap<Float>()
                    (
                        scaleIn(initialScale = 0f, animationSpec = arrive) +
                            fadeIn(tween(Pace.QUICK, delayMillis = 80, easing = Pace.settle))
                        ).togetherWith(fadeOut(tween(Pace.BLINK, easing = Pace.dart)))
                },
            ) { isDone ->
                if (isDone) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Look.palette.positive,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Look.palette.inkFaint,
                    )
                }
            }
        }
        RowLabels(title, subtitle)
        AnimatedVisibility(
            visible = !done,
            enter = fadeIn(tween(Pace.QUICK, easing = Pace.settle)) +
                expandHorizontally(tween(Pace.QUICK, easing = Pace.settle)),
            exit = fadeOut(tween(Pace.QUICK, easing = Pace.dart)) +
                shrinkHorizontally(tween(Pace.QUICK, easing = Pace.dart)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Look.palette.inkGhost,
            )
        }
    }
}
