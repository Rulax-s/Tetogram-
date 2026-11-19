/* Tetogram, 2026. GNU GPL v2 or later. Read-only history scan for local statistics; nothing is stored or sent. */
package org.telegram.ui.tetogram;

import java.util.TimeZone;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.tetogram.ChatStats;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;

final class ChatStatsLoader {
    interface Listener {
        void progress(int loaded,int total,int waitSeconds);
        void done(ChatStats.Result result,boolean complete,String error);
    }
    /** Server-side totals, -1 when unavailable. Indexes follow {@link #COUNTER_FILTERS}. */
    interface CountersListener{void counters(int[] values);}
    static final int COUNTER_PHOTOS=0,COUNTER_VIDEOS=1,COUNTER_VOICE=2,COUNTER_ROUND=3,COUNTER_LINKS=4,COUNTER_FILES=5,COUNTER_MUSIC=6,COUNTER_GIFS=7,COUNTER_COUNT=8;

    private static final int PAGE=100;
    private final int account;
    private final long dialog,self;
    private final int limit;
    private final Listener listener;
    private final ChatStats stats=new ChatStats(TimeZone.getDefault());
    private volatile boolean cancelled,stopRequested;
    private int request=-1,loaded,total,offsetId;

    ChatStatsLoader(int account,long dialog,int limit,Listener listener){
        this.account=account;this.dialog=dialog;this.limit=limit<=0?Integer.MAX_VALUE:limit;this.listener=listener;
        self=UserConfig.getInstance(account).getClientUserId();
    }

    void start(){Utilities.globalQueue.postRunnable(this::next);}
    void cancel(){
        if(cancelled)return;
        cancelled=true;
        if(request>=0)ConnectionsManager.getInstance(account).cancelRequest(request,true);
    }

    /** Stops after the current page and reports the statistics collected so far. */
    void stopEarly(){
        Utilities.globalQueue.postRunnable(()->{
            if(cancelled||stopRequested)return;
            stopRequested=true;
            if(request>=0){ConnectionsManager.getInstance(account).cancelRequest(request,true);request=-1;}
            finish(false,null);
        });
    }

    private void next(){
        if(cancelled||stopRequested)return;
        TLRPC.InputPeer peer=MessagesController.getInstance(account).getInputPeer(dialog);
        if(peer==null||peer instanceof TLRPC.TL_inputPeerEmpty){finish(false,"PEER_INVALID");return;}
        TLRPC.TL_messages_getHistory req=new TLRPC.TL_messages_getHistory();
        req.peer=peer;req.offset_id=offsetId;req.limit=Math.min(PAGE,limit-loaded);
        request=ConnectionsManager.getInstance(account).sendRequest(req,(response,error)->Utilities.globalQueue.postRunnable(()->page(response,error)));
    }

    private void page(Object response,TLRPC.TL_error error){
        request=-1;
        if(cancelled||stopRequested)return;
        if(error!=null){
            int wait=floodWait(error.text);
            if(wait>0&&wait<=120){
                AndroidUtilities.runOnUIThread(()->{if(!cancelled)listener.progress(loaded,total,wait);});
                Utilities.globalQueue.postRunnable(this::next,wait*1000L+300);
                return;
            }
            finish(false,error.text);return;
        }
        if(!(response instanceof TLRPC.messages_Messages)){finish(false,"UNEXPECTED_RESPONSE");return;}
        TLRPC.messages_Messages res=(TLRPC.messages_Messages)response;
        AndroidUtilities.runOnUIThread(()->{MessagesController mc=MessagesController.getInstance(account);mc.putUsers(res.users,false);mc.putChats(res.chats,false);});
        int minId=Integer.MAX_VALUE;
        for(TLRPC.Message m:res.messages){
            if(m==null)continue;
            if(m.id>0&&m.id<minId)minId=m.id;
            if(m instanceof TLRPC.TL_messageEmpty||m instanceof TLRPC.TL_messageService)continue;
            stats.add(sender(m),m.date,m.message,media(m),duration(m),m.fwd_from!=null);
        }
        loaded+=res.messages.size();
        total=Math.max(res.count,loaded);
        boolean all=res instanceof TLRPC.TL_messages_messages;
        boolean end=res.messages.isEmpty()||minId==Integer.MAX_VALUE||minId<=1||all||minId==offsetId;
        offsetId=minId;
        if(end){finish(true,null);return;}
        if(loaded>=limit){finish(false,null);return;}
        int l=loaded,t=total;
        AndroidUtilities.runOnUIThread(()->{if(!cancelled)listener.progress(l,t,0);});
        Utilities.globalQueue.postRunnable(this::next,40);
    }

    private void finish(boolean complete,String error){
        if(cancelled)return;
        ChatStats.Result result=stats.finish(ConnectionsManager.getInstance(account).getCurrentTime());
        AndroidUtilities.runOnUIThread(()->{if(!cancelled)listener.done(result,complete,error);});
    }

    private long sender(TLRPC.Message m){
        if(m.out)return self;
        if(m.from_id!=null){long id=MessageObject.getPeerId(m.from_id);if(id!=0)return id;}
        return dialog;
    }

    static int media(TLRPC.Message m){
        TLRPC.MessageMedia media=m.media;
        if(media==null||media instanceof TLRPC.TL_messageMediaEmpty)return ChatStats.MEDIA_NONE;
        if(media instanceof TLRPC.TL_messageMediaPhoto)return ChatStats.MEDIA_PHOTO;
        if(media instanceof TLRPC.TL_messageMediaWebPage)return ChatStats.MEDIA_LINK;
        if(media instanceof TLRPC.TL_messageMediaPoll)return ChatStats.MEDIA_POLL;
        if(media instanceof TLRPC.TL_messageMediaGeo||media instanceof TLRPC.TL_messageMediaGeoLive||media instanceof TLRPC.TL_messageMediaVenue)return ChatStats.MEDIA_LOCATION;
        if(media instanceof TLRPC.TL_messageMediaDocument){
            if(MessageObject.isVoiceMessage(m))return ChatStats.MEDIA_VOICE;
            if(MessageObject.isRoundVideoMessage(m))return ChatStats.MEDIA_ROUND;
            if(MessageObject.isStickerMessage(m)||MessageObject.isAnimatedStickerMessage(m))return ChatStats.MEDIA_STICKER;
            if(MessageObject.isGifMessage(m))return ChatStats.MEDIA_GIF;
            if(MessageObject.isMusicMessage(m))return ChatStats.MEDIA_MUSIC;
            if(MessageObject.isVideoMessage(m))return ChatStats.MEDIA_VIDEO;
            return ChatStats.MEDIA_FILE;
        }
        return ChatStats.MEDIA_OTHER;
    }

    private static int duration(TLRPC.Message m){
        if(m.media==null||m.media.document==null)return 0;
        try{return (int)Math.round(MessageObject.getDocumentDuration(m.media.document));}catch(Throwable ignore){return 0;}
    }

    static int floodWait(String text){
        if(text==null||!text.startsWith("FLOOD_WAIT_"))return 0;
        try{return Integer.parseInt(text.substring(11));}catch(NumberFormatException e){return 0;}
    }

    interface TotalListener{void total(int count);}
    /** One-message request that only reads the server's total message count. */
    static int requestTotal(int account,long dialog,TotalListener listener){
        TLRPC.InputPeer peer=MessagesController.getInstance(account).getInputPeer(dialog);
        if(peer==null||peer instanceof TLRPC.TL_inputPeerEmpty)return -1;
        TLRPC.TL_messages_getHistory req=new TLRPC.TL_messages_getHistory();
        req.peer=peer;req.limit=1;
        return ConnectionsManager.getInstance(account).sendRequest(req,(response,error)->{
            if(!(response instanceof TLRPC.messages_Messages))return;
            TLRPC.messages_Messages res=(TLRPC.messages_Messages)response;
            int count=res instanceof TLRPC.TL_messages_messages?res.messages.size():res.count;
            AndroidUtilities.runOnUIThread(()->listener.total(count));
        });
    }

    static int requestCounters(int account,long dialog,CountersListener listener){
        TLRPC.InputPeer peer=MessagesController.getInstance(account).getInputPeer(dialog);
        if(peer==null||peer instanceof TLRPC.TL_inputPeerEmpty)return -1;
        TLRPC.TL_messages_getSearchCounters req=new TLRPC.TL_messages_getSearchCounters();
        req.peer=peer;
        req.filters.add(new TLRPC.TL_inputMessagesFilterPhotos());
        req.filters.add(new TLRPC.TL_inputMessagesFilterVideo());
        req.filters.add(new TLRPC.TL_inputMessagesFilterVoice());
        req.filters.add(new TLRPC.TL_inputMessagesFilterRoundVideo());
        req.filters.add(new TLRPC.TL_inputMessagesFilterUrl());
        req.filters.add(new TLRPC.TL_inputMessagesFilterDocument());
        req.filters.add(new TLRPC.TL_inputMessagesFilterMusic());
        req.filters.add(new TLRPC.TL_inputMessagesFilterGif());
        return ConnectionsManager.getInstance(account).sendRequest(req,(response,error)->{
            int[] values=new int[COUNTER_COUNT];java.util.Arrays.fill(values,-1);
            if(response instanceof Vector){
                Vector vector=(Vector)response;
                for(Object o:vector.objects){
                    if(!(o instanceof TLRPC.TL_messages_searchCounter))continue;
                    TLRPC.TL_messages_searchCounter c=(TLRPC.TL_messages_searchCounter)o;
                    int index=c.filter instanceof TLRPC.TL_inputMessagesFilterPhotos?COUNTER_PHOTOS:c.filter instanceof TLRPC.TL_inputMessagesFilterVideo?COUNTER_VIDEOS:c.filter instanceof TLRPC.TL_inputMessagesFilterVoice?COUNTER_VOICE:c.filter instanceof TLRPC.TL_inputMessagesFilterRoundVideo?COUNTER_ROUND:c.filter instanceof TLRPC.TL_inputMessagesFilterUrl?COUNTER_LINKS:c.filter instanceof TLRPC.TL_inputMessagesFilterDocument?COUNTER_FILES:c.filter instanceof TLRPC.TL_inputMessagesFilterMusic?COUNTER_MUSIC:c.filter instanceof TLRPC.TL_inputMessagesFilterGif?COUNTER_GIFS:-1;
                    if(index>=0)values[index]=c.count;
                }
            }
            AndroidUtilities.runOnUIThread(()->listener.counters(values));
        });
    }
}
