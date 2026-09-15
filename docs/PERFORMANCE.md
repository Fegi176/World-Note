# Performance observations — 0.2.0

Measured on an Android 14 x86_64 emulator (WHPX, software GPU), debug builds. These are observations on the Windows development host, not achieved physical-phone targets.

## Deterministic fixtures

`Fixtures.kt` contains UUID-stable synthetic records and mixed-script long prose. Typical: 2,000 lore (including 500 events), 5,000 relationships, **30 boards**, 400 placements on the dense board plus 290 on regional boards: **7,720 records**. Stress: 10,000 lore, 25,000 relationships, 30 boards, 2,000 placements on the dense board plus 290 regional placements: **37,320 records**. Every 100th lore entry has a long note. No network services or private lore are used.

```text
./gradlew :core:makeTestWorld -PfixtureScale=typical -PfixtureOutput=.tooling/typical.json
./gradlew :core:makeTestWorld -PfixtureScale=stress -PfixtureOutput=.tooling/stress.json
```

## Changes

Room writes batch field/reference/time/index deletion and insertion instead of issuing many suspending queries for each record. Selected-world invalidations are debounced and load changed records in bounded batches. Unchanged immutable records, including long prose, are reused across emissions. A process-local generation map detects normalized field/reference/time changes even when the authored revision number is unchanged. Draft writes and board viewport saves load just the necessary record/owned drafts. UUID checks reject collisions before changes can overwrite another world.

The earlier 7,401-record baseline took 58,757 ms to insert in its final v0.1.0 run, although earlier less-contended runs took 14,897–16,701 ms. The first v0.2.0 run of the same fixture took 5,876 ms. This comparison is indicative; host contention prevents a controlled speedup claim.

## Latest complete in-memory Room benchmark

| Fixture | Insert | Full snapshot | Indexed + substring search | Chronology ordering |
|---|---:|---:|---:|---:|
| Typical / 7,720 records | 5,565 ms | 441 ms | 30 ms | 13 ms |
| Stress / 37,320 records | 43,220 ms | 1,179 ms | 132 ms | 41 ms |

These timings exclude UI rendering, the 200 ms search debounce, and durable disk fsync. No fixed universal timing assertion is imposed on a contended emulator. Raw logs retain earlier runs rather than reporting only the fastest result.

## Native stress UI

`StressUiInstrumentedTest` inserts a separately remapped stress world into the actual app database, opens the dense board, injects four two-finger gestures, captures gfxinfo/memory, then opens Outline. Exact results and limitations are recorded in TEST_REPORT.md and the raw `artifacts/performance-api34.log` capture.

The final on-disk stress run measured generation + insertion **44,309 ms**, selected-world + board opening **2,638 ms**, and completed four pointer-injected pinches plus Outline navigation. Total process PSS was **271,300 KB**, including instrumentation/native memory. The 192 MB managed heap did not exhaust after incremental observation was introduced.

The pointer test uses the Compose test clock. Its gfxinfo sample contained only **8 rendered frames**, all classified janky (95th percentile 300 ms). This small synthetic sample does not establish sustained interactive FPS, and is not presented as meeting a 60 Hz target. Software GPU/emulator/instrumentation overhead and the fixture held by the test affect these measurements. Raw unfavorable results are retained.

A complete logical world remains in the UI model, but unchanged record bodies are reused rather than reloaded on each viewport update. Paging, advanced layouts and sustained physical-phone frame qualification remain future work.
