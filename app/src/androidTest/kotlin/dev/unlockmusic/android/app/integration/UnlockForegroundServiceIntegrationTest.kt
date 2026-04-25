package dev.unlockmusic.android.app.integration

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.unlockmusic.android.app.execution.UnlockExecutionState
import dev.unlockmusic.android.app.execution.UnlockExecutionStore
import dev.unlockmusic.android.app.execution.UnlockForegroundService
import dev.unlockmusic.android.app.testing.TestDocuments
import dev.unlockmusic.android.core.decrypt.qmc.QmcStaticCipher
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.data.document.DocumentBytesReader
import dev.unlockmusic.android.data.settings.LastSessionSettingsStore
import dev.unlockmusic.android.data.settings.UnlockExecutionSnapshotStore
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnlockForegroundServiceIntegrationTest {
    private lateinit var context: Context
    private lateinit var documentBytesReader: DocumentBytesReader

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        documentBytesReader = DocumentBytesReader(context.contentResolver)
        resetFixtures()
    }

    @After
    fun tearDown() {
        context.stopService(Intent(context, UnlockForegroundService::class.java))
        resetFixtures()
    }

    @Test
    fun foregroundServiceProcessesQueuedDocumentsFromProviderIntoOutputTree() {
        val mp3Plain = byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00, 0x00, 0x41, 0x42, 0x43)
        val flacPlain = byteArrayOf(0x66, 0x4C, 0x61, 0x43, 0x11, 0x22, 0x33, 0x44)

        val mp3SourceUri =
            TestDocuments.seedInputFile(
                context.contentResolver,
                "alpha.qmc0",
                encryptQmcStatic(mp3Plain),
            )
        val flacSourceUri =
            TestDocuments.seedInputFile(
                context.contentResolver,
                "beta.mflac0",
                encryptQmcStatic(flacPlain),
            )

        UnlockExecutionStore.replaceQueue(
            tasks =
                listOf(
                    queuedTask("task-alpha", mp3SourceUri.toString(), "alpha.qmc0"),
                    queuedTask("task-beta", flacSourceUri.toString(), "beta.mflac0"),
                ),
            outputDirectoryUri = TestDocuments.outputTreeUri().toString(),
        )

        context.startForegroundService(
            Intent(context, UnlockForegroundService::class.java).apply {
                action = UnlockForegroundService.ACTION_RUN_QUEUE
            },
        )

        awaitFinishedState()

        val finalState = UnlockExecutionStore.snapshot()
        assertEquals(2, finalState.summary.success)
        assertEquals(0, finalState.summary.failed)
        assertEquals(0, finalState.summary.canceled)
        assertEquals(2, finalState.processedCount)
        assertTrue(finalState.tasks.all { it.status is UnlockStatus.Success })
        assertEquals(
            "批量解锁完成：成功解锁 2 个文件。",
            finalState.lastMessage,
        )
        assertArrayEquals(mp3Plain, documentBytesReader.readBytes(TestDocuments.outputDocumentUri("alpha.mp3")))
        assertArrayEquals(flacPlain, documentBytesReader.readBytes(TestDocuments.outputDocumentUri("beta.flac")))
    }

    private fun awaitFinishedState(timeoutMillis: Long = 15_000) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        var lastState = UnlockExecutionStore.snapshot()
        while (SystemClock.elapsedRealtime() < deadline) {
            lastState = UnlockExecutionStore.snapshot()
            if (!lastState.isExecuting && lastState.tasks.all { it.status !is UnlockStatus.Queued && it.status !is UnlockStatus.Running }) {
                return
            }
            SystemClock.sleep(100)
        }
        throw AssertionError("Foreground service did not finish in time. Last state: $lastState")
    }

    private fun queuedTask(
        id: String,
        uriString: String,
        displayName: String,
    ): UnlockTask {
        return UnlockTask(
            id = id,
            source =
                UnlockSource(
                    uriString = uriString,
                    displayName = displayName,
                    detectedFileType = DetectedFileType.QMC,
                ),
            status = UnlockStatus.Queued,
            queuedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    private fun resetFixtures() {
        TestDocuments.reset(context.contentResolver)
        runBlocking {
            UnlockExecutionSnapshotStore(context).clear()
            LastSessionSettingsStore(context).clear()
        }
        UnlockExecutionStore.restore(UnlockExecutionState())
    }

    private fun encryptQmcStatic(plainBytes: ByteArray): ByteArray {
        return plainBytes.copyOf().apply {
            QmcStaticCipher().decrypt(this, 0)
        }
    }
}
