# OpGram mapping generator

Telegram Web (and the Play-Store client) ship R8-obfuscated: every release
renames the classes and methods OpGram hooks. `mapgen.py` finds the new names
automatically so you don't have to remap by hand each time.

## Usage

```
pip install androguard
python3 tools/mapgen/mapgen.py /path/to/Telegram.apk
```

It writes `app/src/main/assets/clients/TelegramWeb-<versionCode>.json`
(and its `Alias/` file). At runtime OpGram loads the file matching the
installed Telegram versionCode, falling back to the generic `TelegramWeb.json`.

## How it works

Each wanted class is located by the **string constants** it references
(these survive obfuscation), with superclass and member hints as tie-breakers,
defined in `fingerprints.json`. Members are then matched by their JVM
descriptor inside the resolved class. Symbols in `org.telegram.messenger`,
`tgnet` and `SQLite` are not obfuscated and are emitted unchanged.

## Maintaining it

`fingerprints.json` is a scaffold covering the main classes. When Telegram
changes an internal string or you add a new hook, add or adjust an entry
there. Anything the tool can't resolve is printed at the end of a run.
The hand-made `TelegramWeb-70999.json` remains the reference for 12.10.4.

## Current coverage

The generator auto-resolves the R8-renamed classes OpGram depends on most
(ChatActivity, ChatMessageCell, TextCheckCell, ProfileActivity, PhotoViewer,
SecretMediaViewer) and several members by descriptor. Two kinds of symbol
still need help and are reported at the end of a run:

- **Classes with no string constants** (small cells such as HeaderCell,
  TextSettingsCell, SimpleTextView, UItem). They must be matched by
  superclass + member descriptors; add those hints to `fingerprints.json`.
- **Classes whose strings are shared** with a sibling (e.g. SettingsActivity
  vs the debug-menu builder). Disambiguating them needs matching by the
  *obfuscated* superclass, which is a planned enhancement.

For these, keep using the hand-made `TelegramWeb-<versionCode>.json` as the
reference. The generator never overwrites it (writes `.generated.json` unless
`--force`).


## v1.2 improvements

The generator now runs in several passes and can match a class by its
*obfuscated* superclass once that super has been resolved, plus by method
descriptor sets (`methodDescriptors` in a fingerprint). This disambiguates
many classes that share strings.

Still hard, and left to the hand-made reference mapping:
- Classes whose superclass is `Object` and that carry no distinctive strings
  (e.g. SettingsActivity vs the debug-menu builder share the same debug
  strings). These need call-graph analysis, planned for later.
