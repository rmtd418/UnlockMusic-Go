package dev.unlockmusic.android.domain.usecase

import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask
import org.junit.Assert.assertEquals
import org.junit.Test

class RetryUnfinishedUnlockTasksUseCaseTest {
    private val useCase = RetryUnfinishedUnlockTasksUseCase()

    @Test
    fun `retry converts failed canceled and running tasks back to queued`() {
        val tasks =
            listOf(
                task(id = "failed", status = UnlockStatus.Failed("broken")),
                task(id = "canceled", status = UnlockStatus.Canceled),
                task(id = "running", status = UnlockStatus.Running(progressPercent = 55)),
                task(id = "queued", status = UnlockStatus.Queued),
                task(id = "success", status = UnlockStatus.Success("done.flac")),
                task(id = "unsupported", status = UnlockStatus.Failed("unsupported"), detectedFileType = DetectedFileType.UNKNOWN),
            )

        val retried = useCase(tasks)

        assertEquals(UnlockStatus.Queued, retried[0].status)
        assertEquals(UnlockStatus.Queued, retried[1].status)
        assertEquals(UnlockStatus.Queued, retried[2].status)
        assertEquals(UnlockStatus.Queued, retried[3].status)
        assertEquals(UnlockStatus.Success("done.flac"), retried[4].status)
        assertEquals(UnlockStatus.Failed("unsupported"), retried[5].status)
    }

    private fun task(
        id: String,
        status: UnlockStatus,
        detectedFileType: DetectedFileType = DetectedFileType.QMC,
    ): UnlockTask {
        return UnlockTask(
            id = id,
            source =
                UnlockSource(
                    uriString = "content://$id",
                    displayName = "$id.qmc0",
                    detectedFileType = detectedFileType,
                ),
            status = status,
            queuedAtEpochMillis = 1L,
        )
    }
}
