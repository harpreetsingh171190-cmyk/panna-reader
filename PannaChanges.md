# Panna Reader 1.4 — Research library features

Package `com.harpreet.panna` · versionCode 5 · versionName 1.4.0.

| Feature | Where to use it |
|---|---|
| Source metadata | Reading → More → Source details; author, source URL, publication date |
| Open original source | Reading → More → Open source in browser |
| Multiple collections | Reading → More → Collections; up to 10 memberships |
| Collection batch actions | Select cards → Actions → Add to / Remove from collection |
| Advanced search | Fields, quoted phrases, AND terms, exclusions, has:/is: predicates; examples in Search help |
| CSV catalog export | Settings → Export catalog CSV; current results / all saved / selected |

Search example: `author:"Harpreet Singh" has:highlights -tag:draft`.

Metadata is entered manually; web pages are never downloaded automatically. Collection names are case-sensitive, and a collection name disappears when its last member is removed. Source data, memberships and annotations are saved in the JSON backup. The CSV contains metadata and counts, but body text and notes cannot be fully restored from it. Spreadsheet-formula-like text cells are prefixed with an apostrophe.

The new backup format is schema 4. This build reads schemas 1–4; older app versions cannot open schema 4. Upgrading an installed app requires the same signing key.

Verification: 57 research/search/CSV checks + 30 study checks + 26 catalog/merge checks + 23 reading/chunk checks = 136 passed. Syntax parsing of the eleven Java sources and XML/ZIP integrity checks also pass. Android compilation, lint, APK build and device/UI testing are still unverified. This is a source ZIP.
