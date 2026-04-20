package dev.unlockmusic.android.app.execution

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dev.unlockmusic.android.data.document.UriPermissionManager
import dev.unlockmusic.android.domain.model.UnlockStatus
import java.io.File

data class UnlockExecutionPreflightResult(
    val outputDirectoryError: String? = null,
    val missingSourceTaskIds: Set<String> = emptySet(),
)

class UnlockExecutionPreflightChecker(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val uriPermissionManager = UriPermissionManager(applicationContext.contentResolver)

    fun validate(state: UnlockExecutionState): UnlockExecutionPreflightResult {
        val outputDirectoryUriString =
            state.outputDirectoryUri
                ?: return UnlockExecutionPreflightResult(
                    outputDirectoryError = "运行队列前请先选择输出目录。",
                )
        val outputDirectoryUri = Uri.parse(outputDirectoryUriString)

        if (outputDirectoryUri.scheme == ContentResolver.SCHEME_FILE) {
            val outputDirectory =
                requireNotNull(outputDirectoryUri.path) { "Missing file output directory path" }
                    .let(::File)
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }
            if (!outputDirectory.exists() || !outputDirectory.isDirectory || !outputDirectory.canWrite()) {
                return UnlockExecutionPreflightResult(
                    outputDirectoryError = INVALID_OUTPUT_DIRECTORY_MESSAGE,
                )
            }
            return validateSourcePermissions(state)
        }

        if (!uriPermissionManager.hasPersistedDirectoryPermission(outputDirectoryUri)) {
            return UnlockExecutionPreflightResult(
                outputDirectoryError = INVALID_OUTPUT_DIRECTORY_MESSAGE,
            )
        }

        val outputDirectory = DocumentFile.fromTreeUri(applicationContext, outputDirectoryUri)
        if (outputDirectory == null || !outputDirectory.exists() || !outputDirectory.isDirectory || !outputDirectory.canWrite()) {
            return UnlockExecutionPreflightResult(
                outputDirectoryError = INVALID_OUTPUT_DIRECTORY_MESSAGE,
            )
        }

        return validateSourcePermissions(state)
    }

    private fun validateSourcePermissions(state: UnlockExecutionState): UnlockExecutionPreflightResult {
        val missingSourceTaskIds =
            state.tasks
                .filter { task -> task.status is UnlockStatus.Queued }
                .filterNot { task -> uriPermissionManager.hasPersistedReadPermission(Uri.parse(task.source.uriString)) }
                .mapTo(linkedSetOf()) { task -> task.id }

        return UnlockExecutionPreflightResult(
            missingSourceTaskIds = missingSourceTaskIds,
        )
    }

    companion object {
        const val INVALID_OUTPUT_DIRECTORY_MESSAGE =
            "已保存的输出目录访问权限失效，请重新选择输出目录后再试。"
        const val MISSING_SOURCE_ACCESS_MESSAGE =
            "文件读取权限已失效，请重新选择该文件后再运行。"
    }
}
