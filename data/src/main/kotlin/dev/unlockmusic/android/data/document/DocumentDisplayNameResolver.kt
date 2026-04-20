package dev.unlockmusic.android.data.document

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

class DocumentDisplayNameResolver(
    private val contentResolver: ContentResolver,
) {
    fun resolve(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }

        return uri.lastPathSegment ?: "unknown"
    }
}

