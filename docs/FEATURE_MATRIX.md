# Feature matrix

All WB IDs have working native implementation paths. “Implemented” describes functionality, not completion of every acceptance permutation. WB-18 has native accessibility, gesture, lifecycle and failure-injection checks; physical qualification remains limited; see ACCEPTANCE_TESTS.md for evidence and KNOWN_LIMITATIONS.md for specific gaps. Paths below are relative to the project; UI files are under `app/src/main/kotlin/app/nodenote/worldbuilder/ui`, model files under `core/src/main/kotlin/app/nodenote/core`.

| ID | Implemented behavior / principal files | Evidence / limits |
|---|---|---|
| WB-01 | Worlds create/switch/rename/duplicate/archive/delete, preferences; App.kt, AppModel, Repository | Room isolation/duplication, UI; durable navigation, board selection, filters, timeline viewport and editor context |
| WB-02 | Reusable identities, optional templates, typed fields, aliases/tags; Model, Editor | Sample integrity, Room roundtrip; incompatible conversion requires explicit reference fixes |
| WB-03 | Long notes, safe Markdown, stable-ID wiki links, backlinks, drafts; Editor, Markdown, AppModel | Unicode >100k repository test; native rapid writing/background test |
| WB-04 | Canvas pan/pinch/drag, placements, stickies, groups, local edges, undo100, fit, insertion, outline/layout preview, per-placement image/note/tags/status previews; Board, BoardCardContent, Graph | Generated transform/undo tests and native sample; exhaustive gestures unverified |
| WB-05 | Directed/symmetric independent relationships with notes, reuse across boards; Model, Board, Editor | Shared placement/relationship persistence and sample |
| WB-06 | Private originals/thumbnails, gallery, captions/source/cover, direct board Image import; Files, Editor, Board | Sample images and archive hash roundtrip; managed source deletion, corrupt input/preview and image archive checks pass; exhaustive image fuzzing remains |
| WB-07 | Whole-world FTS/substring, filters/saved filters, favorites/inbox; Repository, App | Unicode/isolation/performance tests |
| WB-08 | Dedicated Epoch Codex, eras/events, sections, state/legacy and focused board; Editor, Timeline | Five-epoch sample and native Codex navigation |
| WB-09 | Signed 64-bit exact/approx/range/offset/local/within/unknown plus strict order; Chronology, TimeEditor | JVM semantic contracts including overflow/unresolved cycles |
| WB-10 | Readable overview, scaled viewport/lanes, unresolved tray, diagnostics; Timeline, Graph | Precision/lanes/order tests; emulator screenshots; physical frame times open |
| WB-11 | Explicit period associations, separate historical states and legacy links; Editor, Model | Demo/Room/archive roundtrips; no inferred state fallback |
| WB-12 | Story hierarchy/reorder and reveal records independent of dates; Timeline, Editor | Demo and archive; runtime Reveals navigation |
| WB-13 | Claims/truth/public status, explicit knowledge, account assertions/shared prose; Editor, Projection | Sentinel projection test, archive, native cross-tab account filter |
| WB-14 | Schemes/stages planned vs actual, mysteries/clues/arcs and links; Model, Editor | Schema/demo/archive coverage; no automated intrigue inference |
| WB-15 | Independent canon/writing states, labels, lock, revision retention/restore/conflict choices, trash/tombstones; Repository, AppModel, Editor | Atomic rejection/draft/lock/trash/retention/duplicate tests |
| WB-16 | Versioned all-record/assets/prefs ZIP, validated staged restore/remap/replacement, recovery archives, Markdown bible/public export; Archive, Integrity, AppModel | JVM hostile archives, complete Room roundtrip, real Android DocumentsUI; file promotion/SQL commit boundary failure injection and verified safety-copy replacement |
| WB-17 | Map-scoped NodeNote v1, original raw retention, JSON Canvas text/edges/groups warnings; Interchange | Synthetic legacy Unicode/scoped IDs, Canvas roundtrip; no real old archive supplied |
| WB-18 | Native lifecycle/IME/Back, themes/resources/outline, real Gradle/SDK/Room tests/APK/CI | Cobalt/black/green adaptive UI, contrast/automated accessibility, native pinch; physical accessibility qualification remains partial |
| WX-01 | Not implemented: map pins/nested maps | Extended |
| WX-02 | Not implemented: full calendar conversion | Year-level chronology implemented under WB-09 |
| WX-03 | Not implemented: character/chapter-as-of reader | Basic account mode under WB-13 |
| WX-04 | Not implemented: family/incarnation visualizations | Reusable relationship data available |
| WX-05 | Not implemented: external scheduled backup | Manual picker backup implemented |
| WX-06 | Not implemented: advanced layouts/diffs/large-scale tuning | Basic undo/layout preview and snapshots implemented |
| FUT-01 | Excluded: cloud/collaboration/accounts | No network permission |
| FUT-02 | Excluded: AI/API/canon rewriting | No inference or generated user lore |
| FUT-03 | Excluded: map painting/terrain generation | No simulated completion |
