package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Base64;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import static org.telegram.messenger.LocaleController.getString;

/** Separate UI gate. PIN hash only; encryption keys stay in AndroidKeyStore, not settings exports. */
public final class NyxArchiveGate {
    private static final DispatchQueue worker=new DispatchQueue("nyx-pin");
    private static volatile long unlockedUntil;
    private static int generation;
    private static final java.util.ArrayList<java.lang.ref.WeakReference<android.app.Dialog>> dialogs=new java.util.ArrayList<>();
    public static void track(android.app.Dialog dialog){dialogs.add(new java.lang.ref.WeakReference<>(dialog));}
    private static SharedPreferences p(){return ApplicationLoader.applicationContext.getSharedPreferences("nyx_security",Context.MODE_PRIVATE);}
    public static boolean hasPin(){return p().contains("hash");}
    public static boolean canShow(int account){return !NyxConfig.get("protectArchive",account,false) || (hasPin() && SystemClock.elapsedRealtime()<unlockedUntil);}
    public static void lock(){unlockedUntil=0;generation++;
        if(org.telegram.ui.PhotoViewer.hasInstance()){org.telegram.messenger.MessageObject photo=org.telegram.ui.PhotoViewer.getInstance().getCurrentMessageObject();if(photo!=null&&photo.nyxDeleted)org.telegram.ui.PhotoViewer.getInstance().closePhoto(false,false);}
for(java.lang.ref.WeakReference<android.app.Dialog> ref:dialogs){android.app.Dialog dialog=ref.get();if(dialog!=null&&dialog.isShowing())dialog.dismiss();}dialogs.clear();for(int a=0;a<UserConfig.MAX_ACCOUNT_COUNT;a++)NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces,MessagesController.UPDATE_MASK_ALL);}
    private static void unlocked(){
        unlockedUntil=SystemClock.elapsedRealtime()+5*60*1000L;int token=++generation;
        AndroidUtilities.runOnUIThread(()->{if(token==generation)lock();},5*60*1000L);
    }
    public static void require(int account,BaseFragment fragment,Runnable done){
        if(canShow(account)){done.run();return;} ask(account,fragment,!hasPin(),done);
    }
    public static void authorize(int account,BaseFragment fragment,Runnable done){ask(account,fragment,!hasPin(),done);}
    private static byte[] derive(char[] pin,byte[] salt)throws Exception{
        PBEKeySpec spec=new PBEKeySpec(pin,salt,210000,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}
    }
    public static void ask(int account,BaseFragment fragment,boolean setup,Runnable done){
        if(fragment.getParentActivity()==null)return;
        if(!setup && System.currentTimeMillis()<p().getLong("blockedUntil",0)){Toast.makeText(fragment.getParentActivity(),getString(R.string.NyxPinWait),Toast.LENGTH_LONG).show();return;}
        int authGeneration=generation;
        LinearLayout box=new LinearLayout(fragment.getParentActivity());box.setOrientation(LinearLayout.VERTICAL);
        EditText input=new EditText(fragment.getParentActivity());input.setSingleLine();input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);input.setHint(getString(setup?R.string.NyxPinSet:R.string.NyxPinEnter));
        box.addView(input,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,48,Gravity.CENTER,20,8,20,0));
        EditText repeat=new EditText(fragment.getParentActivity());repeat.setSingleLine();repeat.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);repeat.setHint(getString(R.string.NyxPinRepeat));
        if(setup)box.addView(repeat,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,48,Gravity.CENTER,20,0,20,0));
        AlertDialog.Builder builder=new AlertDialog.Builder(fragment.getParentActivity()).setTitle(getString(setup?R.string.NyxPinSet:R.string.NyxPinEnter))
            .setMessage(getString(R.string.NyxPinHint)).setView(box).setNegativeButton(getString(R.string.NyxCancel),null)
            .setPositiveButton(getString(R.string.NyxDone),(dialog,which)->{
                char[] pin=input.getText().toString().toCharArray();String confirm=repeat.getText().toString();input.setText("");repeat.setText("");
                if(!new String(pin).matches("[0-9]{4,12}") || (setup&&!new String(pin).equals(confirm))){java.util.Arrays.fill(pin,'\0');Toast.makeText(fragment.getParentActivity(),getString(R.string.NyxPinMismatch),Toast.LENGTH_LONG).show();return;}
                worker.postRunnable(()->{
                    boolean ok=false;
                    try{
                        if(setup){byte[] salt=new byte[24];new SecureRandom().nextBytes(salt);byte[] hash=derive(pin,salt);p().edit().putString("salt",Base64.encodeToString(salt,Base64.NO_WRAP)).putString("hash",Base64.encodeToString(hash,Base64.NO_WRAP)).remove("failures").remove("blockedUntil").commit();ok=true;}
                        else{byte[] salt=Base64.decode(p().getString("salt",""),Base64.NO_WRAP);byte[] expected=Base64.decode(p().getString("hash",""),Base64.NO_WRAP);ok=MessageDigest.isEqual(expected,derive(pin,salt));}
                    }catch(Exception error){FileLog.e(error);}finally{java.util.Arrays.fill(pin,'\0');}
                    final boolean success=ok;
                    if(!ok){int attempts=p().getInt("failures",0)+1;p().edit().putInt("failures",attempts).putLong("blockedUntil",attempts>=5?System.currentTimeMillis()+60000L*Math.min(5,attempts-4):0).commit();}
                    else p().edit().remove("failures").remove("blockedUntil").commit();
                    AndroidUtilities.runOnUIThread(()->{
                        if(fragment.getParentActivity()==null || ApplicationLoader.mainInterfacePaused || authGeneration!=generation)return;
                        if(success){unlocked();done.run();}else Toast.makeText(fragment.getParentActivity(),getString(R.string.NyxPinWrong),Toast.LENGTH_LONG).show();
                    });
                });
            });
        if(!setup)builder.setNeutralButton(getString(R.string.NyxPinForgot),(dialog,which)->recover(fragment));
        AlertDialog pinDialog=builder.create();track(pinDialog);fragment.showDialog(pinDialog);
    }
    private static void recover(BaseFragment fragment){
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity()).setTitle(getString(R.string.NyxArchiveClear))
            .setMessage(getString(R.string.NyxPinForgotAsk)).setPositiveButton(getString(R.string.NyxResetAction),(d,w)->
                NyxArchive.clear(fragment.getCurrentAccount(),true,()->{p().edit().clear().commit();lock();}))
            .setNegativeButton(getString(R.string.NyxCancel),null).create());
    }
}
