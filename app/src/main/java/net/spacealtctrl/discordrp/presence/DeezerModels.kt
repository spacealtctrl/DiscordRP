package net.spacealtctrl.discordrp.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal interface DeezerReply {
    val error: DeezerError?
}

@Serializable
internal data class DeezerAlbumSearch(
    val data: List<DeezerAlbum> = emptyList(),
    override val error: DeezerError? = null,
) : DeezerReply

@Serializable
internal data class DeezerTrackSearch(
    val data: List<DeezerTrack> = emptyList(),
    override val error: DeezerError? = null,
) : DeezerReply

@Serializable
internal data class DeezerAlbum(
    val id: Long? = null,
    val title: String? = null,
    val artist: DeezerArtist? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
)

@Serializable
internal data class DeezerTrack(
    val title: String? = null,
    val artist: DeezerArtist? = null,
    val album: DeezerAlbumRef? = null,
)

@Serializable
internal data class DeezerAlbumRef(
    val id: Long? = null,
    val title: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
)

@Serializable
internal data class DeezerAlbumDetail(
    val title: String? = null,
    val upc: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("record_type") val recordType: String? = null,
    val label: String? = null,
    val link: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
    val artist: DeezerArtist? = null,
    override val error: DeezerError? = null,
) : DeezerReply

@Serializable
internal data class DeezerTrackList(
    val data: List<DeezerAlbumTrack> = emptyList(),
    override val error: DeezerError? = null,
) : DeezerReply

@Serializable
internal data class DeezerAlbumTrack(
    val title: String? = null,
    val duration: Int? = null,
    @SerialName("track_position") val trackPosition: Int? = null,
    @SerialName("disk_number") val diskNumber: Int? = null,
    val artist: DeezerArtist? = null,
)

@Serializable
internal data class DeezerArtist(
    val name: String? = null,
)

@Serializable
internal class DeezerError
