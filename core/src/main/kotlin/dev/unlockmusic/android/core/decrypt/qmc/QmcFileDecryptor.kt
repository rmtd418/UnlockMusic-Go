package dev.unlockmusic.android.core.decrypt.qmc

import dev.unlockmusic.android.core.decrypt.FileDecryptResult
import dev.unlockmusic.android.core.decrypt.FileDecryptor
import dev.unlockmusic.android.core.decrypt.HeaderCaptureOutputStream
import dev.unlockmusic.android.core.decrypt.STREAM_BUFFER_SIZE
import dev.unlockmusic.android.core.decrypt.resetForWrite
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.core.metadata.FilenameParser
import java.io.File
import java.io.RandomAccessFile

class QmcFileDecryptor : FileDecryptor {
    override val supportedTypes: Set<DetectedFileType> = setOf(DetectedFileType.QMC)

    override fun decryptToFile(
        filename: String,
        inputFile: File,
        outputFile: File,
    ): FileDecryptResult {
        val parsed = FilenameParser.parse(filename)
        val fallbackExtension = requireNotNull(HANDLER_MAP[parsed.extension]) {
            "QMC cannot handle type: ${parsed.extension}"
        }
        outputFile.resetForWrite()

        val outputExtension =
            RandomAccessFile(inputFile, "r").use { input ->
                val decodePlan = DecodePlan.from(input)
                outputFile.outputStream().use { rawOutput ->
                    val output = HeaderCaptureOutputStream(rawOutput)
                    streamDecrypt(input, decodePlan, output)
                    output.flush()
                    output.sniffExtension(fallbackExtension)
                }
            }

        return FileDecryptResult(
            detectedFileType = DetectedFileType.QMC,
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
        input.seek(0)
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        var processed = 0L
        var remaining = decodePlan.audioSize

        while (remaining > 0) {
            val readLength = minOf(buffer.size.toLong(), remaining).toInt()
            input.readFully(buffer, 0, readLength)
            val chunk = if (readLength == buffer.size) buffer else buffer.copyOf(readLength)
            decodePlan.cipher.decrypt(chunk, processed.toInt())
            output.write(chunk, 0, readLength)
            processed += readLength
            remaining -= readLength
        }
    }

    private data class DecodePlan(
        val audioSize: Long,
        val cipher: QmcStreamCipher,
    ) {
        companion object {
            fun from(input: RandomAccessFile): DecodePlan {
                val size = input.length()
                require(size >= 4) { "QMC file is too short" }

                val last4Bytes = ByteArray(4)
                input.seek(size - 4)
                input.readFully(last4Bytes)

                return when (String(last4Bytes, Charsets.UTF_8)) {
                    "STag" -> error("QMC file does not contain an embedded key")
                    "QTag" -> parseQTag(input, size)
                    else -> parseLegacyOrStatic(input, size, last4Bytes)
                }
            }

            private fun parseQTag(
                input: RandomAccessFile,
                size: Long,
            ): DecodePlan {
                input.seek(size - 8)
                val keySize = input.readUInt32BigEndian()
                val audioSize = size - keySize - 8
                require(audioSize > 0 && keySize > 0 && keySize <= Int.MAX_VALUE) { "invalid audio size" }

                val rawKey = ByteArray(keySize.toInt())
                input.seek(audioSize)
                input.readFully(rawKey)

                val keyEnd = rawKey.indexOf(COMMA_BYTE)
                require(keyEnd >= 0) { "invalid key: search raw key failed" }
                return DecodePlan(
                    audioSize = audioSize,
                    cipher = cipherFor(rawKey.copyOfRange(0, keyEnd)),
                )
            }

            private fun parseLegacyOrStatic(
                input: RandomAccessFile,
                size: Long,
                last4Bytes: ByteArray,
            ): DecodePlan {
                val keySize = readUInt32LittleEndian(last4Bytes)
                if (keySize < 0x400L) {
                    val audioSize = size - keySize - 4
                    require(audioSize > 0 && keySize > 0 && keySize <= Int.MAX_VALUE) { "invalid audio size" }

                    val rawKey = ByteArray(keySize.toInt())
                    input.seek(audioSize)
                    input.readFully(rawKey)
                    return DecodePlan(
                        audioSize = audioSize,
                        cipher = cipherFor(rawKey),
                    )
                }

                return DecodePlan(
                    audioSize = size,
                    cipher = QmcStaticCipher(),
                )
            }

            private fun cipherFor(rawKey: ByteArray): QmcStreamCipher {
                val key = qmcDeriveKey(rawKey)
                return if (key.size > 300) QmcRc4Cipher(key) else QmcMapCipher(key)
            }

            private fun RandomAccessFile.readUInt32BigEndian(): Long {
                val b0 = readUnsignedByte().toLong()
                val b1 = readUnsignedByte().toLong()
                val b2 = readUnsignedByte().toLong()
                val b3 = readUnsignedByte().toLong()
                return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
            }

            private fun readUInt32LittleEndian(source: ByteArray): Long {
                return (source[0].toLong() and 0xFF) or
                    ((source[1].toLong() and 0xFF) shl 8) or
                    ((source[2].toLong() and 0xFF) shl 16) or
                    ((source[3].toLong() and 0xFF) shl 24)
            }

            private val COMMA_BYTE = ','.code.toByte()
        }
    }

    private companion object {
        val HANDLER_MAP =
            mapOf(
                "mgg" to "ogg",
                "mgg0" to "ogg",
                "mgg1" to "ogg",
                "mggl" to "ogg",
                "mflac" to "flac",
                "mflac0" to "flac",
                "mflach" to "flac",
                "mmp4" to "mmp4",
                "qmcflac" to "flac",
                "qmcogg" to "ogg",
                "qmc0" to "mp3",
                "qmc2" to "ogg",
                "qmc3" to "mp3",
                "qmc4" to "ogg",
                "qmc6" to "ogg",
                "qmc8" to "ogg",
                "bkcmp3" to "mp3",
                "bkcm4a" to "m4a",
                "bkcflac" to "flac",
                "bkcwav" to "wav",
                "bkcape" to "ape",
                "bkcogg" to "ogg",
                "bkcwma" to "wma",
                "tkm" to "m4a",
                "666c6163" to "flac",
                "6d7033" to "mp3",
                "6f6767" to "ogg",
                "6d3461" to "m4a",
                "776176" to "wav",
            )
    }
}
