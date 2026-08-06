package net.spacealtctrl.discordrp.inbox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.scale
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Collections
import kotlin.coroutines.resume

internal class FaceFetcher(private val context: Context) {
    private val loader = ImageLoader(context)

    private val cache: MutableMap<String, IconCompat> =
        Collections.synchronizedMap(object : LinkedHashMap<String, IconCompat>() {
            override fun removeEldestEntry(eldest: Map.Entry<String, IconCompat>) = size > CACHE_CAP
        })

    suspend fun face(url: String?): IconCompat? {
        if (url.isNullOrBlank()) return null
        cache[url]?.let { return it }
        val bitmap = download(url) ?: return null
        return IconCompat.createWithAdaptiveBitmap(bitmap).also { cache[url] = it }
    }

    private suspend fun download(url: String): Bitmap? = suspendCancellableCoroutine { cont ->
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .target(
                onSuccess = { drawable ->
                    val bitmap = when (drawable) {
                        is BitmapDrawable -> drawable.bitmap
                        else -> Bitmap.createBitmap(
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        ).also { canvasTarget ->
                            val canvas = Canvas(canvasTarget)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                        }
                    }
                    if (cont.isActive) cont.resume(shrink(bitmap))
                },
                onError = { if (cont.isActive) cont.resume(null) },
            )
            .build()

        val disposable = loader.enqueue(request)
        cont.invokeOnCancellation { disposable.dispose() }
    }

    private fun shrink(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= EDGE_CAP && bitmap.height <= EDGE_CAP) return bitmap
        val factor = EDGE_CAP.toFloat() / maxOf(bitmap.width, bitmap.height)
        return bitmap.scale(
            (bitmap.width * factor).toInt().coerceAtLeast(1),
            (bitmap.height * factor).toInt().coerceAtLeast(1),
        )
    }

    private companion object {
        const val EDGE_CAP = 256
        const val CACHE_CAP = 40
    }
}
