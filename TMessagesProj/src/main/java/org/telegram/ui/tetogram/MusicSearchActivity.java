/* Tetogram, 2026. GNU GPL v2 or later. All music from the user's Telegram chats and channels, with search. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import java.util.ArrayList;
import java.util.HashSet;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.Playlists;
import org.telegram.messenger.tetogram.PlaylistsStore;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class MusicSearchActivity extends LabsPage implements NotificationCenter.NotificationCenterDelegate {
    private static final int MORE=1;
    private EditTextBoldCursor search;
    private String query="";
    private final ArrayList<MessageObject> results=new ArrayList<>();
    private final HashSet<String> seen=new HashSet<>();
    private int nextRate,reqId,generation;private boolean loading,endReached;
    private final Runnable searchRun=()->load(true);
    public MusicSearchActivity(int account){super(account);}
    @Override protected CharSequence title(){return getString(R.string.TetogramMusicAll);}
    @Override public boolean onFragmentCreate(){getNotificationCenter().addObserver(this,NotificationCenter.messagePlayingDidStart);return super.onFragmentCreate();}
    @Override public void onFragmentDestroy(){getNotificationCenter().removeObserver(this,NotificationCenter.messagePlayingDidStart);AndroidUtilities.cancelRunOnUIThread(searchRun);if(reqId!=0)getConnectionsManager().cancelRequest(reqId,true);super.onFragmentDestroy();}
    @Override public void didReceivedNotification(int notification,int account,Object... args){refresh();}
    @Override public View createView(Context context){
        search=new EditTextBoldCursor(context);search.setSingleLine(true);search.setTextSize(16);search.setHint(getString(R.string.TetogramMusicSearchAll));search.setPadding(dp(20),0,dp(20),0);
        ThemeBind.textColor(search,Theme.key_windowBackgroundWhiteBlackText);ThemeBind.hintTextColor(search,Theme.key_windowBackgroundWhiteHintText);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString().trim();AndroidUtilities.cancelRunOnUIThread(searchRun);AndroidUtilities.runOnUIThread(searchRun,400);}});
        View root=super.createView(context);load(true);return root;
    }
    private void refresh(){if(list!=null)list.adapter.update(true);}

    private void load(boolean reset){
        if(reset){if(reqId!=0)getConnectionsManager().cancelRequest(reqId,true);reqId=0;results.clear();seen.clear();nextRate=0;endReached=false;loading=false;generation++;}
        if(loading||endReached)return;loading=true;refresh();
        TLRPC.TL_messages_searchGlobal req=new TLRPC.TL_messages_searchGlobal();req.q=query;req.filter=new TLRPC.TL_inputMessagesFilterMusic();req.limit=50;
        if(results.isEmpty()){req.offset_peer=new TLRPC.TL_inputPeerEmpty();}
        else{MessageObject last=results.get(results.size()-1);req.offset_id=last.getId();req.offset_rate=nextRate;req.offset_peer=getMessagesController().getInputPeer(MessageObject.getPeerId(last.messageOwner.peer_id));}
        int gen=generation;
        reqId=getConnectionsManager().sendRequestTyped(req,AndroidUtilities::runOnUIThread,(res,error)->{
            if(gen!=generation)return;reqId=0;loading=false;
            if(res!=null){getMessagesController().putUsers(res.users,false);getMessagesController().putChats(res.chats,false);
                int before=results.size();
                for(TLRPC.Message msg:res.messages){MessageObject m=new MessageObject(currentAccount,msg,false,true);if(m.isMusic()&&seen.add(m.getDialogId()+":"+m.getId()))results.add(m);}
                nextRate=res.next_rate;endReached=res.messages.isEmpty()||results.size()==before||!(res instanceof TLRPC.TL_messages_messagesSlice);
            }else endReached=true;
            refresh();
        },ConnectionsManager.RequestFlagFailOnServerErrors);
    }
    private ArrayList<Playlists.Track> tracks(){ArrayList<Playlists.Track> out=new ArrayList<>();for(MessageObject m:results){Playlists.Track t=PlaylistsStore.track(m);if(t!=null)out.add(t);}return out;}

    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        items.add(UItem.asCustom(search,48));
        ArrayList<Playlists.Track> tracks=tracks();
        for(int i=0;i<results.size();i++){final MessageObject m=results.get(i);Playlists.Track t=PlaylistsStore.track(m);if(t==null)continue;final int index=i;
            items.add(UItem.asCustom(MusicUi.trackRow(getContext(),t,MusicUi.isPlaying(t),v->playFrom(index),v->{ArrayList<Playlists.Track> one=new ArrayList<>();one.add(t);MusicUi.addToPlaylist(this,one);}),-2));}
        if(loading)items.add(UItem.asShadow(getString(R.string.Loading)));
        else if(results.isEmpty())items.add(UItem.asShadow(getString(query.isEmpty()?R.string.TetogramMusicAllEmpty:R.string.TetogramBookmarksNothingFound)));
        else if(!endReached)items.add(UItem.asButton(MORE,getString(R.string.TetogramMusicLoadMore)));
        if(!tracks.isEmpty())items.add(UItem.asShadow(getString(R.string.TetogramMusicAllTip)));
    }
    private void playFrom(int index){
        ArrayList<MessageObject> playable=new ArrayList<>(results);if(playable.isEmpty())return;
        PlaylistsStore.play(playable,index);
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){if(item.id==MORE)load(false);}
}
