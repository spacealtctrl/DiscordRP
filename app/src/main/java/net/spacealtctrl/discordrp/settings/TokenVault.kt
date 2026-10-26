package net.spacealtctrl.discordrp.settings

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Seals the Discord token with an Android Keystore key.
 *
 * The key is deliberately not user-authentication bound and not tied to the
 * unlocked-device state: the gateway service has to read the token while the
 * screen is off. `EncryptedSharedPreferences` would add a second wrapping layer
 * without changing that property, so the vault is the single, documented story
 * for token storage. The sealed blob lives in private preferences, which the
 * backup rules exclude.
 *
 * Plaintext is held only for the duration of one call. Byte buffers used to
 * move it through the cipher are zeroed afterwards; Kotlin strings themselves
 * cannot be wiped, so the callers keep the returned value short-lived.
 */
internal object TokenVault {
    /** Alias used by the signed-in app. Test stores derive their own alias. */
    const val ALIAS = "discordrp.token.v1"

    private const val KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256

    @Synchronized
    fun encrypt(plaintext: String, alias: String = ALIAS): String {
        val bytes = plaintext.toByteArray(Charsets.UTF_8)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key(alias))
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val body = Base64.encodeToString(cipher.doFinal(bytes), Base64.NO_WRAP)
            return "$iv:$body"
        } finally {
            bytes.fill(0)
        }
    }

    @Synchronized
    fun decrypt(sealed: String, alias: String = ALIAS): String? = try {
        val parts = sealed.split(':', limit = 2)
        if (parts.size != 2) {
            null
        } else {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(alias),
                GCMParameterSpec(TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP)),
            )
            val plain = cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
            try {
                String(plain, Charsets.UTF_8)
            } finally {
                plain.fill(0)
            }
        }
    } catch (_: Exception) {
        // A missing or invalidated key means the stored token is unusable; the
        // user has to sign in again rather than fall back to plaintext.
        null
    }

    /**
     * Drops the key. Called whenever the stored credential is discarded, so
     * even a sealed blob that escaped the app sandbox (backup, extracted file)
     * cannot be opened again.
     */
    @Synchronized
    fun forget(alias: String = ALIAS) {
        runCatching {
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(alias)
        }
    }

    private fun key(alias: String): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return generate(alias)
    }

    private fun generate(alias: String): SecretKey {
        // Hardware-backed keys when the device offers them; the fallback keeps
        // the app working where StrongBox is absent or already occupied.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { generate(alias, strongBox = true) }.getOrNull()?.let { return it }
        }
        return generate(alias, strongBox = false)
    }

    private fun generate(alias: String, strongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(builder.build())
        return generator.generateKey()
    }
}
