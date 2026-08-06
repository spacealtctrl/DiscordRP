package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

private val TWO_PI = (2 * PI).toFloat()

@Composable
fun Cluster(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Look.corners.card)
            .background(Look.palette.panel),
    ) {
        content()
    }
}

@Composable
fun RowRule(inset: Boolean = true) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = if (inset) 56.dp else 0.dp)
            .background(Look.palette.outlineSoft)
            .padding(top = 1.dp),
    )
}

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    glowing: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val f by animateFloatAsState(
        targetValue = if (glowing) 1f else 0f,
        animationSpec = tween(Pace.EASY, easing = Pace.settle),
        label = "glowFraction",
    )
    val border = lerp(Look.palette.outlineSoft, Look.palette.accent.copy(alpha = 0.55f), f)
    val glowBreath: State<Float> = if (f > 0.01f) {
        rememberInfiniteTransition(label = "glowBreath").animateFloat(
            initialValue = 0f,
            targetValue = TWO_PI,
            animationSpec = infiniteRepeatable(
                animation = tween(Pace.BREATHE, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowBreathPhase",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier
            .fillMaxWidth()
            .clip(Look.corners.big)
            .background(Look.palette.panel)
            .border(1.dp, border, Look.corners.big)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = f * (0.85f + 0.15f * sin(glowBreath.value)) }
                .background(Look.palette.heroGlow),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Look.palette.sheen),
        )
        Column(Modifier.padding(Look.gaps.open)) { content() }
    }
}
