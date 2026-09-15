# Changelog

## 0.2.1 — 2026-09-15

- Import an image directly from a board as reusable lore with a managed original.
- Choose cover image, note preview, tags and status separately for each placement.
- Fit images without cropping; wrap and clip card text; size connection geometry from actual card dimensions.
- Preserve board image/content settings in complete backups; clean up copied assets when insertion fails.
- Passed four focused Android 14 instrumented methods and native image-picker open/cancel checks. Installed and launched on the owner's Android 14 phone.

## 0.2.0 — 2026-09-15

- Redesigned the native interface with cobalt, black and phthalo green, clearer navigation, large-text layouts and a light theme.
- Added durable navigation/filter restoration, cache management and safety-archive controls.
- Hardened restore staging/recovery, replacement backups, identity validation and search.
- Shared the Room instance/writer and introduced incremental record reuse to fix locking and stress-memory failures.
- Passed 42 JVM and 29 Android 14 instrumented methods, followed by three affected UI regressions after final visual refinements. Verified update-in-place and settled process restarts.

## 0.1.0 — 2026-09-15

- Initial native Kotlin/Compose/Room implementation of REQUIRED WB-01–WB-18.
- Reusable lore and relationships, visual boards, writing/media, Epoch Codex, flexible chronology, historical states, reveals, knowledge/accounts, recovery, complete backups and interchange.

See the [test report](docs/TEST_REPORT.md) for exact coverage and the [limitations](docs/KNOWN_LIMITATIONS.md) for qualification that remains open. A passing subset does not certify the complete acceptance matrix.
