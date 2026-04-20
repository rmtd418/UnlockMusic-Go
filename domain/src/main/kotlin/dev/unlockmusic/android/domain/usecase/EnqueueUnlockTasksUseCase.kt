package dev.unlockmusic.android.domain.usecase

import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask
import java.util.UUID

class EnqueueUnlockTasksUseCase {
    operator fun invoke(
        sources: List<UnlockSource>,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): List<UnlockTask> {
        return sources.map { source ->
            UnlockTask(
                id = UUID.randomUUID().toString(),
                source = source,
                status =
                    if (source.detectedFileType == DetectedFileType.UNKNOWN) {
                        UnlockStatus.Failed(UNSUPPORTED_FILE_MESSAGE)
                    } else {
                        UnlockStatus.Queued
                    },
                queuedAtEpochMillis = nowEpochMillis,
            )
        }
    }

    companion object {
        const val UNSUPPORTED_FILE_MESSAGE = "当前 Android 版本暂不支持此文件类型。"
    }
}
