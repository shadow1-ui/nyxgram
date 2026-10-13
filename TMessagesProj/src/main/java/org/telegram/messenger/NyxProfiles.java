package org.telegram.messenger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.ActionBar.BaseFragment;
import android.os.Bundle;
import android.widget.Toast;
import java.util.ArrayList;

/** Local bookmarks keyed by the signed-in user ID, not reusable account slots. */
public final class NyxProfiles {
    public static class Item{public long id;public String name;}
    private static String key(int account){return "profiles_"+UserConfig.getInstance(account).getClientUserId();}
    public static ArrayList<Item> list(int account){
        ArrayList<Item> result=new ArrayList<>();try{JSONArray array=new JSONArray(NyxConfig.prefs().getString(key(account),"[]"));for(int i=0;i<array.length();i++){JSONObject o=array.getJSONObject(i);Item p=new Item();p.id=o.getLong("id");p.name=o.optString("name","ID "+p.id);result.add(p);}}catch(Exception e){FileLog.e(e);}return result;
    }
    private static void write(int account,ArrayList<Item> items){try{JSONArray a=new JSONArray();for(Item i:items){JSONObject o=new JSONObject();o.put("id",i.id);o.put("name",i.name);a.put(o);}NyxConfig.prefs().edit().putString(key(account),a.toString()).apply();}catch(Exception e){FileLog.e(e);}}
    public static void save(int account,long id,String name){ArrayList<Item>a=list(account);for(Item i:a)if(i.id==id){i.name=name;write(account,a);return;}Item i=new Item();i.id=id;i.name=name==null?"ID "+id:name;if(i.name.length()>120)i.name=i.name.substring(0,120);a.add(0,i);while(a.size()>200)a.remove(a.size()-1);write(account,a);}
    public static void remove(int account,long id){ArrayList<Item>a=list(account);a.removeIf(i->i.id==id);write(account,a);}
    public static void open(BaseFragment fragment,long id){
        int account=fragment.getCurrentAccount();long owner=UserConfig.getInstance(account).getClientUserId();
        Utilities.globalQueue.postRunnable(()->{
            MessagesController controller=MessagesController.getInstance(account);TLRPC.User user=null;TLRPC.Chat chat=null;
            if(id>0){user=controller.getUser(id);if(user==null)user=MessagesStorage.getInstance(account).getUserSync(id);}
            else{chat=controller.getChat(-id);if(chat==null)chat=MessagesStorage.getInstance(account).getChatSync(-id);}
            TLRPC.User u=user;TLRPC.Chat c=chat;
            AndroidUtilities.runOnUIThread(()->{
                if(fragment.getParentActivity()==null||owner!=UserConfig.getInstance(account).getClientUserId())return;
                if(u==null&&c==null){Toast.makeText(fragment.getParentActivity(),LocaleController.getString(R.string.NyxProfileUnavailable),Toast.LENGTH_LONG).show();return;}
                if(u!=null)controller.putUser(u,true);if(c!=null)controller.putChat(c,true);Bundle args=new Bundle();args.putLong(id>0?"user_id":"chat_id",Math.abs(id));fragment.presentFragment(new ProfileActivity(args));
            });
        });
    }
}
