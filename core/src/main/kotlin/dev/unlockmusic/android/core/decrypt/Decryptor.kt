package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.metadata.DetectedFileType

interface Decryptor {
    val supportedTypes: Set<DetectedFileType>

    suspend fun decrypt(
        filename: String,
        inputBytes: ByteArray,
    ): DecryptResult
}

