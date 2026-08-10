package net.spacealtctrl.discordrp.presence

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.spacealtctrl.discordrp.presence.tidy.AlbumQuality
import net.spacealtctrl.discordrp.presence.tidy.TrackFacts
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Serializable
private data class MemoEntry(
    val title: String = "",
    val artist: String = "",
    val album: String? = null,
    val releaseGroupId: String? = null,
    val coverUrl: String? = null,
    val confidence: Double = 0.0,
    val quality: String = AlbumQuality.NONE.name,
    val storedAt: Long = 0L,
    val miss: Boolean = false,
)

@Singleton
class TrackMemo @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.applicationContext
        .getSharedPreferences("tidy.memo", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()

    private val entries: LinkedHashMap<String, MemoEntry> by lazy { readAll() }

    @JvmInline
    value class Recalled(val facts: TrackFacts?)

    suspend fun recall(key: String): Recalled? = lock.withLock {
        val entry = entries[key] ?: return@withLock null
        val age = System.currentTimeMillis() - entry.storedAt
        val lifetime = if (entry.miss) MISS_LIFETIME_MS else HIT_LIFETIME_MS
        if (age !in 0 until lifetime) {
            entries.remove(key)
            return@withLock null
        }
        entries.remove(key)
        entries[key] = entry
        if (entry.miss) Recalled(null) else Recalled(entry.toFacts())
    }

    suspend fun remember(key: String, facts: TrackFacts?) = lock.withLock {
        entries.remove(key)
        entries[key] = facts.toEntry()
        while (entries.size > MAX_ENTRIES) {
            val oldest = entries.keys.firstOrNull() ?: break
            entries.remove(oldest)
        }
        writeAll()
    }

    private fun MemoEntry.toFacts() = TrackFacts(
        title = title,
        artist = artist,
        album = album,
        releaseGroupId = releaseGroupId,
        coverUrl = coverUrl,
        confidence = confidence,
        albumQuality = runCatching { AlbumQuality.valueOf(quality) }.getOrDefault(AlbumQuality.NONE),
    )

    private fun TrackFacts?.toEntry() = MemoEntry(
        title = this?.title.orEmpty(),
        artist = this?.artist.orEmpty(),
        album = this?.album,
        releaseGroupId = this?.releaseGroupId,
        coverUrl = this?.coverUrl,
        confidence = this?.confidence ?: 0.0,
        quality = (this?.albumQuality ?: AlbumQuality.NONE).name,
        storedAt = System.currentTimeMillis(),
        miss = this == null,
    )

    private fun readAll(): LinkedHashMap<String, MemoEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return LinkedHashMap()
        val stored = runCatching { json.decodeFromString<Map<String, MemoEntry>>(raw) }
            .getOrDefault(emptyMap())
        return LinkedHashMap(stored)
    }

    private fun writeAll() {
        val encoded = runCatching { json.encodeToString<Map<String, MemoEntry>>(entries) }
            .getOrNull() ?: return
        prefs.edit().putString(KEY_ENTRIES, encoded).apply()
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val MAX_ENTRIES = 200
        val HIT_LIFETIME_MS = 30.days.inWholeMilliseconds
        val MISS_LIFETIME_MS = 2.days.inWholeMilliseconds
    }
}
