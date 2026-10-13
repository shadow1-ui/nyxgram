package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.ui.Components.AnimatedEmojiDrawable;

/** Nyx preferences never contain credentials or message contents. */
public final class NyxConfig {
    private NyxConfig() {}
    public static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("nyxgram", Context.MODE_PRIVATE);
    }
    public static boolean get(String key, int account, boolean fallback) {
        return prefs().getBoolean(key + "_" + account, fallback);
    }
    public static void set(String key, int account, boolean value) {
        prefs().edit().putBoolean(key + "_" + account, value).apply();
    }
    public static int number(String key, int account, int fallback) { return prefs().getInt(key + "_" + account, fallback); }
    public static void setNumber(String key, int account, int value) { prefs().edit().putInt(key + "_" + account, value).apply(); }
    public static boolean hideRead(int account) { return get("hideRead", account, false); }
    public static boolean hideOnline(int account) { return get("hideOnline", account, false); }
    public static boolean hideTyping(int account) { return get("hideTyping", account, false); }
    public static boolean ghost(int account) { return hideRead(account) && hideOnline(account) && hideTyping(account); }
    public static void setGhost(int account, boolean value) {
        prefs().edit().putBoolean("hideRead_" + account, value)
            .putBoolean("hideOnline_" + account, value).putBoolean("hideTyping_" + account, value).apply();
        MessagesController.getInstance(account).updateNyxOnlineState();
    }
    public static String displayPhone(org.telegram.tgnet.TLRPC.User user, int account) {
        if (user == null) return "";
        if (user.id == UserConfig.getInstance(account).getClientUserId() && get("hidePhone", account, false)) {
            return LocaleController.getString(R.string.NyxPhoneHidden);
        }
        return org.telegram.PhoneFormat.PhoneFormat.getInstance().format("+" + user.phone);
    }
    public static int startupFolder(int account) { return prefs().getInt("startupFolder_" + account, 0); }
    public static void setStartupFolder(int account, int id) { prefs().edit().putInt("startupFolder_" + account, id).apply(); }

    public static final int BLUR_FLAGS = LiteMode.FLAG_CHAT_BLUR | LiteMode.FLAG_LIQUID_GLASS;
    public static final int ANIMATION_FLAGS = LiteMode.FLAG_CHAT_BACKGROUND | LiteMode.FLAG_CHAT_SCALE |
        LiteMode.FLAG_CHAT_THANOS | LiteMode.FLAG_PARTICLES | LiteMode.FLAG_CALLS_ANIMATIONS | LiteMode.FLAG_CHAT_SPOILER;
    public static final int EFFECT_FLAGS = BLUR_FLAGS | ANIMATION_FLAGS | LiteMode.FLAGS_ANIMATED_EMOJI |
        LiteMode.FLAGS_ANIMATED_STICKERS | LiteMode.FLAG_AUTOPLAY_GIFS | LiteMode.FLAG_AUTOPLAY_VIDEOS;

    private static void snapshotEffects() {
        if (!prefs().contains("originalEffects")) {
            prefs().edit().putInt("originalEffects", LiteMode.getValue(true) & EFFECT_FLAGS)
                .putBoolean("originalAnimations", SharedConfig.animationsEnabled()).apply();
        }
    }
    public static boolean disabled(int flags) { return (LiteMode.getValue(true) & flags) == 0; }
    public static void setDisabled(int flags, boolean disabled) {
        snapshotEffects();
        int old = LiteMode.getValue(true);
        LiteMode.setAllFlags(disabled ? old & ~flags : old | flags);
        AnimatedEmojiDrawable.updateAll();
        SvgHelper.SvgDrawable.updateLiteValues();
        org.telegram.ui.ActionBar.Theme.reloadWallpaper(true);
    }
    public static boolean noAnimations() { return disabled(ANIMATION_FLAGS) && !SharedConfig.animationsEnabled(); }
    public static void setNoAnimations(boolean disabled) {
        setDisabled(ANIMATION_FLAGS, disabled);
        SharedConfig.setAnimationsEnabled(!disabled);
        MessagesController.getGlobalMainSettings().edit().putBoolean("view_animations", !disabled).apply();
    }
    public static boolean noDownloads(int account) {
        DownloadController d = DownloadController.getInstance(account);
        return !d.mobilePreset.enabled && !d.wifiPreset.enabled && !d.roamingPreset.enabled;
    }
    public static void setNoDownloads(int account, boolean disabled) {
        setNoDownloads(account, disabled, false);
    }
    private static void setNoDownloads(int account, boolean disabled, boolean exactRestore) {
        DownloadController d = DownloadController.getInstance(account);
        String key = "downloadSnapshot_" + account;
        if (disabled && !prefs().getBoolean(key, false)) {
            prefs().edit().putBoolean(key, true).putBoolean("mobile_" + account, d.mobilePreset.enabled)
                .putBoolean("wifi_" + account, d.wifiPreset.enabled).putBoolean("roaming_" + account, d.roamingPreset.enabled).apply();
        }
        d.mobilePreset.enabled = !disabled && prefs().getBoolean("mobile_" + account, true);
        d.wifiPreset.enabled = !disabled && prefs().getBoolean("wifi_" + account, true);
        d.roamingPreset.enabled = !disabled && prefs().getBoolean("roaming_" + account, false);
        if (!disabled && !exactRestore && !d.mobilePreset.enabled && !d.wifiPreset.enabled && !d.roamingPreset.enabled) {
            d.mobilePreset.enabled = d.wifiPreset.enabled = true;
        }
        if (!disabled) prefs().edit().remove(key).remove("mobile_" + account).remove("wifi_" + account).remove("roaming_" + account).apply();
        MessagesController.getMainSettings(account).edit().putString("mobilePreset", d.mobilePreset.toString())
            .putString("wifiPreset", d.wifiPreset.toString()).putString("roamingPreset", d.roamingPreset.toString()).apply();
        d.checkAutodownloadSettings();
    }
    public static boolean lite(int account) { return disabled(EFFECT_FLAGS) && !SharedConfig.animationsEnabled() && noDownloads(account); }
    public static void setLite(int account, boolean enabled) {
        if (enabled) {
            setDisabled(EFFECT_FLAGS, true);
            setNoAnimations(true);
            setNoDownloads(account, true);
        } else {
            restoreEffects();
            if (prefs().getBoolean("downloadSnapshot_" + account, false)) setNoDownloads(account, false, true);
        }
    }
    private static void restoreEffects() {
        if (!prefs().contains("originalEffects")) return;
        int value = (LiteMode.getValue(true) & ~EFFECT_FLAGS) | prefs().getInt("originalEffects", 0);
        LiteMode.setAllFlags(value);
        boolean animations = prefs().getBoolean("originalAnimations", true);
        SharedConfig.setAnimationsEnabled(animations);
        MessagesController.getGlobalMainSettings().edit().putBoolean("view_animations", animations).apply();
        prefs().edit().remove("originalEffects").remove("originalAnimations").apply();
        AnimatedEmojiDrawable.updateAll();
        SvgHelper.SvgDrawable.updateLiteValues();
        org.telegram.ui.ActionBar.Theme.reloadWallpaper(true);
    }
    public static void reset(int account) {
        setLite(account, false);
        NyxAppearance.restore();
        SharedPreferences.Editor e = prefs().edit();
        for (String key : new String[]{"hideRead", "hideOnline", "hideTyping", "hidePhone", "confirmCalls", "confirmStickers", "showId", "showDc", "startupFolder", "storyGhost"}) {
            e.remove(key + "_" + account);
        }
        e.apply();
        MessagesController.getInstance(account).clearNyxReadState();
        MessagesController.getInstance(account).updateNyxOnlineState();
    }
}
