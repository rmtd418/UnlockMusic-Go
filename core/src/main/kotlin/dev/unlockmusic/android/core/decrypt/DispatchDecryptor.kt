package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.metadata.detectFileType

class DispatchDecryptor(
    private val decryptors: List<Decryptor>,
) {
    suspend fun decrypt(
        filename: String,
        inputBytes: ByteArray,
    ): DecryptResult {
        val detectedFileType = detectFileType(filename)
        val decryptor = decryptors.firstOrNull { detectedFileType in it.supportedTypes }
            ?: error("No decryptor registered for $detectedFileType")

        return decryptor.decrypt(filename, inputBytes)
    }
}

