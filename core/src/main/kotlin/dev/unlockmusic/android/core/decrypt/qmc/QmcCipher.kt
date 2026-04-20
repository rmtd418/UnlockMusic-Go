package dev.unlockmusic.android.core.decrypt.qmc

import kotlin.math.floor

interface QmcStreamCipher {
    fun decrypt(
        buffer: ByteArray,
        offset: Int,
    )
}

class QmcStaticCipher : QmcStreamCipher {
    override fun decrypt(
        buffer: ByteArray,
        offset: Int,
    ) {
        buffer.indices.forEach { index ->
            val mask = getMask(offset + index)
            buffer[index] = (buffer[index].toInt() xor mask).toByte()
        }
    }

    private fun getMask(offset: Int): Int {
        val normalizedOffset = if (offset > 0x7FFF) offset % 0x7FFF else offset
        return STATIC_CIPHER_BOX[(normalizedOffset * normalizedOffset + 27) and 0xFF]
    }

    private companion object {
        val STATIC_CIPHER_BOX =
            intArrayOf(
                0x77, 0x48, 0x32, 0x73, 0xDE, 0xF2, 0xC0, 0xC8,
                0x95, 0xEC, 0x30, 0xB2, 0x51, 0xC3, 0xE1, 0xA0,
                0x9E, 0xE6, 0x9D, 0xCF, 0xFA, 0x7F, 0x14, 0xD1,
                0xCE, 0xB8, 0xDC, 0xC3, 0x4A, 0x67, 0x93, 0xD6,
                0x28, 0xC2, 0x91, 0x70, 0xCA, 0x8D, 0xA2, 0xA4,
                0xF0, 0x08, 0x61, 0x90, 0x7E, 0x6F, 0xA2, 0xE0,
                0xEB, 0xAE, 0x3E, 0xB6, 0x67, 0xC7, 0x92, 0xF4,
                0x91, 0xB5, 0xF6, 0x6C, 0x5E, 0x84, 0x40, 0xF7,
                0xF3, 0x1B, 0x02, 0x7F, 0xD5, 0xAB, 0x41, 0x89,
                0x28, 0xF4, 0x25, 0xCC, 0x52, 0x11, 0xAD, 0x43,
                0x68, 0xA6, 0x41, 0x8B, 0x84, 0xB5, 0xFF, 0x2C,
                0x92, 0x4A, 0x26, 0xD8, 0x47, 0x6A, 0x7C, 0x95,
                0x61, 0xCC, 0xE6, 0xCB, 0xBB, 0x3F, 0x47, 0x58,
                0x89, 0x75, 0xC3, 0x75, 0xA1, 0xD9, 0xAF, 0xCC,
                0x08, 0x73, 0x17, 0xDC, 0xAA, 0x9A, 0xA2, 0x16,
                0x41, 0xD8, 0xA2, 0x06, 0xC6, 0x8B, 0xFC, 0x66,
                0x34, 0x9F, 0xCF, 0x18, 0x23, 0xA0, 0x0A, 0x74,
                0xE7, 0x2B, 0x27, 0x70, 0x92, 0xE9, 0xAF, 0x37,
                0xE6, 0x8C, 0xA7, 0xBC, 0x62, 0x65, 0x9C, 0xC2,
                0x08, 0xC9, 0x88, 0xB3, 0xF3, 0x43, 0xAC, 0x74,
                0x2C, 0x0F, 0xD4, 0xAF, 0xA1, 0xC3, 0x01, 0x64,
                0x95, 0x4E, 0x48, 0x9F, 0xF4, 0x35, 0x78, 0x95,
                0x7A, 0x39, 0xD6, 0x6A, 0xA0, 0x6D, 0x40, 0xE8,
                0x4F, 0xA8, 0xEF, 0x11, 0x1D, 0xF3, 0x1B, 0x3F,
                0x3F, 0x07, 0xDD, 0x6F, 0x5B, 0x19, 0x30, 0x19,
                0xFB, 0xEF, 0x0E, 0x37, 0xF0, 0x0E, 0xCD, 0x16,
                0x49, 0xFE, 0x53, 0x47, 0x13, 0x1A, 0xBD, 0xA4,
                0xF1, 0x40, 0x19, 0x60, 0x0E, 0xED, 0x68, 0x09,
                0x06, 0x5F, 0x4D, 0xCF, 0x3D, 0x1A, 0xFE, 0x20,
                0x77, 0xE4, 0xD9, 0xDA, 0xF9, 0xA4, 0x2B, 0x76,
                0x1C, 0x71, 0xDB, 0x00, 0xBC, 0xFD, 0x0C, 0x6C,
                0xA5, 0x47, 0xF7, 0xF6, 0x00, 0x79, 0x4A, 0x11,
            )
    }
}

class QmcMapCipher(
    private val key: ByteArray,
) : QmcStreamCipher {
    private val size = key.size

    init {
        require(key.isNotEmpty()) { "qmc/cipher_map: invalid key size" }
    }

    override fun decrypt(
        buffer: ByteArray,
        offset: Int,
    ) {
        buffer.indices.forEach { index ->
            val mask = getMask(offset + index)
            buffer[index] = (buffer[index].toInt() xor mask).toByte()
        }
    }

    private fun getMask(offset: Int): Int {
        val normalizedOffset = if (offset > 0x7FFF) offset % 0x7FFF else offset
        val index = (normalizedOffset * normalizedOffset + 71214) % size
        return rotate(unsignedByte(key[index]), index and 0x7)
    }

    private fun rotate(
        value: Int,
        bits: Int,
    ): Int {
        val rotation = (bits + 4) % 8
        val left = value shl rotation
        val right = value ushr rotation
        return (left or right) and 0xFF
    }
}

class QmcRc4Cipher(
    private val key: ByteArray,
) : QmcStreamCipher {
    private val size = key.size
    private val seedBox = IntArray(size) { index -> index and 0xFF }
    private val hashBase: Long

    init {
        require(key.isNotEmpty()) { "invalid key size" }

        var j = 0
        repeat(size) { index ->
            j = (seedBox[index] + j + unsignedByte(key[index % size])) % size
            val temp = seedBox[index]
            seedBox[index] = seedBox[j]
            seedBox[j] = temp
        }

        var hash = 1L
        for (index in 0 until size) {
            val value = unsignedByte(key[index]).toLong()
            if (value == 0L) continue

            val nextHash = (hash * value) and 0xFFFF_FFFFL
            if (nextHash == 0L || nextHash <= hash) break
            hash = nextHash
        }
        hashBase = hash
    }

    override fun decrypt(
        buffer: ByteArray,
        offset: Int,
    ) {
        var remaining = buffer.size
        var processed = 0
        var currentOffset = offset

        fun postProcess(length: Int): Boolean {
            remaining -= length
            processed += length
            currentOffset += length
            return remaining == 0
        }

        if (currentOffset < FIRST_SEGMENT_SIZE) {
            val segmentLength = minOf(buffer.size, FIRST_SEGMENT_SIZE - currentOffset)
            encryptFirstSegment(buffer, 0, segmentLength, currentOffset)
            if (postProcess(segmentLength)) return
        }

        if (currentOffset % SEGMENT_SIZE != 0) {
            val segmentLength = minOf(SEGMENT_SIZE - (currentOffset % SEGMENT_SIZE), remaining)
            encryptSegment(buffer, processed, segmentLength, currentOffset)
            if (postProcess(segmentLength)) return
        }

        while (remaining > SEGMENT_SIZE) {
            encryptSegment(buffer, processed, SEGMENT_SIZE, currentOffset)
            postProcess(SEGMENT_SIZE)
        }

        if (remaining > 0) {
            encryptSegment(buffer, processed, remaining, currentOffset)
        }
    }

    private fun encryptFirstSegment(
        buffer: ByteArray,
        start: Int,
        length: Int,
        offset: Int,
    ) {
        repeat(length) { index ->
            val mask = unsignedByte(key[getSegmentKey(offset + index)])
            buffer[start + index] = (buffer[start + index].toInt() xor mask).toByte()
        }
    }

    private fun encryptSegment(
        buffer: ByteArray,
        start: Int,
        length: Int,
        offset: Int,
    ) {
        val seed = seedBox.copyOf()
        val skipLength = (offset % SEGMENT_SIZE) + getSegmentKey(offset / SEGMENT_SIZE)

        var j = 0
        var k = 0
        var bufferIndex = start

        for (streamIndex in -skipLength until length) {
            j = (j + 1) % size
            k = (seed[j] + k) % size

            val temp = seed[k]
            seed[k] = seed[j]
            seed[j] = temp

            if (streamIndex >= 0) {
                val mask = seed[(seed[j] + seed[k]) % size]
                buffer[bufferIndex] = (buffer[bufferIndex].toInt() xor mask).toByte()
                bufferIndex++
            }
        }
    }

    private fun getSegmentKey(id: Int): Int {
        val seed = unsignedByte(key[id % size]).toDouble()
        if (seed == 0.0) return 0

        val denominator = (id + 1).toDouble() * seed
        val index = floor((hashBase.toDouble() / denominator) * 100.0)
        if (!index.isFinite()) return 0
        return (index % size.toDouble()).toInt()
    }

    private companion object {
        private const val FIRST_SEGMENT_SIZE = 0x80
        private const val SEGMENT_SIZE = 5120
    }
}

private fun unsignedByte(value: Byte): Int = value.toInt() and 0xFF
