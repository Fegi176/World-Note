# World atlas interface — 0.2.0

An original native Compose design built around the requested cobalt, black and phthalo green palette. No third-party artwork was copied. Vector marks and system type keep the app offline and sharp at different densities; no raster generation was needed for this text-focused interface.

## Design decisions

- Black canvas and restrained green surfaces establish hierarchy; cobalt marks the main creation action. Pale blue links remain readable on dark surfaces. The light theme uses warm paper and green ink.
- Serif page headings distinguish the world atlas from editing controls. Body text uses a familiar sans serif with generous line height.
- Adaptive world header, named navigation tabs and labeled creation actions replace ambiguous glyphs. The header grows with text size; regular navigation targets have a minimum 64dp height. Above font scale 1.3, navigation uses two rows of named targets (minimum 56dp) and a full-width creation action above them.
- Lore cards separate type, title, preview and status. Search combines placement/field matches under their reusable owner, avoiding repeated copies of one character.
- Board tools collapse behind an explicit control. Outline remains a direct action with native entry and relationship controls for keyboard/accessibility navigation. Both canvases clip drawing to their bounds; a pixel assertion verifies zoom leaves the toolbar unchanged.
- Save acknowledgement, protected state and recovery actions stay visible in the editor. Destructive safety-copy removal requires a concrete confirmation.
- Storage & recovery exposes preview cache size, cache regeneration, verified safety archive export/inspection and explicit removal.

## Validation

`DesignInstrumentedTest` checks principal text/background pairs at contrast >=4.5:1 and enables Android's Compose Accessibility Test Framework for search, entry editing, board outline and storage controls. A separate test injects real two-finger gestures and checks that zoom does not move placements or rewrite lore. The native writing test covers 200% font scale. The emulator matrix script separately exercises 360dp / 200% navigation and landscape board/outline; exact execution outcomes are in TEST_REPORT.md.

Automated checks complement, but do not replace, a manual spoken TalkBack review. No such manual review is claimed.

## References

The [Bleu Cobalt editorial portfolio on Behance](https://www.behance.net/gallery/170647655/Bleu-Cobalt) informed the restrained editorial direction; this app uses its own layout, marks and code. Android's [Compose accessibility testing documentation](https://developer.android.com/develop/ui/compose/accessibility/testing) guided the automated checks, and the [native touch-test API](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/package-summary) supplies pointer injection.

## Board images and content — 0.2.1

The board Image action uses Android OpenDocument and creates one reusable Note, its managed attachment/media and a board placement in one database transaction. Rejected insertion removes the newly copied asset. Removing the placement leaves the lore/image available in the Library. Images imported this way participate in complete backup/restore.

Card content settings (cover image, note preview, tags/status) are fields on the placement. Existing compact placements retain their dimensions until the owner enables richer content. Applying choices grows height as needed and participates in board Undo; shrinking is explicit through Card size. Image import itself is a durable creation operation; remove its placement to undo its presence on the board. Underlying lore stays reusable.

Canvas previews use the marked entry cover, falling back to its first available image, and fit the whole image without cropping. Missing previews have a text fallback. Decodes run on IO, use sampled images at most 256 pixels per dimension, an 8 MiB LRU cache, and at most 64 visible unique images at once. Outline exposes ordinary native image/text controls. Long text is wrapped/ellipsized and clipped to each card. Full original notes remain in the editor. The native image/text implementation uses Android BitmapFactory and StaticLayout; no new library/network dependency was added.
