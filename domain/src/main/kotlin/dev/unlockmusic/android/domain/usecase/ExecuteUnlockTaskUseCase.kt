package dev.unlockmusic.android.domain.usecase

import dev.unlockmusic.android.core.decrypt.DefaultDispatchDecryptor
import dev.unlockmusic.android.core.decrypt.DecryptResult
import dev.unlockmusic.android.core.decrypt.DispatchDecryptor
import dev.unlockmusic.android.domain.model.UnlockTask

class ExecuteUnlockTaskUseCase(
    private val dispatchDecryptor: DispatchDecryptor = DefaultDispatchDecryptor.create(),
) {
    suspend operator fun invoke(
        task: UnlockTask,
        inputBytes: ByteArray,
    ): DecryptResult {
        return dispatchDecryptor.decrypt(
            filename = task.source.displayName,
            inputBytes = inputBytes,
        )
    }
}
