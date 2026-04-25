package dev.unlockmusic.android.app.ui

import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockSummary
import dev.unlockmusic.android.domain.model.UnlockTask
import kotlin.math.roundToInt

data class UnlockUiState(
    val selectedSources: List<UnlockSource> = emptyList(),
    val outputDirectoryUri: String? = null,
    val queuedTasks: List<UnlockTask> = emptyList(),
    val isExecuting: Boolean = false,
    val executionMessage: String? = null,
    val cancelRequested: Boolean = false,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentTaskName: String? = null,
    val currentTaskProgressPercent: Int = 0,
    val activeTaskId: String? = null,
) {
    val summary: UnlockSummary = UnlockSummary.from(queuedTasks)
    val visibleItems: List<UnlockListItem> = queuedTasks.map { task -> task.toListItem(activeTaskId = activeTaskId) }
    val supportedSelectedCount: Int =
        queuedTasks.count { it.source.detectedFileType != DetectedFileType.UNKNOWN }
    val unsupportedSelectedCount: Int =
        queuedTasks.count { it.source.detectedFileType == DetectedFileType.UNKNOWN }
    val unsupportedSelectedNames: List<String> =
        queuedTasks
            .filter { it.source.detectedFileType == DetectedFileType.UNKNOWN }
            .map { it.source.displayName }
    val queuedCount: Int = queuedTasks.count { it.status is UnlockStatus.Queued }
    val successfulCount: Int = queuedTasks.count { it.status is UnlockStatus.Success }
    val unsupportedItemCount: Int = visibleItems.count { it.state == UnlockListItemState.Unsupported }
    val retryableCount: Int =
        queuedTasks.count {
            it.source.detectedFileType != DetectedFileType.UNKNOWN &&
                (
                    it.status is UnlockStatus.Failed ||
                        it.status is UnlockStatus.Canceled
                )
        }
    val overallProgressPercent: Int =
        if (queuedTasks.isEmpty()) {
            0
        } else {
            val completedUnits =
                queuedTasks.sumOf { task ->
                    when (val taskStatus = task.status) {
                        UnlockStatus.Queued -> 0.0
                        is UnlockStatus.Running -> taskStatus.progressPercent.coerceIn(0, 100) / 100.0
                        UnlockStatus.Canceled,
                        is UnlockStatus.Failed,
                        is UnlockStatus.Success -> 1.0
                    }
                }
            ((completedUnits / queuedTasks.size) * 100).roundToInt().coerceIn(0, 100)
        }
}

data class UnlockListItem(
    val key: String,
    val taskId: String?,
    val source: UnlockSource,
    val state: UnlockListItemState,
    val detail: String? = null,
    val isRemovable: Boolean,
)

enum class UnlockListItemState {
    NotQueued,
    Unsupported,
    Queued,
    Running,
    Success,
    Failed,
    Canceled,
}

private fun UnlockTask.toListItem(activeTaskId: String?): UnlockListItem {
    if (source.detectedFileType == DetectedFileType.UNKNOWN) {
        val detail =
            when (val taskStatus = status) {
                is UnlockStatus.Failed -> taskStatus.message
                else -> "当前 Android 版本暂不支持此文件类型。"
            }
        return UnlockListItem(
            key = id,
            taskId = id,
            source = source,
            state = UnlockListItemState.Unsupported,
            detail = detail,
            isRemovable = true,
        )
    }

    return when (val taskStatus = status) {
        UnlockStatus.Canceled ->
            UnlockListItem(
                key = id,
                taskId = id,
                source = source,
                state = UnlockListItemState.Canceled,
                detail = "此文件已取消。",
                isRemovable = true,
            )
        is UnlockStatus.Failed ->
            UnlockListItem(
                key = id,
                taskId = id,
                source = source,
                state = UnlockListItemState.Failed,
                detail = taskStatus.message,
                isRemovable = true,
            )
        UnlockStatus.Queued ->
            UnlockListItem(
                key = id,
                taskId = id,
                source = source,
                state = UnlockListItemState.Queued,
                detail = "已加入队列，等待执行。",
                isRemovable = true,
            )
        is UnlockStatus.Running ->
            UnlockListItem(
                key = id,
                taskId = id,
                source = source,
                state = UnlockListItemState.Running,
                detail =
                    if (activeTaskId == id) {
                        "正在处理，当前进度 ${taskStatus.progressPercent}%。"
                    } else {
                        "正在处理，进度 ${taskStatus.progressPercent}%。"
                    },
                isRemovable = false,
            )
        is UnlockStatus.Success ->
            UnlockListItem(
                key = id,
                taskId = id,
                source = source,
                state = UnlockListItemState.Success,
                detail = "已输出：${taskStatus.outputName}",
                isRemovable = true,
            )
    }
}
