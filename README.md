# Unlock Music Android / 音乐解锁 Android

中文 | [English](README.en.md)

Unlock Music Android 是一个 Android 原生音乐文件本地处理工具，目标是将原始 `unlock-music` 项目的部分能力迁移到 Android 设备上。应用使用 Kotlin、Jetpack Compose、Storage Access Framework 和 Android 前台服务实现，支持批量导入、队列执行、任务恢复、取消、重试和本地输出。

本项目目前处于 alpha / MVP 阶段，适合测试、研究和自用场景。请在遵守所在地法律法规、平台服务条款和版权要求的前提下使用。

## 与原始项目的关系

本项目基于原始 `unlock-music` 项目的理念、格式研究成果和 MIT 协议允许的改造方向进行 Android 原生化开发。

- 原始项目官方地址：https://git.unlock-music.dev/um/web
- 原始 GitHub 地址曾为：https://github.com/unlock-music/unlock-music
- 本项目不是原始 `unlock-music` 项目的官方 Android 客户端。
- 原始项目版权归原作者及贡献者所有。本项目新增 Android 原生实现部分版权归本项目作者及贡献者所有。

详细归属说明见 [NOTICE.md](NOTICE.md)。

## 功能特性

- Android 原生界面，基于 Jetpack Compose。
- 通过 Android Storage Access Framework 选择输入文件和输出目录。
- 批量任务队列，支持任务状态展示、取消、重试和清理。
- 使用 Android 前台服务执行批量处理，避免长任务被轻易中断。
- DataStore 持久化队列和输出目录设置，支持应用重启后的任务恢复。
- 文件级处理路径，降低大文件处理时的内存峰值。
- 默认输出目录：`Android/data/dev.unlockmusic.android/files/UnlockMusicOutput`。
- 文件在本地设备处理，当前应用不会主动上传文件内容。

## 当前支持格式

Android 版本目前只声明支持已完成并经过测试覆盖的格式：

- QMC 系列：如 `.qmc0`、`.qmcflac`、`.mflac`、`.mflac0`、`.mgg`、`.mgg1`、`.mggl`、`.tkm`、`.bkc*` 等。
- NCM：网易云音乐 `.ncm`。
- KGM / KGMA / VPR：酷狗相关格式。

原始 Web 项目支持更多格式，但它们尚未全部迁移到本 Android 项目。README 中未列出的格式不应视为已支持。

## 当前限制

- NCM 元数据写回暂未实现。
- KGM / VPR 元数据写回暂未实现。
- 暂无历史记录页面；当前队列状态使用 DataStore 保存。
- Compose instrumentation 测试在 API 36 模拟器镜像上会因 Espresso 兼容性问题跳过对应用例。
- 本项目不会提供、托管或分发任何音乐文件。

## 使用说明

1. 打开应用。
2. 点击 `选择文件`，导入需要本地处理的文件。
3. 默认会输出到应用自动创建的 `UnlockMusicOutput` 目录，也可以点击 `选择输出目录` 手动切换。
4. 点击 `开始执行`。
5. 处理过程中可以在应用页面和系统通知中查看进度。
6. 失败或取消的任务可以重试；不支持的文件会保留在列表中并标记为不支持。

从列表中移除项目只会移除应用内的当前工作项，不会删除原始文件。

## 构建要求

- Android Studio 或等价 Android SDK 环境。
- JDK 21。
- Android SDK compileSdk 36。

## 构建与测试

在 Windows PowerShell 中：

```powershell
.\gradlew.bat :core:test :domain:test :app:assembleDebug --no-daemon
```

编译 Android instrumentation 测试：

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

连接设备或模拟器后运行 instrumentation 测试：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

## 项目结构

```text
app     Android 应用入口、Compose UI、前台服务和执行状态
core    文件类型识别、解密器接口和核心格式处理逻辑
data    Android 文件访问、输出写入、DataStore 设置持久化
domain  批量任务模型和用例
DOCS    路线图和 UI/UX 设计说明
```

## 隐私说明

- 文件处理在本地设备完成。
- 当前应用不会主动上传你的文件内容。
- 应用通过 Android 系统文件选择器获取你授权的文件或目录访问权限。
- 你可以在系统设置或文件选择器授权管理中撤销相关权限。

## 免责声明

本项目仅用于学习、研究与个人数据互操作场景。请仅处理你有权访问和使用的文件。使用者应自行确保其行为符合所在地法律法规、平台服务条款和版权要求。

本项目按 MIT 协议以“按原样”方式提供，不提供任何明示或暗示担保。由使用本项目产生的风险和责任由使用者自行承担。

## 贡献

欢迎提交 Issue 和 Pull Request。参与前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

如果你要报告安全问题或可能涉及敏感滥用的问题，请先阅读 [SECURITY.md](SECURITY.md)。

## 许可证

本项目使用 MIT License。详见 [LICENSE](LICENSE)。

原始 `unlock-music` 项目同样使用 MIT License。本仓库保留了上游版权声明和许可证要求，详见 [NOTICE.md](NOTICE.md)。

## 致谢

感谢原始 `unlock-music` 项目作者和贡献者对格式研究、Web 实现和社区维护所做的工作。
