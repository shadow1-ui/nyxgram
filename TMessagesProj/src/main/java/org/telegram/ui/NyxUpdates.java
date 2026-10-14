package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.telegram.messenger.LocaleController.getString;

/** Public stable Releases only. No GitHub token is shipped. Checksum + package + signer verification before install. */
public final class NyxUpdates {
    public static final String VERSION="0.4.3";
    private static final AtomicBoolean checking=new AtomicBoolean(),downloading=new AtomicBoolean();
    private static final DispatchQueue worker=new DispatchQueue("nyx-updates");
    private static Context context(){return ApplicationLoader.applicationContext;}
    private static boolean allowed(URL u){String h=u.getHost();return "https".equals(u.getProtocol())&&(h.equals("github.com")||h.endsWith(".github.com")||h.endsWith(".githubusercontent.com"));}
    private static HttpURLConnection connect(String address)throws Exception{
        URL url=new URL(address);for(int i=0;i<6;i++){
            if(!allowed(url))throw new IOException("Untrusted update host");HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(20000);c.setReadTimeout(45000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","Nyxgram/"+VERSION);c.setRequestProperty("Accept","application/vnd.github+json");
            int status=c.getResponseCode();if(status>=300&&status<400){String next=c.getHeaderField("Location");c.disconnect();if(next==null)throw new IOException("No redirect location");url=new URL(url,next);continue;}if(status!=200){c.disconnect();throw new IOException("Update HTTP "+status);}return c;
        }throw new IOException("Too many redirects");
    }
    private static String text(String address,int max)throws Exception{
        HttpURLConnection c=connect(address);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>max)throw new IOException("Update metadata too large");out.write(buf,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }finally{c.disconnect();}
    }
    public static void check(BaseFragment fragment,boolean manual){
        if(!manual && (!NyxConfig.prefs().getBoolean("autoUpdates",true)||System.currentTimeMillis()-NyxConfig.prefs().getLong("lastUpdateCheck",0)<12*60*60*1000L))return;
        if(!checking.compareAndSet(false,true))return;NyxConfig.prefs().edit().putLong("lastUpdateCheck",System.currentTimeMillis()).apply();
        worker.postRunnable(()->{
            try{
                JSONObject release;
                try{release=new JSONObject(text("https://api.github.com/repos/shadow1-ui/nyxgram/releases/latest",512000));}catch(IOException e){if(e.getMessage()!=null&&e.getMessage().endsWith("404")){AndroidUtilities.runOnUIThread(()->{if(manual)toast(fragment,R.string.NyxUpdateNone);});return;}throw e;}
                if(release.optBoolean("draft")||release.optBoolean("prerelease"))throw new IOException("Unpublished release");
                JSONArray assets=release.getJSONArray("assets");String manifest=null,apk=null;for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);if(a.getString("name").equals("nyx-update.json"))manifest=a.getString("browser_download_url");if(a.getString("name").endsWith(".apk"))apk=a.getString("browser_download_url");}
                if(manifest==null||apk==null)throw new IOException("Incomplete release");JSONObject update=new JSONObject(text(manifest,65536));
                if(!context().getPackageName().equals(update.getString("package")))throw new IOException("Wrong update package");
                int code=update.getInt("versionCode"),current=code(context().getPackageManager().getPackageInfo(context().getPackageName(),0));
                if(code<=current){AndroidUtilities.runOnUIThread(()->{if(manual)toast(fragment,R.string.NyxUpdateNone);});return;}
                String sha=update.getString("sha256");if(!sha.matches("[0-9a-fA-F]{64}"))throw new IOException("Bad update checksum");long size=update.getLong("size");if(size<=0||size>400L*1024*1024)throw new IOException("Invalid APK size");
                String url=apk,body=release.optString("body","");if(body.length()>4000)body=body.substring(0,4000);String notes=body;String tag=release.optString("name",release.getString("tag_name"));
                AndroidUtilities.runOnUIThread(()->{
                    if(fragment.getParentActivity()==null || LaunchActivity.getLastFragmentIncludeMainTabs()!=fragment)return;
                    fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity()).setTitle(getString(R.string.NyxUpdateFound)).setMessage(tag+"\n\n"+notes+"\n\n"+(size/1024/1024)+" МБ")
                        .setPositiveButton(getString(R.string.NyxUpdateDownload),(d,w)->download(fragment,url,sha,size,code))
                        .setNegativeButton(getString(R.string.NyxUpdateLater),null).create());
                });
            }catch(Exception error){FileLog.e(error);AndroidUtilities.runOnUIThread(()->{if(manual)toast(fragment,R.string.NyxUpdateError);});}finally{checking.set(false);}
        });
    }
    private static int code(PackageInfo p){return Build.VERSION.SDK_INT>=28?(int)p.getLongVersionCode():p.versionCode;}
    private static byte[][] signers(PackageInfo p){
        android.content.pm.Signature[] signatures=Build.VERSION.SDK_INT>=28&&p.signingInfo!=null?p.signingInfo.getApkContentsSigners():p.signatures;
        if(signatures==null)return new byte[0][];byte[][] result=new byte[signatures.length][];for(int i=0;i<result.length;i++)result[i]=signatures[i].toByteArray();return result;
    }
    private static boolean signer(PackageInfo a,PackageInfo b){byte[][] x=signers(a),y=signers(b);if(x.length==0||x.length!=y.length)return false;for(byte[] one:x){boolean found=false;for(byte[] two:y)if(MessageDigest.isEqual(one,two)){found=true;break;}if(!found)return false;}return true;}
    private static void download(BaseFragment f,String url,String sha,long size,int expectedCode){
        if(!downloading.compareAndSet(false,true))return;
        AlertDialog progress=new AlertDialog(f.getParentActivity(),AlertDialog.ALERT_TYPE_LOADING);progress.setTitle(getString(R.string.NyxUpdateDownloading));progress.setCanCancel(false);progress.setCancelable(false);progress.setCanceledOnTouchOutside(false);f.showDialog(progress);
        worker.postRunnable(()->{
            File directory=new File(context().getFilesDir(),"nyx_updates");directory.mkdirs();File file=new File(directory,"nyxgram.download.apk"),ready=new File(directory,"nyxgram.apk");
            try{
                MessageDigest digest=MessageDigest.getInstance("SHA-256");HttpURLConnection c=connect(url);long total=0;
                try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(file)){
                    byte[] buf=new byte[65536];int n;long last=0;while((n=in.read(buf))!=-1){total+=n;if(total>size)throw new IOException("APK size mismatch");out.write(buf,0,n);digest.update(buf,0,n);if(total-last>1024*1024){last=total;int percent=(int)(total*100/size);AndroidUtilities.runOnUIThread(()->progress.setMessage(getString(R.string.NyxUpdateDownloading)+" "+percent+"%"));}}
                }finally{c.disconnect();}
                StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));if(total!=size||!hash.toString().equalsIgnoreCase(sha))throw new IOException("APK checksum mismatch");
                int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
                PackageManager pm=context().getPackageManager();PackageInfo candidate=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags),installed=pm.getPackageInfo(context().getPackageName(),flags);
                if(candidate==null||!installed.packageName.equals(candidate.packageName)||code(candidate)!=expectedCode||code(candidate)<=code(installed)||!signer(candidate,installed))throw new IOException("APK identity or signature mismatch");
                if(ready.exists()&&!ready.delete())throw new IOException("Cannot replace downloaded APK");if(!file.renameTo(ready))throw new IOException("Cannot finalize APK");
                AndroidUtilities.runOnUIThread(()->{progress.dismiss();if(f.getParentActivity()!=null)f.showDialog(new AlertDialog.Builder(f.getParentActivity()).setTitle(getString(R.string.NyxUpdateInstall)).setMessage(getString(R.string.NyxUpdateReady)).setPositiveButton(getString(R.string.NyxUpdateInstall),(d,w)->install(f,ready)).setNegativeButton(getString(R.string.NyxUpdateLater),null).create());});
            }catch(Exception error){file.delete();FileLog.e(error);AndroidUtilities.runOnUIThread(()->{progress.dismiss();toast(f,R.string.NyxUpdateInvalid);});}finally{downloading.set(false);}
        });
    }
    public static void installDownloaded(BaseFragment f){File apk=new File(context().getFilesDir(),"nyx_updates/nyxgram.apk");if(apk.isFile())install(f,apk);else check(f,true);}
    private static void install(BaseFragment f,File apk){
        if(f.getParentActivity()==null)return;
        try{
            if(Build.VERSION.SDK_INT>=26&&!context().getPackageManager().canRequestPackageInstalls()){toast(f,R.string.NyxUpdatePermission);f.getParentActivity().startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+context().getPackageName())));return;}
            Uri uri=FileProvider.getUriForFile(context(),context().getPackageName()+".provider",apk);Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);f.getParentActivity().startActivity(intent);
        }catch(Exception error){FileLog.e(error);toast(f,R.string.NyxUpdateInvalid);}
    }
    private static void toast(BaseFragment f,int text){if(f.getParentActivity()!=null)Toast.makeText(f.getParentActivity(),getString(text),Toast.LENGTH_LONG).show();}
}
