package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.*;
import org.telegram.ui.Stories.StealthModeAlert;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;

/** Native settings, with one authoritative state for Telegram performance options. */
public class NyxgramActivity extends BaseFragment {
    private final int section;
    private UniversalRecyclerView listView;
    public NyxgramActivity() { this(0); }
    public NyxgramActivity(int section) { this.section = section; }
    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        int[] titles = {R.string.NyxTitle, R.string.NyxPrivacy, R.string.NyxProfileLinks, R.string.NyxPerformance, R.string.NyxAbout, R.string.NyxHistory, R.string.NyxUpdates};
        actionBar.setTitle(getString(titles[section]));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frame;
        listView = new UniversalRecyclerView(context, currentAccount, classGuid, this::fillItems, this::onClick, null, getResourceProvider());
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.adapter.update(false);
        return fragmentView;
    }
    private void check(ArrayList<UItem> items, int id, int label, boolean value) {
        items.add(UItem.asCheck(id, getString(label)).setChecked(value));
    }
    private void hint(ArrayList<UItem> items, int label) { items.add(UItem.asShadow(getString(label))); }
    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (section == 0) {
            check(items, 10, R.string.NyxGhost, NyxConfig.ghost(currentAccount));
            hint(items, R.string.NyxGhostHint);
            items.add(UItem.asButton(1, getString(R.string.NyxPrivacy)));
            items.add(UItem.asButton(2, getString(R.string.NyxInfo)));
            items.add(UItem.asButton(5, getString(R.string.NyxHistory)));
            items.add(UItem.asButton(90, getString(R.string.NyxAppearance)));
            check(items, 69, R.string.NyxLocalPremium, NyxConfig.get("localPremium",currentAccount,false));hint(items,R.string.NyxLocalPremiumHint);
            check(items, 70, R.string.NyxStartup, NyxConfig.prefs().getBoolean("nyxStartup",true));
            items.add(UItem.asButton(68,getString(R.string.NyxLocalPins)));
            items.add(UItem.asButton(3, getString(R.string.NyxPerformance)));
            items.add(UItem.asButton(6, getString(R.string.NyxUpdates)));
            items.add(UItem.asButton(4, getString(R.string.NyxAbout)));
            items.add(UItem.asShadow(null));
            items.add(UItem.asButton(40, getString(R.string.NyxStartupFolder), folderName()));
            hint(items, R.string.NyxFolderHint);
        } else if (section == 1) {
            check(items, 11, R.string.NyxHideRead, NyxConfig.hideRead(currentAccount)); hint(items, R.string.NyxHideReadHint);
            check(items, 12, R.string.NyxHideOnline, NyxConfig.hideOnline(currentAccount)); hint(items, R.string.NyxHideOnlineHint);
            check(items, 13, R.string.NyxHideTyping, NyxConfig.hideTyping(currentAccount)); hint(items, R.string.NyxHideTypingHint);
            items.add(UItem.asButton(14, getString(R.string.NyxStories))); hint(items, R.string.NyxStoriesHint);
            check(items, 18, R.string.NyxStoryClientGhost, NyxConfig.get("storyGhost", currentAccount, false)); hint(items, R.string.NyxStoryClientHint);
            check(items, 15, R.string.NyxHidePhone, NyxConfig.get("hidePhone", currentAccount, false)); hint(items, R.string.NyxHidePhoneHint);
            check(items, 16, R.string.NyxConfirmCalls, NyxConfig.get("confirmCalls", currentAccount, false)); hint(items, R.string.NyxConfirmCallsHint);
            check(items, 17, R.string.NyxConfirmStickers, NyxConfig.get("confirmStickers", currentAccount, true)); hint(items, R.string.NyxConfirmStickersHint);
            check(items, 19, R.string.NyxConfirmGifs, NyxConfig.get("confirmGifs",currentAccount,true));
        } else if (section == 2) {
            check(items, 20, R.string.NyxShowId, NyxConfig.get("showId", currentAccount, true));
            check(items, 21, R.string.NyxShowDc, NyxConfig.get("showDc", currentAccount, true)); hint(items, R.string.NyxDcHint);
            hint(items, R.string.NyxProfileLinkHint);
            items.add(UItem.asButton(22, getString(R.string.NyxSavedProfiles)));
        } else if (section == 3) {
            check(items, 30, R.string.NyxLite, NyxConfig.lite(currentAccount)); hint(items, R.string.NyxLiteHint);
            check(items, 31, R.string.NyxNoBlur, NyxConfig.disabled(NyxConfig.BLUR_FLAGS));
            check(items, 32, R.string.NyxNoAnimations, NyxConfig.noAnimations());
            check(items, 33, R.string.NyxNoEmoji, NyxConfig.disabled(LiteMode.FLAGS_ANIMATED_EMOJI));
            check(items, 34, R.string.NyxNoStickers, NyxConfig.disabled(LiteMode.FLAGS_ANIMATED_STICKERS));
            check(items, 35, R.string.NyxNoGifs, NyxConfig.disabled(LiteMode.FLAG_AUTOPLAY_GIFS));
            check(items, 36, R.string.NyxNoVideos, NyxConfig.disabled(LiteMode.FLAG_AUTOPLAY_VIDEOS)); hint(items, R.string.NyxEffectsHint);
            check(items, 37, R.string.NyxNoDownloads, NyxConfig.noDownloads(currentAccount));
            items.add(UItem.asButton(38, getString(R.string.NyxMediaSettings)));
            items.add(UItem.asButton(39, getString(R.string.NyxCache)));
            items.add(UItem.asShadow(null));
        } else if (section == 5) {
            check(items, 80, R.string.NyxKeepDeleted, NyxConfig.get("keepDeleted", currentAccount, false));
            check(items, 81, R.string.NyxKeepEdits, NyxConfig.get("keepEdits", currentAccount, false)); hint(items, R.string.NyxArchiveHint);
            items.add(UItem.asButton(82, getString(R.string.NyxDeletedArchive)));
            items.add(UItem.asButton(83, getString(R.string.NyxEditsArchive)));
            items.add(UItem.asShadow(null));
            check(items, 84, R.string.NyxProtectArchive, NyxConfig.get("protectArchive", currentAccount, false));
            items.add(UItem.asButton(89, getString(R.string.NyxLockNow))); hint(items, R.string.NyxPinHint);
            int days = NyxConfig.number("retention", currentAccount, 30);
            items.add(UItem.asButton(85, getString(R.string.NyxArchiveRetention), days == 0 ? getString(R.string.NyxRetentionForever) : days + " дн."));
            items.add(UItem.asButton(86, getString(R.string.NyxArchiveMark), NyxAppearance.symbol(currentAccount)));
            items.add(UItem.asButton(87, getString(R.string.NyxArchiveColor)));
            hint(items, R.string.NyxArchiveLimitHint);
            items.add(UItem.asButton(88, getString(R.string.NyxArchiveClear)));
        } else if (section == 6) {
            check(items, 62, R.string.NyxAutoUpdates, NyxConfig.prefs().getBoolean("autoUpdates", true));
            items.add(UItem.asButton(60, getString(R.string.NyxCheckUpdates)));
            items.add(UItem.asButton(61, getString(R.string.NyxUpdateInstall)));
            hint(items, R.string.NyxUpdateHint);
        } else {
            hint(items, R.string.NyxAboutHint);
            items.add(UItem.asButton(-50, "Nyxgram " + NyxUpdates.VERSION + " · Telegram " + BuildVars.BUILD_VERSION_STRING));
            items.add(UItem.asButton(64, getString(R.string.NyxExport)));
            items.add(UItem.asButton(65, getString(R.string.NyxImport))); hint(items, R.string.NyxTransferHint);
            items.add(UItem.asButton(50, getString(R.string.NyxReset)));
        }
    }
    private String folderName() {
        int id = NyxConfig.startupFolder(currentAccount);
        for (MessagesController.DialogFilter f : getMessagesController().getDialogFilters()) {
            if (f.id == id && !f.isDefault() && !f.locked) return f.name;
        }
        return getString(R.string.NyxAllChats);
    }
    private void folders() {
        ArrayList<CharSequence> names = new ArrayList<>(); ArrayList<Integer> ids = new ArrayList<>();
        names.add(getString(R.string.NyxAllChats)); ids.add(0);
        for (MessagesController.DialogFilter f : getMessagesController().getDialogFilters()) {
            if (!f.isDefault() && !f.locked) { names.add(f.name); ids.add(f.id); }
        }
        showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.NyxStartupFolder))
            .setItems(names.toArray(new CharSequence[0]), (dialog, index) -> {
                NyxConfig.setStartupFolder(currentAccount, ids.get(index)); listView.adapter.update(true);
            }).setNegativeButton(getString(R.string.NyxCancel), null).create());
    }
    private void toggle(String key, boolean fallback) { NyxConfig.set(key, currentAccount, !NyxConfig.get(key, currentAccount, fallback)); }
    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case 1: case 2: case 3: case 4: case 5: case 6: presentFragment(new NyxgramActivity(item.id)); return;
            case 10: NyxConfig.setGhost(currentAccount, !NyxConfig.ghost(currentAccount)); break;
            case 11: toggle("hideRead", false); break;
            case 12: toggle("hideOnline", false); getMessagesController().updateNyxOnlineState(); break;
            case 13: toggle("hideTyping", false); break;
            case 14: showDialog(new StealthModeAlert(getContext(), 0, StealthModeAlert.TYPE_FROM_DIALOGS, getResourceProvider())); return;
            case 15: toggle("hidePhone", false); getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_PHONE); break;
            case 16: toggle("confirmCalls", false); break;
            case 17: toggle("confirmStickers", true); break;
            case 18:
                if (NyxConfig.get("storyGhost", currentAccount, false)) toggle("storyGhost", false);
                else {
                    showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.NyxStoryClientGhost)).setMessage(getString(R.string.NyxStoryClientHint))
                        .setPositiveButton(getString(R.string.NyxEnable), (dialog, which) -> { NyxConfig.set("storyGhost", currentAccount, true); listView.adapter.update(false); }).setNegativeButton(getString(R.string.NyxCancel), null).create()); return;
                }
                break;
            case 19: toggle("confirmGifs",true); break;
            case 20: toggle("showId", true); break;
            case 21: toggle("showDc", true); break;
            case 22: presentFragment(new NyxProfilesActivity()); return;
            case 30: NyxConfig.setLite(currentAccount, !NyxConfig.lite(currentAccount)); break;
            case 31: NyxConfig.setDisabled(NyxConfig.BLUR_FLAGS, !NyxConfig.disabled(NyxConfig.BLUR_FLAGS)); break;
            case 32: NyxConfig.setNoAnimations(!NyxConfig.noAnimations()); break;
            case 33: NyxConfig.setDisabled(LiteMode.FLAGS_ANIMATED_EMOJI, !NyxConfig.disabled(LiteMode.FLAGS_ANIMATED_EMOJI)); break;
            case 34: NyxConfig.setDisabled(LiteMode.FLAGS_ANIMATED_STICKERS, !NyxConfig.disabled(LiteMode.FLAGS_ANIMATED_STICKERS)); break;
            case 35: NyxConfig.setDisabled(LiteMode.FLAG_AUTOPLAY_GIFS, !NyxConfig.disabled(LiteMode.FLAG_AUTOPLAY_GIFS)); break;
            case 36: NyxConfig.setDisabled(LiteMode.FLAG_AUTOPLAY_VIDEOS, !NyxConfig.disabled(LiteMode.FLAG_AUTOPLAY_VIDEOS)); break;
            case 37: NyxConfig.setNoDownloads(currentAccount, !NyxConfig.noDownloads(currentAccount)); break;
            case 38: presentFragment(new DataSettingsActivity()); return;
            case 39: presentFragment(new CacheControlActivity()); return;
            case 40: folders(); return;
            case 68: presentFragment(new NyxLocalPinsActivity()); return;
            case 69: NyxPremium.setEnabled(currentAccount,!NyxPremium.enabled(currentAccount)); break;
            case 70: NyxConfig.prefs().edit().putBoolean("nyxStartup",!NyxConfig.prefs().getBoolean("nyxStartup",true)).apply();break;
            case 60: NyxUpdates.check(this, true); return;
            case 61: NyxUpdates.installDownloaded(this); return;
            case 62: NyxConfig.prefs().edit().putBoolean("autoUpdates", !NyxConfig.prefs().getBoolean("autoUpdates", true)).apply(); break;
            case 64: NyxSettingsTransfer.choose(this, true); return;
            case 65: NyxSettingsTransfer.choose(this, false); return;
            case 80: archiveToggle("keepDeleted"); return;
            case 81: archiveToggle("keepEdits"); return;
            case 82: NyxArchiveGate.require(currentAccount, this, () -> presentFragment(new NyxArchiveActivity(0, NyxArchive.DELETED))); return;
            case 83: NyxArchiveGate.require(currentAccount, this, () -> presentFragment(new NyxArchiveActivity(0, NyxArchive.EDITED))); return;
            case 84: NyxArchiveGate.authorize(currentAccount, this, () -> { toggle("protectArchive", false); listView.adapter.update(false); }); return;
            case 85: chooseNumber("retention", new int[]{7, 30, 90, 365, 0}, new String[]{getString(R.string.NyxDays7), getString(R.string.NyxDays30), getString(R.string.NyxDays90), getString(R.string.NyxDays365), getString(R.string.NyxRetentionForever)}, R.string.NyxArchiveRetention); return;
            case 86: chooseNumber("deletedIcon", new int[]{0,1,2,3}, new String[]{"✕", "⌫", "●", "▱"}, R.string.NyxArchiveMark); return;
            case 87: chooseNumber("deletedColor", new int[]{0xFFD976A6,0xFFA98BFA,0xFF70D9E8,0xFFEF7878}, new String[]{getString(R.string.NyxPink),getString(R.string.NyxPurple),getString(R.string.NyxCyan),getString(R.string.NyxRed)}, R.string.NyxArchiveColor); return;
            case 88: NyxArchiveGate.require(currentAccount, this, () -> showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.NyxArchiveClear)).setMessage(getString(R.string.NyxArchiveClearAsk))
                .setPositiveButton(getString(R.string.NyxResetAction), (d,w) -> NyxArchive.clear(currentAccount, false, () -> Toast.makeText(getParentActivity(),getString(R.string.NyxDone),Toast.LENGTH_SHORT).show())).setNegativeButton(getString(R.string.NyxCancel), null).create())); return;
            case 89: NyxArchiveGate.lock(); break;
            case 90: presentFragment(new NyxAppearanceActivity()); return;
            case 50:
                showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.NyxReset))
                    .setMessage(getString(R.string.NyxResetQuestion))
                    .setPositiveButton(getString(R.string.NyxResetAction), (dialog, which) -> {
                        NyxConfig.reset(currentAccount); listView.adapter.update(true);
                        getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_PHONE);
                    }).setNegativeButton(getString(R.string.NyxCancel), null).create()); return;
            default: return;
        }
        listView.adapter.update(true);
    }
    private void archiveToggle(String key) {
        if (NyxConfig.get(key, currentAccount, false)) { NyxConfig.set(key, currentAccount, false); archiveChanged(key); listView.adapter.update(false); return; }
        showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.NyxHistory)).setMessage(getString(R.string.NyxArchiveConsent))
            .setPositiveButton(getString(R.string.NyxEnable), (d,w) -> { NyxConfig.set(key, currentAccount, true); archiveChanged(key); listView.adapter.update(false); }).setNegativeButton(getString(R.string.NyxCancel), null).create());
    }
    private void archiveChanged(String key) {
        if ("keepDeleted".equals(key)) getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces,
                MessagesController.UPDATE_MASK_ALL, "nyx_deleted_visibility", getUserConfig().getClientUserId());
    }
    private void chooseNumber(String key, int[] numbers, String[] labels, int title) {
        showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(title)).setItems(labels, (d,n) -> { NyxConfig.setNumber(key, currentAccount, numbers[n]); if ("deletedIcon".equals(key) || "deletedColor".equals(key)) getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL, "nyx_archive_style"); listView.adapter.update(false); }).setNegativeButton(getString(R.string.NyxCancel), null).create());
    }
    @Override public void onActivityResultFragment(int request, int result, android.content.Intent data) {
        if (!NyxSettingsTransfer.result(this, request, result, data)) super.onActivityResultFragment(request, result, data);
    }
    @Override public void onResume() { super.onResume(); if (listView != null) listView.adapter.update(false); }
}
