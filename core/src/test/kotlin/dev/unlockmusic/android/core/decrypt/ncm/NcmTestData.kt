package dev.unlockmusic.android.core.decrypt.ncm

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object NcmTestData {
    private val magicHeader = byteArrayOf(0x43, 0x54, 0x45, 0x4E, 0x46, 0x44, 0x41, 0x4D)
    private val coreKey = hexToBytes("687a4852416d736f356b496e62617857")
    private val metaKey = hexToBytes("2331346C6A6B5F215C5D2630553C2728")
    private val keyPrefix = "neteasecloudmusic".toByteArray(Charsets.UTF_8)
    private val metadataPrefix = "163 key(Don't modify):".toByteArray(Charsets.UTF_8)
    private val keyData = byteArrayOf(0x4E, 0x43, 0x4D, 0x21, 0x23, 0x45, 0x67, 0x01, 0x10, 0x20, 0x30, 0x40)

    fun encryptedFile(
        audioBytes: ByteArray,
        format: String? = null,
    ): ByteArray {
        val keySection = buildKeySection()
        val metadataSection = buildMetadataSection(format)
        val audioSection = encryptAudio(audioBytes)

        return magicHeader +
            byteArrayOf(0, 0) +
            littleEndianInt(keySection.size) +
            keySection +
            metadataSection +
            ByteArray(13) +
            audioSection
    }

    fun flacAudio(): ByteArray {
        return byteArrayOf(0x66, 0x4C, 0x61, 0x43, 0x00, 0x00, 0x00, 0x22) +
            "synthetic-flac-audio".toByteArray(Charsets.UTF_8)
    }

    fun mp3Audio(): ByteArray {
        return byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x21) +
            "synthetic-mp3-audio".toByteArray(Charsets.UTF_8)
    }

    private fun buildKeySection(): ByteArray {
        val encrypted = encryptAesEcb(keyPrefix + keyData, coreKey)
        return encrypted.map { value -> (value.toInt() xor 0x64).toByte() }.toByteArray()
    }

    private fun buildMetadataSection(format: String?): ByteArray {
        if (format == null) return littleEndianInt(0)

        val metadataText = """music:{"format":"$format","musicName":"Synthetic"}"""
        val encrypted = encryptAesEcb(metadataText.toByteArray(Charsets.UTF_8), metaKey)
        val payload = metadataPrefix + Base64.getEncoder().encode(encrypted)
        val obfuscatedPayload = payload.map { value -> (value.toInt() xor 0x63).toByte() }.toByteArray()
        return littleEndianInt(obfuscatedPayload.size) + obfuscatedPayload
    }

    private fun encryptAudio(audioBytes: ByteArray): ByteArray {
        val keyBox = buildKeyBox()
        return audioBytes.copyOf().also { encrypted ->
            encrypted.indices.forEach { index ->
                encrypted[index] = (encrypted[index].toInt() xor keyBox[index and 0xFF].toInt()).toByte()
            }
        }
    }

    private fun buildKeyBox(): ByteArray {
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

    private fun encryptAesEcb(
        plainText: ByteArray,
        key: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(plainText)
    }

    private fun littleEndianInt(value: Int): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value ushr 8).toByte(),
            (value ushr 16).toByte(),
            (value ushr 24).toByte(),
        )
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex string" }
        return ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}
