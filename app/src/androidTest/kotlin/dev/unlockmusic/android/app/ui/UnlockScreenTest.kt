package dev.unlockmusic.android.app.ui

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.unlockmusic.android.app.theme.UnlockMusicTheme
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.domain.model.UnlockSource
import dev.unlockmusic.android.domain.model.UnlockStatus
import dev.unlockmusic.android.domain.model.UnlockTask
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("Compose instrumentation is blocked by Espresso on API 36 emulator images")
class UnlockScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun queuedFilesAndOutputDirectoryEnableExecution() {
        skipOnApi36()
        var runClicked = false
        composeRule.setContent {
            UnlockMusicTheme {
                UnlockScreen(
                    uiState =
                        UnlockUiState(
                            queuedTasks =
                                listOf(
                                    task(id = "one", displayName = "song-one.qmc0", status = UnlockStatus.Queued),
                                    task(id = "two", displayName = "song-two.ncm", status = UnlockStatus.Queued),
                                ),
                            outputDirectoryUri = "content://tree/output",
                        ),
                    onOpenAbout = {},
                    onSelectFiles = {},
                    onSelectOutput = {},
                    onRunQueue = { runClicked = true },
                    onCancelRun = {},
                    onRetryUnfinished = {},
                    onClearQueue = {},
                    onRemoveListItem = { _ -> },
                    onClearSuccessfulItems = {},
                )
            }
        }

        composeRule.onNodeWithText("输出目录：content://tree/output").assertIsDisplayed()
        composeRule.onNodeWithText("开始执行").assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertTrue(runClicked)
        }
    }

    @Test
    fun executingStateShowsProgressAndCancelAction() {
        skipOnApi36()
        var cancelClicked = false
        composeRule.setContent {
            UnlockMusicTheme {
                UnlockScreen(
                    uiState =
                        UnlockUiState(
                            selectedSources =
                                listOf(
                                    source("demo.kgm"),
                                    source("next.vpr"),
                                ),
                            queuedTasks =
                                listOf(
                                    task(
                                        id = "running",
                                        displayName = "demo.kgm",
                                        status = UnlockStatus.Running(progressPercent = 55),
                                    ),
                                    task(
                                        id = "queued",
                                        displayName = "next.vpr",
                                        status = UnlockStatus.Queued,
                                    ),
                                ),
                            outputDirectoryUri = "content://tree/output",
                            isExecuting = true,
                            processedCount = 0,
                            totalCount = 2,
                            currentTaskName = "demo.kgm",
                            currentTaskProgressPercent = 55,
                            activeTaskId = "running",
                        ),
                    onOpenAbout = {},
                    onSelectFiles = {},
                    onSelectOutput = {},
                    onRunQueue = {},
                    onCancelRun = { cancelClicked = true },
                    onRetryUnfinished = {},
                    onClearQueue = {},
                    onRemoveListItem = { _ -> },
                    onClearSuccessfulItems = {},
                )
            }
        }

        composeRule.onNodeWithText("取消执行").assertIsEnabled().performClick()
        composeRule.onNodeWithText("当前批次 0/2，正在处理 demo.kgm").assertIsDisplayed()
        composeRule.onNodeWithText("状态：执行中").assertIsDisplayed()

        composeRule.runOnIdle {
            assertTrue(cancelClicked)
        }
    }

    @Test
    fun retryableStateShowsRetryAction() {
        skipOnApi36()
        var retryClicked = false
        composeRule.setContent {
            UnlockMusicTheme {
                UnlockScreen(
                    uiState =
                        UnlockUiState(
                            selectedSources =
                                listOf(
                                    source("failed.ncm"),
                                    source("canceled.qmc0"),
                                ),
                            queuedTasks =
                                listOf(
                                    task(
                                        id = "failed",
                                        displayName = "failed.ncm",
                                        status = UnlockStatus.Failed("broken"),
                                    ),
                                    task(
                                        id = "canceled",
                                        displayName = "canceled.qmc0",
                                        status = UnlockStatus.Canceled,
                                    ),
                                ),
                            outputDirectoryUri = "content://tree/output",
                        ),
                    onOpenAbout = {},
                    onSelectFiles = {},
                    onSelectOutput = {},
                    onRunQueue = {},
                    onCancelRun = {},
                    onRetryUnfinished = { retryClicked = true },
                    onClearQueue = {},
                    onRemoveListItem = { _ -> },
                    onClearSuccessfulItems = {},
                )
            }
        }

        composeRule.onNodeWithText("重试未完成项").assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertTrue(retryClicked)
        }
    }

    @Test
    fun emptyStateKeepsExecutionDisabled() {
        skipOnApi36()
        composeRule.setContent {
            UnlockMusicTheme {
                UnlockScreen(
                    uiState = UnlockUiState(outputDirectoryUri = "content://tree/output"),
                    onOpenAbout = {},
                    onSelectFiles = {},
                    onSelectOutput = {},
                    onRunQueue = {},
                    onCancelRun = {},
                    onRetryUnfinished = {},
                    onClearQueue = {},
                    onRemoveListItem = { _ -> },
                    onClearSuccessfulItems = {},
                )
            }
        }

        composeRule.onNodeWithText("还没有导入文件。").assertIsDisplayed()
        composeRule.onNodeWithText("开始执行").assertIsNotEnabled()
    }

    private fun source(
        displayName: String,
        detectedFileType: DetectedFileType = DetectedFileType.QMC,
    ): UnlockSource {
        return UnlockSource(
            uriString = "content://$displayName",
            displayName = displayName,
            detectedFileType = detectedFileType,
        )
    }

    private fun task(
        id: String,
        displayName: String,
        status: UnlockStatus,
    ): UnlockTask {
        return UnlockTask(
            id = id,
            source = source(displayName),
            status = status,
            queuedAtEpochMillis = 1L,
        )
    }

    private fun skipOnApi36() {
        assumeTrue("Compose instrumentation is blocked by Espresso on API 36", Build.VERSION.SDK_INT < 36)
    }
}
