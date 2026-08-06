package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import net.spacealtctrl.discordrp.presence.NowPlaying
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()
private const val EQ_LOOP_MS = 9_600
private const val REST_LEVEL = 0.18f

private val EQ_F = intArrayOf(8, 11, 13, 17)
private val EQ_G = intArrayOf(5, 7, 9, 10)

@Composable
fun EqualizerBars(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Look.palette.accent,
    barCount: Int = 4,
) {
    val energy by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 150f),
        label = "eqEnergy",
    )

    val still = remember { mutableFloatStateOf(0f) }
    val phase: State<Float> = if (playing || energy > 0.01f) {
        rememberInfiniteTransition(label = "eq").animateFloat(
            initialValue = 0f,
            targetValue = TWO_PI,
            animationSpec = infiniteRepeatable(tween(EQ_LOOP_MS, easing = LinearEasing)),
            label = "eqPhase",
        )
    } else {
        still
    }

    Canvas(modifier.size(width = (barCount * 5).dp, height = 16.dp)) {
        val barWidth = size.width / (barCount * 2f - 1f)
        for (index in 0 until barCount) {
            val f = EQ_F[index % EQ_F.size]
            val g = EQ_G[index % EQ_G.size]
            val organic = (
                0.55f +
                    0.32f * sin(f * phase.value + index * 2.399f) +
                    0.13f * sin(g * phase.value + 1.7f * index)
                ).coerceIn(0.22f, 1f)
            val level = lerp(REST_LEVEL, organic, energy)
            val barHeight = size.height * level
            drawRoundRect(
                color = color,
                topLeft = Offset(index * barWidth * 2f, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
fun TrackTimeline(
    startMillis: Long?,
    endMillis: Long?,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White,
) {
    val start = startMillis ?: return
    val now by produceState(initialValue = System.currentTimeMillis(), start, endMillis) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000 - ((System.currentTimeMillis() - start) % 1_000))
        }
    }

    val elapsed = ((now - start) / 1000).coerceAtLeast(0)
    val total = endMillis?.let { ((it - start) / 1000).coerceAtLeast(0) }

    if (total == null || total == 0L) {
        Text(
            text = clock(elapsed),
            style = MaterialTheme.typography.labelMedium,
            color = Look.palette.online,
            modifier = modifier.padding(top = 4.dp),
        )
        return
    }

    val span = (endMillis - start).toFloat()
    val target = ((now - start + 1_000L) / span).coerceIn(0f, 1f)
    val lastTarget = remember { mutableFloatStateOf(target) }
    val jumped = abs(target - lastTarget.floatValue) > 0.05f
    SideEffect { lastTarget.floatValue = target }
    val fraction by animateFloatAsState(
        targetValue = target,
        animationSpec = if (jumped) {
            tween(Pace.EASY, easing = Pace.settle)
        } else {
            tween(1_000, easing = LinearEasing)
        },
        label = "timelineFraction",
    )

    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(clock(elapsed), style = MaterialTheme.typography.labelMedium, color = Look.palette.inkFaint)
        Canvas(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(10.dp),
        ) {
            val y = size.height / 2f
            val stroke = 3.dp.toPx()
            drawLine(
                color = Color(0x33FFFFFF),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width * fraction.coerceIn(0f, 1f), y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        Text(clock(total), style = MaterialTheme.typography.labelMedium, color = Look.palette.inkFaint)
    }
}

fun clock(seconds: Long): String =
    if (seconds >= 3600) {
        "%d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    } else {
        "%02d:%02d".format(seconds / 60, seconds % 60)
    }

private enum class GlyphShape { PLAY, STOP, PAUSE }

private fun NowPlaying.glyphShape(): GlyphShape = when {
    playing -> GlyphShape.PLAY
    stopped -> GlyphShape.STOP
    else -> GlyphShape.PAUSE
}

@Composable
fun PlaybackGlyph(
    track: NowPlaying,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val shape = track.glyphShape()
    val previous = remember { mutableStateOf(shape) }
    val swap = remember { Animatable(1f) }
    LaunchedEffect(shape) {
        if (previous.value != shape) {
            swap.snapTo(0f)
            swap.animateTo(1f, Pace.snap())
            previous.value = shape
        }
    }

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Look.palette.accent),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.45f)) {
            val f = swap.value.coerceIn(0f, 1f)
            if (f < 1f && previous.value != shape) {
                scale(1f - 0.3f * f) { drawGlyph(previous.value, alpha = 1f - f) }
            }
            scale(0.7f + 0.3f * f) { drawGlyph(shape, alpha = f) }
        }
    }
}

private fun DrawScope.drawGlyph(shape: GlyphShape, alpha: Float) {
    val w = size.width
    val h = size.height
    val white = Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
    when (shape) {
        GlyphShape.PLAY -> {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.15f, 0f)
                lineTo(w, h / 2f)
                lineTo(w * 0.15f, h)
                close()
            }
            drawPath(path, color = white)
        }
        GlyphShape.STOP -> drawRoundRect(
            color = white,
            cornerRadius = CornerRadius(w * 0.12f),
        )
        GlyphShape.PAUSE -> {
            val barWidth = w * 0.3f
            drawRoundRect(
                color = white,
                topLeft = Offset(0f, 0f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
            drawRoundRect(
                color = white,
                topLeft = Offset(w - barWidth, 0f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
fun HeroArt(
    track: NowPlaying,
    appIcon: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    showGlyph: Boolean = false,
) {
    val context = LocalContext.current
    AnimatedContent(
        targetState = track,
        contentKey = { "${it.appPackage}|${it.title}" },
        transitionSpec = {
            (
                scaleIn(initialScale = 0.88f, animationSpec = Pace.pop()) +
                    fadeIn(tween(Pace.QUICK, easing = Pace.settle))
                )
                .togetherWith(fadeOut(tween(Pace.QUICK, easing = Pace.dart)))
        },
        modifier = modifier,
        label = "heroArt",
    ) { current ->
        Box {
            Box(
                Modifier
                    .size(size)
                    .clip(Look.corners.card)
                    .background(Look.palette.hover),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = current.artwork,
                    animationSpec = tween(Pace.EASY, easing = Pace.settle),
                    label = "heroCover",
                ) { art ->
                    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                        when {
                            art != null -> Image(
                                bitmap = art.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(size),
                            )
                            showGlyph -> PlaybackGlyph(track = current, size = size * 0.55f)
                            appIcon != null -> AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(appIcon)
                                    .crossfade(Pace.CROSSFADE)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.size(size * 0.55f),
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = current.artwork != null && (showGlyph || appIcon != null),
                enter = scaleIn(initialScale = 0.6f, animationSpec = Pace.pop()) +
                    fadeIn(tween(Pace.QUICK, easing = Pace.settle)),
                exit = fadeOut(tween(Pace.BLINK, easing = Pace.dart)),
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Look.palette.canvas),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showGlyph) {
                        PlaybackGlyph(track = current, size = 22.dp)
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(appIcon)
                                .crossfade(Pace.CROSSFADE)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            }
        }
    }
}
