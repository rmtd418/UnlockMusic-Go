package dev.unlockmusic.android.data.document

import android.content.ContentResolver
import android.net.Uri
import java.io.File

class DocumentBytesReader(
    private val contentResolver: ContentResolver,
) {
    fun readBytes(uri: Uri): ByteArray {
        return requireNotNull(contentResolver.openInputStream(uri)) {
            "Unable to open input stream for $uri"
        }.use { input ->
            input.readBytes()
        }
    }

    fun copyToFile(
        uri: Uri,
        destination: File,
    ) {
        destination.parentFile?.mkdirs()
        requireNotNull(contentResolver.openInputStream(uri)) {
            "Unable to open input stream for $uri"
        }.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
    }
}
