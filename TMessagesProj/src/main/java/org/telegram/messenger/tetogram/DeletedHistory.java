/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.messenger.tetogram;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/** Local overlay only. All SQL runs on the existing account's storage queue. */
public final class DeletedHistory {
    private DeletedHistory() {}
    public interface Callback<T> { void run(T value); }
    public static final String SCHEMA = "CREATE TABLE IF NOT EXISTS tetogram_deleted_v1 (account INTEGER NOT NULL, did INTEGER NOT NULL, mid INTEGER NOT NULL, channel INTEGER NOT NULL, sender INTEGER NOT NULL, text TEXT NOT NULL, search_text TEXT NOT NULL, topic INTEGER NOT NULL, sent INTEGER NOT NULL, received INTEGER NOT NULL, deleted INTEGER NOT NULL DEFAULT 0, data BLOB NOT NULL, PRIMARY KEY(account,did,mid))";
    public static final String MARK_GLOBAL = "UPDATE tetogram_deleted_v1 SET deleted=? WHERE account=? AND mid=? AND deleted=0 AND channel=0";
    public static final String MARK_CHANNEL = "UPDATE tetogram_deleted_v1 SET deleted=? WHERE account=? AND mid=? AND deleted=0 AND did=? AND channel=?";
    private static final DeletedRowPolicy.Row<MessageObject> ROW = new DeletedRowPolicy.Row<MessageObject>() {
        public int account(MessageObject m) { return m.currentAccount; }
        public long dialog(MessageObject m) { return m.getDialogId(); }
        public int id(MessageObject m) { return m.getId(); }
        public int stableId(MessageObject m) { return m.stableId; }
        public void stableId(MessageObject m, int id) { m.stableId = id; }
        public void copyStableParams(MessageObject m, MessageObject original) { m.copyStableParams(original); }
        public MessageObject clonePayload(MessageObject snapshot) {
            MessageObject m = new MessageObject(snapshot.currentAccount,
                    org.telegram.tgnet.TLObject.deepCopy(snapshot.messageOwner, TLRPC.Message::TLdeserialize), true, false);
            m.tetogramLocalDeleted = true;
            m.tetogramDeletedAt = snapshot.tetogramDeletedAt;
            m.tetogramReceivedAt = snapshot.tetogramReceivedAt;
            return m;
        }
    };
    /** UI-thread only: keep stock stable params without sharing mutable MessageObjects across fragments. */
    public static MessageObject copyForRow(MessageObject original, MessageObject payload, java.util.function.IntSupplier allocate) {
        return DeletedRowPolicy.replace(original, payload, ROW, allocate);
    }
    private static long uid(int a) { return UserConfig.getInstance(a).getClientUserId(); }
    private static SQLiteDatabase db(int a) throws Exception {
        SQLiteDatabase db = MessagesStorage.getInstance(a).getDatabase();
        if (db == null) throw new IllegalStateException("Account database not opened");
        db.executeFast(SCHEMA).stepThis().dispose();
        db.executeFast("CREATE INDEX IF NOT EXISTS tetogram_deleted_time_v1 ON tetogram_deleted_v1(account,did,deleted)").stepThis().dispose();
        db.executeFast("CREATE INDEX IF NOT EXISTS tetogram_deleted_age_v1 ON tetogram_deleted_v1(account,received)").stepThis().dispose();
        db.executeFast("CREATE INDEX IF NOT EXISTS tetogram_deleted_ids_v1 ON tetogram_deleted_v1(account,channel,mid)").stepThis().dispose();
        db.executeFast("PRAGMA secure_delete=ON").stepThis().dispose();
        int days = LabsSettings.days(a);
        if (days > 0) db.executeFast("DELETE FROM tetogram_deleted_v1 WHERE account=" + uid(a) + " AND received<" + (System.currentTimeMillis()/1000 - days*86400L)).stepThis().dispose();
        return db;
    }
    public static boolean eligible(int a, TLRPC.Message m) {
        if(m==null) return false;
        long did=MessageObject.getDialogId(m);
        TLRPC.MessageMedia media=MessageObject.getMedia(m);
        TLRPC.Chat chat=did<0 && !DialogObject.isEncryptedDialog(did) ? org.telegram.messenger.MessagesController.getInstance(a).getChat(-did) : null;
        boolean restricted=m.noforwards || chat!=null && chat.noforwards;
        return did!=0 && HistoryPolicy.maySave(true,m instanceof TLRPC.TL_message && !MessageObject.isEphemeral(m),DialogObject.isEncryptedDialog(did),m.out,restricted,LabsSettings.excluded(a,did),m.id,m.ttl,m.ttl_period,media==null?0:media.ttl_seconds,m.expire_date);

    }
    /** Called before stock SQLite writes, never from UI, never uploads history. */
    public static void capture(int a, ArrayList<TLRPC.Message> messages) {
        if (!LabsSettings.enabled(a) || messages == null || messages.isEmpty()) return;
        try {
            SQLiteDatabase db = db(a);
            db.executeFast("SAVEPOINT tetogram_capture_v1").stepThis().dispose();
            try {
            for (TLRPC.Message m : messages) {
                if(m==null)continue;
                if (!eligible(a, m)) {
                    db.executeFast("DELETE FROM tetogram_deleted_v1 WHERE account="+uid(a)+" AND did="+MessageObject.getDialogId(m)+" AND mid="+m.id).stepThis().dispose();
                    continue;
                }
                long did = MessageObject.getDialogId(m);
                NativeByteBuffer buffer = new NativeByteBuffer(m.getObjectSize());
                try {
                    m.serializeToStream(buffer);
                    SQLitePreparedStatement insert = db.executeFast("INSERT OR IGNORE INTO tetogram_deleted_v1 VALUES(?,?,?,?,?,?,?,?,?,?,0,?)");
                    try {
                        insert.bindLong(1,uid(a)); insert.bindLong(2,did); insert.bindInteger(3,m.id);
                        insert.bindLong(4,m.peer_id == null ? 0 : m.peer_id.channel_id);
                        insert.bindLong(5,MessageObject.getPeerId(m.from_id));
                        insert.bindString(6,m.message == null ? "" : m.message);
                        insert.bindString(7,(m.message == null ? "" : m.message).toLowerCase(java.util.Locale.ROOT));
                        insert.bindLong(8,m.reply_to == null ? 0 : m.reply_to.reply_to_top_id != 0 ? m.reply_to.reply_to_top_id : m.reply_to.reply_to_msg_id);
                        insert.bindInteger(9,m.date); insert.bindLong(10,System.currentTimeMillis()/1000); insert.bindByteBuffer(11,buffer); insert.step();
                    } finally { insert.dispose(); }
                    SQLitePreparedStatement update = db.executeFast("UPDATE tetogram_deleted_v1 SET text=?,search_text=?,data=? WHERE account=? AND did=? AND mid=? AND deleted=0");
                    try {
                        update.bindString(1,m.message == null ? "" : m.message);
                        update.bindString(2,(m.message == null ? "" : m.message).toLowerCase(java.util.Locale.ROOT));
                        update.bindByteBuffer(3,buffer); update.bindLong(4,uid(a)); update.bindLong(5,did); update.bindInteger(6,m.id); update.step();
                    } finally { update.dispose(); }
                } finally { buffer.reuse(); }
            }
            } catch(Exception e) {
                db.executeFast("ROLLBACK TO SAVEPOINT tetogram_capture_v1").stepThis().dispose(); throw e;
            } finally { db.executeFast("RELEASE SAVEPOINT tetogram_capture_v1").stepThis().dispose(); }
        } catch (Exception e) { FileLog.e("Tetogram history capture failed", e); }
    }
    private static MessageObject read(int a, SQLiteCursor c) throws Exception {
        NativeByteBuffer data = c.byteBufferValue(0);
        if (data == null) return null;
        try {
            TLRPC.Message m = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
            if (m == null) return null;
            m.dialog_id = c.longValue(1);
            if (!eligible(a,m)) return null;
            m.unread = false; m.media_unread = false;
            MessageObject result = new MessageObject(a,m,true,false);
            result.tetogramDeletedAt = c.longValue(2); result.tetogramReceivedAt = c.longValue(3);
            result.tetogramLocalDeleted = true;
            return result;
        } finally { data.reuse(); }
    }
    /** Only MTProto remote-delete paths call this. Stock deletion continues independently. */
    public static void dispatchDeleted(int a, long dialogKey, ArrayList<Integer> ids) {
        ArrayList<Integer> copy = new ArrayList<>(ids);
        if(!LabsSettings.enabled(a)) {
            NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.messagesDeleted,copy,-dialogKey,false);
            return;
        }
        long owner = uid(a);
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(() -> {
            HashMap<Integer, MessageObject> retained = new HashMap<>();
            if (LabsSettings.enabled(a) && owner == uid(a)) {
                try {
                    SQLiteDatabase db = db(a);
                    for (int id : copy) {
                        String scope = dialogKey == 0 ? "channel=0" : "did=" + dialogKey + " AND channel=" + (-dialogKey);
                        SQLitePreparedStatement mark=db.executeFast(dialogKey==0 ? MARK_GLOBAL : MARK_CHANNEL);
                        try {
                            mark.bindLong(1,System.currentTimeMillis()/1000); mark.bindLong(2,owner); mark.bindInteger(3,id);
                            if(dialogKey!=0) {mark.bindLong(4,dialogKey);mark.bindLong(5,-dialogKey);} mark.step();
                        }finally{mark.dispose();}
                        SQLiteCursor c = db.queryFinalized("SELECT data,did,deleted,received FROM tetogram_deleted_v1 WHERE account=" + owner + " AND mid=" + id + " AND deleted>0 AND " + scope);
                        try { while(c.next()) { MessageObject m = read(a,c); if(m != null) retained.put(id,m); } } finally { c.dispose(); }
                    }
                } catch(Exception e) { FileLog.e("Tetogram deletion archive failed",e); }
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (owner != uid(a)) return;
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.messagesDeleted, copy, -dialogKey, false, false, false, 0, null, retained);
            });
        });
    }
    public static class Page extends ArrayList<MessageObject> {
        public int serverSize;
        public boolean hasMore;
        Page(ArrayList<MessageObject> source) { super(source); serverSize=source.size(); }
    }
    public static void deliver(int a, long did, ArrayList<MessageObject> source, int count, int anchor, int loadType, boolean cache, boolean end, int mode, long thread, Callback<ArrayList<MessageObject>> callback) {
        if (!LabsSettings.visible(a) || LabsSettings.excluded(a,did) || DialogObject.isEncryptedDialog(did) || mode != 0) { callback.run(source); return; }
        long owner=uid(a);
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(() -> {
            Page page=new Page(source);
            try {
                SQLiteDatabase db=db(a);
                int low=Integer.MAX_VALUE, high=0;
                HashSet<Integer> known=new HashSet<>();
                for(MessageObject m:source) if(m.getId()>0 && !m.isEphemeral()) { low=Math.min(low,m.getId()); high=Math.max(high,m.getId()); known.add(m.getId()); }
                boolean serverEnd=(!cache && source.size()<count) || end;
                String range;
                if(loadType==0) range="mid>="+(serverEnd || high==0 ? 1 : low)+" AND mid<="+(anchor>0 ? anchor : Integer.MAX_VALUE);
                else if(loadType==1) range="mid>"+anchor+" AND mid<="+(serverEnd ? Integer.MAX_VALUE : Math.max(high,anchor));
                else range=high>0 ? "mid BETWEEN "+(serverEnd && anchor==0 ? 1 : low)+" AND "+(anchor==0 ? Integer.MAX_VALUE : high) : "mid>0";
                String order=HistoryPagePolicy.archiveOrder(anchor,loadType);
                SQLiteCursor c=db.queryFinalized("SELECT data,did,deleted,received FROM tetogram_deleted_v1 WHERE account="+owner+" AND did="+did+" AND deleted>0 "+(thread==0 ? "" : " AND (topic="+thread+" OR mid="+thread+") ")+" AND "+range+" ORDER BY "+order+" LIMIT "+(Math.max(count,30)+1));
                int added=0;
                ArrayList<MessageObject> archived = new ArrayList<>();
                try { while(c.next()) {
                    MessageObject m=read(a,c);
                    if(m==null || known.contains(m.getId())) continue;
                    if(added++>=Math.max(count,30)) { page.serverSize=count; page.hasMore=true; break; }
                    archived.add(m);
                }} finally { c.dispose(); }
                if(HistoryPagePolicy.merge(page,source,archived,count,anchor,loadType,new HistoryPagePolicy.Message<MessageObject>() {
                    @Override public int get(MessageObject m) { return m.getId(); }
                    @Override public int date(MessageObject m) { return m.messageOwner.date; }
                })) { page.hasMore=true; page.serverSize=count; }

            } catch(Exception e) { FileLog.e("Tetogram history page failed",e); }
            AndroidUtilities.runOnUIThread(()->callback.run(owner==uid(a) && LabsSettings.visible(a) && !LabsSettings.excluded(a,did) ? page : source));
        });
    }
    /** Resolve native draft/selected reply IDs after restart or deletion while composing. */
    public static void find(int a,long did,int mid,Callback<MessageObject> callback) {
        long owner=uid(a);
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(() -> {
            MessageObject found=null;
            try {
                SQLiteCursor c=db(a).queryFinalized("SELECT data,did,deleted,received FROM tetogram_deleted_v1 WHERE account=? AND did=? AND mid=? AND deleted>0",owner,did,mid);
                try {if(c.next())found=read(a,c);}finally{c.dispose();}
            }catch(Exception e){FileLog.e("Tetogram reply lookup failed",e);}
            MessageObject result=found;
            AndroidUtilities.runOnUIThread(() -> {if(owner!=0 && owner==uid(a))callback.run(result);});
        });
    }
    public static final class LocalList extends ArrayList<MessageObject> { public boolean failed; }
    public static void list(int a,long did,String query,int offset,Callback<ArrayList<MessageObject>> callback) {
        long owner=uid(a);
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()-> {
            LocalList result=new LocalList();
            try {
                SQLiteCursor c=db(a).queryFinalized("SELECT data,did,deleted,received FROM tetogram_deleted_v1 WHERE account=? AND did=? AND deleted>0 AND instr(search_text,?)>0 ORDER BY deleted DESC,mid DESC LIMIT 50 OFFSET ?",owner,did,query.toLowerCase(java.util.Locale.ROOT),offset);
                try { while(c.next()) { MessageObject m=read(a,c); if(m!=null) result.add(m); } } finally { c.dispose(); }
            } catch(Exception e) { result.failed=true; FileLog.e("Tetogram history list failed",e); }
            AndroidUtilities.runOnUIThread(()->callback.run(owner==uid(a) ? result : new ArrayList<>()));
        });
    }
    public static void erase(int a,long did,int mid,Callback<Boolean> done) {
        long owner=uid(a);
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()-> {
            boolean success=false;
            try {
                SQLiteDatabase db=db(a);
                db.executeFast("DELETE FROM tetogram_deleted_v1 WHERE account="+owner+(did==0 ? "" : " AND did="+did)+(mid==0 ? "" : " AND mid="+mid)).stepThis().dispose();
                db.executeFast("PRAGMA wal_checkpoint(TRUNCATE)").stepThis().dispose();
                success=true;
            } catch(Exception e) { FileLog.e("Tetogram history erase failed",e); }
            boolean ok=success; AndroidUtilities.runOnUIThread(()->done.run(ok));
        });
    }
    public static void maintain(int a) {
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()-> {try {db(a);}catch(Exception e){FileLog.e("Tetogram retention cleanup failed",e);}});
    }
    public static void reconcile(int a,long did,ArrayList<Integer> ids,Callback<ArrayList<Integer>> callback) {
        long owner=uid(a);
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()-> {
            ArrayList<Integer> removed=new ArrayList<>();
            try {
                SQLiteDatabase db=db(a);
                for(int id:ids) {
                    if(!LabsSettings.visible(a) || LabsSettings.excluded(a,did) || db.executeInt("SELECT 1 FROM tetogram_deleted_v1 WHERE account=? AND did=? AND mid=? AND deleted>0",owner,did,id)==null) removed.add(id);
                }
            }catch(Exception e){FileLog.e("Tetogram history reconciliation failed",e);}
            AndroidUtilities.runOnUIThread(()-> {if(owner==uid(a))callback.run(removed);});
        });
    }

    public static void forgetLocalDelete(int a,long did,ArrayList<Integer> ids) {
        if(ids==null || ids.isEmpty() || DialogObject.isEncryptedDialog(did))return;
        long owner=uid(a); ArrayList<Integer> copy=new ArrayList<>(ids);
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()-> {
            try { SQLiteDatabase db=db(a);
                for(int id:copy) db.executeFast("DELETE FROM tetogram_deleted_v1 WHERE account="+owner+" AND did="+did+" AND mid="+id).stepThis().dispose();
            }catch(Exception e){FileLog.e("Tetogram local deletion failed",e);}
        });
    }

}
