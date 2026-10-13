package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Local visual capabilities, deliberately separate from UserConfig.isPremium/server entitlements. */
public final class NyxPremium {
    private static final HashMap<Integer,NotificationCenter.NotificationCenterDelegate> observers=new HashMap<>();
    public static boolean enabled(int account){return UserConfig.getInstance(account).getClientUserId()!=0 && NyxConfig.get("localPremium",account,false);}
    public static boolean available(int account){return UserConfig.getInstance(account).isPremium()||enabled(account);}
    public static boolean localOnly(int account){return enabled(account)&&!UserConfig.getInstance(account).isPremium();}
    public static boolean displayUser(TLRPC.User user,int account){return user!=null&&(user.premium||(user.id==UserConfig.getInstance(account).getClientUserId()&&enabled(account)));}
    private static String statusKey(int account){return "localStatus_"+UserConfig.getInstance(account).getClientUserId();}
    public static TLRPC.EmojiStatus status(int account,TLRPC.User user){
        if(user==null)return null;
        String k=statusKey(account);
        if(!localOnly(account)||user.id!=UserConfig.getInstance(account).getClientUserId()||!NyxConfig.prefs().contains(k))return user.emoji_status;
        long id=NyxConfig.prefs().getLong(k,0);int until=NyxConfig.prefs().getInt(k+"_until",0);
        if(id==0||until>0&&until<=System.currentTimeMillis()/1000)return new TLRPC.TL_emojiStatusEmpty();
        TLRPC.TL_emojiStatus s=new TLRPC.TL_emojiStatus();s.document_id=id;if(until>0){s.flags|=1;s.until=until;}return s;
    }
    public static boolean setStatus(int account,TLRPC.EmojiStatus status){
        if(!localOnly(account))return false;
        if(!(status instanceof TLRPC.TL_emojiStatus)&&!(status instanceof TLRPC.TL_emojiStatusEmpty)){
            android.widget.Toast.makeText(ApplicationLoader.applicationContext,LocaleController.getString(R.string.NyxLocalCollectibleUnavailable),android.widget.Toast.LENGTH_LONG).show();return true;
        }
        String key=statusKey(account);long id=status instanceof TLRPC.TL_emojiStatus?((TLRPC.TL_emojiStatus)status).document_id:0;
        int until=status instanceof TLRPC.TL_emojiStatus&&(((TLRPC.TL_emojiStatus)status).flags&1)!=0?((TLRPC.TL_emojiStatus)status).until:0;
        NyxConfig.prefs().edit().putLong(key,id).putInt(key+"_until",until).apply();refresh(account);return true;
    }
    public static void setEnabled(int account,boolean value){NyxConfig.set("localPremium",account,value);observe(account);refresh(account);}
    private static void refresh(int account){
        TLRPC.User self=UserConfig.getInstance(account).getCurrentUser();
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.userEmojiStatusUpdated,self);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces,MessagesController.UPDATE_MASK_ALL);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.premiumStatusChangedGlobal);
        AnimatedEmojiDrawable.updateAll();
    }
    /** Ordinary text and free emoji entities go to Telegram; paid custom emoji render locally only. */
    public static ArrayList<TLRPC.MessageEntity> serverEntities(int account,long peer,ArrayList<TLRPC.MessageEntity> input){
        if(!localOnly(account)||peer==UserConfig.getInstance(account).getClientUserId()||input==null)return input;
        ArrayList<TLRPC.MessageEntity> out=new ArrayList<>();
        for(TLRPC.MessageEntity e:input){
            if(e instanceof TLRPC.TL_messageEntityCustomEmoji){TLRPC.Document d=AnimatedEmojiDrawable.findDocument(account,((TLRPC.TL_messageEntityCustomEmoji)e).document_id);if(d==null||!MessageObject.isFreeEmoji(d))continue;}
            out.add(e);
        }
        return out;
    }
    // Bounded, owner-scoped metadata only. No message text and no subscription fields are persisted.
    private static SharedPreferences emojiPrefs(){return ApplicationLoader.applicationContext.getSharedPreferences("nyx_local_emoji",Context.MODE_PRIVATE);}
    private static String emojiKey(int account,long did,int mid){return UserConfig.getInstance(account).getClientUserId()+":"+did+":"+mid;}
    private static String fingerprint(String text)throws Exception{return android.util.Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest((text==null?"":text).getBytes(StandardCharsets.UTF_8)),android.util.Base64.NO_WRAP);}
    public static void remember(int account,TLRPC.Message message,ArrayList<TLRPC.MessageEntity> entities){
        if(!localOnly(account)||message==null||entities==null||!NyxArchive.eligibleOutgoingEmoji(message))return;
        JSONArray data=new JSONArray();
        for(TLRPC.MessageEntity e:entities)if(e instanceof TLRPC.TL_messageEntityCustomEmoji&&e.offset>=0&&e.length>0&&(long)e.offset+e.length<=(message.message==null?0:message.message.length())&&data.length()<64){JSONArray a=new JSONArray();a.put(e.offset);a.put(e.length);a.put(((TLRPC.TL_messageEntityCustomEmoji)e).document_id);data.put(a);}
        if(data.length()==0)return;
        try{JSONObject o=new JSONObject();o.put("emoji",data);o.put("hash",fingerprint(message.message));o.put("changed",System.currentTimeMillis());SharedPreferences p=emojiPrefs();SharedPreferences.Editor edit=p.edit();if(p.getAll().size()>=1000){ArrayList<Map.Entry<String,?>> entries=new ArrayList<>(p.getAll().entrySet());entries.sort((a,b)->Long.compare(touched(a.getValue()),touched(b.getValue())));for(int i=0;i<entries.size()-950;i++)edit.remove(entries.get(i).getKey());}edit.putString(emojiKey(account,MessageObject.getDialogId(message),message.id),o.toString()).apply();observe(account);}catch(Exception e){FileLog.e(e);}
    }
    public static void rememberEdit(int account,TLRPC.Message source,String text,ArrayList<TLRPC.MessageEntity> entities){
        if(!localOnly(account)||source==null||!eligibleForLocalEmoji(source))return;
        emojiPrefs().edit().remove(emojiKey(account,MessageObject.getDialogId(source),source.id)).apply();
        TLRPC.TL_message copy=new TLRPC.TL_message();copy.id=source.id;copy.dialog_id=MessageObject.getDialogId(source);copy.out=source.out;copy.message=text;remember(account,copy,entities);
    }
    private static boolean eligibleForLocalEmoji(TLRPC.Message m){return NyxArchive.eligibleOutgoingEmoji(m);}
    private static long touched(Object value){try{return new JSONObject(String.valueOf(value)).optLong("changed",0);}catch(Exception e){return 0;}}
    public static ArrayList<TLRPC.MessageEntity> renderEntities(int account,TLRPC.Message message,ArrayList<TLRPC.MessageEntity> original){
        if(!localOnly(account)||message==null||!message.out||!NyxArchive.eligibleOutgoingEmoji(message))return original;
        String raw=emojiPrefs().getString(emojiKey(account,MessageObject.getDialogId(message),message.id),null);if(raw==null)return original;
        try{JSONObject o=new JSONObject(raw);if(!fingerprint(message.message).equals(o.optString("hash")))return original;JSONArray data=o.getJSONArray("emoji");ArrayList<TLRPC.MessageEntity> out=original==null?new ArrayList<>():new ArrayList<>(original);out.removeIf(e->e instanceof TLRPC.TL_messageEntityCustomEmoji);for(int i=0;i<data.length();i++){JSONArray a=data.getJSONArray(i);int offset=a.getInt(0),length=a.getInt(1);if(offset<0||length<=0||(long)offset+length>message.message.length())continue;TLRPC.TL_messageEntityCustomEmoji e=new TLRPC.TL_messageEntityCustomEmoji();e.offset=offset;e.length=length;e.document_id=a.getLong(2);out.add(e);}return out;}catch(Exception e){FileLog.e(e);return original;}
    }
    public static void observe(int account){
        if(observers.containsKey(account))return;
        NotificationCenter.NotificationCenterDelegate observer=(id,a,args)->{if(id==NotificationCenter.messageReceivedByServer&&args.length>3&&args[0] instanceof Integer&&args[1] instanceof Integer&&args[3] instanceof Long){SharedPreferences p=emojiPrefs();String old=emojiKey(account,(Long)args[3],(Integer)args[0]),next=emojiKey(account,(Long)args[3],(Integer)args[1]);String body=p.getString(old,null);if(body!=null)p.edit().remove(old).putString(next,body).apply();}};
        observers.put(account,observer);NotificationCenter.getInstance(account).addObserver(observer,NotificationCenter.messageReceivedByServer);
    }
}
