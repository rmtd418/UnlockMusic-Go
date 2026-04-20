package dev.unlockmusic.android.core.decrypt.qmc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.runBlocking

class QmcDecryptorTest {
    @Test
    fun `decryptor emits expected extension and bytes`() {
        val encrypted = QmcTestData.bytes("mflac0_rc4_raw.bin") + QmcTestData.bytes("mflac0_rc4_suffix.bin")
        val expected = QmcTestData.bytes("mflac0_rc4_target.bin")

        val result =
            runBlocking {
                QmcDecryptor().decrypt("sample.mflac0", encrypted)
            }

        assertEquals("flac", result.outputExtension)
        assertEquals("sample.flac", result.suggestedFileName)
        assertArrayEquals(expected, result.outputBytes)
    }
}
