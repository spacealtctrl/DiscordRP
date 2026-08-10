package net.spacealtctrl.discordrp.presence.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.spacealtctrl.discordrp.presence.tidy.Candidate
import net.spacealtctrl.discordrp.presence.tidy.CandidateRelease

@Serializable
internal data class BrainzRecordingSearch(
    val recordings: List<BrainzRecording> = emptyList(),
)

@Serializable
internal data class BrainzRecording(
    val id: String? = null,
    val title: String? = null,
    val score: Int = 0,
    val length: Long? = null,
    val video: Boolean? = null,
    @SerialName("artist-credit") val artistCredit: List<BrainzCredit> = emptyList(),
    val releases: List<BrainzRelease> = emptyList(),
)

@Serializable
internal data class BrainzCredit(
    val name: String? = null,
    val joinphrase: String? = null,
    val artist: BrainzArtist? = null,
)

@Serializable
internal data class BrainzArtist(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
internal data class BrainzRelease(
    val id: String? = null,
    val title: String? = null,
    val status: String? = null,
    val date: String? = null,
    @SerialName("release-group") val releaseGroup: BrainzReleaseGroup? = null,
)

@Serializable
internal data class BrainzReleaseGroup(
    val id: String? = null,
    val title: String? = null,
    @SerialName("primary-type") val primaryType: String? = null,
    @SerialName("secondary-types") val secondaryTypes: List<String> = emptyList(),
)

internal fun List<BrainzCredit>.printedName(): String = joinToString("") { credit ->
    (credit.name ?: credit.artist?.name.orEmpty()) + credit.joinphrase.orEmpty()
}.trim()

internal fun BrainzRecording.toCandidate(): Candidate? {
    val name = title?.takeIf { it.isNotBlank() } ?: return null
    return Candidate(
        title = name,
        artist = artistCredit.printedName(),
        relevance = score,
        durationMs = length,
        isVideo = video == true,
        releases = releases.mapNotNull { it.toCandidateRelease() },
    )
}

private fun BrainzRelease.toCandidateRelease(): CandidateRelease? {
    val name = releaseGroup?.title?.takeIf { it.isNotBlank() }
        ?: title?.takeIf { it.isNotBlank() }
        ?: return null
    return CandidateRelease(
        title = name,
        groupId = releaseGroup?.id,
        primaryType = releaseGroup?.primaryType,
        secondaryTypes = releaseGroup?.secondaryTypes.orEmpty(),
        status = status,
        date = date,
    )
}
