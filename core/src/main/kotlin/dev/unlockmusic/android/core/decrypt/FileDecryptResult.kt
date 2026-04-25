package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.metadata.DetectedFileType
import java.io.File

data class FileDecryptResult(
    val detectedFileType: DetectedFileType,
    val outputExtension: String,
    val outputFile: File,
    val suggestedFileName: String,
)
