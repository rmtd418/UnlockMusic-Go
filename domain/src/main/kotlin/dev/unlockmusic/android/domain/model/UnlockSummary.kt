package dev.unlockmusic.android.domain.model

data class UnlockSummary(
    val total: Int,
    val queued: Int,
    val running: Int,
    val success: Int,
    val failed: Int,
    val canceled: Int,
) {
    companion object {
        fun from(tasks: List<UnlockTask>): UnlockSummary {
            return UnlockSummary(
                total = tasks.size,
                queued = tasks.count { it.status is UnlockStatus.Queued },
                running = tasks.count { it.status is UnlockStatus.Running },
                success = tasks.count { it.status is UnlockStatus.Success },
                failed = tasks.count { it.status is UnlockStatus.Failed },
                canceled = tasks.count { it.status is UnlockStatus.Canceled },
            )
        }
    }
}

