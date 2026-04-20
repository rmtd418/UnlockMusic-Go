package dev.unlockmusic.android.core.decrypt.kgm

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class KgmDecoderTest {
    @Test
    fun `decoder decrypts kgm bytes`() {
        val expected = KgmTestData.flacAudio()
        val encrypted = KgmTestData.kgmFile(expected)

        val result = KgmDecoder(encrypted, "kgm").decrypt()

        assertArrayEquals(expected, result)
    }

    @Test
    fun `decoder decrypts vpr bytes`() {
        val expected = KgmTestData.mp3Audio()
        val encrypted = KgmTestData.vprFile(expected)

        val result = KgmDecoder(encrypted, "vpr").decrypt()

        assertArrayEquals(expected, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decoder rejects invalid header`() {
        KgmDecoder(byteArrayOf(0x00, 0x01, 0x02), "kgm").decrypt()
    }
}
