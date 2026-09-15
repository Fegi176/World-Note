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
| [Decisions](DECISIONS.md) | Why the current engineering approach was chosen |

## Building and checking

| Document | Read it for |
|---|---|
| [Build and test guide](BUILDING.md) | Toolchain setup, commands, APK location, signing and safe emulator tests |
| [Pinned environment](BUILD_ENVIRONMENT.md) | Exact dependency/tool versions |
| [Test report](TEST_REPORT.md) | What actually ran, the tested revision and what was not rerun |
| [Acceptance matrix](ACCEPTANCE_TESTS.md) | Individual AT scenarios: covered, partial and open |
| [Performance](PERFORMANCE.md) | Measured fixtures and the limits of emulator results |
| [Contributing](../CONTRIBUTING.md) | Change/review workflow and data-preservation rules |

## Product contract and progress

- [Feature matrix](FEATURE_MATRIX.md): REQUIRED WB IDs and optional WX/FUT scope.
- [Implementation status](../IMPLEMENTATION_STATUS.md): current release, device evidence and remaining work.
- [Original product specification](../NodeNote_Worldbuilder_Codex_Prompt.md): full product and implementation contract.
- [Agent guidance](../AGENTS.md): short repository rules for coding agents.

### Reading the evidence

Paths beginning with `artifacts/` in historical reports refer to local build evidence, not committed repository files. Raw device logs, generated packages and old build directories are intentionally excluded from Git. The repository includes [curated synthetic screenshots](images/) and [compact test summaries](verification/README.md); GitHub Actions uploads its own build outputs/reports when run.

An implemented feature is not a claim that every acceptance permutation passed. Follow the exact version and scope in the test report.