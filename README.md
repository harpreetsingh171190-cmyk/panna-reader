# Panna Reader

An offline, no-account Android reader for plain text and Markdown. Import or paste text, read with a remembered position, highlight and annotate passages, organise a research library with collections and advanced search, and keep everything on the device.

> **Status:** source project, version 1.4.0. The pure-Java logic is covered by 136 automated checks. The Android build, lint run and on-device testing have **not** been executed yet — see [`PannaValidation.md`](PannaValidation.md) and [`REVIEW.md`](REVIEW.md).

## Features

- **Library:** import UTF-8 `.txt` / `.md` / `.markdown` or paste text; favorites, pinning, archive, reversible Trash, tags and multiple collections (up to 10 per reading).
- **Reader:** remembered place and progress, adjustable type size, serif/sans, light / dark / sepia themes, find-in-text, named bookmarks.
- **Study tools:** highlights with notes (3 colours), Markdown heading navigation, study-notes export.
- **Advanced search:** AND terms, `"exact phrases"`, `-exclusions`, field search and predicates — for example `author:"Bin Jing" has:highlights -tag:draft`. Literal matching only; no regex.
- **Read Aloud:** uses the device text-to-speech engine (device language, English, Punjabi or Hindi; 0.75× / 1× / 1.25×).
- **Backup and export:** JSON backup / restore / non-overwriting merge (schema 4, reads schemas 1–4), TXT export, catalog CSV export with spreadsheet-formula protection.
- **Privacy:** no internet permission, no storage permission, no analytics. Data lives in app-private storage; backups are plain readable JSON.

Limits: 2 MiB per entry, 500 entries, 24 MiB total library. No PDF or EPUB support.

## Build

Requirements: JDK 17+, Android SDK with platform 34 and build-tools 34.0.0.

```bash
# 1. Fast, SDK-free logic checks (JDK only)
bash checks/panna-check.sh

# 2. Lint + debug APK
gradle :reader:lintDebug :reader:assembleDebug
# APK: reader/build/outputs/apk/debug/
```

The committed `gradle/wrapper/gradle-wrapper.jar` is a legacy (2015) wrapper inherited from the upstream project. It is configured for Gradle 8.5, but you should refresh it once with a locally installed Gradle:

```bash
gradle wrapper --gradle-version 8.5
```

After that, `./gradlew` works as usual. GitHub Actions (`.github/workflows/panna-android.yml`) uses `gradle/actions/setup-gradle` with Gradle 8.5 and does not depend on the wrapper JAR.

## Project layout

```
reader/src/main/java/com/amrik/panna/
  PannaLibraryActivity   UI: library, reader, dialogs, import/export
  PannaShelf / PannaEntry / PannaHighlight / PannaBookmark   storage and JSON schema
  PannaSearch / PannaResearch / PannaCatalog / PannaStudy / PannaText   pure-Java logic (unit-checked)
  PannaVoice             text-to-speech wrapper
checks/                  JDK-only check programs run by panna-check.sh
PannaGuide.md            user guide (Punjabi)
PannaChanges.md          release notes
PannaValidation.md       what has and has not been verified
```

## Known limitations

- Library state is saved as a single JSON file on the UI thread; very large libraries (many MB) will make saves and position updates slow.
- Very long readings (near 2 MiB) in a selectable, highlighted `TextView` can be heavy on low-memory devices.
- `targetSdk` is 34; raise it before publishing to Google Play.

## License and attribution

Apache License 2.0 (see [`LICENSE`](LICENSE)). Panna Reader is rebuilt from, and substantially changes, [CloudReader](https://github.com/youlookwhat/CloudReader) by Bin Jing (Copyright 2016); see [`NOTICE`](NOTICE).
