package net.spacealtctrl.discordrp.discord

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.spacealtctrl.discordrp.settings.Stash
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ProfileFetch {
    data object Loading : ProfileFetch
    data class Fresh(val profile: DiscordProfile) : ProfileFetch

    data class Stale(val profile: DiscordProfile?) : ProfileFetch
}

@Singleton
class ProfileRepository @Inject constructor(
    private val api: DiscordApi,
    private val stash: Stash,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun profile(): Flow<ProfileFetch> = flow {
        emit(ProfileFetch.Loading)
        api.me().fold(
            onSuccess = { fetched ->
                val enriched = fetched.copy(
                    bio = fetched.bio ?: stash.selfBio.takeIf { it.isNotBlank() },
                    nitro = stash.selfNitro,
                )
                stash.profileCache = json.encodeToString(enriched)
                emit(ProfileFetch.Fresh(enriched))
            },
            onFailure = { emit(ProfileFetch.Stale(cached())) },
        )
    }

    fun cached(): DiscordProfile? {
        val raw = stash.profileCache.takeIf { it.isNotBlank() } ?: return null
        return runCatching { json.decodeFromString<DiscordProfile>(raw) }.getOrNull()
    }
}
