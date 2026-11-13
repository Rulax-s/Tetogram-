/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.DeletedHistory;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;

public final class HistoryActivity extends LabsPage implements org.telegram.messenger.NotificationCenter.NotificationCenterDelegate {
    private final long dialogId;
    private final ArrayList<MessageObject> rows=new ArrayList<>();
    private String query="";
    private int generation;
    private boolean loading,more,loadError;
    private Runnable search;
    public HistoryActivity(int account,long did) { super(account);dialogId=did; }
    @Override public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this,org.telegram.messenger.NotificationCenter.messagesDeleted);
        return super.onFragmentCreate();
    }
    @Override public void didReceivedNotification(int id,int account,Object... args) { if(list!=null)load(true); }
    @Override protected CharSequence title() { return getString(R.string.TetogramDeletedMessages); }
    @Override public View createView(Context context) {
        View view=super.createView(context);
        EditTextBoldCursor input=new EditTextBoldCursor(context);
        input.setSingleLine(true);input.setTextSize(16);input.setHint(getString(R.string.Search));
        org.telegram.ui.ActionBar.ThemeBind.textColor(input,Theme.key_windowBackgroundWhiteBlackText);
        org.telegram.ui.ActionBar.ThemeBind.hintTextColor(input,Theme.key_windowBackgroundWhiteHintText);
        input.setPadding(AndroidUtilities.dp(16),0,AndroidUtilities.dp(16),0);
        ((FrameLayout)fragmentView).addView(input,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,48));
        list.setPadding(0,AndroidUtilities.dp(48),0,0);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int after) {}
            @Override public void onTextChanged(CharSequence s,int st,int before,int count) {
                query=s.toString(); generation++;
                if(search!=null)AndroidUtilities.cancelRunOnUIThread(search);
                search=()->load(true); AndroidUtilities.runOnUIThread(search,250);
            }
            @Override public void afterTextChanged(Editable text) {}
        });
        return view;
    }
    @Override public void onResume() { super.onResume();load(true); }
    @Override public void onFragmentDestroy() {getNotificationCenter().removeObserver(this,org.telegram.messenger.NotificationCenter.messagesDeleted);generation++;if(search!=null)AndroidUtilities.cancelRunOnUIThread(search);super.onFragmentDestroy();}
    private void load(boolean reset) {
        int ticket=++generation;loading=true;
        if(reset)rows.clear();
        if(list!=null)list.adapter.update(false);
        DeletedHistory.list(currentAccount,dialogId,query,rows.size(),result->{
            if(ticket!=generation || list==null)return;
            loadError=result instanceof DeletedHistory.LocalList && ((DeletedHistory.LocalList)result).failed; rows.addAll(result);more=result.size()==50;loading=false;list.adapter.update(true);
        });
    }
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter) {
        items.add(UItem.asShadow(getString(R.string.TetogramLocalOnly)));
        for(int i=0;i<rows.size();i++) {
            MessageObject m=rows.get(i);
            String text=m.messageOwner.message;
            if(text==null || text.isEmpty())text=getString(R.string.TetogramMetadataOnly);
            items.add(UItem.asButton(i+1,text.replace('\n',' ')));
        }
        if(loadError)items.add(UItem.asShadow(getString(R.string.TetogramStorageError)));
        else if(rows.isEmpty())items.add(UItem.asShadow(getString(loading ? R.string.Loading : R.string.TetogramHistoryEmpty)));
        if(more)items.add(UItem.asButton(-1,getString(R.string.TetogramLoadMore)));
    }
    @Override protected void click(UItem item,View v,int p,float x,float y) {
        if(item.id==-1 && !loading)load(false);
        else if(item.id>0 && item.id<=rows.size()) {
            MessageObject m=rows.get(item.id-1);
            showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramDeletedMessages))
                    .setItems(new CharSequence[]{getString(R.string.TetogramLocalInfo),getString(R.string.TetogramGoToMessage),getString(R.string.TetogramDeleteCopy)},(d,w)-> {
                        if(w==0)showInfo(this,m);
                        else if(w==1)presentFragment(ChatActivity.of(dialogId,m.getId()));
                        else confirmErase(this,m,()->load(true));
                    }).create());
        }
    }
    public static void showInfo(BaseFragment parent,MessageObject m) {
        if(parent.getParentActivity()==null)return;
        DateFormat format=DateFormat.getDateTimeInstance();
        String info=getString(R.string.TetogramLocalOnly)+"\n\n"+(m.messageOwner.message==null?"":m.messageOwner.message)
                +"\n\n"+getString(R.string.TetogramSentAt)+": "+format.format(new Date(m.messageOwner.date*1000L))
                +"\n"+getString(R.string.TetogramReceivedAt)+": "+format.format(new Date(m.tetogramReceivedAt*1000L))
                +"\n"+getString(R.string.TetogramDeletedAt)+": "+format.format(new Date(m.tetogramDeletedAt*1000L))
                +"\nID: "+m.getId()+" · "+getString(R.string.TetogramSender)+": "+m.getFromChatId();
        parent.showDialog(new AlertDialog.Builder(parent.getContext()).setTitle(getString(R.string.TetogramLocalInfo))
                .setMessage(info).setPositiveButton(getString(R.string.OK),null)
                .setNegativeButton(getString(R.string.TetogramDeleteCopy),(d,w)->confirmErase(parent,m,()-> {
                    if(parent instanceof ChatActivity)((ChatActivity)parent).tetogramReconcileHistory();
                    else if(parent instanceof HistoryActivity)((HistoryActivity)parent).load(true);
                })).create());
    }
    private static void confirmErase(BaseFragment parent,MessageObject m,Runnable done) {
        parent.showDialog(new AlertDialog.Builder(parent.getContext()).setTitle(getString(R.string.TetogramDeleteCopy))
                .setMessage(getString(R.string.TetogramClearConfirm))
                .setPositiveButton(getString(R.string.Delete),(d,w)->DeletedHistory.erase(m.currentAccount,m.getDialogId(),m.getId(),ok->{
                    if(ok)done.run();else if(parent.getParentActivity()!=null)parent.showDialog(new AlertDialog.Builder(parent.getContext()).setMessage(getString(R.string.TetogramStorageError)).setPositiveButton(getString(R.string.OK),null).create());
                })).setNegativeButton(getString(R.string.Cancel),null).create());
    }
}
