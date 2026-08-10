package net.spacealtctrl.discordrp.presence.tidy

data class ScrubbedTrack(
    val title: String,
    val artist: String?,
    val album: String?,
    val worthLookup: Boolean,
)

object TitleScrub {
    private const val SPLIT_CONFIDENCE = 0.75

    fun scrubTitle(raw: String): String {
        var text = JunkRules.BRACKET_NOISE.replace(raw, " ")
        text = JunkRules.LEADING_INDEX.replace(text, "")
        repeat(3) {
            val next = JunkRules.TRAILING_NOISE.replace(text, "")
            if (next == text) return@repeat
            text = next
        }
        text = JunkRules.WHITESPACE.replace(text, " ").trim()
        text = text.trim(' ', '-', '–', '—', '|', '·', '•', '\t')
        val tidied = JunkRules.WHITESPACE.replace(text, " ").trim()
        return tidied.ifBlank { raw.trim() }
    }

    fun stripTrailingBracket(title: String): String {
        var text = title
        repeat(3) {
            val next = JunkRules.TRAILING_BRACKET.replace(text, "").trim()
            if (next == text || next.isBlank()) return@repeat
            text = next
        }
        return text.ifBlank { title }
    }

    fun cleanArtist(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val topic = JunkRules.TOPIC_CHANNEL.replace(value, "").trim()
        if (topic != value) return topic.ifBlank { null }
        return JunkRules.VEVO_CHANNEL.replace(value, "").trim().ifBlank { null }
    }

    data class TitleSplit(
        val left: String,
        val right: String,
        val unambiguous: Boolean,
    )

    fun splitArtistTitle(title: String): TitleSplit? {
        val found = JunkRules.SEPARATORS
            .map { it to title.indexOf(it) }
            .filter { (_, at) -> at > 0 }
            .minByOrNull { (_, at) -> at }
        if (found != null) {
            val (separator, at) = found
            val left = title.substring(0, at).trim()
            val right = title.substring(at + separator.length).trim()
            if (left.isNotEmpty() && right.isNotEmpty()) {
                return TitleSplit(left, right, unambiguous = false)
            }
        }
        val cjk = JunkRules.CJK_TITLE.find(title) ?: return null
        val left = cjk.groupValues[1].trim()
        val right = cjk.groupValues[2].trim()
        return if (left.isNotEmpty() && right.isNotEmpty()) {
            TitleSplit(left, right, unambiguous = true)
        } else {
            null
        }
    }

    fun scrub(
        title: String,
        artist: String?,
        album: String?,
        appLabel: String?,
    ): ScrubbedTrack {
        val cleanedTitle = scrubTitle(title)
        val namedArtist = cleanArtist(artist)
        val artistIsJunk = SourceKind.isJunkArtist(artist, appLabel)
        val fromTopic = artist != null && JunkRules.TOPIC_CHANNEL.containsMatchIn(artist)
        val artistEchoesTitle = TextCanon.sameText(namedArtist, cleanedTitle) ||
            TextCanon.sameText(namedArtist, title)

        var finalTitle = cleanedTitle
        var finalArtist = if (artistIsJunk || artistEchoesTitle) null else namedArtist

        val split = splitArtistTitle(cleanedTitle)
        if (split != null) {
            val left = scrubTitle(split.left)
            val right = scrubTitle(split.right)
            val trusted = finalArtist
            val hint = namedArtist?.takeUnless { artistEchoesTitle }
            val leftLikeArtist = TextCanon.similarity(hint, left)
            val rightLikeArtist = TextCanon.similarity(hint, right)
            when {
                fromTopic && trusted != null ->
                    finalTitle = if (leftLikeArtist >= SPLIT_CONFIDENCE) right else left

                split.unambiguous -> {
                    finalTitle = right
                    finalArtist = left
                }

                trusted != null && leftLikeArtist >= SPLIT_CONFIDENCE -> finalTitle = right
                trusted != null && rightLikeArtist >= SPLIT_CONFIDENCE -> finalTitle = left
                trusted != null -> Unit

                rightLikeArtist >= SPLIT_CONFIDENCE && rightLikeArtist > leftLikeArtist -> {
                    finalTitle = left
                    finalArtist = right
                }

                else -> {
                    finalTitle = right
                    finalArtist = left
                }
            }
        }

        if (finalArtist == null && namedArtist != null && !namedArtist.equals(artist?.trim(), true)) {
            finalArtist = namedArtist
        }

        val keptAlbum = album?.takeUnless { SourceKind.isJunkAlbum(it, appLabel) }

        return ScrubbedTrack(
            title = finalTitle.ifBlank { title.trim() },
            artist = finalArtist?.takeIf { it.isNotBlank() },
            album = keptAlbum?.trim()?.takeIf { it.isNotBlank() },
            worthLookup = !JunkRules.NOT_ONE_SONG.containsMatchIn(title) &&
                finalTitle.isNotBlank(),
        )
    }
}
