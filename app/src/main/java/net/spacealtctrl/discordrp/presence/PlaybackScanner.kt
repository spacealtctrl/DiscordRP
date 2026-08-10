package net.spacealtctrl.discordrp.presence

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.service.MediaNoticeListener
import net.spacealtctrl.discordrp.settings.Stash
import javax.inject.Inject

class PlaybackScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stash: Stash,
    private val log: AppLog,
) {
    private val listenerComponent = ComponentName(context, MediaNoticeListener::class.java)

    fun currentTrack(): NowPlaying? {
        val manager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sessions = try {
            manager.getActiveSessions(listenerComponent)
        } catch (e: SecurityException) {
            log.error(TAG, "Media session access revoked")
            return null
        }
        return sessions.firstNotNullOfOrNull { readSession(it) }
    }

    private fun readSession(controller: MediaController): NowPlaying? {
        if (!stash.isPlayerChosen(controller.packageName)) return null

        val state = controller.playbackState?.state
        val playing = state == PlaybackState.STATE_PLAYING
        val idle = state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_STOPPED
        if (stash.hideWhenPaused && idle) return null

        val metadata = controller.metadata ?: return null
        val title = metadata.firstText(
            MediaMetadata.METADATA_KEY_TITLE,
            MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
        ) ?: return null
        val timeline = timeline(controller, playing, metadata)

        return NowPlaying(
            title = title,
            artist = metadata.firstText(
                MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_AUTHOR,
            ),
            album = metadata.firstText(MediaMetadata.METADATA_KEY_ALBUM),
            albumArtist = metadata.firstText(
                MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
                MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_AUTHOR,
            ),
            appLabel = labelFor(controller.packageName),
            appPackage = controller.packageName,
            artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = playing,
            stopped = state == PlaybackState.STATE_STOPPED,
            startedAtMillis = timeline?.first,
            endsAtMillis = timeline?.second,
            durationMillis = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
                .takeIf { it > 0L },
        )
    }

    private fun timeline(
        controller: MediaController,
        playing: Boolean,
        metadata: MediaMetadata,
    ): Pair<Long, Long>? {
        if (!playing) return null
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        if (duration == 0L) return null
        val position = controller.playbackState?.position ?: 0L
        val now = System.currentTimeMillis()
        return (now - position) to (now + duration - position)
    }

    private fun MediaMetadata.firstText(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> getString(key)?.takeIf { it.isNotEmpty() } }

    private fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    private companion object {
        const val TAG = "PlaybackScanner"
    }
}
