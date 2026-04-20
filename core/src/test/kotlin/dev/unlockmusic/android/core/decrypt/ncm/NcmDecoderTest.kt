package dev.unlockmusic.android.core.decrypt.ncm

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NcmDecoderTest {
    @Test
    fun `decoder extracts format and audio bytes`() {
        val expectedAudio = NcmTestData.flacAudio()
        val encrypted = NcmTestData.encryptedFile(expectedAudio, format = "flac")

        val result = NcmDecoder(encrypted).decrypt()

        assertEquals("flac", result.format)
        assertArrayEquals(expectedAudio, result.audioBytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decoder rejects invalid header`() {
        NcmDecoder(byteArrayOf(0x00, 0x01, 0x02)).decrypt()
    }

    @Test
    fun `decoder handles missing metadata`() {
        val expectedAudio = NcmTestData.mp3Audio()
        val encrypted = NcmTestData.encryptedFile(expectedAudio, format = null)

        val result = NcmDecoder(encrypted).decrypt()

        assertNull(result.format)
        assertArrayEquals(expectedAudio, result.audioBytes)
    }
}
