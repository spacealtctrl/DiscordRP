package net.spacealtctrl.discordrp.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.spacealtctrl.discordrp.BuildConfig
import net.spacealtctrl.discordrp.log.AppLog
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdate(val version: String, val apkUrl: String)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
    data class Downloading(val update: AppUpdate) : UpdateState
    data object Failed : UpdateState
}

@Singleton
class UpdateHub @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: HttpClient,
    private val log: AppLog,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _outcomes = MutableSharedFlow<UpdateState>(extraBufferCapacity = 4)
    val outcomes: SharedFlow<UpdateState> = _outcomes.asSharedFlow()

    @Volatile private var checkedAt = 0L

    fun autoCheck() {
        if (System.currentTimeMillis() - checkedAt < AUTO_CHECK_COOLDOWN_MS) return
        check()
    }

    fun check() {
        val busy = _state.value
        if (busy is UpdateState.Checking || busy is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        checkedAt = System.currentTimeMillis()
        scope.launch {
            val outcome = fetchLatest()
            _state.value = outcome
            _outcomes.tryEmit(outcome)
        }
    }

    fun install(update: AppUpdate) {
        if (_state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Downloading(update)
        scope.launch {
            try {
                val apk = download(update)
                launchInstaller(apk)
                _state.value = UpdateState.Available(update)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error(TAG, "Update download failed: ${e::class.java.simpleName}")
                _state.value = UpdateState.Failed
                _outcomes.tryEmit(UpdateState.Failed)
            }
        }
    }

    private suspend fun fetchLatest(): UpdateState = try {
        val release: FeedRelease = http.get(FEED_URL) {
            header(HttpHeaders.Accept, "application/vnd.github+json")
        }.body()
        val version = release.tagName.removePrefix("v")
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
        when {
            apk == null -> UpdateState.UpToDate
            VersionOrder.isNewer(version, BuildConfig.VERSION_NAME) ->
                UpdateState.Available(AppUpdate(version, apk.downloadUrl))
            else -> UpdateState.UpToDate
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn(TAG, "Update check failed: ${e::class.java.simpleName}")
        UpdateState.Failed
    }

    private suspend fun download(update: AppUpdate): File {
        val dir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
        val file = File(dir, "DiscordRP-${update.version}.apk")
        http.prepareGet(update.apkUrl) {
            timeout { requestTimeoutMillis = DOWNLOAD_TIMEOUT_MS }
        }.execute { response ->
            if (response.status.value !in 200..299) {
                error("download answered ${response.status.value}")
            }
            file.outputStream().use { out ->
                val channel = response.bodyAsChannel()
                val buffer = ByteArray(DOWNLOAD_CHUNK_BYTES)
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    if (read > 0) out.write(buffer, 0, read)
                }
            }
        }
        return file
    }

    private fun launchInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    @Serializable
    private data class FeedAsset(
        val name: String,
        @SerialName("browser_download_url") val downloadUrl: String,
    )

    @Serializable
    private data class FeedRelease(
        @SerialName("tag_name") val tagName: String,
        val assets: List<FeedAsset> = emptyList(),
    )

    private companion object {
        const val TAG = "UpdateHub"

        const val FEED_URL =
            "https://api.github.com/repos/spacealtctrl/DiscordRP/releases/latest"

        const val UPDATES_DIR = "updates"
        const val AUTO_CHECK_COOLDOWN_MS = 6 * 60 * 60 * 1000L
        const val DOWNLOAD_TIMEOUT_MS = 10 * 60 * 1000L
        const val DOWNLOAD_CHUNK_BYTES = 64 * 1024
    }
}
