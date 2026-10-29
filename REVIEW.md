# Panna Reader 1.4.0 — review summary

## Scope
Read all 11 Java sources, manifest, resources, Gradle files and CI workflow. Re-ran the JDK-only checks. **Could not** run Android compile / lint / APK / device tests (no Android SDK, no network in the review environment), so Android-API correctness is reviewed by reading only.

## Fixed
| # | Area | Problem | Fix |
|---|------|---------|-----|
| 1 | Build | AGP 8.0.2 predates compileSdk 34 (unsupported-SDK warning) | AGP 8.2.2 + Gradle 8.5 (also supports JDK 21) |
| 2 | Build | Committed wrapper JAR is from Gradle 2.10 (2015) | Wrapper removed; CI and docs use Gradle 8.5 directly |
| 3 | CI | Used the legacy wrapper, no lint report, duplicate runs | `setup-gradle`, concurrency, timeout, lint-report artifact |
| 4 | Voice | If TTS init failed the dead engine stayed cached, so Read Aloud said "initializing" forever | Engine is shut down and cleared on init failure |
| 5 | Performance | Search lower-cased every multi-MB text on every keystroke | Allocation-free case-insensitive match |
| 6 | Performance | Word count / reading time recomputed for every card on every render | Cached per text on `PannaEntry` |
| 7 | Config | No lint policy, `.gitignore` missed APK/AAB | Explicit `lint {}` block, extended `.gitignore` |
| 8 | Docs | No README | Added `README.md`; check counts updated (136) |

Three regression checks were added for the new search matcher.

## Not fixed (noted)
- Whole-library JSON is written synchronously on the UI thread on every position save (slow for large libraries).
- Highlight preview may cut a surrogate pair at 180 chars (cosmetic).
- Serif/sans toggle in Settings gives no immediate feedback (applies next time a reading opens).
- targetSdk 34 is below current Play Store requirements.

## Still unverified
Android compile, lint results, APK, and the 11-point device checklist in `PannaValidation.md`. Push to GitHub and read the first Actions run; fix whatever lint or compile errors it reports.
