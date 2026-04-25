# Contributing

Thanks for your interest in Unlock Music Android.

## Ground Rules

- Be respectful and constructive.
- Keep discussions focused on technical issues, Android behavior, tests, documentation, and user experience.
- Do not submit copyrighted music files, private keys, account data, or any other content you do not have the right to share.
- Do not request features whose primary purpose is abuse, unauthorized access, or copyright infringement.

## Development Setup

Requirements:

- Android Studio or an equivalent Android SDK setup.
- JDK 21.
- Android SDK compileSdk 36.

Useful commands:

```powershell
.\gradlew.bat :core:test :domain:test :app:assembleDebug --no-daemon
```

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

With a connected device or emulator:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

## Pull Request Guidelines

- Keep changes focused.
- Add or update tests when behavior changes.
- Update README or DOCS when user-visible behavior changes.
- Preserve upstream attribution and license notices.
- Avoid committing generated files such as `build/`, `.gradle/`, `.kotlin/`, APK outputs, IDE caches, or local SDK paths.

## Issue Guidelines

When reporting a bug, include:

- Android version and device/emulator model.
- App version or commit hash.
- Input format, such as QMC, NCM, KGM, or VPR.
- Expected behavior and actual behavior.
- Relevant logs if available.

Do not attach copyrighted music files to public issues. If a minimal synthetic sample can reproduce the issue, prefer that.

## Format Support Requests

Format support is accepted only when it can be implemented and tested responsibly. Please provide public documentation, synthetic fixtures, or legal test data.
