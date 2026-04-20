package dev.unlockmusic.android.domain.usecase

import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask

class CancelUnlockTaskUseCase {
    operator fun invoke(task: UnlockTask): UnlockTask {
        return task.copy(status = UnlockStatus.Canceled)
    }
}

