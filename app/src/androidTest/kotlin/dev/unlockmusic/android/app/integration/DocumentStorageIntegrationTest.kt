package dev.unlockmusic.android.app.integration

import android.content.Context
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.unlockmusic.android.app.testing.TestDocuments
import dev.unlockmusic.android.core.decrypt.qmc.QmcStaticCipher
import dev.unlockmusic.android.data.document.DocumentBytesReader
import dev.unlockmusic.android.data.document.TreeDocumentWriter
import dev.unlockmusic.android.data.settings.LastSessionSettingsStore
import dev.unlockmusic.android.data.settings.UnlockExecutionSnapshotStore
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentStorageIntegrationTest {
    private lateinit var context: Context
    private lateinit var documentBytesReader: DocumentBytesReader
    private lateinit var treeDocumentWriter: TreeDocumentWriter

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        documentBytesReader = DocumentBytesReader(context.contentResolver)
        treeDocumentWriter = TreeDocumentWriter(context, context.contentResolver)
        resetFixtures()
    }

    @After
    fun tearDown() {
        resetFixtures()
    }

    @Test
    fun documentBytesReaderReadsSourceBytesFromDocumentsProvider() {
        val expected = encryptQmcStatic(byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00, 0x00, 0x21, 0x32))
        val sourceUri = TestDocuments.seedInputFile(context.contentResolver, "reader.qmc0", expected)

        val actual = documentBytesReader.readBytes(sourceUri)

        assertArrayEquals(expected, actual)
    }

    @Test
    fun treeDocumentWriterWritesFilesAndResolvesCollisionsThroughTreeUri() {
        val firstBytes = byteArrayOf(0x49, 0x44, 0x33, 0x10, 0x20)
        val secondBytes = byteArrayOf(0x49, 0x44, 0x33, 0x30, 0x40)

        val firstName =
            treeDocumentWriter.writeBytes(
                treeUri = TestDocuments.outputTreeUri(),
                displayName = "track.mp3",
                bytes = firstBytes,
                mimeType = "audio/mpeg",
            )
        val secondName =
            treeDocumentWriter.writeBytes(
                treeUri = TestDocuments.outputTreeUri(),
                displayName = "track.mp3",
                bytes = secondBytes,
                mimeType = "audio/mpeg",
            )

        eventually {
            readBytesOrNull(firstName) != null && readBytesOrNull(secondName) != null
        }

        assertEquals("track.mp3", firstName)
        assertEquals("track (1).mp3", secondName)
        assertArrayEquals(firstBytes, readBytesOrNull(firstName))
        assertArrayEquals(secondBytes, readBytesOrNull(secondName))
    }

    private fun readBytesOrNull(displayName: String): ByteArray? {
        return runCatching {
            documentBytesReader.readBytes(TestDocuments.outputDocumentUri(displayName))
        }.getOrNull()
    }

    private fun resetFixtures() {
        TestDocuments.reset(context.contentResolver)
        UnlockExecutionSnapshotStore(context).clear()
        LastSessionSettingsStore(context).clear()
    }

    private fun encryptQmcStatic(plainBytes: ByteArray): ByteArray {
        return plainBytes.copyOf().apply {
            QmcStaticCipher().decrypt(this, 0)
        }
    }

    private fun eventually(
        timeoutMillis: Long = 5_000,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for condition.")
    }
}
