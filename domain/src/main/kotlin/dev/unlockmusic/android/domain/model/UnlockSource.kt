package dev.unlockmusic.android.domain.model

import dev.unlockmusic.android.core.metadata.DetectedFileType

data class UnlockSource(
    val uriString: String,
    val displayName: String,
    val detectedFileType: DetectedFileType,
)

