/* Tetogram, 2026. GNU GPL v2 or later. Shared pieces of Tetogram Music: track rows, playing, "add to playlist". */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.Playlists;
import org.telegram.messenger.tetogram.PlaylistsStore;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;

public final class MusicUi {
    private MusicUi(){}
    public static int color(int index){return Theme.getColor(Theme.keys_avatar_nameInMessage[Math.abs(index)%Theme.keys_avatar_nameInMessage.length]);}

    /** Square cover with the playlist colour and a note, or a custom icon. */
    public static View cover(Context ctx,int colorIndex,int icon,int sizeDp){
        ImageView v=new ImageView(ctx);v.setScaleType(ImageView.ScaleType.CENTER);v.setImageResource(icon);v.setColorFilter(0xffffffff);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{color(colorIndex),Theme.multAlpha(color(colorIndex),0.65f)});bg.setCornerRadius(dp(sizeDp/5f));v.setBackground(bg);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp),dp(sizeDp)));return v;
    }
    /** Title, performer · duration, optional marker on the right. */
    public static LinearLayout trackRow(Context ctx,Playlists.Track t,boolean playing,View.OnClickListener tap,View.OnClickListener more){
        LinearLayout row=new LinearLayout(ctx);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(6),dp(4),dp(6));row.setMinimumHeight(dp(58));
        View art=cover(ctx,(int)(Math.abs(t.dialog+t.message)%7),playing?R.drawable.ic_action_play:R.drawable.msg_filled_data_music,42);row.addView(art);
        LinearLayout texts=new LinearLayout(ctx);texts.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(12);row.addView(texts,tp);
        TextView title=new TextView(ctx);title.setText(t.title.isEmpty()?getString(R.string.AudioUnknownTitle):t.title);title.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);ThemeBind.textColor(title,playing?Theme.key_windowBackgroundWhiteBlueText:Theme.key_windowBackgroundWhiteBlackText);if(playing)title.setTypeface(AndroidUtilities.bold());texts.addView(title);
        TextView sub=new TextView(ctx);sub.setText((t.performer.isEmpty()?getString(R.string.AudioUnknownArtist):t.performer)+" · "+Playlists.duration(t.duration));sub.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);sub.setSingleLine(true);sub.setEllipsize(TextUtils.TruncateAt.END);ThemeBind.textColor(sub,Theme.key_windowBackgroundWhiteGrayText);texts.addView(sub);
        if(more!=null){ImageView dots=new ImageView(ctx);dots.setImageResource(R.drawable.ic_ab_other);dots.setScaleType(ImageView.ScaleType.CENTER);dots.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon));dots.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),1));dots.setOnClickListener(more);dots.setContentDescription(getString(R.string.AccDescrMoreOptions));row.addView(dots,new LinearLayout.LayoutParams(dp(44),dp(44)));}
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));row.setOnClickListener(tap);
        row.setContentDescription(title.getText()+", "+sub.getText());
        return row;
    }
    public static boolean isPlaying(Playlists.Track t){MessageObject m=org.telegram.messenger.MediaController.getInstance().getPlayingMessageObject();return m!=null&&m.getId()==t.message&&m.getDialogId()==t.dialog;}

    /** Loads the messages and plays {@code tracks} from {@code start}; reports unavailable ones. */
    public static void play(BaseFragment f,List<Playlists.Track> tracks,int start){
        if(tracks.isEmpty()||f.getParentActivity()==null)return;
        AlertDialog wait=new AlertDialog(f.getParentActivity(),AlertDialog.ALERT_TYPE_SPINNER);wait.setCanCancel(true);boolean[] cancelled={false},done={false};wait.setOnCancelListener(d->cancelled[0]=true);
        AndroidUtilities.runOnUIThread(()->{if(!cancelled[0]&&!done[0])f.showDialog(wait);},400);
        Playlists.Track first=tracks.get(Math.max(0,Math.min(start,tracks.size()-1)));
        PlaylistsStore.resolve(f.getCurrentAccount(),tracks,(available,missing)->{
            done[0]=true;try{wait.dismiss();}catch(Exception ignored){}
            if(cancelled[0]||f.getParentActivity()==null)return;
            if(available.isEmpty()){BulletinFactory.of(f).createSimpleBulletin(R.raw.error,getString(R.string.TetogramMusicNothingToPlay)).show();return;}
            int index=0;for(int i=0;i<available.size();i++){MessageObject m=available.get(i);if(m.getId()==first.message&&m.getDialogId()==first.dialog){index=i;break;}}
            PlaylistsStore.play(available,index);
            if(missing>0)BulletinFactory.of(f).createSimpleBulletin(R.raw.chats_infotip,formatString(R.string.TetogramMusicMissing,missing)).show();
        });
    }

    public interface Created{void created(int id);}
    public static void editPlaylist(BaseFragment f,Playlists.Playlist existing,Created done){
        Context ctx=f.getParentActivity();if(ctx==null)return;int a=f.getCurrentAccount();
        LinearLayout box=new LinearLayout(ctx);box.setOrientation(LinearLayout.VERTICAL);
        EditTextBoldCursor field=new EditTextBoldCursor(ctx);field.setTextSize(17);field.setSingleLine(true);field.setHint(getString(R.string.TetogramMusicName));field.setPadding(dp(24),dp(8),dp(24),dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);ThemeBind.hintTextColor(field,Theme.key_dialogTextHint);
        if(existing!=null)field.setText(existing.name);box.addView(field);
        LinearLayout colors=new LinearLayout(ctx);colors.setOrientation(LinearLayout.HORIZONTAL);colors.setGravity(Gravity.CENTER);colors.setPadding(dp(16),dp(12),dp(16),dp(4));
        int[] chosen={existing==null?(int)(System.currentTimeMillis()%Playlists.COLORS):existing.color};Dot[] dots=new Dot[Playlists.COLORS];
        for(int c=0;c<Playlists.COLORS;c++){final int color=c;Dot d=new Dot(ctx,c);d.selected=c==chosen[0];dots[c]=d;d.setOnClickListener(v->{chosen[0]=color;for(Dot x:dots){x.selected=x.color==color;x.invalidate();}});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(36),dp(36));lp.leftMargin=lp.rightMargin=dp(3);colors.addView(d,lp);}
        box.addView(colors);
        f.showDialog(new AlertDialog.Builder(ctx).setTitle(getString(existing==null?R.string.TetogramMusicNew:R.string.TetogramMusicEdit)).setView(box)
            .setPositiveButton(getString(R.string.Save),(d,n)->{String name=field.getText().toString();if(name.trim().isEmpty())return;int[] id={existing==null?0:existing.id};
                boolean ok=PlaylistsStore.update(a,p->{if(existing==null)id[0]=p.create(name,chosen[0],System.currentTimeMillis()).id;else p.rename(existing.id,name,chosen[0]);});
                if(ok&&done!=null)done.created(id[0]);})
            .setNegativeButton(getString(R.string.Cancel),null).create());
    }

    /** "Add to playlist": choose an existing playlist or create one. */
    public static void addToPlaylist(BaseFragment f,List<Playlists.Track> tracks){
        Context ctx=f.getParentActivity();if(ctx==null||tracks.isEmpty())return;int a=f.getCurrentAccount();
        Playlists lib=PlaylistsStore.load(a);ArrayList<Playlists.Playlist> lists=new ArrayList<>(lib.playlists);
        CharSequence[] names=new CharSequence[lists.size()+1];names[0]="＋ "+getString(R.string.TetogramMusicNew);
        for(int i=0;i<lists.size();i++){Playlists.Playlist p=lists.get(i);boolean has=tracks.size()==1&&p.contains(tracks.get(0).dialog,tracks.get(0).message);names[i+1]=(has?"✓ ":"")+p.name;}
        f.showDialog(new AlertDialog.Builder(ctx).setTitle(getString(R.string.TetogramMusicAddTo)).setItems(names,(d,n)->{
            if(n==0){editPlaylist(f,null,id->{if(id!=0)addAll(f,a,id,tracks);});return;}
            addAll(f,a,lists.get(n-1).id,tracks);
        }).create());
    }
    private static void addAll(BaseFragment f,int a,int id,List<Playlists.Track> tracks){
        int[] added={0};boolean ok=PlaylistsStore.update(a,p->{for(Playlists.Track t:tracks)if(p.add(id,t))added[0]++;});
        Playlists.Playlist p=PlaylistsStore.load(a).playlist(id);if(f.getParentActivity()==null)return;
        BulletinFactory.of(f).createSimpleBulletin(R.raw.saved_messages,!ok?getString(R.string.TetogramStorageError):added[0]==0?getString(R.string.TetogramMusicAlready):formatString(R.string.TetogramMusicAdded,p==null?"":p.name)).show();
    }

    static final class Dot extends View {
        final int color;boolean selected;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Dot(Context c,int color){super(c);this.color=color;}
        @Override protected void onDraw(Canvas canvas){float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-dp(4);paint.setStyle(Paint.Style.FILL);paint.setColor(MusicUi.color(color));canvas.drawCircle(cx,cy,selected?r-dp(3):r,paint);
            if(selected){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));canvas.drawCircle(cx,cy,r+dp(1),paint);}}
    }
}
