package dev.unlockmusic.android.core.decrypt.qmc

import dev.unlockmusic.android.core.codec.decodeLenientBase64
import kotlin.math.abs
import kotlin.math.tan

private const val SALT_LENGTH = 2
private const val ZERO_LENGTH = 7
private val MIX_KEY_1 =
    byteArrayOf(
        0x33,
        0x38,
        0x36,
        0x5A,
        0x4A,
        0x59,
        0x21,
        0x40,
        0x23,
        0x2A,
        0x24,
        0x25,
        0x5E,
        0x26,
        0x29,
        0x28,
    )
private val MIX_KEY_2 =
    byteArrayOf(
        0x2A,
        0x2A,
        0x23,
        0x21,
        0x28,
        0x23,
        0x24,
        0x25,
        0x26,
        0x5E,
        0x61,
        0x31,
        0x63,
        0x5A,
        0x2C,
        0x54,
    )
private val V2_PREFIX = "QQMusic EncV2,Key:".toByteArray(Charsets.UTF_8)

fun qmcDeriveKey(raw: ByteArray): ByteArray {
    var rawDecoded = decodeLenientBase64(raw)
    require(rawDecoded.size >= 16) { "key length is too short" }

    rawDecoded = decryptV2Key(rawDecoded)

    val simpleKey = simpleMakeKey(106, 8)
    val teaKey = ByteArray(TeaCipher.KEY_SIZE)
    repeat(8) { index ->
        teaKey[index shl 1] = simpleKey[index].toByte()
        teaKey[(index shl 1) + 1] = rawDecoded[index]
    }

    val subKey = decryptTencentTea(rawDecoded.copyOfRange(8, rawDecoded.size), teaKey)
    subKey.copyInto(rawDecoded, destinationOffset = 8)
    return rawDecoded.copyOf(8 + subKey.size)
}

fun simpleMakeKey(
    salt: Int,
    length: Int,
): IntArray {
    return IntArray(length) { index ->
        val value = tan(salt + index * 0.1)
        (abs(value) * 100.0).toInt() and 0xFF
    }
}

private fun decryptV2Key(key: ByteArray): ByteArray {
    if (key.size < V2_PREFIX.size || !key.copyOfRange(0, V2_PREFIX.size).contentEquals(V2_PREFIX)) {
        return key
    }

    var output = decryptTencentTea(key.copyOfRange(V2_PREFIX.size, key.size), MIX_KEY_1)
    output = decryptTencentTea(output, MIX_KEY_2)

    val keyDecoded = decodeLenientBase64(output)
    require(keyDecoded.size >= 16) { "EncV2 key decode failed" }
    return keyDecoded
}

private fun decryptTencentTea(
    input: ByteArray,
    key: ByteArray,
): ByteArray {
    require(input.size % TeaCipher.BLOCK_SIZE == 0) { "inBuf size not a multiple of the block size" }
    require(input.size >= 16) { "inBuf size too small" }

    val cipher = TeaCipher(key, 32)
    val tmpBuffer = ByteArray(TeaCipher.BLOCK_SIZE)
    cipher.decrypt(tmpBuffer, input, 0)

    val paddingLength = tmpBuffer[0].toInt() and 0x7
    val outputLength = input.size - 1 - paddingLength - SALT_LENGTH - ZERO_LENGTH
    val output = ByteArray(outputLength)

    var ivPrevious = ByteArray(TeaCipher.BLOCK_SIZE)
    var ivCurrent = input.copyOfRange(0, TeaCipher.BLOCK_SIZE)
    var inputPosition = TeaCipher.BLOCK_SIZE
    var tmpIndex = 1 + paddingLength

    fun cryptBlock() {
        ivPrevious = ivCurrent
        ivCurrent = input.copyOfRange(inputPosition, inputPosition + TeaCipher.BLOCK_SIZE)
        repeat(TeaCipher.BLOCK_SIZE) { index ->
            tmpBuffer[index] = (tmpBuffer[index].toInt() xor ivCurrent[index].toInt()).toByte()
        }
        cipher.decrypt(tmpBuffer, tmpBuffer)
        inputPosition += TeaCipher.BLOCK_SIZE
        tmpIndex = 0
    }

    var skippedSaltBytes = 1
    while (skippedSaltBytes <= SALT_LENGTH) {
        if (tmpIndex < TeaCipher.BLOCK_SIZE) {
            tmpIndex++
            skippedSaltBytes++
        } else {
            cryptBlock()
        }
    }

    var outputPosition = 0
    while (outputPosition < outputLength) {
        if (tmpIndex < TeaCipher.BLOCK_SIZE) {
            output[outputPosition] = (tmpBuffer[tmpIndex].toInt() xor ivPrevious[tmpIndex].toInt()).toByte()
            outputPosition++
            tmpIndex++
        } else {
            cryptBlock()
        }
    }

    repeat(ZERO_LENGTH) {
        if (tmpIndex == TeaCipher.BLOCK_SIZE) {
            cryptBlock()
        }
        if (tmpBuffer.getOrNull(tmpIndex) != ivPrevious.getOrNull(tmpIndex)) {
            error("zero check failed")
        }
        tmpIndex++
    }

    return output
}
