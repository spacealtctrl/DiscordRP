package net.spacealtctrl.discordrp.presence.tidy

object MatchRank {
    private const val ACCEPT_FLOOR = 0.70
    private const val TITLE_BAR_WITH_ARTIST = 0.80
    private const val ARTIST_BAR = 0.62
    private const val TITLE_BAR_ALONE = 0.90
    private const val RELEASE_FLOOR = -1.5
    private const val CLEAN_ALBUM_BAR = 4.0
    private const val SAME_ACT = 0.75
    private const val VARIANT_PENALTY = 0.15

    private val STATUS_SCORE = mapOf(
        "Official" to 3.0,
        "Promotion" to 0.5,
        "Pseudo-Release" to -2.0,
        "Bootleg" to -6.0,
        "Withdrawn" to -5.0,
        "Cancelled" to -5.0,
    )

    private val PRIMARY_SCORE = mapOf(
        "Album" to 3.0,
        "EP" to 2.0,
        "Single" to 2.0,
        "Broadcast" to -2.0,
        "Other" to -1.0,
    )

    private val SECONDARY_SCORE = mapOf(
        "Compilation" to -4.0,
        "Live" to -4.0,
        "DJ-mix" to -4.0,
        "Remix" to -2.5,
        "Demo" to -2.0,
        "Soundtrack" to -0.5,
        "Interview" to -6.0,
        "Spokenword" to -4.0,
        "Audiobook" to -6.0,
        "Field recording" to -4.0,
        "Mixtape/Street" to -1.5,
    )

    private val VARIANT_WORDS = setOf(
        "live", "remix", "acoustic", "instrumental", "karaoke", "cover", "unplugged",
        "demo", "nightcore", "reprise", "acapella", "sped", "slowed", "rehearsal", "session",
        "tribute", "backing", "originally", "made", "popularized",
    )

    internal data class Scored(
        val rank: Double,
        val accept: Double,
        val titleSim: Double,
        val artistSim: Double,
    )

    fun resolve(
        candidates: List<Candidate>,
        wantTitle: String,
        wantArtist: String?,
        wantDurationMs: Long?,
        albumHint: String? = null,
    ): TrackFacts? {
        if (candidates.isEmpty()) return null
        val hasArtist = !wantArtist.isNullOrBlank()

        val scored = candidates
            .map { it to score(it, wantTitle, wantArtist, wantDurationMs) }
            .sortedByDescending { it.second.rank }

        val verified = scored.filter { accepts(it.second, hasArtist) }
        if (verified.isEmpty()) return null

        val (best, bestScore) = verified.first()

        val releases = verified
            .filter { (candidate, _) ->
                candidate === best || TextCanon.similarity(candidate.artist, best.artist) >= SAME_ACT
            }
            .flatMap { entry -> entry.first.releases.map { it to entry.first } }
        val picked = releases
            .maxByOrNull { (release, _) ->
                releaseScore(release) + if (TextCanon.sameText(release.title, albumHint)) 5.0 else 0.0
            }
            ?.takeIf { (release, _) ->
                releaseScore(release) >= RELEASE_FLOOR || TextCanon.sameText(release.title, albumHint)
            }
        val chosen = picked?.first
        val borrowed = picked != null && picked.second !== best
        val hinted = chosen != null && TextCanon.sameText(chosen.title, albumHint)

        val quality = when {
            chosen == null -> AlbumQuality.NONE
            hinted -> AlbumQuality.CLEAN
            borrowed -> AlbumQuality.MESSY
            releaseScore(chosen) >= CLEAN_ALBUM_BAR -> AlbumQuality.CLEAN
            else -> AlbumQuality.MESSY
        }

        return TrackFacts(
            title = best.title,
            artist = best.artist,
            album = chosen?.title,
            releaseGroupId = chosen?.groupId,
            coverUrl = chosen?.coverUrl,
            confidence = bestScore.accept,
            albumQuality = quality,
        )
    }

    internal fun releaseScore(release: CandidateRelease): Double {
        var score = STATUS_SCORE[release.status] ?: 0.0
        score += PRIMARY_SCORE[release.primaryType] ?: 0.0
        release.secondaryTypes.forEach { score += SECONDARY_SCORE[it] ?: -1.0 }
        val year = release.date?.take(4)?.toIntOrNull()?.takeIf { it in 1900..2100 }
        if (release.date?.isNotBlank() == true) score += 0.5
        if (year != null) score += (2100 - year) / 1000.0
        return score
    }

    internal fun durationTerm(wantMs: Long?, candidateMs: Long?): Double {
        if (wantMs == null || wantMs <= 0L || candidateMs == null || candidateMs <= 0L) return 0.0
        val driftSeconds = Math.abs(wantMs - candidateMs) / 1000.0
        return when {
            driftSeconds <= 7 -> 0.20
            driftSeconds <= 20 -> 0.10
            driftSeconds <= 45 -> 0.0
            driftSeconds <= 120 -> -0.10
            else -> -0.20
        }
    }

    internal fun score(
        candidate: Candidate,
        wantTitle: String,
        wantArtist: String?,
        wantDurationMs: Long?,
    ): Scored {
        val titleSim = TextCanon.similarity(wantTitle, candidate.title)
        val artistSim =
            if (wantArtist.isNullOrBlank()) 0.0
            else TextCanon.similarity(wantArtist, candidate.artist)
        val relevance = candidate.relevance.coerceIn(0, 100) / 100.0

        var base = if (wantArtist.isNullOrBlank()) {
            0.70 * titleSim + 0.15 * relevance
        } else {
            0.45 * titleSim + 0.33 * artistSim + 0.10 * relevance
        }
        if (candidate.isVideo) base -= 0.05
        base += bestReleaseBonus(candidate)

        if ((variantWords(candidate.title) - variantWords(wantTitle)).isNotEmpty()) {
            base -= VARIANT_PENALTY
        }
        if (!wantArtist.isNullOrBlank() &&
            (variantWords(candidate.artist) - variantWords(wantArtist)).isNotEmpty()
        ) {
            base -= VARIANT_PENALTY
        }

        return Scored(
            rank = base + durationTerm(wantDurationMs, candidate.durationMs),
            accept = base,
            titleSim = titleSim,
            artistSim = artistSim,
        )
    }

    private fun accepts(scored: Scored, hasArtist: Boolean): Boolean {
        if (scored.accept < ACCEPT_FLOOR) return false
        return if (hasArtist) {
            scored.titleSim >= TITLE_BAR_WITH_ARTIST && scored.artistSim >= ARTIST_BAR
        } else {
            scored.titleSim >= TITLE_BAR_ALONE
        }
    }

    private fun bestReleaseBonus(candidate: Candidate): Double {
        if (candidate.releases.isEmpty()) return -0.10
        val best = candidate.releases.maxOf { releaseScore(it) }
        return 0.15 * (best / 6.0).coerceIn(-1.0, 1.0)
    }

    private fun variantWords(value: String): Set<String> =
        TextCanon.fold(value).split(' ').filterTo(mutableSetOf()) { it in VARIANT_WORDS }
}
