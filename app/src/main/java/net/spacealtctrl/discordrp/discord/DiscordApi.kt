package net.spacealtctrl.discordrp.discord

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.spacealtctrl.discordrp.BuildConfig
import net.spacealtctrl.discordrp.settings.Stash
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ChannelSummary(
    val id: String? = null,
    val name: String? = null,
    @SerialName("guild_id") val guildId: String? = null,
)

@Serializable
data class GuildSummary(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class GuildMembership(
    val roles: List<String> = emptyList(),
)

@Serializable
data class HostedAsset(
    @SerialName("external_asset_path") val path: String,
    val url: String,
)

@Singleton
class DiscordApi @Inject constructor(
    private val http: HttpClient,
    private val stash: Stash,
) {
    private val base = BuildConfig.DISCORD_API_URL

    suspend fun me(): Result<DiscordProfile> = call {
        http.get { url("$base/users/@me"); sign() }.expect()
    }

    suspend fun channel(channelId: String): Result<ChannelSummary> = call {
        http.get { url("$base/channels/$channelId"); sign() }.expect()
    }

    suspend fun guild(guildId: String): Result<GuildSummary> = call {
        http.get { url("$base/guilds/$guildId"); sign() }.expect()
    }

    suspend fun myMembership(guildId: String): Result<GuildMembership> = call {
        http.get { url("$base/users/@me/guilds/$guildId/member"); sign() }.expect()
    }

    suspend fun hostedAssetFor(imageUrl: String): Result<String> = call {
        val response = http.post {
            url("$base/applications/${BuildConfig.DISCORD_APP_ID}/external-assets")
            sign()
            contentType(ContentType.Application.Json)
            setBody(mapOf("urls" to listOf(imageUrl)))
        }
        response.raiseForStatus()
        val assets: List<HostedAsset> = response.body()
        "mp:" + assets.first().path
    }

    private fun io.ktor.client.request.HttpRequestBuilder.sign() {
        header(HttpHeaders.Authorization, stash.authToken)
    }

    private suspend inline fun <reified T> HttpResponse.expect(): T {
        raiseForStatus()
        return body()
    }

    private fun HttpResponse.raiseForStatus() {
        if (status.value !in 200..299) throw ApiRefusal(status.value)
    }

    private inline fun <T> call(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

}

class ApiRefusal(val status: Int) : Exception("Discord API answered $status") {
    val permanent: Boolean get() = status == 401 || status == 403 || status == 404
}
