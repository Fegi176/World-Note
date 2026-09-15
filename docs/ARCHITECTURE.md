# Architecture

## One world, several independent views

The same character can appear on several boards, have different historical states, and be mentioned by a public account. Those views reference one stable lore identity; they do not own duplicate copies of its prose.

```mermaid
flowchart TD
    UI["Compose screens: Library · Boards · Editor · Timeline"] --> VM["AppModel: navigation, save state, operations"]
    VM --> Repo["Repository: validation and transactions"]
    Repo --> DB["Shared Room / SQLite database"]
    VM --> Files["ManagedFiles / RestoreEngine"]
    Files --> Assets["App-private originals and recovery files"]
    VM --> Prefs["DataStore: lightweight UI preferences"]
    Repo --> Core["core: model, chronology, integrity, archives"]
```

## App and core

- **core** is a pure Kotlin JVM module. It defines record kinds and references, year expressions and resolution, geometry, archive validation, import/export and public projections. These rules can be tested without Android.
- **app** supplies Compose screens, lifecycle/ViewModel state, DataStore preferences, Room persistence and Android document/image APIs.
- The app never requires a network service. The system picker mediates imports/exports; a selected provider has its own behavior.

See the [repository map](REPOSITORY_MAP.md) for concrete files.

## Identity and persistence

Room is authoritative. A normalized registry stores typed records plus scalar fields, ordered references and temporal expressions; it is not a serialized whole-world blob. UUIDs identify worlds, lore, placements, relationships and secondary records independently.

Repository owns production writes, validates world/type references and uses transactions. One process-wide Room instance and writer mutex avoid competing activity-owned connections. Selected-world observations reuse unchanged immutable records; process-local generations tell observers which normalized children need reloading.

Do not bypass Repository for ordinary mutations: raw DAO writes can leave incremental observation/search state stale. DAO-level access is reserved for migration/test setup.

Database schema version, archive format version and application version are separate compatibility decisions. See [data model](DATA_MODEL.md).

## Editing and board operations

An editor's recoverable draft precedes canonical save/validation. Save sequencing controls the visible acknowledgement; failed/conflicting writes preserve recovery options. Revisions are explicit checkpoints.

Gesture frames stay in memory until a board command is committed. Placement coordinates, size, color and image/note display flags belong to the placement. Lore prose and relationship explanations remain shared. Board undo applies to local graph state; removing a placement cannot delete its lore.

Image import validates/copies an original into private storage, then inserts its lore/media/placement transactionally. Canvas thumbnails are bounded and decoded off the UI thread. Full original bytes remain available for backup.

## Chronology and knowledge

Chronology expressions retain their uncertainty and anchor identities. Resolution/diagnostics do not silently replace unknown dates with exact coordinates. Overview layout and numeric scaled layout serve different reading tasks.

Story reveal order, author truth, explicit character knowledge and recorded public accounts remain separate models. Public export uses an allowlisted projection of selected prose; there is no fallback to private canonical prose or captions.

## Restore and recovery

A complete archive is inspected before mutation. Original images are staged/promoted with a recovery journal; database insertion/replacement is transactional. Replacement first produces a verified safety archive. Startup journal recovery resolves interrupted file promotion against committed attachment identities.

These boundaries are covered by tests, but they are not a guarantee for every hardware power-loss timing or document provider. See [backup format](BACKUP_FORMAT.md), [testing and coverage](TESTING.md) and [limitations](KNOWN_LIMITATIONS.md).

## Testing boundaries

Core tests cover pure semantics, bounds and formats. Instrumented tests use real Room/SQLite and native Android controls, including DocumentsUI, writing, gestures and accessibility checks. Synthetic typical/stress fixtures measure specific operations; they do not establish sustained performance on every phone.

A real Android 14 phone has received the signed test updates and passed launch checks. Full physical TalkBack/performance qualification remains open.
