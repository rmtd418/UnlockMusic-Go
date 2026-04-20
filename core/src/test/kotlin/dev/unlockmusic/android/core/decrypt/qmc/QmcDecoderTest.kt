package dev.unlockmusic.android.core.decrypt.qmc

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class QmcDecoderTest {
    @Test
    fun `decoder decrypts reference qmc files`() {
        listOf("mflac0_rc4", "mflac_rc4", "mflac_map", "mgg_map", "qmc0_static").forEach { name ->
            val encrypted = loadEncryptedSample(name)
            val expected = QmcTestData.bytes("${name}_target.bin")

            val actual = QmcDecoder(encrypted).decrypt()
            assertArrayEquals(expected, actual)
        }
    }

    private fun loadEncryptedSample(name: String): ByteArray {
        val body = QmcTestData.bytes("${name}_raw.bin")
        val suffix = QmcTestData.bytes("${name}_suffix.bin")
        return body + suffix
    }
}
