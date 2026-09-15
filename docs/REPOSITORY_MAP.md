# What is where

The project has two Gradle modules. `core` has no Android dependency; `app` provides the native Android interface and persistence.

## Top-level folders

| Path | Contents |
|---|---|
| `app/src/main/kotlin/app/nodenote/worldbuilder/` | Android entry point, state, UI and storage |
| `app/src/main/res/` | Strings, launcher vectors and Android configuration resources |
| `app/src/androidTest/` | Instrumented Room, migration, media, picker, UI and stress tests |
| `app/schemas/` | Exported Room schemas; retain old versions for migration tests |
| `core/src/main/kotlin/app/nodenote/core/` | Models, chronology, graph operations, validation and archive formats |
| `core/src/test/` | JVM chronology, identity and archive-failure tests |
| `gradle/wrapper/` | Pinned Gradle distribution metadata and wrapper JAR |
| `scripts/` | Verification, package generation and Android 14 installation/smoke helpers |
| `docs/` | Product/developer documentation and curated synthetic screenshots |
| `.github/workflows/` | Build/lint/test automation |
| `artifacts/` | Ignored local APKs, ZIPs, logs, screenshots and reports |
| `.tooling/` | Ignored workspace-specific SDK/JDK/tools |

## Start at these files

All paths below are relative to `app/src/main/kotlin/app/nodenote/worldbuilder/`.

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Native activity and application UI entry |
| `AppModel.kt` | ViewModel state, navigation, save coordination and long-running operations |
| `ui/App.kt` | App shell, worlds, Library, Search, menus, settings and import/export routing |
| `ui/Editor.kt` | Lore editing, details, links, history, Media, revisions and Epoch Codex |
| `ui/Board.kt` | Native canvas gestures, selections, placements, edges, groups and outline |
| `ui/BoardCardContent.kt` | Placement preview options, bounded image loading and native card text/image drawing |
| `ui/Timeline.kt` | Period/event/reveal views, overview and scaled chronology |
| `ui/AtlasTheme.kt` | Cobalt/black/green palette, type, icons and adaptive navigation |
| `ui/Components.kt` | Reusable form controls, cards, pickers, time editing and Markdown display |
| `ui/PersistentUi.kt` | Durable per-world/per-entry UI preferences |
| `ui/Storage.kt` | Thumbnail cache and safety-archive management |
| `data/Database.kt` | Room entities/DAOs, shared database and migrations |
| `data/Repository.kt` | Transactions, validation, revisions/drafts, search and incremental observations |
| `data/Files.kt` | Managed image originals/thumbnails and safety-copy files |
| `data/RestoreEngine.kt` | Staged file promotion, restore journal, SQL commit and recovery |

## Core module map

| File | Responsibility |
|---|---|
| `Model.kt` | Record kinds, stable IDs, fields/references and typed model rules |
| `Chronology.kt` | Uncertain dates, anchors, resolution and diagnostics |
| `Graph.kt` | Geometry, transforms, overlap/layout helpers and undo history |
| `Integrity.kt` | Cross-record consistency and reference validation |
| `Archive.kt` / `BoundedJson.kt` | Complete backups, hashes, bounded parsing and restore inspection |
| `Interchange.kt` | NodeNote v1/JSON Canvas import/export and explicit public-account projection |
| `Markdown.kt` | Markdown world-bible export |
| `Demo.kt` / `Fixtures.kt` | Original sample world and generated test/performance fixtures |

## Which file should I change?

- A board's appearance: `BoardCardContent.kt` / `AtlasTheme.kt`; geometry and gestures: `Board.kt` / `Graph.kt`.
- A new scalar lore detail: `Model.kt` and `Editor.kt`; persistence invariants still belong in Repository/Integrity.
- Date meaning or ordering: `Chronology.kt` and core tests first, then Timeline/TimeEditor.
- Backup compatibility: Archive, Integrity and RestoreEngine, with roundtrip/failure tests.
- Database shape: Database plus an explicit migration and schema fixture.
- A saved UI preference: PersistentUi/AppModel; do not put authored lore in DataStore.

Read [architecture](ARCHITECTURE.md) before changing ownership or persistence behavior.
