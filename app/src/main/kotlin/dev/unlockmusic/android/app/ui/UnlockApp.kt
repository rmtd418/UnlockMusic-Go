package dev.unlockmusic.android.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.unlockmusic.android.core.metadata.DetectedFileType
import dev.unlockmusic.android.core.metadata.detectFileType
import dev.unlockmusic.android.data.document.DefaultOutputDirectoryManager
import dev.unlockmusic.android.data.document.DocumentDisplayNameResolver
import dev.unlockmusic.android.data.document.UriPermissionManager
import dev.unlockmusic.android.data.settings.LastSessionSettingsStore
import dev.unlockmusic.android.domain.model.UnlockSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UnlockApp() {
    val context = LocalContext.current
    val factory = remember(context) { UnlockViewModelFactory(context.applicationContext) }
    val viewModel: UnlockViewModel = viewModel(factory = factory)
    val defaultOutputDirectoryManager = remember(context) { DefaultOutputDirectoryManager(context) }
    val resolver = remember(context) { DocumentDisplayNameResolver(context.contentResolver) }
    val permissionManager = remember(context) { UriPermissionManager(context.contentResolver) }
    val sessionStore = remember(context) { LastSessionSettingsStore(context) }
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var showAboutPage by rememberSaveable { mutableStateOf(false) }

    val pickFiles = rememberLauncherForActivityResult(OpenMultipleDocuments()) { uris ->
        uris.forEach(permissionManager::persistReadPermission)
        val sources = uris.map { uri ->
            val displayName = resolver.resolve(uri)
            UnlockSource(
                uriString = uri.toString(),
                displayName = displayName,
                detectedFileType = detectFileType(displayName),
            )
        }
        viewModel.onSourcesSelected(sources)
    }

    val pickDirectory = rememberLauncherForActivityResult(OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            permissionManager.persistDirectoryPermission(uri)
            viewModel.onOutputDirectorySelected(uri.toString())
            coroutineScope.launch {
                sessionStore.saveOutputDirectoryUri(uri.toString())
            }
        }
    }

    LaunchedEffect(sessionStore, defaultOutputDirectoryManager) {
        if (uiState.outputDirectoryUri == null) {
            val initialOutputDirectory =
                sessionStore.loadOutputDirectoryUri()
                    ?: withContext(Dispatchers.IO) {
                        defaultOutputDirectoryManager.ensureDefaultOutputDirectoryUri()
                    }.also { sessionStore.saveOutputDirectoryUri(it) }
            viewModel.onOutputDirectorySelected(initialOutputDirectory)
        }
    }

    if (showAboutPage) {
        AboutScreen(
            onBack = { showAboutPage = false },
        )
    } else {
        UnlockScreen(
            uiState = uiState,
            onOpenAbout = { showAboutPage = true },
            onSelectFiles = { pickFiles.launch(arrayOf("*/*")) },
            onSelectOutput = { pickDirectory.launch(null) },
            onRunQueue = viewModel::runQueuedTasks,
            onCancelRun = viewModel::cancelExecution,
            onRetryUnfinished = viewModel::retryUnfinishedTasks,
            onClearQueue = viewModel::clearQueue,
            onRemoveListItem = viewModel::removeListItem,
            onClearSuccessfulItems = viewModel::clearSuccessfulItems,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UnlockScreen(
    uiState: UnlockUiState,
    onOpenAbout: () -> Unit,
    onSelectFiles: () -> Unit,
    onSelectOutput: () -> Unit,
    onRunQueue: () -> Unit,
    onCancelRun: () -> Unit,
    onRetryUnfinished: () -> Unit,
    onClearQueue: () -> Unit,
    onRemoveListItem: (UnlockListItem) -> Unit,
    onClearSuccessfulItems: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val transientMessage = transientSnackbarMessage(uiState)

    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = transientMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音乐解锁") },
                actions = {
                    TextButton(onClick = onOpenAbout) {
                        Text("关于")
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onSelectFiles,
                    enabled = !uiState.isExecuting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("选择文件")
                }

                Button(
                    onClick = onSelectOutput,
                    enabled = !uiState.isExecuting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("选择输出目录")
                }
            }

            uiState.outputDirectoryUri?.let { outputDirectoryUri ->
                Text(
                    text = "输出目录：${formatOutputDirectoryLabel(outputDirectoryUri)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onRunQueue,
                    enabled = uiState.queuedCount > 0 && uiState.outputDirectoryUri != null && !uiState.isExecuting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isExecuting) "运行中..." else "开始执行")
                }
            }

            if (uiState.isExecuting) {
                OutlinedButton(
                    onClick = onCancelRun,
                    enabled = !uiState.cancelRequested,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.cancelRequested) "已请求取消" else "取消执行")
                }
            }

            FileListSection(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                onRetryUnfinished = onRetryUnfinished,
                onClearQueue = onClearQueue,
                onClearSuccessfulItems = onClearSuccessfulItems,
                onRemoveListItem = onRemoveListItem,
            )
        }
    }
}

@Composable
private fun FileListSection(
    modifier: Modifier = Modifier,
    uiState: UnlockUiState,
    onRetryUnfinished: () -> Unit,
    onClearQueue: () -> Unit,
    onClearSuccessfulItems: () -> Unit,
    onRemoveListItem: (UnlockListItem) -> Unit,
) {
    val listTitle =
        if (uiState.visibleItems.isEmpty()) {
            "导入列表（0）"
        } else {
            "导入列表（" + uiState.visibleItems.size + " / " + uiState.overallProgressPercent + "%）"
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = listTitle,
            style = MaterialTheme.typography.titleLarge,
        )
        if (uiState.successfulCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onClearSuccessfulItems,
                    enabled = !uiState.isExecuting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("清空已成功项")
                }
            }
        }

        if (uiState.retryableCount > 0 || uiState.visibleItems.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.retryableCount > 0) {
                    OutlinedButton(
                        onClick = onRetryUnfinished,
                        enabled = !uiState.isExecuting,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("重试未完成项")
                    }
                }

                OutlinedButton(
                    onClick = onClearQueue,
                    enabled = !uiState.isExecuting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("全部清空")
                }
            }
        }

        if (uiState.visibleItems.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxWidth())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.visibleItems, key = UnlockListItem::key) { item ->
                    FileListItemCard(
                        item = item,
                        canRemove = item.isRemovable && !uiState.isExecuting,
                        onRemove = { onRemoveListItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListItemCard(
    item: UnlockListItem,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    val colors =
        when (item.state) {
            UnlockListItemState.Unsupported,
            UnlockListItemState.Failed,
            UnlockListItemState.Canceled ->
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            UnlockListItemState.Running ->
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            UnlockListItemState.Success ->
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            else -> CardDefaults.cardColors()
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(item.source.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("类型：${fileTypeLabel(item.source.detectedFileType)}")
                    Text("状态：${stateLabel(item.state)}")
                }

                OutlinedButton(
                    onClick = onRemove,
                    enabled = canRemove,
                ) {
                    Text("移除")
                }
            }

            item.detail?.let { detail ->
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("还没有导入文件。")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AboutScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName =
        remember(context) {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrDefault("1.0.0")
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于与说明") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                InfoCard(
                    title = "项目地址",
                    lines =
                        listOf(
                            "版本：$versionName",
                        ),
                    links =
                        listOf(
                            InfoLink(
                                label = "原始项目参考",
                                url = ORIGINAL_PROJECT_URL,
                            ),
                            InfoLink(
                                label = "当前 Android 版开源仓库",
                                url = ANDROID_PROJECT_URL,
                            ),
                        ),
                    onOpenLink = uriHandler::openUri,
                )
            }
            item {
                InfoCard(
                    title = "支持格式",
                    lines =
                        listOf(
                            "QMC 系列：qmc0、qmcflac、mflac、mgg 等",
                            "NCM",
                            "KGM / KGMA / VPR",
                        ),
                )
            }
            item {
                InfoCard(
                    title = "免责声明",
                    lines =
                        listOf(
                            "本项目仅供学习与研究用途。",
                            "请在遵守当地法律、平台协议和版权要求的前提下使用。",
                            "由使用本应用带来的风险和责任由使用者自行承担。",
                        ),
                )
            }
            item {
                InfoCard(
                    title = "隐私说明",
                    lines =
                        listOf(
                            "文件在本地设备上处理。",
                            "当前应用不会主动上传你的文件内容。",
                        ),
                )
            }
            item {
                InfoCard(
                    title = "开源许可",
                    lines =
                        listOf(
                            "应用本体会继续保持开源。",
                            "第三方库许可信息后续会整理到单独列表或页面中。",
                        ),
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    lines: List<String>,
    links: List<InfoLink> = emptyList(),
    onOpenLink: ((String) -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
            links.forEach { link ->
                TextButton(
                    onClick = { onOpenLink?.invoke(link.url) },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("${link.label}：${link.url}")
                }
            }
        }
    }
}

private data class InfoLink(
    val label: String,
    val url: String,
)

private fun fileTypeLabel(fileType: DetectedFileType): String {
    return when (fileType) {
        DetectedFileType.QMC -> "QMC"
        DetectedFileType.NCM -> "NCM"
        DetectedFileType.KGM -> "KGM / VPR"
        DetectedFileType.UNKNOWN -> "未识别"
    }
}

private fun stateLabel(state: UnlockListItemState): String {
    return when (state) {
        UnlockListItemState.NotQueued -> "未入队"
        UnlockListItemState.Unsupported -> "不支持"
        UnlockListItemState.Queued -> "排队中"
        UnlockListItemState.Running -> "执行中"
        UnlockListItemState.Success -> "成功"
        UnlockListItemState.Failed -> "失败"
        UnlockListItemState.Canceled -> "已取消"
    }
}

private fun formatOutputDirectoryLabel(outputDirectoryUri: String?): String {
    if (outputDirectoryUri == null) return "未选择"

    val uri = Uri.parse(outputDirectoryUri)
    return when (uri.scheme) {
        "file" -> uri.path ?: outputDirectoryUri
        else -> outputDirectoryUri
    }
}

private fun queueActionHint(uiState: UnlockUiState): String? {
    if (uiState.isExecuting || uiState.visibleItems.isEmpty()) {
        return null
    }

    return when {
        uiState.outputDirectoryUri == null ->
            "还没有输出目录。默认会先使用应用自动创建的 UnlockMusicOutput，也可以手动改成你自己的目录。"
        else -> null
    }
}

private fun transientSnackbarMessage(uiState: UnlockUiState): String? {
    return when {
        uiState.isExecuting && uiState.cancelRequested ->
            "已请求取消。当前文件处理完成后，剩余排队文件会被标记为已取消。"
        !uiState.isExecuting && uiState.executionMessage != null ->
            uiState.executionMessage
        else -> queueActionHint(uiState)
    }
}

private const val ORIGINAL_PROJECT_URL = "https://git.unlock-music.dev/um/web"
private const val ANDROID_PROJECT_URL = "https://github.com/rmtd418/UnlockMusic-Go"
