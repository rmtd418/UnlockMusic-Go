package dev.unlockmusic.android.data.document

import android.content.Context
import android.net.Uri
import java.io.File

class DefaultOutputDirectoryManager(
    context: Context,
) {
    private val applicationContext = context.applicationContext

    fun ensureDefaultOutputDirectoryUri(): String {
        val baseDirectory =
            applicationContext.getExternalFilesDir(null)
                ?: applicationContext.filesDir
        val outputDirectory = File(baseDirectory, DEFAULT_OUTPUT_DIRECTORY_NAME)
        check(outputDirectory.exists() || outputDirectory.mkdirs()) {
            "Unable to create default output directory: ${outputDirectory.absolutePath}"
        }
        return Uri.fromFile(outputDirectory).toString()
    }

    companion object {
        const val DEFAULT_OUTPUT_DIRECTORY_NAME = "UnlockMusicOutput"
    }
}
