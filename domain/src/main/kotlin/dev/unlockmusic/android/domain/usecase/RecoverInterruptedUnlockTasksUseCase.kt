package dev.unlockmusic.android.domain.usecase

import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask

class RecoverInterruptedUnlockTasksUseCase(
    private val failureMessage: String = DEFAULT_FAILURE_MESSAGE,
) {
    operator fun invoke(tasks: List<UnlockTask>): List<UnlockTask> {
        return tasks.map { task ->
            when (task.status) {
                is UnlockStatus.Running -> task.copy(status = UnlockStatus.Failed(failureMessage))
                else -> task
            }
        }
    }

    companion object {
        const val DEFAULT_FAILURE_MESSAGE = "任务在完成前被中断，请重试未完成的文件后继续。"
    }
}
