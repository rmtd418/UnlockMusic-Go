package dev.unlockmusic.android.data.document

import android.content.ContentResolver
import android.net.Uri

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
}

