# Qualification limits — 0.2.1

These limits distinguish tested behavior from broader qualification.

1. **Android 14 is the target.** Version 0.2.1 was installed and launched on a Samsung Android 14 phone. Full physical workflow/performance qualification remains open.
2. Automated Android accessibility checks, main contrast pairs, native writing/gestures and emulator display variations are recorded in TESTING.md. A manual spoken TalkBack review and broad vendor/keyboard/provider qualification remain unperformed.
3. The UI retains a complete logical world, with incremental loading and reuse of unchanged records after debounced invalidations. Typical/stress SQLite and actual stress-board observations are reported without claiming a universal phone frame-rate target. Paging and advanced graph layouts are optional future optimizations.
4. Restore fault injection covers file promotion and SQL commit boundaries, journal recovery, cross-world identity collisions and atomic replacement. Parser expansion/truncation and bad images are tested. Actual hardware power loss, filled physical volumes and every third-party document-provider failure cannot be inferred from those tests.
5. Backup bounds: 512 MiB total, 64 MiB/member, 10,000 members. Images: 32 MiB/80 MP. Oversize data is rejected explicitly. No silent truncation.
6. Revision retention defaults to unlimited; optional 10/50/100 policies prune at a checkpoint. Storage & recovery lets the owner clear regenerated previews and export/remove safety copies. Authored originals and retained raw imports are not automatically deleted; they can consume storage.
7. The UI is English. Static copy uses Android resources where applicable; editable template prompts and canonical schema/status codes remain English. No additional language translation is claimed. Search is Unicode-safe but not language-specific morphological search.
8. Recorded-history mode uses explicit public account prose/shared sections. It does not infer chapter-as-of character knowledge, and cannot remove spoilers deliberately written into public prose.
9. Incompatible type conversions require resolving typed references. Complete backups are lossless; JSON Canvas/Markdown may be lossy, with warnings and original input retained.
10. Durable navigation, filters, scroll, selection and editor context are restored. Transient confirmation dialogs and unsent creation dialogs intentionally do not replay after process death. A settled force-stop test does not prove an arbitrary unsaved keystroke survives a power cut.
11. No original NodeNote source/archive/signing certificate was supplied. Legacy interchange uses synthetic fixtures; optional same-package legacyUpgrade remains unavailable. The signed test APK uses a debug certificate, not a production release key. GitHub Actions results are separate from the local verification recorded here; consult the repository Actions page for the current run.
12. Maps, full calendars, advanced character perspectives, family views, scheduled external backups and advanced layouts/diffs are not implemented. Cloud sync and terrain painting are outside this release.
