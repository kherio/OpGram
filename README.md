# OpGram

OpGram is an Xposed/LSPosed module that adds a large set of features to Telegram.
It is a fork of **TeleVip**, originally created by **@m_1_iq**
(https://github.com/mustafa1dev/TeleVip-Lsposed). TeleVip is GPL-licensed and the
attribution to the original author is kept throughout the module.

## Features

### Advanced Mode (privacy)
- Hide *seen* / read receipts, in private chats and in channels/groups.
- Hide *typing…*, and separately hide *recording audio/video* and *sending…*.
- Hide *online* status (reduces how often you appear online).
- Hide story views, and hide your phone number.
- Local Telegram Premium (client-side).
- Don't send *listened* for voice notes and round video messages.
- Per-chat exceptions: excluded chats behave like normal Telegram.
- "Mark as read now" for a single chat while Advanced Mode is on.

### Deleted & edited messages
- Keep messages that were deleted, so you can still read them.
- Save the edit history of messages.
- Show the exact deletion time next to the "deleted" tag.
- Built-in viewer for the saved edit history, with search.
- Alerts when a message is deleted or edited.
- Clear the saved edit history.

### Message time & status
- Show seconds in the message time.
- Show the exact last-seen date and time (when the other user shares it).
- Show the user ID and the message ID.
- Copy a profile name with one tap.
- Disable number rounding (show exact counts).

### Media & files
- Save secret ("view once") media and media from restricted chats.
- Allow saving stories and voice messages.
- Remove the "content saving restricted" limit.
- Set a default playback speed for voice notes and round videos.
- Confirm before sending stickers, GIFs and voice messages.
- Faster downloads.

### Privacy & security
- Hide sponsored messages (channel ads).
- Hide the proxy sponsor.
- Lock selected chats behind the device fingerprint/credential (Android 9+).

### Interface
- Hide pinned messages, disable swipe-back in chats and profiles, hide update
  prompts, hide TL errors.
- Searchable settings with collapsible sections (tap a header to fold it).

### Battery & data
- Don't preload stories in the background.
- Hide the stories bar in the chat list.

### Backup & updates
- Export / import OpGram settings.
- Built-in updater: checks GitHub Releases, shows the changelog, and downloads
  and installs new versions.
- Safe mode: if Telegram crashes several times on startup, OpGram disables its
  hooks and offers to re-enable them.

## Install

1. Install OpGram.
2. Enable it in LSPosed/Vector and select Telegram in its scope.
3. Force-stop Telegram and open it again.

Open **Settings → Advanced Mode** inside Telegram to configure OpGram.

## Updating

OpGram checks GitHub Releases for a newer version and can download and install it
(**Settings → Check for updates**, or automatically). In-place updates require
that **every release is signed with the same key**.

## Building

Standard Gradle project:

```
./gradlew :app:assembleRelease
```

CI (`.github/workflows/build.yml`) builds on every push, on pull requests and on
release. Add the signing secrets described in `.github/README-CI.md` to have CI
produce a signed APK and attach it to releases.

## Mapping (for new Telegram versions)

Telegram ships R8-obfuscated, so each release renames the classes OpGram hooks.
The mapping lives in `app/src/main/assets/clients/TelegramWeb-<versionCode>.json`
and OpGram loads the file matching the installed Telegram, falling back to the
generic one. `tools/mapgen/` contains a generator that resolves the renamed
classes automatically from a new APK; see `tools/mapgen/README.md`.

## Credits

- Original module: **TeleVip** by @m_1_iq — https://github.com/mustafa1dev/TeleVip-Lsposed
- This fork: OpGram — https://github.com/kherio/OpGram

## License

GPL, inherited from TeleVip. Keep the attribution to the original author.
