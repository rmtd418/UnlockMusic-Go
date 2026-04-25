package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.metadata.AudioHeaderSniffer
import java.io.File
import java.io.OutputStream

internal const val STREAM_BUFFER_SIZE = 64 * 1024

internal class HeaderCaptureOutputStream(
    private val delegate: OutputStream,
    private val captureLimit: Int = 64,
) : OutputStream() {
    private val header = ByteArray(captureLimit)
    private var capturedCount = 0

    override fun write(b: Int) {
        if (capturedCount < header.size) {
            header[capturedCount++] = b.toByte()
        }
        delegate.write(b)
    }

    override fun write(
        b: ByteArray,
        off: Int,
        len: Int,
    ) {
        val remainingCapture = header.size - capturedCount
        if (remainingCapture > 0) {
            val toCapture = minOf(remainingCapture, len)
            b.copyInto(
                destination = header,
                destinationOffset = capturedCount,
                startIndex = off,
                endIndex = off + toCapture,
            )
            capturedCount += toCapture
        }
        delegate.write(b, off, len)
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        delegate.close()
    }

    fun sniffExtension(fallbackExtension: String): String {
        return AudioHeaderSniffer.sniffExtension(header.copyOf(capturedCount), fallbackExtension)
    }
}

internal fun File.resetForWrite() {
    parentFile?.mkdirs()
    if (exists()) {
        check(delete()) { "Unable to replace temporary output file: $absolutePath" }
    }
    check(createNewFile()) { "Unable to create temporary output file: $absolutePath" }
}
