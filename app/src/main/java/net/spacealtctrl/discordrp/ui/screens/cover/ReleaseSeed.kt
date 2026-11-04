package net.spacealtctrl.discordrp.ui.screens.cover

import net.spacealtctrl.discordrp.presence.CoverNames
import net.spacealtctrl.discordrp.presence.CoverQueries
import net.spacealtctrl.discordrp.presence.DeezerAlbumDetail
import net.spacealtctrl.discordrp.presence.DeezerAlbumTrack
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

internal object ReleaseSeed {
    const val ADDED_MARKER = "https://musicbrainz.org/discordrp-release-added"
    const val ADD_URL = "https://musicbrainz.org/release/add"

    private const val REPO = "https://github.com/spacealtctrl/DiscordRP"
    private const val DIGITAL = "Digital Media"
    private const val WORLDWIDE = "XW"
    private val BARCODE = Regex("^[0-9]{8,14}$")
    private val DATE = Regex("^([0-9]{4})-([0-9]{2})-([0-9]{2})$")
    private val DEEZER_ALBUM = Regex("^https://www\\.deezer\\.com/(?:[a-z]{2}/)?album/[0-9]+$")

    fun fields(
        album: DeezerAlbumDetail,
        tracks: List<DeezerAlbumTrack>,
        artistMbid: String?,
    ): List<Pair<String, String>> = buildList {
        val artist = album.artist?.name?.trim().orEmpty()
        album.title?.trim()?.takeIf { it.isNotEmpty() }?.let { add("name" to it) }
        addArtist(artist, artistMbid)
        types(album.recordType).forEach { add("type" to it) }
        add("status" to "official")
        album.upc?.trim()?.takeIf(BARCODE::matches)?.let { add("barcode" to it) }
        addDate(album.releaseDate)
        album.label?.trim()?.takeIf { it.isNotEmpty() }?.let { add("labels.0.name" to it) }
        addMediums(tracks, artist)
        val link = album.link?.trim()?.takeIf(DEEZER_ALBUM::matches)
        link?.let { add("urls.0.url" to it) }
        add("edit_note" to "Imported from ${link ?: "Deezer"} with DiscordRP: $REPO")
        add("redirect_uri" to ADDED_MARKER)
    }

    fun body(fields: List<Pair<String, String>>): String = fields.joinToString("&") { (name, value) ->
        "${encode(name)}=${encode(value)}"
    }

    fun addedRelease(raw: String?): String? {
        if (raw == null || !raw.startsWith("$ADDED_MARKER?")) return null
        val query = runCatching { URI(raw).rawQuery }.getOrNull() ?: return null
        return query.split('&')
            .mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
            .firstOrNull { it[0] == "release_mbid" }
            ?.let { URLDecoder.decode(it[1], Charsets.UTF_8.name()) }
            ?.takeIf(CoverQueries::isMbid)
    }

    private fun MutableList<Pair<String, String>>.addArtist(artist: String, mbid: String?) {
        if (artist.isEmpty()) return
        add("artist_credit.names.0.name" to artist)
        if (CoverQueries.isMbid(mbid)) {
            add("artist_credit.names.0.mbid" to mbid!!)
        } else {
            add("artist_credit.names.0.artist.name" to artist)
        }
    }

    private fun MutableList<Pair<String, String>>.addDate(raw: String?) {
        val parts = raw?.trim()?.let(DATE::find)?.groupValues ?: return
        val (year, month, day) = Triple(parts[1], parts[2], parts[3])
        if (year == "0000") return
        add("events.0.date.year" to year)
        if (month != "00") add("events.0.date.month" to month.trimStart('0'))
        if (month != "00" && day != "00") add("events.0.date.day" to day.trimStart('0'))
        add("events.0.country" to WORLDWIDE)
    }

    private fun MutableList<Pair<String, String>>.addMediums(tracks: List<DeezerAlbumTrack>, albumArtist: String) {
        val discs = tracks
            .filter { !it.title.isNullOrBlank() }
            .groupBy { it.diskNumber?.takeIf { disc -> disc > 0 } ?: 1 }
            .toSortedMap()
            .values
        discs.forEachIndexed { disc, discTracks ->
            add("mediums.$disc.format" to DIGITAL)
            discTracks.sortedBy { it.trackPosition ?: Int.MAX_VALUE }.forEachIndexed { index, track ->
                val prefix = "mediums.$disc.track.$index"
                add("$prefix.name" to track.title!!.trim())
                add("$prefix.number" to (track.trackPosition ?: index + 1).toString())
                track.duration?.takeIf { it > 0 }?.let { add("$prefix.length" to (it * 1000L).toString()) }
                val guest = track.artist?.name?.trim().orEmpty()
                if (guest.isNotEmpty() && CoverNames.key(guest) != CoverNames.key(albumArtist)) {
                    add("$prefix.artist_credit.names.0.name" to guest)
                    add("$prefix.artist_credit.names.0.artist.name" to guest)
                }
            }
        }
    }

    private fun types(recordType: String?): List<String> = when (recordType?.lowercase()) {
        "single" -> listOf("single")
        "ep" -> listOf("ep")
        "compile" -> listOf("album", "compilation")
        else -> listOf("album")
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
