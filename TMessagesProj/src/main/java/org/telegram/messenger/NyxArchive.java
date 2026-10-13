package org.telegram.messenger;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.NyxArchiveGate;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Separate, bounded, encrypted local snapshots. Never bypasses Telegram deletion or TTL storage. */
public final class NyxArchive {
    private static final DispatchQueue queue = new DispatchQueue("nyx-archive");
    private static SQLiteDatabase db;
    private static SecretKey key, fingerprintKey;
    private static long lastPrune;
    public static final int DELETED = 1, EDITED = 2;
    public static class Entry {
        public long id, dialog, changed; public int mid, kind; public TLRPC.Message message;
    }
    private static long owner(int account) { return UserConfig.getInstance(account).getClientUserId(); }
    public static boolean enabled(int account) {
        return Build.VERSION.SDK_INT >= 23 && owner(account) != 0 &&
            (NyxConfig.get("keepDeleted", account, false) || NyxConfig.get("keepEdits", account, false));
    }
    public static boolean eligible(TLRPC.Message m) {
        if (m == null || m.id <= 0 || DialogObject.isEncryptedDialog(MessageObject.getDialogId(m)) || m.ttl != 0 || m.ttl_period != 0) return false;
        if (m.media != null && m.media.ttl_seconds != 0) return false;
        return m.action == null || m.action instanceof TLRPC.TL_messageActionEmpty;
    }
    public static boolean eligibleOutgoingEmoji(TLRPC.Message m){return m!=null&&!DialogObject.isEncryptedDialog(MessageObject.getDialogId(m))&&m.ttl==0&&m.ttl_period==0&&(m.media==null||m.media.ttl_seconds==0);}
    private static void open() throws Exception {
        if (db == null) {
            db = ApplicationLoader.applicationContext.openOrCreateDatabase("nyx_archive.db", 0, null);
            db.execSQL("PRAGMA secure_delete=ON");
            db.execSQL("CREATE TABLE IF NOT EXISTS snapshots(owner INTEGER,did INTEGER,mid INTEGER,body BLOB,hash TEXT,changed INTEGER,PRIMARY KEY(owner,did,mid))");
            db.execSQL("CREATE TABLE IF NOT EXISTS history(id INTEGER PRIMARY KEY AUTOINCREMENT,owner INTEGER,did INTEGER,mid INTEGER,kind INTEGER,body BLOB,changed INTEGER,hash TEXT,UNIQUE(owner,did,mid,kind,changed,hash))");
            db.execSQL("CREATE INDEX IF NOT EXISTS nyx_history_owner_did ON history(owner,did,kind,id)");
        }
        if (key == null) {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
            String alias = "nyx-local-history-v1";
            if (!store.containsAlias(alias)) {
                KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build()); generator.generateKey();
            }
            key = (SecretKey) store.getKey(alias, null);
            String macAlias="nyx-local-history-fingerprints-v1";
            if(!store.containsAlias(macAlias)){
                KeyGenerator macGenerator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256,"AndroidKeyStore");
                macGenerator.init(new KeyGenParameterSpec.Builder(macAlias,KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY).setDigests(KeyProperties.DIGEST_SHA256).setKeySize(256).build());macGenerator.generateKey();
            }
            fingerprintKey=(SecretKey)store.getKey(macAlias,null);
        }
    }
    private static byte[] crypt(byte[] body, long owner, long did, int mid, int kind, boolean encrypt) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] input = body;
        if (encrypt) cipher.init(Cipher.ENCRYPT_MODE, key);
        else {
            if (body.length < 29) throw new IllegalArgumentException("Invalid archive payload");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOfRange(body, 0, 12)));
            input = Arrays.copyOfRange(body, 12, body.length);
        }
        cipher.updateAAD((owner+":"+did+":"+mid+":"+kind).getBytes(StandardCharsets.UTF_8));
        byte[] result = cipher.doFinal(input);
        if (!encrypt) return result;
        byte[] output = new byte[12 + result.length]; System.arraycopy(cipher.getIV(),0,output,0,12);System.arraycopy(result,0,output,12,result.length); return output;
    }
    private static byte[] encode(TLRPC.Message message) {
        SerializedData s = new SerializedData(); message.serializeToStream(s); byte[] bytes = s.toByteArray(); s.cleanup(); return bytes;
    }
    private static TLRPC.Message decode(byte[] bytes, long uid) {
        SerializedData s = new SerializedData(bytes); TLRPC.Message m = TLRPC.Message.TLdeserialize(s, s.readInt32(false), false); if(m!=null)m.readAttachPath(s,uid); s.cleanup(); return m;
    }
    private static String hash(TLRPC.Message m) throws Exception {
        String value = m.message == null ? "" : m.message;
        if (m.media != null) {
            if (m.media.photo != null) value += "|photo:" + m.media.photo.id;
            if (m.media.document != null) value += "|document:" + m.media.document.id;
        }
        javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(fingerprintKey);return Base64.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    }
    private static void history(long uid, long did, int mid, int kind, byte[] bytes, String hash, long changed) throws Exception {
        if(kind==DELETED)try(Cursor check=db.rawQuery("SELECT id FROM history WHERE owner=? AND did=? AND mid=? AND kind=1 LIMIT 1",new String[]{""+uid,""+did,""+mid})){if(check.moveToFirst())return;}
        ContentValues c = new ContentValues(); c.put("owner",uid);c.put("did",did);c.put("mid",mid);c.put("kind",kind);
        c.put("body",crypt(bytes,uid,did,mid,kind,true));c.put("hash",hash);c.put("changed",changed);
        db.insertWithOnConflict("history",null,c,SQLiteDatabase.CONFLICT_IGNORE);
    }
    /** Called only on Telegram's storage queue, before overwriting messages_v2. */
    public static void capture(int account, List<TLRPC.Message> messages, org.telegram.SQLite.SQLiteDatabase telegram) {
        if (!enabled(account) || messages == null) return;
        long uid = owner(account);
        for (TLRPC.Message m : messages) {
            if (!eligible(m)) continue;
            long did = MessageObject.getDialogId(m); byte[] old = null;
            if (m.edit_date > 0 && NyxConfig.get("keepEdits",account,false)) {
                org.telegram.SQLite.SQLiteCursor cursor = null;
                try {
                    cursor = telegram.queryFinalized("SELECT data FROM messages_v2 WHERE uid = ? AND mid = ?", did, m.id);
                    if (cursor.next()) old = cursor.byteArrayValue(0);
                } catch (Exception error) { FileLog.e(error); } finally { if (cursor != null) cursor.dispose(); }
            }
            byte[] before = old, fresh = encode(m); int mid = m.id, edit = m.edit_date;
            queue.postRunnable(() -> {
                if (owner(account) != uid || !enabled(account)) return;
                try {
                    open(); TLRPC.Message next = decode(fresh,uid); String fingerprint = hash(next); byte[] previous = before;
                    String oldHash = null;
                    try (Cursor c = db.rawQuery("SELECT body,hash FROM snapshots WHERE owner=? AND did=? AND mid=?",new String[]{""+uid,""+did,""+mid})) {
                        if (c.moveToFirst()) { previous = crypt(c.getBlob(0),uid,did,mid,0,false); oldHash = c.getString(1); }
                    }
                    if (previous != null && edit > 0 && NyxConfig.get("keepEdits",account,false)) {
                        TLRPC.Message prior = decode(previous,uid);
                        if (eligible(prior)) {
                            if (oldHash == null) oldHash = hash(prior);
                            if (!fingerprint.equals(oldHash)) history(uid,did,mid,EDITED,previous,oldHash,(long)edit*1000);
                        }
                    }
                    ContentValues c = new ContentValues();c.put("owner",uid);c.put("did",did);c.put("mid",mid);
                    c.put("body",crypt(fresh,uid,did,mid,0,true));c.put("hash",fingerprint);c.put("changed",System.currentTimeMillis());
                    db.insertWithOnConflict("snapshots",null,c,SQLiteDatabase.CONFLICT_REPLACE); prune(account,uid,false);
                } catch (Exception error) { FileLog.e(error); }
            });
        }
    }
    /** Called on storage queue before actual native deletion; channel 0 means globally unique non-channel message IDs. */
    public static void captureDeletion(int account, long did, List<Integer> ids, int through, org.telegram.SQLite.SQLiteDatabase telegram) {
        if (!enabled(account)) return;
        org.telegram.SQLite.SQLiteCursor cursor = null;
        try {
            String where;
            if (through > 0) where = "uid="+did+" AND mid <= "+through;
            else {
                if (ids == null || ids.isEmpty()) return;
                StringBuilder list = new StringBuilder();for(int id:ids){if(list.length()>0)list.append(',');list.append(id);}
                where = "mid IN("+list+")" + (did != 0 ? " AND uid="+did : " AND is_channel=0");
            }
            cursor = telegram.queryFinalized("SELECT uid,data FROM messages_v2 WHERE "+where);
            while (cursor.next()) {
                long dialog = cursor.longValue(0); byte[] bytes = cursor.byteArrayValue(1);
                TLRPC.Message m = decode(bytes,owner(account)); if (!eligible(m)) continue;
                m.dialog_id = dialog; deleted(account,m);
            }
        } catch (Exception error) { FileLog.e(error); } finally { if(cursor!=null)cursor.dispose(); }
    }
    public static void deleted(int account, TLRPC.Message message) {
        if (!NyxConfig.get("keepDeleted",account,false) || !eligible(message)) return;
        long uid=owner(account),did=MessageObject.getDialogId(message);int mid=message.id;byte[] bytes=encode(message);
        queue.postRunnable(() -> {
            if(owner(account)!=uid || !NyxConfig.get("keepDeleted",account,false)) return;
            try { open();history(uid,did,mid,DELETED,bytes,hash(decode(bytes,uid)),System.currentTimeMillis());prune(account,uid,true); }
            catch(Exception error){FileLog.e(error);}
        });
    }
    private static void prune(int account,long uid,boolean force) {
        if(!force && System.currentTimeMillis()-lastPrune<60000)return;lastPrune=System.currentTimeMillis();
        int days=NyxConfig.number("retention",account,30);
        if(days>0) { String[] a={""+uid,""+(System.currentTimeMillis()-days*86400000L)};db.delete("history","owner=? AND changed<?",a);db.delete("snapshots","owner=? AND changed<?",a); }
        db.execSQL("DELETE FROM snapshots WHERE owner=? AND rowid NOT IN (SELECT rowid FROM snapshots WHERE owner=? ORDER BY changed DESC LIMIT 8000)",new Object[]{uid,uid});
        db.execSQL("DELETE FROM history WHERE owner=? AND id NOT IN (SELECT id FROM history WHERE owner=? ORDER BY id DESC LIMIT 3000)",new Object[]{uid,uid});
    }
    public static void clear(int account, boolean all, Runnable done) {
        long uid=owner(account);queue.postRunnable(() -> {
            try {open();db.delete("history",all?null:"owner=?",all?null:new String[]{""+uid});db.delete("snapshots",all?null:"owner=?",all?null:new String[]{""+uid});}
            catch(Exception error){FileLog.e(error);AndroidUtilities.runOnUIThread(()->android.widget.Toast.makeText(ApplicationLoader.applicationContext,LocaleController.getString(R.string.NyxArchiveError),android.widget.Toast.LENGTH_LONG).show());return;}AndroidUtilities.runOnUIThread(()->{
                NyxArchiveGate.lock();
                for(int a=0;a<UserConfig.MAX_ACCOUNT_COUNT;a++){
                    long accountOwner=owner(a);
                    if(accountOwner!=0 && (all || accountOwner==uid))NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces,MessagesController.UPDATE_MASK_ALL,"nyx_archive_cleared",accountOwner);
                }
                done.run();
            });
        });
    }
    public static void list(int account,long did,int kind,String query,long before,Utilities.Callback<ArrayList<Entry>> callback) {
        long uid=owner(account);queue.postRunnable(() -> {
            ArrayList<Entry> result=new ArrayList<>();
            try {
                open();prune(account,uid,true);
                String where="owner=? AND kind=?";ArrayList<String> a=new ArrayList<>(Arrays.asList(""+uid,""+kind));
                if(did!=0){where+=" AND did=?";a.add(""+did);}if(before>0){where+=" AND id<?";a.add(""+before);}
                try(Cursor c=db.query("history",null,where,a.toArray(new String[0]),null,null,"id DESC","3000")) {
                    while(c.moveToNext() && result.size()<100) {
                        Entry e=new Entry();e.id=c.getLong(c.getColumnIndexOrThrow("id"));e.dialog=c.getLong(c.getColumnIndexOrThrow("did"));e.mid=c.getInt(c.getColumnIndexOrThrow("mid"));e.kind=kind;e.changed=c.getLong(c.getColumnIndexOrThrow("changed"));
                        e.message=decode(crypt(c.getBlob(c.getColumnIndexOrThrow("body")),uid,e.dialog,e.mid,kind,false),uid);
                        e.message.dialog_id=e.dialog;
                        if(query!=null && !query.isEmpty() && !(e.message.message==null?"":e.message.message).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;
                        result.add(e);
                    }
                }
            }catch(Exception error){FileLog.e(error);}
            AndroidUtilities.runOnUIThread(() -> callback.run(owner(account)==uid && NyxArchiveGate.canShow(account)?result:new ArrayList<>()));
        });
    }
    public static void merge(int account,long did,long thread,ArrayList<MessageObject> source,Utilities.Callback<ArrayList<MessageObject>> callback) {
        if(!NyxConfig.get("keepDeleted",account,false) || !NyxArchiveGate.canShow(account) || thread!=0 || DialogObject.isEncryptedDialog(did)) {callback.run(source);return;}
        long uid=owner(account);ArrayList<MessageObject> input=new ArrayList<>(source);
        queue.postRunnable(() -> {
            ArrayList<MessageObject> output=new ArrayList<>(input);
            try {
                open();prune(account,uid,true);int min=Integer.MAX_VALUE,max=0;Set<Integer> ids=new HashSet<>();
                for(MessageObject m:input)if(m.getDialogId()==did && m.getId()>0){min=Math.min(min,m.getId());max=Math.max(max,m.getId());ids.add(m.getId());}
                String selection="owner=? AND did=? AND kind=1";ArrayList<String> range=new ArrayList<>(Arrays.asList(""+uid,""+did));
                if(max>0){selection+=" AND mid>=? AND mid<=?";range.add(""+min);range.add(""+max);}
                try(Cursor c=db.rawQuery("SELECT mid,body FROM history WHERE "+selection+" GROUP BY mid ORDER BY mid DESC LIMIT 100",range.toArray(new String[0]))) {
                    while(c.moveToNext()) {
                        int mid=c.getInt(0);
                        for(MessageObject present:output)if(present.getId()==mid && present.getDialogId()==did)present.nyxDeleted=true;
                        if(ids.contains(mid))continue;
                        TLRPC.Message message=decode(crypt(c.getBlob(1),uid,did,mid,DELETED,false),uid);message.dialog_id=did;
                        MessageObject m=new MessageObject(account,message,true,true);
                        if(message.media!=null && !(message.media instanceof TLRPC.TL_messageMediaEmpty) && !m.mediaExists && !m.attachPathExists) {
                            message.media=new TLRPC.TL_messageMediaEmpty();message.message=LocaleController.getString(R.string.NyxArchiveLocalFile)+(message.message==null || message.message.isEmpty()?"":"\n"+message.message);
                            m=new MessageObject(account,message,true,false);
                        }
                        m.nyxDeleted=true;output.add(m);
                    }
                }
                output.sort((a,b)->Integer.compare(b.getId(),a.getId()));
            }catch(Exception error){FileLog.e(error);}
            AndroidUtilities.runOnUIThread(()->callback.run(owner(account)==uid && NyxArchiveGate.canShow(account)?output:source));
        });
    }
}
