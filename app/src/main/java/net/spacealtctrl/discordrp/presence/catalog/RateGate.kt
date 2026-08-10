package net.spacealtctrl.discordrp.presence.catalog

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateGate @Inject constructor() {
    private class Gate {
        val mutex = Mutex()

        @Volatile
        var lastAt = 0L
    }

    private val gates = ConcurrentHashMap<String, Gate>()

    suspend fun await(host: String, minIntervalMs: Long) {
        val gate = gates.computeIfAbsent(host) { Gate() }
        gate.mutex.withLock {
            val now = System.nanoTime()
            val sinceMs = (now - gate.lastAt) / 1_000_000
            if (gate.lastAt != 0L && sinceMs in 0 until minIntervalMs) {
                delay(minIntervalMs - sinceMs)
            }
            gate.lastAt = System.nanoTime()
        }
    }

    companion object {
        const val MUSICBRAINZ = "musicbrainz"
        const val DEEZER = "deezer"

        const val MUSICBRAINZ_INTERVAL_MS = 1_100L
        const val DEEZER_INTERVAL_MS = 250L
    }
}
