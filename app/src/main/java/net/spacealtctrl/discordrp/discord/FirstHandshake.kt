package net.spacealtctrl.discordrp.discord

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.spacealtctrl.discordrp.gateway.GatewayEngine
import net.spacealtctrl.discordrp.gateway.GatewayEvent
import net.spacealtctrl.discordrp.settings.Stash
import javax.inject.Inject

class FirstHandshake @Inject constructor(
    private val stash: Stash,
) {
    suspend fun run(token: String): Boolean {
        val probe = GatewayEngine(token = token, mobileBadge = false, autoRedial = false)
        return try {
            val outcome = coroutineScope {
                val firstAnswer = async {
                    withTimeoutOrNull(HANDSHAKE_TIMEOUT_MS) {
                        probe.events.first {
                            it is GatewayEvent.SessionReady || it is GatewayEvent.AuthRejected
                        }
                    }
                }
                probe.start()
                firstAnswer.await()
            }
            val user = (outcome as? GatewayEvent.SessionReady)?.user ?: return false
            val id = user.id ?: return false

            stash.authToken = token
            stash.selfId = id
            stash.selfBio = user.bio.orEmpty()
            stash.selfNitro = user.premiumType in 1..3
            true
        } finally {
            probe.shutdown()
        }
    }

    private companion object {
        const val HANDSHAKE_TIMEOUT_MS = 20_000L
    }
}
