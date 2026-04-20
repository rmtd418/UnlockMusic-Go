package dev.unlockmusic.android.core.decrypt.kgm

import dev.unlockmusic.android.core.decrypt.DecryptResult
import dev.unlockmusic.android.core.decrypt.Decryptor
import dev.unlockmusic.android.core.metadata.AudioHeaderSniffer
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.core.metadata.FilenameParser

class KgmDecryptor : Decryptor {
    override val supportedTypes: Set<DetectedFileType> = setOf(DetectedFileType.KGM)

    override suspend fun decrypt(
        filename: String,
        inputBytes: ByteArray,
    ): DecryptResult {
        val parsed = FilenameParser.parse(filename)
        val decodedBytes = KgmDecoder(inputBytes, parsed.extension).decrypt()
        val outputExtension = AudioHeaderSniffer.sniffExtension(decodedBytes, DEFAULT_FALLBACK_EXTENSION)

        return DecryptResult(
            detectedFileType = DetectedFileType.KGM,
            outputExtension = outputExtension,
            outputBytes = decodedBytes,
            suggestedFileName = "${parsed.baseName}.$outputExtension",
        )
    }

    private companion object {
        const val DEFAULT_FALLBACK_EXTENSION = "mp3"
    }
}
