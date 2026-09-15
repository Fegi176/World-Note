# Acceptance evidence

The specification's AT IDs are retained below. **Covered** means the named automated assertion passed; it does not imply every manual variation was exercised. **Partial** marks missing parts of the scenario. **Open** means no direct execution evidence. Latest run outcomes/device details are in TEST_REPORT.md; raw XML/HTML is delivered in `artifacts/test-reports/`.

Test abbreviations: C = `core/.../CoreTest`; T = `ChronologyContractTest`; A = `ArchiveFailureTest`; R = `RepositoryInstrumentedTest`; M = `MigrationInstrumentedTest`; U = `UiInstrumentedTest`; W = `WritingInstrumentedTest`; P = `PickerInstrumentedTest`; H = `HardeningInstrumentedTest`; D = `DesignInstrumentedTest`. See test source for actual assertions rather than treating this table as a replacement suite.

| ID | Outcome and evidence |
|---|---|
| AT-01 | Covered for settled writes: R/W exact Unicode persistence and background/reopen; native ADB force-stop/new-process script restores the open Codex and Library filter state. Arbitrary kill timing remains unproven |
| AT-02 | Covered at repository layer: R placement independence; demo shares character across three boards |
| AT-03 | Covered: R placement move changes only selected placement |
| AT-04 | Covered at repository layer: H deletes a complete board while shared lore/relationship notes remain identical; full UI deletion sequence remains unautomated |
| AT-05 | Covered for identity: H renames one of two equal titles and checks stable inline links; C tests map-scoped remapping; ambiguous-title picker uses identity |
| AT-06 | Partial: sticky conversion implementation preserves placement; exhaustive linked conversion test open |
| AT-07 | Covered for exercised flow: W rapid edit/Done/Home/reopen and exact stored value; arbitrary kill timing open |
| AT-08 | Partial: R failed/stale edit atomicity and durable draft; device storage failure UX injection open |
| AT-09 | Covered for tested gestures: C generated inverse/centroid math; D injects real two-finger zoom and verifies unchanged placements/lore plus recreation; arbitrary gesture interruptions remain partial |
| AT-10 | Partial: C undo/redo and repository independence; full group/filter/gesture sequence open |
| AT-11 | Covered for parallel persistence: H edits one relationship while parallel prose and existing board edges remain unchanged; exhaustive direction UI combinations remain partial |
| AT-12 | Partial: shared semantic identity and R roundtrip; two-board edit UI assertion open |
| AT-13 | Covered: R world-scoped alias/body search including relationship text; performance fixture includes relation markers |
| AT-14 | Partial: R >100k mixed-script exact save/search; W native long Unicode note/IME; 100k UI cursor navigation unmeasured |
| AT-15 | Covered in model: unknown-title-only period valid; native sample opens dedicated Codex |
| AT-16 | Partial: sample 90k/short periods and actual overview/scaled captures; physical readability audit open |
| AT-17 | Covered: C/T typed serialization/resolution, archive roundtrip |
| AT-18 | Covered: C approximateHasNoInventedBounds |
| AT-19 | Covered: T pointUncertaintyAndDurationAreDifferentOriginalModels; visual capture supplements semantic test |
| AT-20 | Covered: C offsetPropagatesBounds, T boundedOffsetNegativePropagation |
| AT-21 | Covered: C epochLocalYearAndUnknown, T unknownEpochAndLocalEventRemainUnresolved |
| AT-22 | Covered: T linkedBoundaryEventRecomputesWithoutCopyingDates |
| AT-23 | Covered: C cyclesAndOverflowAreInvalid, order-cycle test, demo integrity/hierarchy validator; UI remediation audit open |
| AT-24 | Covered: C orderCycleDetectedButGeneralGraphAllowed |
| AT-25 | Covered: T emptyAndImpossibleSpansRejected, approximateOverlapDoesNotAssertContradiction |
| AT-26 | Partial: C half-open/shared-boundary tests; explicit cross-period association data retained; full UI scenario open |
| AT-27 | Covered: T crossRegionOverlapsAreValid and C overlap lanes |
| AT-28 | Covered: T orderOnlyIsBetweenAnchorsInPresentationButHasNoNumericYear |
| AT-29 | Covered: C largeTimelinePreservesNarrowDifferences, exact64BitRoundTrip; overflow T |
| AT-30 | Partial: chronology has no wall-clock/timezone dependency by code inspection; phone settings mutation not run |
| AT-31 | Covered: T renamedOriginHasNoEffectOnHistory |
| AT-32 | Partial: sample explicit observatory states and R roundtrip; all state-selection UI combinations open |
| AT-33 | Partial: history UI only lists explicit states; no-present-fallback behavior inspected, no standalone device assertion |
| AT-34 | Partial: sample Chapter 7 reveal separate from event date; U reveal view and R roundtrip |
| AT-35 | Covered at repository layer: H reorders/renames a story unit while all reveal references and historical dates remain identical |
| AT-36 | Partial: separate truth/public/undecided fields and archive coverage; full edited scenario open |
| AT-37 | Covered for non-inference: H edits canonical claim prose/truth without changing any explicit character knowledge or public account output; ledger UI exists |
| AT-38 | Covered for projection: C sentinel test; U cross-tab account filter; canonical archive unchanged |
| AT-39 | Partial: planned/actual stage fields in editor/model; full scenario assertion open |
| AT-40 | Covered at repository layer: R lockedEntryTrashRevisionAndDuplication, retention; label UI implemented |
| AT-41 | Partial: R identity/trash/revision/duplication and archive; all referenced-media restore UI paths open |
| AT-42 | Covered against synthetic fixture: C legacyScopedIdsAndUnicode; actual old archive unavailable |
| AT-43 | Covered: C legacyScopedIdsAndUnicode and remapAllReferencesAndRevision |
| AT-44 | Covered for exercised inputs: C future legacy, A future manifest/depth/truncation, R atomicity; full fuzz space open |
| AT-45 | Covered: C archive/hash/remap, R complete assets/drafts archive, P real picker import as new world |
| AT-46 | Covered: R demo/full-record roundtrip and archiveWithAssetsDraftsAndSecretsRoundTrips |
| AT-47 | Covered for tested corpus: C/A traversal, case conflicts, symlink, hash/depth checks; A expands a compressed member beyond 64 MiB and verifies cleanup; H rejects invalid image and repairs a damaged preview |
| AT-48 | Partial: A simulated output failure; H injects failures before/after file promotion and SQL commit, then recovers journals; P picker cancellation. Actual physical storage exhaustion/power loss remains untested |
| AT-49 | Covered: H imports a real PNG, deletes its original picked source, checks managed hash/thumbnail, then corrupts and regenerates the disposable preview |
| AT-50 | Covered: C publicProjectionLeaksNoCanonicalToken verifies allowlisted export content; explicit author-written public spoilers remain possible |
| AT-51 | Covered baseline: C canvasRoundTrip; unsupported raw/warnings retained; group variants not exhaustively roundtripped |
| AT-52 | See BUILD_REPORT: conventional Gradle APK plus SDK signature/manifest/checksum inspection; no fabricated packaging |
| AT-53 | Covered for U/W/P exercised native flows, activity recreation/keyboard/Back/background; broader lifecycle matrix partial |
| AT-54 | Partial: P genuine DocumentsUI save/open/restore/cancel; provider-specific failures not injected |
| AT-55 | Not applicable: optional matching-signature legacy upgrade unavailable without source/certificate |
| AT-56 | Earlier authorized v0.1.0 debug install/launch succeeded on Samsung SM-G990B2 Android 14. The old app.nodenote package was never removed. Separate QA package supports isolated tests |
| AT-57 | Final package has no INTERNET permission; historical 0.1.0 airplane-mode evidence is retained separately. No packet capture was performed |
| AT-58 | Covered for automated checks: D contrast >=4.5:1 on main pairs and Android accessibility framework during native navigation; W 200% writing, U light theme; native size/landscape smoke reported separately. Manual spoken TalkBack review remains open |
| AT-59 | Covered: M real prefilled version-1 SQLite migration to v2, Unicode and FTS retained; no destructive fallback |
| AT-60 | Covered: R world isolation/duplication plus C full ID remapping including revision snapshots |

## Re-run

```text
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
ANDROID_SERIAL=<dedicated-emulator> ./gradlew :app:connectedDebugAndroidTest
```

PowerShell uses `$env:ANDROID_SERIAL` and `gradlew.bat`. Tests create synthetic worlds and picker exports in a dedicated emulator; do not point test automation at a personal app installation.
