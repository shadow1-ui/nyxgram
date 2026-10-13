# Nyxgram 0.2 (Android / arm64)

Independent Telegram fork. Update installs over the initial Nyxgram APK with the same signing key. Do not uninstall to update.

## Features

- Settings → Nyxgram: Privacy, Information, Performance, About.
- User/chat/channel internal MTProto IDs and profile-photo DC in profiles, tap to copy.
- Separate username and public-profile-link copy actions (only for public usernames).
- Account-local read receipt, online status, typing/action suppression; all off by default.
- Quick ghost toggle in the home overflow menu and chat menu.
- Explicit manual server read action in a normal chat, with confirmation and server error reporting.
- Official Premium story-stealth entry point (server rules unchanged).
- Own-phone masking on the main settings/account/profile screens; number-change/security screens are not a privacy boundary.
- Confirmation before private calls and group-call creation/joining.
- Confirmation before ordinary static, animated and video sticker sends.
- Lightweight preset and individual native effect/media autoplay toggles; no duplicate shadow performance settings.
- Disable media autodownload for mobile, Wi-Fi and roaming; open native settings for granular limits.
- Native cache management shortcut. No silent cache deletion.
- Preferred startup folder by server filter ID. Deleted/locked folders fall back to All Chats.
- Nyx settings reset with confirmation, preserving chats/files and other accounts' privacy preferences.
- Formal Russian Nyx UI. Native Telegram dialogs follow the chosen app language.

## Privacy limits

This is not an anonymity tool. Sending messages/calling/other sessions can reveal activity. Story or channel views are not hidden by chat ghost mode. Story stealth requires official Telegram Premium support and follows Telegram cooldown/duration rules.

Secret chats, disappearing/view-once media and known auto-delete chats keep original receipt/timer behavior. Local read state can differ from server state. Voice playback receipts are not resent by the manual chat-read button. Bot stream-stop control and live group-call speaking signaling keep their original behavior. App restart clears the bounded suppressed-read task cache; manually reading still works for messages currently loaded in the chat.

DC is **photo storage DC**, not authoritative account DC. IDs shown are internal MTProto peer IDs (not Bot API signed chat IDs). Username links are public profiles, not private invitation links.

Effects are application-global; autodownload/privacy/default-folder preferences are account-local. Reset restores an effects snapshot for the whole application and an autodownload-enabled snapshot for the current account. It does not clear native cache, account privacy settings, messages or sign-in data.

Deleted-message history and protected-content features are **not included** in this version.

## Validation

- Changed Java files: parser syntax validation.
- Strings XML: parse/unique-name/reference checks.
- NyxConfig: 19 isolated behavior assertions (defaults, account isolation, ghost bundle, native state, preserving unrelated flags, exact restore, reset, phone masking, startup folder).
- CI compiles Java before the native release build (fail-fast).
- Device validation still required: login, ordinary messaging, ghost mode vs. another account, voice/view-once/secret-chat timers, stickers cancel/send, calls cancel/start, folder cold start, performance restore, and installation over the previous signed APK.
