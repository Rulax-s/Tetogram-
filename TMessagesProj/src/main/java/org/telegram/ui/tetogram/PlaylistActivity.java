/* Tetogram, 2026. GNU GPL v2 or later. One playlist (or an automatic list): play, shuffle, reorder, remove. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.Playlists;
import org.telegram.messenger.tetogram.PlaylistsStore;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class PlaylistActivity extends LabsPage implements NotificationCenter.NotificationCenterDelegate {
    public static final int RECENT=-1,MOST=-2,LIBRARY=-3;
    private static final int EDIT=1,DELETE=2;
    private final int id;
    private String query="";
    private EditTextBoldCursor search;
    private final Runnable searchRefresh=this::refresh;
    private int reorderSection=-1;
    private List<Playlists.Track> shown=new ArrayList<>();
    public PlaylistActivity(int account,int id){super(account);this.id=id;}

    @Override public boolean onFragmentCreate(){getNotificationCenter().addObserver(this,NotificationCenter.messagePlayingDidStart);getNotificationCenter().addObserver(this,NotificationCenter.messagePlayingDidReset);return super.onFragmentCreate();}
    @Override public void onFragmentDestroy(){getNotificationCenter().removeObserver(this,NotificationCenter.messagePlayingDidStart);getNotificationCenter().removeObserver(this,NotificationCenter.messagePlayingDidReset);AndroidUtilities.cancelRunOnUIThread(searchRefresh);super.onFragmentDestroy();}
    @Override public void didReceivedNotification(int notification,int account,Object... args){refresh();}

    private Playlists.Playlist playlist(){return id>0?PlaylistsStore.load(currentAccount).playlist(id):null;}
    private List<Playlists.Track> tracks(){
        Playlists lib=PlaylistsStore.load(currentAccount);
        if(id==RECENT)return lib.recent;if(id==MOST)return lib.mostPlayed(50);if(id==LIBRARY)return lib.allTracks();
        Playlists.Playlist p=lib.playlist(id);return p==null?new ArrayList<>():p.tracks;
    }
    @Override protected CharSequence title(){
        if(id==RECENT)return getString(R.string.TetogramMusicRecent);if(id==MOST)return getString(R.string.TetogramMusicMost);if(id==LIBRARY)return getString(R.string.TetogramMusicLibrary);
        Playlists.Playlist p=playlist();return p==null?getString(R.string.TetogramMusic):p.name;
    }
    @Override public View createView(Context context){
        search=new EditTextBoldCursor(context);search.setSingleLine(true);search.setTextSize(16);search.setHint(getString(R.string.TetogramMusicSearchHint));search.setPadding(dp(20),0,dp(20),0);
        ThemeBind.textColor(search,Theme.key_windowBackgroundWhiteBlackText);ThemeBind.hintTextColor(search,Theme.key_windowBackgroundWhiteHintText);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();AndroidUtilities.cancelRunOnUIThread(searchRefresh);AndroidUtilities.runOnUIThread(searchRefresh,200);}});
        View root=super.createView(context);
        if(id>0){list.listenReorder(this::reordered);list.allowReorder(true);}
        return root;
    }
    private void refresh(){if(list!=null)list.adapter.update(true);}

    private View header(List<Playlists.Track> all){
        Context ctx=getContext();LinearLayout box=new LinearLayout(ctx);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);box.setPadding(dp(16),dp(16),dp(16),dp(12));
        Playlists.Playlist p=playlist();int color=p!=null?p.color:id==RECENT?4:id==MOST?0:5;int icon=id==RECENT?R.drawable.msg_recent:id==MOST?R.drawable.msg_premium_liststar:R.drawable.msg_filled_data_music;
        View cover=MusicUi.cover(ctx,color,icon,112);box.addView(cover);
        TextView name=new TextView(ctx);name.setText(title());name.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);name.setTypeface(AndroidUtilities.bold());name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);ThemeBind.textColor(name,Theme.key_windowBackgroundWhiteBlackText);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-2,-2);np.topMargin=dp(12);box.addView(name,np);
        int seconds=0;for(Playlists.Track t:all)seconds+=t.duration;
        TextView sub=new TextView(ctx);sub.setText(all.isEmpty()?getString(R.string.TetogramMusicEmptyShort):formatString(R.string.TetogramMusicSummary,all.size(),Playlists.duration(seconds)));sub.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);ThemeBind.textColor(sub,Theme.key_windowBackgroundWhiteGrayText);box.addView(sub);
        if(!all.isEmpty()){
            LinearLayout buttons=new LinearLayout(ctx);buttons.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(44));bp.topMargin=dp(14);box.addView(buttons,bp);
            buttons.addView(button(ctx,getString(R.string.TetogramMusicPlay),true,()->MusicUi.play(this,all,0)),new LinearLayout.LayoutParams(0,-1,1));
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-1,1);sp.leftMargin=dp(10);
            buttons.addView(button(ctx,getString(R.string.TetogramMusicShuffle),false,()->MusicUi.play(this,Playlists.shuffled(all,null,System.nanoTime()),0)),sp);
        }
        return box;
    }
    private TextView button(Context ctx,CharSequence text,boolean primary,Runnable run){
        TextView b=new TextView(ctx);b.setText(text);b.setGravity(Gravity.CENTER);b.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);b.setTypeface(AndroidUtilities.bold());
        int accent=Theme.getColor(Theme.key_featuredStickers_addButton);
        b.setTextColor(primary?Theme.getColor(Theme.key_featuredStickers_buttonText):accent);
        b.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(12),primary?accent:Theme.multAlpha(accent,0.12f),Theme.multAlpha(primary?0xffffffff:accent,0.2f)));
        b.setOnClickListener(v->run.run());return b;
    }

    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        List<Playlists.Track> all=new ArrayList<>(tracks());
        items.add(UItem.asCustom(header(all),-2));
        if(all.isEmpty()){items.add(UItem.asShadow(getString(id==RECENT?R.string.TetogramMusicRecentEmpty:id==MOST?R.string.TetogramMusicMostEmpty:R.string.TetogramMusicEmpty)));}
        else{
            if(all.size()>12)items.add(UItem.asCustom(search,48));
            shown=Playlists.search(all,query);
            boolean reorderable=id>0&&query.trim().isEmpty();
            if(reorderable)reorderSection=adapter.reorderSectionStart();
            for(int i=0;i<shown.size();i++){final int index=i;Playlists.Track t=shown.get(i);
                UItem row=UItem.asCustom(MusicUi.trackRow(getContext(),t,MusicUi.isPlaying(t),v->MusicUi.play(this,shown,index),v->actions(t)),-2);row.object=t.key();items.add(row);}
            if(reorderable)adapter.reorderSectionEnd();
            if(shown.isEmpty())items.add(UItem.asShadow(getString(R.string.TetogramBookmarksNothingFound)));
            else items.add(UItem.asShadow(getString(reorderable?R.string.TetogramMusicDragInfo:R.string.TetogramMusicTapInfo)));
        }
        if(id>0){items.add(UItem.asButton(EDIT,R.drawable.msg_edit,getString(R.string.TetogramMusicEdit)));items.add(UItem.asButton(DELETE,R.drawable.msg_delete,getString(R.string.TetogramMusicDelete)).red());}
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id==EDIT){Playlists.Playlist p=playlist();if(p!=null)MusicUi.editPlaylist(this,p,x2->{simpleActionBar.setTitle(title());refresh();});}
        else if(item.id==DELETE){Playlists.Playlist p=playlist();if(p==null)return;
            showDialog(new AlertDialog.Builder(getContext()).setTitle(p.name).setMessage(getString(R.string.TetogramMusicDeleteText)).setPositiveButton(getString(R.string.Delete),(d,n)->{PlaylistsStore.update(currentAccount,l->l.delete(id));finishFragment();}).setNegativeButton(getString(R.string.Cancel),null).create());}
    }
    private void reordered(int section,ArrayList<UItem> items){
        if(section!=reorderSection)return;ArrayList<String> order=new ArrayList<>();for(UItem i:items)if(i.object instanceof String)order.add((String)i.object);
        PlaylistsStore.update(currentAccount,lib->{Playlists.Playlist p=lib.playlist(id);if(p==null)return;ArrayList<Playlists.Track> sorted=new ArrayList<>();
            for(String k:order)for(Playlists.Track t:p.tracks)if(t.key().equals(k)){sorted.add(t);break;}for(Playlists.Track t:p.tracks)if(!sorted.contains(t))sorted.add(t);p.tracks.clear();p.tracks.addAll(sorted);});
    }
    private void actions(Playlists.Track t){
        ArrayList<CharSequence> names=new ArrayList<>();ArrayList<Runnable> run=new ArrayList<>();
        names.add(getString(R.string.TetogramMusicAddTo));run.add(()->{ArrayList<Playlists.Track> one=new ArrayList<>();one.add(t);MusicUi.addToPlaylist(this,one);});
        names.add(getString(R.string.TetogramMusicOpenChat));run.add(()->open(t));
        if(id>0){names.add(getString(R.string.TetogramMusicRemove));run.add(()->{PlaylistsStore.update(currentAccount,l->l.remove(id,t.dialog,t.message));refresh();});}
        showDialog(new AlertDialog.Builder(getContext()).setTitle(t.title.isEmpty()?getString(R.string.AudioUnknownTitle):t.title).setItems(names.toArray(new CharSequence[0]),(d,n)->run.get(n).run()).create());
    }
    private void open(Playlists.Track t){
        Bundle args=new Bundle();long did=t.dialog;
        if(DialogObject.isEncryptedDialog(did))args.putInt("enc_id",DialogObject.getEncryptedChatId(did));else if(did>0)args.putLong("user_id",did);else args.putLong("chat_id",-did);
        args.putInt("message_id",t.message);presentFragment(new ChatActivity(args));
    }
}
