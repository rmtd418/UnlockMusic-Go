package dev.unlockmusic.android.core.codec

import java.util.Base64

private val PADDING = '='.code.toByte()
private val PLUS = '+'.code.toByte()
private val SLASH = '/'.code.toByte()
private val DASH = '-'.code.toByte()
private val UNDERSCORE = '_'.code.toByte()

fun decodeLenientBase64(raw: ByteArray): ByteArray {
    val filtered = ByteArray(raw.size + 3)
    var count = 0

    raw.forEach { value ->
        when {
            value.isBase64Alphabet() || value == PADDING -> {
                filtered[count++] = value
            }
            value == DASH -> {
                filtered[count++] = PLUS
            }
            value == UNDERSCORE -> {
                filtered[count++] = SLASH
            }
        }
    }

    require(count > 0) { "Base64 payload is empty" }

    while (count % 4 != 0) {
        filtered[count++] = PADDING
    }

    return Base64.getDecoder().decode(filtered.copyOf(count))
}

private fun Byte.isBase64Alphabet(): Boolean {
    val value = toInt() and 0xFF
    return value in 'A'.code..'Z'.code ||
        value in 'a'.code..'z'.code ||
        value in '0'.code..'9'.code ||
        value == PLUS.toInt() ||
        value == SLASH.toInt()
}
