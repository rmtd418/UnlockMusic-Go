package dev.unlockmusic.android.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.unlockmusic.android.app.execution.UnlockExecutionPreflightChecker
import dev.unlockmusic.android.app.execution.UnlockExecutionStore
import dev.unlockmusic.android.app.execution.UnlockForegroundServiceStarter
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.data.settings.LastSessionSettingsStore
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.usecase.EnqueueUnlockTasksUseCase
import dev.unlockmusic.android.domain.usecase.RetryUnfinishedUnlockTasksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UnlockViewModel(
    private val serviceStarter: UnlockForegroundServiceStarter,
    private val preflightChecker: UnlockExecutionPreflightChecker,
    private val lastSessionSettingsStore: LastSessionSettingsStore,
    private val enqueueUnlockTasks: EnqueueUnlockTasksUseCase = EnqueueUnlockTasksUseCase(),
    private val retryUnfinishedUnlockTasks: RetryUnfinishedUnlockTasksUseCase = RetryUnfinishedUnlockTasksUseCase(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            UnlockExecutionStore.state.collect { executionState ->
                _uiState.update { state ->
                    state.copy(
                        selectedSources = executionState.tasks.map { it.source }.distinctBy { it.uriString },
                        outputDirectoryUri = executionState.outputDirectoryUri ?: state.outputDirectoryUri,
                        queuedTasks = executionState.tasks,
                        isExecuting = executionState.isExecuting,
                        executionMessage = executionState.lastMessage,
                        cancelRequested = executionState.cancelRequested,
                        processedCount = executionState.processedCount,
                        totalCount = executionState.totalCount,
                        currentTaskName = executionState.currentTaskName,
                        currentTaskProgressPercent = executionState.currentTaskProgressPercent,
                        activeTaskId = executionState.activeTaskId,
                    )
                }
            }
        }
    }

    fun onSourcesSelected(sources: List<UnlockSource>) {
        val existingTasks = UnlockExecutionStore.snapshot().tasks
        val existingUris = existingTasks.map { it.source.uriString }.toSet()
        val deduplicatedNewSources =
            sources.filter { incoming ->
                incoming.uriString !in existingUris
            }
        val supportedCount =
            deduplicatedNewSources.count { source ->
                source.detectedFileType != DetectedFileType.UNKNOWN
            }
        val duplicateCount = sources.size - deduplicatedNewSources.size
        val unsupportedCount = deduplicatedNewSources.size - supportedCount

        if (deduplicatedNewSources.isEmpty()) {
            UnlockExecutionStore.replaceTasks(
                tasks = existingTasks,
                message =
                    when {
                        duplicateCount > 0 ->
                            "所选文件都已在列表中，已跳过重复导入。"
                        else -> null
                    },
            )
            return
        }

        val newTasks = enqueueUnlockTasks(deduplicatedNewSources)
        UnlockExecutionStore.replaceTasks(
            tasks = existingTasks + newTasks,
            message =
                when {
                    unsupportedCount > 0 && duplicateCount > 0 ->
                        "已导入 ${deduplicatedNewSources.size} 个文件；$unsupportedCount 个当前不支持，已标记为不支持；另跳过 $duplicateCount 个重复文件。"
                    unsupportedCount > 0 ->
                        "已导入 ${deduplicatedNewSources.size} 个文件；$unsupportedCount 个当前不支持，已标记为不支持。"
                    duplicateCount > 0 ->
                        "已导入 ${deduplicatedNewSources.size} 个文件，跳过 $duplicateCount 个重复文件。"
                    else -> "已导入 ${deduplicatedNewSources.size} 个文件。"
                },
        )
    }

    fun onOutputDirectorySelected(uriString: String?) {
        UnlockExecutionStore.updateOutputDirectory(uriString)
        _uiState.update { state ->
            state.copy(outputDirectoryUri = uriString)
        }
    }

    fun clearQueue() {
        _uiState.update { state ->
            state.copy(
                selectedSources = emptyList(),
                executionMessage = null,
            )
        }
        UnlockExecutionStore.clearQueue()
    }

    fun removeListItem(item: UnlockListItem) {
        if (_uiState.value.isExecuting) return

        val updatedSources =
            _uiState.value.selectedSources.filterNot { source ->
                source.uriString == item.source.uriString
            }
        _uiState.update { state ->
            state.copy(
                selectedSources = updatedSources,
                executionMessage = "已从列表移除 ${item.source.displayName}。",
            )
        }

        val snapshot = UnlockExecutionStore.snapshot()
        if (item.taskId != null) {
            UnlockExecutionStore.replaceTasks(
                tasks = snapshot.tasks.filterNot { task -> task.id == item.taskId },
                message = "已从列表移除 ${item.source.displayName}。",
            )
        }
    }

    fun clearSuccessfulItems() {
        if (_uiState.value.isExecuting) return

        val successfulUris =
            UnlockExecutionStore.snapshot().tasks
                .filter { task -> task.status is UnlockStatus.Success }
                .map { task -> task.source.uriString }
                .toSet()
        if (successfulUris.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                selectedSources =
                    state.selectedSources.filterNot { source ->
                        source.uriString in successfulUris
                    },
                executionMessage = "已清空 ${successfulUris.size} 个成功项。",
            )
        }

        UnlockExecutionStore.replaceTasks(
            tasks =
                UnlockExecutionStore.snapshot().tasks.filterNot { task ->
                    task.source.uriString in successfulUris &&
                        task.status is UnlockStatus.Success
                },
            message = "已清空 ${successfulUris.size} 个成功项。",
        )
    }

    fun runQueuedTasks() {
        var state = UnlockExecutionStore.snapshot()
        if (state.outputDirectoryUri == null || state.isExecuting) return
        if (state.tasks.none { task -> task.status is UnlockStatus.Queued }) {
            return
        }

        val preflight = preflightChecker.validate(state)
        if (preflight.outputDirectoryError != null) {
            viewModelScope.launch {
                lastSessionSettingsStore.clear()
            }
            UnlockExecutionStore.updateOutputDirectory(null)
            UnlockExecutionStore.replaceTasks(
                tasks = state.tasks,
                message = preflight.outputDirectoryError,
            )
            return
        }

        if (preflight.missingSourceTaskIds.isNotEmpty()) {
            val updatedTasks =
                state.tasks.map { task ->
                    if (task.id in preflight.missingSourceTaskIds) {
                        task.copy(
                            status = UnlockStatus.Failed(UnlockExecutionPreflightChecker.MISSING_SOURCE_ACCESS_MESSAGE),
                        )
                    } else {
                        task
                    }
                }
            val remainingQueuedCount = updatedTasks.count { task -> task.status is UnlockStatus.Queued }
            UnlockExecutionStore.replaceTasks(
                tasks = updatedTasks,
                message =
                    if (remainingQueuedCount > 0) {
                        "${preflight.missingSourceTaskIds.size} 个文件的读取权限已失效，已标记为失败；其余待处理文件将继续。"
                    } else {
                        "${preflight.missingSourceTaskIds.size} 个待处理文件的读取权限已失效，请重新选择这些文件后再次加入队列。"
                    },
            )
            if (remainingQueuedCount == 0) return
            state = UnlockExecutionStore.snapshot()
        }

        if (state.tasks.none { task -> task.status is UnlockStatus.Queued }) return
        serviceStarter.start()
    }

    fun cancelExecution() {
        if (!UnlockExecutionStore.snapshot().isExecuting) return
        serviceStarter.cancel()
    }

    fun retryUnfinishedTasks() {
        val state = UnlockExecutionStore.snapshot()
        if (state.isExecuting) return

        val retryableCount =
            state.tasks.count { task ->
                task.source.detectedFileType != DetectedFileType.UNKNOWN &&
                    (
                        task.status is UnlockStatus.Failed ||
                            task.status is UnlockStatus.Canceled ||
                            task.status is UnlockStatus.Running
                    )
            }
        if (retryableCount == 0) return

        UnlockExecutionStore.replaceTasks(
            tasks = retryUnfinishedUnlockTasks(state.tasks),
            message = "已将 $retryableCount 个未完成文件重新加入队列。",
        )
    }
}
