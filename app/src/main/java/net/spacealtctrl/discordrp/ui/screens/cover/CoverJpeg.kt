package net.spacealtctrl.discordrp.ui.screens.cover

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

internal object CoverJpeg {
    private const val QUALITY = 95

    fun base64(artwork: Bitmap): String {
        val source = if (artwork.config == Bitmap.Config.HARDWARE) {
            artwork.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            artwork
        }
        val bytes = ByteArrayOutputStream().use { out ->
            source.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
