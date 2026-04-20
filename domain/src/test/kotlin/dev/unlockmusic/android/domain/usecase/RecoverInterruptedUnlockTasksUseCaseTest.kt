package dev.unlockmusic.android.domain.usecase

import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoverInterruptedUnlockTasksUseCaseTest {
    private val useCase = RecoverInterruptedUnlockTasksUseCase()

    @Test
    fun `running tasks become failed while other statuses stay intact`() {
        val tasks =
            listOf(
                task(id = "running", status = UnlockStatus.Running(progressPercent = 45)),
                task(id = "queued", status = UnlockStatus.Queued),
                task(id = "success", status = UnlockStatus.Success("done.flac")),
            )

        val recovered = useCase(tasks)

        assertTrue(recovered[0].status is UnlockStatus.Failed)
        assertEquals(
            RecoverInterruptedUnlockTasksUseCase.DEFAULT_FAILURE_MESSAGE,
            (recovered[0].status as UnlockStatus.Failed).message,
        )
        assertEquals(UnlockStatus.Queued, recovered[1].status)
        assertEquals(UnlockStatus.Success("done.flac"), recovered[2].status)
    }

    private fun task(
        id: String,
        status: UnlockStatus,
    ): UnlockTask {
        return UnlockTask(
            id = id,
            source =
                UnlockSource(
                    uriString = "content://$id",
                    displayName = "$id.qmc0",
                    detectedFileType = DetectedFileType.QMC,
                ),
            status = status,
            queuedAtEpochMillis = 1L,
        )
    }
}
