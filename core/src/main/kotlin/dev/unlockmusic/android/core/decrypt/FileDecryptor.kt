package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.metadata.DetectedFileType
import java.io.File

interface FileDecryptor {
    val supportedTypes: Set<DetectedFileType>

    fun decryptToFile(
        filename: String,
        inputFile: File,
        outputFile: File,
    ): FileDecryptResult
}
