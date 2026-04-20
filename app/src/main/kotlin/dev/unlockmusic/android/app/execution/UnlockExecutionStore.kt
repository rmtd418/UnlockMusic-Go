package dev.unlockmusic.android.app.execution

import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockSummary
import dev.unlockmusic.android.domain.model.UnlockTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UnlockExecutionState(
    val outputDirectoryUri: String? = null,
    val tasks: List<UnlockTask> = emptyList(),
    val isExecuting: Boolean = false,
    val activeTaskId: String? = null,
    val lastMessage: String? = null,
    val cancelRequested: Boolean = false,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentTaskName: String? = null,
    val currentTaskProgressPercent: Int = 0,
) {
    val summary: UnlockSummary = UnlockSummary.from(tasks)
}

object UnlockExecutionStore {
    private val _state = MutableStateFlow(UnlockExecutionState())
    val state: StateFlow<UnlockExecutionState> = _state.asStateFlow()

    fun snapshot(): UnlockExecutionState = _state.value

    fun restore(state: UnlockExecutionState) {
        _state.value = state
    }

    fun updateOutputDirectory(uriString: String?) {
        _state.update { state ->
            state.copy(outputDirectoryUri = uriString)
        }
    }

    fun replaceQueue(
        tasks: List<UnlockTask>,
        outputDirectoryUri: String?,
    ) {
        val queuedCount = tasks.count { it.status is UnlockStatus.Queued }
        val failedCount = tasks.count { it.status is UnlockStatus.Failed }
        _state.update { state ->
            state.copy(
                outputDirectoryUri = outputDirectoryUri ?: state.outputDirectoryUri,
                tasks = tasks,
                isExecuting = false,
                activeTaskId = null,
                cancelRequested = false,
                processedCount = 0,
                totalCount = 0,
                currentTaskName = null,
                currentTaskProgressPercent = 0,
                lastMessage =
                    when {
                        queuedCount > 0 && failedCount > 0 ->
                            "已加入 $queuedCount 个可处理文件，$failedCount 个不支持的文件已标记为失败。"
                        queuedCount > 0 -> "已加入 $queuedCount 个文件。"
                        failedCount > 0 -> "$failedCount 个不支持的文件已标记为失败。"
                        else -> null
                    },
            )
        }
    }

    fun replaceTasks(
        tasks: List<UnlockTask>,
        message: String?,
    ) {
        _state.update { state ->
            state.copy(
                tasks = tasks,
                isExecuting = false,
                activeTaskId = null,
                cancelRequested = false,
                processedCount = 0,
                totalCount = 0,
                currentTaskName = null,
                currentTaskProgressPercent = 0,
                lastMessage = message,
            )
        }
    }

    fun clearQueue(keepOutputDirectory: Boolean = true) {
        _state.update { state ->
            state.copy(
                outputDirectoryUri = if (keepOutputDirectory) state.outputDirectoryUri else null,
                tasks = emptyList(),
                isExecuting = false,
                activeTaskId = null,
                cancelRequested = false,
                processedCount = 0,
                totalCount = 0,
                currentTaskName = null,
                currentTaskProgressPercent = 0,
                lastMessage = null,
            )
        }
    }

    fun markExecutionStarted(
        totalCount: Int,
        message: String,
    ) {
        _state.update { state ->
            state.copy(
                isExecuting = true,
                cancelRequested = false,
                processedCount = 0,
                totalCount = totalCount,
                currentTaskName = null,
                currentTaskProgressPercent = 0,
                lastMessage = message,
            )
        }
    }

    fun updateTaskProgress(
        taskId: String,
        taskName: String,
        progressPercent: Int,
        processedCount: Int,
        message: String,
    ) {
        _state.update { state ->
            state.copy(
                tasks =
                    state.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(status = UnlockStatus.Running(progressPercent))
                        } else {
                            task
                        }
                    },
                activeTaskId = taskId,
                processedCount = processedCount,
                currentTaskName = taskName,
                currentTaskProgressPercent = progressPercent,
                lastMessage = message,
            )
        }
    }

    fun updateTaskStatus(
        taskId: String,
        status: UnlockStatus,
        activeTaskId: String? = null,
        message: String? = null,
        processedCount: Int? = null,
        currentTaskName: String? = null,
        currentTaskProgressPercent: Int? = null,
    ) {
        _state.update { state ->
            state.copy(
                tasks =
                    state.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(status = status)
                        } else {
                            task
                        }
                    },
                activeTaskId = activeTaskId,
                processedCount = processedCount ?: state.processedCount,
                currentTaskName = currentTaskName ?: state.currentTaskName,
                currentTaskProgressPercent = currentTaskProgressPercent ?: state.currentTaskProgressPercent,
                lastMessage = message ?: state.lastMessage,
            )
        }
    }

    fun requestCancellation(message: String) {
        _state.update { state ->
            if (!state.isExecuting) {
                state
            } else {
                state.copy(
                    cancelRequested = true,
                    lastMessage = message,
                )
            }
        }
    }

    fun cancelPendingTasks(message: String) {
        _state.update { state ->
            state.copy(
                tasks =
                    state.tasks.map { task ->
                        if (task.status is UnlockStatus.Queued) {
                            task.copy(status = UnlockStatus.Canceled)
                        } else {
                            task
                        }
                    },
                isExecuting = false,
                activeTaskId = null,
                cancelRequested = false,
                currentTaskName = null,
                currentTaskProgressPercent = 0,
                lastMessage = message,
            )
        }
    }

    fun markExecutionFinished(message: String) {
        _state.update { state ->
            state.copy(
                isExecuting = false,
                activeTaskId = null,
                cancelRequested = false,
                currentTaskName = null,
                currentTaskProgressPercent = 0,
                lastMessage = message,
            )
        }
    }

    fun runnableTasks(): List<UnlockTask> {
        return snapshot().tasks.filter { task -> task.status is UnlockStatus.Queued }
    }
}
