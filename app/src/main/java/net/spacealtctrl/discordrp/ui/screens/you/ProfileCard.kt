package net.spacealtctrl.discordrp.ui.screens.you

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.discord.DiscordProfile
import net.spacealtctrl.discordrp.presence.NowPlaying
import net.spacealtctrl.discordrp.settings.Mood
import net.spacealtctrl.discordrp.ui.kit.HeroArt
import net.spacealtctrl.discordrp.ui.kit.MobileMark
import net.spacealtctrl.discordrp.ui.kit.MoodDot
import net.spacealtctrl.discordrp.ui.kit.TrackTimeline
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace
import net.spacealtctrl.discordrp.ui.theme.moodColor
import java.text.DateFormat
import java.util.Date

private val CardShell = Color(0xFF1A1B21)
private val InnerTint = Color(0x14FFFFFF)

@Composable
fun ProfileCard(
    profile: DiscordProfile?,
    track: NowPlaying?,
    mood: Mood,
    mobile: Boolean,
    modifier: Modifier = Modifier,
    showGlyph: Boolean = false,
    showAppIcon: Boolean = true,
    cardTitle: String? = null,
) {
    val context = LocalContext.current
    val accent = profile?.bannerColor
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: profile?.accentColor?.let { Color(0xFF000000 or it.toLong()) }
        ?: Look.palette.accent
    val banner = profile?.bannerUrl()

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardShell),
    ) {
        if (banner != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(banner).crossfade(Pace.CROSSFADE)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(30.dp),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color(0xD40E0F13)),
            )
        }

        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .background(accent),
                ) {
                    if (banner != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(banner)
                                .crossfade(Pace.CROSSFADE)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(118.dp),
                        )
                    }
                }
                RingedAvatar(
                    profile = profile,
                    mood = mood,
                    mobile = mobile,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 72.dp),
                )
            }

            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
                NameBlock(profile)

                if (track != null) {
                    Spacer(Modifier.height(16.dp))
                    ActivityBlock(track, showGlyph, showAppIcon, cardTitle)
                }

                val bio = profile?.bio
                val joined = profile?.joinedAtMillis()
                if (!bio.isNullOrBlank() || joined != null) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(InnerTint)
                            .padding(16.dp),
                    ) {
                        if (!bio.isNullOrBlank()) {
                            InnerLabel(stringResource(R.string.profile_bio_label))
                            BioText(bio)
                        }
                        if (joined != null) {
                            if (!bio.isNullOrBlank()) Spacer(Modifier.height(16.dp))
                            InnerLabel(stringResource(R.string.profile_member_since_label))
                            Text(
                                text = DateFormat.getDateInstance(DateFormat.MEDIUM)
                                    .format(Date(joined)),
                                color = Look.palette.inkFaint,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RingedAvatar(
    profile: DiscordProfile?,
    mood: Mood,
    mobile: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(modifier) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(CardShell),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(profile?.avatarUrl(160))
                    .crossfade(Pace.CROSSFADE)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(if (mobile) RoundedCornerShape(9.dp) else CircleShape)
                .background(CardShell),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = mobile && mood != Mood.INVISIBLE,
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
                    MobileMark(color = moodColor(mood))
                } else {
                    MoodDot(mood = mood, size = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun NameBlock(profile: DiscordProfile?) {
    val display = profile?.displayName() ?: "…"
    val handle = profile?.username.orEmpty()
    Column {
        Text(
            text = display,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (handle.isNotBlank() && !handle.equals(display, ignoreCase = true)) {
            Text(
                text = handle,
                color = Look.palette.inkFaint,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun InnerLabel(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun BioText(bio: String) {
    val link = Regex("""https?://\S+""").find(bio)
    if (link == null) {
        Text(bio, color = Look.palette.inkFaint, style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column {
        val before = bio.substring(0, link.range.first).trim()
        if (before.isNotEmpty()) {
            Text(before, color = Look.palette.inkFaint, style = MaterialTheme.typography.bodyMedium)
        }
        Text(link.value, color = Look.palette.link, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActivityBlock(
    track: NowPlaying,
    showGlyph: Boolean,
    showAppIcon: Boolean,
    cardTitle: String?,
) {
    val context = LocalContext.current
    val appIcon = androidx.compose.runtime.remember(track.appPackage, showAppIcon) {
        if (!showAppIcon) null
        else runCatching { context.packageManager.getApplicationIcon(track.appPackage) }.getOrNull()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InnerTint)
            .padding(16.dp),
    ) {
        InnerLabel(stringResource(R.string.profile_listening_heading, cardTitle ?: track.appLabel))
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            HeroArt(track = track, appIcon = appIcon, size = 58.dp, showGlyph = showGlyph)
            Column(
                Modifier
                    .padding(start = 14.dp)
                    .weight(1f),
            ) {
                Text(
                    text = track.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                track.artist?.let {
                    Text(
                        text = it,
                        color = Look.palette.inkFaint,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                track.album?.takeIf { !it.equals(track.artist, ignoreCase = true) }?.let {
                    Text(
                        text = it,
                        color = Look.palette.inkFaint,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TrackTimeline(
                    startMillis = track.startedAtMillis,
                    endMillis = track.endsAtMillis,
                )
            }
        }
    }
}
