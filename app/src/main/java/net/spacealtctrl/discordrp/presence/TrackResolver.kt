package net.spacealtctrl.discordrp.presence

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withTimeoutOrNull
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.presence.catalog.BrainzCatalog
import net.spacealtctrl.discordrp.presence.catalog.DeezerCatalog
import net.spacealtctrl.discordrp.presence.catalog.LookupAsk
import net.spacealtctrl.discordrp.presence.tidy.AlbumQuality
import net.spacealtctrl.discordrp.presence.tidy.JunkRules
import net.spacealtctrl.discordrp.presence.tidy.ScrubbedTrack
import net.spacealtctrl.discordrp.presence.tidy.SourceKind
import net.spacealtctrl.discordrp.presence.tidy.SourceTrust
import net.spacealtctrl.discordrp.presence.tidy.TextCanon
import net.spacealtctrl.discordrp.presence.tidy.TitleScrub
import net.spacealtctrl.discordrp.presence.tidy.TrackFacts
import net.spacealtctrl.discordrp.settings.Stash
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackResolver @Inject constructor(
    private val brainz: BrainzCatalog,
    private val deezer: DeezerCatalog,
    private val memo: TrackMemo,
    private val stash: Stash,
    private val log: AppLog,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<TrackFacts?>>()

    private val _resolved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val resolved: SharedFlow<Unit> = _resolved.asSharedFlow()

    suspend fun repair(track: NowPlaying): NowPlaying {
        if (!stash.tidyMetadata) return track
        if (!needsRepair(track)) return track

        val scrubbed = TitleScrub.scrub(
            title = track.title,
            artist = track.artist,
            album = track.album,
            appLabel = track.appLabel,
        )
        val floor = track.withScrub(scrubbed)
        if (!scrubbed.worthLookup) {
            log.debug(TAG, "Not a single song; keeping the scrubbed fields only")
            return floor
        }

        val ask = LookupAsk(
            title = scrubbed.title,
            artist = scrubbed.artist,
            album = scrubbed.album,
            durationMs = track.durationMillis,
        )
        val facts = factsFor(cacheKey(track), ask) ?: return floor
        return floor.withFacts(facts)
    }

    fun abandonLookups() {
        scope.coroutineContext.cancelChildren()
        inFlight.clear()
    }

    private fun needsRepair(track: NowPlaying): Boolean {
        if (SourceKind.trustOf(track.appPackage) == SourceTrust.VIDEO) return true
        return JunkRules.VIDEO_TITLE_MARKER.containsMatchIn(track.title)
    }

    private suspend fun factsFor(key: String, ask: LookupAsk): TrackFacts? {
        memo.recall(key)?.let { return it.facts }

        val lookup = lookupFor(key, ask)
        val answered = withTimeoutOrNull(LOOKUP_WAIT_MS) { lookup.await() }
        if (answered != null) return answered
        if (lookup.isCompleted) return null

        log.debug(TAG, "Lookup still running; showing the scrubbed fields for now")
        lookup.invokeOnCompletion { failure ->
            if (failure == null && lookup.getCompleted() != null) _resolved.tryEmit(Unit)
        }
        return null
    }

    private fun lookupFor(key: String, ask: LookupAsk): Deferred<TrackFacts?> {
        inFlight[key]?.let { return it }
        val fresh = scope.async(start = CoroutineStart.LAZY) {
            val found = try {
                runLookup(ask)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                log.warn(TAG, "Lookup failed: ${failure::class.java.simpleName}")
                null
            }
            found.also { memo.remember(key, it) }
        }
        val running = inFlight.putIfAbsent(key, fresh)
        if (running != null) {
            fresh.cancel()
            return running
        }
        fresh.invokeOnCompletion { inFlight.remove(key, fresh) }
        fresh.start()
        return fresh
    }

    private suspend fun runLookup(ask: LookupAsk): TrackFacts? {
        val open = brainz.lookup(ask)
        if (open != null && open.albumQuality == AlbumQuality.CLEAN) return open
        if (!stash.deezerFallback) return open

        val commercial = deezer.lookup(ask)
        val chosen = when {
            commercial == null -> open
            open == null -> commercial
            open.album == null -> commercial
            open.albumQuality == AlbumQuality.MESSY &&
                commercial.albumQuality == AlbumQuality.CLEAN -> commercial
            else -> open
        }
        if (chosen != null) {
            log.info(TAG, "Resolved \"${chosen.title}\" by ${chosen.artist}")
        }
        return chosen
    }

    private fun cacheKey(track: NowPlaying): String = listOf(
        track.appPackage,
        TextCanon.fold(track.title),
        TextCanon.fold(track.artist),
        TextCanon.fold(track.album),
    ).joinToString("|")

    private fun NowPlaying.withScrub(scrubbed: ScrubbedTrack) = copy(
        title = scrubbed.title,
        artist = scrubbed.artist,
        album = scrubbed.album,
        albumArtist = scrubbed.artist,
    )

    private fun NowPlaying.withFacts(facts: TrackFacts) = copy(
        title = facts.title,
        artist = facts.artist,
        album = facts.album ?: album,
        albumArtist = facts.artist,
        releaseGroupId = facts.releaseGroupId,
        coverUrl = facts.coverUrl,
    )

    private companion object {
        const val TAG = "TrackResolver"

        const val LOOKUP_WAIT_MS = 5_000L
    }
}
