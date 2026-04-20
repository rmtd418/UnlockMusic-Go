package dev.unlockmusic.android.core.decrypt.qmc

class TeaCipher(
    key: ByteArray,
    private val rounds: Int = NUM_ROUNDS,
) {
    private val k0: Int
    private val k1: Int
    private val k2: Int
    private val k3: Int

    init {
        require(key.size == KEY_SIZE) { "incorrect key size" }
        require(rounds % 2 == 0) { "odd number of rounds specified" }
        k0 = readInt32BigEndian(key, 0)
        k1 = readInt32BigEndian(key, 4)
        k2 = readInt32BigEndian(key, 8)
        k3 = readInt32BigEndian(key, 12)
    }

    fun encrypt(
        dst: ByteArray,
        src: ByteArray,
        srcOffset: Int = 0,
        dstOffset: Int = 0,
    ) {
        var v0 = readInt32BigEndian(src, srcOffset)
        var v1 = readInt32BigEndian(src, srcOffset + 4)
        var sum = 0

        repeat(rounds / 2) {
            sum += DELTA
            v0 += ((v1 shl 4) + k0) xor (v1 + sum) xor ((v1 ushr 5) + k1)
            v1 += ((v0 shl 4) + k2) xor (v0 + sum) xor ((v0 ushr 5) + k3)
        }

        writeInt32BigEndian(dst, dstOffset, v0)
        writeInt32BigEndian(dst, dstOffset + 4, v1)
    }

    fun decrypt(
        dst: ByteArray,
        src: ByteArray,
        srcOffset: Int = 0,
        dstOffset: Int = 0,
    ) {
        var v0 = readInt32BigEndian(src, srcOffset)
        var v1 = readInt32BigEndian(src, srcOffset + 4)
        var sum = (DELTA * rounds) / 2

        repeat(rounds / 2) {
            v1 -= ((v0 shl 4) + k2) xor (v0 + sum) xor ((v0 ushr 5) + k3)
            v0 -= ((v1 shl 4) + k0) xor (v1 + sum) xor ((v1 ushr 5) + k1)
            sum -= DELTA
        }

        writeInt32BigEndian(dst, dstOffset, v0)
        writeInt32BigEndian(dst, dstOffset + 4, v1)
    }

    companion object {
        const val BLOCK_SIZE = 8
        const val KEY_SIZE = 16
        const val NUM_ROUNDS = 64
        private const val DELTA = 0x9E3779B9.toInt()

        private fun readInt32BigEndian(
            source: ByteArray,
            offset: Int,
        ): Int {
            return ((source[offset].toInt() and 0xFF) shl 24) or
                ((source[offset + 1].toInt() and 0xFF) shl 16) or
                ((source[offset + 2].toInt() and 0xFF) shl 8) or
                (source[offset + 3].toInt() and 0xFF)
        }

        private fun writeInt32BigEndian(
            destination: ByteArray,
            offset: Int,
            value: Int,
        ) {
            destination[offset] = (value ushr 24).toByte()
            destination[offset + 1] = (value ushr 16).toByte()
            destination[offset + 2] = (value ushr 8).toByte()
            destination[offset + 3] = value.toByte()
        }
    }
}
