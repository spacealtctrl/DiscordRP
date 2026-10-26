package net.spacealtctrl.discordrp.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.security.MessageDigest

/**
 * An update must be the same app, signed by the same key that installed the
 * running build. Android enforces that at install time; checking it before
 * handing the file to the installer keeps a foreign or re-signed package from
 * ever being presented as an update, and turns the failure into a clear one.
 */
internal object UpdateIdentity {
    fun verify(context: Context, apk: File) {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(apk.absolutePath, signingFlags)
        if (archive == null) {
            // Some devices refuse to parse an archive at all when signatures are
            // requested and the APK has none. Confirm the package if the file is
            // otherwise readable, then fail on identity.
            val readable = pm.getPackageArchiveInfo(apk.absolutePath, 0)
                ?: error("unreadable update package")
            if (readable.packageName != context.packageName) error("update package id mismatch")
            error("update signature mismatch")
        }
        if (archive.packageName != context.packageName) {
            error("update package id mismatch")
        }

        val candidate = signerDigests(archive)
        val installed = signerDigests(pm.getPackageInfo(context.packageName, signingFlags))
        // Signing-key rotation is intentionally not accepted. Verifying a
        // lineage would let a rotated key pass, but without checking the APK's
        // lineage against the installed certificate history a rotated key looks
        // exactly like a foreign one, so identity failures stay fail-closed.
        if (candidate.isEmpty() || installed.isEmpty() || candidate != installed) {
            error("update signature mismatch")
        }
    }

    private val signingFlags: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Some devices leave signingInfo unset when parsing an archive, so
            // ask for the legacy field as well and fall back to it below.
            PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signersP(info)
        } else {
            legacySignatures(info)
        }.orEmpty()
        if (signers.isEmpty()) return emptySet()
        return signers.mapTo(mutableSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun signersP(info: PackageInfo) = info.signingInfo?.apkContentsSigners

    @Suppress("DEPRECATION")
    private fun legacySignatures(info: PackageInfo) = info.signatures
}
