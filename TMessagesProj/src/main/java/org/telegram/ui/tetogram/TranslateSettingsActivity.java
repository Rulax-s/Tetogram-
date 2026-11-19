/* Tetogram, 2026. GNU GPL v2 or later. Settings for Tetogram chat translation. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import android.view.View;
import java.util.ArrayList;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.tetogram.ChatTranslation;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class TranslateSettingsActivity extends LabsPage {
    private static final int DEVICE=1,GOOGLE=2,DEEPL=3,KEY=4,TARGET=5,OUTGOING=6,CLEAR=7,CHAT=1000;
    private final ArrayList<Long> chats=new ArrayList<>();
    public TranslateSettingsActivity(int account){super(account);}
    @Override protected CharSequence title(){return getString(R.string.TetogramTranslate);}
    private void update(){if(list!=null)list.adapter.update(true);}
    private String name(long id){if(id>0){TLRPC.User u=getMessagesController().getUser(id);if(u!=null)return UserObject.getUserName(u);}else{TLRPC.Chat c=getMessagesController().getChat(-id);if(c!=null)return c.title;}return "ID "+id;}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        String engine=ChatTranslation.engine();
        items.add(UItem.asHeader(getString(R.string.TetogramTranslateEngine)));
        items.add(UItem.asRadio(DEVICE,getString(R.string.TetogramTranslateDevice)).setChecked(ChatTranslation.DEVICE.equals(engine)));
        items.add(UItem.asRadio(GOOGLE,getString(R.string.TetogramTranslateGoogle)).setChecked(ChatTranslation.GOOGLE.equals(engine)));
        items.add(UItem.asRadio(DEEPL,getString(R.string.TetogramTranslateDeepl)).setChecked(ChatTranslation.DEEPL.equals(engine)));
        if(!ChatTranslation.DEVICE.equals(engine))items.add(UItem.asButton(KEY,getString(R.string.TetogramTranslateKey),getString(ChatTranslation.key(engine).isEmpty()?R.string.TetogramTranslateKeyMissing:R.string.TetogramTranslateKeySet)));
        items.add(UItem.asShadow(getString(ChatTranslation.DEVICE.equals(engine)?R.string.TetogramTranslateDeviceInfo:R.string.TetogramTranslateOnlineInfo)));
        items.add(UItem.asHeader(getString(R.string.TetogramTranslateOptions)));
        items.add(UItem.asButton(TARGET,getString(R.string.TetogramTranslateTarget),ChatTranslation.languageName(ChatTranslation.target())));
        items.add(UItem.asCheck(OUTGOING,getString(R.string.TetogramTranslateOutgoing)).setChecked(ChatTranslation.translateOutgoing()));
        items.add(UItem.asShadow(getString(R.string.TetogramTranslateOutgoingInfo)));
        int saved=ChatTranslation.storedCount();
        if(saved>0){items.add(UItem.asButton(CLEAR,getString(R.string.TetogramTranslateClear),String.valueOf(saved)));items.add(UItem.asShadow(getString(R.string.TetogramTranslateClearInfo)));}
        chats.clear();chats.addAll(ChatTranslation.chats(currentAccount));
        items.add(UItem.asHeader(getString(R.string.TetogramTranslateChats)));
        if(chats.isEmpty())items.add(UItem.asShadow(getString(R.string.TetogramTranslateNoChats)));
        else{for(int i=0;i<chats.size();i++){long id=chats.get(i);String lang=ChatTranslation.chatLanguage(currentAccount,id);items.add(UItem.asButton(CHAT+i,name(id),lang==null?getString(R.string.TetogramTranslateOff):ChatTranslation.languageName(lang)));}
            items.add(UItem.asShadow(getString(R.string.TetogramTranslateChatsInfo)));}
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id==DEVICE){ChatTranslation.setEngine(ChatTranslation.DEVICE);update();}
        else if(item.id==GOOGLE||item.id==DEEPL){String e=item.id==GOOGLE?ChatTranslation.GOOGLE:ChatTranslation.DEEPL;ChatTranslation.setEngine(e);update();if(ChatTranslation.key(e).isEmpty())key(e);}
        else if(item.id==KEY)key(ChatTranslation.engine());
        else if(item.id==TARGET)ChatTranslateUi.pickLanguage(this,getString(R.string.TetogramTranslateTarget),true,lang->{ChatTranslation.setTarget(lang);update();});
        else if(item.id==CLEAR){ChatTranslation.clearStored();update();}
        else if(item.id==OUTGOING){ChatTranslation.setTranslateOutgoing(!ChatTranslation.translateOutgoing());update();}
        else if(item.id>=CHAT&&item.id-CHAT<chats.size()){long id=chats.get(item.id-CHAT);
            showDialog(new AlertDialog.Builder(getContext()).setTitle(name(id)).setItems(new CharSequence[]{getString(R.string.TetogramTranslateChatLanguage),getString(R.string.TetogramTranslateStop)},(d,n)->{
                if(n==0)ChatTranslateUi.pickLanguage(this,getString(R.string.TetogramTranslateChatLanguage),true,lang->{ChatTranslation.setChatLanguage(currentAccount,id,lang);update();});
                else{ChatTranslation.setActive(currentAccount,id,false);update();}
            }).create());}
    }
    private void key(String engine){
        EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(16);field.setSingleLine(true);field.setText(ChatTranslation.key(engine));field.setHint(getString(R.string.TetogramTranslateKeyHint));field.setPadding(dp(24),dp(8),dp(24),dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);
        showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(ChatTranslation.GOOGLE.equals(engine)?R.string.TetogramTranslateGoogle:R.string.TetogramTranslateDeepl)).setMessage(getString(R.string.TetogramTranslateKeyInfo)).setView(field)
            .setPositiveButton(getString(R.string.Save),(d,n)->{ChatTranslation.setKey(engine,field.getText().toString());update();}).setNegativeButton(getString(R.string.Cancel),null).create());
    }
}
