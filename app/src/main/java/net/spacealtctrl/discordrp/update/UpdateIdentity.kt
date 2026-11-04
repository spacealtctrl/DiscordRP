package net.spacealtctrl.discordrp.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.security.MessageDigest

internal object UpdateIdentity {
    fun verify(context: Context, apk: File) {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(apk.absolutePath, signingFlags)
        if (archive == null) {
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
        if (candidate.isEmpty() || installed.isEmpty() || candidate != installed) {
            error("update signature mismatch")
        }
    }

    private val signingFlags: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
