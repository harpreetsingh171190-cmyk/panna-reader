# Panna Reader 1.4 — Research library features

Package `com.amrik.panna` · versionCode 5 · versionName 1.4.0.

| ਨਵੀਂ ਸਹੂਲਤ | ਕਿੱਥੇ ਵਰਤੋ |
|---|---|
| Source metadata | Reading → More → Source details; author, source URL, publication date |
| Original source ਖੋਲ੍ਹੋ | Reading → More → Open source in browser |
| Multiple collections | Reading → More → Collections; 10 memberships ਤੱਕ |
| Collection batch actions | Cards ਚੁਣੋ → Actions → Add to / Remove from collection |
| Advanced search | Fields, quoted phrases, AND terms, exclusions, has:/is: predicates; Search help ਵਿੱਚ examples |
| CSV catalog export | Settings → Export catalog CSV; current results / all saved / selected |

Search example: `author:"Amrik Singh" has:highlights -tag:draft`.

Metadata manually ਭਰੀ ਜਾਂਦੀ ਹੈ; webpages ਆਪਣੇ ਆਪ download ਨਹੀਂ ਹੁੰਦੇ। Collections case-sensitive memberships ਹਨ ਅਤੇ ਆਖ਼ਰੀ member ਹਟਣ ਨਾਲ empty ਨਾਮ ਨਹੀਂ ਰਹਿੰਦਾ। Source, memberships ਅਤੇ annotations JSON backup ਵਿੱਚ ਸੇਵ ਹਨ। CSV ਵਿੱਚ metadata ਅਤੇ counts ਆਉਂਦੇ ਹਨ; body/notes ਦਾ ਪੂਰਾ restore ਨਹੀਂ ਹੁੰਦਾ। Formula ਵਰਗੇ CSV text cells ਨੂੰ apostrophe ਨਾਲ prefix ਕੀਤਾ ਜਾਂਦਾ ਹੈ।

ਨਵਾਂ backup schema 4 ਹੈ। ਇਹ build schema 1–4 ਪੜ੍ਹਦਾ ਹੈ; ਪੁਰਾਣੀ app schema 4 ਨਹੀਂ ਖੋਲ੍ਹੇਗੀ। App upgrade ਲਈ ਉਹੀ signing key ਚਾਹੀਦੀ ਹੈ।

ਜਾਂਚ: 57 research/search/CSV checks + 30 study checks + 26 catalog/merge checks + 23 reading/chunk checks = 136 passed. ਗਿਆਰਾਂ Java sources ਦੀ syntax parsing ਅਤੇ XML/ZIP integrity checks ਪਾਸ। Android compilation/lint, APK ਅਤੇ device/UI testing ਦੀ ਪੁਸ਼ਟੀ ਬਾਕੀ ਹੈ। ਇਹ source ZIP ਹੈ।
