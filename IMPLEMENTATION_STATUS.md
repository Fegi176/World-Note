# Implementation status — 15 September 2026

## Current delivery: 0.2.1 (4)

The native Kotlin/Compose/Room app retains the REQUIRED WB-01–WB-18 implementation: reusable lore and relationship notes, native boards, writing/media, Epoch Codex, flexible signed 64-bit chronology, historical states, stories/reveals, claims/knowledge/accounts, intrigue, revisions/drafts/trash, complete backup/restore and interchange. The cobalt/black/phthalo-green design and 0.2.0 reliability improvements remain intact. See docs/FEATURE_MATRIX.md.

### Board images and richer cards

- Boards → Image imports a picture into private app storage and creates a reusable lore entry, managed media and placement atomically.
- Select a card → Card content enables its cover image, note preview, tags and status. These are placement settings; linked prose remains authoritative in the original entry and other boards keep their appearance.
- Existing compact placements keep their layout until changed. Rich cards grow to fit; Card size can adjust them. Image previews fit without cropping, use bounded background decoding and survive source-file deletion.
- Complete backups retain image originals and presentation fields. Failed image insertion cleans up newly copied assets. Native text wraps/ellipsizes inside each card; Outline provides an accessible alternative.
- Content-setting changes support board Undo. Image import creates durable reusable lore; remove its placement to remove it from the board. The lore/image remains in the Library.

## Current verification

- Conventional Gradle assembly/lint succeeded; lint 0 errors, 26 warnings, no suppression.
- Four targeted Android 14 instrumented methods passed in 31.59s: two new board-media tests plus the two Design regression methods. This includes rendered-image pixel assertions, content controls, independent placements, archive contents, recreation, insertion rollback, pinch/toolbar pixels and automated accessibility/contrast checks.
- Native ADB smoke verified the board Image action opens Android DocumentsUI and cancellation returns to the canvas. Actual production copying was tested with a synthetic file URI; a real user-selected provider image was not chosen in that smoke.
- Core test task was invoked and UP-TO-DATE because core source is unchanged. Historical 0.2.0 passed 42 core JVM and 29 Android 14 methods; that complete runtime suite was not rerun for this focused update.
- SDK signature verification passes with the same Android Debug RSA-2048/v2 certificate. Package app.nodenote.worldbuilder.debug, version 0.2.1 (4), min SDK 26, compile/target 36; requested runtime is Android 14. No new dependency or network permission.

## Phone installation

**Version 0.2.1 (4) is installed and launched on Samsung SM-G990B2 / Android 14.** After USB debugging was authorized, adb install -r returned Success. Android reported versionCode=4/versionName=0.2.1 and a successful cold launch in 1,189ms. Process 28331 was running and the activity was foreground. No uninstall or data clear was used. Evidence: artifacts/v0.2.1-phone-install.log. This is an installation/launch check, not a full physical workflow or performance test.

## Remaining work / limits

1. Extend the successful physical installation/launch check with phone workflow, typing, frame pacing and memory tests.
2. Manual spoken TalkBack review and remaining partial/manual acceptance permutations remain open. Automated checks do not certify every interaction.
3. Hostile-provider, actual full-volume and exhaustive hardware power-loss tests remain open. Prior restore fault tests cover four file/database boundaries; settled restart checks do not prove arbitrary unsaved-keystroke recovery.
4. Canvas uses at most 64 visible unique image previews at once, sampled to at most 256px per dimension, with an 8MiB cache. Open Media for the full image. Large worlds retain a complete logical snapshot with incremental record reuse. English only; further localization is pending.
5. Optional matching-signature legacy upgrade needs original source/signing credentials. WX-01–WX-06 remain extended, FUT-01–FUT-03 excluded. Remote CI has not run.

## Delivery

- artifacts/NodeNote-Worldbuilder-debug.apk: 12,446,712 bytes.
- APK SHA-256: bd5a6974ac571a7e904d0dc925d7e0617f305828944954eb3a65b116eb3c17e7.
- Source ZIP and SHA256SUMS are adjacent. Previous 0.1.0 and 0.2.0 artifacts are preserved in their version directories.
- Exact current/historical evidence: docs/TEST_REPORT.md, artifacts/BUILD_REPORT.md and artifacts/board-media-*.log. Native test capture: artifacts/screenshots/18-board-media-api34.png.

The original specification remains unchanged. Personal app data was not cleared or uninstalled.