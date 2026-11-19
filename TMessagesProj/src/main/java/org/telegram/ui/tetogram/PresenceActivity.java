/* Tetogram, 2026. GNU GPL v2 or later. Settings for "online in Tetogram". */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import android.view.View;
import java.util.ArrayList;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.TetoPresence;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class PresenceActivity extends LabsPage {
    private static final int VISIBLE=2,SERVER=4;
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
        items.add(UItem.asCheck(VISIBLE,getString(R.string.TetogramPresenceVisible)).setChecked(TetoPresence.visible(currentAccount)));
        items.add(UItem.asShadow(getString(R.string.TetogramPresenceVisibleInfo)));
        items.add(UItem.asButton(SERVER,getString(R.string.TetogramPresenceServer),TetoPresence.server().replace("https://","")));
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id==SERVER)server();
        else if(item.id==VISIBLE){TetoPresence.setVisible(currentAccount,!TetoPresence.visible(currentAccount));update();}
    }
    private void server(){
        EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(16);field.setSingleLine(true);field.setText(TetoPresence.server());field.setHint("https://…workers.dev");field.setPadding(dp(24),dp(8),dp(24),dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);
        showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramPresenceServer)).setView(field)
            .setPositiveButton(getString(R.string.Save),(d,n)->{TetoPresence.setServer(field.getText().toString());update();}).setNegativeButton(getString(R.string.Cancel),null).create());
    }
}
