/* Tetogram, 2026. GPL v2 or later. Read-only local versions, never fake server messages. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.getString;
import android.view.View;
import java.util.*;
import java.text.DateFormat;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.*;

public final class EditHistoryActivity extends LabsPage implements NotificationCenter.NotificationCenterDelegate {
    private final long owner,dialog;
    private final int message;
    private int generation;
    private boolean loading,failed,more;
    private final ArrayList<EditHistory.Version> versions=new ArrayList<>();
    public EditHistoryActivity(int account,long dialog,int message){super(account);owner=UserConfig.getInstance(account).getClientUserId();this.dialog=dialog;this.message=message;}
    private boolean valid(){return owner>0&&owner==getUserConfig().getClientUserId();}
    @Override protected CharSequence title(){return getString(R.string.TetogramEditHistory);}
    @Override public boolean onFragmentCreate(){getNotificationCenter().addObserver(this,NotificationCenter.tetogramEditHistoryChanged);return super.onFragmentCreate();}
    @Override public void onResume(){super.onResume();if(valid())load(true);else finishFragment();}
    @Override public void onFragmentDestroy(){generation++;getNotificationCenter().removeObserver(this,NotificationCenter.tetogramEditHistoryChanged);super.onFragmentDestroy();}
    @Override public void didReceivedNotification(int id,int account,Object...args){long did=(Long)args[0];int mid=(Integer)args[1];if(list!=null&&(did==0||did==dialog)&&(mid==0||message==0||mid==message))load(true);}
    private void load(boolean reset){
        if(!valid()||list==null)return;
        int ticket=++generation;loading=true;failed=false;if(reset)versions.clear();list.adapter.update(false);
        EditHistory.list(currentAccount,dialog,message,versions.size(),page->{if(ticket!=generation||list==null||!valid())return;loading=false;failed=page.failed;more=page.size()==50;versions.addAll(page);list.adapter.update(false);});
    }
    private String author(MessageObject m){long sender=m.getFromChatId();if(sender>0){TLRPC.User user=getMessagesController().getUser(sender);if(user!=null)return UserObject.getUserName(user);}else if(sender<0){TLRPC.Chat chat=getMessagesController().getChat(-sender);if(chat!=null)return chat.title;}return getString(R.string.TetogramSender)+": "+sender;}
    private CharSequence body(MessageObject m){String text=m.messageOwner.message;return text!=null&&!text.isEmpty()?text:m.messageText==null||m.messageText.length()==0?getString(R.string.TetogramMetadataOnly):m.messageText;}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        if(!valid())return;
        items.add(UItem.asShadow(getString(R.string.TetogramEditsLocalInfo)));
        DateFormat date=DateFormat.getDateTimeInstance();
        for(EditHistory.Version version:versions){
            MessageObject m=version.message;
            String label=getString(version.latest?R.string.TetogramEditNew:R.string.TetogramEditOld);
            if(m.messageOwner.edit_date==0)label+=" · "+getString(R.string.TetogramEditOriginal);
            String details=author(m)+" · ID "+m.getId()+"\n"+date.format(new Date((m.messageOwner.edit_date>0?m.messageOwner.edit_date:m.messageOwner.date)*1000L))+"\n"+getString(R.string.TetogramEditsObserved)+": "+date.format(new Date(version.observed*1000L));
            items.add(UItem.asHeader(label));
            items.add(UItem.asCustom(new AppearanceSettingRow(getContext(),body(m),details,true,()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditHistory)).setMessage(body(m)).setPositiveButton(getString(R.string.OK),null).create())),-2));
        }
        if(failed)items.add(UItem.asShadow(getString(R.string.TetogramStorageError)));
        else if(versions.isEmpty())items.add(UItem.asShadow(getString(loading?R.string.Loading:R.string.TetogramEditsEmpty)));
        if(more&&!loading)items.add(UItem.asButton(1,getString(R.string.TetogramLoadMore)));
        if(!versions.isEmpty())items.add(UItem.asButton(2,getString(R.string.TetogramEditsClear)).red());
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){if(item.id==1&&!loading)load(false);else if(item.id==2)confirmClear();}
    private void confirmClear(){showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditsClear)).setMessage(getString(R.string.TetogramEditsClearConfirm)).setPositiveButton(getString(R.string.Delete),(d,n)->EditHistory.erase(currentAccount,dialog,message,ok->{if(!valid()||list==null)return;if(ok)load(true);else showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramStorageError)).setPositiveButton(getString(R.string.OK),null).create());})).setNegativeButton(getString(R.string.Cancel),null).create());}
}
