package dev.unlockmusic.android.domain.model

sealed interface UnlockStatus {
    data object Queued : UnlockStatus

    data class Running(
        val progressPercent: Int,
    ) : UnlockStatus

    data class Success(
        val outputName: String,
    ) : UnlockStatus

    data class Failed(
        val message: String,
    ) : UnlockStatus

    data object Canceled : UnlockStatus
}

