/* Tetogram, 2026. GNU GPL v2 or later. Settings for hiding blocked people's messages in groups. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.os.Bundle;
import android.view.View;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.tetogram.Ignore;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.DialogsActivity;

public final class IgnoreActivity extends LabsPage {
    private static final int ENABLE=1,SYNC=2,ADD_CHAT=3,PERSON=1000000;
    private boolean syncing;
    private final ArrayList<Long> people=new ArrayList<>(),chats=new ArrayList<>();
    public IgnoreActivity(int account){super(account);}
    @Override protected CharSequence title(){return getString(R.string.TetogramIgnoreTitle);}
    @Override public void onResume(){super.onResume();if(Ignore.enabled(currentAccount)&&Ignore.syncedAt(currentAccount)==0)sync();}
    private String name(long id){if(id>0){TLRPC.User u=getMessagesController().getUser(id);if(u!=null)return UserObject.getUserName(u);}else{TLRPC.Chat c=getMessagesController().getChat(-id);if(c!=null)return c.title;}return "ID "+id;}
    private void sync(){if(syncing)return;syncing=true;update();Ignore.sync(currentAccount,ok->{syncing=false;if(!ok&&getParentActivity()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramIgnoreSyncFailed)).setPositiveButton(getString(R.string.OK),null).create());update();});}
    private void update(){if(list!=null)list.adapter.update(true);}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        boolean on=Ignore.enabled(currentAccount);
        items.add(UItem.asCheck(ENABLE,getString(R.string.TetogramIgnoreEnable)).setChecked(on));
        items.add(UItem.asShadow(getString(R.string.TetogramIgnoreInfo)));
        items.add(UItem.asHeader(getString(R.string.TetogramIgnoreBlockList)));
        long synced=Ignore.syncedAt(currentAccount);
        String value=syncing?getString(R.string.Loading):synced==0?getString(R.string.TetogramIgnoreNeverSynced):formatString(R.string.TetogramIgnoreSyncedValue,Ignore.blocked(currentAccount).size(),DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(synced)));
        items.add(UItem.asButton(SYNC,getString(R.string.TetogramIgnoreSync),value));
        people.clear();people.addAll(Ignore.blocked(currentAccount));
        if(!people.isEmpty()){
            items.add(UItem.asHeader(getString(R.string.TetogramIgnoreShowPeople)));
            for(int i=0;i<people.size();i++){long id=people.get(i);items.add(UItem.asCheck(PERSON+i,name(id)).setChecked(Ignore.allowedPeople(currentAccount).contains(id)));}
            items.add(UItem.asShadow(getString(R.string.TetogramIgnoreShowPeopleInfo)));
        }
        items.add(UItem.asHeader(getString(R.string.TetogramIgnoreChats)));
        chats.clear();chats.addAll(Ignore.allowedChats(currentAccount));
        for(int i=0;i<chats.size();i++)items.add(UItem.asButton(-(i+1),name(chats.get(i)),getString(R.string.TetogramIgnoreRemove)));
        items.add(UItem.asButton(ADD_CHAT,getString(R.string.TetogramIgnoreAddChat)));
        items.add(UItem.asShadow(getString(R.string.TetogramIgnoreChatsInfo)));
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id==ENABLE){boolean on=!Ignore.enabled(currentAccount);Ignore.setEnabled(currentAccount,on);if(on&&Ignore.syncedAt(currentAccount)==0)sync();update();}
        else if(item.id==SYNC)sync();
        else if(item.id==ADD_CHAT)pickChat();
        else if(item.id>=PERSON&&item.id-PERSON<people.size()){long id=people.get(item.id-PERSON);Ignore.allowPerson(currentAccount,id,!Ignore.allowedPeople(currentAccount).contains(id));update();}
        else if(item.id<0&&-item.id-1<chats.size()){Ignore.allowChat(currentAccount,chats.get(-item.id-1),false);update();}
    }
    private void pickChat(){
        Bundle args=new Bundle();args.putBoolean("onlySelect",true);args.putInt("dialogsType",0);args.putBoolean("allowUsers",false);args.putBoolean("allowBots",false);
        DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(currentAccount);
        picker.setDelegate((fragment,dids,message,param,notify,schedule,repeat,topics)->{if(dids.isEmpty())return false;for(org.telegram.messenger.MessagesStorage.TopicKey key:dids)if(key.dialogId<0)Ignore.allowChat(currentAccount,key.dialogId,true);fragment.finishFragment();update();return true;});
        presentFragment(picker);
    }
}
