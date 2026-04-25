package dev.unlockmusic.android.app.execution

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.unlockmusic.android.app.MainActivity
import dev.unlockmusic.android.app.R
import dev.unlockmusic.android.core.decrypt.DefaultFileDispatchDecryptor
import dev.unlockmusic.android.core.decrypt.FileDispatchDecryptor
import dev.unlockmusic.android.data.document.DocumentBytesReader
import dev.unlockmusic.android.data.document.TreeDocumentWriter
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockSummary
import dev.unlockmusic.android.domain.model.UnlockTask
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class UnlockForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileDispatchDecryptor: FileDispatchDecryptor by lazy { DefaultFileDispatchDecryptor.create() }
    private val documentBytesReader by lazy { DocumentBytesReader(contentResolver) }
    private val treeDocumentWriter by lazy { TreeDocumentWriter(this, contentResolver) }
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    @Volatile
    private var isProcessing = false

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_CANCEL_QUEUE -> {
                handleCancelRequest()
                return START_NOT_STICKY
            }
            ACTION_RUN_QUEUE -> Unit
            else -> return START_NOT_STICKY
        }

        if (isProcessing) return START_NOT_STICKY

        val outputDirectoryUri = UnlockExecutionStore.snapshot().outputDirectoryUri
        val tasksToRun = UnlockExecutionStore.runnableTasks()
        if (outputDirectoryUri == null || tasksToRun.isEmpty()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        isProcessing = true
        val initialMessage = "正在准备后台处理 ${tasksToRun.size} 个文件。"
        startForegroundWith(initialMessage)
        UnlockExecutionStore.markExecutionStarted(
            totalCount = tasksToRun.size,
            message = initialMessage,
        )

        serviceScope.launch {
            runBatch(
                startId = startId,
                outputDirectoryUri = outputDirectoryUri,
                tasksToRun = tasksToRun,
            )
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun runBatch(
        startId: Int,
        outputDirectoryUri: String,
        tasksToRun: List<UnlockTask>,
    ) {
        try {
            val total = tasksToRun.size

            for ((index, task) in tasksToRun.withIndex()) {
                if (UnlockExecutionStore.snapshot().cancelRequested) {
                    finishCanceledBatch()
                    return
                }

                val taskName = task.source.displayName
                updateStage(
                    task = task,
                    taskName = taskName,
                    progressPercent = 5,
                    processedCount = index,
                    message = "开始处理 ${index + 1}/$total：$taskName",
                )

                runCatching {
                    updateStage(
                        task = task,
                        taskName = taskName,
                        progressPercent = 15,
                        processedCount = index,
                        message = "正在读取 ${index + 1}/$total：$taskName",
                    )
                    val inputFile = temporaryFile("unlock-input-", task.id)
                    val outputFile = temporaryFile("unlock-output-", task.id)

                    try {
                        documentBytesReader.copyToFile(Uri.parse(task.source.uriString), inputFile)

                        updateStage(
                            task = task,
                            taskName = taskName,
                            progressPercent = 55,
                            processedCount = index,
                            message = "正在解密 ${index + 1}/$total：$taskName",
                        )
                        val result =
                            fileDispatchDecryptor.decryptToFile(
                                filename = task.source.displayName,
                                inputFile = inputFile,
                                outputFile = outputFile,
                            )

                        updateStage(
                            task = task,
                            taskName = taskName,
                            progressPercent = 85,
                            processedCount = index,
                            message = "正在写出 ${index + 1}/$total：$taskName",
                        )
                        treeDocumentWriter.writeFile(
                            treeUri = Uri.parse(outputDirectoryUri),
                            displayName = result.suggestedFileName,
                            sourceFile = result.outputFile,
                            mimeType = mimeTypeFor(result.outputExtension),
                        )
                    } finally {
                        inputFile.delete()
                        outputFile.delete()
                    }
                }.onSuccess { outputName ->
                    UnlockExecutionStore.updateTaskStatus(
                        taskId = task.id,
                        status = UnlockStatus.Success(outputName),
                        activeTaskId = task.id,
                        message = "已完成 ${index + 1}/$total：$taskName",
                        processedCount = index + 1,
                        currentTaskName = taskName,
                        currentTaskProgressPercent = 100,
                    )
                }.onFailure { error ->
                    UnlockExecutionStore.updateTaskStatus(
                        taskId = task.id,
                        status = UnlockStatus.Failed(error.message ?: "未知错误"),
                        activeTaskId = task.id,
                        message = "处理失败 ${index + 1}/$total：$taskName",
                        processedCount = index + 1,
                        currentTaskName = taskName,
                        currentTaskProgressPercent = 100,
                    )
                }

                val currentState = UnlockExecutionStore.snapshot()
                updateNotification(
                    contentText = currentState.lastMessage ?: "已处理 ${index + 1}/$total 个文件。",
                    state = currentState,
                    ongoing = true,
                )

                if (currentState.cancelRequested) {
                    finishCanceledBatch()
                    return
                }
            }

            val finalState = UnlockExecutionStore.snapshot()
            val finalMessage = finalMessageFor(finalState.summary)
            UnlockExecutionStore.markExecutionFinished(finalMessage)
            updateNotification(
                contentText = finalMessage,
                state = UnlockExecutionStore.snapshot(),
                ongoing = false,
            )
        } catch (error: Throwable) {
            val failureMessage = "批量处理已停止：${error.message ?: "未知错误"}"
            UnlockExecutionStore.markExecutionFinished(failureMessage)
            updateNotification(
                contentText = failureMessage,
                state = UnlockExecutionStore.snapshot(),
                ongoing = false,
            )
        } finally {
            isProcessing = false
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf(startId)
        }
    }

    private fun startForegroundWith(contentText: String) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(
                contentText = contentText,
                state = UnlockExecutionStore.snapshot(),
                ongoing = true,
            ),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun handleCancelRequest() {
        if (!isProcessing) return

        val message = "已请求取消。当前文件会先处理完成，剩余文件随后跳过。"
        UnlockExecutionStore.requestCancellation(message)
        updateNotification(
            contentText = message,
            state = UnlockExecutionStore.snapshot(),
            ongoing = true,
        )
    }

    private fun updateStage(
        task: UnlockTask,
        taskName: String,
        progressPercent: Int,
        processedCount: Int,
        message: String,
    ) {
        UnlockExecutionStore.updateTaskProgress(
            taskId = task.id,
            taskName = taskName,
            progressPercent = progressPercent,
            processedCount = processedCount,
            message = message,
        )
        updateNotification(
            contentText = message,
            state = UnlockExecutionStore.snapshot(),
            ongoing = true,
        )
    }

    private fun finishCanceledBatch() {
        val state = UnlockExecutionStore.snapshot()
        val skippedCount = state.tasks.count { it.status is UnlockStatus.Queued }
        val canceledMessage =
            getString(
                R.string.notification_canceled_format,
                state.processedCount,
                state.totalCount,
                skippedCount,
            )
        UnlockExecutionStore.cancelPendingTasks(canceledMessage)
        updateNotification(
            contentText = canceledMessage,
            state = UnlockExecutionStore.snapshot(),
            ongoing = false,
        )
    }

    private fun updateNotification(
        contentText: String,
        state: UnlockExecutionState,
        ongoing: Boolean,
    ) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(
                contentText = contentText,
                state = state,
                ongoing = ongoing,
            ),
        )
    }

    private fun buildNotification(
        contentText: String,
        state: UnlockExecutionState,
        ongoing: Boolean,
    ): Notification {
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val cancelIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(this, UnlockForegroundService::class.java).apply {
                    action = ACTION_CANCEL_QUEUE
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val title =
            if (state.cancelRequested && ongoing) {
                getString(R.string.notification_title_cancel_requested)
            } else if (ongoing) {
                getString(R.string.notification_title_running)
            } else {
                getString(R.string.notification_title_finished)
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    listOf(contentText, buildStatusLines(state, ongoing))
                        .filter { it.isNotBlank() }
                        .joinToString("\n"),
                ),
            )
            .setContentIntent(contentIntent)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(
                state.totalCount.coerceAtLeast(0),
                state.processedCount.coerceAtLeast(0),
                state.totalCount <= 0,
            )
            .apply {
                if (ongoing) {
                    addAction(
                        0,
                        getString(R.string.notification_cancel_action),
                        cancelIntent,
                    )
                }
            }
            .build()
    }

    private fun buildStatusLines(
        state: UnlockExecutionState,
        ongoing: Boolean,
    ): String {
        val summaryLine =
            if (state.totalCount > 0) {
                getString(
                    R.string.notification_progress_format,
                    state.processedCount,
                    state.totalCount,
                    state.summary.success,
                    state.summary.failed,
                    state.summary.canceled,
                )
            } else {
                getString(
                    R.string.notification_result_format,
                    state.summary.success,
                    state.summary.failed,
                    state.summary.canceled,
                )
            }

        val currentLine =
            if (ongoing && state.currentTaskName != null) {
                getString(
                    R.string.notification_current_task_format,
                    state.currentTaskName,
                    state.currentTaskProgressPercent,
                )
            } else {
                ""
            }

        return listOf(summaryLine, currentLine)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun ensureNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
        notificationManager.createNotificationChannel(channel)
    }

    private fun finalMessageFor(summary: UnlockSummary): String {
        return if (summary.failed == 0 && summary.canceled == 0) {
            "批量解锁完成：成功解锁 ${summary.success} 个文件。"
        } else {
            "批量解锁完成：成功 ${summary.success}，失败 ${summary.failed}，取消 ${summary.canceled}。"
        }
    }

    private fun mimeTypeFor(extension: String): String {
        return when (extension) {
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/x-wav"
            "wma" -> "audio/x-ms-wma"
            else -> "application/octet-stream"
        }
    }

    private fun temporaryFile(
        prefix: String,
        taskId: String,
    ): File {
        val directory = File(cacheDir, "unlock-execution").apply {
            mkdirs()
        }
        return File.createTempFile(prefix + taskId.take(8), ".tmp", directory)
    }

    companion object {
        const val ACTION_RUN_QUEUE = "dev.unlockmusic.android.app.action.RUN_QUEUE"
        const val ACTION_CANCEL_QUEUE = "dev.unlockmusic.android.app.action.CANCEL_QUEUE"

        private const val CHANNEL_ID = "unlock_music_execution"
        private const val NOTIFICATION_ID = 1001
    }
}
