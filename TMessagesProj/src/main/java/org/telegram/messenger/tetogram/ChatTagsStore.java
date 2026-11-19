/* Tetogram, 2026. GNU GPL v2 or later. Per-account chat tag storage, owner-fenced. */
package org.telegram.messenger.tetogram;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.ConcurrentHashMap;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

public final class ChatTagsStore {
    private ChatTagsStore(){}
    private static final Object LOCK=new Object();
    private static final ConcurrentHashMap<Long,Object[]> CACHE=new ConcurrentHashMap<>();
    private static long owner(int a){return UserConfig.getInstance(a).getClientUserId();}
    private static SharedPreferences prefs(long uid){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_tags_u"+uid,Context.MODE_PRIVATE);}
    /** Read-only view, cached until the stored text changes; unreadable data gives an empty set and is never overwritten. */
    public static ChatTags load(int a){
        long uid=owner(a);if(uid<=0)return new ChatTags();
        String raw=prefs(uid).getString("data","");Object[] cached=CACHE.get(uid);if(cached!=null&&raw.equals(cached[0]))return (ChatTags)cached[1];
        ChatTags t;try{t=ChatTags.decode(raw);}catch(RuntimeException e){FileLog.e("Tetogram tags unreadable");return new ChatTags();}
        CACHE.put(uid,new Object[]{raw,t});return t;
    }
    public interface Mutation{void apply(ChatTags latest);}
    /** Applies the change to the latest stored tags, saves and refreshes the chat list. */
    public static boolean update(int a,Mutation change){
        long uid=owner(a);if(uid<=0)return false;boolean ok;
        synchronized(LOCK){
            SharedPreferences p=prefs(uid);ChatTags latest;
            try{latest=ChatTags.decode(p.getString("data",""));}catch(RuntimeException e){return false;}
            try{change.apply(latest);}catch(RuntimeException e){FileLog.e("Tetogram tags change rejected");return false;}
            if(uid!=owner(a))return false;
            ok=p.edit().putString("data",latest.encode()).commit();
        }
        if(ok)NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces,0);
        return ok;
    }
}
