package net.spacealtctrl.discordrp.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours

class Stash private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("stash", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _revisions = MutableStateFlow(0)
    val revisions: StateFlow<Int> = _revisions.asStateFlow()

    private fun write(block: SharedPreferences.Editor.() -> Unit) {
        val editor = prefs.edit()
        editor.block()
        editor.apply()
        _revisions.update { it + 1 }
    }

    var authToken: String
        get() = prefs.getString(KEY_TOKEN, null).orEmpty()
        set(value) = write { putString(KEY_TOKEN, value) }

    var selfId: String
        get() = prefs.getString(KEY_SELF_ID, null).orEmpty()
        set(value) = write { putString(KEY_SELF_ID, value) }

    var selfBio: String
        get() = prefs.getString(KEY_SELF_BIO, null).orEmpty()
        set(value) = write { putString(KEY_SELF_BIO, value) }

    var selfNitro: Boolean
        get() = prefs.getBoolean(KEY_SELF_NITRO, false)
        set(value) = write { putBoolean(KEY_SELF_NITRO, value) }

    var profileCache: String
        get() = prefs.getString(KEY_PROFILE_CACHE, null).orEmpty()
        set(value) = write { putString(KEY_PROFILE_CACHE, value) }

    val signedIn: Boolean get() = authToken.isNotBlank()

    var presenceEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRESENCE_ON, true)
        set(value) = write { putBoolean(KEY_PRESENCE_ON, value) }

    var mood: Mood
        get() = Mood.fromWire(prefs.getString(KEY_MOOD, null))
        set(value) = write { putString(KEY_MOOD, value.wire) }

    var mobileBadge: Boolean
        get() = prefs.getBoolean(KEY_MOBILE_BADGE, true)
        set(value) = write { putBoolean(KEY_MOBILE_BADGE, value) }

    var showArtist: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ARTIST, false)
        set(value) = write { putBoolean(KEY_SHOW_ARTIST, value) }

    var showAlbum: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ALBUM, false)
        set(value) = write { putBoolean(KEY_SHOW_ALBUM, value) }

    var showPlaybackGlyph: Boolean
        get() = prefs.getBoolean(KEY_SHOW_GLYPH, false)
        set(value) = write { putBoolean(KEY_SHOW_GLYPH, value) }

    var showAppIcon: Boolean
        get() = prefs.getBoolean(KEY_SHOW_APP_ICON, true)
        set(value) = write { putBoolean(KEY_SHOW_APP_ICON, value) }

    var showTimeline: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TIMELINE, true)
        set(value) = write { putBoolean(KEY_SHOW_TIMELINE, value) }

    var hideWhenPaused: Boolean
        get() = prefs.getBoolean(KEY_HIDE_PAUSED, false)
        set(value) = write { putBoolean(KEY_HIDE_PAUSED, value) }

    var tidyMetadata: Boolean
        get() = prefs.getBoolean(KEY_TIDY_METADATA, true)
        set(value) = write { putBoolean(KEY_TIDY_METADATA, value) }

    var deezerFallback: Boolean
        get() = prefs.getBoolean(KEY_DEEZER_FALLBACK, true)
        set(value) = write { putBoolean(KEY_DEEZER_FALLBACK, value) }

    var songAsTitle: Boolean
        get() = prefs.getBoolean(KEY_TITLE_SONG, false)
        set(value) = write { putBoolean(KEY_TITLE_SONG, value) }

    var artistAsTitle: Boolean
        get() = prefs.getBoolean(KEY_TITLE_ARTIST, false)
        set(value) = write { putBoolean(KEY_TITLE_ARTIST, value) }

    var chosenPlayers: Set<String>
        get() = prefs.getStringSet(KEY_PLAYERS, null) ?: DEFAULT_PLAYERS
        set(value) = write { putStringSet(KEY_PLAYERS, value) }

    fun isPlayerChosen(packageName: String?): Boolean =
        packageName != null && packageName in chosenPlayers

    fun togglePlayer(packageName: String) {
        chosenPlayers = chosenPlayers.let {
            if (packageName in it) it - packageName else it + packageName
        }
    }

    var alertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALERTS_ON, true)
        set(value) = write { putBoolean(KEY_ALERTS_ON, value) }

    var alertScope: AlertScope
        get() = AlertScope.fromStored(prefs.getString(KEY_ALERT_SCOPE, null))
        set(value) = write { putString(KEY_ALERT_SCOPE, value.stored) }

    var skipBots: Boolean
        get() = prefs.getBoolean(KEY_SKIP_BOTS, false)
        set(value) = write { putBoolean(KEY_SKIP_BOTS, value) }

    var clearReadElsewhere: Boolean
        get() = prefs.getBoolean(KEY_CLEAR_READ_ELSEWHERE, false)
        set(value) = write { putBoolean(KEY_CLEAR_READ_ELSEWHERE, value) }

    var startOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTOSTART, false)
        set(value) = write { putBoolean(KEY_AUTOSTART, value) }

    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = write { putBoolean(KEY_SETUP_DONE, value) }

    var appIconAssets: Map<String, String>
        get() = readMap(KEY_ICON_ASSETS)
        set(value) = write { putString(KEY_ICON_ASSETS, json.encodeToString(value)) }

    var coverAssets: Map<String, String>
        get() = readMap(KEY_COVER_ASSETS)
        set(value) = write { putString(KEY_COVER_ASSETS, json.encodeToString(value)) }

    fun purgeStaleArtCaches(now: Long = System.currentTimeMillis()) {
        val last = prefs.getLong(KEY_ART_PURGED_AT, 0L)
        if (last == 0L) {
            write { putLong(KEY_ART_PURGED_AT, now) }
            return
        }
        if (now - last > DAY_MS) {
            write {
                remove(KEY_ICON_ASSETS)
                remove(KEY_COVER_ASSETS)
                putLong(KEY_ART_PURGED_AT, now)
            }
        }
    }

    private fun readMap(key: String): Map<String, String> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }
            .getOrDefault(emptyMap())
    }

    companion object {
        @Volatile
        private var shared: Stash? = null

        fun of(context: Context): Stash =
            shared ?: synchronized(this) {
                shared ?: Stash(context).also { shared = it }
            }

        private const val KEY_TOKEN = "discord.token"
        private const val KEY_SELF_ID = "discord.user.id"
        private const val KEY_SELF_BIO = "discord.user.bio"
        private const val KEY_SELF_NITRO = "discord.user.nitro"
        private const val KEY_PROFILE_CACHE = "discord.profile.cache"

        private const val KEY_PRESENCE_ON = "presence.enabled"
        private const val KEY_MOOD = "presence.mood"
        private const val KEY_MOBILE_BADGE = "presence.mobile_badge"
        private const val KEY_SHOW_ARTIST = "presence.show_artist"
        private const val KEY_SHOW_ALBUM = "presence.show_album"
        private const val KEY_SHOW_GLYPH = "presence.show_playback_glyph"
        private const val KEY_SHOW_APP_ICON = "presence.show_app_icon"
        private const val KEY_SHOW_TIMELINE = "presence.show_timeline"
        private const val KEY_HIDE_PAUSED = "presence.hide_when_paused"
        private const val KEY_TIDY_METADATA = "presence.tidy_metadata"
        private const val KEY_DEEZER_FALLBACK = "presence.deezer_fallback"
        private const val KEY_TITLE_SONG = "presence.song_as_title"
        private const val KEY_TITLE_ARTIST = "presence.artist_as_title"
        private const val KEY_PLAYERS = "presence.players"

        private const val KEY_ALERTS_ON = "alerts.enabled"
        private const val KEY_ALERT_SCOPE = "alerts.scope"
        private const val KEY_SKIP_BOTS = "alerts.skip_bots"
        private const val KEY_CLEAR_READ_ELSEWHERE = "alerts.clear_read_elsewhere"

        private const val KEY_AUTOSTART = "app.start_on_boot"
        private const val KEY_SETUP_DONE = "app.setup_complete"

        private const val KEY_ICON_ASSETS = "art.icon_assets"
        private const val KEY_COVER_ASSETS = "art.cover_assets"
        private const val KEY_ART_PURGED_AT = "art.purged_at"

        private val DAY_MS = 24.hours.inWholeMilliseconds

        private val STREAMING_SERVICES = setOf(
            "com.amazon.mp3",
            "com.apple.android.music",
            "com.aspiro.tidal",
            "com.pandora.android",
            "com.rhapsody",
            "com.soundcloud.android",
            "com.spotify.music",
            "deezer.android.app",
        )

        private val LOCAL_PLAYERS = setOf(
            "app.symfonik.music.player",
            "com.google.android.music",
            "com.jrtstudio.AnotherMusicPlayer",
            "com.maxmpz.audioplayer",
            "com.sec.android.app.music",
            "com.sonyericsson.music",
            "com.tbig.playerpro",
            "org.oxycblt.auxio",
            "code.name.monkey.retromusic",
        )

        private val VIDEO_APPS = setOf(
            "com.google.android.apps.mediashell",
            "com.google.android.videos",
            "com.mxtech.videoplayer.ad",
            "com.mxtech.videoplayer.pro",
            "com.netflix.mediaclient",
            "org.videolan.vlc",
            "tv.twitch.android.app",
        )

        private val YOUTUBE_APPS = setOf(
            "com.google.android.apps.youtube.music",
            "com.google.android.apps.youtube.kids",
            "com.google.android.apps.youtube.unplugged",
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.google.android.youtube.googletv",
        )

        private val ALT_YOUTUBE_APPS = setOf(
            "org.schabi.newpipe",
            "org.polymorphicshade.tubular",
            "org.wisso.newpipematerial",
            "InfinityLoop1309.NewPipeEnhanced",
            "com.github.libretube",
            "com.futo.platformplayer",
            "free.rm.skytube",
            "free.rm.skytube.oss",
            "free.rm.skytube.extra",
            "com.kapp.youtube.final",
        )

        val DEFAULT_PLAYERS: Set<String> =
            STREAMING_SERVICES + LOCAL_PLAYERS + YOUTUBE_APPS + VIDEO_APPS + ALT_YOUTUBE_APPS
    }
}
