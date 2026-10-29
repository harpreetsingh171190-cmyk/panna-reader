# Panna validation

## Executed in the creation environment

- JDK 17: 23 platform-independent reading logic and speech-chunk checks passed, including long multilingual round trips and surrogate boundaries.
- Catalog rules: 26 checks passed for merge conflicts, duplicate IDs, capacity rejection without input mutation, Trash/Archive visibility, pin order and exact Unicode tags.
- Study rules: 30 checks passed for Markdown heading offsets, CRLF/Unicode, code-fence exclusion, heading limits, valid highlight ranges, overlap and surrogate-pair boundaries.
- Research rules: 57 checks passed for metadata URL/date validation, Unicode collection membership, CSV quoting/formula-like cell protection, bounded advanced-search parsing, phrases, fields, exclusions and predicates.
- JDK compiler parser: eleven Android Java sources passed syntax parsing only.
- Python XML parser: manifest, identity/theme and icon XML parsed successfully.
- Archive integrity: verified after packaging.
- Android build attempted: blocked before compilation because Gradle 8.0 could not be downloaded (network unreachable). No Android SDK was present.

These checks do not establish Android API compatibility or runtime correctness. The included CI workflow is an unexecuted build path, not a successful CI result.

## Build gate (requires Android SDK and network on initial sync)

```bash
bash panna-check.sh
gradle lintDebug assembleDebug
```

## Device acceptance checklist (not yet executed)

1. Install on Android 6+; confirm first-run sample, icon and app label.
2. Import UTF-8 Punjabi/English TXT and MD files; cancel the picker; reject files over 2 MiB or invalid UTF-8.
3. Paste a new entry, validate missing title/text, edit it, then reopen after process termination.
4. Search by title, body, note and tag; exercise every filter and sort order.
5. Read a long document halfway, leave/reopen, rotate and relaunch; confirm restored place.
6. Exercise font bounds, serif switch, theme persistence, text selection and find highlights.
7. Favorite, annotate, mark finished, restart, share and delete (cancel and confirm).
8. Export backup, edit library, restore backup and compare notes/positions/favorites.
9. Try malformed, unrelated, duplicate-ID, oversized and unsupported-version backups. Existing data must survive rejection.
10. Revoke file-provider access or choose a read-only destination; confirm clear failure without false success.
11. Test large libraries and constrained-memory devices; evaluate UI/save latency.
12. Test font scaling, TalkBack, narrow screens, keyboard visibility and document-provider lifecycle changes.

## Known implementation limits

- UI labels are English. Punjabi content is supported through Android Unicode fonts.
- Backup exports exclude display preferences. Old CloudReader data is not compatible.
- Restore replaces, rather than merges, the current library after confirmation.
- The editor is a dialog; rotation and theme/keyboard changes no longer recreate the Activity (configChanges), but unsaved edits are still lost if Android kills the app process. Save before leaving the app.
- Local JSON serialization and atomic saves occur on the main thread. File-provider streaming and backup parsing use a worker thread. Large libraries require performance testing.
- Import/picker tasks in flight are not resumed after process death or Activity recreation; retry the operation.
- Word counts assume whitespace-delimited words; read-time is approximate.


## Version 1.1 acceptance checks (device testing pending)

- Add multiple named bookmarks, relaunch, jump/delete, export/restore and verify offsets. Try a legacy backup without bookmarks. Reject more than 100 bookmarks, malformed bookmark arrays and out-of-range offsets without overwriting data.
- Change only a title: keep bookmarks. Change text: reset progress and bookmarks. Simulate save failure: recover previous fields and bookmarks.
- Listen to a reading longer than 3000 UTF-16 units, including emoji, Punjabi and Hindi. Confirm no dropped/duplicated chunks, correct stop after backgrounding/navigation, and useful errors for missing language/engine data. Stop/restart quickly to test stale callbacks.
- Select English, Punjabi, Hindi and device voice; test all three speeds. Language availability is engine dependent and is not established by source checks.
- Receive text via Android Share with cold/warm app, cancel, rotate, and verify no duplicate reading. Share a URL and verify it remains plain text. Input is never auto-saved.
- Export a single TXT, verify UTF-8 title/body, cancel picker, deny destination access, and rotate during export. Interrupted exports require retry.
- Switch light/dark/sepia, reopen, and confirm theme persistence. Toggle keep-awake and verify the flag clears on leaving the reader or backgrounding.
- Compare library statistics with known fixture entries, finished readings, notes and bookmarks. Confirm stats make no claim about tracked reading duration.

Read Aloud is foreground only, has no audio-focus/media-session integration, and relies on the selected Android engine. Other apps' audio may continue concurrently. It does not synchronize visual scroll or save an audio resume cursor. Preference settings (including voice/theme) are excluded from backups.


## Version 1.2 acceptance checks (device testing pending)

1. Move active and archived readings to Trash, relaunch, restore and verify prior pin/archive/note/bookmark state. Cancel permanent deletion, then confirm deletion of a test item. Ensure Trash counts toward storage limits and persists until explicitly emptied item by item.
2. Pin several readings; apply Recent/Title/Shortest and verify pin priority, deterministic secondary ordering and Pinned filter. Archive a pinned favorite; it must appear only in Archived until returned.
3. Choose Punjabi/English tags, Untagged and All tags within active/Archived/Trash. Combine search, category and tag filters. Rotate to verify view-state restoration.
4. Export notes with Punjabi text, empty notes, no bookmarks and many bookmarks. Verify UTF-8 text, title/tag, bookmark percentages/offsets and picker cancellation/failure handling.
5. Merge a backup with an older conflicting ID, new IDs and imported Trash/Archive items. Preview must report correct added/skipped counts. Cancel preview and verify no changes; commit and verify local conflicting notes win. Repeat the same merge and verify no duplicates.
6. Test merge at the 500-entry and 24 MiB limits, invalid backups and denied writes. A rejected or failed save must leave existing library data intact.
7. Restore schema 1 (Panna 1.0/1.1) backups without new fields, then export/restore schema 2. Verify flags survive relaunch. Reject invalid flag types and unsupported schema versions.
8. Test Save after adding from a filtered or Trash view: reset to All and show the new reading. Opening a Trash card must offer restore/delete rather than editable reader UI.

Backup compatibility: Panna 1.2 accepts schema 1 and 2 and emits schema 2. Panna 1.0/1.1 readers reject schema 2; this prevents older builds from silently losing the new Trash/Archive state on a restore/resave cycle.

Merged entries retain their original IDs and states. Matching local IDs always win, including trashed local entries. There is no field-by-field reconciliation or title/content deduplication. A merge preview and its uncommitted selection do not survive Activity recreation; retry if interrupted.


## Version 1.3 acceptance checks (device testing pending)

1. Select a Punjabi/English/emoji passage with Android text selection. Save a highlight in each color, add/edit/remove its annotation, relaunch, and verify source offsets and displayed colors. Check TalkBack, copy menu preservation, selection overflow menu and manufacturer-specific selection toolbars.
2. Add adjacent highlights, try an overlapping highlight, reach the 200-highlight limit, and test 3000-unit notes. Restore backups containing invalid ranges, overlapping ranges, surrogate splits or unknown colors; reject without overwriting current data.
3. Search inside/outside highlights and clear temporary search marks. Saved annotation colors must return. Change only title and keep annotations; change body text and reset annotations/bookmarks/progress after the editor's notice.
4. Use Contents for ATX and setext headings, CRLF, emoji/Punjabi text, and backtick/tilde code fences. Verify jumping to original source positions and 300-heading cap. Test long headings on small screens.
5. Hold a library card, select/deselect others, select all within a query/tag/section, rotate and cancel. Batch favorite/pin/archive/tag/trash and restore from Trash. Search/category/tag changes must clear selection; each save failure must roll back every selected item, not just the last one.
6. Export study notes with multilingual quotes, empty notes, bookmark positions and multiple annotation notes. Verify text, then verify JSON backup/restore preserves all annotation data separately.
7. Load schema 1/2 fixtures with no highlights, then export/restore schema 3 and merge entries with annotations. Conflicting local IDs must still win. Older readers should reject schema 3 rather than silently discard annotations.

Batch actions intentionally exclude permanent deletion. Multi-selection is confined to matching visible cards. A batch action uses one atomic file save and an in-memory JSON rollback snapshot; this is not a persistent undo history. Save/rollback performance on a full 24 MiB library requires device testing.

Heading detection is a bounded, lightweight ATX/setext detector that skips fenced code. It is not a full CommonMark parser; blockquotes, nested containers and arbitrary plain-text titles are not promised. Read Aloud does not narrate annotation notes.


## Version 1.4 acceptance checks (device testing pending)

1. Edit author, source URL and publication date. Reject invalid dates, non-HTTP(S) URLs, embedded credentials and length violations without dismissing the editor. Clear optional fields, relaunch and verify persistence. Open source in an external browser, test no installed handler, then return to the same reading. Body/highlights must remain intact.
2. Assign several collections, remove membership, use Unfiled and exact-name filters, and restore/merge backups containing memberships. Reject duplicate/invalid names and more than 10 memberships. Batch-add to a selection containing a full member list must reject before mutation. Verify failure rollback for batch add/remove.
3. Search with multiple literal terms, quoted phrases, exclusions, field restrictions, Punjabi text, annotations, publication date and has:/is: predicates. Verify malformed input displays inline error and exports no stale result scope. Check section/tag/collection intersection and query/case/term limits.
4. Export CSV for current results, selected entries and all saved readings. Parse with an independent CSV reader; verify 15 columns, UTF-8 BOM, commas/double quotes/newlines, Unicode, formula-like cell apostrophes, JSON collection arrays, row counts and Trash scope. Current/selected exports follow the current result order; all-saved exports use stored order. CSV is a catalog, not a full backup.
5. Cancel the CSV picker, rotate or kill the process while picking, deny a destination and test worker completion after Activity destruction. Interrupted exports require retry; never report success for a failed write.
6. Load schema 1/2/3 backups with missing metadata defaults, then round-trip schema 4 and merge conflicts. Older readers must reject schema 4 rather than silently dropping metadata/collection fields.

Source metadata is manually entered and not fetched or verified online. Opening a source delegates to an external browser. Collections are per-entry memberships, not independently stored empty folders. Advanced search is literal AND/exclusion search, not regex, semantic search or a full boolean expression language. CSV applies formula-like cell escaping and is intended for catalog review, not restoration.

## Review pass (October 2026)

- Re-ran `bash panna-check.sh` on JDK 21: 57 + 30 + 26 + 23 checks passed (3 new search checks), plus the syntax-parse gate.
- Still **not** done: Android compilation, lint, APK build and any device testing. See `REVIEW.md` for what was fixed and what remains unverified.
