package dev.unlockmusic.android.data.document

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

class TreeDocumentWriter(
    private val context: Context,
    private val contentResolver: ContentResolver,
) {
    fun writeBytes(
        treeUri: Uri,
        displayName: String,
        bytes: ByteArray,
        mimeType: String = "application/octet-stream",
    ): String {
        if (treeUri.scheme == ContentResolver.SCHEME_FILE) {
            return writeBytesToFileDirectory(treeUri, displayName, bytes)
        }

        val directory = requireNotNull(DocumentFile.fromTreeUri(context, treeUri)) {
            "Unable to resolve output directory"
        }
        val resolvedName = resolveUniqueDisplayName(directory, displayName)
        val outputDocument = requireNotNull(directory.createFile(mimeType, resolvedName)) {
            "Unable to create output file: $resolvedName"
        }

        requireNotNull(contentResolver.openOutputStream(outputDocument.uri, "w")) {
            "Unable to open output stream for ${outputDocument.uri}"
        }.use { output ->
            output.write(bytes)
            output.flush()
        }

        return resolvedName
    }

    fun writeFile(
        treeUri: Uri,
        displayName: String,
        sourceFile: File,
        mimeType: String = "application/octet-stream",
    ): String {
        if (treeUri.scheme == ContentResolver.SCHEME_FILE) {
            return writeFileToFileDirectory(treeUri, displayName, sourceFile)
        }

        val directory = requireNotNull(DocumentFile.fromTreeUri(context, treeUri)) {
            "Unable to resolve output directory"
        }
        val resolvedName = resolveUniqueDisplayName(directory, displayName)
        val outputDocument = requireNotNull(directory.createFile(mimeType, resolvedName)) {
            "Unable to create output file: $resolvedName"
        }

        requireNotNull(contentResolver.openOutputStream(outputDocument.uri, "w")) {
            "Unable to open output stream for ${outputDocument.uri}"
        }.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
            output.flush()
        }

        return resolvedName
    }

    private fun writeBytesToFileDirectory(
        directoryUri: Uri,
        displayName: String,
        bytes: ByteArray,
    ): String {
        val directory =
            requireNotNull(directoryUri.path) { "Unable to resolve output directory path" }
                .let(::File)
        check(directory.exists() || directory.mkdirs()) {
            "Unable to create output directory: ${directory.absolutePath}"
        }
        check(directory.isDirectory && directory.canWrite()) {
            "Output directory is not writable: ${directory.absolutePath}"
        }

        val resolvedName = resolveUniqueDisplayName(directory, displayName)
        File(directory, resolvedName).outputStream().use { output ->
            output.write(bytes)
            output.flush()
        }
        return resolvedName
    }

    private fun writeFileToFileDirectory(
        directoryUri: Uri,
        displayName: String,
        sourceFile: File,
    ): String {
        val directory =
            requireNotNull(directoryUri.path) { "Unable to resolve output directory path" }
                .let(::File)
        check(directory.exists() || directory.mkdirs()) {
            "Unable to create output directory: ${directory.absolutePath}"
        }
        check(directory.isDirectory && directory.canWrite()) {
            "Output directory is not writable: ${directory.absolutePath}"
        }

        val resolvedName = resolveUniqueDisplayName(directory, displayName)
        sourceFile.inputStream().use { input ->
            File(directory, resolvedName).outputStream().use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
        return resolvedName
    }

    private fun resolveUniqueDisplayName(
        directory: DocumentFile,
        displayName: String,
    ): String {
        if (directory.findFile(displayName) == null) return displayName

        val separator = displayName.lastIndexOf('.')
        val baseName = if (separator > 0) displayName.substring(0, separator) else displayName
        val extension = if (separator > 0) displayName.substring(separator) else ""

        var suffix = 1
        while (true) {
            val candidate = "$baseName ($suffix)$extension"
            if (directory.findFile(candidate) == null) {
                return candidate
            }
            suffix++
        }
    }

    private fun resolveUniqueDisplayName(
        directory: File,
        displayName: String,
    ): String {
        if (!File(directory, displayName).exists()) return displayName

        val separator = displayName.lastIndexOf('.')
        val baseName = if (separator > 0) displayName.substring(0, separator) else displayName
        val extension = if (separator > 0) displayName.substring(separator) else ""

        var suffix = 1
        while (true) {
            val candidate = "$baseName ($suffix)$extension"
            if (!File(directory, candidate).exists()) {
                return candidate
            }
            suffix++
        }
    }
}
