# Testing and coverage

## Run the checks

Use the [build guide](BUILDING.md) for commands, toolchain setup, dedicated Android 14 emulator testing and the separate QA package. Never target a personal writing installation with connected debug tests.

| Tests | What they exercise |
|---|---|
| `core/src/test/` | Chronology, graph transforms, identity, archive bounds and malformed inputs |
| `RepositoryInstrumentedTest` / `MigrationInstrumentedTest` | Real Room/SQLite, writing, search, revisions and schema migration |
| `HardeningInstrumentedTest` | World isolation, stable references, independent story/knowledge data, image recovery and restore commit boundaries |
| `BoardMediaInstrumentedTest` | Board images, content controls, independent placements, archive contents and failed-insertion cleanup |
| `UiInstrumentedTest` / `DesignInstrumentedTest` | Native navigation, public-account views, themes, contrast, accessibility, pinch gestures and toolbar clipping |
| `WritingInstrumentedTest` / `PickerInstrumentedTest` | Unicode/IME/lifecycle/large text and actual Android DocumentsUI |
| `PerformanceInstrumentedTest` / `StressUiInstrumentedTest` | Synthetic typical/stress worlds, dense boards, memory and operation timings |

The standalone restart and display scripts exercise settled process recovery and screen-size/font changes on a dedicated emulator.

## Recorded local results

- **0.2.1:** four focused Android 14 methods passed: two board-media tests and two Design regressions. Native board Image picker launch/cancel also passed. Production copying used a synthetic file URI; the picker smoke did not select a provider image. Installed and launched on an Android 14 Samsung phone without clearing data.
- **0.2.0 baseline:** 42 core JVM tests and 29 Android 14 methods passed, followed by three affected UI regressions after final visual refinements. A settled restart and update-in-place check also passed.
- The full 29-method runtime suite was **not repeated for 0.2.1**. Core source was unchanged and its task was UP-TO-DATE. The app unit-test task is NO-SOURCE, not an extra passing suite.
- Final local 0.2.1 assembly/lint passed with **0 errors and 26 warnings**. Warnings were retained, not suppressed.

The local 0.2.1 APK tested and installed had SHA-256 `bd5a6974ac571a7e904d0dc925d7e0617f305828944954eb3a65b116eb3c17e7`. Debug signing keys differ between machines and CI; a new build need not have that checksum or update an existing installation.

## Reports and CI

Gradle writes reports to `core/build/reports/tests/`, `app/build/reports/` and `app/build/outputs/androidTest-results/`. Generated reports, device logs and delivery artifacts stay out of Git. [GitHub Actions](https://github.com/Fegi176/NodeNote/actions) uploads its own reports and APK; check the actual run result separately from local history.

## Remaining qualification

Manual spoken TalkBack review, broader physical-phone workflow/performance, arbitrary power-loss timing, actual full-volume behavior and every document-provider failure remain open. Passing synthetic tests does not prove every interaction or sustained phone frame rate. See [performance](PERFORMANCE.md) and [known limitations](KNOWN_LIMITATIONS.md).
