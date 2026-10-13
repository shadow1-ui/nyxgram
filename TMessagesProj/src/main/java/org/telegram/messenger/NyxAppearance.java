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
        if(d.preset==1){
            try{
                File file=new File(ApplicationLoader.getFilesDirFixed(),"nyxgram-night.attheme");
                String text="windowBackgroundWhite=-15198175\nwindowBackgroundGray=-15986155\nwindowBackgroundWhiteBlackText=-1447184\nwindowBackgroundWhiteGrayText=-6314328\nwindowBackgroundWhiteGrayText2=-6314328\nwindowBackgroundWhiteBlueHeader=-5588737\nactionBarDefault=-15526620\nactionBarDefaultIcon=-1447184\nactionBarDefaultTitle=-1447184\nactionBarDefaultSubtitle=-6314328\nactionBarDefaultSelector=-13221042\nchat_wallpaper=-15986155\nchat_inBubble=-14341831\nchat_outBubble=-11199887\nchat_messageTextIn=-1447184\nchat_messageTextOut=-1\nchat_inTimeText=-6314328\nchat_outTimeText=-2950145\nchat_inReplyNameText=-5588737\nchat_outReplyNameText=-6110721\nchat_inReplyMessageText=-1447184\nchat_outReplyMessageText=-1\nchat_inReplyLine=-5588737\nchat_outReplyLine=-6110721\nchat_inFileNameText=-1447184\nchat_outFileNameText=-1\nchat_inAudioTitleText=-1447184\nchat_outAudioTitleText=-1\nchat_inAudioPerformerText=-6314328\nchat_outAudioPerformerText=-2950145\nchat_inVoiceSeekbar=-5588737\nchat_outVoiceSeekbar=-6110721\nchat_messagePanelBackground=-15198175\nchat_messagePanelText=-1447184\nchat_messagePanelHint=-6314328\nchat_messagePanelIcons=-6314328\nchat_messagePanelSend=-5588737\nchat_messageLinkIn=-6110721\nchat_messageLinkOut=-6110721\nlistSelectorSDK21=-13221042\nswitchTrack=-11709818\nswitchTrackChecked=-5588737\n";
                try(FileOutputStream out=new FileOutputStream(file)){out.write(text.getBytes(StandardCharsets.UTF_8));}
                Theme.applyThemeFile(file,"Nyxgram Night",null,false);
            }catch(Exception error){FileLog.e(error);}
        }else if(d.preset==0){restoreThemeOnly();}
        NyxConfig.prefs().edit().putInt("appearance_revision",NyxConfig.prefs().getInt("appearance_revision",0)+1).apply();
        Theme.refreshThemeColors();
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
