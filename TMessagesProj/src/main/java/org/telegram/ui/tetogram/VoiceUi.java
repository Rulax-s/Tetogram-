/* Tetogram, 2026. GNU GPL v2 or later. "Transcribe on device" for voice and round video messages. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.app.Activity;
import java.io.File;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.ChatTranslation;
import org.telegram.messenger.tetogram.VoiceTranscription;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

public final class VoiceUi {
    private VoiceUi(){}
    /** Hidden for now at the owner's request; the code stays for a later return. */
    public static final boolean ENABLED=false;
    public static boolean supports(MessageObject m){return ENABLED&&m!=null&&m.messageOwner!=null&&(m.isVoice()||m.isRoundVideo())&&m.getDocument()!=null;}
    private static String key(MessageObject m){return m.currentAccount+":"+m.getDialogId()+":"+m.getId();}

    public static void transcribe(BaseFragment f,MessageObject m){
        Activity ctx=f.getParentActivity();if(ctx==null||!supports(m))return;
        String cached=VoiceTranscription.cached(key(m));if(cached!=null){result(f,cached);return;}
        String lang=VoiceTranscription.language();
        if(!VoiceTranscription.installed(lang)){askModel(f,lang,()->transcribe(f,m));return;}
        AlertDialog progress=new AlertDialog(ctx,AlertDialog.ALERT_TYPE_LOADING);progress.setTitle(getString(R.string.TetogramVoiceTranscribing));progress.setMessage(getString(R.string.TetogramVoicePreparing));progress.setCanCancel(false);
        f.showDialog(progress);
        withFile(m,0,file->{
            if(file==null){progress.dismiss();BulletinFactory.of(f).createSimpleBulletin(R.raw.error,getString(R.string.TetogramVoiceNoFile)).show();return;}
            progress.setMessage(getString(R.string.TetogramVoiceRecognising));
            VoiceTranscription.transcribe(key(m),file,progress::setProgress,(text,error)->{
                progress.dismiss();if(f.getParentActivity()==null)return;
                if(text!=null){result(f,text);return;}
                BulletinFactory.of(f).createSimpleBulletin(R.raw.error,getString("empty".equals(error)?R.string.TetogramVoiceNothing:R.string.TetogramVoiceFailed)).show();
            });
        });
    }

    private interface FileReady{void ready(File file);}
    /** The audio file, downloading it first if needed (up to a minute). */
    private static void withFile(MessageObject m,int attempt,FileReady ready){
        File file=null;
        if(m.messageOwner.attachPath!=null&&!m.messageOwner.attachPath.isEmpty()){File own=new File(m.messageOwner.attachPath);if(own.exists())file=own;}
        if(file==null){File cached=FileLoader.getInstance(m.currentAccount).getPathToMessage(m.messageOwner);if(cached!=null&&cached.exists()&&cached.length()>0)file=cached;}
        if(file!=null){ready.ready(file);return;}
        if(attempt==0)FileLoader.getInstance(m.currentAccount).loadFile(m.getDocument(),m,FileLoader.PRIORITY_HIGH,0);
        if(attempt>=120){ready.ready(null);return;}
        AndroidUtilities.runOnUIThread(()->withFile(m,attempt+1,ready),500);
    }

    /** Offers to download the speech model for {@code lang}; runs {@code after} once it is installed. */
    public static void askModel(BaseFragment f,String lang,Runnable after){
        Activity ctx=f.getParentActivity();if(ctx==null)return;String[] model=VoiceTranscription.model(lang);if(model==null)return;
        f.showDialog(new AlertDialog.Builder(ctx).setTitle(getString(R.string.TetogramVoiceModelTitle))
            .setMessage(formatString(R.string.TetogramVoiceModelText,ChatTranslation.languageName(lang),model[2]))
            .setPositiveButton(getString(R.string.TetogramVoiceDownload),(d,n)->download(f,lang,after)).setNegativeButton(getString(R.string.Cancel),null).create());
    }
    public static void download(BaseFragment f,String lang,Runnable after){
        Activity ctx=f.getParentActivity();if(ctx==null)return;
        AlertDialog progress=new AlertDialog(ctx,AlertDialog.ALERT_TYPE_LOADING);progress.setTitle(getString(R.string.TetogramVoiceDownloading));progress.setMessage(ChatTranslation.languageName(lang));
        progress.setCanCancel(true);progress.setOnCancelListener(d->VoiceTranscription.cancelDownload());
        f.showDialog(progress);
        VoiceTranscription.download(lang,progress::setProgress,(ok,error)->{
            progress.dismiss();if(f.getParentActivity()==null)return;
            if(ok!=null){if(after!=null)after.run();return;}
            if(!"cancel".equals(error))BulletinFactory.of(f).createSimpleBulletin(R.raw.error,getString(R.string.TetogramVoiceDownloadFailed)).show();
        });
    }

    private static void result(BaseFragment f,String text){
        Activity ctx=f.getParentActivity();if(ctx==null)return;
        AlertDialog.Builder b=new AlertDialog.Builder(ctx).setTitle(getString(R.string.TetogramVoiceResult)).setMessage(text)
            .setPositiveButton(getString(R.string.Copy),(d,n)->{AndroidUtilities.addToClipboard(text);BulletinFactory.of(f).createCopyBulletin(getString(R.string.TextCopied)).show();})
            .setNegativeButton(getString(R.string.Close),null);
        b.setNeutralButton(getString(R.string.TetogramVoiceTranslate),(d,n)->{
            AlertDialog wait=new AlertDialog(ctx,AlertDialog.ALERT_TYPE_SPINNER);f.showDialog(wait);
            ChatTranslation.translate(text,VoiceTranscription.language(),ChatTranslation.target(),out->{wait.dismiss();if(f.getParentActivity()==null)return;
                if(out==null)BulletinFactory.of(f).createSimpleBulletin(R.raw.error,getString(R.string.TetogramTranslateFailed)).show();
                else f.showDialog(new AlertDialog.Builder(ctx).setTitle(getString(R.string.TetogramTranslate)).setMessage(out).setPositiveButton(getString(R.string.OK),null).create());});
        });
        f.showDialog(b.create());
    }
}
