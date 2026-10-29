# Panna Reader 1.4

Panna Reader is an offline reading app for Android, rebuilt from the CloudReader project under a new name. This is an Android **source project**; no prebuilt APK is included.

## Features

- Green/cream library home and reading screens, with a book icon.
- Import UTF-8 `.txt`, `.md` and `.markdown` files, or paste/write text. Markdown is shown as plain text.
- Search across titles, full text, notes and tags; filters for favorites, unread, in progress and finished; sort by recent, title or length.
- Remembers your reading position and shows progress as a percentage; mark finished / start again.
- Notes and a topic tag per reading; edit text, move to Trash, share sheet.
- Font size 14–34 sp; serif or sans serif; light, dark and sepia themes; optional keep-screen-awake while reading.
- Find a word or phrase inside a reading and highlight the matches.
- JSON backup export/restore with file validation and atomic local saves.
- No account, analytics, internet permission or broad storage permission.
- **Read Aloud** with the phone's text-to-speech engine: device language, English, Punjabi or Hindi, at 0.75×, 1× or 1.25× speed. Available voices depend on the installed engine and voice data.
- Up to 100 named bookmarks per reading, with jump and delete.
- Receive text from other apps through the Share sheet; export one reading as TXT.
- Trash (restore, or permanently delete after a separate confirmation); pinned readings stay on top; archived readings have their own filter.
- Browse by tag, including *Untagged* and *All tags*.
- Export a reading's notes and bookmarks as a separate TXT (study notes, including highlighted quotes and annotations).
- Backup merge with preview: only readings with new IDs are added; existing entries and notes are never changed.
- Highlights in yellow, green or blue, each with an optional note; jump to, edit or delete from the highlights list.
- Contents navigation from Markdown headings (fenced code is not treated as headings).
- Batch actions on several selected readings: favorite, pin, archive, tag, Trash, and restore from Trash.
- Source details: author, HTTP(S) source URL and publication date; open the original source in the browser.
- Put a reading in several custom collections; batch add/remove.
- Advanced search: AND terms, exact phrases, exclusions, field search and `has:` / `is:` predicates.
- UTF-8 CSV catalog export of current results, all saved readings, or selected readings.
- Library statistics: readings, saved words, finished readings, favorites, bookmarks and readings with notes.

## Opening the project

1. Extract the ZIP and open the `PannaReader` folder in Android Studio.
2. Install JDK 17, Android SDK 34 and Build Tools 34.0.0.
3. The first Gradle sync needs internet access.
4. From a terminal with Gradle installed: `gradle :reader:assembleDebug` (or `bash gradlew :reader:assembleDebug` after refreshing the wrapper, see below).
5. The APK is written to `reader/build/outputs/apk/debug/reader-debug.apk`.

On Windows use `gradlew.bat :reader:assembleDebug`. The minimum supported version is Android 6.0 (API 23). You must add your own release signing key; the debug APK is for local testing.

The committed Gradle wrapper JAR is very old (2015). Refresh it once with `gradle wrapper --gradle-version 8.5`.

If you place the contents of this folder at the root of a GitHub repository, the included workflow runs the logic checks, lint and a debug build, and uploads the debug APK as an artifact.

## What changed from CloudReader

| Before | Now |
|---|---|
| CloudReader-master | PannaReader |
| CloudReader | Panna Reader |
| `com.example.jingbin.cloudreader` | `com.harpreet.panna` |
| `app` module | `reader` module |
| Old main/feed UI | `PannaLibraryActivity.java` |
| Old network models | `PannaEntry.java` |
| Old data layer | `PannaShelf.java` |
| New text calculations | `PannaText.java` |
| Old launcher assets | `panna_mark.xml` |
| Old theme/string files | `panna_identity.xml` |

`AndroidManifest.xml`, standard Gradle filenames, `LICENSE` and the wrapper filenames are kept for tooling and licensing reasons. The old app's files were replaced by a new structure rather than renamed, and the old `bymvvm` module and its dependencies were removed.

## Limits and data

- The old Gank, WanAndroid and movie feeds, login and online collections are not included. Data from the old app is not migrated, and because the package name is new it installs as a separate app.
- The UI is in English. Punjabi and other UTF-8 text can be imported, searched and read. PDF/EPUB, rich Markdown rendering, OCR and cloud sync are not included.
- Read Aloud runs through Android's separate voice engine.
- Each entry is at most 2 MiB of UTF-8 text. The library holds at most 500 entries (Trash and Archive included) and 24 MiB serialized.
- Reading time is estimated at 200 words per minute. For short readings use More → Mark finished.
- Save and search performance on very large libraries still needs device testing.
- A backup contains readings, notes, tags, favorites, positions, bookmarks, highlights, annotations, and Pin/Archive/Trash state. Backups from Panna Reader 1.0–1.3 can be restored (missing fields get defaults). Version 1.4 writes schema 4, which older app versions cannot open. Font and theme settings are not backed up.
- Restore replaces the current library, so export first. Merge only adds readings with new IDs and keeps existing data.
- Backups are plain, readable JSON. Uninstalling the app deletes the local library; Android automatic backup is disabled.

## Verification status

`bash checks/panna-check.sh` passes 136 logic checks (23 reading/chunking, 26 catalog/merge, 30 study/heading, 57 metadata/search/CSV) and parses all eleven Java sources. XML parsing and ZIP integrity were also checked.

Android compilation, lint, the APK build, emulator and physical-phone UI tests have **not** been run, so the app's behaviour on a device is unverified. See `PannaValidation.md` for the manual verification steps.

The original author credit and Apache 2.0 license are kept in `NOTICE` and `LICENSE`.

## How to use the features

### Bookmarks, Read Aloud, sharing, themes (1.1)

Open a reading → **Bookmarks** to name and save the current position; pick a saved bookmark to jump to it or delete it. Changing a reading's text resets its bookmarks, highlights, annotations and progress; changing only the title does not.

Use **Listen → Voice language**, **Reading speed** and **Play from visible position** to start listening, and **Stop** to stop. Speech stops when you leave the app, switch readings, or the screen is rebuilt (for example after a font change). The audio position is not saved and does not move the visual progress. A Punjabi voice is not available on every device or engine, and offline speech needs installed offline voice data. The voice engine receives the reading text, and its own network/data policy may differ.

**More → Export TXT** writes one reading as a file (title and body only; use the JSON backup for bookmarks and notes). To save text from another app: select it → Share → **Panna Reader** → check the title and Save. A shared URL arrives as text only; no web page is fetched.

Pick Sepia in **Settings → Theme**. **Keep screen awake** applies only while the reader is visible. **Reading statistics** are library totals, not tracked reading time.

### Version 1.2: keeping your readings in order

**More → Pin reading** keeps a reading at the top of the list; pins take priority, then the chosen sort applies. **More → Archive reading** hides it from the main All / Favorites / Pinned / Unread / Finished lists; open **Filter → Archived** and use More → Return from Archive to bring it back.

**More → Move to Trash** keeps the reading with its notes and bookmarks. Use **Settings → Open Trash** or **Filter → Trash**, then select a card to restore it. A reading that was archived before returns as archived. Permanent deletion asks for a separate confirmation. Trash is not emptied automatically and counts toward the 500-entry / 24 MiB limits.

In **Tags** (or Settings → Browse tags) choose an exact tag. The tag list is built for the current active/Archived/Trash section. Changing the filter resets the search and tag selection. A newly saved reading appears in All.

**More → Export study notes** produces a UTF-8 TXT with the title, tag, notes and bookmark names/positions. It is not a backup, and restoring that TXT does not recreate bookmarks. Use the JSON backup to keep all data.

**Settings → Merge a backup into this library**: choose a backup, review the add/skip preview and press Merge. For the same ID, the reading on your phone is kept, even if it is in Trash. Readings with different IDs are treated as separate even when title and body match. Imported Archive/Trash states are kept. If the limit is exceeded or saving fails, the current library should not change; fault testing on a device is still to be done.

### Version 1.3: study tools and batch organization

Long-press text in a reading to select it → **Highlight & note** (it may be in Android's overflow menu). Pick Yellow, Green or Blue and an optional note, then Save. Each reading allows up to 200 non-overlapping highlights, and each note can be up to 3000 UTF-16 units. Adjacent ranges are allowed; overlapping ranges are not.

In **More → Highlights & annotations** pick a passage to Jump, Edit note/color or Delete. Highlights survive closing the app and restoring a backup. Search marks are separate and temporary; **Find → Clear search marks** does not delete saved highlights.

**More → Contents (Markdown headings)** jumps to `# Heading`, `## Subheading` and setext (`===` / `---`) headings. Fenced code blocks are skipped, and at most 300 headings are listed. This is lightweight heading detection, not a full Markdown renderer, and plain-text titles are not guessed.

In the library, hold a card, then select more cards. **Select all** selects only the cards matching the current search/filter/tag. **Actions** offers Favorite/Unfavorite, Pin/Unpin, Archive/Return, Set tag or Move to Trash; in the Trash section there is **Restore selected**. Bulk permanent deletion is not included. Changing the search, category or tag clears the selection. A batch is saved once; if saving fails, the in-memory snapshot is restored. Device fault testing is still to be done.

The study-notes TXT now also contains highlighted quotes with their annotation notes. Use the JSON backup for restoring. Editing the text resets highlights, annotations, bookmarks and progress (the editor warns you first); changing only the title does not.

Version 1.4 reads schema 1, 2, 3 and 4 backups and writes schema 4. App versions 1.3 and older cannot restore a schema 4 backup. Display and voice preferences are not part of the backup yet.

### Version 1.4: research library

**More → Source details** stores the author (240 UTF-16 units), an HTTP(S) source URL (2048 units) and a publication date. The date must be a real Gregorian date in `YYYY-MM-DD` form, or empty. The URL must have a host; embedded username/password, invalid schemes and unescaped spaces are rejected. Everything is entered manually. **More → Open source in browser** opens the external browser; Panna never downloads the page. Changing metadata does not erase the body or highlights.

**More → Collections → Add to collection** lets you choose a collection or enter a new name. A reading can have up to 10 unique memberships (names up to 80 UTF-16 units) and can be in several collections. Use the **Collections** button in the library to pick an exact collection, All collections or Unfiled. Empty collections are not stored as separate folders: when the last membership is removed the name disappears from the list. Names are case-sensitive; search is case-insensitive. Selecting cards also offers Add to collection / Remove from collection in Actions.

#### Advanced search examples

| Query | Meaning |
|---|---|
| `solar energy` | Both words required; they may be in different fields |
| `"solar energy"` | The exact phrase, in that order |
| `author:"Harpreet Singh"` | Phrase in the author field |
| `collection:Study has:highlights -tag:draft` | In the Study collection, has saved highlights, no draft tag |
| `published:2026` | Year within the publication date |
| `note:thermal` | Word in the reading note or in a passage annotation |

Fields: `title`, `text`, `note`, `tag`, `author`, `source`, `published`, `collection`. For `has:` use `notes`, `annotations`, `highlights`, `bookmarks`, `source`; `has:notes` means the reading-level note and `has:annotations` means at least one passage note. For `is:` use `favorite`, `pinned`, `archived`, `trash`, `finished`, `unread`, `inprogress`.

A query applies only inside the current Filter / tag / collection. To search Archived or Trash, choose that Filter first. A query is limited to 500 UTF-16 units and 16 terms. Unknown field prefixes are treated as literal text so URL searches work. There is no regex or OR syntax; malformed quoted phrases or field values show an inline message. Search help is also available in the app.

**Settings → Export catalog CSV**: choose the scope: current matching results, all saved readings (Archive included, Trash excluded) or selected readings. Current/selected scopes include Trash only if you choose from the Trash view. The CSV contains IDs, titles, author/date/source, JSON-encoded collection names, tag, status, flags, word/progress counts, bookmark/highlight counts and the updated time in epoch milliseconds; body text, notes and full annotations are not included. It uses a UTF-8 BOM and CRLF rows. Formula-like text cells may get a leading apostrophe so a spreadsheet opens them as text. This is not a lossless restore format; only the JSON backup restores everything.

All metadata and collection memberships are stored in the schema 4 backup, and during a merge the local entry with the same ID always wins. Display and voice settings are not in the backup yet.
