package net.spacealtctrl.discordrp.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BrainzGroupSearch(
    @SerialName("release-groups") val groups: List<BrainzGroup> = emptyList(),
)

@Serializable
internal data class BrainzGroup(
    val id: String? = null,
    val title: String? = null,
    val score: Int = 0,
    @SerialName("artist-credit") val credit: List<BrainzCredit> = emptyList(),
)

@Serializable
internal data class BrainzCredit(
    val name: String? = null,
    val artist: BrainzCreditArtist? = null,
)

@Serializable
internal data class BrainzCreditArtist(
    val name: String? = null,
)

@Serializable
internal data class BrainzRecordingSearch(
    val recordings: List<BrainzRecording> = emptyList(),
)

@Serializable
internal data class BrainzRecording(
    val title: String? = null,
    val score: Int = 0,
    val length: Long? = null,
    val releases: List<BrainzRelease> = emptyList(),
    @SerialName("artist-credit") val credit: List<BrainzCredit> = emptyList(),
)

@Serializable
internal data class BrainzRelease(
    @SerialName("release-group") val group: BrainzGroupRef? = null,
)

@Serializable
internal data class BrainzGroupRef(
    val id: String? = null,
    val title: String? = null,
)

@Serializable
internal data class BrainzReleaseBrowse(
    val releases: List<BrainzReleaseItem> = emptyList(),
)

@Serializable
internal data class BrainzReleaseItem(
    val id: String? = null,
    val status: String? = null,
    val date: String? = null,
    val media: List<BrainzMedium> = emptyList(),
)

@Serializable
internal data class BrainzMedium(
    val format: String? = null,
)

@Serializable
internal data class BrainzReleaseLookup(
    @SerialName("release-group") val group: BrainzGroupRef? = null,
)

@Serializable
internal data class BrainzArtistSearch(
    val artists: List<BrainzArtist> = emptyList(),
)

@Serializable
internal data class BrainzArtist(
    val id: String? = null,
    val name: String? = null,
    @SerialName("sort-name") val sortName: String? = null,
    val aliases: List<BrainzAlias> = emptyList(),
)

@Serializable
internal data class BrainzAlias(
    val name: String? = null,
)
