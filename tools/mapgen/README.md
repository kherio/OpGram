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
