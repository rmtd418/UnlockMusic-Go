package dev.unlockmusic.android.core.decrypt.ncm

import dev.unlockmusic.android.core.codec.decodeLenientBase64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class NcmDecodedAudio(
    val audioBytes: ByteArray,
    val format: String?,
)

class NcmDecoder(
    private val file: ByteArray,
) {
    private var offset: Int = 10

    init {
        require(file.size >= 10) { "NCM file is too short" }
        require(file.copyOfRange(0, MAGIC_HEADER.size).contentEquals(MAGIC_HEADER)) {
            "Invalid NCM header"
        }
    }

    fun decrypt(): NcmDecodedAudio {
        val keyBox = buildKeyBox(readKeyData())
        val format = readFormat()
        skipImageSection()
        val audioBytes = readAudioBytes(keyBox)

        return NcmDecodedAudio(
            audioBytes = audioBytes,
            format = format,
        )
    }

    private fun readKeyData(): ByteArray {
        val keyLength = readUInt32LittleEndian()
        val cipherText = readBytes(keyLength).map { value -> (value.toInt() xor 0x64).toByte() }.toByteArray()
        val plainText = decryptAesEcb(cipherText, CORE_KEY)

        require(plainText.size > KEY_PREFIX_LENGTH) { "Invalid NCM key data" }
        return plainText.copyOfRange(KEY_PREFIX_LENGTH, plainText.size)
    }

    private fun buildKeyBox(keyData: ByteArray): ByteArray {
        require(keyData.isNotEmpty()) { "Invalid NCM key box" }

        val box = ByteArray(256) { index -> index.toByte() }
        var j = 0
        for (i in box.indices) {
            j = (box[i].toInt().and(0xFF) + j + keyData[i % keyData.size].toInt().and(0xFF)) and 0xFF
            val temp = box[i]
            box[i] = box[j]
            box[j] = temp
        }

        return ByteArray(256) { index ->
            val i = (index + 1) and 0xFF
            val si = box[i].toInt().and(0xFF)
            val sj = box[(i + si) and 0xFF].toInt().and(0xFF)
            box[(si + sj) and 0xFF]
        }
    }

    private fun readFormat(): String? {
        val metadataLength = readUInt32LittleEndian()
        if (metadataLength == 0) return null

        val cipherText = readBytes(metadataLength).map { value -> (value.toInt() xor 0x63).toByte() }.toByteArray()
        require(cipherText.size > METADATA_PREFIX_LENGTH) { "Invalid NCM metadata" }

        val base64Payload =
            cipherText
                .copyOfRange(METADATA_PREFIX_LENGTH, cipherText.size)
        val metadataText = decryptAesEcb(decodeLenientBase64(base64Payload), META_KEY).toString(Charsets.UTF_8)

        return FORMAT_PATTERN.find(metadataText)?.groupValues?.get(1)?.lowercase()
    }

    private fun skipImageSection() {
        val imageLength = readUInt32LittleEndian(offset + IMAGE_LENGTH_OFFSET)
        skipBytes(IMAGE_SECTION_PREFIX_SIZE + imageLength)
    }

    private fun readAudioBytes(keyBox: ByteArray): ByteArray {
        val audioBytes = file.copyOfRange(offset, file.size)
        audioBytes.indices.forEach { index ->
            audioBytes[index] = (audioBytes[index].toInt() xor keyBox[index and 0xFF].toInt()).toByte()
        }
        return audioBytes
    }

    private fun readUInt32LittleEndian(absoluteOffset: Int = offset): Int {
        require(absoluteOffset + 4 <= file.size) { "Unexpected end of NCM file" }
        val value =
            (file[absoluteOffset].toInt().and(0xFF)) or
                (file[absoluteOffset + 1].toInt().and(0xFF) shl 8) or
                (file[absoluteOffset + 2].toInt().and(0xFF) shl 16) or
                (file[absoluteOffset + 3].toInt().and(0xFF) shl 24)
        if (absoluteOffset == offset) {
            offset += 4
        }
        return value
    }

    private fun readBytes(length: Int): ByteArray {
        require(length >= 0) { "Invalid NCM section length" }
        require(offset + length <= file.size) { "Unexpected end of NCM file" }

        val result = file.copyOfRange(offset, offset + length)
        offset += length
        return result
    }

    private fun skipBytes(length: Int) {
        require(length >= 0) { "Invalid NCM skip length" }
        require(offset + length <= file.size) { "Unexpected end of NCM file" }
        offset += length
    }

    private fun decryptAesEcb(
        cipherText: ByteArray,
        key: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(cipherText)
    }

    private companion object {
        val MAGIC_HEADER = byteArrayOf(0x43, 0x54, 0x45, 0x4E, 0x46, 0x44, 0x41, 0x4D)
        val CORE_KEY = hexToBytes("687a4852416d736f356b496e62617857")
        val META_KEY = hexToBytes("2331346C6A6B5F215C5D2630553C2728")
        const val KEY_PREFIX_LENGTH = 17
        const val METADATA_PREFIX_LENGTH = 22
        const val IMAGE_LENGTH_OFFSET = 5
        const val IMAGE_SECTION_PREFIX_SIZE = 13
        val FORMAT_PATTERN = Regex("\"format\"\\s*:\\s*\"([^\"]+)\"")

        fun hexToBytes(hex: String): ByteArray {
            require(hex.length % 2 == 0) { "Invalid hex string" }
            return ByteArray(hex.length / 2) { index ->
                hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }
    }
}
