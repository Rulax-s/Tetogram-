/* Tetogram, 2026. GNU GPL v2 or later. Local bookmarks: all chats or one chat, with search and notes. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
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
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.tetogram.Bookmarks;
import org.telegram.messenger.tetogram.BookmarksStore;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class BookmarksActivity extends LabsPage {
    private final long dialog;
    private String query="";
    private EditTextBoldCursor search;
    private final Runnable searchRefresh=this::refresh;
    /** @param dialog one chat, or 0 for every chat */
    public BookmarksActivity(int account,long dialog){super(account);this.dialog=dialog;}
    @Override protected CharSequence title(){return getString(dialog==0?R.string.TetogramBookmarks:R.string.TetogramBookmarksChat);}
    @Override public View createView(Context context){
        search=new EditTextBoldCursor(context);search.setSingleLine(true);search.setTextSize(16);search.setHint(getString(R.string.TetogramBookmarksSearch));search.setPadding(dp(20),0,dp(20),0);
        ThemeBind.textColor(search,Theme.key_windowBackgroundWhiteBlackText);ThemeBind.hintTextColor(search,Theme.key_windowBackgroundWhiteHintText);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();AndroidUtilities.cancelRunOnUIThread(searchRefresh);AndroidUtilities.runOnUIThread(searchRefresh,200);}});
        View root=super.createView(context);
        if(dialog!=0)simpleActionBar.setSubtitle(chatName(dialog));
        return root;
    }
    @Override public void onFragmentDestroy(){AndroidUtilities.cancelRunOnUIThread(searchRefresh);super.onFragmentDestroy();}
    private void refresh(){if(list!=null)list.adapter.update(true);}

    private String chatName(long did){
        if(DialogObject.isEncryptedDialog(did)){TLRPC.EncryptedChat e=getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(did));if(e!=null)did=e.user_id;}
        if(did>0){TLRPC.User u=getMessagesController().getUser(did);if(u!=null)return UserObject.isUserSelf(u)?getString(R.string.SavedMessages):UserObject.getUserName(u);}
        else{TLRPC.Chat c=getMessagesController().getChat(-did);if(c!=null)return c.title;}
        return "ID "+did;
    }

    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        Bookmarks all=BookmarksStore.load(currentAccount);
        int total=dialog==0?all.items.size():all.forDialog(dialog).size();
        if(total>0)items.add(UItem.asCustom(search,48));
        ArrayList<Bookmarks.Item> shown=all.search(query,dialog);
        if(total==0){items.add(UItem.asShadow(getString(R.string.TetogramBookmarksEmpty)));return;}
        if(shown.isEmpty()){items.add(UItem.asShadow(getString(R.string.TetogramBookmarksNothingFound)));return;}
        for(Bookmarks.Item item:shown)items.add(UItem.asCustom(row(item),-2));
        items.add(UItem.asShadow(getString(R.string.TetogramBookmarksInfo)));
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){}

    private View row(Bookmarks.Item item){
        Context ctx=getContext();
        LinearLayout row=new LinearLayout(ctx);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(20),dp(10),dp(20),dp(10));row.setMinimumHeight(dp(56));
        LinearLayout top=new LinearLayout(ctx);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView where=new TextView(ctx);where.setText(dialog==0?chatName(item.dialog):sender(item));where.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);where.setTypeface(AndroidUtilities.bold());where.setSingleLine(true);where.setEllipsize(TextUtils.TruncateAt.END);ThemeBind.textColor(where,Theme.key_windowBackgroundWhiteBlueText);top.addView(where,new LinearLayout.LayoutParams(0,-2,1));
        TextView date=new TextView(ctx);date.setText(item.date>0?LocaleController.formatDateChat(item.date):"");date.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);ThemeBind.textColor(date,Theme.key_windowBackgroundWhiteGrayText);LinearLayout.LayoutParams dateParams=new LinearLayout.LayoutParams(-2,-2);dateParams.leftMargin=dp(8);top.addView(date,dateParams);
        row.addView(top);
        TextView text=new TextView(ctx);text.setText(item.preview.isEmpty()?getString(R.string.TetogramBookmarksNoText):item.preview);text.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);text.setMaxLines(3);text.setEllipsize(TextUtils.TruncateAt.END);ThemeBind.textColor(text,Theme.key_windowBackgroundWhiteBlackText);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(3);row.addView(text,tp);
        if(!item.note.isEmpty()){TextView note=new TextView(ctx);note.setText(item.note);note.setTypeface(null,android.graphics.Typeface.ITALIC);note.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);note.setMaxLines(2);note.setEllipsize(TextUtils.TruncateAt.END);ThemeBind.textColor(note,Theme.key_windowBackgroundWhiteGrayText);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(3);row.addView(note,np);}
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));
        row.setContentDescription(where.getText()+", "+text.getText());
        row.setOnClickListener(v->open(item));
        row.setOnLongClickListener(v->{actions(item);return true;});
        return row;
    }
    private String sender(Bookmarks.Item item){if(item.sender==0)return chatName(item.dialog);if(item.sender==getUserConfig().getClientUserId())return getString(R.string.TetogramStatsYou);return chatName(item.sender);}

    private void open(Bookmarks.Item item){
        Bundle args=new Bundle();
        if(DialogObject.isEncryptedDialog(item.dialog))args.putInt("enc_id",DialogObject.getEncryptedChatId(item.dialog));
        else if(item.dialog>0)args.putLong("user_id",item.dialog);
        else args.putLong("chat_id",-item.dialog);
        args.putInt("message_id",item.message);
        presentFragment(new ChatActivity(args));
    }
    private void actions(Bookmarks.Item item){
        CharSequence[] names={getString(R.string.TetogramBookmarksOpen),getString(item.note.isEmpty()?R.string.TetogramBookmarksAddNote:R.string.TetogramBookmarksEditNote),getString(R.string.TetogramBookmarkRemove)};
        showDialog(new AlertDialog.Builder(getContext()).setTitle(chatName(item.dialog)).setItems(names,(d,n)->{if(n==0)open(item);else if(n==1)note(item);else{BookmarksStore.update(currentAccount,b->b.remove(item.dialog,item.message));refresh();}}).create());
    }
    private void note(Bookmarks.Item item){
        EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(17);field.setText(item.note);field.setHint(getString(R.string.TetogramBookmarksNoteHint));field.setPadding(dp(24),dp(8),dp(24),dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);
        showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramBookmarksNote)).setView(field).setPositiveButton(getString(R.string.Save),(d,n)->{String value=field.getText().toString();BookmarksStore.update(currentAccount,b->b.setNote(item.dialog,item.message,value));refresh();}).setNegativeButton(getString(R.string.Cancel),null).create());
    }
}
