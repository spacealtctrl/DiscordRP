package net.spacealtctrl.discordrp.di

import android.app.NotificationManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import net.spacealtctrl.discordrp.BuildConfig
import net.spacealtctrl.discordrp.log.AndroidAppLog
import net.spacealtctrl.discordrp.log.AppLog
import net.spacealtctrl.discordrp.settings.Stash
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppProviders {
    @Provides
    @Singleton
    fun stash(@ApplicationContext context: Context): Stash = Stash.of(context)

    @Provides
    fun appLog(): AppLog = AndroidAppLog

    @Provides
    fun notificationManager(@ApplicationContext context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun httpClient(json: Json, log: AppLog): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.HEADERS else LogLevel.NONE
            sanitizeHeader { it == HttpHeaders.Authorization }
            logger = object : Logger {
                override fun log(message: String) = log.debug("Http", message)
            }
        }
    }
}
