package net.spacealtctrl.discordrp.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    private val digest = "a".repeat(64)

    @Test
    fun acceptsOnlyUpstreamReleaseAssets() {
        assertTrue(
            UpdatePolicy.isTrustedApkUrl(
                "https://github.com/spacealtctrl/DiscordRP/releases/download/v1.6.0/app.apk",
            ),
        )
        assertFalse(
            UpdatePolicy.isTrustedApkUrl(
                "https://github.com/cutieway/DiscordRP/releases/download/v1.6.0/app.apk",
            ),
        )
        assertFalse(
            UpdatePolicy.isTrustedApkUrl(
                "http://github.com/spacealtctrl/DiscordRP/releases/download/v1.6.0/app.apk",
            ),
        )
        assertFalse(
            UpdatePolicy.isTrustedApkUrl(
                "https://github.com.evil.test/spacealtctrl/DiscordRP/releases/download/v1.6.0/app.apk",
            ),
        )
        assertFalse(
            UpdatePolicy.isTrustedApkUrl(
                "https://user@github.com/spacealtctrl/DiscordRP/releases/download/v1.6.0/app.apk",
            ),
        )
        assertFalse(
            UpdatePolicy.isTrustedApkUrl(
                "https://github.com:8443/spacealtctrl/DiscordRP/releases/download/v1.6.0/app.apk",
            ),
        )
    }

    @Test
    fun requiresACompleteSha256Digest() {
        assertEquals(digest, UpdatePolicy.sha256("sha256:$digest"))
        assertEquals(digest, UpdatePolicy.sha256("sha256:${digest.uppercase()}"))
        assertNull(UpdatePolicy.sha256(null))
        assertNull(UpdatePolicy.sha256(""))
        assertNull(UpdatePolicy.sha256("sha256:${"a".repeat(63)}"))
        assertNull(UpdatePolicy.sha256("sha256:${"a".repeat(65)}"))
        assertNull(UpdatePolicy.sha256("md5:$digest"))
    }

    @Test
    fun acceptsOnlyGitHubRedirectTargets() {
        val asset = "https://github.com/spacealtctrl/DiscordRP/releases/download/v1.6.0/app.apk"
        assertTrue(UpdatePolicy.isTrustedDownloadUrl(asset, asset))
        assertTrue(
            UpdatePolicy.isTrustedDownloadUrl(
                asset,
                "https://objects.githubusercontent.com/github-production-release-asset-2e65be/1?X-Amz-Signature=abc",
            ),
        )
        assertTrue(
            UpdatePolicy.isTrustedDownloadUrl(
                asset,
                "https://release-assets.githubusercontent.com/github-production-release-asset/2?X-Amz-Signature=def",
            ),
        )

        assertFalse(UpdatePolicy.isTrustedDownloadUrl(asset, "https://evil.test/app.apk"))
        assertFalse(
            UpdatePolicy.isTrustedDownloadUrl(
                asset,
                "https://objects.githubusercontent.com.evil.test/app.apk",
            ),
        )
        assertFalse(
            UpdatePolicy.isTrustedDownloadUrl(
                asset,
                "http://objects.githubusercontent.com/app.apk",
            ),
        )
        assertFalse(
            UpdatePolicy.isTrustedDownloadUrl(
                asset,
                "https://user@objects.githubusercontent.com/app.apk",
            ),
        )
        assertFalse(
            UpdatePolicy.isTrustedDownloadUrl(
                asset,
                "https://objects.githubusercontent.com:8443/app.apk",
            ),
        )
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(asset, "https://github.com/spacealtctrl"))
        assertFalse(UpdatePolicy.isTrustedDownloadUrl("https://evil.test/app.apk", asset))
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(asset, null))
    }
}
