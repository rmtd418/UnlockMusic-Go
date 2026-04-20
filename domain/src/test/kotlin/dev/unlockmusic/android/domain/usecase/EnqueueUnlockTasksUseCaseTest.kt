package dev.unlockmusic.android.domain.usecase

import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnqueueUnlockTasksUseCaseTest {
    private val useCase = EnqueueUnlockTasksUseCase()

    @Test
    fun `supported files stay queued while unknown files fail immediately`() {
        val tasks =
            useCase(
                sources =
                    listOf(
                        source(displayName = "supported.qmc0", detectedFileType = DetectedFileType.QMC),
                        source(displayName = "unknown.bin", detectedFileType = DetectedFileType.UNKNOWN),
                    ),
                nowEpochMillis = 123L,
            )

        assertEquals(2, tasks.size)
        assertEquals(UnlockStatus.Queued, tasks[0].status)
        assertTrue(tasks[1].status is UnlockStatus.Failed)
        assertEquals(
            EnqueueUnlockTasksUseCase.UNSUPPORTED_FILE_MESSAGE,
            (tasks[1].status as UnlockStatus.Failed).message,
        )
        assertEquals(123L, tasks[0].queuedAtEpochMillis)
        assertEquals(123L, tasks[1].queuedAtEpochMillis)
    }

    private fun source(
        displayName: String,
        detectedFileType: DetectedFileType,
    ): UnlockSource {
        return UnlockSource(
            uriString = "content://$displayName",
            displayName = displayName,
            detectedFileType = detectedFileType,
        )
    }
}
