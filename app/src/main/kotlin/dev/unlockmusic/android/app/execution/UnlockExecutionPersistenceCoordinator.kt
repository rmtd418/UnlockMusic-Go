package dev.unlockmusic.android.app.execution

import dev.unlockmusic.android.data.settings.UnlockExecutionSnapshot
import dev.unlockmusic.android.data.settings.UnlockExecutionSnapshotStore
import dev.unlockmusic.android.domain.usecase.RecoverInterruptedUnlockTasksUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UnlockExecutionPersistenceCoordinator(
    private val snapshotStore: UnlockExecutionSnapshotStore,
    private val recoverInterruptedUnlockTasks: RecoverInterruptedUnlockTasksUseCase = RecoverInterruptedUnlockTasksUseCase(),
) {
    @Volatile
    private var initialized = false

    fun initialize(scope: CoroutineScope) {
        if (initialized) return
        initialized = true

        snapshotStore.load()
            ?.let(::recoverSnapshot)
            ?.let(UnlockExecutionStore::restore)

        scope.launch {
            UnlockExecutionStore.state
                .collect { state ->
                    snapshotStore.save(
                        UnlockExecutionSnapshot(
                            outputDirectoryUri = state.outputDirectoryUri,
                            tasks = state.tasks,
                            isExecuting = state.isExecuting,
                            activeTaskId = state.activeTaskId,
                            lastMessage = state.lastMessage,
                            cancelRequested = state.cancelRequested,
                            processedCount = state.processedCount,
                            totalCount = state.totalCount,
                            currentTaskName = state.currentTaskName,
                            currentTaskProgressPercent = state.currentTaskProgressPercent,
                        ),
                    )
                }
        }
    }

    private fun recoverSnapshot(snapshot: UnlockExecutionSnapshot): UnlockExecutionState {
        if (!snapshot.isExecuting && !snapshot.cancelRequested) {
            return UnlockExecutionState(
                outputDirectoryUri = snapshot.outputDirectoryUri,
                tasks = snapshot.tasks,
                isExecuting = false,
                activeTaskId = snapshot.activeTaskId,
                lastMessage = snapshot.lastMessage,
                cancelRequested = false,
                processedCount = snapshot.processedCount,
                totalCount = snapshot.totalCount,
                currentTaskName = snapshot.currentTaskName,
                currentTaskProgressPercent = snapshot.currentTaskProgressPercent,
            )
        }

        return UnlockExecutionState(
            outputDirectoryUri = snapshot.outputDirectoryUri,
            tasks = recoverInterruptedUnlockTasks(snapshot.tasks),
            isExecuting = false,
            activeTaskId = null,
            lastMessage = "应用重启后已恢复上次队列，可重试未完成的文件继续处理。",
            cancelRequested = false,
            processedCount = 0,
            totalCount = 0,
            currentTaskName = null,
            currentTaskProgressPercent = 0,
        )
    }
}
