# Documentation

## Using NodeNote

| Document | Read it for |
|---|---|
| [User guide](USER_GUIDE.md) | First world, writing, boards/images, chronology, recovery and exports |
| [Privacy](PRIVACY.md) | What stays on the phone, document providers and backup exposure |
| [Known limitations](KNOWN_LIMITATIONS.md) | Current bounds, unsupported extensions and unverified behavior |
| [Changelog](../CHANGELOG.md) | What changed in each test release |

## Understanding the code

| Document | Read it for |
|---|---|
| [Repository map](REPOSITORY_MAP.md) | Which folder/file owns a feature and where to make a change |
| [Architecture](ARCHITECTURE.md) | UI → ViewModel → repository → database flow and critical invariants |
| [Data model](DATA_MODEL.md) | Stable IDs, normalized records, references, revisions and incremental reads |
| [Chronology](CHRONOLOGY.md) | Dates, uncertainty, ordering, resolution and timeline semantics |
| [Backup format](BACKUP_FORMAT.md) | Archive members, validation, restore stages and limits |
| [Migration](MIGRATION.md) | Old NodeNote/JSON Canvas import and signing boundaries |
| [UI design](UI_DESIGN.md) | Palette, text, accessibility and board-image presentation |

## Building and checking

| Document | Read it for |
|---|---|
| [Build and test guide](BUILDING.md) | Toolchain setup, commands, APK location, signing and safe emulator tests |
| [Pinned environment](BUILD_ENVIRONMENT.md) | Exact dependency/tool versions |
| [Testing and coverage](TESTING.md) | Test responsibilities, recorded results and remaining qualification |
| [Performance](PERFORMANCE.md) | Measured fixtures and the limits of emulator results |
| [Contributing](../CONTRIBUTING.md) | Change/review workflow and data-preservation rules |

Generated packages and raw device logs stay out of Git. The repository includes [sample screenshots](images/); GitHub Actions uploads its own build reports.
