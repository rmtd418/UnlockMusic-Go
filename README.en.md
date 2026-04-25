# Unlock Music Android

[中文](README.md) | English

Unlock Music Android is an Android-native local music file processing app. It brings part of the original `unlock-music` experience to Android with Kotlin, Jetpack Compose, Android Storage Access Framework, and foreground-service based batch execution.

This project is currently in alpha / MVP status. It is intended for testing, research, and personal interoperability use cases. Please use it only with files you are legally allowed to access and process.

## Relationship With The Original Project

This project is an Android-native adaptation inspired by the original `unlock-music` project and its format research.

- Original project: https://git.unlock-music.dev/um/web
- Historical GitHub repository: https://github.com/unlock-music/unlock-music
- This project is not an official Android client of the original `unlock-music` project.
- The original project remains copyrighted by its original authors and contributors. Android-specific implementation work in this repository is copyrighted by this project's authors and contributors.

See [NOTICE.md](NOTICE.md) for attribution details.

## Features

- Android-native UI built with Jetpack Compose.
- File and output-directory selection via Android Storage Access Framework.
- Batch queue with visible task states, cancellation, retry, and cleanup actions.
- Foreground-service execution for long-running batches.
- DataStore-backed queue and settings persistence.
- File-based processing path to reduce memory pressure on large files.
- Default output directory: `Android/data/dev.unlockmusic.android/files/UnlockMusicOutput`.
- Local-only processing. The app does not actively upload file contents.

## Supported Formats

The Android version currently supports only the formats implemented and tested in this repository:

- QMC family, including `.qmc0`, `.qmcflac`, `.mflac`, `.mflac0`, `.mgg`, `.mgg1`, `.mggl`, `.tkm`, `.bkc*`, and related variants.
- NCM.
- KGM / KGMA / VPR.

The original Web project supports more formats. Formats not listed here should not be considered supported by this Android version.

## Known Limitations

- NCM metadata write-back is not implemented yet.
- KGM / VPR metadata write-back is not implemented yet.
- There is no queue history page yet.
- Some Compose instrumentation cases are skipped on API 36 emulator images because of upstream Espresso compatibility issues.
- This project does not provide, host, or distribute music files.

## Usage

1. Open the app.
2. Tap `Select files` / `选择文件` to import files.
3. Use the default `UnlockMusicOutput` directory or choose another output directory.
4. Start the queue.
5. Follow progress in the app and Android notification.
6. Retry failed or canceled tasks when needed.

Removing an item from the list only removes it from the current app queue. It does not delete the original source file.

## Build Requirements

- Android Studio or an equivalent Android SDK setup.
- JDK 21.
- Android SDK compileSdk 36.

## Build And Test

On Windows PowerShell:

```powershell
.\gradlew.bat :core:test :domain:test :app:assembleDebug --no-daemon
```

Compile Android instrumentation tests:

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

Run instrumentation tests with a connected device or emulator:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

## Project Layout

```text
app     Android app shell, Compose UI, foreground service, execution state
core    File detection, decryptor contracts, and core format processing
data    Android document access, output writing, DataStore settings
domain  Batch task models and use cases
DOCS    Roadmap and UI/UX notes
```

## Privacy

- File processing happens locally on the device.
- The app does not actively upload file contents.
- File and directory access is granted through Android system pickers.
- You can revoke granted permissions through Android system settings or document-provider permission management.

## Disclaimer

This project is provided for learning, research, and personal interoperability scenarios. Only process files that you have the right to access and use. Users are responsible for complying with applicable laws, platform terms, and copyright requirements.

The software is provided under the MIT License on an "as is" basis, without warranty of any kind.

## Contributing

Issues and pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before contributing.

For security-sensitive reports, please read [SECURITY.md](SECURITY.md).

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).

The original `unlock-music` project is also licensed under the MIT License. This repository preserves upstream attribution in [NOTICE.md](NOTICE.md).

## Acknowledgements

Thanks to the original `unlock-music` authors and contributors for their format research, Web implementation, and community work.
