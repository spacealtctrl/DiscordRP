package net.spacealtctrl.discordrp.ui.screens.cover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverWebPolicyTest {
    private val release = "545f7071-1fee-485d-8e0a-9f036708c3f2"

    @Test
    fun `the upload page lives on MusicBrainz`() {
        assertEquals(
            "https://musicbrainz.org/release/$release/add-cover-art",
            CoverWebPolicy.addCoverUrl(release),
        )
    }

    @Test
    fun `only MusicBrainz and MetaBrainz pages may load`() {
        assertTrue(CoverWebPolicy.isAllowed("https://musicbrainz.org/login?returnto=%2Frelease%2Fx"))
        assertTrue(CoverWebPolicy.isAllowed("https://musicbrainz.org/release/$release/add-cover-art"))
        assertTrue(CoverWebPolicy.isAllowed("https://metabrainz.org/privacy"))
        assertTrue(CoverWebPolicy.isAllowed("https://accounts.metabrainz.org/login"))

        assertFalse(CoverWebPolicy.isAllowed("http://musicbrainz.org/login"))
        assertFalse(CoverWebPolicy.isAllowed("https://musicbrainz.org.evil.test/login"))
        assertFalse(CoverWebPolicy.isAllowed("https://evilmusicbrainz.org/login"))
        assertFalse(CoverWebPolicy.isAllowed("https://user@musicbrainz.org/login"))
        assertFalse(CoverWebPolicy.isAllowed("https://musicbrainz.org:8443/login"))
        assertFalse(CoverWebPolicy.isAllowed("https://archive.org/details/x"))
        assertFalse(CoverWebPolicy.isAllowed("javascript:alert(1)"))
        assertFalse(CoverWebPolicy.isAllowed(null))
    }

    @Test
    fun `the upload form and the finished page are told apart`() {
        assertTrue(CoverWebPolicy.isUploadForm("https://musicbrainz.org/release/$release/add-cover-art", release))
        assertTrue(CoverWebPolicy.isUploadForm("https://musicbrainz.org/release/$release/add-cover-art/", release))
        assertFalse(CoverWebPolicy.isUploadForm("https://musicbrainz.org/release/$release/cover-art", release))

        assertTrue(CoverWebPolicy.isUploaded("https://musicbrainz.org/release/$release/cover-art", release))
        assertFalse(CoverWebPolicy.isUploaded("https://musicbrainz.org/release/$release/add-cover-art", release))
        assertFalse(CoverWebPolicy.isUploaded("https://musicbrainz.org/release/other/cover-art", release))
        assertFalse(CoverWebPolicy.isUploaded("https://evil.test/release/$release/cover-art", release))
    }
}
