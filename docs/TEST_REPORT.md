# Board images update — 0.2.1 (4)

Android 14/API 34 emulator: **4 targeted instrumented methods passed in 31.59s** (BoardMediaInstrumentedTest 2, DesignInstrumentedTest 2). The exact application APK tested is SHA-256 bd5a6974ac571a7e904d0dc925d7e0617f305828944954eb3a65b116eb3c17e7.

The new tests exercise production image import, source deletion, native rendered-image pixel checks, Card content switches, per-placement independence, full archive inspection retaining image/record data, Activity recreation and rejected-insertion asset cleanup. Existing Design tests reran native pinch, toolbar pixel containment, contrast and accessibility assertions. A separate native ADB smoke opened Android DocumentsUI from the board Image button and cancelled back to the canvas. It did not choose a real provider image; production import was exercised using a synthetic file URI.

Build/assembly/lint passed (artifacts/board-media-final-build.log, 2m11s), lint 0 errors/26 warnings. One new warning suggests a Canvas KTX convenience extension; no warnings are suppressed. :core:test was invoked and UP-TO-DATE, as core source is unchanged. The previous full 29-method suite was not rerun for this focused change. The initial new JUnit test incorrectly inferred a Boolean return; after adding its required Unit return, test assembly passed (36s) and all four methods passed. Application code/APK did not change between those test runs.

Evidence: artifacts/board-media-api34-tests.log, board-image-picker-api34.log, board-media-signature.txt, board-media-badging.txt and screenshots/18-board-media-api34.png. The image capture deliberately uses a magenta synthetic fixture to assert rendered pixels. All runtime image/board tests were confined to the emulator. Physical installation details are in IMPLEMENTATION_STATUS.md.

## Subsequent 0.2.1 phone installation

Samsung SM-G990B2 / Android14: adb install -r succeeded, installed version 0.2.1 (4), cold launch Status: ok in 1,189ms, process running and activity foreground. No uninstall/data clear was used. Evidence: artifacts/v0.2.1-phone-install.log. This does not imply full physical UI or performance testing.

## Historical 0.2.0 verification

# Test report — 15 September 2026

Native Android test release **0.2.0 (3)**, package app.nodenote.worldbuilder.debug. Android 14/API 34 is the requested runtime baseline. Tests use a dedicated Google APIs x86_64 emulator, normally 1080×2400 at 420 dpi. No new later-version work was undertaken.

## Actual outcomes

| Check | Result | Evidence |
|---|---|---|
| Core JVM | 42 tests, zero failures/errors: CoreTest 21, ChronologyContractTest 12, ArchiveFailureTest 9 | artifacts/test-reports/v0.2.0/core/ |
| Full Android 14 suite | 29 methods, zero failures/errors/skips; Gradle success in 4m1s | artifacts/m6-final-incremental-api34.log; artifacts/test-reports/v0.2.0/api34/ |
| Final affected UI rerun | UiInstrumentedTest 1 + DesignInstrumentedTest 2; all 3 pass in 23.347s | artifacts/m6-final-ui-api34.log |
| Final assembly/lint | Successful in 1m18s; 0 errors, 25 warnings | artifacts/m6-final-clip-build.log; artifacts/test-reports/v0.2.0/lint/ |
| Update-in-place | 0.1.0 → 0.2.0 with adb install -r; logical database and original image hashes preserved | artifacts/m6-upgrade-api34.log; artifacts/upgrade-api34.json |
| Settled process restart | New PIDs; open Epoch Codex restored, then Library/expanded filters/Character filter restored on a second restart | artifacts/m6-process-restart-api34.log |
| Native display matrix | PASS: 360dp/200% text Library, Timeline, Search; landscape board/outline at normal density. PNG dimensions asserted as 1080×2400 and 2400×1080 | artifacts/m6-ui-matrix-verified-api34.log |
| Signature | SDK verifies v2, Android Debug RSA-2048 | artifacts/m6-apk-signature.txt |
| Physical phone / remote CI | Current version not exercised; only emulator in final ADB inventory; remote CI not run | artifacts/m6-final-adb-devices.txt |

The full 29-method suite, upgrade and restart checks exercised APK SHA-256 **9aa95c877f473b6d237d45107d4f02839d34af9d4dd9f98cdde47c526b3388b2**. Subsequent application changes were limited to large-font navigation/creation layout and clipping board/timeline drawing areas. The **delivered APK** is SHA-256 **0557b3a638f9044f314212bd67bb9fd67ab2ec64cea534c852db1eef1d703b75**; its build/lint, all 3 affected UI methods, toolbar pixel assertion and final display matrix were rerun. The whole 29-method suite was not repeated after those visual-only refinements.

Core tests were also invoked with the adaptive-layout build (artifacts/m6-adaptive-final-build.log). :app:testDebugUnitTest is NO-SOURCE, not another suite. Lint warnings remain visible and were not suppressed.

## Full runtime suite: 29 methods

- Repository 7: long Unicode, shared placement identity, search/world isolation, revisions/protection/trash, draft atomicity, retention and complete archive roundtrip.
- Migration 1: actual prefilled version-1 SQLite upgrade, relationships, Unicode and search retained.
- Hardening 13: world collision protection, duplicate import rejection, board deletion without lore deletion, equal-title identity, independent relationship prose, story/reveal date separation, no inferred knowledge, managed image/corrupt-preview recovery, metadata observation, broad search, incremental child-field observation, verified replacement safety archive and restore commit-boundary faults.
- Performance 2: typical and stress fixtures through real in-memory Room/SQLite.
- Stress UI 1: 37,320 records persisted on disk, a 2,000-placement board opened, four injected pinches and native outline.
- Design 2: principal text/background contrast >=4.5:1, Android Accessibility Test Framework on exercised controls, actual two-finger gestures preserving lore/placement coordinates and Activity recreation.
- Native navigation 1: sample/editor/Epoch Codex/timeline/reveals, account projection/privacy, validation and light theme.
- Writing 1: native long mixed-script input, save/background/recreation and 200% text.
- DocumentsUI 1: real system export/open/import-as-new/cancellation with a unique filename.

The final Design rerun additionally compares toolbar pixels before and after zoom, verifying that canvas drawing cannot cover the toolbar. Native screenshots led to two-row named navigation and a full-width creation action at large font sizes.

## Preservation and repaired failures

The upgrade script installed retained 0.1.0, seeded its demo, stopped the process, copied SQLite plus WAL and hashed originals, then updated to 0.2.0. After opening the Codex, 1 world, 220 records, 368 fields, 214 references, 28 time records and original asset hashes remained intact. Schema 2 is byte-identical. No app data clear was used.

Tests exposed simultaneous Room connections causing locks and whole-world reloads exhausting the stress heap. Production code now shares a database/writer and reuses unchanged records; viewport writes avoid loading the whole world. The full suite passed after these changes. Other repaired failures included atomic-file descriptor ownership, thumbnail recovery and picker synchronization. Earlier failing logs remain diagnostics.

The initial display script changed a setting without asserting actual orientation; screenshot review caught portrait captures mislabeled as landscape. The corrected script uses WindowManager rotation control and asserts PNG width/height before recording success.

## Performance and coverage limits

Final in-memory Room observations: typical 7,720 records, insert 5,565ms, snapshot 441ms, search 30ms, chronology 13ms; stress 37,320 records, insert 43,220ms, snapshot 1,179ms, search 132ms, chronology 41ms. Separate on-disk stress generation/insert took 44,309ms and world/board opening 2,638ms. Total PSS was 271,300KB including test/native overhead.

The software-emulator frame sample contains only 8 frames, all classified janky, p95 300ms. It does not establish sustained frame rate; passing gesture assertions do not override this limitation. See PERFORMANCE.md.

Fault tests cover four restore boundaries, output-stream failure, compressed members beyond 64MiB, truncation, traversal/hash/depth errors and invalid images. They do not prove every hardware power-loss timing, actual full-volume behavior, every provider failure or an exhaustive malicious archive corpus. Restart checks use settled saves. No manual spoken TalkBack or complete manual acceptance pass is claimed.

Historical 0.1.0 installation/launch succeeded on authorized Samsung SM-G990B2 / Android 14. Final 0.2.0 installation remains pending reconnection. Current tests were confined to the emulator. A distinct .qa variant was assembled for future isolated device tests; never run connected debug tests on the personal installation.

## Subsequent physical-phone update

After reconnection, the authorized Samsung SM-G990B2 / Android 14 received version 0.2.0 (3) successfully via adb install -r. No uninstall or data clear was used. Android returned Status: ok, LaunchState: COLD, TotalTime: 1500ms; the process was running and activity foreground. See artifacts/v0.2.0-phone-install.log (relative to the workspace root). This supersedes the earlier pending-installation statement. Full physical workflow, TalkBack and performance qualification remain open.
