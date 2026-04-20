package dev.unlockmusic.android.data.document

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

class UriPermissionManager(
    private val contentResolver: ContentResolver,
) {
    fun persistReadPermission(uri: Uri) {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun persistDirectoryPermission(uri: Uri) {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, flags)
    }

    fun hasPersistedReadPermission(uri: Uri): Boolean {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return true
        return contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
    }

    fun hasPersistedDirectoryPermission(uri: Uri): Boolean {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return true
        return contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }
}
