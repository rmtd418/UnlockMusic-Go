package dev.unlockmusic.android.app.testing

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException

class TestDocumentsProvider : DocumentsProvider() {
    override fun onCreate(): Boolean {
        resetStorage()
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val row = cursor.newRow()
        for (column in cursor.columnNames) {
            row.add(rootValueFor(column))
        }
        return cursor
    }

    override fun queryDocument(
        documentId: String,
        projection: Array<out String>?,
    ): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        addDocumentRow(cursor, documentId)
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val parent = requireExistingFile(parentDocumentId)
        require(parent.isDirectory) { "Document is not a directory: $parentDocumentId" }

        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        parent.listFiles()
            ?.sortedBy(File::getName)
            ?.forEach { child -> addDocumentRow(cursor, documentIdFor(child)) }
        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        val file = requireExistingFile(documentId)
        if (file.isDirectory) {
            throw FileNotFoundException("Cannot open directory: $documentId")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String,
    ): String {
        val parent = requireExistingFile(parentDocumentId)
        require(parent.isDirectory) { "Parent is not a directory: $parentDocumentId" }

        val child = File(parent, displayName)
        if (mimeType == Document.MIME_TYPE_DIR) {
            check(child.mkdirs() || child.isDirectory) { "Unable to create directory: $displayName" }
        } else {
            child.parentFile?.mkdirs()
            check(child.createNewFile() || child.isFile) { "Unable to create file: $displayName" }
        }
        return documentIdFor(child)
    }

    override fun deleteDocument(documentId: String) {
        val file = requireExistingFile(documentId)
        check(file.deleteRecursively()) { "Unable to delete document: $documentId" }
    }

    override fun isChildDocument(
        parentDocumentId: String,
        documentId: String,
    ): Boolean {
        val parent = requireExistingFile(parentDocumentId).canonicalFile
        val child = requireExistingFile(documentId).canonicalFile
        return child.path == parent.path || child.path.startsWith(parent.path + File.separator)
    }

    override fun getDocumentType(documentId: String): String {
        return mimeTypeFor(requireExistingFile(documentId))
    }

    override fun queryRecentDocuments(
        rootId: String,
        projection: Array<out String>?,
    ): Cursor {
        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
    }

    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<out String>?,
    ): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        storageRoot().walkTopDown()
            .filter { it.isFile && it.name.contains(query, ignoreCase = true) }
            .sortedBy(File::getName)
            ?.forEach { file -> addDocumentRow(cursor, documentIdFor(file)) }
        return cursor
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?,
    ): AssetFileDescriptor {
        throw FileNotFoundException("Thumbnails are not supported")
    }

    override fun call(
        method: String,
        arg: String?,
        extras: Bundle?,
    ): Bundle {
        return when (method) {
            METHOD_RESET -> {
                resetStorage()
                Bundle()
            }

            METHOD_SEED_FILE -> {
                val parentDocumentId =
                    requireNotNull(extras?.getString(EXTRA_PARENT_DOCUMENT_ID)) {
                        "Missing parent document id"
                    }
                val displayName =
                    requireNotNull(extras.getString(EXTRA_DISPLAY_NAME)) {
                        "Missing display name"
                    }
                val bytes = extras.getByteArray(EXTRA_BYTES) ?: ByteArray(0)
                val mimeType = extras.getString(EXTRA_MIME_TYPE) ?: "application/octet-stream"
                val parent = requireExistingFile(parentDocumentId)
                val file = File(parent, displayName)
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)

                Bundle().apply {
                    putString(EXTRA_DOCUMENT_ID, documentIdFor(file))
                    putString(EXTRA_MIME_TYPE, mimeType)
                }
            }

            METHOD_READ_BYTES -> {
                val documentId =
                    requireNotNull(extras?.getString(EXTRA_DOCUMENT_ID)) {
                        "Missing document id"
                    }
                Bundle().apply {
                    putByteArray(EXTRA_BYTES, requireExistingFile(documentId).readBytes())
                }
            }

            else -> super.call(method, arg, extras) ?: Bundle()
        }
    }

    private fun addDocumentRow(
        cursor: MatrixCursor,
        documentId: String,
    ) {
        val file = requireExistingFile(documentId)
        val row = cursor.newRow()
        for (column in cursor.columnNames) {
            row.add(documentValueFor(column, documentId, file))
        }
    }

    private fun rootValueFor(column: String): Any? {
        return when (column) {
            Root.COLUMN_ROOT_ID -> ROOT_ID
            Root.COLUMN_DOCUMENT_ID -> ROOT_DOCUMENT_ID
            Root.COLUMN_TITLE -> "Unlock Music Android Tests"
            Root.COLUMN_FLAGS -> Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_CREATE
            Root.COLUMN_MIME_TYPES -> "*/*"
            Root.COLUMN_AVAILABLE_BYTES -> outputDirectory().usableSpace
            else -> null
        }
    }

    private fun documentValueFor(
        column: String,
        documentId: String,
        file: File,
    ): Any? {
        return when (column) {
            Document.COLUMN_DOCUMENT_ID -> documentId
            Document.COLUMN_DISPLAY_NAME -> displayNameFor(documentId, file)
            Document.COLUMN_MIME_TYPE -> mimeTypeFor(file)
            Document.COLUMN_FLAGS -> flagsFor(file)
            Document.COLUMN_SIZE -> if (file.isFile) file.length() else 0L
            Document.COLUMN_LAST_MODIFIED -> file.lastModified()
            else -> null
        }
    }

    private fun displayNameFor(
        documentId: String,
        file: File,
    ): String {
        return when (documentId) {
            ROOT_DOCUMENT_ID -> "unlock-music-test-root"
            INPUT_DIRECTORY_DOCUMENT_ID -> "input"
            OUTPUT_DIRECTORY_DOCUMENT_ID -> "output"
            else -> file.name
        }
    }

    private fun mimeTypeFor(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR

        return when (file.extension.lowercase()) {
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/x-wav"
            "wma" -> "audio/x-ms-wma"
            else -> "application/octet-stream"
        }
    }

    private fun flagsFor(file: File): Int {
        var flags = Document.FLAG_SUPPORTS_DELETE
        if (file.isDirectory) {
            flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        return flags
    }

    private fun storageRoot(): File {
        val providerContext = requireContext()
        return File(providerContext.cacheDir, STORAGE_DIRECTORY_NAME)
    }

    private fun outputDirectory(): File = requireExistingFile(OUTPUT_DIRECTORY_DOCUMENT_ID)

    private fun resetStorage() {
        val base = storageRoot()
        if (base.exists()) {
            check(base.deleteRecursively()) { "Unable to clear provider storage" }
        }
        check(File(base, ROOT_DOCUMENT_ID).mkdirs()) { "Unable to create root storage" }
        check(File(base, INPUT_DIRECTORY_DOCUMENT_ID).mkdirs()) { "Unable to create input directory" }
        check(File(base, OUTPUT_DIRECTORY_DOCUMENT_ID).mkdirs()) { "Unable to create output directory" }
    }

    private fun requireExistingFile(documentId: String): File {
        val base = storageRoot().canonicalFile
        val candidate =
            File(base, documentId.replace('/', File.separatorChar)).canonicalFile
        require(candidate.path.startsWith(base.path + File.separator) || candidate.path == base.path) {
            "Document escapes provider root: $documentId"
        }
        if (!candidate.exists()) {
            throw FileNotFoundException("Missing document: $documentId")
        }
        return candidate
    }

    private fun documentIdFor(file: File): String {
        return file.canonicalFile
            .relativeTo(storageRoot().canonicalFile)
            .invariantSeparatorsPath
    }

    companion object {
        const val AUTHORITY = "dev.unlockmusic.android.test.documents"
        const val ROOT_ID = "unlock-music-tests"
        const val ROOT_DOCUMENT_ID = "root"
        const val INPUT_DIRECTORY_DOCUMENT_ID = "root/input"
        const val OUTPUT_DIRECTORY_DOCUMENT_ID = "root/output"

        const val METHOD_RESET = "reset"
        const val METHOD_SEED_FILE = "seed_file"
        const val METHOD_READ_BYTES = "read_bytes"

        const val EXTRA_PARENT_DOCUMENT_ID = "parent_document_id"
        const val EXTRA_DOCUMENT_ID = "document_id"
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_MIME_TYPE = "mime_type"
        const val EXTRA_BYTES = "bytes"

        private const val STORAGE_DIRECTORY_NAME = "test-documents-provider"

        private val DEFAULT_ROOT_PROJECTION =
            arrayOf(
                Root.COLUMN_ROOT_ID,
                Root.COLUMN_DOCUMENT_ID,
                Root.COLUMN_TITLE,
                Root.COLUMN_FLAGS,
                Root.COLUMN_MIME_TYPES,
                Root.COLUMN_AVAILABLE_BYTES,
            )

        private val DEFAULT_DOCUMENT_PROJECTION =
            arrayOf(
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_MIME_TYPE,
                Document.COLUMN_FLAGS,
                Document.COLUMN_SIZE,
                Document.COLUMN_LAST_MODIFIED,
            )
    }
}
