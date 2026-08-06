package net.spacealtctrl.discordrp.ui.kit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.spacealtctrl.discordrp.settings.Mood
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace
import net.spacealtctrl.discordrp.ui.theme.moodColor

@Composable
fun AvatarBadge(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    mood: Mood? = null,
    mobile: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    var badgeMood by remember { mutableStateOf(mood) }
    if (mood != null) badgeMood = mood

    Box(
        modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Look.palette.hover),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(Pace.CROSSFADE)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Look.palette.inkGhost,
                    modifier = Modifier.size(size * 0.55f),
                )
            }
        }

        AnimatedVisibility(
            visible = mood != null,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = scaleIn(initialScale = 0.5f, animationSpec = Pace.pop()) +
                fadeIn(tween(Pace.QUICK, easing = Pace.settle)),
            exit = scaleOut() + fadeOut(tween(Pace.BLINK, easing = Pace.dart)),
        ) {
            badgeMood?.let { shown ->
                Box(
                    Modifier
                        .size(size * 0.42f)
                        .clip(CircleShape)
                        .background(Look.palette.canvas),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = mobile && shown != Mood.INVISIBLE,
                        transitionSpec = {
                            (
                                scaleIn(initialScale = 0.6f, animationSpec = Pace.pop()) +
                                    fadeIn(tween(Pace.QUICK, easing = Pace.settle))
                                ).togetherWith(
                                scaleOut(targetScale = 0.6f) +
                                    fadeOut(tween(Pace.BLINK, easing = Pace.dart))
                            )
                        },
                        contentAlignment = Alignment.Center,
                    ) { showMobile ->
                        if (showMobile) {
                            MobileMark(color = moodColor(shown), height = size * 0.26f)
                        } else {
                            MoodDot(mood = shown, size = size * 0.26f)
                        }
                    }
                }
            }
        }
    }
}
