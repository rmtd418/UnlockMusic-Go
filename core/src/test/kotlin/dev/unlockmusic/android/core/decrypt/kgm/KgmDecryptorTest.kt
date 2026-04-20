package dev.unlockmusic.android.core.decrypt.kgm

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class KgmDecryptorTest {
    @Test
    fun `decryptor emits sniffed flac file for kgm`() {
        val expected = KgmTestData.flacAudio()
        val encrypted = KgmTestData.kgmFile(expected)

        val result =
            runBlocking {
                KgmDecryptor().decrypt("sample.kgm", encrypted)
            }

        assertEquals("flac", result.outputExtension)
        assertEquals("sample.flac", result.suggestedFileName)
        assertArrayEquals(expected, result.outputBytes)
    }

    @Test
    fun `decryptor emits sniffed mp3 file for vpr`() {
        val expected = KgmTestData.mp3Audio()
        val encrypted = KgmTestData.vprFile(expected)

        val result =
            runBlocking {
                KgmDecryptor().decrypt("voice.vpr", encrypted)
            }

        assertEquals("mp3", result.outputExtension)
        assertEquals("voice.mp3", result.suggestedFileName)
        assertArrayEquals(expected, result.outputBytes)
    }
}
