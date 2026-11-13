/* Tetogram, 2026. GNU GPL v2 or later. Local presentation, never serialized into Telegram messages. */
package org.telegram.messenger.tetogram;
import java.util.ArrayList;
import org.telegram.SQLite.*;
import org.telegram.messenger.*;
import org.telegram.tgnet.*;
public final class LocalReplies {
    private LocalReplies() {}
    public static final String SCHEMA="CREATE TABLE IF NOT EXISTS tetogram_reply_links_v1 (owner INTEGER NOT NULL,did INTEGER NOT NULL,random_id INTEGER NOT NULL,mid INTEGER NOT NULL,mode INTEGER NOT NULL,source_mid INTEGER NOT NULL,sender INTEGER NOT NULL,body_start INTEGER NOT NULL,prefix_hash TEXT NOT NULL,PRIMARY KEY(owner,did,random_id,mode))";
    private static SQLiteDatabase db(int a) throws Exception { SQLiteDatabase d=DeletedHistory.db(a);d.executeFast(SCHEMA).stepThis().dispose();return d; }
    public static void remember(MessageObject sent,MessageObject source,int start) {
        if(start<=0 || source==null || sent.isEphemeral() || sent.getDialogId()!=source.getDialogId())return;
        int a=sent.currentAccount; long owner=UserConfig.getInstance(a).getClientUserId();
        if(owner==0 || source.tetogramHistoryOwner!=owner)return;
        String hash=LocalReplyPolicy.prefix(sent.messageOwner.message,start);if(hash.isEmpty())return;
        final int mid=sent.getId(),mode=sent.scheduled?1:0,sourceId=source.getId();
        final long did=sent.getDialogId(),random=sent.messageOwner.random_id,sender=MessageObject.getPeerId(source.messageOwner.from_id);
        if(random==0)return;
        sent.setTetogramLocalReply(cloneSource(source),start,hash,owner);
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()-> {
            if(owner!=UserConfig.getInstance(a).getClientUserId())return;
            try {
                SQLitePreparedStatement s=db(a).executeFast("INSERT OR REPLACE INTO tetogram_reply_links_v1 VALUES(?,?,?,?,?,?,?,?,?)");
                try {s.bindLong(1,owner);s.bindLong(2,did);s.bindLong(3,random);s.bindInteger(4,mid);s.bindInteger(5,mode);s.bindInteger(6,sourceId);s.bindLong(7,sender);s.bindInteger(8,start);s.bindString(9,hash);s.step();}finally{s.dispose();}
            } catch(Exception e){FileLog.e("Tetogram reply link write failed",e);}
        });
    }
    private static MessageObject cloneSource(MessageObject source) {
        MessageObject copy=new MessageObject(source.currentAccount,TLObject.deepCopy(source.messageOwner,TLRPC.Message::TLdeserialize),true,false);
        copy.tetogramHistoryOwner=source.tetogramHistoryOwner;copy.tetogramLocalDeleted=true;
        copy.tetogramDeletedAt=source.tetogramDeletedAt;copy.tetogramReceivedAt=source.tetogramReceivedAt;return copy;
    }
    /** Called on the native storage queue during ID acknowledgement. */
    public static void remap(int a,long did,long random,int oldId,int newId,int mode) {
        try {
            SQLiteDatabase d=db(a);long owner=UserConfig.getInstance(a).getClientUserId();
            if(mode>1)return;
            SQLitePreparedStatement s=d.executeFast("UPDATE tetogram_reply_links_v1 SET mid=?"+(mode>=0?",mode="+mode:"")+" WHERE owner=? AND did=? AND "+(random!=0?"random_id=?":"mode="+(mode==1?1:0)+" AND mid=?"));
            try{s.bindInteger(1,newId);s.bindLong(2,owner);s.bindLong(3,did);s.bindLong(4,random!=0?random:oldId);s.step();}finally{s.dispose();}
        }catch(Exception e){FileLog.e("Tetogram reply link remap failed",e);}
    }
    public static void hydrate(int a,ArrayList<MessageObject> messages,Runnable done) {
        long owner=UserConfig.getInstance(a).getClientUserId();
        MessagesStorage.getInstance(a).getStorageQueue().postRunnable(()-> {
            ArrayList<MessageObject> originals=new ArrayList<>();ArrayList<Integer> starts=new ArrayList<>();ArrayList<String> hashes=new ArrayList<>();
            try {
                SQLiteDatabase d=db(a);
                for(MessageObject m:messages) {
                    MessageObject original=null;int start=0;String hash=null;
                    if(m!=null && m.currentAccount==a && !m.tetogramLocalDeleted && !m.isEphemeral() && !m.isSecretMedia() && !DialogObject.isEncryptedDialog(m.getDialogId())) {
                        SQLiteCursor c=d.queryFinalized("SELECT h.data,h.did,h.deleted,h.received,l.body_start,l.prefix_hash FROM tetogram_reply_links_v1 l JOIN tetogram_deleted_v1 h ON h.account=l.owner AND h.did=l.did AND h.mid=l.source_mid AND h.sender=l.sender WHERE l.owner="+owner+" AND l.did="+m.getDialogId()+" AND l.mode="+(m.scheduled?1:0)+" AND l.mid="+m.getId()+" AND h.deleted>0 LIMIT 1");
                        try {if(c.next()){original=DeletedHistory.read(a,c);start=c.intValue(4);hash=c.stringValue(5);}}finally{c.dispose();}
                    }
                    originals.add(original);starts.add(start);hashes.add(hash);
                }
            }catch(Exception e){FileLog.e("Tetogram reply link load failed",e);}
            AndroidUtilities.runOnUIThread(()-> {
                if(owner!=UserConfig.getInstance(a).getClientUserId())return;
                for(int i=0;i<originals.size();i++)if(messages.get(i)!=null)messages.get(i).setTetogramLocalReply(originals.get(i),starts.get(i),hashes.get(i),owner);
                done.run();
            });
        });
    }
}
