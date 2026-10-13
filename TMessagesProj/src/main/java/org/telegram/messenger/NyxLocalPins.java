package org.telegram.messenger;
import org.json.JSONArray;
import org.telegram.tgnet.TLRPC;
import java.util.*;
public final class NyxLocalPins {
    public static final int LIMIT=500;
    private static final java.util.HashMap<Long,ArrayList<Long>> cache=new java.util.HashMap<>();
    private static final java.util.HashMap<Long,java.util.HashMap<Long,Integer>> rankCache=new java.util.HashMap<>();
    private static java.util.HashMap<Long,Integer> ranks(int account){long uid=owner(account);java.util.HashMap<Long,Integer> result=rankCache.get(uid);if(result==null){result=new java.util.HashMap<>();ArrayList<Long> pins=read(account);for(int i=0;i<pins.size();i++)result.put(pins.get(i),i);rankCache.put(uid,result);}return result;}
    private static long owner(int account){return UserConfig.getInstance(account).getClientUserId();}
    private static ArrayList<Long> read(int account){long uid=owner(account);ArrayList<Long> ids=cache.get(uid);if(ids==null){ids=new ArrayList<>();try{JSONArray a=new JSONArray(NyxConfig.prefs().getString("localPins_"+uid,"[]"));for(int i=0;i<a.length()&&ids.size()<LIMIT;i++){long id=a.getLong(i);if(id!=0&&!DialogObject.isFolderDialogId(id)&&!ids.contains(id))ids.add(id);}}catch(Exception e){FileLog.e(e);}cache.put(uid,ids);}return ids;}
    public static ArrayList<Long> list(int account){return new ArrayList<>(read(account));}
    public static boolean contains(int account,long id){return ranks(account).containsKey(id);}
    public static int compare(int account,long a,long b){java.util.HashMap<Long,Integer> map=ranks(account);int x=map.getOrDefault(a,-1),y=map.getOrDefault(b,-1);return x<0?(y<0?0:1):y<0?-1:Integer.compare(x,y);}
    public static boolean toggle(int account,long id){if(owner(account)==0||id==0||DialogObject.isFolderDialogId(id))return false;ArrayList<Long> ids=read(account);if(!ids.remove(id)){if(ids.size()>=LIMIT)return false;ids.add(0,id);}rankCache.remove(owner(account));JSONArray a=new JSONArray();for(long n:ids)a.put(n);NyxConfig.prefs().edit().putString("localPins_"+owner(account),a.toString()).apply();MessagesController.getInstance(account).sortDialogs(null);NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);return true;}
    public static String title(int account,long did){MessagesController c=MessagesController.getInstance(account);if(did>0){TLRPC.User u=c.getUser(did);if(u!=null)return UserObject.getUserName(u);}else{TLRPC.Chat chat=c.getChat(-did);if(chat!=null)return chat.title;}return "ID "+did;}
}
