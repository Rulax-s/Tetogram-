/* Tetogram, 2026. GNU GPL v2 or later. Tetogram Music: playlists, automatic lists and all music from Telegram. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.Playlists;
import org.telegram.messenger.tetogram.PlaylistsStore;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class MusicActivity extends LabsPage {
    private static final int SEARCH=1,NEW=2;
    public MusicActivity(int account){super(account);}
    @Override protected CharSequence title(){return getString(R.string.TetogramMusic);}

    private View card(int colorIndex,int icon,CharSequence name,CharSequence details,Runnable open){
        Context ctx=getContext();LinearLayout row=new LinearLayout(ctx);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(8),dp(16),dp(8));row.setMinimumHeight(dp(72));
        row.addView(MusicUi.cover(ctx,colorIndex,icon,56));
        LinearLayout texts=new LinearLayout(ctx);texts.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(14);row.addView(texts,tp);
        TextView title=new TextView(ctx);title.setText(name);title.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);title.setTypeface(AndroidUtilities.bold());title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);ThemeBind.textColor(title,Theme.key_windowBackgroundWhiteBlackText);texts.addView(title);
        TextView sub=new TextView(ctx);sub.setText(details);sub.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);sub.setSingleLine(true);ThemeBind.textColor(sub,Theme.key_windowBackgroundWhiteGrayText);texts.addView(sub);
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));row.setOnClickListener(v->open.run());row.setContentDescription(name+", "+details);
        return row;
    }
    static CharSequence summary(Playlists.Playlist p){return p.tracks.isEmpty()?getString(R.string.TetogramMusicEmptyShort):formatString(R.string.TetogramMusicSummary,p.tracks.size(),Playlists.duration(p.totalSeconds()));}

    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        Playlists lib=PlaylistsStore.load(currentAccount);
        items.add(UItem.asButton(SEARCH,R.drawable.msg_search,getString(R.string.TetogramMusicAll)));
        items.add(UItem.asShadow(getString(R.string.TetogramMusicAllInfo)));
        items.add(UItem.asHeader(getString(R.string.TetogramMusicForYou)));
        items.add(UItem.asCustom(card(4,R.drawable.msg_recent,getString(R.string.TetogramMusicRecent),lib.recent.isEmpty()?getString(R.string.TetogramMusicEmptyShort):formatString(R.string.TetogramMusicCount,lib.recent.size()),()->presentFragment(new PlaylistActivity(currentAccount,PlaylistActivity.RECENT))),-2));
        int most=lib.mostPlayed(50).size();
        items.add(UItem.asCustom(card(0,R.drawable.msg_premium_liststar,getString(R.string.TetogramMusicMost),most==0?getString(R.string.TetogramMusicMostEmpty):formatString(R.string.TetogramMusicCount,most),()->presentFragment(new PlaylistActivity(currentAccount,PlaylistActivity.MOST))),-2));
        int all=lib.allTracks().size();
        if(all>0)items.add(UItem.asCustom(card(5,R.drawable.msg_filled_data_music,getString(R.string.TetogramMusicLibrary),formatString(R.string.TetogramMusicCount,all),()->presentFragment(new PlaylistActivity(currentAccount,PlaylistActivity.LIBRARY))),-2));
        items.add(UItem.asHeader(getString(R.string.TetogramMusicPlaylists)));
        for(Playlists.Playlist p:lib.playlists){final int id=p.id;items.add(UItem.asCustom(card(p.color,R.drawable.msg_filled_data_music,p.name,summary(p),()->presentFragment(new PlaylistActivity(currentAccount,id))),-2));}
        if(lib.playlists.size()<Playlists.MAX_PLAYLISTS)items.add(UItem.asButton(NEW,R.drawable.msg_add,getString(R.string.TetogramMusicNew)));
        items.add(UItem.asShadow(getString(R.string.TetogramMusicInfo)));
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id==SEARCH)presentFragment(new MusicSearchActivity(currentAccount));
        else if(item.id==NEW)MusicUi.editPlaylist(this,null,id->{if(list!=null)list.adapter.update(true);if(id!=0)presentFragment(new PlaylistActivity(currentAccount,id));});
    }
}
