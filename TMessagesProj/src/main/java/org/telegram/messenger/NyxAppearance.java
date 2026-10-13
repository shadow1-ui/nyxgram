package org.telegram.messenger;

import org.telegram.ui.ActionBar.Theme;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** App-global appearance settings, staged by the preview screen. */
public final class NyxAppearance {
    public static boolean get(String key){return NyxConfig.prefs().getBoolean("appearance_"+key,false);}
    public static void set(String key,boolean value){NyxConfig.prefs().edit().putBoolean("appearance_"+key,value).apply();}
    public static int color(int account){return NyxConfig.number("deletedColor",account,0xFFD976A6);}
    public static String symbol(int account){String[] symbols={"✕","⌫","●","▱"};int n=NyxConfig.number("deletedIcon",account,0);return symbols[Math.max(0,Math.min(3,n))];}
    public static boolean hideTime(MessageObject m){
        if(m==null||m.isSendError()||m.isSending())return false;
        return (m.isAnyKindOfSticker()&&get("stickerTime")) || (m.isGif()&&get("gifTime")) || (m.isRoundVideo()&&get("roundTime"));
    }
    public static class Draft {
        public int preset,radius,font;public boolean stickers,gifs,round,avatars,dates;
        public Draft(){radius=SharedConfig.bubbleRadius;font=SharedConfig.fontSize;preset=NyxConfig.prefs().getInt("appearance_preset",0);stickers=get("stickerTime");gifs=get("gifTime");round=get("roundTime");avatars=get("avatars");dates=get("dates");}
        public void preset(int n){preset=n;if(n==0){radius=17;font=16;stickers=gifs=round=avatars=dates=false;}else if(n==1){radius=18;font=16;stickers=true;gifs=round=avatars=dates=false;}else if(n==2){radius=10;font=14;stickers=gifs=round=true;avatars=dates=false;}else{radius=4;font=15;stickers=gifs=round=avatars=dates=true;}}
    }
    public static void apply(Draft d){
        if(!NyxConfig.prefs().contains("appearance_originalTheme"))NyxConfig.prefs().edit().putString("appearance_originalTheme",Theme.getCurrentTheme().getKey()).putInt("appearance_originalRadius",SharedConfig.bubbleRadius).putInt("appearance_originalFont",SharedConfig.fontSize).apply();
        NyxConfig.prefs().edit().putInt("appearance_preset",d.preset).apply();
        set("stickerTime",d.stickers);set("gifTime",d.gifs);set("roundTime",d.round);set("avatars",d.avatars);set("dates",d.dates);
        dimensions(d.radius,d.font);
        if(d.preset==1)applyNight();else if(d.preset==0)restoreThemeOnly();
        NyxConfig.prefs().edit().putInt("appearance_revision",NyxConfig.prefs().getInt("appearance_revision",0)+1).apply();
        Theme.refreshThemeColors();
    }
    private static final int NIGHT_VERSION=2;
    public static void migrateNight(){
        String name=Theme.getCurrentTheme().name;
        if(name!=null && name.startsWith("Nyxgram Night") && NyxConfig.prefs().getInt("nyxNightVersion",0)<NIGHT_VERSION)applyNight();
    }
    public static void applyNight(){
        try{
            Theme.ThemeInfo nativeNight=Theme.getTheme("Dark Blue");
            if(nativeNight==null)throw new IllegalStateException("Native night palette unavailable");
            Theme.applyTheme(nativeNight);
            java.util.LinkedHashMap<String,Integer> colors=new java.util.LinkedHashMap<>();
            for(int i=0;i<Theme.colorsCount;i++){
                String name=org.telegram.ui.ActionBar.ThemeColors.getStringName(i);
                if(name!=null && i!=Theme.key_wallpaperFileOffset)colors.put(name,Theme.getColor(i));
            }
            String[][] groups={
                {"#0B1020","windowBackgroundGray","chat_wallpaper"},
                {"#141B2D","windowBackgroundWhite","dialogBackground","actionBarDefault","actionBarDefaultArchived","chat_messagePanelBackground","chat_emojiPanelBackground","chats_menuBackground","chats_menuTopBackground","chats_menuTopShadow","actionBarActionModeDefault","actionBarActionModeDefaultTop"},
                {"#1C263E","chat_inBubble","chat_botKeyboardButtonBackground","windowBackgroundGrayShadow","dialogBackgroundGray"},
                {"#47336E","chat_outBubble","chat_outBubbleGradient1","chat_outBubbleGradient2","chat_outBubbleGradient3"},
                {"#F1F3FA","windowBackgroundWhiteBlackText","dialogTextBlack","actionBarDefaultTitle","actionBarDefaultIcon","actionBarDefaultSearch","chats_name","chats_nameArchived","chats_archiveText","chats_menuItemText","chats_menuName","chat_messageTextIn","chat_messageTextOut","chat_messagePanelText","chat_botButtonText","profile_title"},
                {"#B7BED2","windowBackgroundWhiteGrayText","windowBackgroundWhiteGrayText2","windowBackgroundWhiteGrayText3","windowBackgroundWhiteHintText","dialogTextGray","dialogTextGray2","dialogTextGray3","dialogTextHint","chats_message","chats_messageArchived","chats_date","chats_menuItemIcon","chats_menuPhone","actionBarDefaultSubtitle","chat_inTimeText","chat_outTimeText","chat_messagePanelHint","chat_messagePanelIcons","chat_inAudioPerformerText","chat_outAudioPerformerText"},
                {"#A98BFA","windowBackgroundWhiteBlueHeader","windowBackgroundWhiteBlueText","windowBackgroundWhiteBlueText2","windowBackgroundWhiteBlueText3","windowBackgroundWhiteBlueIcon","dialogTextBlue","dialogTextBlue2","dialogButton","chat_messagePanelSend","chat_inReplyNameText","chat_outReplyNameText","chat_inReplyLine","chat_outReplyLine","switchTrackChecked","radioBackgroundChecked"},
                {"#70D9E8","chat_messageLinkIn","chat_messageLinkOut","chat_inFileNameText","chat_outFileNameText"},
                {"#26324A","listSelectorSDK21","actionBarDefaultSelector","dialogScrollGlow","switchTrack","divider"}
            };
            for(String[] group:groups){int color=android.graphics.Color.parseColor(group[0]);for(int n=1;n<group.length;n++)colors.put(group[n],color);}
            StringBuilder text=new StringBuilder();for(java.util.Map.Entry<String,Integer> e:colors.entrySet())text.append(e.getKey()).append('=').append(e.getValue()).append('\n');
            File file=new File(ApplicationLoader.getFilesDirFixed(),"nyxgram-night.attheme");
            try(FileOutputStream out=new FileOutputStream(file)){out.write(text.toString().getBytes(StandardCharsets.UTF_8));}
            if(Theme.applyThemeFile(file,"Nyxgram Night",null,false)==null)throw new IllegalStateException("Night palette could not be installed");
            NyxConfig.prefs().edit().putInt("nyxNightVersion",NIGHT_VERSION).apply();
        }catch(Exception error){FileLog.e(error);}
    }
    private static void dimensions(int radius,int font){
        SharedConfig.bubbleRadius=Math.max(0,Math.min(24,radius));SharedConfig.fontSize=Math.max(12,Math.min(24,font));SharedConfig.fontSizeIsDefault=false;
        MessagesController.getGlobalMainSettings().edit().putInt("bubbleRadius",SharedConfig.bubbleRadius).putInt("fons_size",SharedConfig.fontSize).apply();Theme.createCommonMessageResources();
    }
    private static void restoreThemeOnly(){String key=NyxConfig.prefs().getString("appearance_originalTheme","");Theme.ThemeInfo theme=Theme.getTheme(key);if(theme!=null)Theme.applyTheme(theme);}
    public static void restore(){
        restoreThemeOnly();dimensions(NyxConfig.prefs().getInt("appearance_originalRadius",17),NyxConfig.prefs().getInt("appearance_originalFont",16));
        android.content.SharedPreferences.Editor e=NyxConfig.prefs().edit();for(String key:new String[]{"stickerTime","gifTime","roundTime","avatars","dates","preset","originalTheme","originalRadius","originalFont"})e.remove("appearance_"+key);e.apply();NyxConfig.prefs().edit().putInt("appearance_revision",NyxConfig.prefs().getInt("appearance_revision",0)+1).apply();
    }
}
