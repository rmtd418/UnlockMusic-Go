package dev.unlockmusic.android.core.decrypt.ncm

import dev.unlockmusic.android.core.codec.decodeLenientBase64
import dev.unlockmusic.android.core.decrypt.FileDecryptResult
import dev.unlockmusic.android.core.decrypt.FileDecryptor
import dev.unlockmusic.android.core.decrypt.HeaderCaptureOutputStream
import dev.unlockmusic.android.core.decrypt.STREAM_BUFFER_SIZE
import dev.unlockmusic.android.core.decrypt.resetForWrite
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.core.metadata.FilenameParser
import java.io.File
import java.io.RandomAccessFile
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class NcmFileDecryptor : FileDecryptor {
    override val supportedTypes: Set<DetectedFileType> = setOf(DetectedFileType.NCM)

    override fun decryptToFile(
        filename: String,
        inputFile: File,
        outputFile: File,
    ): FileDecryptResult {
        val parsed = FilenameParser.parse(filename)
        outputFile.resetForWrite()

        val formatAndExtension =
            RandomAccessFile(inputFile, "r").use { input ->
                val decodePlan = DecodePlan.from(input)
                outputFile.outputStream().use { rawOutput ->
                    val output = HeaderCaptureOutputStream(rawOutput)
                    streamDecrypt(input, decodePlan, output)
                    output.flush()
                    decodePlan.format to output.sniffExtension(decodePlan.format ?: DEFAULT_FALLBACK_EXTENSION)
                }
            }
        val outputExtension = formatAndExtension.second

        return FileDecryptResult(
            detectedFileType = DetectedFileType.NCM,
            outputExtension = outputExtension,
            outputFile = outputFile,
            suggestedFileName = "${parsed.baseName}.$outputExtension",
        )
    }

    private fun streamDecrypt(
        input: RandomAccessFile,
        decodePlan: DecodePlan,
        output: HeaderCaptureOutputStream,
    ) {
        input.seek(decodePlan.audioOffset)
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        var processed = 0L
        while (true) {
            val readLength = input.read(buffer)
            if (readLength < 0) break
            repeat(readLength) { index ->
                buffer[index] = (buffer[index].toInt() xor decodePlan.keyBox[(processed + index).toInt() and 0xFF].toInt()).toByte()
            }
            output.write(buffer, 0, readLength)
            processed += readLength
        }
    }

    private data class DecodePlan(
        val keyBox: ByteArray,
        val format: String?,
        val audioOffset: Long,
    ) {
        companion object {
            fun from(input: RandomAccessFile): DecodePlan {
                require(input.length() >= 10) { "NCM file is too short" }
                val magic = ByteArray(MAGIC_HEADER.size)
                input.seek(0)
                input.readFully(magic)
                require(magic.contentEquals(MAGIC_HEADER)) { "Invalid NCM header" }
                input.seek(10)

                val keyBox = buildKeyBox(readKeyData(input))
                val format = readFormat(input)
                skipImageSection(input)
                return DecodePlan(
                    keyBox = keyBox,
                    format = format,
                    audioOffset = input.filePointer,
                )
            }

            private fun readKeyData(input: RandomAccessFile): ByteArray {
                val keyLength = input.readUInt32LittleEndian()
                val cipherText = ByteArray(keyLength)
                input.readFully(cipherText)
                cipherText.indices.forEach { index ->
                    cipherText[index] = (cipherText[index].toInt() xor 0x64).toByte()
                }
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

            private fun readFormat(input: RandomAccessFile): String? {
                val metadataLength = input.readUInt32LittleEndian()
                if (metadataLength == 0) return null

                val cipherText = ByteArray(metadataLength)
                input.readFully(cipherText)
                cipherText.indices.forEach { index ->
                    cipherText[index] = (cipherText[index].toInt() xor 0x63).toByte()
                }
                require(cipherText.size > METADATA_PREFIX_LENGTH) { "Invalid NCM metadata" }

                val base64Payload = cipherText.copyOfRange(METADATA_PREFIX_LENGTH, cipherText.size)
                val metadataText = decryptAesEcb(decodeLenientBase64(base64Payload), META_KEY).toString(Charsets.UTF_8)
                return FORMAT_PATTERN.find(metadataText)?.groupValues?.get(1)?.lowercase()
            }

            private fun skipImageSection(input: RandomAccessFile) {
                val sectionOffset = input.filePointer
                input.seek(sectionOffset + IMAGE_LENGTH_OFFSET)
                val imageLength = input.readUInt32LittleEndian()
                input.seek(sectionOffset + IMAGE_SECTION_PREFIX_SIZE + imageLength)
            }

            private fun RandomAccessFile.readUInt32LittleEndian(): Int {
                val b0 = readUnsignedByte()
                val b1 = readUnsignedByte()
                val b2 = readUnsignedByte()
                val b3 = readUnsignedByte()
                return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
            }

            private fun decryptAesEcb(
                cipherText: ByteArray,
                key: ByteArray,
            ): ByteArray {
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
                return cipher.doFinal(cipherText)
            }

            private val MAGIC_HEADER = byteArrayOf(0x43, 0x54, 0x45, 0x4E, 0x46, 0x44, 0x41, 0x4D)
            private val CORE_KEY = hexToBytes("687a4852416d736f356b496e62617857")
            private val META_KEY = hexToBytes("2331346C6A6B5F215C5D2630553C2728")
            private const val KEY_PREFIX_LENGTH = 17
            private const val METADATA_PREFIX_LENGTH = 22
            private const val IMAGE_LENGTH_OFFSET = 5
            private const val IMAGE_SECTION_PREFIX_SIZE = 13
            private val FORMAT_PATTERN = Regex("\"format\"\\s*:\\s*\"([^\"]+)\"")

            private fun hexToBytes(hex: String): ByteArray {
                require(hex.length % 2 == 0) { "Invalid hex string" }
                return ByteArray(hex.length / 2) { index ->
                    hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_FALLBACK_EXTENSION = "mp3"
    }
}
