/* Tetogram, 2026. GNU GPL v2 or later. Settings for "online in Tetogram". */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import android.view.View;
import java.util.ArrayList;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.tetogram.TetoPresence;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class PresenceActivity extends LabsPage {
    private static final int CONNECT=1,VISIBLE=2,DISCONNECT=3,SERVER=4;
    private boolean connecting;
    public PresenceActivity(int account){super(account);}
    @Override protected CharSequence title(){return getString(R.string.TetogramPresence);}
    private void update(){if(list!=null)list.adapter.update(true);}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        items.add(UItem.asShadow(getString(R.string.TetogramPresenceInfo)));
        if(!TetoPresence.configured()){
            items.add(UItem.asButton(SERVER,getString(R.string.TetogramPresenceServer),getString(R.string.TetogramPresenceServerMissing)));
            items.add(UItem.asShadow(getString(R.string.TetogramPresenceServerInfo)));
            return;
        }
        if(!TetoPresence.connected(currentAccount)){
            items.add(UItem.asButton(CONNECT,getString(connecting?R.string.TetogramPresenceConnecting:R.string.TetogramPresenceConnect)));
            items.add(UItem.asShadow(getString(R.string.TetogramPresenceConnectInfo)));
        }else{
            items.add(UItem.asCheck(VISIBLE,getString(R.string.TetogramPresenceVisible)).setChecked(TetoPresence.visible(currentAccount)));
            items.add(UItem.asShadow(getString(R.string.TetogramPresenceVisibleInfo)));
            items.add(UItem.asButton(DISCONNECT,getString(R.string.TetogramPresenceDisconnect)).red());
        }
        items.add(UItem.asButton(SERVER,getString(R.string.TetogramPresenceServer),TetoPresence.server().replace("https://","")));
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id==SERVER)server();
        else if(item.id==CONNECT&&!connecting){connecting=true;update();
            TetoPresence.connect(currentAccount,url->Browser.openUrl(getParentActivity(),url),(ok,error)->{connecting=false;update();
                if(getParentActivity()==null)return;
                BulletinFactory.of(this).createSimpleBulletin(ok?R.raw.contact_check:R.raw.error,getString(ok?R.string.TetogramPresenceConnected:"account".equals(error)?R.string.TetogramPresenceWrongAccount:R.string.TetogramPresenceFailed)).show();});}
        else if(item.id==VISIBLE){TetoPresence.setVisible(currentAccount,!TetoPresence.visible(currentAccount));update();}
        else if(item.id==DISCONNECT){TetoPresence.disconnect(currentAccount);update();}
    }
    private void server(){
        EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(16);field.setSingleLine(true);field.setText(TetoPresence.server());field.setHint("https://…workers.dev");field.setPadding(dp(24),dp(8),dp(24),dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);
        showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramPresenceServer)).setView(field)
            .setPositiveButton(getString(R.string.Save),(d,n)->{TetoPresence.setServer(field.getText().toString());update();}).setNegativeButton(getString(R.string.Cancel),null).create());
    }
}
