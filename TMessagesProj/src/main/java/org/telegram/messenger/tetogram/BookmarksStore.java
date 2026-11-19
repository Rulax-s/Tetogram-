/* Tetogram, 2026. GNU GPL v2 or later. Per-account bookmark storage, owner-fenced. */
package org.telegram.messenger.tetogram;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import java.util.concurrent.ConcurrentHashMap;

public final class BookmarksStore {
    private static final Object LOCK=new Object();
    private static final ConcurrentHashMap<Long,Object[]> CACHE=new ConcurrentHashMap<>();
    private BookmarksStore(){}
    private static long owner(int a){return UserConfig.getInstance(a).getClientUserId();}
    private static SharedPreferences prefs(long uid){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_bookmarks_u"+uid,Context.MODE_PRIVATE);}
    /** Read-only view; unreadable data gives an empty list (and is never overwritten, see {@link #update}). */
    public static Bookmarks load(int a){
        long uid=owner(a);if(uid<=0)return new Bookmarks();
        String raw=prefs(uid).getString("items","");Object[] cached=CACHE.get(uid);if(cached!=null&&raw.equals(cached[0]))return (Bookmarks)cached[1];
        Bookmarks b;try{b=Bookmarks.decode(raw);}catch(RuntimeException e){FileLog.e("Tetogram bookmarks unreadable");return new Bookmarks();}
        CACHE.put(uid,new Object[]{raw,b});return b;
    }
    public interface Mutation{void apply(Bookmarks latest);}
    public static boolean update(int a,Mutation change){
        long uid=owner(a);if(uid<=0)return false;
        synchronized(LOCK){
            SharedPreferences p=prefs(uid);Bookmarks latest;
            try{latest=Bookmarks.decode(p.getString("items",""));}catch(RuntimeException e){return false;}
            change.apply(latest);
            if(uid!=owner(a))return false;
            return p.edit().putString("items",latest.encode()).commit();
        }
    }
    public static boolean contains(int a,MessageObject m){return m!=null&&load(a).contains(m.getDialogId(),m.getId());}
    /** Short text for the list: the text or caption, else what Telegram shows for the media. */
    public static String preview(MessageObject m){
        if(m==null||m.messageOwner==null)return "";
        String text=m.messageOwner.message;if(text!=null&&!text.trim().isEmpty())return text;
        CharSequence shown=m.messageText;return shown==null?"":shown.toString();
    }
    public static boolean toggle(int a,MessageObject m){
        if(m==null||m.getId()<=0)return false;long did=m.getDialogId();int mid=m.getId();boolean[] added={false};
        boolean ok=update(a,b->{if(b.contains(did,mid))b.remove(did,mid);else{b.add(did,mid,m.getFromChatId(),m.messageOwner.date,preview(m),System.currentTimeMillis());added[0]=true;}});
        return ok&&added[0];
    }
}
