/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.getString;
import android.os.Bundle;
import android.view.View;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.DeletedHistory;
import org.telegram.messenger.tetogram.LabsSettings;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.util.ArrayList;

public final class LabsActivity extends LabsPage {
    public LabsActivity(int account) { super(account); }
    @Override protected CharSequence title() { return getString(R.string.TetogramLabs); }
    @Override protected void fill(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(getString(R.string.TetogramLabsWarning)));
        items.add(UItem.asHeader(getString(R.string.TetogramHistorySection)));
        items.add(UItem.asCheck(1,getString(R.string.TetogramSaveDeleted)).setChecked(LabsSettings.enabled(currentAccount)));
        items.add(UItem.asCheck(2,getString(R.string.TetogramShowDeleted)).setChecked(LabsSettings.prefs(currentAccount).getBoolean("show_deleted",true)));
        items.add(UItem.asCheck(3,getString(R.string.TetogramDeletionMark)).setChecked(LabsSettings.mark(currentAccount)));
        items.add(UItem.asButton(4,getString(R.string.TetogramExcludedChats),Integer.toString(LabsSettings.exclusions(currentAccount).size())));
        items.add(UItem.asButton(5,getString(R.string.TetogramRetention),retentionText()));
        items.add(UItem.asButton(6,getString(R.string.TetogramOpenHistory)));
        items.add(UItem.asButton(7,getString(R.string.TetogramClearHistory)).red());
        items.add(UItem.asShadow(getString(R.string.TetogramHistoryPrivacy)));
        items.add(UItem.asHeader(getString(R.string.TetogramEditHistory)));
        items.add(UItem.asShadow(getString(R.string.TetogramEditUnavailable)));
        items.add(UItem.asHeader(getString(R.string.TetogramInterfaceSection)));
        items.add(UItem.asShadow(getString(R.string.TetogramInterfaceFuture)));
        items.add(UItem.asHeader(getString(R.string.TetogramInformation)));
        items.add(UItem.asButton(8,"Tetogram",BuildVars.BUILD_VERSION_STRING));
        items.add(UItem.asButton(9,"Telegram","13.2.2"));
        items.add(UItem.asButton(10,getString(R.string.TetogramLicenses)));
        items.add(UItem.asShadow(getString(R.string.TetogramIntro)));
    }
    private CharSequence retentionText() {
        int d=LabsSettings.days(currentAccount);
        return d==0 ? getString(R.string.TetogramUnlimited) : d+" "+getString(R.string.TetogramDays);
    }
    private void refresh() { if(list!=null) list.adapter.update(true); }
    @Override protected void click(UItem item,View view,int position,float x,float y) {
        switch(item.id) {
            case 1:
                if(LabsSettings.enabled(currentAccount)) { LabsSettings.prefs(currentAccount).edit().putBoolean("save_deleted",false).apply(); refresh(); }
                else if(LabsSettings.prefs(currentAccount).getBoolean("consent_v1",false)) enable();
                else showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramLabs))
                        .setMessage(getString(R.string.TetogramConsent))
                        .setPositiveButton(getString(R.string.Enable),(d,w)->enable())
                        .setNegativeButton(getString(R.string.Cancel),null).create());
                break;
            case 2: toggle("show_deleted",true); break;
            case 3: toggle("deletion_mark",true); break;
            case 4: presentFragment(new ExcludedChatsActivity(currentAccount)); break;
            case 5:
                CharSequence[] choices={"7 "+getString(R.string.TetogramDays),"30 "+getString(R.string.TetogramDays),"90 "+getString(R.string.TetogramDays),getString(R.string.TetogramUnlimited)};
                showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramRetention)).setItems(choices,(d,which)-> {
                    LabsSettings.prefs(currentAccount).edit().putInt("retention_days",new int[]{7,30,90,0}[which]).apply();
                    DeletedHistory.maintain(currentAccount); refresh();
                }).create()); break;
            case 6:
                selectChat(); break;
            case 7:
                showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramClearHistory))
                        .setMessage(getString(R.string.TetogramClearConfirm))
                        .setPositiveButton(getString(R.string.Delete),(d,w)->DeletedHistory.erase(currentAccount,0,0,ok-> {
                            if(getParentActivity()!=null) showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(ok ? R.string.TetogramCleared : R.string.TetogramStorageError)).setPositiveButton(getString(R.string.OK),null).create());
                        })).setNegativeButton(getString(R.string.Cancel),null).create()); break;
            case 8: case 9: case 10:
                showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramInformation)).setMessage(getString(R.string.TetogramLicenseInfo)).setPositiveButton(getString(R.string.OK),null).create()); break;
        }
    }
    private void enable() { LabsSettings.prefs(currentAccount).edit().putBoolean("consent_v1",true).putBoolean("save_deleted",true).apply(); refresh(); }
    private void toggle(String key,boolean fallback) { LabsSettings.prefs(currentAccount).edit().putBoolean(key,!LabsSettings.prefs(currentAccount).getBoolean(key,fallback)).apply(); refresh(); }
    private void selectChat() {
        Bundle args=new Bundle(); args.putBoolean("onlySelect",true); args.putInt("dialogsType",0);
        DialogsActivity picker=new DialogsActivity(args); picker.setCurrentAccount(currentAccount);
        picker.setDelegate((fragment,dids,message,param,notify,schedule,repeat,topics)-> {
            if(dids.isEmpty() || DialogObject.isEncryptedDialog(dids.get(0).dialogId)) return false;
            fragment.finishFragment(); presentFragment(new HistoryActivity(currentAccount,dids.get(0).dialogId)); return true;
        });
        presentFragment(picker);
    }
}
