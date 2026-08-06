package net.spacealtctrl.discordrp.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Immutable
data class Gaps(
    val hair: Dp = 2.dp,
    val tight: Dp = 4.dp,
    val snug: Dp = 8.dp,
    val cozy: Dp = 12.dp,
    val room: Dp = 16.dp,
    val open: Dp = 20.dp,
    val wide: Dp = 28.dp,
    val vast: Dp = 40.dp,
    val gutter: Dp = 16.dp,
)

@Immutable
data class Corners(
    val nub: RoundedCornerShape = RoundedCornerShape(6.dp),
    val soft: RoundedCornerShape = RoundedCornerShape(10.dp),
    val card: RoundedCornerShape = RoundedCornerShape(14.dp),
    val big: RoundedCornerShape = RoundedCornerShape(20.dp),
    val hero: RoundedCornerShape = RoundedCornerShape(26.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(999.dp),
)

object Pace {
    val glide: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val settle: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val dart: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val sine: Easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

    const val BLINK = 90
    const val QUICK = 160
    const val EASY = 260
    const val LAZY = 400

    const val DRIFT = 4200
    const val BREATHE = 2600
    const val SWEEP = 1100
    const val SPIN = 1100
    const val CROSSFADE = 260

    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 2400f)

    fun <T> snap(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = 600f, visibilityThreshold = visibilityThreshold)

    fun <T> pop(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = 400f, visibilityThreshold = visibilityThreshold)

    fun <T> glidespring(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 400f, visibilityThreshold = visibilityThreshold)

    val reveal: EnterTransition =
        fadeIn(tween(EASY, easing = settle)) + expandVertically(tween(EASY, easing = settle))

    val conceal: ExitTransition =
        fadeOut(tween(QUICK, easing = dart)) + shrinkVertically(tween(QUICK, easing = dart))

    fun fadeThrough(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tween(EASY, delayMillis = BLINK, easing = settle)) +
            scaleIn(initialScale = 0.97f, animationSpec = glidespring()),
        initialContentExit = fadeOut(tween(BLINK, easing = dart)),
        sizeTransform = SizeTransform(clip = false) { _, _ ->
            glidespring(IntSize.VisibilityThreshold)
        },
    )
}

val LocalGaps = staticCompositionLocalOf { Gaps() }
val LocalCorners = staticCompositionLocalOf { Corners() }
