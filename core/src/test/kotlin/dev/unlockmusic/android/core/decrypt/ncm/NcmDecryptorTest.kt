package dev.unlockmusic.android.core.decrypt.ncm

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class NcmDecryptorTest {
    @Test
    fun `decryptor prefers sniffed extension over metadata fallback`() {
        val expectedAudio = NcmTestData.flacAudio()
        val encrypted = NcmTestData.encryptedFile(expectedAudio, format = "mp3")

        val result =
            runBlocking {
                NcmDecryptor().decrypt("sample.ncm", encrypted)
            }

        assertEquals("flac", result.outputExtension)
        assertEquals("sample.flac", result.suggestedFileName)
        assertArrayEquals(expectedAudio, result.outputBytes)
    }

    @Test
    fun `decryptor uses default fallback when metadata is absent`() {
        val expectedAudio = NcmTestData.mp3Audio()
        val encrypted = NcmTestData.encryptedFile(expectedAudio, format = null)

        val result =
            runBlocking {
                NcmDecryptor().decrypt("demo.ncm", encrypted)
            }

        assertEquals("mp3", result.outputExtension)
        assertEquals("demo.mp3", result.suggestedFileName)
        assertArrayEquals(expectedAudio, result.outputBytes)
    }
}
