# Changelog

All notable changes to OpGram are documented here.

## 1.3

Added
- Undo send: hold an outgoing message a few seconds so it can be cancelled.
- Message reminders ("remind me": in 1 hour / tonight / tomorrow), from the chat
  menu, delivered as a notification.
- Saved quick-reply templates with variables ({date}, {time}, {datetime}).

## 1.2

Added
- Searchable settings with collapsible sections (tap a header to fold it).
- Alerts when a message is deleted or edited.
- Update dialog now shows the release changelog.

Improved
- Mapping generator: multi-pass resolution and matching by obfuscated
  superclass / method descriptors, disambiguating more classes.

## 1.1

Added
- Telegram Web 12.10.4 support (R8 obfuscation mapping + parameter resolver).
- Advanced Mode extras: per-chat exceptions, "mark as read now".
- Don't send "listened" for voice/round videos; hide "recording"/"sending".
- Exact last-seen date/time; seconds in message time; deletion time.
- Default voice/round-video playback speed.
- Confirm before sending stickers, GIFs and voice.
- Hide sponsored messages.
- Chat lock with fingerprint/credential (Android 9+).
- Battery savers: don't preload stories, hide the stories bar.
- Saved-edits viewer with search; export/import settings; clear edits history.
- In-app updater (checks GitHub Releases) and safe mode against crash loops.
- Mapping generator (`tools/mapgen`) and GitHub Actions CI.

Changed
- Rebranded from TeleVip to OpGram (original credit to @m_1_iq kept).
- Panel header and system app name show "OpGram <version>".
- "Ghost Mode" renamed to "Advanced Mode".
