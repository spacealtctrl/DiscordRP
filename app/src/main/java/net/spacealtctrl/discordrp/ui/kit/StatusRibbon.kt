package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace
import kotlin.math.PI
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()
private const val LAYER_FADE_IN_DELAY = 120
private const val DASH_CRAWL_MS = 6_000
private val DashLength = 4.dp
private val DashGap = 9.dp

@Composable
fun StatusRibbon(
    running: Boolean,
    sharing: Boolean,
    modifier: Modifier = Modifier,
) {
    val lively = running && sharing
    val liveliness by animateFloatAsState(
        targetValue = if (lively) 1f else 0f,
        animationSpec = if (lively) Pace.pop() else tween(Pace.EASY, easing = Pace.dart),
        label = "liveliness",
    )
    val quiet = running && !sharing
    val dashedAlpha by animateFloatAsState(
        targetValue = if (quiet) 1f else 0f,
        animationSpec = tween(
            Pace.EASY,
            delayMillis = if (quiet) LAYER_FADE_IN_DELAY else 0,
            easing = Pace.sine,
        ),
        label = "dashedAlpha",
    )
    val severedAlpha by animateFloatAsState(
        targetValue = if (!running) 1f else 0f,
        animationSpec = tween(
            Pace.EASY,
            delayMillis = if (!running) LAYER_FADE_IN_DELAY else 0,
            easing = Pace.sine,
        ),
        label = "severedAlpha",
    )

    val dashPeriodPx = with(LocalDensity.current) { (DashLength + DashGap).toPx() }
    val transition = rememberInfiniteTransition(label = "ribbon")
    val drift: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(Pace.DRIFT, easing = LinearEasing)),
        label = "drift",
    )
    val breathePhase: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(Pace.BREATHE, easing = LinearEasing)),
        label = "breathe",
    )
    val dashTravel: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = dashPeriodPx,
        animationSpec = infiniteRepeatable(tween(DASH_CRAWL_MS, easing = LinearEasing)),
        label = "dashTravel",
    )

    val accent = Look.palette.accent
    val ghost = Look.palette.inkGhost

    Canvas(
        modifier
            .fillMaxWidth(0.66f)
            .height(52.dp),
    ) {
        val midY = size.height / 2f

        if (severedAlpha >= 0.01f) {
            severedWire(
                midY = midY,
                color = ghost,
                spark = accent,
                alpha = severedAlpha,
                breathe = breathePhase.value,
                cycle = drift.value / TWO_PI,
            )
        }
        if (dashedAlpha >= 0.01f) quietWire(midY, ghost, dashedAlpha, dashTravel.value)

        val life = liveliness
        if (life >= 0.01f) {
            val breathe = 0.775f + 0.225f * sin(breathePhase.value)
            val amplitude = size.height * 0.34f * breathe * life
            val glow = life.coerceIn(0f, 1f)
            val fade = Brush.horizontalGradient(
                listOf(
                    accent.copy(alpha = 0f),
                    accent,
                    accent,
                    accent.copy(alpha = 0f),
                ),
            )
            ripple(
                cycles = 3.4f,
                phase = -drift.value,
                amplitude = amplitude * 0.45f,
                midY = midY,
                brush = fade,
                strokeWidth = 3.dp.toPx(),
                alpha = 0.35f * glow,
            )
            ripple(
                cycles = 2.2f,
                phase = drift.value,
                amplitude = amplitude,
                midY = midY,
                brush = fade,
                strokeWidth = 4.dp.toPx(),
                alpha = glow,
            )
        }
    }
}

private fun DrawScope.severedWire(
    midY: Float,
    color: Color,
    spark: Color,
    alpha: Float,
    breathe: Float,
    cycle: Float,
) {
    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
    val gap = size.width * 0.14f
    val dipLeft = size.height * 0.18f * (1f + 0.16f * sin(breathe))
    val dipRight = size.height * 0.18f * (1f + 0.16f * sin(breathe + 1.9f))

    val leftTip = Offset((size.width - gap) / 2f, midY + dipLeft * 0.6f)
    val rightTip = Offset((size.width + gap) / 2f, midY - dipRight * 0.6f)

    val left = Path().apply {
        moveTo(0f, midY)
        quadraticBezierTo(size.width * 0.30f, midY + dipLeft, leftTip.x, leftTip.y)
    }
    val right = Path().apply {
        moveTo(size.width, midY)
        quadraticBezierTo(size.width * 0.70f, midY - dipRight, rightTip.x, rightTip.y)
    }
    drawPath(left, color, alpha = alpha, style = stroke)
    drawPath(right, color, alpha = alpha, style = stroke)
    drawCircle(color, radius = 2.5.dp.toPx(), center = leftTip, alpha = alpha)
    drawCircle(color, radius = 2.5.dp.toPx(), center = rightTip, alpha = alpha)

    val pulse = (1f - kotlin.math.abs(cycle % 1f - 0.5f) / 0.06f).coerceIn(0f, 1f)
    if (pulse > 0.01f) {
        val flash = pulse * alpha
        val bolt = Path().apply {
            moveTo(leftTip.x, leftTip.y)
            val third = (rightTip.x - leftTip.x) / 3f
            val rise = (rightTip.y - leftTip.y) / 3f
            lineTo(leftTip.x + third, leftTip.y + rise - 3.dp.toPx())
            lineTo(leftTip.x + third * 2f, leftTip.y + rise * 2f + 3.dp.toPx())
            lineTo(rightTip.x, rightTip.y)
        }
        drawPath(
            bolt, spark, alpha = flash,
            style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(spark, radius = 3.dp.toPx(), center = leftTip, alpha = flash)
        drawCircle(spark, radius = 3.dp.toPx(), center = rightTip, alpha = flash)
    }
}

private fun DrawScope.quietWire(
    midY: Float,
    color: Color,
    alpha: Float,
    travel: Float,
) {
    drawLine(
        color = color,
        start = Offset(0f, midY),
        end = Offset(size.width, midY),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(DashLength.toPx(), DashGap.toPx()),
            -travel,
        ),
    )
}

private fun DrawScope.ripple(
    cycles: Float,
    phase: Float,
    amplitude: Float,
    midY: Float,
    brush: Brush,
    strokeWidth: Float,
    alpha: Float,
) {
    val path = Path()
    var x = 0f
    while (x <= size.width) {
        val progress = x / size.width
        val envelope = sin(PI.toFloat() * progress)
        val y = midY + amplitude * envelope * sin(progress * cycles * TWO_PI + phase)
        if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
        x += 3f
    }
    drawPath(
        path = path,
        brush = brush,
        alpha = alpha,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}
