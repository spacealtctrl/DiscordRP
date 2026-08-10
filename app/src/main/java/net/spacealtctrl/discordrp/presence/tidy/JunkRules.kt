package net.spacealtctrl.discordrp.presence.tidy

internal object JunkRules {
    private const val NOISE_PHRASE =
        """official\s+music\s+video|official\s+video|official\s+audio|official\s+visuali[sz]er|""" +
            """official\s+lyrics?\s*video|official\s+hd\s+video|official\s+trailer|""" +
            """music\s+video|lyrics?\s*video|audio\s+only|full\s+audio|""" +
            """free\s+download|out\s+now|now\s+available|clean\s+version|with\s+lyrics|""" +
            """color\s+coded\s+lyrics?[^)\]]*|han/rom/eng[^)\]]*|""" +
            """sub\s+espa\S+|subtitulado|legendado|tradu\S+|vietsub|thaisub|engsub|""" +
            """video\s+oficial|audio\s+oficial|videoclip\s+oficial|clip\s+officiel|""" +
            """performance\s+video|dance\s+practice|audio\s+version"""

    private const val NOISE_WORD =
        """official|lyrics|visuali[sz]er|hd|hq|4k|8k|1080p|720p|mv|m/v|hi-?res|""" +
            """remaster(?:ed)?(?:\s+\d{4})?|explicit|letra|teaser"""

    val BRACKET_NOISE = Regex(
        """[(\[{【〔《（]\s*[^)\]}】〕》）]*?\b(?:$NOISE_PHRASE|$NOISE_WORD)\b[^)\]}】〕》）]*?\s*[)\]}】〕》）]""",
        RegexOption.IGNORE_CASE,
    )

    val TRAILING_NOISE = Regex(
        """(?:\s*[|/·•-]\s*(?:$NOISE_PHRASE|$NOISE_WORD)|\s+(?:$NOISE_PHRASE))\s*$""",
        RegexOption.IGNORE_CASE,
    )

    val LEADING_INDEX = Regex("""^\s*\d{1,2}\s*[.)]\s+""")

    val TRAILING_BRACKET = Regex("""\s*[(\[][^)\]]*[)\]]\s*$""")

    val TOPIC_CHANNEL = Regex("""\s*-\s*Topic\s*$""", RegexOption.IGNORE_CASE)

    val VEVO_CHANNEL = Regex("""VEVO\s*$""", RegexOption.IGNORE_CASE)

    val CHANNEL_HANDLE = Regex("""^@|_|\d{4,}$""")

    val NOT_ONE_SONG = Regex(
        """\b(?:full\s+album|album\s+completo|greatest\s+hits|all\s+songs|full\s+ep|""" +
            """\d+\s*hours?\s+(?:of|mix|version|non-?stop)|megamix|""" +
            """live\s+stream|livestream|radio\s+24/?7|""" +
            """full\s+movie|full\s+episode|podcast\s+ep(?:isode)?\s*\d+)\b""",
        RegexOption.IGNORE_CASE,
    )

    val VIDEO_TITLE_MARKER = Regex(
        """\b(?:official\s+music\s+video|official\s+video|official\s+audio|music\s+video|""" +
            """lyrics?\s*video|official\s+visuali[sz]er|video\s+oficial|clip\s+officiel)\b""",
        RegexOption.IGNORE_CASE,
    )

    val WHITESPACE = Regex("""\s+""")

    val SEPARATORS = listOf(" - ", " – ", " — ", " ~ ", " | ", " // ", " ‐ ", " ‒ ")

    val CJK_TITLE = Regex("""^(.+?)\s*[「『]\s*(.+?)\s*[」』]""")

    val JUNK_ALBUMS: Set<String> = setOf(
        "newpipe", "newpipe material", "tubular", "libretube", "piped", "pipepipe", "grayjay",
        "skytube", "youtube", "youtube music", "yt music", "ymusic", "revanced", "innertune",
        "outertune", "simpmusic", "metrolist", "muzza", "vivimusic", "gyawun",
        "unknown album", "unknown", "unknown artist", "untitled", "video", "videos", "media",
        "stream", "livestream", "live stream", "audio", "music", "no album", "none", "null",
        "vlc", "mpv", "mpv android", "browser", "firefox", "fennec", "chrome", "chromium",
        "mull", "brave", "twitch", "soundcloud", "podcast", "podcasts", "downloads", "local",
        "local files", "sd card", "internal storage", "recently added", "n a",
    )
}
