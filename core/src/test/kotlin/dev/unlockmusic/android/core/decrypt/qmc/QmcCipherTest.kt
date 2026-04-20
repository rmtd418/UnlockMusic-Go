package dev.unlockmusic.android.core.decrypt.qmc

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class QmcCipherTest {
    @Test
    fun `static cipher matches high offset vector`() {
        val cipher = QmcStaticCipher()
        val buffer = ByteArray(16)

        cipher.decrypt(buffer, 0x7FF8)

        assertArrayEquals(
            byteArrayOf(
                0xD8.toByte(), 0x52, 0xF7.toByte(), 0x67,
                0x90.toByte(), 0xCA.toByte(), 0xD6.toByte(), 0x4A,
                0x4A, 0xD6.toByte(), 0xCA.toByte(), 0x90.toByte(),
                0x67, 0xF7.toByte(), 0x52, 0xD8.toByte(),
            ),
            buffer,
        )
    }

    @Test
    fun `static cipher matches low offset vector`() {
        val cipher = QmcStaticCipher()
        val buffer = ByteArray(16)

        cipher.decrypt(buffer, 0)

        assertArrayEquals(
            byteArrayOf(
                0xC3.toByte(), 0x4A, 0xD6.toByte(), 0xCA.toByte(),
                0x90.toByte(), 0x67, 0xF7.toByte(), 0x52,
                0xD8.toByte(), 0xA1.toByte(), 0x66, 0x62,
                0x9F.toByte(), 0x5B, 0x09, 0x00,
            ),
            buffer,
        )
    }

    @Test
    fun `map cipher mask matches reference vector`() {
        val key = ByteArray(256) { index -> index.toByte() }
        val buffer = ByteArray(16)

        QmcMapCipher(key).decrypt(buffer, 0)

        assertArrayEquals(
            byteArrayOf(
                0xBB.toByte(), 0x7D, 0x80.toByte(), 0xBE.toByte(),
                0xFF.toByte(), 0x38, 0x81.toByte(), 0xFB.toByte(),
                0xBB.toByte(), 0xFF.toByte(), 0x82.toByte(), 0x3C,
                0xFF.toByte(), 0xBA.toByte(), 0x83.toByte(), 0x79,
            ),
            buffer,
        )
    }

    @Test
    fun `map cipher decrypts full reference files`() {
        listOf("mflac_map", "mgg_map").forEach { name ->
            val key = QmcTestData.bytes("${name}_key.bin")
            val encrypted = QmcTestData.bytes("${name}_raw.bin")
            val expected = QmcTestData.bytes("${name}_target.bin")

            QmcMapCipher(key).decrypt(encrypted, 0)
            assertArrayEquals(expected, encrypted)
        }
    }

    @Test
    fun `rc4 cipher decrypts full reference files`() {
        listOf("mflac0_rc4", "mflac_rc4").forEach { name ->
            val key = QmcTestData.bytes("${name}_key.bin")
            val encrypted = QmcTestData.bytes("${name}_raw.bin")
            val expected = QmcTestData.bytes("${name}_target.bin")

            QmcRc4Cipher(key).decrypt(encrypted, 0)
            assertArrayEquals(expected, encrypted)
        }
    }

    @Test
    fun `rc4 cipher decrypts first segment`() {
        listOf("mflac0_rc4", "mflac_rc4").forEach { name ->
            val key = QmcTestData.bytes("${name}_key.bin")
            val encrypted = QmcTestData.bytes("${name}_raw.bin").copyOfRange(0, 128)
            val expected = QmcTestData.bytes("${name}_target.bin").copyOfRange(0, 128)

            QmcRc4Cipher(key).decrypt(encrypted, 0)
            assertArrayEquals(expected, encrypted)
        }
    }

    @Test
    fun `rc4 cipher decrypts aligned block`() {
        listOf("mflac0_rc4", "mflac_rc4").forEach { name ->
            val key = QmcTestData.bytes("${name}_key.bin")
            val encrypted = QmcTestData.bytes("${name}_raw.bin").copyOfRange(128, 5120)
            val expected = QmcTestData.bytes("${name}_target.bin").copyOfRange(128, 5120)

            QmcRc4Cipher(key).decrypt(encrypted, 128)
            assertArrayEquals(expected, encrypted)
        }
    }

    @Test
    fun `rc4 cipher decrypts standard segment`() {
        listOf("mflac0_rc4", "mflac_rc4").forEach { name ->
            val key = QmcTestData.bytes("${name}_key.bin")
            val encrypted = QmcTestData.bytes("${name}_raw.bin").copyOfRange(5120, 10240)
            val expected = QmcTestData.bytes("${name}_target.bin").copyOfRange(5120, 10240)

            QmcRc4Cipher(key).decrypt(encrypted, 5120)
            assertArrayEquals(expected, encrypted)
        }
    }
}

