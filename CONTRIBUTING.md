# Contributing

Start with the [repository map](docs/REPOSITORY_MAP.md) and [architecture](docs/ARCHITECTURE.md).

## Local workflow

1. Use JDK 17 and the pinned Android SDK requirements in the [build guide](docs/BUILDING.md).
2. Create a branch for a focused change. Keep existing writing, identities and archive compatibility intact.
3. Run core tests, Android lint and assembly. Exercise changed data or UI behavior on a dedicated Android 14 emulator.
4. Update the relevant guide when behavior or setup changes.
5. Open a pull request explaining the behavior change, data/schema impact and validation.

## Rules that matter

- Keep the installed app offline; no accounts, analytics, remote assets or runtime network permission.
- Lore entries, relationship notes, board placements, chronology and story order have separate identities and responsibilities.
- Route production writes through Repository so transactions, search and incremental observations remain consistent.
- Never use destructive Room migrations. Preserve original imports and validate before replacing data.
- Unknown dates stay unknown. Canon edits must not infer character knowledge or public accounts.
- Use a dedicated emulator or the separate `.qa` package. Connected debug tests can remove the tested package; never point them at a personal writing installation.
- Commit source and curated synthetic screenshots only. Keep signing keys, SDKs, build output, credentials, private lore and device logs out of Git.

Follow the existing Kotlin style and avoid reformatting unrelated files. Add regression tests for changes to persistence, import/export, identity or gestures.

## Reporting bugs

Include the app version, Android version, steps, expected/actual behavior and whether the problem reproduces in the sample world. Share a small synthetic example when possible. Do not post private world backups, signing material or authentication tokens in issues.
