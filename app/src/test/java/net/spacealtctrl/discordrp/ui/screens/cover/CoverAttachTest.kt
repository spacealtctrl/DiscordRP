package net.spacealtctrl.discordrp.ui.screens.cover

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverAttachTest {
    private val jpeg = "/9j/4AAQSkZJRgABAQAAAQABAAD="

    @Test
    fun `the script fills MusicBrainz's own file box and ticks Front`() {
        val script = CoverAttach.script(jpeg)
        assertNotNull(script)
        script!!
        assertTrue(script.contains("input.add-files"))
        assertTrue(script.contains("'$jpeg'"))
        assertTrue(script.contains("image/jpeg"))
        assertTrue(script.contains("input.type[value=\"1\"]"))
        assertTrue(script.contains("add-cover-art-submit"))
    }

    @Test
    fun `the script never presses Enter edit itself`() {
        val script = CoverAttach.script(jpeg)!!
        assertTrue(!script.contains("submit()") && !script.contains("submit').click") && !script.contains("submit\").click"))
    }

    @Test
    fun `anything that is not base64 is refused`() {
        assertNull(CoverAttach.script(""))
        assertNull(CoverAttach.script("abc'); alert(1); ('"))
        assertNull(CoverAttach.script("abc\ndef"))
    }
}
