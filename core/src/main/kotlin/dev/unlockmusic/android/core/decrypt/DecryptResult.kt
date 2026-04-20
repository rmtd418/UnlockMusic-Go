package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.metadata.DetectedFileType

data class DecryptResult(
    val detectedFileType: DetectedFileType,
    val outputExtension: String,
    val outputBytes: ByteArray,
    val suggestedFileName: String,
)

