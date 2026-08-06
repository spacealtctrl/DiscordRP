package net.spacealtctrl.discordrp.inbox

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.spacealtctrl.discordrp.discord.ApiRefusal
import net.spacealtctrl.discordrp.discord.DiscordApi
import net.spacealtctrl.discordrp.log.AppLog
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelDirectory @Inject constructor(
    private val api: DiscordApi,
    private val log: AppLog,
) {
    private val channelNames = ConcurrentHashMap<String, String>()
    private val guildNames = ConcurrentHashMap<String, String>()
    private val heldRoles = ConcurrentHashMap<String, Set<String>>()
    private val deadEnds: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val gates = ConcurrentHashMap<String, Mutex>()

    suspend fun conversationTitle(channelId: String, guildId: String?): String? {
        if (guildId == null) return null
        val channel = lookup("c:$channelId", channelNames, channelId) {
            api.channel(channelId).map { it.name }
        }
        val guild = lookup("g:$guildId", guildNames, guildId) {
            api.guild(guildId).map { it.name }
        }
        return when {
            channel != null && guild != null -> "#$channel - $guild"
            channel != null -> "#$channel"
            else -> guild
        }
    }

    suspend fun myRoles(guildId: String): Set<String> {
        heldRoles[guildId]?.let { return it }
        return gated("r:$guildId") {
            heldRoles[guildId] ?: fetchRoles(guildId)
        }
    }

    private suspend fun fetchRoles(guildId: String): Set<String> =
        api.myMembership(guildId).fold(
            onSuccess = { membership ->
                membership.roles.toSet().also { heldRoles[guildId] = it }
            },
            onFailure = {
                log.warn(TAG, "Role lookup failed for guild: ${it.message}")
                emptySet()
            },
        )

    private suspend fun lookup(
        gateKey: String,
        cache: ConcurrentHashMap<String, String>,
        id: String,
        fetch: suspend () -> Result<String?>,
    ): String? {
        cache[id]?.let { return it }
        if (gateKey in deadEnds) return null

        return gated(gateKey) {
            cache[id]?.let { return@gated it }
            if (gateKey in deadEnds) return@gated null

            fetch().fold(
                onSuccess = { name ->
                    if (name != null) cache[id] = name else deadEnds.add(gateKey)
                    name
                },
                onFailure = { failure ->
                    if ((failure as? ApiRefusal)?.permanent == true) {
                        deadEnds.add(gateKey)
                    } else {
                        log.warn(TAG, "Lookup failed, will retry later: ${failure.message}")
                    }
                    null
                },
            )
        }
    }

    private suspend fun <T> gated(key: String, block: suspend () -> T): T {
        val gate = gates.getOrPut(key) { Mutex() }
        return try {
            gate.withLock { block() }
        } finally {
            if (!gate.isLocked) gates.remove(key, gate)
        }
    }

    private companion object {
        const val TAG = "ChannelDirectory"
    }
}
