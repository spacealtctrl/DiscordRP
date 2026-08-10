package net.spacealtctrl.discordrp.presence

import net.spacealtctrl.discordrp.BuildConfig
import net.spacealtctrl.discordrp.gateway.ActivityCard
import net.spacealtctrl.discordrp.gateway.ArtRefs
import net.spacealtctrl.discordrp.gateway.PresenceUpdate
import net.spacealtctrl.discordrp.gateway.TimeSpan
import net.spacealtctrl.discordrp.settings.Mood
import net.spacealtctrl.discordrp.settings.Stash
import javax.inject.Inject

data class ActivityLines(
    val name: String,
    val details: String?,
    val state: String?,
    val largeText: String?,
)

class PresenceComposer @Inject constructor(
    private val stash: Stash,
    private val artRegistry: ArtRegistry,
) {
    fun quietUpdate(mood: Mood = stash.mood): PresenceUpdate =
        PresenceUpdate(status = mood.wire, afk = false, since = 0L)

    suspend fun listeningUpdate(track: NowPlaying): PresenceUpdate {
        val lines = arrangeLines(
            title = track.title,
            artist = track.artist.takeIf { stash.showArtist },
            album = track.album.takeIf { stash.showAlbum },
            appLabel = track.appLabel,
            songAsTitle = stash.songAsTitle,
            artistAsTitle = stash.artistAsTitle,
        )

        val art = resolveArt(track)
        val assets = art?.let { (large, small) ->
            ArtRefs(
                largeImage = large,
                smallImage = small,
                largeText = ActivityCard.clip(lines.largeText),
                smallText = ActivityCard.clip(track.appLabel),
            )
        }

        return PresenceUpdate(
            status = stash.mood.wire,
            afk = true,
            since = 0L,
            activities = listOf(
                ActivityCard(
                    name = lines.name,
                    type = LISTENING,
                    details = ActivityCard.clip(lines.details),
                    state = ActivityCard.clip(lines.state),
                    timestamps = track.takeIf { stash.showTimeline }?.let {
                        TimeSpan(start = it.startedAtMillis, end = it.endsAtMillis)
                            .takeIf { span -> span.start != null || span.end != null }
                    },
                    assets = assets,
                    applicationId = BuildConfig.DISCORD_APP_ID,
                ),
            ),
        )
    }

    private suspend fun resolveArt(track: NowPlaying): Pair<String, String?>? {
        val cover = ArtSource.Cover(
            cacheKey = coverKey(track),
            artist = track.albumArtist ?: track.artist,
            album = track.album,
            releaseGroupId = track.releaseGroupId,
            directUrl = track.coverUrl,
        ).takeIf {
            !track.album.isNullOrBlank() ||
                track.releaseGroupId != null ||
                track.coverUrl != null
        }
        val icon = ArtSource.AppIcon(track.appPackage).takeIf { stash.showAppIcon }

        val coverRef = artRegistry.resolve(cover)
        val glyphRef = artRegistry.resolve(playbackGlyph(track))
        if (coverRef != null) {
            return coverRef to (glyphRef ?: artRegistry.resolve(icon))
        }
        val iconRef = artRegistry.resolve(icon)
        return when {
            iconRef != null -> iconRef to glyphRef
            glyphRef != null -> glyphRef to null
            else -> null
        }
    }

    private fun playbackGlyph(track: NowPlaying): ArtSource.Remote? {
        if (!stash.showPlaybackGlyph) return null
        val name = when {
            track.playing -> "play"
            track.stopped -> "stop"
            else -> "pause"
        }
        return ArtSource.Remote(url = "$GLYPH_BASE/$name.png", cacheKey = "glyph:$name")
    }

    private fun coverKey(track: NowPlaying): String = track.releaseGroupId?.let { "rg:$it" }
        ?: "${track.albumArtist ?: track.artist}|${track.album}".lowercase()

    companion object {
        const val LISTENING = 2

        fun cardName(track: NowPlaying, stash: Stash): String = arrangeLines(
            title = track.title,
            artist = track.artist.takeIf { stash.showArtist },
            album = track.album.takeIf { stash.showAlbum },
            appLabel = track.appLabel,
            songAsTitle = stash.songAsTitle,
            artistAsTitle = stash.artistAsTitle,
        ).name

        const val GLYPH_BASE =
            "https://raw.githubusercontent.com/spacealtctrl/DiscordRP/main/assets/glyphs"

        fun arrangeLines(
            title: String,
            artist: String?,
            album: String?,
            appLabel: String,
            songAsTitle: Boolean,
            artistAsTitle: Boolean,
        ): ActivityLines = when {
            artistAsTitle -> ActivityLines(
                name = artist?.takeIf { it.isNotBlank() } ?: title,
                details = title,
                state = artist.orEmpty(),
                largeText = album,
            )
            songAsTitle -> ActivityLines(
                name = title,
                details = artist,
                state = album,
                largeText = appLabel,
            )
            else -> ActivityLines(
                name = appLabel,
                details = title,
                state = artist,
                largeText = album,
            )
        }
    }
}
