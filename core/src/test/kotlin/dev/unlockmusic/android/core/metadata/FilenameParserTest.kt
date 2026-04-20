package dev.unlockmusic.android.core.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FilenameParserTest {
    @Test
    fun `parse returns base name and lowercase extension`() {
        val parsed = FilenameParser.parse("track.QMCFLAC")

        assertEquals("track", parsed.baseName)
        assertEquals("qmcflac", parsed.extension)
    }

    @Test
    fun `detect file type maps known qmc suffix`() {
        assertSame(DetectedFileType.QMC, detectFileType("test.mflac0"))
    }
}

