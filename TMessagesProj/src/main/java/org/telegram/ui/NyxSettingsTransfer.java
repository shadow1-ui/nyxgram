package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import org.json.JSONObject;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.telegram.messenger.LocaleController.getString;

/** A strict allowlist. No arbitrary preference dump, IDs, account objects, PINs or archives. */
public final class NyxSettingsTransfer {
    public static final int EXPORT=9930,IMPORT=9931;
    private static final String[] BOOLS={"hideRead","hideOnline","hideTyping","hidePhone","confirmCalls","confirmStickers","confirmGifs","showId","showDc","storyGhost"};
    private static final String[] STYLE={"stickerTime","gifTime","roundTime","avatars","dates"};
    public static void choose(BaseFragment f,boolean export){
        Intent intent=new Intent(export?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");
        if(export)intent.putExtra(Intent.EXTRA_TITLE,"nyxgram-settings.json");
        f.startActivityForResult(intent,export?EXPORT:IMPORT);
    }
    public static boolean result(BaseFragment f,int request,int result,Intent data){
        if(request!=EXPORT && request!=IMPORT)return false;if(result!=Activity.RESULT_OK||data==null||data.getData()==null)return true;
        Uri uri=data.getData();int account=f.getCurrentAccount();
        Utilities.globalQueue.postRunnable(()->{
            try{
                if(request==EXPORT){
                    JSONObject o=new JSONObject();o.put("format","nyxgram-settings");o.put("schema",1);
                    JSONObject p=new JSONObject();for(String k:BOOLS)p.put(k,NyxConfig.get(k,account,k.equals("showId")||k.equals("showDc")||k.equals("confirmStickers")||k.equals("confirmGifs")));o.put("privacy",p);
                    JSONObject appearance=new JSONObject();for(String k:STYLE)appearance.put(k,NyxAppearance.get(k));appearance.put("preset",NyxConfig.prefs().getInt("appearance_preset",0));appearance.put("font",SharedConfig.fontSize);appearance.put("radius",SharedConfig.bubbleRadius);o.put("appearance",appearance);
                    o.put("effects",LiteMode.getValue(true)&NyxConfig.EFFECT_FLAGS);o.put("animations",SharedConfig.animationsEnabled());o.put("deletedIcon",NyxConfig.number("deletedIcon",account,0));o.put("deletedColor",NyxAppearance.color(account));
                    try(OutputStream out=ApplicationLoader.applicationContext.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("No document stream");out.write(o.toString(2).getBytes(StandardCharsets.UTF_8));}
                    AndroidUtilities.runOnUIThread(()->toast(f,R.string.NyxExportDone));
                }else{
                    byte[] bytes;
                    try(InputStream in=ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                        if(in==null)throw new IOException("No document stream");byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){if(out.size()+n>65536)throw new IOException("Settings file too large");out.write(chunk,0,n);}bytes=out.toByteArray();
                    }
                    JSONObject o=new JSONObject(new String(bytes,StandardCharsets.UTF_8));validate(o);
                    AndroidUtilities.runOnUIThread(()->{
                        if(f.getParentActivity()==null)return;
                        f.showDialog(new AlertDialog.Builder(f.getParentActivity()).setTitle(getString(R.string.NyxImport))
                            .setMessage(getString(R.string.NyxImportAsk)).setPositiveButton(getString(R.string.NyxApply),(d,w)->{
                                try{apply(account,o);toast(f,R.string.NyxImportDone);}catch(Exception error){FileLog.e(error);toast(f,R.string.NyxTransferError);}
                            }).setNegativeButton(getString(R.string.NyxCancel),null).create());
                    });
                }
            }catch(Exception error){FileLog.e(error);AndroidUtilities.runOnUIThread(()->toast(f,R.string.NyxTransferError));}
        });return true;
    }
    private static int integer(JSONObject o,String key,int min,int max)throws Exception{Object value=o.get(key);if(!(value instanceof Integer))throw new IllegalArgumentException("Invalid integer");int n=(Integer)value;if(n<min||n>max)throw new IllegalArgumentException("Out of range");return n;}
    private static void bool(JSONObject o,String key)throws Exception{if(!(o.get(key) instanceof Boolean))throw new IllegalArgumentException("Invalid switch");}
    private static void validate(JSONObject o)throws Exception{
        if(!"nyxgram-settings".equals(o.getString("format"))||integer(o,"schema",1,1)!=1)throw new IllegalArgumentException("Unsupported settings format");
        JSONObject p=o.getJSONObject("privacy"),a=o.getJSONObject("appearance");if(!p.has("confirmGifs"))p.put("confirmGifs",true);for(String k:BOOLS)bool(p,k);for(String k:STYLE)bool(a,k);bool(o,"animations");
        integer(a,"preset",0,3);integer(a,"font",12,24);integer(a,"radius",0,24);int flags=integer(o,"effects",0,Integer.MAX_VALUE);if((flags&~NyxConfig.EFFECT_FLAGS)!=0)throw new IllegalArgumentException("Unknown effects");integer(o,"deletedIcon",0,3);integer(o,"deletedColor",Integer.MIN_VALUE,-1);
    }
    private static void apply(int account,JSONObject o)throws Exception{
        JSONObject p=o.getJSONObject("privacy"),a=o.getJSONObject("appearance");
        NyxAppearance.Draft draft=new NyxAppearance.Draft();draft.preset=a.getInt("preset");draft.radius=a.getInt("radius");draft.font=a.getInt("font");draft.stickers=a.getBoolean("stickerTime");draft.gifs=a.getBoolean("gifTime");draft.round=a.getBoolean("roundTime");draft.avatars=a.getBoolean("avatars");draft.dates=a.getBoolean("dates");NyxAppearance.apply(draft);
        int original=LiteMode.getValue(true);NyxConfig.setDisabled(NyxConfig.EFFECT_FLAGS,true);LiteMode.setAllFlags((original&~NyxConfig.EFFECT_FLAGS)|o.getInt("effects"));SharedConfig.setAnimationsEnabled(o.getBoolean("animations"));MessagesController.getGlobalMainSettings().edit().putBoolean("view_animations",o.getBoolean("animations")).apply();
        for(String k:BOOLS)NyxConfig.set(k,account,p.getBoolean(k));NyxConfig.setNumber("deletedIcon",account,o.getInt("deletedIcon"));NyxConfig.setNumber("deletedColor",account,o.getInt("deletedColor"));MessagesController.getInstance(account).updateNyxOnlineState();
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces,MessagesController.UPDATE_MASK_ALL);
    }
    private static void toast(BaseFragment f,int text){if(f.getParentActivity()!=null)Toast.makeText(f.getParentActivity(),getString(text),Toast.LENGTH_LONG).show();}
}
