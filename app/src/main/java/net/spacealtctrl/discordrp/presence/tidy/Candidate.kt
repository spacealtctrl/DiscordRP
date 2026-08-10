package net.spacealtctrl.discordrp.presence.tidy

data class CandidateRelease(
    val title: String,
    val groupId: String? = null,
    val primaryType: String? = null,
    val secondaryTypes: List<String> = emptyList(),
    val status: String? = null,
    val date: String? = null,
    val coverUrl: String? = null,
)

data class Candidate(
    val title: String,
    val artist: String,
    val relevance: Int = 100,
    val durationMs: Long? = null,
    val isVideo: Boolean = false,
    val releases: List<CandidateRelease> = emptyList(),
)

enum class AlbumQuality {
    CLEAN,
    MESSY,
    NONE,
}

data class TrackFacts(
    val title: String,
    val artist: String,
    val album: String?,
    val releaseGroupId: String? = null,
    val coverUrl: String? = null,
    val confidence: Double = 0.0,
    val albumQuality: AlbumQuality = AlbumQuality.NONE,
)
