package dev.unlockmusic.android.core.decrypt.qmc

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TeaCipherTest {
    @Test(expected = IllegalArgumentException::class)
    fun `rejects invalid key size`() {
        TeaCipher(ByteArray(15))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects odd rounds`() {
        TeaCipher(ByteArray(TeaCipher.KEY_SIZE), TeaCipher.NUM_ROUNDS - 1)
    }

    @Test
    fun `encrypt and decrypt match known vectors`() {
        teaTests.forEach { vector ->
            val cipher = TeaCipher(vector.key, vector.rounds)
            val buffer = ByteArray(TeaCipher.BLOCK_SIZE)

            cipher.encrypt(buffer, vector.plainText)
            assertArrayEquals(vector.cipherText, buffer)

            cipher.decrypt(buffer, vector.cipherText)
            assertArrayEquals(vector.plainText, buffer)
        }
    }

    private data class TeaVector(
        val rounds: Int,
        val key: ByteArray,
        val plainText: ByteArray,
        val cipherText: ByteArray,
    )

    private companion object {
        val teaTests =
            listOf(
                TeaVector(
                    rounds = TeaCipher.NUM_ROUNDS,
                    key = ByteArray(TeaCipher.KEY_SIZE),
                    plainText = ByteArray(TeaCipher.BLOCK_SIZE),
                    cipherText = byteArrayOf(0x41, 0xEA.toByte(), 0x3A, 0x0A, 0x94.toByte(), 0xBA.toByte(), 0xA9.toByte(), 0x40),
                ),
                TeaVector(
                    rounds = TeaCipher.NUM_ROUNDS,
                    key = ByteArray(TeaCipher.KEY_SIZE) { 0xFF.toByte() },
                    plainText = ByteArray(TeaCipher.BLOCK_SIZE) { 0xFF.toByte() },
                    cipherText = byteArrayOf(0x31, 0x9B.toByte(), 0xBE.toByte(), 0xFB.toByte(), 0x01, 0x6A, 0xBD.toByte(), 0xB2.toByte()),
                ),
                TeaVector(
                    rounds = 16,
                    key = ByteArray(TeaCipher.KEY_SIZE),
                    plainText = ByteArray(TeaCipher.BLOCK_SIZE),
                    cipherText = byteArrayOf(0xED.toByte(), 0x28, 0x5D, 0xA1.toByte(), 0x45, 0x5B, 0x33, 0xC1.toByte()),
                ),
            )
    }
}

