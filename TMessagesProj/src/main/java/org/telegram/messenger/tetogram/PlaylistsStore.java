/* Tetogram, 2026. GNU GPL v2 or later. Per-account playlist storage, owner-fenced, plus loading tracks for playback. */
package org.telegram.messenger.tetogram;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public final class PlaylistsStore {
    private PlaylistsStore(){}
    private static final Object LOCK=new Object();
    private static final ConcurrentHashMap<Long,Object[]> CACHE=new ConcurrentHashMap<>();
    private static long owner(int a){return UserConfig.getInstance(a).getClientUserId();}
    private static SharedPreferences prefs(long uid){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_music_u"+uid,Context.MODE_PRIVATE);}

    /** Read-only view; unreadable data gives an empty library and is never overwritten. */
    public static Playlists load(int a){
        long uid=owner(a);if(uid<=0)return new Playlists();
        String raw=prefs(uid).getString("data","");Object[] cached=CACHE.get(uid);if(cached!=null&&raw.equals(cached[0]))return (Playlists)cached[1];
        Playlists p;try{p=Playlists.decode(raw);}catch(RuntimeException e){FileLog.e("Tetogram playlists unreadable");return new Playlists();}
        CACHE.put(uid,new Object[]{raw,p});return p;
    }
    public interface Mutation{void apply(Playlists latest);}
    public static boolean update(int a,Mutation change){
        long uid=owner(a);if(uid<=0)return false;
        synchronized(LOCK){
            SharedPreferences p=prefs(uid);Playlists latest;
            try{latest=Playlists.decode(p.getString("data",""));}catch(RuntimeException e){return false;}
            try{change.apply(latest);}catch(RuntimeException e){FileLog.e("Tetogram playlist change rejected");return false;}
            if(uid!=owner(a))return false;
            return p.edit().putString("data",latest.encode()).commit();
        }
    }

    public static Playlists.Track track(MessageObject m){
        if(m==null||!m.isMusic()||m.getId()<=0)return null;
        try{return Playlists.track(m.getDialogId(),m.getId(),m.getMusicTitle(),m.getMusicAuthor(),(int)Math.round(m.getDuration()),System.currentTimeMillis());}catch(RuntimeException e){return null;}
    }
    /** Called by the player whenever a different music track starts. */
    public static void played(MessageObject m){
        Playlists.Track t=track(m);if(t==null)return;int a=m.currentAccount;
        org.telegram.messenger.Utilities.globalQueue.postRunnable(()->update(a,p->p.played(t)));
    }

    // ---- playback ----
    public interface Loaded{void loaded(List<MessageObject> available,int missing);}
    /** Fetches the messages behind the tracks (in order), up to 100 per request per chat. */
    public static void resolve(int a,List<Playlists.Track> tracks,Loaded done){
        LinkedHashMap<Long,ArrayList<Integer>> byDialog=new LinkedHashMap<>();
        for(Playlists.Track t:tracks){if(DialogObject.isEncryptedDialog(t.dialog))continue;ArrayList<Integer> ids=byDialog.get(t.dialog);if(ids==null){ids=new ArrayList<>();byDialog.put(t.dialog,ids);}ids.add(t.message);}
        HashMap<String,MessageObject> found=new HashMap<>();
        ArrayList<Runnable> requests=new ArrayList<>();int[] left={0};
        Runnable finish=()->{ArrayList<MessageObject> out=new ArrayList<>();int missing=0;for(Playlists.Track t:tracks){MessageObject m=found.get(t.key());if(m!=null&&m.isMusic())out.add(m);else missing++;}done.loaded(out,missing);};
        MessagesController mc=MessagesController.getInstance(a);
        for(java.util.Map.Entry<Long,ArrayList<Integer>> e:byDialog.entrySet()){
            long dialog=e.getKey();ArrayList<Integer> ids=e.getValue();
            for(int from=0;from<ids.size();from+=100){
                ArrayList<Integer> chunk=new ArrayList<>(ids.subList(from,Math.min(ids.size(),from+100)));
                TLObject req;
                TLRPC.Chat chat=dialog<0?mc.getChat(-dialog):null;
                if(chat!=null&&org.telegram.messenger.ChatObject.isChannel(chat)){TLRPC.TL_channels_getMessages r=new TLRPC.TL_channels_getMessages();r.channel=mc.getInputChannel(-dialog);r.id=chunk;req=r;}
                else{TLRPC.TL_messages_getMessages r=new TLRPC.TL_messages_getMessages();r.id=chunk;req=r;}
                left[0]++;
                requests.add(()->ConnectionsManager.getInstance(a).sendRequest(req,(response,error)->AndroidUtilities.runOnUIThread(()->{
                    if(response instanceof TLRPC.messages_Messages){TLRPC.messages_Messages res=(TLRPC.messages_Messages)response;mc.putUsers(res.users,false);mc.putChats(res.chats,false);
                        for(TLRPC.Message msg:res.messages){if(msg instanceof TLRPC.TL_messageEmpty)continue;MessageObject m=new MessageObject(a,msg,false,true);found.put(m.getDialogId()+":"+m.getId(),m);}}
                    if(--left[0]==0)finish.run();
                })));
            }
        }
        if(requests.isEmpty()){finish.run();return;}
        for(Runnable r:requests)r.run();
    }
    /** Plays tracks in the given order starting at {@code start} (index into the playable list). */
    public static boolean play(List<MessageObject> ordered,int start){
        if(ordered.isEmpty())return false;MessageObject first=ordered.get(Math.max(0,Math.min(start,ordered.size()-1)));
        MediaController.tetogramKeepOrder=true;
        return MediaController.getInstance().setPlaylist(new ArrayList<>(ordered),first,0,false,null);
    }
    public static void notifyChanged(int a){NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces,0);}
}
