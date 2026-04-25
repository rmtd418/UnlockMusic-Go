package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.metadata.detectFileType
import java.io.File

class FileDispatchDecryptor(
    private val decryptors: List<FileDecryptor>,
) {
    fun decryptToFile(
        filename: String,
        inputFile: File,
        outputFile: File,
    ): FileDecryptResult {
        val detectedFileType = detectFileType(filename)
        val decryptor = decryptors.firstOrNull { detectedFileType in it.supportedTypes }
            ?: error("No decryptor registered for $detectedFileType")

        return decryptor.decryptToFile(filename, inputFile, outputFile)
    }
}
