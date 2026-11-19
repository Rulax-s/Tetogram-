/* Tetogram, 2026. GNU GPL v2 or later. Local chat tags: manage tags, see chats with a tag, put tags on a chat. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.tetogram.ChatTags;
import org.telegram.messenger.tetogram.ChatTagsStore;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class TagsActivity extends LabsPage {
    private static final int ADD=1,TAG=1000,CHAT=100000;
    /** 0 = all tags, else the chats carrying this tag */
    private final int tagId;
    private final ArrayList<Integer> shownTags=new ArrayList<>();
    private final ArrayList<Long> shownChats=new ArrayList<>();
    public TagsActivity(int account){this(account,0);}
    public TagsActivity(int account,int tagId){super(account);this.tagId=tagId;}
    @Override protected CharSequence title(){if(tagId!=0){ChatTags.Tag t=ChatTagsStore.load(currentAccount).tag(tagId);if(t!=null)return t.name;}return getString(R.string.TetogramTags);}
    @Override public void onResume(){super.onResume();update();}
    private void update(){if(list!=null)list.adapter.update(true);}

    static String chatName(BaseFragment f,long did){
        if(DialogObject.isEncryptedDialog(did)){TLRPC.EncryptedChat e=f.getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(did));if(e!=null)did=e.user_id;}
        if(did>0){TLRPC.User u=f.getMessagesController().getUser(did);if(u!=null)return UserObject.isUserSelf(u)?getString(R.string.SavedMessages):UserObject.getUserName(u);}
        else{TLRPC.Chat c=f.getMessagesController().getChat(-did);if(c!=null)return c.title;}
        return "ID "+did;
    }

    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        ChatTags all=ChatTagsStore.load(currentAccount);
        if(tagId!=0){
            shownChats.clear();shownChats.addAll(all.chatsWith(tagId));
            if(shownChats.isEmpty()){items.add(UItem.asShadow(getString(R.string.TetogramTagsNoChats)));return;}
            for(int i=0;i<shownChats.size();i++)items.add(UItem.asButton(CHAT+i,chatName(this,shownChats.get(i))));
            items.add(UItem.asShadow(getString(R.string.TetogramTagsChatsInfo)));
            return;
        }
        shownTags.clear();Map<Integer,Integer> counts=all.counts();
        if(!all.tags.isEmpty())items.add(UItem.asHeader(getString(R.string.TetogramTags)));
        for(int i=0;i<all.tags.size();i++){ChatTags.Tag t=all.tags.get(i);shownTags.add(t.id);Integer n=counts.get(t.id);
            UItem row=UItem.asButton(TAG+i,new DotDrawable(t.color),t.name);row.textValue=formatString(R.string.TetogramLabsCount,n==null?0:n);items.add(row);}
        if(all.tags.size()<ChatTags.MAX_TAGS)items.add(UItem.asButton(ADD,R.drawable.msg_add,getString(R.string.TetogramTagsNew)));
        items.add(UItem.asShadow(getString(R.string.TetogramTagsInfo)));
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id==ADD)edit(this,currentAccount,null,id->update());
        else if(item.id>=CHAT&&item.id-CHAT<shownChats.size()){long did=shownChats.get(item.id-CHAT);
            showDialog(new AlertDialog.Builder(getContext()).setTitle(chatName(this,did)).setItems(new CharSequence[]{getString(R.string.TetogramBookmarksOpen),getString(R.string.TetogramTagsRemoveFromChat)},(d,n)->{if(n==0)open(did);else{ChatTagsStore.update(currentAccount,c->c.set(did,tagId,false));update();}}).create());}
        else if(item.id>=TAG&&item.id-TAG<shownTags.size()){int id=shownTags.get(item.id-TAG);ChatTags.Tag t=ChatTagsStore.load(currentAccount).tag(id);if(t==null)return;
            CharSequence[] names={getString(R.string.TetogramTagsShowChats),getString(R.string.TetogramTagsEdit),getString(R.string.TetogramTagsUp),getString(R.string.Delete)};
            showDialog(new AlertDialog.Builder(getContext()).setTitle(t.name).setItems(names,(d,n)->{
                if(n==0)presentFragment(new TagsActivity(currentAccount,id));
                else if(n==1)edit(this,currentAccount,t,x2->update());
                else if(n==2){int index=item.id-TAG;ChatTagsStore.update(currentAccount,c->c.move(index,Math.max(0,index-1)));update();}
                else{ChatTagsStore.update(currentAccount,c->c.delete(id));update();}
            }).create());}
    }
    private void open(long did){
        Bundle args=new Bundle();
        if(DialogObject.isEncryptedDialog(did))args.putInt("enc_id",DialogObject.getEncryptedChatId(did));else if(did>0)args.putLong("user_id",did);else args.putLong("chat_id",-did);
        presentFragment(new ChatActivity(args));
    }

    public interface Saved{void saved(int id);}
    /** Create (tag == null) or edit a tag: name and one of the theme's seven name colours. */
    public static void edit(BaseFragment f,int account,ChatTags.Tag tag,Saved done){
        Context ctx=f.getParentActivity();if(ctx==null)return;
        LinearLayout box=new LinearLayout(ctx);box.setOrientation(LinearLayout.VERTICAL);
        EditTextBoldCursor field=new EditTextBoldCursor(ctx);field.setTextSize(17);field.setSingleLine(true);field.setHint(getString(R.string.TetogramTagsName));field.setPadding(dp(24),dp(8),dp(24),dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);ThemeBind.hintTextColor(field,Theme.key_dialogTextHint);
        if(tag!=null)field.setText(tag.name);
        box.addView(field,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout colors=new LinearLayout(ctx);colors.setOrientation(LinearLayout.HORIZONTAL);colors.setGravity(Gravity.CENTER);colors.setPadding(dp(16),dp(12),dp(16),dp(4));
        int[] chosen={tag==null?(int)(System.currentTimeMillis()%ChatTags.COLORS):tag.color};
        TagDot[] dots=new TagDot[ChatTags.COLORS];
        for(int c=0;c<ChatTags.COLORS;c++){final int color=c;TagDot dot=new TagDot(ctx,c);dot.selected=c==chosen[0];dots[c]=dot;
            dot.setOnClickListener(v->{chosen[0]=color;for(TagDot d:dots){d.selected=d.color==color;d.invalidate();}});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(36),dp(36));lp.leftMargin=lp.rightMargin=dp(3);colors.addView(dot,lp);}
        box.addView(colors,new LinearLayout.LayoutParams(-1,-2));
        f.showDialog(new AlertDialog.Builder(ctx).setTitle(getString(tag==null?R.string.TetogramTagsNew:R.string.TetogramTagsEdit)).setView(box)
            .setPositiveButton(getString(R.string.Save),(d,n)->{String name=field.getText().toString();if(name.trim().isEmpty())return;int[] id={tag==null?0:tag.id};
                boolean ok=ChatTagsStore.update(account,c->{if(tag==null)id[0]=c.add(name,chosen[0]).id;else c.rename(tag.id,name,chosen[0]);});
                if(ok)done.saved(id[0]);})
            .setNegativeButton(getString(R.string.Cancel),null).create());
    }

    /** Chat header ⋮ → Tags: tick the tags of this chat. */
    public static void chatMenu(BaseFragment f,int account,long dialog){
        Context ctx=f.getParentActivity();if(ctx==null)return;
        ChatTags all=ChatTagsStore.load(account);
        if(all.tags.isEmpty()){edit(f,account,null,id->{if(id!=0)ChatTagsStore.update(account,c->c.set(dialog,id,true));});return;}
        LinearLayout box=new LinearLayout(ctx);box.setOrientation(LinearLayout.VERTICAL);
        List<ChatTags.Tag> tags=new ArrayList<>(all.tags);
        for(ChatTags.Tag t:tags){CheckBoxCell cell=new CheckBoxCell(ctx,1);cell.setBackground(Theme.getSelectorDrawable(false));boolean on=all.has(dialog,t.id);cell.setText(t.name,"",on,false);cell.setTag(on);
            cell.setOnClickListener(v->{boolean now=!(Boolean)cell.getTag();cell.setTag(now);cell.setChecked(now,true);ChatTagsStore.update(account,c->c.set(dialog,t.id,now));});
            box.addView(cell,new LinearLayout.LayoutParams(-1,dp(50)));}
        AlertDialog[] dialog1=new AlertDialog[1];
        dialog1[0]=new AlertDialog.Builder(ctx).setTitle(getString(R.string.TetogramTags)).setView(box)
            .setPositiveButton(getString(R.string.Done),null)
            .setNeutralButton(getString(R.string.TetogramTagsNew),(d,n)->edit(f,account,null,id->{if(id!=0)ChatTagsStore.update(account,c->c.set(dialog,id,true));}))
            .create();
        f.showDialog(dialog1[0]);
    }

    static final class DotDrawable extends android.graphics.drawable.Drawable {
        private final int color;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        DotDrawable(int color){this.color=color;}
        @Override public void draw(Canvas canvas){android.graphics.Rect b=getBounds();paint.setColor(Theme.getColor(Theme.keys_avatar_nameInMessage[color%Theme.keys_avatar_nameInMessage.length]));canvas.drawCircle(b.exactCenterX(),b.exactCenterY(),dp(8),paint);}
        @Override public int getIntrinsicWidth(){return dp(24);}
        @Override public int getIntrinsicHeight(){return dp(24);}
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){}
        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
    /** Small colour circle used as colour picker. */
    static final class TagDot extends View {
        final int color;boolean selected;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TagDot(Context c,int color){super(c);this.color=color;}
        @Override protected void onDraw(Canvas canvas){
            float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-dp(4);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Theme.getColor(Theme.keys_avatar_nameInMessage[color%Theme.keys_avatar_nameInMessage.length]));canvas.drawCircle(cx,cy,selected?r-dp(3):r,paint);
            if(selected){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));canvas.drawCircle(cx,cy,r+dp(1),paint);}
        }
    }
}
