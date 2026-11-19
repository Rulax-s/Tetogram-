/* Tetogram, 2026. GNU GPL v2 or later. Hide group messages from people on the Telegram block list, locally. */
package org.telegram.messenger.tetogram;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

/**
 * Only presentation: nothing is deleted, reported or marked as read, and the server block list is never changed here.
 * Telegram loads its own block list only on the privacy screens, so the full list is synced here (paged
 * contacts.getBlocked) and kept current from block/unblock actions and updatePeerBlocked.
 */
public final class Ignore {
    private Ignore(){}
    public static final long SYNC_INTERVAL_MS=6L*3600*1000;

    private static final class State {
        final boolean enabled;final Set<Long> blocked,people,chats;final long syncedAt;
        State(boolean enabled,Set<Long> blocked,Set<Long> people,Set<Long> chats,long syncedAt){this.enabled=enabled;this.blocked=blocked;this.people=people;this.chats=chats;this.syncedAt=syncedAt;}
    }
    private static final ConcurrentHashMap<Long,State> STATES=new ConcurrentHashMap<>();
    private static final Set<Long> SYNCING=ConcurrentHashMap.newKeySet();

    private static long owner(int a){return UserConfig.getInstance(a).getClientUserId();}
    private static SharedPreferences prefs(long uid){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_ignore_u"+uid,Context.MODE_PRIVATE);}
    private static Set<Long> ids(String csv){HashSet<Long> out=new HashSet<>();if(csv!=null)for(String part:csv.split(","))if(!part.isEmpty())try{out.add(Long.parseLong(part));}catch(NumberFormatException ignored){}return Collections.unmodifiableSet(out);}
    private static String csv(Set<Long> ids){StringBuilder b=new StringBuilder();for(long id:ids){if(b.length()>0)b.append(',');b.append(id);}return b.toString();}
    private static State state(long uid){
        if(uid<=0)return null;State s=STATES.get(uid);if(s!=null)return s;
        SharedPreferences p=prefs(uid);
        s=new State(p.getBoolean("enabled",false),ids(p.getString("blocked","")),ids(p.getString("people","")),ids(p.getString("chats","")),p.getLong("synced",0));
        STATES.put(uid,s);return s;
    }
    private static synchronized void store(long uid,State s){STATES.put(uid,s);prefs(uid).edit().putBoolean("enabled",s.enabled).putString("blocked",csv(s.blocked)).putString("people",csv(s.people)).putString("chats",csv(s.chats)).putLong("synced",s.syncedAt).apply();}

    // ---- settings ----
    public static boolean enabled(int a){State s=state(owner(a));return s!=null&&s.enabled;}
    public static Set<Long> blocked(int a){State s=state(owner(a));return s==null?Collections.emptySet():s.blocked;}
    public static Set<Long> allowedPeople(int a){State s=state(owner(a));return s==null?Collections.emptySet():s.people;}
    public static Set<Long> allowedChats(int a){State s=state(owner(a));return s==null?Collections.emptySet():s.chats;}
    public static long syncedAt(int a){State s=state(owner(a));return s==null?0:s.syncedAt;}
    public static synchronized void setEnabled(int a,boolean on){long uid=owner(a);State s=state(uid);if(s==null)return;store(uid,new State(on,s.blocked,s.people,s.chats,s.syncedAt));if(on)maybeSync(a);}
    public static synchronized void allowPerson(int a,long id,boolean allow){long uid=owner(a);State s=state(uid);if(s==null||id==0)return;HashSet<Long> next=new HashSet<>(s.people);if(allow)next.add(id);else next.remove(id);store(uid,new State(s.enabled,s.blocked,Collections.unmodifiableSet(next),s.chats,s.syncedAt));}
    public static synchronized void allowChat(int a,long dialog,boolean allow){long uid=owner(a);State s=state(uid);if(s==null||dialog==0)return;HashSet<Long> next=new HashSet<>(s.chats);if(allow)next.add(dialog);else next.remove(dialog);store(uid,new State(s.enabled,s.blocked,s.people,Collections.unmodifiableSet(next),s.syncedAt));}
    /** Called by MessagesController on block/unblock and on updatePeerBlocked. */
    public static synchronized void blockedChanged(int a,long id,boolean blocked){long uid=owner(a);State s=state(uid);if(s==null||id==0||s.blocked.contains(id)==blocked)return;HashSet<Long> next=new HashSet<>(s.blocked);if(blocked)next.add(id);else next.remove(id);store(uid,new State(s.enabled,Collections.unmodifiableSet(next),s.people,s.chats,s.syncedAt));}

    // ---- decisions (any thread) ----
    /** Group or channel discussion messages only; private chats are already covered by blocking itself. */
    public static boolean hides(int a,long dialog,long sender){
        if(dialog>=0||sender==0)return false;
        State s=state(owner(a));
        return s!=null&&s.enabled&&s.blocked.contains(sender)&&!s.people.contains(sender)&&!s.chats.contains(dialog);
    }
    public static boolean hides(int a,MessageObject m){return m!=null&&m.messageOwner!=null&&!m.isOutOwner()&&hides(a,m.getDialogId(),m.getFromChatId());}
    public static boolean hidesReply(int a,MessageObject m){if(m==null||!enabled(a))return false;MessageObject reply=m.getDisplayReplyMessageObject();return reply!=null&&!reply.isOutOwner()&&hides(a,m.getDialogId(),reply.getFromChatId());}
    private static boolean any(int a,ArrayList<MessageObject> list){if(list==null||!enabled(a))return false;for(MessageObject m:list)if(hides(a,m))return true;return false;}
    /** History page: removes hidden messages while keeping the server page size, so paging still works. */
    public static ArrayList<MessageObject> page(int a,ArrayList<MessageObject> page){
        if(!any(a,page))return page;
        DeletedHistory.Page out=page instanceof DeletedHistory.Page?(DeletedHistory.Page)page:new DeletedHistory.Page(page);
        out.removeIf(m->hides(a,m));
        return out;
    }
    /** New messages and notifications: a filtered copy, the original list is left untouched. */
    public static ArrayList<MessageObject> fresh(int a,ArrayList<MessageObject> list){if(!any(a,list))return list;ArrayList<MessageObject> out=new ArrayList<>(list.size());for(MessageObject m:list)if(!hides(a,m))out.add(m);return out;}

    // ---- sync of the full block list ----
    public static void maybeSync(int a){State s=state(owner(a));if(s!=null&&s.enabled&&System.currentTimeMillis()-s.syncedAt>SYNC_INTERVAL_MS)sync(a,null);}
    public interface Done{void run(boolean ok);}
    public static void sync(int a,Done done){
        long uid=owner(a);if(uid<=0||!SYNCING.add(uid)){if(done!=null)done.run(false);return;}
        page(a,uid,new HashSet<>(),done);
    }
    private static void page(int a,long uid,HashSet<Long> collected,Done done){
        TLRPC.TL_contacts_getBlocked req=new TLRPC.TL_contacts_getBlocked();req.offset=collected.size();req.limit=100;
        ConnectionsManager.getInstance(a).sendRequest(req,(response,error)->AndroidUtilities.runOnUIThread(()->{
            if(uid!=owner(a)||!(response instanceof TLRPC.contacts_Blocked)){SYNCING.remove(uid);if(done!=null)done.run(false);return;}
            TLRPC.contacts_Blocked res=(TLRPC.contacts_Blocked)response;
            MessagesController mc=MessagesController.getInstance(a);mc.putUsers(res.users,false);mc.putChats(res.chats,false);
            int before=collected.size();
            for(TLRPC.TL_peerBlocked blocked:res.blocked)collected.add(MessageObject.getPeerId(blocked.peer_id));
            boolean more=res instanceof TLRPC.TL_contacts_blockedSlice&&res.blocked.size()==req.limit&&collected.size()>before&&collected.size()<res.count;
            if(more){page(a,uid,collected,done);return;}
            synchronized(Ignore.class){State s=state(uid);if(s!=null)store(uid,new State(s.enabled,Collections.unmodifiableSet(new HashSet<>(collected)),s.people,s.chats,System.currentTimeMillis()));}
            SYNCING.remove(uid);if(done!=null)done.run(true);
        }));
    }
}
