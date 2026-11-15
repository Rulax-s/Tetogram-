/* Tetogram, 2026. GPL v2 or later. Version ledger inside the existing account database. */
package org.telegram.messenger.tetogram;

import java.util.*;
import org.telegram.SQLite.*;
import org.telegram.messenger.*;
import org.telegram.tgnet.*;

public final class EditHistory {
    private EditHistory() {}
    public static final String SCHEMA = "CREATE TABLE IF NOT EXISTS tetogram_edits_v1 (seq INTEGER PRIMARY KEY AUTOINCREMENT, account INTEGER NOT NULL, did INTEGER NOT NULL, mid INTEGER NOT NULL, sender INTEGER NOT NULL, sent INTEGER NOT NULL, edited INTEGER NOT NULL, observed INTEGER NOT NULL, fingerprint TEXT NOT NULL, text TEXT NOT NULL, data BLOB NOT NULL)";
    public static final String INSERT = "INSERT INTO tetogram_edits_v1(account,did,mid,sender,sent,edited,observed,fingerprint,text,data) VALUES(?,?,?,?,?,?,?,?,?,?)";
    // UI-thread metadata only. No message text, session or MessageObject is cached globally.
    private static final android.util.LruCache<String,Integer> counts=new android.util.LruCache<>(4096);
    private static long owner(int a) { return UserConfig.getInstance(a).getClientUserId(); }
    public static boolean eligible(int a,TLRPC.Message m) { return m!=null&&!MessageObject.isQuickReply(m)&&!MessageObject.isWelcomeMessage(m)&&DeletedHistory.eligible(a,m,true); }
    private static SQLiteDatabase db(int a) throws Exception {
        SQLiteDatabase db=MessagesStorage.getInstance(a).getDatabase();
        if(db==null)throw new IllegalStateException("Account database unavailable");
        db.executeFast(SCHEMA).stepThis().dispose();
        db.executeFast("CREATE INDEX IF NOT EXISTS tetogram_edits_message_v1 ON tetogram_edits_v1(account,did,mid,seq)").stepThis().dispose();
        db.executeFast("CREATE INDEX IF NOT EXISTS tetogram_edits_age_v1 ON tetogram_edits_v1(account,observed)").stepThis().dispose();
        db.executeFast("PRAGMA secure_delete=ON").stepThis().dispose();
        int days=LabsSettings.days(a);
        if(days>0)db.executeFast("DELETE FROM tetogram_edits_v1 WHERE account="+owner(a)+" AND observed<"+(System.currentTimeMillis()/1000-days*86400L)).stepThis().dispose();
        return db;
    }
    private static TLRPC.Message read(SQLiteCursor c,int column,long did) throws Exception {
        NativeByteBuffer data=c.byteBufferValue(column);if(data==null)return null;
        try {TLRPC.Message m=TLRPC.Message.TLdeserialize(data,data.readInt32(false),false);if(m!=null)m.dialog_id=did;return m;}finally{data.reuse();}
    }
    private static void save(SQLiteDatabase db,long uid,long did,TLRPC.Message m,String fingerprint,long time) throws Exception {
        SQLiteCursor head=db.queryFinalized("SELECT edited,fingerprint FROM tetogram_edits_v1 WHERE account=? AND did=? AND mid=? ORDER BY seq DESC LIMIT 1",uid,did,m.id);
        try{if(head.next()&&!EditHistoryPolicy.append(head.intValue(0),head.stringValue(1),m.edit_date,fingerprint))return;}finally{head.dispose();}
        NativeByteBuffer data=new NativeByteBuffer(m.getObjectSize());
        try {
            m.serializeToStream(data);SQLitePreparedStatement put=db.executeFast(INSERT);
            try{put.bindLong(1,uid);put.bindLong(2,did);put.bindInteger(3,m.id);put.bindLong(4,MessageObject.getPeerId(m.from_id));put.bindInteger(5,m.date);put.bindInteger(6,m.edit_date);put.bindLong(7,time);put.bindString(8,fingerprint);put.bindString(9,m.message==null?"":m.message);put.bindByteBuffer(10,data);put.step();}finally{put.dispose();}
        }finally{data.reuse();}
    }
    /** Both normal message-write paths call this on storageQueue BEFORE native data or deletion snapshots are overwritten. */
    public static void capture(int a,ArrayList<TLRPC.Message> messages) {
        if(!LabsSettings.edits(a)||messages==null||messages.isEmpty())return;
        long uid=owner(a);if(uid<=0)return;
        try {
            SQLiteDatabase db=db(a);
            for(TLRPC.Message next:messages){
                if(uid!=owner(a)||!LabsSettings.edits(a))break;
                if(next==null)continue;long did=MessageObject.getDialogId(next);
                if(!eligible(a,next)){if(did!=0&&next.id>0){eraseInline(db,uid,did,next.id);changed(a,uid,did,next.id,0);}continue;}
                if(next.edit_date<=0&&next.send_state!=MessageObject.MESSAGE_SEND_STATE_EDITING)continue;
                TLRPC.Message previous=null;
                boolean pendingNative=false;
                SQLiteCursor old=db.queryFinalized("SELECT data,send_state FROM messages_v2 WHERE uid=? AND mid=?",did,next.id);
                try{if(old.next()){previous=read(old,0,did);pendingNative=old.intValue(1)==MessageObject.MESSAGE_SEND_STATE_EDITING;}}finally{old.dispose();}
                if(previous!=null && !eligible(a,previous)){eraseInline(db,uid,did,next.id);changed(a,uid,did,next.id,0);continue;}
                if(next.send_state==MessageObject.MESSAGE_SEND_STATE_EDITING){
                    // Native own-media edits overwrite the cache optimistically. Preserve only the confirmed baseline;
                    // never call the attempted text a server version, including retry/cancel/error paths.
                    if(previous!=null&&!pendingNative){save(db,uid,did,previous,EditHistoryContent.fingerprint(previous),System.currentTimeMillis()/1000);changed(a,uid,did,next.id,count(db,uid,did,next.id));}
                    continue;
                }
                if(next.send_state!=0)continue;
                TLRPC.Message retained=null;
                old=db.queryFinalized("SELECT data FROM tetogram_edits_v1 WHERE account=? AND did=? AND mid=? ORDER BY seq DESC LIMIT 1",uid,did,next.id);
                try{if(old.next())retained=read(old,0,did);}finally{old.dispose();}
                if(previous==null||pendingNative||EditHistoryContent.fingerprint(next).equals(EditHistoryContent.fingerprint(previous))&&retained!=null)previous=retained;
                if(previous!=null&&(previous.id!=next.id||!eligible(a,previous))){eraseInline(db,uid,did,next.id);changed(a,uid,did,next.id,0);continue;}
                String after=EditHistoryContent.fingerprint(next),before=previous==null?null:EditHistoryContent.fingerprint(previous);
                if(previous!=null&&!EditHistoryPolicy.changed(previous.edit_date,next.edit_date,before,after))continue;
                // A repeated/reordered history page cannot move the ledger backwards.
                SQLiteCursor newest=db.queryFinalized("SELECT edited,fingerprint FROM tetogram_edits_v1 WHERE account=? AND did=? AND mid=? ORDER BY seq DESC LIMIT 1",uid,did,next.id);
                int maxEdit=0;String latest=null;try{if(newest.next()){maxEdit=newest.intValue(0);latest=newest.stringValue(1);}}finally{newest.dispose();}
                if(!EditHistoryPolicy.append(maxEdit,latest,next.edit_date,after))continue;
                db.executeFast("SAVEPOINT tetogram_edit_capture_v1").stepThis().dispose();
                try{
                    long now=System.currentTimeMillis()/1000;
                    if(previous!=null)save(db,uid,did,previous,before,now);
                    save(db,uid,did,next,after,now);
                }catch(Exception e){db.executeFast("ROLLBACK TO SAVEPOINT tetogram_edit_capture_v1").stepThis().dispose();throw e;}
                finally{db.executeFast("RELEASE SAVEPOINT tetogram_edit_capture_v1").stepThis().dispose();}
                changed(a,uid,did,next.id,count(db,uid,did,next.id));
            }
        }catch(Exception e){FileLog.e("Tetogram edit history capture failed");}
    }
    private static int count(SQLiteDatabase db,long uid,long did,int mid) throws Exception {
        SQLiteCursor c=db.queryFinalized("SELECT COUNT(*) FROM tetogram_edits_v1 WHERE account=? AND did=? AND mid=?",uid,did,mid);
        try{return c.next()?c.intValue(0):0;}finally{c.dispose();}
    }
    private static void changed(int a,long uid,long did,int mid,int count){AndroidUtilities.runOnUIThread(()->{if(uid<=0||uid!=owner(a))return;counts.put(EditHistoryPolicy.key(uid,did,mid),count);NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.tetogramEditHistoryChanged,did,mid);});}
    public static int cachedCount(MessageObject m){if(m==null||m.scheduled||!eligible(m.currentAccount,m.messageOwner))return 0;Integer n=counts.get(EditHistoryPolicy.key(owner(m.currentAccount),m.getDialogId(),m.getId()));return n==null?0:n;}
    public static boolean hasHistory(MessageObject m){return cachedCount(m)>=2;}
    public static boolean canOpen(MessageObject m){return m!=null&&!m.scheduled&&eligible(m.currentAccount,m.messageOwner)&&(cachedCount(m)>0||LabsSettings.edits(m.currentAccount)&&m.messageOwner.edit_date>0);}
    public static void refresh(int a){NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.tetogramEditHistoryChanged,0L,0);}
    /** Page counts are batched by dialog; hydration never changes message payloads, IDs or ordering. */
    public static void hydrate(int a,ArrayList<MessageObject> messages,Runnable done){
        long uid=owner(a);if(uid<=0||messages.isEmpty()){done.run();return;}
        HashMap<Long,ArrayList<Integer>> ids=new HashMap<>();
        for(MessageObject m:messages)if(!m.scheduled&&eligible(a,m.messageOwner))ids.computeIfAbsent(m.getDialogId(),k->new ArrayList<>()).add(m.getId());
        if(ids.isEmpty()){done.run();return;}
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()->{
            HashMap<String,Integer> loaded=new HashMap<>();
            try{SQLiteDatabase db=db(a);for(Map.Entry<Long,ArrayList<Integer>> entry:ids.entrySet()){
                StringBuilder in=new StringBuilder();for(int id:entry.getValue()){if(in.length()>0)in.append(',');in.append(id);loaded.put(EditHistoryPolicy.key(uid,entry.getKey(),id),0);}
                SQLiteCursor c=db.queryFinalized("SELECT mid,COUNT(*) FROM tetogram_edits_v1 WHERE account="+uid+" AND did="+entry.getKey()+" AND mid IN ("+in+") GROUP BY mid");
                try{while(c.next())loaded.put(EditHistoryPolicy.key(uid,entry.getKey(),c.intValue(0)),c.intValue(1));}finally{c.dispose();}
            }}catch(Exception e){FileLog.e("Tetogram edit history metadata unavailable");}
            AndroidUtilities.runOnUIThread(()->{if(uid==owner(a))for(Map.Entry<String,Integer> e:loaded.entrySet())counts.put(e.getKey(),e.getValue());done.run();});
        });
    }
    public static final class Version {
        public MessageObject message;public long observed;public boolean latest;
    }
    public static final class Page extends ArrayList<Version>{public boolean failed;}
    public static void list(int a,long did,int mid,int offset,DeletedHistory.Callback<Page> callback){
        long uid=owner(a);MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()->{
            Page page=new Page();HashMap<Integer,Integer> loaded=new HashMap<>();
            try{SQLiteDatabase db=db(a);
                SQLiteCursor c=db.queryFinalized("SELECT r.data,r.observed,r.seq=(SELECT MAX(q.seq) FROM tetogram_edits_v1 q WHERE q.account=r.account AND q.did=r.did AND q.mid=r.mid) FROM tetogram_edits_v1 r WHERE r.account="+uid+" AND r.did="+did+(mid==0?"":" AND r.mid="+mid)+" ORDER BY r.seq DESC LIMIT 50 OFFSET "+Math.max(0,offset));
                try{while(c.next()){TLRPC.Message message=read(c,0,did);if(message==null||!eligible(a,message))continue;Version version=new Version();version.message=new MessageObject(a,message,true,false);version.observed=c.longValue(1);version.latest=c.intValue(2)!=0;page.add(version);}}finally{c.dispose();}
                for(Version version:page)loaded.put(version.message.getId(),count(db,uid,did,version.message.getId()));
                if(mid!=0&&!loaded.containsKey(mid))loaded.put(mid,0);
            }catch(Exception e){page.failed=true;FileLog.e("Tetogram edit history lookup failed");}
            AndroidUtilities.runOnUIThread(()->{if(uid==owner(a)){for(Map.Entry<Integer,Integer> e:loaded.entrySet())counts.put(EditHistoryPolicy.key(uid,did,e.getKey()),e.getValue());callback.run(page);}else callback.run(new Page());});
        });
    }
    static void eraseInline(SQLiteDatabase db,long uid,long did,int mid)throws Exception{db.executeFast("DELETE FROM tetogram_edits_v1 WHERE account="+uid+(did==0?"":" AND did="+did)+(mid==0?"":" AND mid="+mid)).stepThis().dispose();}
    private static void invalidate(long uid,long did,int mid){String prefix=uid+":"+(did==0?"":did+":");if(mid!=0&&did!=0){counts.put(EditHistoryPolicy.key(uid,did,mid),0);return;}for(String key:counts.snapshot().keySet())if(key.startsWith(prefix))counts.remove(key);}
    public static void erase(int a,long did,int mid,DeletedHistory.Callback<Boolean> done){long uid=owner(a);MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()->{boolean ok=false;try{SQLiteDatabase db=db(a);eraseInline(db,uid,did,mid);db.executeFast("PRAGMA wal_checkpoint(TRUNCATE)").stepThis().dispose();ok=true;}catch(Exception e){FileLog.e("Tetogram edit history erase failed");}boolean success=ok;AndroidUtilities.runOnUIThread(()->{if(uid!=owner(a))return;if(success){invalidate(uid,did,mid);refresh(a);}done.run(success);});});}
    public static void maintain(int a){long uid=owner(a);MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()->{try{db(a);}catch(Exception e){FileLog.e("Tetogram edit history cleanup failed");}AndroidUtilities.runOnUIThread(()->{if(uid==owner(a)){invalidate(uid,0,0);refresh(a);}});});}
}
