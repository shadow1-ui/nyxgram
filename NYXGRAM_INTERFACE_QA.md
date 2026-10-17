# Interface and audio refinement — review build

## Changes

- Nyxgram home settings are grouped into Style, Privacy and Application, with native icons instead of one long unstructured list. No privacy flags or recording switches are enabled automatically.
- The optional Nyx night palette uses a softer ink/graphite background, muted plum outgoing bubbles, lavender accents and cyan links. Only Apply/Nyx night (or migration of an already-selected Nyx night theme) changes the chat palette; ordinary user themes are untouched.
- Profiles retain ID/DC values, tap-to-copy and link/bookmark actions. Repetitive ID/DC instructions are removed from the profile subtitle and settings. DC still means photo storage, not the account's physical location.
- Existing local deleted-message rendering stays faded at 225/255 opacity, with the timestamp's existing measured/colored marker. The default marker is a text-presentation trash symbol; existing icon indices and privacy/archive rules are retained. This is not recovery of messages never received by this client.
- Tapping the audio bar opens the native music player for music and a dedicated native queue for ordinary voice/round-video messages. Voice queue: current item, remaining native queue, pause/resume, seek, next, playback speed, current-item highlighting, accessible labels. Long press on the bar preserves jump-to-message navigation.
- Queue snapshots are defensive copies. Selection revalidates the live queue, account and dialog, preserves the unread policy and subsequent autoplay, and rejects stale, foreign-account, foreign-dialog, local deleted-copy and view-once entries. The UI dismisses after account/owner/playback-type changes and cancels its refresh callback on dismissal. It does not build a global playlist of all voice messages or download/record extra media.

## Automated checks run locally

- `python3 tools/check_nyx_startup.py`
- `python3 tools/test_nyx_deleted.py` — 274 executable lifecycle/page/thread assertions plus integration checks.
- `python3 tools/test_nyx_interface.py` — 23 executable assertions against the exact new production queue methods, with a stub playback boundary; javac syntax parsing of six changed Java files; resource references and interface/privacy wiring.
- `git diff --check`

The lightweight PR workflow runs these checks without API/signing secrets and cannot publish a release.

## Not yet verified

No complete Android build, emulator or physical device was available in the editing environment (no Android SDK/NDK; Java 25 only, rather than the project's Java 17 build toolchain). Syntax parsing is NOT Android type checking; stub playback tests are NOT ExoPlayer/runtime verification. Keep the PR in draft until CI compilation and device checks succeed. No APK was produced or released.

## Device acceptance checklist

- [ ] Compile the existing Java/Android targets using Java 17 and the configured Android 36 SDK/NDK.
- [ ] Music: tap the bar from chats, chat list and side-menu contexts; native player opens, pause/seek/shuffle/repeat still work, queue changes update.
- [ ] Voice/round video: one-item queue opens; multiple items can be selected; next/automatic continuation preserve unread policy; downloaded and loading media behave normally.
- [ ] Seeking, speed, pause/resume, incoming calls, Bluetooth/headphones, screen rotation and background/foreground transitions.
- [ ] A track ending, being deleted, stopping, changing to music or switching accounts/logging out closes or updates the sheet without leaking the previous owner's entries.
- [ ] View-once/round-once messages are not exposed through the queue. Local deleted copies are not exposed through it, including when archive PIN protection is locked.
- [ ] Long press on the audio bar navigates to the source; existing play/close/speed controls do not trigger the sheet.
- [ ] ID only / DC only / both / neither, copy and long-press links, profiles without a photo or username.
- [ ] Received deleted text, media, albums, threads/topics: remain in place with the marker, archive lock and preservation on/off work; secret/TTL/view-once exclusions remain intact.
- [ ] Trash marker glyph on supported Android fonts, large font, light/dark/custom themes, narrow screens, TalkBack and RTL. This Android change does not replace the timestamp renderer with a vector icon.
- [ ] Explicit Apply/Restore of the theme, existing Nyx theme migration, selected-message visibility and link/timestamp contrast on a device.
