# Nyxgram 0.4.1 — Android arm64

## Scope

Includes the user's next feature set and optional ideas A/G/D/E/J only: edit history, archive search, appearance preview, allowlisted settings transfer and archive PIN gate. It does not add per-chat ghost exceptions, custom local contact names or lightweight-mode per-media exceptions.

### Profiles

One ID/DC row; normal tap copies only the raw internal MTProto peer ID. DC is photo storage DC and cannot classify physical/virtual phone numbers. Unknown DC shows a dash. Existing username row opens copy username/public link/ID link/bookmark choices. Long press on the ID row works without a username. `tg://openmessage?user_id=...&nyxprofile=1` routes to a Nyxgram profile. Other clients may ignore the profile parameter. Existing cached profile access is required, and account deletion/access loss is not bypassed. Local bookmarks are scoped to the signed-in user's ID and capped at 200.

### Local history

Recording defaults off. Independent SQLite snapshots/history, with each serialized message body encrypted using AndroidKeyStore AES-256-GCM and owner/dialog/message/kind AAD. Code protection is a separate UI gate, not PIN-derived encryption. Native Telegram database deletion is not bypassed. Native storage queue copies snapshots before writes/deletions. Edit history compares previous text/media identity; formatting-only edits are not guaranteed. Only locally available previous versions can be saved.

Secret chats, service messages, messages with TTL/TTL-period, and view-once/disappearing media are excluded. No automatic media downloading/copying. Chat rendering restores only available cached media; missing files become a local placeholder. Archive entries are local read-only copies, not sent/forwarded/edited on Telegram. Normal non-thread chat pages merge deleted copies within the loaded range; an empty page may show recent local deletions. Topics/merged migration histories use the separate archive screen.

Search decrypts locally on a dedicated queue. 100-entry pages, owner isolation, default 30-day retention; configurable 7/30/90/365/no deadline, still bounded to 8,000 snapshots and 3,000 history rows per owner. Disabling recording does not erase history. Clear removes both snapshots and history. Unknown callbacks/reused account slots cannot display another owner's data.

Archive/PIN/preferences/downloads are excluded from Android full backup and device transfer. No archive data/keys/PIN verifiers are included in settings exports. Normal Telegram backup handling is otherwise preserved.

PIN: 4–12 digits, 24-byte salt, PBKDF2-HMAC-SHA1 (210,000 iterations, 256-bit verifier), constant-time equality, persisted throttling after five failures. Five-minute UI session, closed on app background. Locked chat rows show a placeholder and suppress plaintext accessibility nodes/touches. Protected dialogs close on relock. Protected archive/chat windows use FLAG_SECURE; forgetting a PIN requires deleting all accounts' local history before clearing the verifier. This is not protection against a rooted/compromised device.

### Appearance / settings transfer

Application-global style state; native Telegram font/radius settings. Staged schematic preview, Apply-only mutation, Standard/Nyx/Compact/Minimal presets. Nyx imports a local static night theme, without cloud theme publication. Time/status hiding does not hide sending/error states. Date separators can be visually hidden, not server-deleted. Appearance restore keeps the original local theme/font/radius.

JSON export/import uses Android document picker, schema version 1, strict known-value/type/range checks, 64 KiB limit and explicit import confirmation. Only UI/native effect/plain privacy switches and deletion marker appearance are transferred. Recording flags, PIN/protection, messages, owners, saved profiles, folder IDs, restore snapshots and auth material are excluded. Unknown fields are never applied.

### Stories

Experimental client-side suppression guards both `stories.readStories` and `stories.incrementStoryViews`; local read bookkeeping continues, so suppressed views are not automatically replayed later. No Premium entitlement is forged. Server-backed Premium stealth is still available separately. Reactions/replies and other clients can reveal activity. Real second-account verification remains necessary; no anonymity guarantee.

### Updates

Only `shadow1-ui/nyxgram` published, non-prerelease latest release; no GitHub token in APK. TLS host allowlist, bounded metadata/file sizes, SHA-256, exact application ID/version and installed signer match before showing system installer. Files stay in a private whitelisted FileProvider directory. Installation requires user confirmation and Android permission. Monotonic CI versionCode derives from run number. The first updater-enabled APK must be installed manually.

Push builds create a **draft** release with APK and checksum manifest. Publication is explicit (`publish_release` manual dispatch or promotion of a tested draft), preventing untested builds from automatically appearing in the updater. Existing installed users see only published stable releases.

## QA

Changed Java syntax, resource XML/reference checks and native CI compile-before-native step. Behavior/regression/crypto/transfer invariant tests accompany development. On-device testing is still required for UI layouts, archive lifecycle/PIN/screenshots, media cache behavior, ghost stories from a second account, Android document picker and installer permissions. A successful build is not a claim that these device tests ran.

## Media send preview (0.3.1)

Separate sticker/GIF switches default enabled; existing explicit sticker setting respected. Native ImageReceiver previews static/TGS/video stickers and GIF documents/web inline results. Explicit selected-preview playback is separate from ordinary chat autoplay. Cancel/back/outside dismiss does not send. Primary panel, sticker suggestions and inline-GIF routes intercept before send/input-reset side effects. Central helpers enforce fallback preview for other pickers.

Approval is synchronous, per-thread and scoped to account/peer/exact selected media; inline results with a document normalize to that document. It is passed explicitly through schedule/payment callbacks and removed in finally, not a persisted flag. Completion rechecks foreground fragment/account/owner/dialog. Native rights, payments and scheduling still apply. Receiver stops/clears on dismissal. v1 settings files without the GIF switch import with the default enabled.

Device QA required: static/TGS/video stickers and saved/search GIF animation; Cancel retains reply/text and sends zero messages; Send once; scheduled/paid sends; disabled switches; long-press menus; background/account switches; offline media; no duplicated dialog. Static and CI checks are not device tests.

## Client-side Premium and deletion rendering (0.4.1)

Local Premium is switchable and distinct from `UserConfig.isPremium()`, which remains the genuine server entitlement. Own-profile badge/status, emoji selection and local sticker effect display are enabled. Paid custom emoji in ordinary outgoing chats use their underlying text for the server; owner-scoped bounded metadata restores animation only on this device. Free emoji entities and real subscribers are unaffected. Local status is not sent as an RPC. Collectible statuses, paid reactions, boosts, upload limits and transcription are not unlocked.

Deleted regular messages remain directly in the conversation, slightly faded, with the configurable symbol/color incorporated into the timestamp layout (including reserved width). Native Telegram deletion/storage processing still proceeds. Turning preservation off hides local copies without clearing encrypted history. Archive/PIN controls and exclusions remain in force. Local copies cannot recover messages never received before deletion.
