# Changelog

## 0.2.2 — 2026-09-15

- Renamed the app to **World Note**, including the launcher, welcome screen and export filenames.
- Kept the existing install identity and backup format so updates retain saved worlds and earlier backups remain compatible.

## 0.2.1 — 2026-09-15

- Import an image directly from a board as reusable lore with a managed original.
- Choose cover image, note preview, tags and status separately for each placement.
- Fit images without cropping; wrap and clip card text; size connection geometry from actual card dimensions.
- Preserve board image/content settings in complete backups; clean up copied assets when insertion fails.

## 0.2.0 — 2026-09-15

- Redesigned the native interface with cobalt, black and phthalo green, clearer navigation, large-text layouts and a light theme.
- Added durable navigation/filter restoration, cache management and safety-archive controls.
- Hardened restore staging/recovery, replacement backups, identity validation and search.
- Shared the Room instance/writer and introduced incremental record reuse to fix locking and stress-memory failures.

## 0.1.0 — 2026-09-15

- Initial native Kotlin/Compose/Room app.
- Reusable lore and relationships, visual boards, writing/media, Epoch Codex, flexible chronology, historical states, reveals, knowledge/accounts, recovery, complete backups and interchange.
