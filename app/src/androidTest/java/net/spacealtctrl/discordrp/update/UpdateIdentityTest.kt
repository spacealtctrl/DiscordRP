package net.spacealtctrl.discordrp.update

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UpdateIdentityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext get() = InstrumentationRegistry.getInstrumentation().context

    @After
    fun removeFixtures() {
        File(context.cacheDir, FIXTURE_DIR).deleteRecursively()
    }

    @Test
    fun acceptsTheInstalledApk() {
        UpdateIdentity.verify(context, File(context.applicationInfo.sourceDir))
    }

    @Test
    fun rejectsAnApkForAnotherPackage() {
        expectRejected(
            File(testContext.applicationInfo.sourceDir),
            "package id",
        )
    }

    /**
     * The critical negative: the archive carries our package id but was signed
     * by someone else. Rotation is deliberately not trusted here either; since
     * no APK signing lineage is verified, a rotated key is indistinguishable
     * from a foreign one and must fail closed.
     */
    @Test
    fun rejectsTheSamePackageSignedByAnotherOrRotatedKey() {
        val apk = asset("update-foreign-signer.apk")
        assertEquals(
            context.packageName,
            context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)?.packageName,
        )
        expectRejected(apk, "signature")
    }

    @Test
    fun rejectsTheSamePackageWithoutAnySigner() {
        expectRejected(asset("update-unsigned.apk"), "signature")
    }

    @Test
    fun rejectsTheSamePackageWithExtraSigners() {
        expectRejected(asset("update-multi-signer.apk"), "signature")
    }

    private fun expectRejected(apk: File, messagePart: String) {
        try {
            UpdateIdentity.verify(context, apk)
            fail("${apk.name} must not pass update verification")
        } catch (e: IllegalStateException) {
            assertTrue("unexpected reason: ${e.message}", e.message.orEmpty().contains(messagePart))
        }
    }

    private fun asset(name: String): File {
        val dir = File(context.cacheDir, FIXTURE_DIR).apply { mkdirs() }
        val file = File(dir, name)
        testContext.assets.open(name).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    private companion object {
        const val FIXTURE_DIR = "identity-fixtures"
    }
}
