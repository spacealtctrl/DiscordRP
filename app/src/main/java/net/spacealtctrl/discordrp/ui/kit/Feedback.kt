package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

enum class CalloutTone { Info, Warning, Danger }

@Composable
fun CalloutBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: CalloutTone = CalloutTone.Info,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val (washTarget, accentTarget) = when (tone) {
        CalloutTone.Info -> Look.palette.accentFaint to Look.palette.accent
        CalloutTone.Warning -> Look.palette.warningWash to Look.palette.warning
        CalloutTone.Danger -> Look.palette.dangerWash to Look.palette.danger
    }
    val wash by animateColorAsState(
        targetValue = washTarget,
        animationSpec = tween(Pace.EASY, easing = Pace.settle),
        label = "calloutWash",
    )
    val accent by animateColorAsState(
        targetValue = accentTarget,
        animationSpec = tween(Pace.EASY, easing = Pace.settle),
        label = "calloutAccent",
    )

    Column(
        modifier
            .fillMaxWidth()
            .clip(Look.corners.card)
            .background(wash)
            .padding(Look.gaps.room),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(end = Look.gaps.cozy)
                        .size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Look.palette.ink,
            )
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = Look.palette.inkFaint,
            modifier = Modifier.padding(top = Look.gaps.tight),
        )
        if (actionLabel != null && onAction != null) {
            Box(Modifier.padding(top = Look.gaps.cozy)) {
                AccentButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun EmptyCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = Look.gaps.wide),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Look.gaps.snug),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Look.palette.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Look.palette.inkGhost,
            textAlign = TextAlign.Center,
        )
    }
}

private val LocalShimmerSweep = compositionLocalOf<State<Float>?> { null }

@Composable
private fun rememberShimmerSweep(label: String): State<Float> =
    rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Pace.SWEEP, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerSweep",
    )

@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sweep = rememberShimmerSweep(label = "shimmerHost")
    CompositionLocalProvider(LocalShimmerSweep provides sweep) {
        Box(modifier) { content() }
    }
}

@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    shape: androidx.compose.ui.graphics.Shape = Look.corners.soft,
) {
    val sweep = LocalShimmerSweep.current ?: rememberShimmerSweep(label = "shimmer")
    val base = Look.palette.hover
    Box(
        modifier
            .height(height)
            .clip(shape)
            .drawBehind {
                drawRect(color = base, alpha = 0.45f)
                val band = size.width * 0.6f
                val x = lerp(-band, size.width + band, sweep.value)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        start = Offset(x - band / 2f, 0f),
                        end = Offset(x + band / 2f, size.height),
                    ),
                )
            },
    )
}

@Composable
fun ArcSpinner(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val transition = rememberInfiniteTransition(label = "spin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(Pace.SPIN, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinAngle",
    )
    val sweep by transition.animateFloat(
        initialValue = 40f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = Pace.sine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "spinSweep",
    )
    val accent = Look.palette.accent
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        drawArc(
            color = accent,
            startAngle = rotation,
            sweepAngle = sweep,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            ),
        )
    }
}
