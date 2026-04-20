package dev.unlockmusic.android.core.decrypt.ncm

import dev.unlockmusic.android.core.decrypt.DecryptResult
import dev.unlockmusic.android.core.decrypt.Decryptor
import dev.unlockmusic.android.core.metadata.AudioHeaderSniffer
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.core.metadata.FilenameParser

class NcmDecryptor : Decryptor {
    override val supportedTypes: Set<DetectedFileType> = setOf(DetectedFileType.NCM)

    override suspend fun decrypt(
        filename: String,
        inputBytes: ByteArray,
    ): DecryptResult {
        val parsed = FilenameParser.parse(filename)
        val decoded = NcmDecoder(inputBytes).decrypt()
        val fallbackExtension = decoded.format ?: DEFAULT_FALLBACK_EXTENSION
        val outputExtension = AudioHeaderSniffer.sniffExtension(decoded.audioBytes, fallbackExtension)

        return DecryptResult(
            detectedFileType = DetectedFileType.NCM,
            outputExtension = outputExtension,
            outputBytes = decoded.audioBytes,
            suggestedFileName = "${parsed.baseName}.$outputExtension",
        )
    }

    private companion object {
        const val DEFAULT_FALLBACK_EXTENSION = "mp3"
    }
}
