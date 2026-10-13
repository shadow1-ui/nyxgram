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
        int[] titles = {R.string.NyxTitle, R.string.NyxPrivacy, R.string.NyxInfo, R.string.NyxPerformance, R.string.NyxAbout};
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
            items.add(UItem.asButton(3, getString(R.string.NyxPerformance)));
            items.add(UItem.asButton(4, getString(R.string.NyxAbout)));
            items.add(UItem.asShadow(null));
            items.add(UItem.asButton(40, getString(R.string.NyxStartupFolder), folderName()));
            hint(items, R.string.NyxFolderHint);
        } else if (section == 1) {
            check(items, 11, R.string.NyxHideRead, NyxConfig.hideRead(currentAccount)); hint(items, R.string.NyxHideReadHint);
            check(items, 12, R.string.NyxHideOnline, NyxConfig.hideOnline(currentAccount)); hint(items, R.string.NyxHideOnlineHint);
            check(items, 13, R.string.NyxHideTyping, NyxConfig.hideTyping(currentAccount)); hint(items, R.string.NyxHideTypingHint);
            items.add(UItem.asButton(14, getString(R.string.NyxStories))); hint(items, R.string.NyxStoriesHint);
            check(items, 15, R.string.NyxHidePhone, NyxConfig.get("hidePhone", currentAccount, false)); hint(items, R.string.NyxHidePhoneHint);
            check(items, 16, R.string.NyxConfirmCalls, NyxConfig.get("confirmCalls", currentAccount, false)); hint(items, R.string.NyxConfirmCallsHint);
            check(items, 17, R.string.NyxConfirmStickers, NyxConfig.get("confirmStickers", currentAccount, false)); hint(items, R.string.NyxConfirmStickersHint);
        } else if (section == 2) {
            check(items, 20, R.string.NyxShowId, NyxConfig.get("showId", currentAccount, true));
            check(items, 21, R.string.NyxShowDc, NyxConfig.get("showDc", currentAccount, true)); hint(items, R.string.NyxDcHint);
            hint(items, R.string.NyxCopyHint);
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
        } else {
            hint(items, R.string.NyxAboutHint);
            items.add(UItem.asButton(-50, "Nyxgram 0.2 · Telegram " + BuildVars.BUILD_VERSION_STRING));
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
            case 1: case 2: case 3: case 4: presentFragment(new NyxgramActivity(item.id)); return;
            case 10: NyxConfig.setGhost(currentAccount, !NyxConfig.ghost(currentAccount)); break;
            case 11: toggle("hideRead", false); break;
            case 12: toggle("hideOnline", false); getMessagesController().updateNyxOnlineState(); break;
            case 13: toggle("hideTyping", false); break;
            case 14: showDialog(new StealthModeAlert(getContext(), 0, StealthModeAlert.TYPE_FROM_DIALOGS, getResourceProvider())); return;
            case 15: toggle("hidePhone", false); getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_PHONE); break;
            case 16: toggle("confirmCalls", false); break;
            case 17: toggle("confirmStickers", false); break;
            case 20: toggle("showId", true); break;
            case 21: toggle("showDc", true); break;
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
    @Override public void onResume() { super.onResume(); if (listView != null) listView.adapter.update(false); }
}
