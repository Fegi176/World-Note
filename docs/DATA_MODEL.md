# Data model and durability

Database version: **2**. Portable archive version: **1**. App version: **0.2.1 / code 4**. These versions are independent.

## Relational representation

| Table | Content and ownership |
|---|---|
| worlds | UUID, name, description, archive state, fixed origin label/coordinate, terminology, timestamps |
| records | UUID/world FK, typed model kind, title/summary/body, independent canon/writing state, revision, lock/favorite/inbox/trash flags |
| fields | `(owner UUID, stable key)` values; built-in scalar metadata and typed custom-field records |
| refs | `(owner UUID, role, ordinal)` → target UUID; target FK indexed; repository validates kind and world |
| times | `(owner UUID, slot)` → versioned typed expression JSON; anchor identities are validated against the same registry |
| search_index | Derived FTS4 index, updated transactionally; can be rebuilt from records and fields |

`Kind` distinguishes lore entries, relationships, boards, placements, edges, groups, sections, template/field definitions, historical states, associations, stories, reveals, claims, knowledge, accounts/assertions, scheme stages, orders, attachments/media links, revisions/drafts, import reports, and saved filters. The identity registry is intentionally shared; the actual records are separate.

Aliases/tags and scalar metadata are searchable field rows. Custom fields have their own UUID record, field type, order, choices, value and reference list. Template application copies definitions into independently editable values and preserves provenance. Template edits never delete existing values.

Relationships reference lore endpoints. Placements reference a board and lore, or retain explicit board-local sticky prose. Edges reference placements on one board plus an optional semantic relationship. Semantic endpoints must match the drawn edge. The same semantic relationship and lore entry can appear on several boards. Removing local graph records cannot erase lore.

## Writes and recovery

- Serialized writer plus SQL transactions; no whole-world last-writer-wins replacement for ordinary edits.
- Editor debounce is 450 ms. A durable recoverable draft precedes canonical validation/save. Edit sequence controls the Saved acknowledgement; cancellation cannot interrupt the commit/acknowledgement boundary.
- Revision conflicts preserve the prior canonical value and recoverable draft. Done/Back pause on save failure. Text can be exported through the picker.
- Meaningful editor sessions checkpoint old content. Restoring a snapshot creates a new revision with the same identity.
- Board gesture frames stay in memory; completion commits one layout command. Undo only affects board-local records.
- Foreign keys restrict accidental target deletion. Permanent content erasure uses explicit tombstones; ordinary trash retains all content. World deletion removes all its rows transactionally after a recovery backup.
- Original attachment files are immutable. Recovery/import journal promotion happens before database insertion, so a committed world never intentionally points at unpromoted assets. Startup removes only journal-listed assets that have no committed attachment record.
- There is no destructive Room fallback. Migration 1→2 adds/rebuilds search from existing prose and custom values. A real prefilled version-1 SQLite fixture is exercised by instrumentation; schemas are exported under `app/schemas`.

## Known scale boundaries

The UI currently observes a complete selected-world logical snapshot. It does not page the entire registry yet. See measured typical-fixture results in PERFORMANCE.md. This is not an 8 MiB workspace blob limit, but large worlds incur snapshot load/allocation costs.

## Runtime ownership and incremental reads (0.2.0)

One Room instance and one writer mutex are shared per process. This prevents overlapping activity lifetimes from opening independent pools against the same SQLite file. Startup restore-journal recovery runs once per process before authoring UI is enabled.

Selected-world observations query lightweight identities and load changed records/normalized children in batches of 500. Repository writes advance process-local generations, including field-only writes with unchanged authored revision numbers. Observers reuse immutable unchanged records, avoiding repeated allocation of long note bodies. New processes rebuild this cache from SQLite. Production mutations must continue through Repository; raw DAO writes are reserved for migration/test setup. No database schema change was needed: version 2 is byte-identical to the 0.1.0 schema.
