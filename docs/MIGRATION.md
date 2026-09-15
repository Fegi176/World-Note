# NodeNote prototype import

No legacy source/archive, preferences, APK, or matching certificate was supplied in this workspace. Import supports the documented NodeNote v1 backup contract without requiring those inputs.

- `format=nodenote`, `version=1`; future versions fail before writing.
- Workspace → new world; maps → boards; nodes → generic lore + placements; edges → independent directed relationships + board edges.
- IDs are remapped per map and import batch. Equal titles and equal old node IDs across maps do not merge.
- Entire Unicode notes, labels, colors, coordinates and viewport translation/scale are preserved. Baseline node dimensions are 190×112. Transform is `screen = world * scale + translation`.
- Complete original JSON and input hash are retained in an IMPORT record; unknown source fields remain recoverable.
- The original picked input is also retained in private recovery storage. Preview/validation precede one transactional insertion.
- The archive's claimed prototype viewport semantics could not be independently reinspected because the old implementation was absent. The implemented transform is documented and math-tested; visual comparison with a real prototype remains unverified.

Default install identity is `app.nodenote.worldbuilder.debug`. It neither reads another app's private preferences nor replaces `app.nodenote`. There is no `legacyUpgrade` variant because no verified matching key/source was available. No prototype keys are used or distributed. Do not uninstall the old app to resolve a signature conflict; export its backup first.
