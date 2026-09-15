# Complete backup and restore

Extensions `.nnbackup` (workspace) and `.nnworld` (selected world) are convenience names for a ZIP with a verified manifest. Never trust an extension alone.

```text
manifest.json
preferences.json
worlds/<world-uuid>/world.json
worlds/<world-uuid>/assets/<attachment-uuid>
```

The manifest contains format `nodenote-worldbuilder`, format version 1, kind, app version, export timestamp, world IDs, and SHA-256/byte counts for every other member. Unknown future versions and unlisted/missing files are rejected. Original attachments are included as bytes. The whole authored registry is exported, including templates, fields, links, boards/groups, chronology, states, knowledge, reveals, schemes, accounts, drafts, revisions, and trash. Indexes and thumbnails are rebuildable.

Backups read a coherent Room logical snapshot and immutable attachment files, never a live SQLite main file without its WAL. A timestamp is recorded only after the selected output stream closes successfully. An individual failed provider write may leave an incomplete external document, but cannot advance successful-backup status.

## Restore stages

1. Copy original selected input to private recovery storage.
2. Inspect ZIP central-directory attributes, paths, sizes, hashes, model IDs, typed references, and canonical consistency without database mutation.
3. Show world/record/image counts and warnings; default to new identities.
4. Stage/promote immutable original files with a journal; validate imported image thumbnails.
5. Insert all included worlds in one transaction, or explicitly replace matching worlds after writing a full pre-restore recovery archive.
6. Report success after commit. Cancellation/failure keeps prior worlds and successful backup status intact.

New-world restore remaps all record IDs, world IDs, references, temporal anchors, inline internal links, attachment paths, saved-filter period IDs, template provenance, and embedded draft/revision snapshots. Same-identity replacement rejects collisions with unrelated worlds and conflicting immutable attachment content.

## Limits

- 512 MiB total compressed input and decompressed output.
- 64 MiB per archive member; 10,000 members; no ZIP64/multi-disk/encrypted ZIP support.
- 100,000 registry records per world; 5,000,000 characters per authored body; 10,000 per title.
- Image imports: 32 MiB / 80 megapixels; JPEG, PNG, WebP, GIF. Animated images are previewed as a still image; original bytes remain intact.
- Paths must be relative, slash-separated, bounded, and free of traversal, drive prefixes, duplicates/case collisions, control characters, or symlink attributes.
- Restore checks a free-space safety margin before staging and propagates write failures. Expansion is counted while streaming, independently of ZIP claims.

Archives are **not encrypted**. An internal recovery copy is not protection against device loss. Long-lived raw recovery copies currently have no automatic retention/cleanup setting.

## Reading and interchange exports

Combined Markdown and linked Markdown+images ZIP are author editions with private prose. They are not full-fidelity editable restores. Combined `.md` uses relative image references; use the ZIP for accompanying images.

Public history exports serialize only the projection allowlist; they never contain the author archive with a hidden flag. Review approved prose for manually entered spoilers. Public exports are a single Markdown document, so no private filenames or attachment captions are serialized.

JSON Canvas is one board, text nodes/geometry/colors/labels and groups. Rich chronology and world-level semantics are flattened and disclosed in the export dialog. Unsupported external cards become explicit placeholders with raw source retained; no URL/file is fetched.

## NodeNote v1 import

Prototype backups with `format=nodenote` and `version=1` import into new worlds. Maps become boards, nodes become lore entries plus placements, and edges become relationships plus board edges. IDs are remapped per map and import batch; equal titles do not merge entries. Notes, labels, colors, coordinates and viewport transforms are retained, along with the original JSON for unsupported fields. Validation precedes transactional insertion.

The current app uses a separate package from the prototype. Export a backup from the old app before importing it here; the new app cannot read another app's private storage.
