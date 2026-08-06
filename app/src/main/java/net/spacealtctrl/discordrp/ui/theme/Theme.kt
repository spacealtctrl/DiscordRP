package net.spacealtctrl.discordrp.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import net.spacealtctrl.discordrp.settings.Mood

object Look {
    val palette: Palette @Composable get() = LocalPalette.current
    val gaps: Gaps @Composable get() = LocalGaps.current
    val corners: Corners @Composable get() = LocalCorners.current
}

@Composable
fun moodColor(mood: Mood): Color = when (mood) {
    Mood.ONLINE -> Look.palette.online
    Mood.IDLE -> Look.palette.idle
    Mood.DND -> Look.palette.dnd
    Mood.INVISIBLE -> Look.palette.invisible
}

@Composable
fun animatedMoodColor(mood: Mood): Color = animateColorAsState(
    targetValue = moodColor(mood),
    animationSpec = tween(Pace.EASY, easing = Pace.settle),
).value

@Composable
fun DiscordRpTheme(content: @Composable () -> Unit) {
    val palette = remember { Palette() }
    val gaps = remember { Gaps() }
    val corners = remember { Corners() }

    val view = LocalView.current
    SideEffect {
        view.context.findWindow()?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    val scheme = remember(palette) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.inkOnAccent,
            primaryContainer = palette.accentDeep,
            onPrimaryContainer = palette.ink,
            secondary = palette.accentDeep,
            onSecondary = palette.inkOnAccent,
            background = palette.canvas,
            onBackground = palette.ink,
            surface = palette.panel,
            onSurface = palette.ink,
            surfaceVariant = palette.raised,
            onSurfaceVariant = palette.inkFaint,
            surfaceContainer = palette.panel,
            surfaceContainerHigh = palette.raised,
            surfaceContainerHighest = palette.hover,
            outline = palette.outline,
            outlineVariant = palette.outlineSoft,
            error = palette.danger,
            onError = palette.ink,
            errorContainer = palette.dangerWash,
            scrim = palette.scrim,
        )
    }

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalGaps provides gaps,
        LocalCorners provides corners,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppType,
            content = content,
        )
    }
}

private tailrec fun Context.findWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findWindow()
    else -> null
}
