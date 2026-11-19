/* Tetogram, 2026. GNU GPL v2 or later. Settings of one local history: deleted messages or edit versions. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.os.Bundle;
import android.view.View;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.DeletedHistory;
import org.telegram.messenger.tetogram.EditHistory;
import org.telegram.messenger.tetogram.LabsSettings;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.DialogsActivity;

/** The same switches, consents and storage as before; they only moved out of the Labs overview. */
public final class LabsHistoryActivity extends LabsPage {
    private final boolean edits;
    private long[] stats;
    public LabsHistoryActivity(int account,boolean edits){super(account);this.edits=edits;}
    @Override protected CharSequence title(){return getString(edits?R.string.TetogramEditHistory:R.string.TetogramLabsDeletedTitle);}
    @Override public void onResume(){super.onResume();loadStats();}
    private void loadStats(){DeletedHistory.Callback<long[]> done=v->{stats=v;if(list!=null)list.adapter.update(false);};if(edits)EditHistory.stats(currentAccount,done);else DeletedHistory.stats(currentAccount,done);}
    private void refresh(){if(list!=null)list.adapter.update(true);}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        if(edits){
            items.add(UItem.asCheck(12,getString(R.string.TetogramEditsSave)).setChecked(LabsSettings.edits(currentAccount)));
            items.add(UItem.asShadow(getString(R.string.TetogramEditsLocalInfo)));
            items.add(UItem.asHeader(getString(R.string.TetogramLabsShow)));
            items.add(UItem.asCheck(13,getString(R.string.TetogramEditsMark)).setChecked(LabsSettings.editMark(currentAccount)));
        }else{
            items.add(UItem.asCheck(1,getString(R.string.TetogramSaveDeleted)).setChecked(LabsSettings.enabled(currentAccount)));
            items.add(UItem.asShadow(getString(R.string.TetogramHistoryPrivacy)));
            items.add(UItem.asHeader(getString(R.string.TetogramLabsShow)));
            items.add(UItem.asCheck(2,getString(R.string.TetogramShowDeleted)).setChecked(LabsSettings.prefs(currentAccount).getBoolean("show_deleted",true)));
            items.add(UItem.asCheck(3,getString(R.string.TetogramDeletionMark)).setChecked(LabsSettings.mark(currentAccount)));
            items.add(UItem.asHeader(getString(R.string.TetogramLabsStorage)));
            int d=LabsSettings.days(currentAccount);
            items.add(UItem.asButton(5,getString(R.string.TetogramRetention),d==0?getString(R.string.TetogramUnlimited):d+" "+getString(R.string.TetogramDays)));
            items.add(UItem.asButton(4,getString(R.string.TetogramExcludedChats),Integer.toString(LabsSettings.exclusions(currentAccount).size())));
        }
        items.add(UItem.asButton(20,getString(R.string.TetogramLabsSavedNow),stats==null?"…":formatString(R.string.TetogramLabsSavedValue,AndroidUtilities.formatWholeNumber((int)Math.min(Integer.MAX_VALUE,stats[0]),0),AndroidUtilities.formatFileSize(stats[1]))));
        items.add(UItem.asButton(edits?14:6,getString(edits?R.string.TetogramEditsOpen:R.string.TetogramOpenHistory)));
        items.add(UItem.asButton(edits?15:7,getString(edits?R.string.TetogramEditsClear:R.string.TetogramClearHistory)).red());
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        switch(item.id){
            case 1:
                if(LabsSettings.enabled(currentAccount)){LabsSettings.prefs(currentAccount).edit().putBoolean("save_deleted",false).apply();refresh();}
                else if(LabsSettings.prefs(currentAccount).getBoolean("consent_v1",false)){LabsSettings.prefs(currentAccount).edit().putBoolean("save_deleted",true).apply();refresh();}
                else showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramLabs)).setMessage(getString(R.string.TetogramConsent)).setPositiveButton(getString(R.string.Enable),(d,w)->{LabsSettings.prefs(currentAccount).edit().putBoolean("consent_v1",true).putBoolean("save_deleted",true).apply();refresh();}).setNegativeButton(getString(R.string.Cancel),null).create());
                break;
            case 2: toggle("show_deleted"); break;
            case 3: toggle("deletion_mark"); break;
            case 4: presentFragment(new ExcludedChatsActivity(currentAccount)); break;
            case 5:
                CharSequence[] choices={"7 "+getString(R.string.TetogramDays),"30 "+getString(R.string.TetogramDays),"90 "+getString(R.string.TetogramDays),getString(R.string.TetogramUnlimited)};
                showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramRetention)).setItems(choices,(d,which)->{LabsSettings.prefs(currentAccount).edit().putInt("retention_days",new int[]{7,30,90,0}[which]).apply();DeletedHistory.maintain(currentAccount);refresh();}).create());
                break;
            case 6: selectChat(false); break;
            case 7: confirmClear(false); break;
            case 12:
                if(LabsSettings.edits(currentAccount)){LabsSettings.prefs(currentAccount).edit().putBoolean("save_edits",false).apply();refresh();}
                else if(LabsSettings.prefs(currentAccount).getBoolean("edit_consent_v1",false)){LabsSettings.prefs(currentAccount).edit().putBoolean("save_edits",true).apply();refresh();}
                else showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditHistory)).setMessage(getString(R.string.TetogramEditsConsent)).setPositiveButton(getString(R.string.Enable),(d,n)->{LabsSettings.prefs(currentAccount).edit().putBoolean("edit_consent_v1",true).putBoolean("save_edits",true).apply();refresh();}).setNegativeButton(getString(R.string.Cancel),null).create());
                break;
            case 13: toggle("edit_history_mark");EditHistory.refresh(currentAccount); break;
            case 14: selectChat(true); break;
            case 15: confirmClear(true); break;
        }
    }
    private void toggle(String key){LabsSettings.prefs(currentAccount).edit().putBoolean(key,!LabsSettings.prefs(currentAccount).getBoolean(key,true)).apply();refresh();}
    private void confirmClear(boolean edits){
        showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(edits?R.string.TetogramEditsClear:R.string.TetogramClearHistory)).setMessage(getString(edits?R.string.TetogramEditsClearConfirm:R.string.TetogramClearConfirm)).setPositiveButton(getString(R.string.Delete),(d,w)->{
            DeletedHistory.Callback<Boolean> done=ok->{if(getParentActivity()==null)return;loadStats();showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(ok?R.string.TetogramCleared:R.string.TetogramStorageError)).setPositiveButton(getString(R.string.OK),null).create());};
            if(edits)EditHistory.erase(currentAccount,0,0,done);else DeletedHistory.erase(currentAccount,0,0,done);
        }).setNegativeButton(getString(R.string.Cancel),null).create());
    }
    private void selectChat(boolean edits){
        Bundle args=new Bundle();args.putBoolean("onlySelect",true);args.putInt("dialogsType",0);
        DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(currentAccount);
        picker.setDelegate((fragment,dids,message,param,notify,schedule,repeat,topics)->{
            if(dids.isEmpty()||DialogObject.isEncryptedDialog(dids.get(0).dialogId))return false;
            fragment.finishFragment();long did=dids.get(0).dialogId;presentFragment(edits?new EditHistoryActivity(currentAccount,did,0):new HistoryActivity(currentAccount,did));return true;
        });
        presentFragment(picker);
    }
}
