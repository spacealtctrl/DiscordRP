package net.spacealtctrl.discordrp.gateway

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface GatewayEvent {
    data class SessionReady(val user: SessionUser?) : GatewayEvent

    data class MessageArrived(val message: ChatMessage) : GatewayEvent

    data class ChannelSeen(val channelId: String) : GatewayEvent

    data object AuthRejected : GatewayEvent
}

interface Gateway {
    val events: SharedFlow<GatewayEvent>

    val linked: StateFlow<Boolean>

    fun start()

    suspend fun redial()

    suspend fun publish(update: PresenceUpdate)

    fun shutdown()
}
