package net.spacealtctrl.discordrp.presence

import kotlin.math.abs

internal object CoverPicks {
    private const val MIN_SCORE = 70
    private const val STRONG_SCORE = 90
    private const val MAX_ARTISTS = 10
    private val COMPOSER_BREAK = Regex("""[:：/]|\s[-–]\s""")

    fun groups(groups: List<BrainzGroup>, album: String): List<String> =
        groups
            .filter { it.id != null && it.score >= MIN_SCORE && CoverNames.sameAlbum(it.title, album) }
            .sortedByDescending { it.score }
            .mapNotNull { it.id }

    fun exactGroup(groups: List<BrainzGroup>, album: String): String? = groups.firstOrNull {
        it.id != null && it.score >= MIN_SCORE && CoverNames.sameAlbum(it.title, album)
    }?.id

    fun songs(recordings: List<BrainzRecording>, title: String, album: String?, credits: List<String>): List<String> {
        val titleKey = CoverNames.key(title)
        return recordings
            .filter { it.score >= MIN_SCORE && CoverNames.key(CoverNames.title(it.title)) == titleKey }
            .flatMap { placesOf(it, album, credits) }
            .sortedByDescending { rank(it, album, titleKey) }
            .mapNotNull { it.id }
            .distinct()
    }

    fun mayNameComposer(album: String): Boolean = COMPOSER_BREAK.containsMatchIn(album)

    fun composerForm(groups: List<BrainzGroup>, album: String): List<String> {
        val want = CoverNames.key(album)
        return groups
            .filter { group ->
                val title = CoverNames.key(group.title)
                group.id != null && group.score >= STRONG_SCORE && title.isNotEmpty() &&
                    group.credit.any { credit ->
                        val name = CoverNames.key(credit.name ?: credit.artist?.name)
                        name.isNotEmpty() && (want == "$name $title" || want == "$title $name")
                    }
            }
            .mapNotNull { it.id }
    }

    fun artistIds(artists: List<BrainzArtist>, names: List<String>): List<String> =
        matching(artists, names).mapNotNull { it.id }.distinct().take(MAX_ARTISTS)

    fun knownByOtherName(artists: List<BrainzArtist>, names: List<String>): Boolean {
        val wanted = names.map(CoverNames::key).filter(String::isNotEmpty).toSet()
        return matching(artists, names).any { CoverNames.key(it.name) !in wanted }
    }

    private fun matching(artists: List<BrainzArtist>, names: List<String>): List<BrainzArtist> {
        val wanted = names.map(CoverNames::key).filter(String::isNotEmpty).toSet()
        return artists.filter { artist ->
            val known = listOfNotNull(artist.name, artist.sortName) + artist.aliases.mapNotNull { it.name }
            artist.id != null && known.any { CoverNames.key(it) in wanted }
        }
    }

    fun byLength(
        recordings: List<BrainzRecording>,
        durationMillis: Long,
        album: String?,
        credits: List<String>,
    ): List<String> {
        val near = recordings.filter { recording ->
            recording.length?.let { abs(it - durationMillis) <= CoverQueries.LENGTH_SLACK_MS } == true
        }
        val titles = near.map { CoverNames.key(CoverNames.title(it.title)) }.toSet()
        if (titles.size != 1) return emptyList()
        return near.flatMap { placesOf(it, album, credits) }.mapNotNull { it.id }.distinct()
    }

    private fun placesOf(recording: BrainzRecording, album: String?, credits: List<String>): List<BrainzGroupRef> {
        val groups = recording.releases.mapNotNull { release -> release.group?.takeIf { it.id != null } }
        if (album == null || !creditedAsShown(recording, credits)) return groups
        return groups.filter { CoverNames.sameAlbum(it.title, album) }
    }

    private fun creditedAsShown(recording: BrainzRecording, credits: List<String>): Boolean {
        val shown = credits.map(CoverNames::key).filter(String::isNotEmpty).toSet()
        val named = recording.credit
            .flatMap { listOfNotNull(it.name, it.artist?.name) }
            .map(CoverNames::key)
            .filter(String::isNotEmpty)
        return named.isEmpty() || named.any(shown::contains)
    }

    private fun rank(group: BrainzGroupRef, album: String?, titleKey: String): Int = when {
        album != null && CoverNames.sameAlbum(group.title, album) -> 2
        CoverNames.key(CoverNames.album(group.title)) == titleKey -> 1
        else -> 0
    }
}
