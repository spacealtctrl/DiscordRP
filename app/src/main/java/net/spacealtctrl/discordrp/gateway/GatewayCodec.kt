package net.spacealtctrl.discordrp.gateway

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal object GatewayCodec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    data class Inbound(
        val op: Int?,
        val seq: Int?,
        val event: String?,
        val data: JsonElement?,
    )

    fun parse(text: String): Inbound? = runCatching {
        val envelope = json.decodeFromString<Envelope>(text)
        Inbound(op = envelope.op, seq = envelope.s, event = envelope.t, data = envelope.d)
    }.getOrNull()

    inline fun <reified T> read(frame: Inbound): T? {
        val data = frame.data ?: return null
        return runCatching { json.decodeFromJsonElement<T>(data) }.getOrNull()
    }

    inline fun <reified T> encode(value: T): JsonElement = json.encodeToJsonElement(value)

    fun sequenceValue(seq: Int): JsonElement =
        if (seq > 0) JsonPrimitive(seq) else JsonNull

    fun outbound(op: Int, data: JsonElement): String =
        json.encodeToString(
            JsonElement.serializer(),
            buildJsonObject {
                put("op", JsonPrimitive(op))
                put("d", data)
            },
        )
}
