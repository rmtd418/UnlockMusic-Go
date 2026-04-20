package dev.unlockmusic.android.core.decrypt.qmc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class QmcKeyTest {
    @Test
    fun `simple key matches known values`() {
        assertArrayEquals(intArrayOf(0x69, 0x56, 0x46, 0x38, 0x2B, 0x20, 0x15, 0x0B), simpleMakeKey(106, 8))
    }

    @Test
    fun `derive key matches reference vectors`() {
        listOf("mflac_map", "mgg_map", "mflac0_rc4", "mflac_rc4").forEach { name ->
            val rawKey = QmcTestData.bytes("${name}_key_raw.bin")
            val expected = QmcTestData.bytes("${name}_key.bin")

            val actual = qmcDeriveKey(rawKey)
            assertEquals(expected.size, actual.size)
            assertArrayEquals(expected, actual)
        }
    }

    @Test
    fun `derive key accepts noisy base64 payload`() {
        val rawKey = QmcTestData.bytes("mflac_rc4_key_raw.bin")
        val expected = QmcTestData.bytes("mflac_rc4_key.bin")
        val noisyRawKey =
            ByteArray(rawKey.size * 3) { index ->
                when (index % 3) {
                    0 -> rawKey[index / 3]
                    1 -> 0
                    else -> 0xFF.toByte()
                }
            }

        val actual = qmcDeriveKey(noisyRawKey)
        assertEquals(expected.size, actual.size)
        assertArrayEquals(expected, actual)
    }
}
