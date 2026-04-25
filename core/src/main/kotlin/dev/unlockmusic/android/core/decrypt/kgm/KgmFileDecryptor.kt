package dev.unlockmusic.android.core.decrypt.kgm

import dev.unlockmusic.android.core.decrypt.FileDecryptResult
import dev.unlockmusic.android.core.decrypt.FileDecryptor
import dev.unlockmusic.android.core.decrypt.HeaderCaptureOutputStream
import dev.unlockmusic.android.core.decrypt.STREAM_BUFFER_SIZE
import dev.unlockmusic.android.core.decrypt.resetForWrite
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.core.metadata.FilenameParser
import java.io.File
import java.io.RandomAccessFile

class KgmFileDecryptor : FileDecryptor {
    override val supportedTypes: Set<DetectedFileType> = setOf(DetectedFileType.KGM)

    override fun decryptToFile(
        filename: String,
        inputFile: File,
        outputFile: File,
    ): FileDecryptResult {
        val parsed = FilenameParser.parse(filename)
        outputFile.resetForWrite()

        val outputExtension =
            RandomAccessFile(inputFile, "r").use { input ->
                val decodePlan = DecodePlan.from(input, parsed.extension)
                outputFile.outputStream().use { rawOutput ->
                    val output = HeaderCaptureOutputStream(rawOutput)
                    streamDecrypt(input, decodePlan, output)
                    output.flush()
                    output.sniffExtension(DEFAULT_FALLBACK_EXTENSION)
                }
            }

        return FileDecryptResult(
            detectedFileType = DetectedFileType.KGM,
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
                buffer[index] = decryptByte(buffer[index], decodePlan.key, (processed + index).toInt(), decodePlan.mode)
            }
            output.write(buffer, 0, readLength)
            processed += readLength
        }
    }

    private fun decryptByte(
        value: Byte,
        key: ByteArray,
        offset: Int,
        mode: KgmMode,
    ): Byte {
        var medium = key[offset % key.size].toInt().and(0xFF) xor value.toInt().and(0xFF)
        medium = medium xor ((medium and 0x0F) shl 4)

        var mask = getMask(offset)
        mask = mask xor ((mask and 0x0F) shl 4)

        var result = medium xor mask
        if (mode == KgmMode.Vpr) {
            result = result xor KgmDecoder.VPR_MASK_DIFF[offset % KgmDecoder.VPR_MASK_DIFF.size]
        }
        return result.toByte()
    }

    private fun getMask(position: Int): Int {
        var offset = position ushr 4
        var value = 0
        while (offset >= 0x11) {
            value = value xor KgmDecoder.TABLE_1[offset % KgmDecoder.MASK_TABLE_SIZE]
            offset = offset ushr 4
            value = value xor KgmDecoder.TABLE_2[offset % KgmDecoder.MASK_TABLE_SIZE]
            offset = offset ushr 4
        }

        return KgmDecoder.MASK_V2_PRE_DEF[position % KgmDecoder.MASK_TABLE_SIZE] xor value
    }

    private data class DecodePlan(
        val audioOffset: Long,
        val key: ByteArray,
        val mode: KgmMode,
    ) {
        companion object {
            fun from(
                input: RandomAccessFile,
                extension: String,
            ): DecodePlan {
                val mode = KgmMode.fromExtension(extension)
                require(input.length() >= KgmDecoder.MIN_HEADER_SIZE) { "KGM file is too short" }

                val header = ByteArray(KgmDecoder.HEADER_SIZE)
                input.seek(0)
                input.readFully(header)
                require(header.contentEquals(mode.header)) { "Invalid ${mode.label} header" }

                input.seek(KgmDecoder.HEADER_LENGTH_OFFSET.toLong())
                val headerLength = input.readUInt32LittleEndian()
                require(headerLength in KgmDecoder.MIN_HEADER_SIZE..input.length().toInt()) { "Invalid KGM header length" }

                val key =
                    ByteArray(KgmDecoder.KEY_SIZE + 1).apply {
                        input.seek(KgmDecoder.KEY_OFFSET.toLong())
                        input.readFully(this, 0, KgmDecoder.KEY_SIZE)
                    }

                return DecodePlan(
                    audioOffset = headerLength.toLong(),
                    key = key,
                    mode = mode,
                )
            }

            private fun RandomAccessFile.readUInt32LittleEndian(): Int {
                val b0 = readUnsignedByte()
                val b1 = readUnsignedByte()
                val b2 = readUnsignedByte()
                val b3 = readUnsignedByte()
                return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
            }
        }
    }

    private enum class KgmMode(
        val header: ByteArray,
        val label: String,
    ) {
        Kgm(KgmDecoder.KGM_HEADER, "kgm"),
        Vpr(KgmDecoder.VPR_HEADER, "vpr"),
        ;

        companion object {
            fun fromExtension(extension: String): KgmMode {
                return if (extension == "vpr") Vpr else Kgm
            }
        }
    }

    private companion object {
        const val DEFAULT_FALLBACK_EXTENSION = "mp3"
    }
}
