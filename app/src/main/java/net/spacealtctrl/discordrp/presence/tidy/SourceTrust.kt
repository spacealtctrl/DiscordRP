package net.spacealtctrl.discordrp.presence.tidy

enum class SourceTrust {
    VIDEO,
    TAGGED,
}

object SourceKind {
    val VIDEO_SOURCES: Set<String> = setOf(
        "com.google.android.youtube",
        "com.google.android.youtube.tv",
        "com.google.android.youtube.googletv",
        "com.google.android.apps.youtube.kids",
        "com.google.android.apps.youtube.unplugged",
        "com.google.android.apps.youtube.mango",
        "app.revanced.android.youtube",
        "app.rvx.android.youtube",
        "com.vanced.android.youtube",
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
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "org.mozilla.fennec_fdroid",
        "us.spotco.fennec_dos",
        "io.github.forkmaintainers.iceraven",
        "com.android.chrome",
        "org.chromium.chrome",
        "com.brave.browser",
        "org.bromite.bromite",
        "com.duckduckgo.mobile.android",
        "org.videolan.vlc",
        "is.xyz.mpv",
        "com.mxtech.videoplayer.ad",
        "com.mxtech.videoplayer.pro",
        "tv.twitch.android.app",
        "com.google.android.videos",
        "com.google.android.apps.mediashell",
    )

    fun trustOf(packageName: String?): SourceTrust =
        if (packageName != null && packageName in VIDEO_SOURCES) SourceTrust.VIDEO
        else SourceTrust.TAGGED

    fun isJunkAlbum(album: String?, appLabel: String?): Boolean {
        val folded = TextCanon.fold(album)
        if (folded.isEmpty()) return true
        if (folded in JunkRules.JUNK_ALBUMS) return true
        if (TextCanon.sameText(album, appLabel)) return true
        return false
    }

    fun isJunkArtist(artist: String?, appLabel: String?): Boolean {
        val raw = artist?.trim().orEmpty()
        if (raw.isEmpty()) return true
        if (JunkRules.TOPIC_CHANNEL.containsMatchIn(raw)) return false
        if (JunkRules.VEVO_CHANNEL.containsMatchIn(raw)) return true
        if (TextCanon.sameText(raw, appLabel)) return true
        if (TextCanon.fold(raw) in JunkRules.JUNK_ALBUMS) return true
        if (!raw.contains(' ') && JunkRules.CHANNEL_HANDLE.containsMatchIn(raw)) return true
        return false
    }
}
