package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

@Composable
fun SearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (focused) Look.palette.accent.copy(alpha = 0.45f) else Look.palette.outlineSoft,
        animationSpec = tween(Pace.EASY, easing = Pace.settle),
    )
    val placeholderAlpha by animateFloatAsState(
        targetValue = if (value.isEmpty()) 1f else 0f,
        animationSpec = tween(Pace.QUICK, easing = Pace.dart),
    )

    Row(
        modifier
            .fillMaxWidth()
            .clip(Look.corners.soft)
            .background(Look.palette.abyss)
            .border(1.dp, borderColor, Look.corners.soft)
            .padding(horizontal = Look.gaps.cozy, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = Look.palette.inkGhost,
            modifier = Modifier
                .padding(end = Look.gaps.snug)
                .size(18.dp),
        )
        Box(Modifier.weight(1f)) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = Look.palette.inkGhost,
                modifier = Modifier.graphicsLayer { alpha = placeholderAlpha },
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Look.palette.ink),
                cursorBrush = SolidColor(Look.palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = scaleIn(initialScale = 0.6f, animationSpec = Pace.snap()) +
                    fadeIn(tween(Pace.QUICK, easing = Pace.settle)),
                exit = scaleOut(targetScale = 0.6f) +
                    fadeOut(tween(Pace.BLINK, easing = Pace.dart)),
            ) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Look.palette.inkGhost,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
