# Unlock Music Android

Android-native migration target for the web-based `unlock-music` project.

## Stack

- Kotlin
- Jetpack Compose
- Kotlin Coroutines + Flow
- Storage Access Framework
- Android Foreground Service for batch execution

## Modules

- `app`: Android UI shell
- `core`: file detection and decryptor contracts
- `domain`: batch-task models and use cases
- `data`: Android-specific document and settings helpers

## Current status

The Android migration now has a working MVP batch unlock flow:

- multi-module Gradle project
- Compose entry screen
- file and output-directory pickers
- persisted batch queue with foreground-service execution
- DataStore-backed queue/session persistence with legacy SharedPreferences migration
- QMC decrypt support in `core`
- NCM decrypt support in `core`
- KGM/VPR decrypt support in `core`
- task cancellation, retry, and progress reporting
- preflight validation for expired SAF permissions and invalid output-directory access
- auto-created default output folder at `Android/data/dev.unlockmusic.android/files/UnlockMusicOutput`, with manual directory override still available
- clearer unsupported-file warnings before files are queued
- provider-backed instrumentation for real SAF document I/O and foreground-service execution paths
- provider-backed `connectedDebugAndroidTest` validation on an emulator
- real sample-file validation for QMC `mflac` conversion on device-side storage

Current gaps:

- NCM metadata write-back is intentionally deferred
- KGM/VPR metadata write-back is intentionally deferred
- Room is intentionally deferred unless queue history grows beyond what DataStore can comfortably support
- Compose instrumentation on API 36 emulator images is currently skipped because Espresso is not yet compatible with that platform image

## Verification

- `.\gradlew.bat :core:test --no-daemon`
- `.\gradlew.bat :app:assembleDebug --no-daemon`
- `.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon`
