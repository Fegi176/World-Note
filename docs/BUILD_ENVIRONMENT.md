# Reproducible build environment

Verified host: Windows 11 x64, PowerShell; workspace initially empty. Downloaded JDK/SDK/Gradle into ignored `.tooling/`; no machine-wide SDK installation was required.

| Component | Pinned / observed version |
|---|---|
| JDK | Temurin 17.0.20.1+1 |
| Gradle wrapper | 8.13 |
| Android Gradle plugin | 8.11.1 |
| Kotlin / Compose compiler plugin | 2.1.20 |
| KSP | 2.1.20-2.0.1 |
| compile / target / min SDK | 36 / 36 / 26 |
| Build tools | 35.0.0 |
| Compose BOM | 2025.06.01 |
| Room | 2.7.2 |
| Activity / Lifecycle | 1.10.1 / 2.9.1 |
| DataStore | 1.1.7 |
| Coroutines / Serialization JSON | 1.10.2 / 1.8.1 |
| Exif / CommonMark | 1.4.1 / 0.24.0 |
| JUnit | 4.13.2 |
| SDK command-line tools | 19.0 |
| Android Emulator | 37.1.11, WHPX, x86_64 Google APIs images |

The real wrapper was generated using Gradle. Distribution SHA-256 is pinned in `gradle/wrapper/gradle-wrapper.properties`: `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`. Wrapper JAR SHA-256 was checked against the official Gradle checksum: `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`.

Use the commands in README. Dependency resolution needs network during setup; the installed app does not. The source archive does not include SDKs, caches, local paths, private keys or generated databases. APK byte-for-byte reproducibility across debug keystores is not claimed.

Windows builds use in-process Kotlin compilation and a single-use Gradle daemon (`org.gradle.daemon=false`). Reused compiler/build daemons intermittently held `classes.jar` open; stopping the task-owned daemon and rebuilding resolved this host issue. Avoid concurrent Gradle invocations against the same checkout.

CI in `.github/workflows/android.yml` pins official action commits, validates the wrapper, builds/tests/lints and optionally runs an API 34 emulator. Remote CI has not been executed. Release signing and same-package legacy upgrades need separately supplied matching credentials and are not configured.
