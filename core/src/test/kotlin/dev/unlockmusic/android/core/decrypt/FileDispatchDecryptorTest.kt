package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.decrypt.kgm.KgmTestData
import dev.unlockmusic.android.core.decrypt.ncm.NcmTestData
import dev.unlockmusic.android.core.decrypt.qmc.QmcTestData
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileDispatchDecryptorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val decryptor = DefaultFileDispatchDecryptor.create()

    @Test
    fun `file dispatch decrypts qmc without materializing result bytes`() {
        val encrypted = QmcTestData.bytes("qmc0_static_raw.bin") + QmcTestData.bytes("qmc0_static_suffix.bin")
        val expected = QmcTestData.bytes("qmc0_static_target.bin")

        val result = decryptToFile("track.qmc0", encrypted)

        assertEquals("mp3", result.outputExtension)
        assertEquals("track.mp3", result.suggestedFileName)
        assertArrayEquals(expected, result.outputFile.readBytes())
    }

    @Test
    fun `file dispatch decrypts ncm without materializing result bytes`() {
        val expected = NcmTestData.flacAudio()
        val encrypted = NcmTestData.encryptedFile(expected, format = "flac")

        val result = decryptToFile("track.ncm", encrypted)

        assertEquals("flac", result.outputExtension)
        assertEquals("track.flac", result.suggestedFileName)
        assertArrayEquals(expected, result.outputFile.readBytes())
    }

    @Test
    fun `file dispatch decrypts kgm without materializing result bytes`() {
        val expected = KgmTestData.flacAudio()
        val encrypted = KgmTestData.kgmFile(expected)

        val result = decryptToFile("track.kgm", encrypted)

        assertEquals("flac", result.outputExtension)
        assertEquals("track.flac", result.suggestedFileName)
        assertArrayEquals(expected, result.outputFile.readBytes())
    }

    private fun decryptToFile(
        filename: String,
        encrypted: ByteArray,
    ): FileDecryptResult {
        val inputFile = temporaryFolder.newFile("input-${filename.replace('.', '-')}")
        val outputFile = temporaryFolder.newFile("output-${filename.replace('.', '-')}")
        inputFile.writeBytes(encrypted)

        return decryptor.decryptToFile(filename, inputFile, outputFile)
    }
}
