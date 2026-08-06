package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squish by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = if (pressed) Pace.press() else Pace.snap(),
        label = "squish",
    )
    val fill by animatePressColor(
        target = when {
            !enabled -> Look.palette.hover
            pressed -> Look.palette.accentDeep
            else -> Look.palette.accent
        },
        label = "fill",
    )
    val content by animatePressColor(
        target = if (enabled) Look.palette.inkOnAccent else Look.palette.inkGhost,
        label = "content",
    )

    Row(
        modifier
            .graphicsLayer {
                scaleX = squish
                scaleY = squish
            }
            .clip(Look.corners.soft)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = content,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp),
            )
            Box(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
        )
    }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squish by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (pressed) Pace.press() else Pace.snap(),
        label = "squish",
    )
    val fill by animatePressColor(
        target = if (pressed) Look.palette.hover else Look.palette.panel,
        label = "fill",
    )
    val content by animatePressColor(
        target = if (destructive) Look.palette.danger else Look.palette.ink,
        label = "content",
    )

    Row(
        modifier
            .graphicsLayer {
                scaleX = squish
                scaleY = squish
            }
            .clip(Look.corners.soft)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp),
            )
            Box(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
fun ThumbSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onToggle: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val transition = updateTransition(targetState = checked, label = "switch")
    val slide by transition.animateDp(
        transitionSpec = { Pace.snap() },
        label = "slide",
    ) { on -> if (on) 18.dp else 2.dp }
    val track by transition.animateColor(
        transitionSpec = { tween(Pace.EASY, easing = Pace.settle) },
        label = "track",
    ) { on ->
        when {
            !enabled -> Look.palette.hover
            on -> Look.palette.accent
            else -> Look.palette.invisible
        }
    }
    val glyph by transition.animateFloat(
        transitionSpec = { tween(Pace.QUICK, delayMillis = 60, easing = Pace.settle) },
        label = "glyph",
    ) { on -> if (on) 1f else 0f }
    val grow by animateFloatAsState(
        targetValue = if (pressed) 1.06f else 1f,
        animationSpec = if (pressed) Pace.press() else Pace.snap(),
        label = "grow",
    )

    Box(
        modifier
            .width(40.dp)
            .height(24.dp)
            .clip(Look.corners.pill)
            .background(track)
            .then(
                if (onToggle != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onClick = onToggle,
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        Box(
            Modifier
                .offset(x = slide, y = 2.dp)
                .size(20.dp)
                .graphicsLayer {
                    scaleX = grow
                    scaleY = grow
                }
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            ThumbGlyph(fraction = glyph, color = track)
        }
    }
}

@Composable
private fun ThumbGlyph(fraction: Float, color: Color) {
    Canvas(Modifier.size(10.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        scale(scale = 1f - 0.3f * fraction, pivot = center) {
            drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
                alpha = 1f - fraction,
            )
            drawLine(
                color = color,
                start = Offset(size.width, 0f),
                end = Offset(0f, size.height),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
                alpha = 1f - fraction,
            )
        }
        scale(scale = 0.7f + 0.3f * fraction, pivot = center) {
            val check = Path().apply {
                moveTo(size.width * 0.05f, size.height * 0.55f)
                lineTo(size.width * 0.4f, size.height * 0.9f)
                lineTo(size.width * 0.95f, size.height * 0.15f)
            }
            drawPath(check, color = color, alpha = fraction, style = stroke)
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squish by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = if (pressed) Pace.press() else Pace.snap(),
        label = "squish",
    )
    val fill by animatePressColor(
        target = if (pressed) Look.palette.hover else Look.palette.panel,
        label = "fill",
    )
    val content by animatePressColor(
        target = if (pressed) Look.palette.ink else Look.palette.inkFaint,
        label = "content",
    )

    Box(
        modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = squish
                scaleY = squish
            }
            .clip(CircleShape)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun animatePressColor(target: Color, label: String): State<Color> =
    animateColorAsState(
        targetValue = target,
        animationSpec = tween(Pace.QUICK, easing = Pace.settle),
        label = label,
    )
