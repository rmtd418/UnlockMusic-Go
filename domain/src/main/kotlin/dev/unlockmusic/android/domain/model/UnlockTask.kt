package dev.unlockmusic.android.domain.model

data class UnlockTask(
    val id: String,
    val source: UnlockSource,
    val status: UnlockStatus,
    val queuedAtEpochMillis: Long,
)

