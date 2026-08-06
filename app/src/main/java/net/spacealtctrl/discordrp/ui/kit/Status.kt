package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.settings.Mood
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace
import net.spacealtctrl.discordrp.ui.theme.animatedMoodColor

@Composable
fun MoodDot(
    mood: Mood,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
) {
    val color = animatedMoodColor(mood)
    val scale = remember { Animatable(1f) }
    var drawnMood by remember { mutableStateOf(mood) }
    LaunchedEffect(mood) {
        if (mood == drawnMood) return@LaunchedEffect
        scale.animateTo(0.75f, tween(Pace.BLINK, easing = Pace.dart))
        drawnMood = mood
        scale.animateTo(1f, Pace.pop())
    }

    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = 0.99f
            },
    ) {
        val radius = this.size.minDimension / 2f
        when (drawnMood) {
            Mood.ONLINE -> drawCircle(color)
            Mood.IDLE -> {
                drawCircle(color)
                drawCircle(
                    color = Color.Transparent,
                    radius = radius * 0.62f,
                    center = Offset(this.size.width * 0.30f, this.size.height * 0.30f),
                    blendMode = BlendMode.Clear,
                )
            }
            Mood.DND -> {
                drawCircle(color)
                val barHeight = this.size.height * 0.28f
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(this.size.width * 0.14f, (this.size.height - barHeight) / 2f),
                    size = Size(this.size.width * 0.72f, barHeight),
                    cornerRadius = CornerRadius(barHeight / 2f),
                    blendMode = BlendMode.Clear,
                )
            }
            Mood.INVISIBLE -> drawCircle(
                color = color,
                radius = radius * 0.78f,
                style = Stroke(width = radius * 0.44f),
            )
        }
    }
}

@Composable
fun MobileMark(
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    Canvas(modifier.size(height * 0.69f, height)) {
        val stroke = size.height * 0.13f
        val inset = stroke / 2f
        val corner = CornerRadius(size.height * 0.16f, size.height * 0.16f)
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = corner,
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.33f, size.height - stroke * 2.2f),
            size = Size(size.width * 0.34f, stroke * 0.8f),
            cornerRadius = CornerRadius(stroke),
        )
    }
}

@Composable
fun moodLabel(mood: Mood): String = stringResource(
    when (mood) {
        Mood.ONLINE -> R.string.mood_online
        Mood.IDLE -> R.string.mood_idle
        Mood.DND -> R.string.mood_dnd
        Mood.INVISIBLE -> R.string.mood_invisible
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodSheet(
    current: Mood,
    onPick: (Mood) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Mood?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Look.palette.panel,
        contentColor = Look.palette.ink,
    ) {
        Column(Modifier.padding(bottom = Look.gaps.vast)) {
            Text(
                text = stringResource(R.string.mood_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Look.palette.ink,
                modifier = Modifier.padding(
                    start = Look.gaps.open,
                    end = Look.gaps.open,
                    bottom = Look.gaps.cozy,
                ),
            )
            Mood.entries.forEach { mood ->
                PickRow(
                    title = moodLabel(mood),
                    leading = { MoodDot(mood, size = 16.dp) },
                    selected = mood == (pending ?: current),
                    onPick = {
                        if (pending == null) {
                            pending = mood
                            scope.launch {
                                delay(Pace.EASY.toLong())
                                onPick(mood)
                            }
                        }
                    },
                )
            }
        }
    }
}
