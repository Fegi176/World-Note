# NodeNote Worldbuilder — Codex implementation prompt

**Specification version:** 1.0  
**Prepared:** 15 September 2026  
**Deliverable:** A working, offline Android worldbuilding app, its complete source project, and a real signed test APK.  
**Working app name:** NodeNote Worldbuilder  
**Product direction:** A phone-first lore library and visual mind map with detailed notes, reusable entries, relationships, epochs, and story mysteries.

---

## How to use this file

Place this file in the Android project's working directory, or in an empty directory for a new project. Open that directory in Codex. The previous `NodeNote-source.zip` is an optional migration/reference input; this specification is sufficient to start without it.

Give Codex this instruction:

```text
Read NodeNote_Worldbuilder_Codex_Prompt.md completely. Treat it as the product and implementation specification. Inspect the workspace, preserve existing work, and then implement the Android app rather than only writing a plan. Start with the buildable native foundation and continue through all REQUIRED scope, including the Epoch Codex and flexible chronology. Make routine engineering decisions yourself, build and test continuously, and produce a signed test APK when the environment permits. Do not substitute a web mockup for the Android deliverable. Record actual progress, test evidence, unresolved issues, and the next exact step in the project files specified in the document.
```

Keep this long specification under its own filename. Do **not** paste the entire document into `AGENTS.md`. Maintain a short `AGENTS.md` pointing to it and describing build commands and critical invariants. Codex's documented instruction discovery has a configurable combined size limit; explicitly reading this specification avoids treating a long specification as automatically loaded project instructions. [S1]

The following sections are the implementation prompt. They are instructions for the coding agent, not a claim that these features already exist.

---

## 01. Your assignment and operating rules

You are the lead Android engineer, product designer, data-model engineer, and test engineer for this project.

Build a real app that the owner can install on an Android phone and use to organize an original fictional world. The core interaction is:

> Create a short card on a visual canvas, tap it, and write as much detail as needed in its associated lore entry. Reuse that entry on other boards, connect it to other entries, and place its history within epochs and eras.

The owner's interests include dark fantasy, cultivation worlds, industrial horror, ancient civilizations, gods, factions, morally complicated antagonists, secret histories, and worlds that span roughly 100,000 years or more. These are use cases, **not restrictions on what kind of world the app can hold**. Do not hardcode the owner's game lore or copy characters from existing novels/games into the sample world.

### Execution rules

- Implement code, persistence, navigation, interaction, tests, and packaging. A GDD, wireframe, static website, or collection of empty screens is not the deliverable.
- Inspect existing files and repository guidance before editing. Preserve unrelated work and user changes. Do not perform destructive Git operations or delete an existing project to simplify the task.
- Use the defaults in this specification when details are unspecified. Record consequential decisions in `docs/DECISIONS.md`; do not stop for routine product approval after every step.
- Respect the execution environment's permissions. Never bypass sandbox restrictions, disclose secrets, publish a repository, upload private lore, or deploy to a public service merely to finish a build.
- Read relevant current official documentation when selecting dependencies or using unfamiliar APIs. Pin a mutually compatible stable toolchain; do not blindly choose every newest library independently.
- Work in buildable vertical slices. Keep each completed slice usable and verify it before expanding scope.
- Do not stop after generating a plan or after completing only the first slice when further implementation is possible.
- REQUIRED means part of the requested complete core app. EXTENDED means the next planned layer; do not sacrifice required data integrity or epoch features to add extended polish.
- Do not show dead buttons for unfinished features. Hide unimplemented functionality and mark it honestly in the feature matrix.
- Never claim installation, device testing, a successful build, or passing tests unless the corresponding action actually ran successfully. Do not inherit old test claims as proof of the new implementation.
- When a real environment blocker prevents a step, complete independent work, record the exact command/error, and provide reproducible local/CI instructions. Do not generate a fake APK or rename HTML/ZIP to `.apk`.
- If execution must end before completion, preserve the latest buildable code and update `IMPLEMENTATION_STATUS.md` with completed IDs, blockers, failing tests, and the next concrete task. Do not label incomplete scope complete.

### Product non-negotiables

1. Android phone first; portrait use is the main workflow.
2. Fully usable offline, with no account, subscription, advertising, analytics, or cloud dependency.
3. Detailed notes belong to real lore entries, not only canvas labels.
4. One lore entry can appear on multiple boards without duplicating its content.
5. Relationships can have their own long notes and validity periods.
6. Epochs are first-class lore entries and a central navigation feature, not an afterthought.
7. Unknown and approximate dates are valid data. Never invent precision to draw a timeline.
8. Historical chronology, public belief, character knowledge, and story reveal order are different concepts.
9. Protect saved writing, import safely, and make complete backups restorable.
10. Deliver the Android source and an installable test APK when the build environment supports it.

---

## 02. Existing prototype: inspect, preserve, and migrate

This project follows a prototype named **NodeNote 1.0**. An earlier **MindPocket** prototype may also be supplied. Do not confuse their storage formats or application IDs.

### Verified reference facts about the supplied NodeNote archive

These observations come from inspection of the supplied `NodeNote-source.zip`, not Android runtime testing:

| Item | Observed in the supplied NodeNote source |
|---|---|
| Android application ID | `app.nodenote` |
| Prototype version | `versionCode = 1`, `versionName = 1.0` |
| Declared minimum / target | API 26 / API 34 in the old manifest |
| UI | Bundled HTML/CSS/JavaScript displayed in a Java WebView wrapper |
| Android storage | SharedPreferences file `nodenote`, string key `workspace` |
| Browser-only storage | Local-storage key `nodenote.workspace.v1` |
| Backup envelope | `format: "nodenote"`, `version: 1`, `active`, `theme`, `maps` |
| Map shape | `id`, `name`, `updated`, `view`, `nodes`, `edges` |
| Node shape | `id`, `title`, `note`, `x`, `y`, `color` |
| Edge shape | `id`, `from`, `to`, `label` |
| Old build route | Custom Python DEX/resource emission; not a normal Gradle/SDK compilation |
| Old signing material | A deliberately shared prototype development key is present in the source archive |
| Existing test scope | Browser tests and package checks; the archived report explicitly says Android runtime/device testing did not run |

Relevant archive paths:

```text
NodeNote/README.md
NodeNote/app/src/main/AndroidManifest.xml
NodeNote/app/src/main/assets/index.html
NodeNote/app/src/main/java/app/nodenote/MainActivity.java
NodeNote/tools/app_dex.py
NodeNote/tools/build_apk.py
NodeNote/verification/package-report.json
```

Reinspect actual inputs before implementing migration. A user's working copy may differ from this archive. Do not run custom build scripts merely because they are present. Treat imported archives as untrusted and extract them safely into a separate reference directory.

### Modernization decision

Implement the worldbuilder with **native Kotlin, Jetpack Compose, Room, and the ordinary Android SDK/Gradle build**. Reuse tested ideas, visual conventions, and import knowledge from NodeNote; do not extend the custom DEX emitter or build the entire new app as one giant WebView/localStorage blob.

This is a deliberate replacement of the prototype's technical foundation, **not permission to discard user data**.

### Installation identity and signing safety

Default to a safe side-by-side app:

```text
Base applicationId: app.nodenote.worldbuilder
Debug applicationId: app.nodenote.worldbuilder.debug
Display name: NodeNote Worldbuilder
```

The default debug app must not replace or uninstall `app.nodenote`. It imports exported NodeNote backups using the system file picker. A different application ID cannot simply read the old application's private preferences.

Provide an optional, explicitly named `legacyUpgrade` variant only when the existing NodeNote source and matching signing material are available. It must use `app.nodenote`, a higher version code, and a verified matching signing certificate. Its purpose is a private prototype-to-native migration, not public distribution. Android signing identity is part of the update path; inspect signatures rather than assuming that an application ID alone permits an update. [S5]

- Do not use the archive's publicly shared development key for a public production release.
- Do not print key passwords in logs, commit new secrets, or include signing keys in generated distribution/source bundles.
- Keep private signing configuration in ignored local files/environment variables.
- Do not change the standard debug identity merely to work around an installation conflict.
- Never use uninstall/clear-data as an automatic installation fix. Explain the mismatch and preserve/export data.
- When testing an upgrade, test only on a dedicated emulator/test device with synthetic data unless the owner explicitly authorizes otherwise.

### Minimum legacy importer contract

Support this valid NodeNote v1 shape even when the old source is absent:

```json
{
  "format": "nodenote",
  "version": 1,
  "active": "map-1",
  "theme": "dark",
  "maps": [
    {
      "id": "map-1",
      "name": "Old lore notes",
      "updated": 1700000000000,
      "view": {"x": 12, "y": 20, "s": 0.8},
      "nodes": [
        {"id": "n-1", "title": "The Ash Physician", "note": "First line.\n\nSecret history: Łódź, 王朝, 星.", "x": 0, "y": 0, "color": "#b9a1ff"},
        {"id": "n-2", "title": "The Lantern Order", "note": "A public relief organization.", "x": 240, "y": 180, "color": "#8bd5b2"}
      ],
      "edges": [
        {"id": "e-1", "from": "n-1", "to": "n-2", "label": "secretly funds"}
      ]
    }
  ]
}
```

Map one imported workspace to a **new world**, each old map to a board, each old node to a generic lore entry plus a board placement, and each old edge to a directed relationship plus a board edge. Preserve title, complete note, label, color, and geometry. Old card dimensions were 190 × 112 canvas units; use them as the migration baseline and verify viewport conversion rather than assuming coordinate semantics.

Old node IDs are scoped by map. Use `(importBatch, oldMapId, oldNodeId)` when remapping; two maps can contain equal old node IDs. Do not merge equally named entries or nodes automatically. Preserve provenance so the owner can merge duplicates deliberately later.

Validate first, show an import summary, then insert transactionally. Keep the original input intact. Reject unsupported future versions with a helpful message. Preserve unknown legacy fields in an import report/raw recovery attachment when practical rather than silently throwing them away.

For the optional same-package upgrade, read the old preferences once, keep a byte-for-byte recovery copy, import transactionally, record the source hash and migration completion in the same logical transaction, and never clear the old preferences automatically. An interrupted migration must not create duplicate worlds on restart.

---

## 03. Scope and priorities

Create `docs/FEATURE_MATRIX.md` using these stable feature IDs. For each ID record its status, source files, tests, and any limitation.

| ID | Capability | Scope |
|---|---|---|
| WB-01 | Multiple worlds, world switching, local preferences | REQUIRED |
| WB-02 | Reusable lore entries, optional templates, custom fields, aliases/tags | REQUIRED |
| WB-03 | Long notes, Markdown preview, internal links, backlinks | REQUIRED |
| WB-04 | Touch canvas, reusable placements, groups, connections, undo/redo | REQUIRED |
| WB-05 | Directed/symmetric relationships with detailed notes | REQUIRED |
| WB-06 | Image attachments, cover image, gallery, source captions | REQUIRED |
| WB-07 | World-wide search, filters, favorites, quick-capture inbox | REQUIRED |
| WB-08 | Epoch Codex, eras, expandable chronology, event entries | REQUIRED |
| WB-09 | Exact/approximate/range/relative/order-only/unknown dates | REQUIRED |
| WB-10 | Overview/scaled timeline modes, overlap lanes, chronology validation | REQUIRED |
| WB-11 | Period links and explicit historical states of entries | REQUIRED |
| WB-12 | Story structure, reveal milestones, separate history/reveal views | REQUIRED |
| WB-13 | Secrets, claims, public/author history, basic knowledge ledger | REQUIRED |
| WB-14 | Schemes, mysteries, clues, character-arc templates and links | REQUIRED |
| WB-15 | Canon/editing status, revision snapshots, trash and recovery | REQUIRED |
| WB-16 | Versioned complete backup, safe restore, Markdown world-bible export | REQUIRED |
| WB-17 | NodeNote v1 import and basic JSON Canvas import/export | REQUIRED |
| WB-18 | Native lifecycle, accessibility, testing, reproducible Android packaging | REQUIRED |
| WX-01 | Geographic maps with pins and nested maps | EXTENDED |
| WX-02 | Full multi-calendar date conversion beyond year-level chronology | EXTENDED |
| WX-03 | Perspective-as-of-character/chapter filtered reading mode | EXTENDED |
| WX-04 | Dedicated family-tree and incarnation/form visualizations | EXTENDED |
| WX-05 | User-selected external scheduled backup destination | EXTENDED |
| WX-06 | Advanced layouts, richer revision diff, larger-scale graph optimizations | EXTENDED |
| FUT-01 | Cloud sync, collaboration, remote accounts | NOT IN THIS BUILD |
| FUT-02 | AI generation, API integrations, automatic canon rewriting | NOT IN THIS BUILD |
| FUT-03 | Full map-painting/terrain-generation application | NOT IN THIS BUILD |

Milestones define implementation order, not permission to reclassify required features as optional. Reach a reliable core before implementing extended scope.

---

## 04. Technology and project organization

### Chosen stack

- Kotlin for app and domain code.
- Jetpack Compose and Material 3 for the native interface.
- A custom native canvas implementation, with Compose/View interop only where it materially improves gesture control or rendering.
- Room/SQLite as the authoritative persistent store for structured world data. Room is Android's documented persistence abstraction over SQLite and supports schema/query tooling; use its migration mechanisms rather than destructive recreation. [S3][S7]
- Kotlin coroutines and Flow for asynchronous persistence and UI observation.
- ViewModels with unidirectional state flow. Keep domain logic separate from UI and storage adapters; this follows the separation emphasized in Android's architecture guidance. [S2]
- DataStore for lightweight UI preferences, not the entire world database.
- Versioned JSON DTOs for portable backup/import/export.
- App-private files for copied image attachments and thumbnails.
- Android system document APIs for file import/export and optional user-selected destinations. The Storage Access Framework supports user-mediated document selection; use that mechanism instead of broad filesystem access. [S4]
- An ordinary Gradle wrapper, Android Gradle Plugin, official Android SDK, and a compatible JDK. The documented command-line Android build route uses the project's Gradle tasks. [S6]

Default `minSdk = 26`. Choose a stable compile/target SDK and mutually compatible dependency set at implementation time. Verify Android 14 behavior as one target, but do not claim compatibility with any OS version that has not been exercised. Do not freeze this project to the prototype's old target SDK.

Use a version catalog or similarly centralized pinned versions. Prefer stable APIs and a modest dependency count. Record exact versions in `docs/BUILD_ENVIRONMENT.md`. Avoid unnecessary dependency-injection/code-generation frameworks; constructor injection and a small app container are sufficient unless the existing project already has a sound alternative.

### Architecture boundaries

Suggested packages/modules:

```text
app/                       Android entry point, navigation, dependency wiring
core/model/                Domain IDs and plain data models
core/chronology/            Year expressions, constraints, resolution, diagnostics
core/graph/                 Board geometry, hit testing, graph/layout commands
core/export/                Portable DTOs, validators, safe projection/export
core/ui/                    Theme, typography, common accessible components
data/database/             Room entities, DAOs, schema migrations
data/repository/           Transactions, draft/revision handling, observable queries
data/files/                Attachment import, thumbnailing, archive handling
feature/worlds/
feature/library/
feature/editor/
feature/boards/
feature/timeline/
feature/history/
feature/intrigue/
feature/search/
feature/settings/
```

These can begin as packages in one app module plus a pure Kotlin core module; do not spend the first milestone creating dozens of empty Gradle modules. Extract more modules only when useful.

Pure chronology, graph mathematics, format validation, and export selection logic must be testable without launching Android. Compose code must not contain SQL, ZIP extraction, relative-date resolution, or migration logic.

### No-network default

Do not request `INTERNET`, broad storage access, contacts, location, microphone, or unrelated permissions. Do not load fonts/scripts/images from CDNs. All essential assets and rendering must work in airplane mode. External source URLs are user-written reference metadata; opening one is an explicit action in an external browser, not background fetching by the app.

Do not add AI SDKs, Firebase, advertising SDKs, accounts, or analytics as convenient scaffolding.

---

## 05. Phone interface and navigation

Aim for a polished writing tool with a dark-first neutral theme, readable typography, restrained accents, subtle depth, and compact but comfortable cards. Also provide a light theme and follow-system option. Do not fill the screen with decorative dashboards or ornamental worldbuilding imagery.

Use four root destinations:

```text
Library | Boards | Timeline | Search
```

The world selector belongs in the top app bar. The selected world persists. The contextual `+` action creates a lore entry, quick note, board, event, epoch, or era as appropriate. Settings, import/export, backup status, templates, and trash are accessible from the world menu without adding many bottom tabs.

### Screen contracts

| Screen | Required behavior |
|---|---|
| World chooser | Create/rename/duplicate/archive worlds; start blank or load an optional demo |
| Library | Filter by type, tags, canon, writing status, epoch; favorites and inbox |
| Entry details | Summary, notes/sections, relationships, history, media, backlinks |
| Entry editor | Full-screen long-form writing, visible save state, keyboard-friendly controls |
| Board list | Thumbnails/previews, recent boards, saved filter summaries |
| Board canvas | Pan/zoom, selectable movable cards, connections, group controls, contextual actions |
| Timeline | Tabs `Epochs`, `Events`, `Reveals`; persistent view/filter state |
| Epoch details | Overview, chronology/children, world state, linked entries, legacy, recorded accounts |
| Relationship editor | Endpoints, type/direction, notes, validity, claims/secrecy |
| Search | Full-world results grouped by entry, relationship, event, claim, and note section |
| Backup/import | File picker, progress, validation summary, recoverable errors, restore confirmation |

Use a full-screen editor for substantive writing. A short bottom-sheet preview is allowed, but the user must not write a thousand-word biography inside a cramped half-screen form.

Default text fields and labels are English. Support arbitrary Unicode in lore, including Polish, Chinese, Japanese, emoji, combining marks, and mixed scripts. Externalize UI strings for later localization. Do not assume an ASCII-only name slug can uniquely identify entries.

### Accessibility and lifecycle

- Use at least 48 dp touch targets for primary controls and generous hit regions for thin links.
- Support system font scaling, a visible focus state, TalkBack labels, and non-color indicators for type/status.
- The canvas needs an accessible list/outline alternative; TalkBack users must be able to open entries and relationships without dragging graphical nodes.
- Respect system bars, edge-to-edge insets, the keyboard, Back behavior, and available display width.
- Support narrow phone layouts around 360 dp width as well as larger phones. Do not use fixed screenshot dimensions as layout dimensions.
- Preserve current world, open entry, draft, board viewport, selection where sensible, timeline expansion, filters, and scroll state across routine recreation.
- Do not promise unsaved in-memory keystrokes survive arbitrary process termination. Persist drafts promptly and distinguish `Saving`, `Saved`, and `Save failed` honestly.
- Do not lock orientation just to conceal broken layouts. Landscape can be a simple responsive layout; portrait remains the priority.

---

## 06. Worlds, lore entries, templates, and custom fields

### Worlds

A world is an isolated workspace with its own entries, relationships, boards, chronology, story outline, templates, attachments, and settings. Data must not leak into another world through search or ID collisions.

Create a world with only a name. Optional settings include description, icon/cover, terminology (`Epoch`, `Era`, etc.), chronology origin label, present anchor, and default entry type.

Duplicate a world by consistently remapping all IDs and references; do not create cross-world pointers accidentally. Archive rather than immediately delete. World deletion needs explicit confirmation and a backup reminder.

### Lore entries

Each entry needs a stable immutable ID, world ID, entry type, title, optional summary, full body/sections, aliases, tags, media links, creation/update timestamps, canon status, writing status, revision number, and trash metadata.

Only the title is required in the create dialog; choose the type's default values. Permit very short/early ideas. Avoid arbitrary restrictions such as a note field limited to a few hundred characters.

Seed editable templates for:

| Type | Useful optional fields/sections |
|---|---|
| Generic note | Summary, main note, open questions |
| Character | Appearance, personality, desires, beliefs, fears, history, abilities, limitations, secrets |
| Faction / sect | Public mission, hidden agenda, ideology, leadership, resources, internal conflicts, rivals |
| Place / realm | Atmosphere, geography, environment, inhabitants, laws, dangers, history |
| Creature / monster | Appearance, behavior, origin, abilities, weaknesses, ecological role |
| Deity | Domains, identity, worship, doctrine, taboos, miracles, concealed truth |
| Religion | Beliefs, institutions, rituals, competing interpretations, schisms |
| Power system | Rules, ranks, progression, costs, limits, exceptions, consequences |
| Artifact / item | Origin, function, activation, price of use, owners, historical significance |
| Event | Chronology, location, participants, causes, actual outcome, consequences, accounts |
| Epoch | Boundaries, civilizations, conditions, gods/powers, developments, legacy, historical knowledge |
| Era | Parent period, boundaries, defining changes, major events, institutions |
| Scheme | Planner, objective, cover story, stages, dependencies, resources, contingencies, outcome |
| Mystery | Central question, author answer, evidence, competing explanations, reveal plan |
| Clue | Observation, source, linked mystery, apparent interpretation, actual meaning, discovery |
| Character arc | Starting belief, pressures, turning points, decisions, ending belief, linked scenes/events |

For character templates, include an optional antagonist section:

```text
What do they want?
What belief justifies their actions?
What are they right about?
Who benefits from their actions?
What line would they never cross?
What could make them cross it?
What would a convincing defeat or change of heart require?
```

These are editable prompts, not mandatory answers or hardcoded moral categories.

### Custom fields and templates

Allow users to add, rename, reorder, hide, and remove optional fields. Initial field types: short text, long Markdown text, number, boolean, choice, multi-choice, entry reference, and reference list. Chronology fields should reuse the real chronology editor, not an unrelated date string.

Template definitions and field definitions have stable IDs. Editing a template must not delete existing values. Removing a field from a template hides/deprecates it for new forms while preserving stored values and offering an explicit data-removal action.

Existing entries keep their data when their type changes. Show an impact preview when a conversion affects specialized metadata. Never delete an epoch's chronology simply because the owner switches its display template.

---

## 07. Notes, internal links, and backlinks

Provide a reliable Markdown source editor with a separate safe preview. Support headings, paragraphs, lists, emphasis, code blocks, quotes, and ordinary links. Editing must preserve original text; preview formatting must not normalize away meaningful user content.

Add `[[Entry Name]]` autocomplete scoped to the current world. Allow aliases and an explicit picker when names are ambiguous. Store a stable target identity instead of relying on the current title forever.

A practical internal form is a readable Markdown label pointing to an app-owned ID URI, for example:

```markdown
[The Lantern Order](nodenote://entry/ENTRY_UUID)
```

The editor may show friendly wiki syntax, but the saved model must preserve the ID. Export rewrites internal links into portable relative Markdown paths. Escape titles and labels correctly.

A renamed entry keeps working references. A trashed/missing target remains visible as an unresolved reference; never silently link to another entry that happens to share its title.

Backlinks include note sections, relationships, schemes, claims, and events referencing the entry. Backlinks are derived/indexed references, not duplicated copies of the source prose.

Do not execute HTML or scripts inside notes. Do not automatically fetch remote Markdown images. Source URLs, `content:` URIs, attachment IDs, and internal links need distinct handling and validation.

### Saving and editing conflict rules

- Persist drafts through a serialized writer per entry; use a short debounce, approximately 300–600 ms, and flush on explicit navigation/Done when possible.
- Update the UI to `Saved` only after the current edit sequence has committed; an older asynchronous save must not mark a newer draft saved.
- Keep a recoverable draft after storage failure and offer retry/export text.
- Separate note edits from board movement so dragging a placement cannot write an old entry snapshot over newer text.
- On any revision conflict, preserve both versions and surface recovery instead of silently choosing a winner.
- Test long notes, rapid typing/navigation, app backgrounding, and repeated open/close cycles.

---

## 08. Canvas, placements, and relationships

### Separate content from presentation

A board placement points to a lore entry. Its position, dimensions, display mode, accent color, collapsed state, and group membership belong to the board, not to the entry.

The same entry may be placed on multiple boards, and optionally multiple times on the same board. Each placement has a different ID. Editing shared lore updates all placements; moving one placement does not move another.

`Remove from board` removes only that placement and its local incident edges. `Move entry to trash` is a separate global action with an impact warning. Deleting a board must not delete its lore entries or semantic relationships.

Quick sticky notes are allowed for unstructured planning. They begin as board-local content or an inbox note with a clearly documented representation. Converting a sticky into a lore entry preserves its text, position, color, and incoming/outgoing board references. Do not convert by discarding and recreating the visible card.

### Required canvas actions

- Create a new entry/card at a visible chosen location.
- Place an existing entry from the library.
- Tap a card to select/open details; expose a clear edit action.
- Drag a card using a movement threshold; tapping must not accidentally move it.
- Pan blank space with one finger; pinch to zoom around the gesture centroid.
- Zoom buttons, reset zoom, fit selection, and fit visible board.
- Connect selected cards and tap a line to edit its relationship/annotation.
- Add a child/branch near the selected card without overwriting existing positions.
- Select multiple placements through an explicit selection mode; move, group, or remove them together.
- Create named groups, collapse/expand them, and move a group with its placements.
- Focus view: selected entry and its one-hop/two-hop neighbors.
- Filter visible content by type, tag, relationship type, epoch, canon status, and story milestone when supported.
- Save board filters and viewport. A hidden-by-filter placement is not deleted.
- Undo/redo meaningful local actions, including movement, grouping, card removal, and edge changes.
- Provide an outline/list view and a simple optional automatic layout with a preview and undo.

Display lower detail when zoomed out: icon/title, then summary/image at closer zoom. Never render a full biography into a tiny card. Do not load every attached full-resolution image when zooming out.

### Coordinate contract

Keep model coordinates in board/world units and rendering coordinates in pixels/dp with explicit conversions. Use sufficient precision for large pans. A canonical transform is:

```text
screenPoint = worldPoint * scale + translation
worldPoint  = (screenPoint - translation) / scale
```

For zoom around screen point `c`, derive the new translation so the same world point stays under `c`. Include pan delta explicitly when the centroid moves. Unit-test transform inverses, centroid anchoring, hit testing at different scales, and density conversion.

Clamp zoom to a documented useful range, such as 0.15×–4× initially; support bounds expansion only after testing. Disable accidental canvas rotation. Compose's multitouch documentation covers transform gesture handling, but application-level drag/selection/pan arbitration remains this project's responsibility. [S8]

Use an explicit gesture state machine: idle, pending tap, dragging placement, panning, pinching, connecting, multi-select. Adding a second finger cancels ambiguous single-finger tap/drag actions cleanly. Back cancels a pending connection before leaving the board.

Persist one coherent movement command on gesture completion; do not write to Room on every pointer frame. Cancelled gestures and process recreation must not corrupt placements.

### Semantic relationships versus drawn lines

A semantic relationship belongs to the world and has stable source/target entry IDs. A board edge is one visual occurrence of that relationship between selected placements. Several boards can show the same relationship.

A board-only annotation line is also allowed, but it must be labeled as local and must not appear as world lore unless promoted deliberately.

Relationships need:

```text
Type/name, source, target, direction/symmetry
Short label and detailed Markdown note
Motivation/reason and optional public explanation
Optional validity span or epoch/era association
Optional canon status and links to claims/secrets
Created/updated metadata and revision identity
```

Seed editable types: allies with, opposes, belongs to, leads, founded, created, worships, secretly funds, manipulates, contains, originates from, caused, inherited from, survives as, and custom.

`A trusts B` must not create `B trusts A`. Symmetric relationships are explicit. Permit multiple relationships between the same pair—siblings, rivals, and secret collaborators are different records. Do not reproduce the old importer behavior that deduplicated every pair regardless of meaning.

A plain board link should not silently assert a world fact. Show a choice between `Lore relationship` and `Board annotation` when appropriate, with a convenient default.

Collapsing a group may aggregate boundary links visually, but it must not create fake relationships or lose the original endpoints. Allow inspecting the underlying connections.

---

## 09. Images, attachments, and references

Support entry cover images and image galleries. Import through the Android photo/document picker. Copy selected files into app-private storage so a moved source file or expired external URI permission does not break the entry.

Record attachment ID, original display name, MIME type, dimensions, byte size, checksum, local relative path, caption, and optional source URL/credit. Use generated safe filenames; do not treat the original filename as a path.

Generate thumbnails off the main thread. Correct orientation, downsample previews, enforce documented import size/pixel limits, and handle malformed images without crashing. Preserve an original copy when accepted rather than silently overwriting it with a thumbnail.

The app must let the user export its attachments and include them in complete backups. Report missing/corrupt attachments in the UI and backup validation.

Deleting a gallery link must not immediately delete a file still used elsewhere or referenced by a recoverable revision. Use explicit references and safe garbage collection after the trash/retention policy permits it.

Reference URLs are metadata. The app must not fetch them, scrape external lore, or upload images without a separate future feature and explicit user action.

---

## 10. Search, capture, and library organization

Search the selected whole world, not only the current board. Index titles, aliases, summaries, note bodies/sections, custom text fields, tags, relationship labels/notes, period/event descriptions, and knowledge/claim notes.

Use a maintained database search index with parameterized queries. Choose an SQLite full-text implementation compatible with the selected Room/SQLite stack; do not assume a particular FTS extension is available on every supported configuration. Provide Unicode-aware behavior and document tokenization limitations.

Results show the matching field/section and enough context to identify the entry. Tapping opens the right record/section. Do not duplicate results for every board placement of the same entry.

Required filters: type, tag, canon status, writing status, epoch/era, has image, and archived/trashed visibility. Exclude trash by default. Save frequently used filters.

Provide favorites, recent entries, and a quick-capture inbox. Quick capture should save an idea with a title or the first line of text; categorization and board placement can happen later. Add an optional share-text-to-inbox intent only after validating the intent and keeping it local.

Search respects active world and history perspective. When a restricted/history view is active, hidden claims must not leak through snippets, autocomplete, counts, backlinks, or relationship labels. A restricted reading view is an authoring aid, not multi-user access security.

---

## 11. Epoch Codex: a primary feature

An epoch is both a **lore entry** and a **historical period**. It must not be implemented as a tag or a wide colored rectangle with no details.

Default hierarchy:

```text
World chronology
  └── Epoch
       ├── Era
       │    └── Events associated with that era
       └── Events associated directly with the epoch
```

The era level is optional. Users can rename the displayed terms to Age, Cycle, Dynasty, Calamity, or other names without changing IDs or breaking relations. Use an acyclic period-parent hierarchy, separate from ordinary semantic links.

Do not make event records owned by only one epoch. An event exists once and can be associated with multiple periods/regions, with an optional primary display placement. A long war can cross an epoch boundary. The same event must not acquire duplicate notes when shown in several histories.

### Epoch fields and sections

| Section | Contents |
|---|---|
| Identity | Title, aliases, icon/symbol, cover image, summary |
| Boundaries | Start and end expressions, opening/closing event links, approximate duration |
| Defining character | What distinguishes this age from the periods before and after it |
| Geography and environment | Continents/realms, climate, transformations, habitability |
| Civilizations and ordinary life | Dominant societies, economies, social order, daily conditions |
| Power and belief | Gods, religions, rulers, technology, cultivation/magic traditions |
| Major developments | Wars, discoveries, migrations, extinctions, ascensions, disasters |
| Important entries | Characters, factions, places, creatures, artifacts, systems |
| Legacy / What remains? | Ruins, bloodlines, artifacts, curses, monsters, surviving institutions |
| Historical knowledge | Official account, alternative accounts, evidence, missing records |
| Author planning | Unresolved decisions, canon status, writing status |

Only the epoch name is required. A user can create `The Forgotten Epoch`, leave both boundaries unknown, write several paragraphs, connect its relics, and position it between other epochs using order-only constraints.

Allow creation of an era, event, linked entry, or legacy relationship from inside an epoch page. Give the user a clear path back to the containing epoch after opening a linked entry.

### Period structure and overlapping histories

A world can contain a global chronology plus regional/civilizational tracks. Two regions can have overlapping named ages. Do not reject all overlapping epochs by default.

For REQUIRED scope, support a `track` or `scope` field and render overlap lanes. The default track is Global. Optional stricter track settings may warn about overlaps/gaps, but world chronology should not require every year to be assigned to exactly one epoch.

An era's structural parent must not form a cycle. When dates conclusively place an era outside its parent, surface a diagnostic and require the owner to fix it or explicitly mark the association disputed/incomplete. Unknown dates alone are not an error.

A boundary event can end one period and begin another. Link to that event rather than copying its date into several independent fields. Changing the source event should update derived boundary labels and diagnostics.

---

## 12. Chronology model: exact, uncertain, relative, and unknown

Build this as a pure Kotlin domain subsystem with dedicated tests. Do not base fictional chronology on Android's Gregorian date picker, Unix milliseconds, the phone's current date, or floating-point year values.

### 12.1 Time basis

For the first required implementation, use **year-level chronology** with signed 64-bit integer year coordinates. Store narrative sequence separately for multiple events within the same year. Day/month/custom-calendar conversion is EXTENDED scope, not a hidden Gregorian assumption.

- Support at least −1,000,000,000 through +1,000,000,000 canonical year coordinates; allow a wider safe range if overflow checks are complete.
- Internal coordinate zero is valid and is an author-defined origin, not AD/CE year zero.
- Default origin/present anchor label: `Present`. Its coordinate starts at 0 and does not advance with the real-world clock.
- Negative numbers can display as `100,000 years before Present`; positive numbers can display as years after the origin.
- World real-world metadata (`createdAt`, `updatedAt`) uses ordinary timestamps. It must never double as fictional time.
- A selected epoch's local year numbering can start at 1. If its exact start is −10,000, then local Year 412 is −9,589: `−10,000 + (412 − 1)`.
- An unknown epoch start leaves its local-year event unresolved globally while still displaying `Year 412 of …`.
- Store serialized 64-bit chronology values as decimal strings in portable JSON, or otherwise enforce and test exact round-trip beyond JavaScript's safe integer range. The format must document the choice.
- Origin renaming changes a label, not stored history. Changing a fixed anchor or rebasing chronology requires an explicit operation and impact preview; never reinterpret existing years silently.

### 12.2 Expressions

Use a sealed domain representation or equivalent validated tagged model. Suggested conceptual variants:

```text
ExactYear(year)
ApproximateYear(centerYear, toleranceYears?)
UncertainYearRange(earliestYear, latestYear)
OffsetFrom(anchorReference, minimumOffsetYears, maximumOffsetYears, qualifier)
WithinPeriod(periodEntryId)
UnknownTime(displayLabel?)
```

`anchorReference` can refer to a fixed world anchor, an event start/end, or a period start/end. Optional `EpochLocalYear(periodId, localYear, numberingBase)` can normalize to an offset while preserving readable input/provenance.

Before/after-only information is represented by explicit ordering constraints, not invented numeric offsets:

```text
After(eventA, eventB)      // A follows B, but distance may be unknown
Before(eventA, eventC)
```

Examples the UI must accept:

| User intention | Representation / behavior |
|---|---|
| Year 412 of the Fourth Epoch | Local-year/offset reference; global position only when anchor resolves |
| Around 100,000 years before Present | Approximate estimate; visible uncertainty |
| Between −5,200 and −5,000 | Uncertain occurrence window, not a 200-year war |
| 200 years after the Shattering | Offset from the Shattering's chosen boundary |
| After the First Ascension, before the Divine War | Two ordering constraints; no invented date |
| Sometime during the Lost Epoch | Within-period reference; no fabricated midpoint |
| Time unknown | Valid unscheduled item with a note |

`ApproximateYear` without a tolerance does **not** mean an exact center with zero uncertainty. It can display a clearly approximate marker at the stated estimate, but it supplies no fabricated numeric lower/upper bounds for hard ordering conclusions. Asking for an optional tolerance in an advanced control is acceptable; requiring one is not.

### 12.3 Uncertain occurrence versus duration

Keep these distinct:

```text
Event occurrence uncertain between −5,200 and −5,000:
    occurrence = UncertainYearRange(−5200, −5000)
    duration is not asserted

War began in −5,200 and ended in −5,000:
    span.start = ExactYear(−5200)
    span.end   = ExactYear(−5000)
    duration = 200 years under the documented boundary convention
```

A period has a start expression and an end expression. An instantaneous/year-positioned event has one occurrence expression. A duration event has a span. Do not infer event duration from uncertainty width.

Use half-open period intervals `[start, end)` when endpoints are exact. A boundary event at year 0 can close the previous epoch and open the next without forcing duplicate ownership. Explain that this is a year-coordinate convention, not day-level historical precision. Treat an event explicitly linked to both epochs as a deliberate association even when its point belongs to only one default containment interval.

### 12.4 Resolution

Return a structured result rather than a nullable number:

```text
ResolvedTemporalPosition:
    earliestPossibleYear?   // inclusive when numeric
    latestPossibleYear?     // inclusive when numeric
    estimatedYear?          // only a stated/derived estimate, never manufactured
    precision
    status: EXACT | BOUNDED | APPROXIMATE | ORDER_ONLY | UNRESOLVED | INVALID
    provenanceReferences
    diagnostics
```

Use separate resolved span endpoints; do not flatten a duration into one date. Preserve original input expressions in addition to any derived cache.

When the anchor is bounded `[a, b]` and a numeric offset is `[c, d]`, a valid propagated bound is `[a + c, b + d]`, with checked integer arithmetic and the original qualifier/provenance retained. Propagate unknown bounds as unknown. A cache must be invalidated when any dependency changes.

A `WithinPeriod` expression inherits a possible occurrence window only when the period bounds make that conclusion valid. For exact integer year bounds `[s, e)`, possible contained year coordinates are `s` through `e−1`. Handle overflow and empty periods explicitly. Do not make within-period membership set a specific occurrence year.

### 12.5 Dependency and consistency validation

Detect reference cycles in computed anchors separately from chronological order contradictions.

Examples:

- Event A's date is based on B, while B's date is based on A: cyclic date dependency.
- A precedes B, B precedes C, C precedes A: cyclic order constraints.
- An exact period start is after its end: impossible span.
- A child's exact span lies outside its parent's exact span: containment diagnostic.
- Numeric dates and an explicit before/after relation cannot both be true: contradiction.
- An approximate date overlaps another estimate: not automatically a contradiction.
- A historically disputed account conflicts with the author's canonical date: an intentional alternative account, not corruption of canonical chronology.

Reject malformed typed input and broken/cyclic canonical computations. For incomplete or potentially inconsistent authoring data, use a reviewable diagnostic with an explicit unresolved/disputed state rather than silently changing dates. Distinguish fatal import corruption, a definite logical conflict, and an informational warning.

Do not sort undated items by creation timestamp and imply that it is historical order. Use a topological ordering for order-only constraints and a clearly separate undated/unplaced section. Equal-year events can use their explicit sequence without changing their stored years. A manual display order is allowed, but label it as presentation order and never convert it into historical facts.

### 12.6 Same-year ordering and typed boundaries

Year-level precision does not establish the exact day or instant of an event. Two events in the same known year may have a valid explicit before/after order. Keep that sequence as a constraint; do not invent fractional years or declare a contradiction merely because their year coordinates are equal. A definite conflict requires incompatible known year order or an actual constraint cycle. Add dedicated tests for this case.

Order-constraint endpoints may reference event occurrences, duration starts/ends, or period starts/ends. For non-overlapping adjacent periods, `previous.end <= next.start` is a valid boundary relationship; distinguish it from strict event sequence. State the selected boundary in relative-date UI labels, such as `200 years after the end of the Shattering`, instead of silently choosing start or end for a duration.

The half-open interval convention above governs period containment and displayed durations at the chosen year-coordinate resolution. It does not manufacture finer historical precision for events within a year.

### 12.7 Timeline rendering precision

Never multiply a billion-year absolute value directly into a giant canvas Float coordinate. Subtract a nearby viewport origin in a sufficiently precise representation before converting a small visible delta for rendering. Test large negative years and narrow zoom windows.

Unknown open endpoints use visual open caps/arrows or an `unknown` label. Approximate boundaries use dashed/hatched treatment. Text and icons explain uncertainty; color alone is insufficient.

---

## 13. Timeline UX: epochs, events, and reveals

The Timeline root contains three tabs:

```text
Epochs | Events | Reveals
```

### Epochs tab

Default to a vertical, expandable sequence of readable period cards. Each card includes title, date/duration label when known, a short description, scope/track, optional cover thumbnail, and a clear uncertainty/knowledge status.

Expand an epoch to show its eras and selected important events. Tap the title or explicit details action to open the full Epoch Codex page. Preserve expansion and scroll position when returning.

Provide two modes:

**Overview:** periods receive enough screen space to read. A 90,000-year epoch and a 200-year epoch can both be inspected. Show `Not to scale` prominently. Never imply equal duration through unlabeled equal-size bars.

**Scaled:** use actual resolved year coordinates on a zoomable axis. Show duration spans, uncertain windows, shared boundary events, and overlaps. Unresolved periods/events appear in a separate accessible tray/list; do not put them at year 0. A user-stated approximate center may be shown with an explicit approximate marker, not as a precise date.

Provide jump to epoch/event, fit selected period, zoom controls, and a switch back to the readable list. Tiny periods need a minimum interactive hit area or an index/list entry without misrepresenting their drawn duration.

### Events tab

Show a searchable chronological list with filters for epoch, era, track, participant, location, event type, and author/recorded history view. Group unknown and order-only events meaningfully. Distinguish a year-precise event, an uncertain point, and an actual duration.

Creating an event from inside a period pre-associates that period but does not invent a date or copy an arbitrary midpoint. Entering a concrete date can suggest possible period membership without silently overwriting an explicit association.

### Reveals tab

Show story structure and when information becomes available to the reader/player. This is not a second historical year axis.

Support an initial hierarchy such as:

```text
Storyline → Arc → Chapter → Scene / reveal beat
```

Allow skipping levels and renaming labels. Use stable IDs and explicit sibling order keys; changing chapter titles or moving Chapter 7 must not break links.

A reveal can refer to an event, entry section, claim, secret, clue, or relationship. The same historical event can have several reveal beats:

```text
Prologue: first strange clue
Chapter 2: an official explanation
Chapter 7: the true explanation
```

Record the information layer being revealed, its audience, and an optional note. A reader reveal does not automatically update every character's knowledge.

Changing a chapter's position affects story order only. It does not move an ancient war to another epoch or rewrite world dates.

---

## 14. Period-specific world states and legacy

Linking an entry to an epoch must support both a simple association and a substantive historical state.

### Historical states

Each explicit state belongs to an existing entry and records:

```text
Entry ID
Period association and/or validity span
Title/summary for this state
Markdown description
Optional field overrides, historical aliases, image, and relationship references
Source/provenance and canon status
```

Example for a single location:

```text
First Epoch: a temple-city devoted to a living god.
Second Epoch: occupied and turned into a fortress.
Third Epoch: buried beneath an expanding wasteland.
Present: an excavation site whose inhabitants misunderstand its purpose.
```

These are historical states of one location, not four copies of it. A historical alias such as `The Bright Sanctuary` can be indexed and displayed with its period without renaming the present-day entry globally.

For REQUIRED scope, show a `History` tab and period-context views of explicitly authored states. Do not infer a complete world's past automatically from current fields or assume every unrecorded property stayed constant.

Use the base entry plus a clearly identified explicit state for a selected period. Where multiple states overlap, show them and ask the author to resolve selection/priority when needed; do not silently choose the most recently edited state. Label missing historical information as `No state recorded for this period` rather than presenting the current state as historical fact.

Relationship validity uses the same chronology/span system. An alliance can end and an enemy relationship begin without rewriting the earlier relationship. Open-ended validity is allowed.

### What remains?

The epoch's Legacy section lists linked surviving consequences: ruins, artifacts, bloodlines, institutions, curses, monsters, religions, technologies, and unfinished schemes.

Use real relationships with explanations, for example:

```text
Ancient experiment → created → inherited curse
Inherited curse → shaped → modern faction
Modern faction → instigated → present conflict
```

Provide a filtered legacy graph or focused board so the owner can follow an ancient cause into a current conflict. Do not implement legacy as a disconnected text checklist with no entry links.

---

## 15. Secrets, claims, knowledge, and recorded histories

Separate three dimensions:

1. **Authoring state:** Has the owner decided the truth? Is this draft or canon?
2. **In-world epistemic state:** What does a person, faction, religion, or public account believe?
3. **Narrative disclosure:** When does the reader/player encounter that information?

A mystery in the story is not the same as an unanswered design question in the author's notes.

### Claims and secrets

Represent a claim/secret as an identifiable content record linked to entries/events/relationships. A practical model includes:

```text
ID, world ID, title
Subject entry/relationship references
Statement / Markdown content
Author truth state: CONFIRMED | FALSE_IN_CANON | UNDECIDED | ALTERNATIVE
Information role: FACT | BELIEF | RUMOR | DELIBERATE_LIE | SECRET
Supporting/refuting clues and source references
Optional historical validity
Optional reveal links
```

A secret is not necessarily a false statement. A rumor may be true. `Unknown to the public` does not mean `undecided by the author`.

Keep broad author/public note sections simple in the editor, but back important reusable facts with identifiable claim records so they can be linked to knowledge and reveal beats.

### Knowledge ledger

For a character, faction, or other knowledge holder, record an explicit state toward a claim:

```text
Knows / Believes / Suspects / Rejects / Has not encountered / Unspecified
First learned/suspected at a linked event or story beat
Optional end/revision beat
Evidence/source and notes
```

Absence of a ledger record means `Unspecified`, not proof that the character does not know. A faction knowing something does not automatically grant that knowledge to every member. A lie's target believing it is separate from its author intentionally spreading it.

Basic REQUIRED UI: open a claim and see who knows/believes/suspects it; open a character and see their knowledge entries and linked beats. The fully filtered `view as this character at this chapter` reader is EXTENDED, but the core ledger and reveal links must already exist.

### Author's History versus Recorded History

A history perspective/account has a name and optional owner (culture, religion, faction, chronicler). It can contain:

- Inclusion/exclusion choices for epochs/events.
- Public titles or aliases.
- Alternative summaries, claimed dates, and links to supporting evidence.
- Claimed completeness/status, such as documented, disputed, mythical, or erased.

Keep canonical author dates intact. Store alternative claimed chronology in the account; switching perspectives must not rewrite the underlying event.

Required basic views:

```text
Author's History
Recorded History — selected account
```

An omitted epoch is omitted in a recorded account, not deleted from the world. Do not show a conspicuous `SECRET EPOCH HIDDEN HERE` marker in a supposedly clean reader export unless the author explicitly requested such a placeholder.

The writer may deliberately display gaps or disputed periods in an account. Implement this as authored account content, not a leak from the hidden canonical chronology.

### Safe presentation boundaries

A local reading perspective is not a login/security boundary. The device owner still has access to the full database. Label it as an authoring/filtering tool, not encryption or role-based access control.

Before filtered views or exports, build a sanitized projection that excludes hidden fields, relationships, filenames, attachment captions, backlinks, counts, and metadata. Do not merely hide widgets after fetching/rendering all secrets. See export tests below.

---

## 16. Schemes, mysteries, clues, and arcs

These use reusable lore entries with structured linked sections, not separate isolated mini-apps.

### Schemes

A scheme records planner, intended outcome, motive, public cover story, stages, dependencies, resources, manipulated people/factions, assumptions, contingencies, failure points, and consequences.

Separate **planned execution** from **actual events**. A stage may succeed for an unintended reason or fail while the planner believes it succeeded.

Each stage can link to required events, participating entries, claims the planner relies on, and actual outcomes. Show stage state such as planned, attempted, succeeded, failed, changed, or unresolved by the author. Do not treat a narrative stage state as a project-management task for the software itself.

### Mysteries and clues

A mystery contains the central question, optional author answer, competing hypotheses, evidence, deliberate misdirection, witnesses, and reveal plan. A clue can support one hypothesis while misleading a character toward another.

Evidence, interpretation, and canonical truth remain distinct. Linking a clue must not automatically mark a hypothesis true. A red herring can still be a real object/event with a misleading interpretation.

### Character arcs

Track starting belief/desire, pressures, meaningful decisions, turning points, changing loyalties, and resulting belief. Link each turning point to actual events and/or story scenes.

A character's power progression or alternate form can be represented using entries/relationships and explicit historical states. A specialized incarnation/form viewer is later scope; no gacha mechanics or game balance system is required in this writing app.

---

## 17. Canon, revisions, trash, and consistency review

Canon status and writing status are independent:

```text
Canon: UNDECIDED | CANON | NON_CANON | ALTERNATIVE
Writing: IDEA | DRAFT | NEEDS_DEVELOPMENT | REVIEWED | COMPLETE
```

Allow configurable labels, but preserve the distinction. A `Needs development` character can be canon. A completely written discarded ending can be non-canon.

Provide an explicit lock/protect action for approved entries. Editing locked content requires `Create revision / Unlock for editing`; do not silently reject edits or let a background template change modify protected canon unnoticed. Unlocking itself should be explicit, not a password/security claim.

### Revisions

- Keep recoverable snapshots at meaningful edit sessions/checkpoints, not one full world copy per keystroke.
- Separate board-layout undo from persistent lore revision history.
- Restore a revision by creating a new current revision, preserving history and stable entry identity.
- Preserve referenced attachments needed by retained revisions.
- Show basic revision timestamp and changed sections; sophisticated word-level diffs are extended scope.
- Make retention limits visible and configurable. Do not silently purge snapshots while telling the user they can always recover them.

### Trash

Moving an entry to trash hides it from default views but retains its identity and recoverable references. Board placements can show an explicit missing/trashed state when inspecting affected boards. Provide an impact summary and restore action.

Removing a board placement is not trashing the entry. Deleting a board is not deleting its content. Purging an entry permanently is a separate confirmed action that handles all incoming references and historical data explicitly.

### Consistency review

Implement deterministic checks, not an AI that invents corrections:

```text
Broken internal links / missing attachments
Cyclic period hierarchy or computed-date dependencies
Impossible spans and contradictory before/after constraints
Duplicate candidate titles/aliases (warning, never auto-merge)
Orphaned board edges / invalid cross-world references
History states with conflicting coverage
Unresolved questions and secrets with no planned reveal (optional authoring warnings)
```

Diagnostics need severity, affected IDs, an explanation, and a link to the relevant editor. The app must never silently rewrite canon to remove a warning.

---

## 18. Data model and integrity contracts

Use normalized relational storage for identities, joins, chronology references, placements, and metadata. JSON is appropriate for versioned custom-field values, typed expression payloads, and portable export—not for storing the entire live world as one opaque database value.

The following is a conceptual model. You may combine implementation tables when it improves simplicity, but preserve these ownership and identity rules. Explain any significant departure in `docs/DATA_MODEL.md`.

| Model | Purpose / important references |
|---|---|
| World | Workspace identity, settings, fixed chronology origin, terminology |
| Entry | Stable lore identity; type, title, body/sections, independent status fields |
| EntrySection | Ordered named content, Markdown/text value, explicit sharing/perspective metadata |
| Template / FieldDefinition | Editable forms with stable field IDs and versioning |
| EntryFieldValue | Typed custom values; references use stable IDs |
| Alias / Tag / EntryTag | Searchable alternate names and organization |
| Relationship | World-level source/target entry IDs, direction/type, note, validity |
| RelationshipType | Editable relationship semantics and default direction |
| Board | World-level view, saved filters, viewport, display preferences |
| BoardPlacement | Board-local geometry/display; references entry or explicit sticky content |
| BoardEdge | Placement endpoints and either relationship ID or local annotation content |
| BoardGroup | Board-local hierarchy/group membership and collapsed state |
| Attachment / AttachmentLink | Managed file metadata and references from records/sections/revisions |
| PeriodDetails | Epoch/era entry extension, parent period, track, start/end expressions |
| ChronologyTrack | Global or regional/civilizational timeline grouping |
| EventDetails | Event entry extension; occurrence or span, participants/location |
| TemporalExpression | Typed original expression and dependency references |
| OrderConstraint | Explicit chronological order independent of numeric dates |
| PeriodAssociation | Links an entry/event to one or more periods, with role and optional primary placement |
| HistoricalState | Explicit past state of an entry, period/span, description/overrides |
| StoryNode | Storyline/arc/chapter/scene tree, stable identity and sibling order |
| RevealBeat | Target information, story node, audience, type of disclosure, note |
| Claim | Identifiable statement, truth state, information role, subject/evidence refs |
| KnowledgeState | Holder, claim, epistemic state, start/end event or story beat, notes |
| HistoryAccount | Named recorded-history perspective and optional owner |
| AccountAssertion | Account-specific inclusion, title/summary/date claim, evidence |
| SchemeStage | Scheme entry, ordered stage, prerequisites, planned/actual refs |
| Revision / Draft | Recoverable authored content, edit sequence, attachment refs |
| InternalReference | Derived link/backlink index with target identity and source section |
| ImportBatch / LegacyMapping | Input hash, provenance, old-to-new IDs, migration completion |
| SavedFilter / Favorite | User organization, scoped to a world |
| Diagnostic | Derived validation result, affected IDs and severity |

### Required invariants

- Stable IDs use UUIDs or an equivalent collision-resistant mechanism; names are never primary keys.
- Enforce that linked objects belong to the same world unless a future explicit cross-world reference type is introduced.
- Use foreign keys/indexes where possible and repository-level validation for typed/polymorphic references. A shared record registry is an acceptable way to validate cross-type IDs; document the chosen representation.
- Enforce typed references: a relationship endpoint must be an entry, a board edge endpoint must be a placement on that same board, and a period parent must be a period.
- General lore graphs may contain cycles. A conspiracy graph is not required to be a tree. Only structures that semantically require acyclicity—period parentage, computed-date dependencies, story parentage, and strict chronological order—must reject cycles.
- Global relationship deletion and board-edge deletion are separate commands. Removing one visual edge must not erase the world relationship from other boards.
- Current lore, historical state, recorded account, and drafts must not overwrite each other through a shared text field.
- A computed date is derived data. Preserve the original expression and dependency identity.
- Derived indexes/caches are rebuildable and are not the only copy of authored information.
- Use database transactions for multi-record operations, migration, world duplication, restore, and conversion from sticky to entry.
- Define deletion behavior explicitly. Avoid blanket cascading deletion of lore because its only current board was removed.
- Never use destructive Room migration as a convenient fallback for production/user data. Export schema snapshots and write migration tests. [S7]
- Make import validation and mutation separate stages. A preview must not partially write a world.
- Handle integer overflow, non-finite canvas coordinates, oversized strings/archives, malformed IDs, dangling references, and unknown enum variants intentionally.
- Backup schema version, database schema version, and application version are separate version numbers.

### Concurrent operations and durability

Use serialized/revision-aware writes for the same authored record. Moving a card, changing a filter, or finishing a thumbnail must never restore an old whole-world snapshot over newer writing.

For background import/thumbnail/export work, use lifecycle-aware jobs and appropriate Android task mechanisms. A progress indicator and cancel action must reflect real work. Do not claim a task will continue after process death unless it has a persistent resumable implementation.

Test interrupted operations and partial file writes. Filesystem operations and SQL transactions are not one atomic mechanism: use staging, a small operation journal, safe promotion, and cleanup/recovery so a crash cannot leave a supposedly completed import with missing files.

---

## 19. Backups, restore, portability, and sharing

The owner is building a long-lived lore library. Backup correctness is a release gate, not optional polish.

### 19.1 Complete portable archive

Provide a versioned archive format, for example `.nnworld` for a world and `.nnbackup` for a workspace. The extension is only a convenience; validate the manifest and content.

Suggested layout:

```text
manifest.json
worlds/<world-id>/world.json
worlds/<world-id>/assets/<opaque-safe-file-name>
reports/export-report.json
```

The manifest includes format ID, format version, kind, app version, creation timestamp, included world IDs, and file size/checksum records. Do not include device-private absolute paths, secrets from signing configuration, or machine-specific URIs as a substitute for actual attachments.

`world.json` includes authored entries/sections/fields, templates, aliases/tags, relationships, boards/placements/groups, chronology expressions/tracks/associations, historical states, story structure/reveals, claims/knowledge/accounts, schemes, and attachment metadata. Include recoverable drafts, revisions, and trash in a **complete** backup, or offer an explicitly labeled smaller current-content export separately. Do not call an export complete when it silently discards these records.

Store attachments themselves, not only source-picker URIs. Indexes, thumbnails, and caches can be regenerated; a restore must not require them to interpret the data.

Take a coherent logical snapshot. Coordinate database reading with immutable/reference-pinned attachment files so files cannot be deleted halfway through backup. Avoid copying only a live SQLite main file while ignoring its WAL or concurrent writes. Prefer the portable logical export rather than raw database copying for the user format.

### 19.2 Safe restore

Required restore flow:

```text
Pick archive → inspect/validate → show summary/warnings → choose destination
→ stage files → transact/import → verify → report completion
```

Default to **Import as a new world**. Remap every identity/reference consistently, including inline links, chronology anchors, period parents, knowledge holders, reveal targets, attachments, and revision references. Do not remap only entries and forget the rest of the graph.

A same-identity replacement restore must show the affected world, require explicit confirmation, keep a pre-restore recovery snapshot, and be all-or-nothing from the user's perspective. Merging separate divergent edits is not required in this build; do not pretend collision skipping is a reliable merge algorithm.

Detect future/unsupported format versions before writing. Present a useful error rather than attempting lossy import. For older supported versions, implement explicit format migrations with fixtures.

### 19.3 Archive security and failure handling

- Reject path traversal, absolute paths, duplicate/conflicting normalized paths, symlinks, and entries outside the staging directory.
- Enforce documented limits on decompressed bytes, compressed bytes, number of files, single-file size, nesting, image dimensions, and parser resource use.
- Check available storage with a safety margin and handle mid-operation disk-full failures.
- Stream large data/assets rather than loading the entire archive into memory. A progress counter must not assume the archive's claimed sizes are trustworthy.
- Verify manifest hashes and byte counts before committing a successful restore.
- Never execute content from an archive, interpret lore text as shell commands, or follow external file references automatically.
- Preserve the original source archive and provide a clear failure report. On cancellation/failure, remove temporary data without touching existing worlds.

### 19.4 Backup UX and retention

Show last successful external backup time and whether edits occurred afterward. A failed or cancelled backup must not advance that timestamp.

Offer manual full backup prominently. Internal recovery snapshots help with corruption but are not protection against uninstall/device loss; explain this distinction in Settings.

Default to no silent cloud backup. Configure and document Android backup/data-transfer rules and their platform limitations; do not claim control over every OEM transfer behavior. The app must not upload lore itself.

Complete archives are **not encrypted by default**. Label them as containing all author notes and secrets. Do not claim that a checksummed ZIP or app-private storage is end-to-end encryption.

An optional user-selected external scheduled backup destination belongs to extended scope. Ask for a destination through the system picker and explain that some providers may be cloud-backed. Do not choose one on the owner's behalf.

### 19.5 Markdown world-bible export

Provide both:

1. One combined readable Markdown document with a contents section and stable anchors.
2. A folder/ZIP of linked Markdown entries plus images and indexes.

Include typed entry sections, relationships and their notes, epochs/eras/events, date uncertainty, legacy, historical states, schemes, and author planning material in the author edition. Use portable relative image and note links. Preserve Unicode and escape Markdown safely.

Use safe filenames with an ID suffix to avoid collisions; do not assume two entries cannot share a title. A Markdown export is for reading/editing in other tools, not guaranteed to restore full editable board layout or structured chronology. Label that limitation.

### 19.6 JSON Canvas interchange

Support import/export against the published JSON Canvas format rather than inventing a similarly named schema. It defines node kinds and edges independently of this app's richer world model. [S9]

Required baseline: text nodes and connections, including complete notes, positions, colors, and labels. Support groups where practical. For file/image/link cards without accessible files or richer unsupported features, show a precise warning and retain a placeholder/raw source description where possible; never silently pretend the import is lossless.

A `.canvas` export represents one board. Rich lore relationships, secret layers, epoch calculations, and full-world state do not fit that format completely. Explain flattening/loss in the export dialog and do not call it a full backup.

Do not fetch external URLs during Canvas import. An Obsidian vault integration/synchronization service is not part of this task.

### 19.7 Public / recorded-history exports

Keep **Full author backup** separate from **Shareable history export**. Default export choice must be explicit, with a warning when it contains private author notes.

Use a central projection/allowlist service before serializing public/recorded history. Only explicitly shared/account-approved sections are included; do not assume an unmarked body is safe. Strip hidden claims and their metadata, omitted events, private alternate titles, internal notes, backlinks, reference filenames, and secret attachment captions.

If a public section explicitly contains a manually typed spoiler, software cannot reliably infer the author's intent. Show a preview and explain that authors must review shared prose. Do not advertise automatic perfect spoiler detection.

Use a regression fixture containing a unique secret token. After exporting a public edition, search every archive member, filename, Markdown body, metadata field, index, and caption to confirm the excluded token is absent. Public export must not be a full author archive with a UI flag saying `hidden`.

---

## 20. Extended features: preserve the design, do not fake completion

Implement these only after REQUIRED scope is working and tested. Keep their schema extension paths in mind without shipping empty navigation destinations.

### WX-01: Geographic maps

Import a map image and place tappable pins linked to location entries or child maps:

```text
World → Region → City → District → Building
```

Store pin positions normalized to the image's own dimensions/orientation, not screen coordinates. Preserve them on zoom/resize. Support nested navigation and Back. Reject accidental cycles in strict map containment while allowing ordinary travel links between places.

This is an image annotation/navigation feature, not a terrain painter. Use managed attachments, thumbnails/tiled rendering as needed, and the same local backup system.

### WX-02: More elaborate calendars

Support named year-numbering systems and eventually custom months/days, leap rules, conversion anchors, and parallel calendars. Keep a display calendar separate from canonical ordering. Never assume months have 30 days or force a Gregorian conversion for fantasy dates.

Before adding conversion, document mathematical semantics, tests, unknown rules, and migration from year-only expressions. Preserve year-level precision; a date recorded only to a year does not become 1 January automatically.

### WX-03: Perspective-as-of reader

Select a character/faction, storyline, and chapter/scene to inspect explicitly recorded knowledge available at that point. Missing knowledge data means unknown to the model, not proven ignorance. Do not infer omniscience from faction membership or a reader's reveal.

Reuse the sanitized projection service and verify search/backlinks/attachments do not bypass the selected view. Label it as a local writing aid, not security isolation.

### WX-04 to WX-06

Dedicated family trees, linked incarnations/forms, external backup scheduling, improved revision diffs, advanced layouts, and larger-scale canvas optimization may be added with tests and documented scope. Do not let these delay usable epoch pages, long notes, or reliable backups.

---

## 21. Performance and robustness targets

These are **engineering targets to measure**, not claims about achieved performance. Record hardware/emulator, build type, data size, timings, and limitations in `docs/PERFORMANCE.md`.

Use three fixture scales:

| Fixture | Suggested content |
|---|---|
| Small demo | 25–40 entries, 3–5 boards, 5 epochs, 6 eras, 20 events, several images |
| Typical world | 2,000 entries, 5,000 relationships, 30 boards, 500 events, long notes |
| Stress world | 10,000 entries, 25,000 relationships, 2,000 board placements on one board, long ancient chronology |

Populate synthetic text/images deterministically. Do not download private/copyrighted lore or require network services to seed tests.

Targets for a documented contemporary mid-range phone class:

- Writing feedback remains immediate while saves happen off the main thread.
- Typical indexed search returns visibly useful first results within roughly 300 ms after the search debounce.
- Common entry navigation is responsive; avoid synchronous full-world loading on open.
- Canvas pan/zoom aims for smooth 60 Hz interaction with hundreds of visible simplified cards, using culling and selective recomposition; measure rather than assert.
- A large note, for example 100,000 characters, can be edited and reopened without truncation or a main-thread freeze.
- A 100,000-year or billion-year chronology does not allocate a pixel per year or lose small visible year differences through Float cancellation.
- Image-heavy boards use thumbnails, bounded caches, and lazy loading.
- Export/restore shows progress and handles cancellation/storage pressure without freezing editing or destroying existing data.

Where stress goals are not met, publish measured limits and optimize the bottleneck. Do not impose the old prototype's 8 MB whole-workspace string ceiling on the new relational design simply because it existed before. Sensible file/import safeguards should protect the device without disguising a tiny database architecture.

---

## 22. Required test plan and acceptance scenarios

Use pure Kotlin unit tests for chronology/graph/export algorithms, repository/Room tests for persistence and migration, Compose/UI tests for interaction, and Android instrumentation/device/emulator checks for lifecycle, system document operations, and APK behavior.

Mock-based tests are valuable but are not proof that the real Android file picker, process lifecycle, signing upgrade, or database driver works. Clearly label each test layer and what ran.

Create `docs/ACCEPTANCE_TESTS.md` with these scenario IDs and outcomes. Add tests for implementation-specific risks as needed.

### Core notes, identity, and boards

| Test | Scenario | Required result |
|---|---|---|
| AT-01 | Create a blank world, character, and long note; restart | Content and world selection persist exactly |
| AT-02 | Place one character on three boards; edit biography once | All placements resolve to the updated same entry |
| AT-03 | Move one placement | Other placements and entry text remain unchanged |
| AT-04 | Remove a placement and then delete its board | Underlying lore and world relationships survive |
| AT-05 | Create two entries with equal titles, then rename one | Links retain correct target IDs; ambiguity is explicit |
| AT-06 | Convert a sticky with links into a lore entry | Text, geometry, and connections are preserved |
| AT-07 | Type rapidly, navigate, background, reopen | Latest acknowledged saved version persists; no stale overwrite |
| AT-08 | Trigger a save failure | Draft stays recoverable; UI does not say Saved |
| AT-09 | Pan, pinch around a point, drag, add a second finger | No unintended tap/drag; centroid/coordinate tests pass |
| AT-10 | Group/collapse/expand, filter, undo/redo | No lost entries/relationships; operations are reversible |
| AT-11 | Create directed and symmetric links, plus parallel relations | Direction and independent relationship notes are retained |
| AT-12 | Edit a relationship on one board | Other views reflect the same semantic record |
| AT-13 | Search an alias and a phrase only inside a relationship note | Correct world-wide results appear without placement duplicates |
| AT-14 | Edit Polish, Chinese, Japanese, emoji, and a 100k-character note | No corruption/truncation; mixed-script navigation works |

### Epochs and mathematical chronology

| Test | Scenario | Required result |
|---|---|---|
| AT-15 | Create an epoch with only a title | Valid useful entry, editable notes, no forced fake date |
| AT-16 | Create a 90k-year epoch and a 200-year epoch | Both readable in Overview; Scaled mode accurately reflects duration |
| AT-17 | Exact, approximate, range, relative, within-period, unknown inputs | Distinct persisted types and truthful labels |
| AT-18 | Approximate center without tolerance | No invented zero-width bounds or false ordering certainty |
| AT-19 | Uncertain occurrence window versus a war span | Different models/rendering; uncertainty is not duration |
| AT-20 | Event 200 years after an event bounded [−5000, −4900] | Derived bounds [−4800, −4700], with provenance |
| AT-21 | Year 412 of epoch starting at −10000, numbering base 1 | Canonical year −9589; unresolved if the start becomes unknown |
| AT-22 | Change a referenced boundary event | All dependent labels/caches recompute; old copied dates are not retained |
| AT-23 | Date dependency cycle, hierarchy cycle, and order cycle | Each is detected with actionable diagnostics |
| AT-24 | Ordinary cyclic faction relationship graph | Allowed; not mistaken for chronology/hierarchy corruption |
| AT-25 | Contradictory exact span; uncertain overlapping bounds | Definite error and uncertain warning are distinguished |
| AT-26 | Event at a shared epoch boundary, and a war spanning epochs | Single event identity and correct explicit associations |
| AT-27 | Local eras overlap across regions | Valid tracks/lanes; no blanket rejection of overlap |
| AT-28 | Only-before/after events with no numeric years | Valid partial order; never placed at zero or sorted as known dates |
| AT-29 | Huge negative year with a narrow visible window | Correct ordering/geometry; no overflow or Float precision collapse |
| AT-30 | Change the phone date/timezone | Fictional history does not move |
| AT-31 | Rename origin/epoch labels | Numeric chronology and stable references do not change |

### History, secrets, and authoring

| Test | Scenario | Required result |
|---|---|---|
| AT-32 | Give one place three period-specific states | Same place identity; selected history shows explicit state |
| AT-33 | Select an epoch with no recorded state for a place | No invented snapshot of the present masquerading as history |
| AT-34 | Add a first-epoch event with Chapter 7 truth reveal | Historical placement and reveal placement remain independent |
| AT-35 | Reorder/rename chapters | Reveal links remain valid; historical dates stay unchanged |
| AT-36 | A public lie, author truth, undecided author question | Three distinct concepts survive editing/export/restore |
| AT-37 | Character believes a false claim; faction knows truth | No automatic knowledge propagation or truth conflation |
| AT-38 | Author has an erased epoch; recorded account omits it | Canonical data stays intact; account view omits it without a leak marker |
| AT-39 | Scheme planned outcome differs from actual result | Both versions are stored and visible |
| AT-40 | Mark entry Canon + Needs development; lock and revise | Independent states, explicit unlock/revision, recoverable history |
| AT-41 | Trash and restore an entry referenced from several places | Identity, links, media, and history recover consistently |

### Migration, backup, privacy, and Android

| Test | Scenario | Required result |
|---|---|---|
| AT-42 | Import the NodeNote v1 fixture | Full note, colors, map geometry, edge label and Unicode preserved |
| AT-43 | Old maps reuse node IDs or equal titles | No unintended cross-map merge; explicit remapping |
| AT-44 | Invalid/truncated/future-version import | Helpful failure; no partial changes to existing worlds |
| AT-45 | Full backup → import as new world | Canonical round-trip matches after ID-remap normalization; files hash-match |
| AT-46 | Restore includes epochs, knowledge, reveals, drafts, revisions, trash | No silent category omission |
| AT-47 | ZIP traversal, duplicate paths, oversize expansion, invalid image | Safe rejection/cleanup; no writes outside staging |
| AT-48 | Cancel import/export and simulate low storage | Existing world intact, no false success timestamp |
| AT-49 | Delete/move original picked image outside app | Managed gallery still opens offline |
| AT-50 | Public export with a hidden unique secret token | Token absent from every exported member, name, index, caption, and metadata |
| AT-51 | Baseline JSON Canvas text/edges export/import | Supported fields preserve content; unsupported types produce warnings |
| AT-52 | Clean native build and signature/package inspection | APK is genuine; expected app ID/version/min SDK/permissions reported |
| AT-53 | Install/launch, keyboard, Back, recreation, local persistence | Android runtime behaves correctly; evidence records device/API |
| AT-54 | Real document picker save/open, cancellation, and provider errors | Native flows work or failures are accurately reported |
| AT-55 | Optional same-signature legacy upgrade with synthetic prefs | Migration is idempotent, old raw data retained, no duplicated worlds |
| AT-56 | Default side-by-side debug install next to old NodeNote | Old app/data untouched; no silent uninstall |
| AT-57 | Airplane mode / network inspection / manifest permission check | Core app needs no network; no unexpected permissions or requests |
| AT-58 | TalkBack, 200% text sizing, narrow screen, dark/light theme | Core actions accessible; no clipped editor controls |
| AT-59 | Database schema migration with prefilled previous-version fixture | Authored data and relationships survive; no destructive fallback |
| AT-60 | Workspace isolation and whole-world duplication | No cross-world search leaks or broken/remnant source-world IDs |

Do not equate `AT-52 passed` with `AT-53 passed`. Cryptographic validity is not Android runtime verification. Do not equate a mocked document provider with a tested system picker.

### Additional test techniques

- Property-based/generated tests for temporal expression serialization, interval propagation, transform inverses, and ID remapping.
- Fuzz/import robustness tests with bounded resource use.
- Crash/interruption simulation around database/file staging boundaries.
- Repeated autosave sequences with out-of-order asynchronous completion.
- Golden export tests for stable Markdown links and filtered projection.
- Room migration tests from every supported on-disk schema version.
- Compose semantic assertions rather than brittle screenshot-only tests.
- A small visual regression set captured from the actual app, not design mockups.

---

## 23. Original demo world and deterministic fixtures

Ship an optional demo world called **The Ashen Meridian**, clearly labeled `Sample world`. It must not be inserted repeatedly into a real user's workspace on every launch. First-run choices are `Create my world` and `Explore sample world`.

Use original placeholder lore. The example is editable and removable, not application logic.

Suggested epochs:

| Epoch | Chronological representation | Demonstrated behavior |
|---|---|---|
| The First Radiance | Start approximately −120,000; end −30,000 | Very long uncertain ancient age |
| The Hollow Crowns | [−30,000, −10,000) | Two nested eras, competing institutions |
| The Lost Interval | After Hollow Crowns, before Ash Dominion; exact span undecided | Order-only and erased history |
| The Ash Dominion | [−9,000, −200) | Linked boundary event and surviving legacy |
| The Lantern Age | Start −200, end unknown | Open-ended present age and current conflicts |

The unfilled gap between −10,000 and −9,000 is intentional and visibly unresolved in the author view; do not secretly assign the Lost Interval those exact bounds. Its order-only information does not establish full coverage of the gap.

Include:

- The Ash Physician: a character who secretly funds the Lantern Order.
- The Lantern Order: a relief institution with a public mission and private archives.
- The Buried Observatory: one location with several historical states and aliases.
- The Glass Plague: an ancient event whose true cause is revealed late in the story.
- The Shattering: an event used as a relative-date anchor.
- An artifact linked to an old experiment and a modern inherited curse.
- A public historical account omitting the Lost Interval.
- A scheme with a planned outcome that differs from what happened.
- A mystery with competing hypotheses and a misleading clue.
- At least one author-undecided question distinct from a secret with a decided answer.
- Multiple knowledge holders with different beliefs about the same claim.
- A Chapter 1 clue, Chapter 2 public explanation, and Chapter 7 truth reveal about one event.
- The same character placed on three boards; multiple different relationships between one pair of entries.

Use simple generated geometric placeholder images or bundled license-compatible assets with credits. Art generation is not required to demonstrate the app. Demo content must not expose real private lore from the user's other projects.

Use a separate deterministic `make_test_world` utility for typical/stress fixtures. Keep test-only hidden tokens and malicious archive fixtures out of ordinary demo content.

---

## 24. Implementation sequence and release gates

Work through these milestones in order, preserving buildability. Once a milestone is complete, continue to the next required one without requesting routine approval. The app is not fully complete until all REQUIRED IDs and the final gate are addressed.

### M0 — Inspect and establish a genuine Android build

1. Read repository instructions and this entire specification. When tool output is truncated, read the file in additional chunks; do not assume the visible prefix is the whole document.
2. Inventory existing source, test fixtures, APK identity, local Android SDK/JDK, Gradle, emulator, and signing configuration. Record facts rather than guesses.
3. Preserve the old project/reference assets. Create or modernize the standard native Android project without destroying existing unrelated files.
4. Pin and document a compatible toolchain. Commit a real Gradle wrapper and checksums/configuration as appropriate.
5. Implement theme/navigation and a small real local persistence operation.
6. Build a debug APK using the ordinary toolchain. Inspect its manifest/signature. Launch it on an available emulator/device and capture actual evidence.
7. Create the short `AGENTS.md`, `IMPLEMENTATION_STATUS.md`, feature matrix, and build/run documentation.

**Gate:** A real buildable Android app, not merely an Android directory structure. Record emulator/device access separately from compilation success.

### M1 — Persistent worldbuilding library

Implement world isolation, reusable entry identity, optional templates/custom fields, full-screen notes, reliable draft saves, aliases/tags, links/backlinks, basic search, and revision/trash foundations. Add managed image attachments and a minimal complete-content backup/restore path early so writing is protected before the app becomes large.

**Gate:** Create, edit, close/reopen, search, backup, and restore notes/images without content loss. Equal titles and Unicode work. Document and test the first database schema.

### M2 — Native visual canvas and relationships

Implement placements, directed/symmetric relationships with notes, board-local annotation edges, touch gesture state machine, groups, filtering/focus, undo/redo, accessible outline, and simple layout preview. Use the same underlying entries as the library.

**Gate:** Shared-entry editing across boards works; board removal cannot destroy lore; gestures and transform tests pass; board state survives restart.

### M3 — Epochs, eras, and flexible chronology

Implement the Epoch Codex and all required date-expression types, dependency resolution, diagnostics, period hierarchy/tracks, event membership, and both Overview and Scaled modes. Build explicit historical states and legacy links.

**Gate:** A world spanning 100,000 years is usable on a phone. Unknown epochs remain valid. Relative dates recompute. Large-coordinate tests pass. Do not defer this milestone behind optional map tools.

### M4 — Story reveals and intrigue

Implement the stable story outline, reveal beats, claims/secrets, basic knowledge ledger, recorded-history accounts, scheme/mystery/clue/arc structures, independent canon/writing status, locking, and accessible editing flows.

**Gate:** Ancient events and later reveals coexist without conflation. Recorded history can omit an epoch without deleting it. Claims and knowledge survive restart and backup.

### M5 — Complete portability and recovery

Finish the full archive format for every required model, schema/format migrations, NodeNote importer, JSON Canvas baseline, Markdown world-bible exports, sanitized public export, import previews, atomic recovery, and corruption/low-space testing.

Implement the optional same-package upgrade only when the correct reference/signing inputs are available. The ordinary side-by-side app must remain fully usable without this optional variant.

**Gate:** Full round-trip fixtures match structurally after ID normalization, attachment hashes match, a failed import leaves the old world untouched, and private tokens do not leak into public exports.

### M6 — Hardening and delivery

Run the acceptance suite, Android runtime checks, accessibility passes, performance fixtures, airplane-mode checks, real picker operations, schema migrations, and installation/signature verification. Fix failures before spending time on extended features.

Update screenshots from the actual app. Produce artifacts and test reports. Review the feature matrix against this specification, not just against what was convenient to implement.

**Gate:** All required features are implemented and verified to the documented extent, or the remaining gaps/blockers are clearly enumerated without a false completion claim.

### M7 — Extended enhancements

Only after the core release gate, implement extended features in the order that best fits remaining capacity: map pins, more advanced perspective readers/calendars, specialized relationship views, external scheduled backups, and performance refinements.

Do not re-open the core product direction or replace it with a different platform during this phase.

---

## 25. Build, test, signing, and CI requirements

### Build configuration

Supply a complete conventional project including:

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml          // if using a version catalog
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/...                  // Kotlin, resources, bundled demo assets
```

Generate/download wrapper and dependencies from trusted official sources under the environment's permissions. Do not fabricate a wrapper JAR or a DEX file when SDK downloads fail. Do not edit generated binary resources by hand as the normal build process.

A local build may download dependencies at development time; the installed app must not require network access. Document the distinction. Keep repository credentials, SDK machine paths, and signing configuration out of committed public files.

### Standard commands

For a no-flavor setup, the following are expected entry points:

```bash
./gradlew --version
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Windows equivalents use `gradlew.bat`. If modules/flavors change task names, provide and verify the actual commands rather than leaving these examples inaccurate. Add a `scripts/verify.sh` and `scripts/verify.ps1` that run available deterministic checks and fail clearly on errors.

`connected...AndroidTest` needs a device/emulator. Report it as `not run — no device/emulator` when appropriate; do not return success by swallowing the error.

### APK verification

Use the official Android toolchain's inspection/signing tools, for example:

```bash
apksigner verify --verbose --print-certs path/to/app-debug.apk
apkanalyzer manifest application-id path/to/app-debug.apk
apkanalyzer manifest permissions path/to/app-debug.apk
adb install -r path/to/app-debug.apk
```

Resolve tools from the installed SDK and document their exact paths/versions when needed. Use the actual variant's APK path and application ID. Inspect min/target SDK, version, exported components, unexpected permissions, debug status, and signing fingerprint. Do not suppress failures to produce a green report.

Installing a debug build should use a dedicated test identity/device by default. Never uninstall an existing user app or clear its data automatically. If a signing mismatch prevents update, explain why and keep both source data and any existing install intact.

A locally generated debug key is a test key, not production signing. Persist it securely enough to reproduce private test updates in that environment, but do not commit it. A CI build signed with a different temporary debug certificate may not update a locally signed install; report fingerprints and this limitation rather than promising interchangeability. Public release signing is a separate owner-controlled setup.

### CI

Provide an appropriate workflow, such as `.github/workflows/android.yml`, that checks out source, installs the pinned JDK/Android requirements, validates the Gradle wrapper, runs unit tests/lint, builds the test APK, and uploads artifact/test reports. Add emulator instrumentation where the runner supports it reliably, or provide a separate manually triggered instrumentation job.

Use current official action versions pinned appropriately and minimum workflow permissions. Never put private production signing material or lore backups in CI. Store only synthetic test/demo content in the repository. Writing a workflow file does not authorize publishing a repository or triggering external services with user credentials.

A CI workflow that has not run is `provided, not executed`, not `CI passed`.

### Environment blockers

If the Android SDK/build network/emulator is unavailable, record:

```text
Blocked command and exact error
What is installed and what is missing
What source/tests were completed independently
Whether an APK exists from a genuine successful build
Exact local/CI commands to resume
Which runtime checks remain unverified
```

Deliver the complete achievable source and honest report. Do not hide the blocker behind a renamed file or an unsupported claim of device compatibility.

---

## 26. Required project documentation and artifacts

Keep these files current during implementation:

```text
README.md                         What the app does, setup, build, use, limitations
AGENTS.md                         Short repository instructions and specification pointer
IMPLEMENTATION_STATUS.md          Current milestone, verified work, blockers, next exact step
docs/FEATURE_MATRIX.md            WB/WX/FUT IDs, implementation files, tests, status
docs/DECISIONS.md                 Consequential technical/product defaults and rationale
docs/DATA_MODEL.md                Schema, identity, ownership, delete/migration rules
docs/CHRONOLOGY.md                Expression semantics, uncertainty, intervals, examples
docs/BACKUP_FORMAT.md             Versioned schema, safe restore, lossless/lossy distinctions
docs/MIGRATION.md                 NodeNote mappings, package/signing caveats, recovery
docs/BUILD_ENVIRONMENT.md         Exact pinned JDK/Gradle/AGP/SDK/dependency versions
docs/ACCEPTANCE_TESTS.md           AT IDs, commands/evidence, outcomes
docs/TEST_REPORT.md               What ran, what passed, what failed, what did not run
docs/PERFORMANCE.md               Measured fixtures, device/build, limitations
docs/PRIVACY.md                   Offline behavior, local data, permissions, backup exposure
docs/KNOWN_LIMITATIONS.md         Specific real limitations, not generic assurances
```

Do not maintain documentation as an alternative to writing code. Update it briefly as evidence and implementation change.

### Artifact directory

Produce when supported by the actual build:

```text
artifacts/NodeNote-Worldbuilder-debug.apk
artifacts/NodeNote-Worldbuilder-source.zip
artifacts/SHA256SUMS
artifacts/BUILD_REPORT.md
artifacts/test-reports/...
artifacts/screenshots/...
```

The source archive includes source, wrapper/build configuration, schemas, tests, demo fixtures, and documentation. Exclude private keys, credentials, local SDKs, build caches, user lore databases, large generated stress data, and old archives containing signing material.

Copy the APK from a successful conventional build. Calculate its checksum from the actual output. Do not leave an older prototype APK under the new artifact name and call it the new implementation.

Required screenshots, only when captured from the running implementation:

```text
Library with several entry types
Native canvas with shared lore cards and labeled relationships
Full note editor with optional sections
Epoch overview showing a long age and an unresolved epoch
Epoch details with legacy/history links
Scaled chronology with uncertainty/overlap
Reveals view linked to an ancient event
Import/backup validation summary
```

Screenshots from an emulator must be labeled emulator captures. Browser or design-preview renders must not be described as Android runtime screenshots.

### End-of-run report

Report:

1. Implemented REQUIRED/EXTENDED IDs and remaining gaps.
2. Main architectural decisions and any deviations from this specification.
3. Actual commands executed and test outcomes.
4. Device/emulator and API levels actually exercised.
5. APK/source artifact paths and APK signature/checksum details, when available.
6. Migration/backup precautions and concrete known limitations.
7. The next exact implementation step only when incomplete.

Never say all tests passed when only a subset ran. Never say the app is complete when required IDs are still stubs or when relevant error flows simply return success.

---

## 27. Short AGENTS.md and continuation templates

Create a short repository instruction file after inspecting existing guidance; merge respectfully rather than overwriting unrelated rules. Adapt command names to the real project.

```markdown
# NodeNote Worldbuilder — repository guidance

Read NodeNote_Worldbuilder_Codex_Prompt.md for the full product contract.
Read IMPLEMENTATION_STATUS.md and docs/FEATURE_MATRIX.md before continuing.

Critical invariants:
- Offline native Android; no accounts, analytics, or runtime network dependency.
- Lore entries, world relationships, and board placements have separate identities.
- Deleting a board/placement must not delete lore.
- Epochs and flexible uncertain chronology are REQUIRED core features.
- Historical time, author truth, recorded beliefs, and reveals are distinct models.
- Never use destructive data migration; test backup/restore and preserve raw imports.
- No fabricated test/build/device claims; report exactly what ran.

Use the verified build/test commands in README.md.
Keep source buildable, update the feature matrix, and record actual blockers.
```

A continuation instruction for a later Codex session:

```text
Continue implementing NodeNote Worldbuilder. Read AGENTS.md, NodeNote_Worldbuilder_Codex_Prompt.md, IMPLEMENTATION_STATUS.md, and docs/FEATURE_MATRIX.md. Inspect the current code and test results before making changes. Resume from the first incomplete REQUIRED item, preserve completed behavior and user data, run the relevant checks, and update the APK and reports after a successful build. Do not restart from scratch or stop at a new plan.
```

This is a recovery/resumption procedure, not permission for the current run to stop after writing a status file while implementation can continue.

---

## 28. Definition of done

The required app is done when the owner can perform this complete workflow on Android:

> Create a world. Add characters, factions, places, powers, creatures, and detailed notes. Reuse them on visual boards and explain their relationships. Organize more than 100,000 years of history into epochs and eras without fabricating unknown dates. Record how places and factions change over time. Distinguish author truth from public history and track secrets, schemes, character knowledge, and story reveals. Search everything, recover edits, export a readable world bible, and restore a complete backup into a clean installation.

The source builds through a conventional Android toolchain, the APK is genuinely signed, and tests/reports distinguish proven behavior from untested assumptions.

The following are not acceptable substitutes:

```text
A static UI demonstration with fake saved state
A web page offered instead of the requested Android app
An enormous unstructured note field instead of reusable lore
A timeline that requires exact Gregorian dates for ancient history
An epoch tag with no epoch detail page or period structure
One duplicated copy of a character per board
A public export containing hidden author data behind a flag
An APK filename with no successful Android build behind it
A green test report assembled from checks that did not run
```

Start implementation with M0, and continue through the required milestones.

---

## 29. Primary implementation references

These references were checked while preparing the brief on 15 September 2026. They support the platform/tooling approach, not a claim that the custom product design is supplied by those sources. Recheck current stable versions and API details when implementing. URLs are provided as copyable code for the coding agent.

**[S1] OpenAI — Codex project instructions / AGENTS.md.** Guidance discovery and the distinction between concise project rules and a separately read specification. The current documentation redirects to OpenAI's ChatGPT Learn documentation.

```text
https://developers.openai.com/codex/guides/agents-md
https://learn.chatgpt.com/docs/agent-configuration/agents-md
```

**[S2] Android Developers — Guide to app architecture.** Separation of UI/data responsibilities and maintainable app architecture.

```text
https://developer.android.com/topic/architecture
```

**[S3] Android Developers — Room local persistence.** Structured local storage and database tooling. Verify current artifact coordinates/version compatibility rather than copying a remembered dependency version.

```text
https://developer.android.com/training/data-storage/room
```

**[S4] Android Developers — Storage Access Framework.** User-mediated access to documents and files without broad filesystem permissions.

```text
https://developer.android.com/training/data-storage/shared/documents-files
```

**[S5] Android Developers — App signing.** Signing keys, debug/release identity, and upgrade implications.

```text
https://developer.android.com/studio/publish/app-signing
```

**[S6] Android Developers — Command-line builds.** Standard Gradle-based Android build and packaging commands.

```text
https://developer.android.com/build/building-cmdline
```

**[S7] Android Developers — Room migrations.** Schema migration and migration testing; avoid destructive fallback for user-authored data.

```text
https://developer.android.com/training/data-storage/room/migrating-db-versions
```

**[S8] Android Developers — Compose multitouch.** Pan/zoom gesture APIs; supplement with this app's own gesture-state and geometry tests.

```text
https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch
```

**[S9] JSON Canvas — Published specification.** Supported node/edge format for interoperable board import/export.

```text
https://jsoncanvas.org/spec/1.0/
```

**[L1] Supplied NodeNote prototype source.** Local input, inspected for legacy identity, storage, data shapes, and build/test limitations. Reinspect the actual files in your workspace before migrating them.

```text
NodeNote-source.zip
  NodeNote/README.md
  NodeNote/app/src/main/AndroidManifest.xml
  NodeNote/app/src/main/assets/index.html
  NodeNote/app/src/main/java/app/nodenote/MainActivity.java
  NodeNote/tools/app_dex.py
  NodeNote/verification/package-report.json
```

No earlier chat transcript, account connection, paid external service, or private game-design file is required to understand this specification.
