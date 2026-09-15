# Build, test and install

## Prerequisites

- JDK **17**.
- Android SDK **platform 36**, **build-tools 35.0.0**, and **platform-tools**.
- Git; Python 3 for optional packaging/smoke scripts.

Set `JAVA_HOME` and `ANDROID_HOME`, or set `sdk.dir` in an untracked `local.properties`. Put platform-tools on PATH for adb.

```text
sdkmanager "platforms;android-36" "build-tools;35.0.0" "platform-tools"
```

The wrapper downloads Gradle 8.13. Initial setup needs network access; the installed app is offline. Plugin and dependency versions are pinned in the Gradle build files. Android 14/API 34 is the runtime target; compile SDK 36 is not the minimum phone version.

## Build

```powershell
git clone https://github.com/Fegi176/NodeNote.git
cd NodeNote
./gradlew.bat :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

On macOS/Linux use `./gradlew`. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The app unit-test task currently reports NO-SOURCE. Core JVM tests and Android instrumentation are the meaningful suites. Reports appear under `core/build/reports/tests/` and `app/build/reports/`.

Helpers: `scripts/verify.ps1` on Windows, `scripts/verify.sh` on Unix. The Windows environment helper prefers optional tools in ignored `.tooling/` when present, otherwise your configured JDK/SDK.

## Install on Android 14

```text
adb devices -l
adb -s YOUR_PHONE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s YOUR_PHONE_SERIAL shell am start -W -n app.nodenote.worldbuilder.debug/app.nodenote.worldbuilder.MainActivity
```

Unlock the phone and accept **Allow USB debugging** when required. An unauthorized device is not ready.

The debug package is separate from old `app.nodenote`. Updates require the same package and certificate. A new machine or GitHub runner normally creates its own debug key, so its APK may not update a previously installed build. Export a complete backup and resolve signing deliberately; never clear data or remove the writing app as an automatic fix.

Production signing is not configured. Private keys are not in this repository.

## Test on an isolated target

Boot a dedicated Android 14/API 34 emulator:

```powershell
$env:ANDROID_SERIAL = 'emulator-5554'
./gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Unix equivalent:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --console=plain
```

Do not point this task at a personal writing installation: connected tests manage/remove their target package. For intentionally isolated physical tests use the distinct QA package:

```powershell
$env:ANDROID_SERIAL = 'YOUR_TEST_DEVICE_SERIAL'
./gradlew.bat -PtestBuildType=qa :app:connectedQaAndroidTest --console=plain
```

It uses `app.nodenote.worldbuilder.qa`. Display and restart smoke scripts require dedicated emulator serials. Read their prerequisites before running them.

## Package a local delivery

After a successful build:

```text
python scripts/package.py
```

Outputs are ignored by Git:

- `artifacts/NodeNote-Worldbuilder-debug.apk`
- `artifacts/NodeNote-Worldbuilder-source.zip`
- `artifacts/SHA256SUMS`

Source packaging includes source/tests/schemas/wrapper/scripts/docs and excludes SDKs, caches, keys, local configuration and user databases. After packaging, `scripts/install-android14.ps1 -Serial YOUR_PHONE_SERIAL` checks Android 14 and updates/launches the app.

## GitHub Actions

[The workflow](../.github/workflows/android.yml) runs core tests, lint and assembly on pushes/PRs, validates the wrapper and uploads APK/report artifacts. Manual dispatch can enable a dedicated API 34 emulator run.

CI APKs use the runner's debug signing identity, which can differ from the downloadable release APK.

## Troubleshooting

| Symptom | Check |
|---|---|
| SDK not found | ANDROID_HOME or local.properties/sdk.dir; platform 36 installed |
| Java/Gradle error | JDK 17 and the repository wrapper |
| adb unauthorized | Unlock the phone and accept USB debugging |
| Update signature mismatch | Existing/new certificates; retain and export data |
| Windows classes.jar locked | Avoid concurrent builds; use the configured single-use daemon |
| Intermittent failure | Inspect actual reports/device state; retain failure evidence |
