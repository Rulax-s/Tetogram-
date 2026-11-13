/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.getString;
import android.os.Bundle;
import android.view.View;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.DeletedHistory;
import org.telegram.messenger.tetogram.LabsSettings;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.util.ArrayList;
import java.util.Collections;

public final class ExcludedChatsActivity extends LabsPage {
    private final ArrayList<String> ids=new ArrayList<>();
    public ExcludedChatsActivity(int account) { super(account); }
    @Override protected CharSequence title() { return getString(R.string.TetogramExcludedChats); }
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter) {
        items.add(UItem.asButton(-1,getString(R.string.TetogramAddExcluded)));
        items.add(UItem.asShadow(getString(R.string.TetogramExclusionInfo)));
        ids.clear(); ids.addAll(LabsSettings.exclusions(currentAccount)); Collections.sort(ids);
        for(int i=0;i<ids.size();i++) items.add(UItem.asButton(i+1,ids.get(i),getString(R.string.TetogramRemoveExclusion)));
    }
    @Override protected void click(UItem item,View v,int p,float x,float y) {
        if(item.id>0 && item.id<=ids.size()) {
            LabsSettings.exclude(currentAccount,Long.parseLong(ids.get(item.id-1)),false); list.adapter.update(true); return;
        }
        if(item.id!=-1) return;
        Bundle args=new Bundle();args.putBoolean("onlySelect",true);args.putInt("dialogsType",0);
        DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(currentAccount);
        picker.setDelegate((fragment,dids,message,param,notify,schedule,repeat,topics)-> {
            if(dids.isEmpty() || DialogObject.isEncryptedDialog(dids.get(0).dialogId)) return false;
            long did=dids.get(0).dialogId;
            fragment.finishFragment();
            showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramExcludedChats))
                    .setMessage(getString(R.string.TetogramExcludeConfirm)).setPositiveButton(getString(R.string.OK),(d,w)-> {
                        LabsSettings.exclude(currentAccount,did,true);
                        DeletedHistory.erase(currentAccount,did,0,ok->{if(list!=null)list.adapter.update(true);if(!ok && getParentActivity()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramStorageError)).setPositiveButton(getString(R.string.OK),null).create());});
                    }).setNegativeButton(getString(R.string.Cancel),null).create());
            return true;
        });presentFragment(picker);
    }
}
