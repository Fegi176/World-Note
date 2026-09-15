# User guide

## 1. Start a world

Choose **Create my world** to begin, or **Explore sample world** to inspect the original Ashen Meridian setting. Worlds keep separate lore, boards and history. Use the world title/menu to switch between them.

The four main areas are **Library**, **Boards**, **Timeline** and **Search**. The menu contains world management, templates, settings/backup, storage/recovery and other authoring tools.

## 2. Write reusable lore

Create an entry in Library and choose an appropriate type: a character, place, faction, artifact, event, epoch, note or another supported kind.

- **Note** contains the main prose, with Markdown preview and internal links.
- **Details** holds structured/custom fields, aliases, tags and editing/canon state.
- **Links** connects stable identities; similarly named entries remain distinct.
- **History** records explicit historical states and period associations.
- **Media** imports pictures, captions, credits and source references.
- **Revisions** provides saved checkpoints and recovery choices.

Entries autosave after a brief pause. Wait for **Saved** before deliberately terminating the app. If saving fails, keep the editor open and use its recovery/export controls. Protected entries require an explicit unlock.

An entry's identity stays the same when renamed. Reusing it on another board does not create a second copy of its notes.

## 3. Make a board

Create or open a board in **Boards**.

| Control | Purpose |
|---|---|
| **+ Card** | Create a new lore entry and place it on this board |
| **Place existing** | Place an entry already in the world's Library |
| **Sticky** | Add a board-local note; convert it to lore when it needs to be reused |
| **Image** | Import a picture through Android's document picker as a reusable image-backed lore card |
| **Outline** | Use ordinary native entry/relationship controls instead of navigating the canvas |
| **Board tools** | Zoom, fit/reset, undo/redo, filters, layout preview and Canvas export |

Pan on empty space, pinch to zoom and drag cards to move them. Select cards to reveal editing, connections, content, size, color and grouping controls. Horizontal action rows can be scrolled.

### Images and richer cards

For a new image, use **Image** on the board. NodeNote copies the original into private storage; deleting the picked source does not remove the imported copy.

For an existing entry:

1. Open the entry and use **Media → Import image** if it has no pictures.
2. Mark an image as the entry cover in its media details, if desired. Otherwise board previews use an available attached image.
3. Return to the board and select its card.
4. Open **Card content**, enable **Cover image**, **Note preview** and/or **Tags and status**, then apply.

These choices belong to the selected placements. The same character can show a picture on one board and stay compact on another. Notes remain linked to the original entry. Cards grow to accommodate content; adjust **Card size** when needed.

Images fit without cropping. Previews are bounded for memory use; open Media for a larger view. Removing a placement removes its presence on that board, not the reusable lore or its images. Content-setting changes support Undo; direct image import creates durable lore that remains in Library.

### Relationship notes

A drawn connection can reference a reusable relationship with its own long notes and validity periods. A local board annotation belongs to that drawing. Use the appropriate one when an explanation must appear on multiple boards.

## 4. Organize history

Open **Timeline** and the **Epoch Codex** for epochs, eras and events.

An unknown date is valid. Use exact years only when the world establishes them; otherwise choose approximate, bounded range, relative, local-to-period, order-only or unknown expressions. Very large and negative years are supported.

**Overview** prioritizes readability. **Scaled** represents numerical distance, with unresolved material kept separate. Overlapping regional periods need not contradict one another.

Historical states describe how an entry actually differed in a period. They are explicit records; today's description is not automatically substituted for missing historical evidence.

## 5. Keep reveals, knowledge and history separate

- **History:** when events happened in the world.
- **Story/reveals:** when the reader encounters information.
- **Claims and knowledge:** what is asserted and what a character explicitly knows.
- **Recorded accounts:** deliberately selected public prose.

Changing a chapter order must not move the historical event. Editing author truth must not automatically rewrite character knowledge. Review public prose yourself for spoilers deliberately included in it.

## 6. Back up and restore

Open **Menu → Settings & backup → Complete workspace backup**, then choose a destination in Android's document picker.

A complete backup includes lore, images, boards, chronology, drafts, revisions, trash and preferences. It is the lossless restore format. Keep a copy outside the phone; app-private recovery files do not protect against device loss.

To restore, select **Restore / NodeNote / JSON Canvas import**, choose the file and review the validated summary. **Import as new worlds** keeps the existing worlds separate. Replacement is an explicit choice and creates an internal safety archive first.

**Markdown** is for readable exports. **JSON Canvas** is board interchange and cannot represent the entire world model; neither replaces a complete backup. Backups/author exports contain readable private lore and are not encrypted.

## 7. Recovery and common questions

- **Can I remove a board without losing a character?** Yes. Board placements and reusable lore have separate ownership.
- **An image preview is broken.** Open **Storage & recovery** to clear regenerable previews. Originals are retained.
- **Where are safety copies?** In **Storage & recovery**, where you can inspect/export them before explicitly removing them.
- **The update will not install.** Confirm Android/ADB authorization and signing identity. Do not uninstall or clear app data to fix a signing mismatch.
- **Can I use the app offline?** Yes. A system document provider you choose may require its own network access; NodeNote itself has no INTERNET permission.

## 8. Storage and current limits

Your notes and imported originals stay in app-private storage. NodeNote has no account, analytics or background network access. Android cloud backup is disabled; use a complete workspace backup to keep an external copy. Backup files and author exports are readable and unencrypted.

Images are limited to 32 MiB / 80 megapixels. Animated images display a still preview while retaining the original file. Complete backups are limited to 512 MiB, with 64 MiB per member and 10,000 members. See [backup format](BACKUP_FORMAT.md) for details.

The interface is English. Maps, full calendars, advanced character perspectives, family views, cloud sync and scheduled external backups are not currently available.
