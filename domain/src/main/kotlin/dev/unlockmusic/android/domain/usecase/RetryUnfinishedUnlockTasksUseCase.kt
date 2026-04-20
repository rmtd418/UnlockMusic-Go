package dev.unlockmusic.android.domain.usecase

import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask

class RetryUnfinishedUnlockTasksUseCase {
    operator fun invoke(tasks: List<UnlockTask>): List<UnlockTask> {
        return tasks.map { task ->
            when (task.status) {
                UnlockStatus.Canceled,
                is UnlockStatus.Failed,
                is UnlockStatus.Running,
                -> task.copy(status = UnlockStatus.Queued)
                UnlockStatus.Queued,
                is UnlockStatus.Success,
                -> task
            }
        }
    }
}
