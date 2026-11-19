/* Tetogram, 2026. GNU GPL v2 or later. Speech models for offline voice transcription. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.view.View;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.ChatTranslation;
import org.telegram.messenger.tetogram.VoiceTranscription;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class VoiceSettingsActivity extends LabsPage {
    private static final int MODEL=100,CLEAR=1;
    public VoiceSettingsActivity(int account){super(account);}
    @Override protected CharSequence title(){return getString(R.string.TetogramVoice);}
    private void update(){if(list!=null)list.adapter.update(true);}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        items.add(UItem.asHeader(getString(R.string.TetogramVoiceLanguage)));
        String current=VoiceTranscription.language();
        for(int i=0;i<VoiceTranscription.MODELS.length;i++){String[] m=VoiceTranscription.MODELS[i];
            String state=VoiceTranscription.installed(m[0])?AndroidUtilities.formatFileSize(VoiceTranscription.size(m[0])):formatString(R.string.TetogramVoiceNotDownloaded,m[2]);
            items.add(UItem.asRadio(MODEL+i,ChatTranslation.languageName(m[0]),state).setChecked(m[0].equals(current)));}
        items.add(UItem.asShadow(getString(R.string.TetogramVoiceInfo)));
        int cached=VoiceTranscription.cachedCount();
        if(cached>0){items.add(UItem.asButton(CLEAR,getString(R.string.TetogramVoiceClear),String.valueOf(cached)));items.add(UItem.asShadow(getString(R.string.TetogramVoiceClearInfo)));}
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id==CLEAR){VoiceTranscription.clearCache();update();return;}
        int i=item.id-MODEL;if(i<0||i>=VoiceTranscription.MODELS.length)return;String lang=VoiceTranscription.MODELS[i][0];
        if(!VoiceTranscription.installed(lang)){VoiceUi.download(this,lang,()->{VoiceTranscription.setLanguage(lang);update();});return;}
        if(lang.equals(VoiceTranscription.language())){
            showDialog(new AlertDialog.Builder(getContext()).setTitle(ChatTranslation.languageName(lang)).setMessage(getString(R.string.TetogramVoiceDeleteText))
                .setPositiveButton(getString(R.string.Delete),(d,n)->{VoiceTranscription.remove(lang);AndroidUtilities.runOnUIThread(this::update,300);}).setNegativeButton(getString(R.string.Cancel),null).create());
            return;
        }
        VoiceTranscription.setLanguage(lang);update();
    }
}
