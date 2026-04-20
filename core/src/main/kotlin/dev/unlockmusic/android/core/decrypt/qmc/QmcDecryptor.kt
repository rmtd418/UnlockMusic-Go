package dev.unlockmusic.android.core.decrypt.qmc

import dev.unlockmusic.android.core.decrypt.DecryptResult
import dev.unlockmusic.android.core.decrypt.Decryptor
import dev.unlockmusic.android.core.metadata.AudioHeaderSniffer
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.core.metadata.FilenameParser

class QmcDecryptor : Decryptor {
    override val supportedTypes: Set<DetectedFileType> = setOf(DetectedFileType.QMC)

    override suspend fun decrypt(
        filename: String,
        inputBytes: ByteArray,
    ): DecryptResult {
        val parsed = FilenameParser.parse(filename)
        val fallbackExtension = requireNotNull(HANDLER_MAP[parsed.extension]) {
            "QMC cannot handle type: ${parsed.extension}"
        }
        val decodedBytes = QmcDecoder(inputBytes).decrypt()
        val outputExtension = AudioHeaderSniffer.sniffExtension(decodedBytes, fallbackExtension)

        return DecryptResult(
            detectedFileType = DetectedFileType.QMC,
            outputExtension = outputExtension,
            outputBytes = decodedBytes,
            suggestedFileName = "${parsed.baseName}.$outputExtension",
        )
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
