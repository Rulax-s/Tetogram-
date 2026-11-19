/* Tetogram, 2026. GNU GPL v2 or later. Chat translation dialogs: chat menu, language picker and the preview before sending. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.tetogram.ChatTranslation;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

public final class ChatTranslateUi {
    private ChatTranslateUi(){}
    public interface Language{void picked(String code);}
    public interface Choice{void send(CharSequence text,boolean translated);}

    /** @param auto offer "automatic" (empty code) as the first entry */
    public static void pickLanguage(BaseFragment f,CharSequence title,boolean auto,Language done){
        if(f==null||f.getParentActivity()==null)return;
        List<String> codes=new ArrayList<>(ChatTranslation.languages());Collections.sort(codes,(a,b)->ChatTranslation.languageName(a).compareToIgnoreCase(ChatTranslation.languageName(b)));
        if(auto)codes.add(0,"");
        CharSequence[] names=new CharSequence[codes.size()];for(int i=0;i<names.length;i++)names[i]=codes.get(i).isEmpty()?getString(R.string.TetogramTranslateAuto):ChatTranslation.languageName(codes.get(i));
        f.showDialog(new AlertDialog.Builder(f.getParentActivity()).setTitle(title).setItems(names,(d,n)->done.picked(codes.get(n))).create());
    }

    /** Header ⋮ item of a chat. {@code changed} refreshes the messages. */
    public static void chatMenu(BaseFragment f,int account,long dialog,Runnable changed){
        if(f.getParentActivity()==null)return;
        boolean on=ChatTranslation.active(account,dialog);String lang=ChatTranslation.chatLanguage(account,dialog);
        CharSequence[] items={getString(on?R.string.TetogramTranslateStop:R.string.TetogramTranslateStart),
            formatString(R.string.TetogramTranslateChatLanguageValue,lang==null?getString(R.string.TetogramTranslateAuto):ChatTranslation.languageName(lang)),
            getString(R.string.TetogramTranslateSettings)};
        f.showDialog(new AlertDialog.Builder(f.getParentActivity()).setTitle(getString(R.string.TetogramTranslate)).setItems(items,(d,n)->{
            if(n==0){ChatTranslation.setActive(account,dialog,!on);changed.run();}
            else if(n==1)pickLanguage(f,getString(R.string.TetogramTranslateChatLanguage),true,code->{ChatTranslation.setChatLanguage(account,dialog,code);changed.run();});
            else f.presentFragment(new TranslateSettingsActivity(account));
        }).create());
    }

    /** Shows the original or what was sent for a message Tetogram displays translated. */
    public static void showOther(BaseFragment f,MessageObject m){
        if(f.getParentActivity()==null||m==null||m.messageOwner==null)return;
        f.showDialog(new AlertDialog.Builder(f.getParentActivity()).setTitle(getString(m.isOutOwner()?R.string.TetogramTranslateSentText:R.string.TetogramTranslateOriginal)).setMessage(m.getDisplayText()).setPositiveButton(getString(R.string.OK),null).create());
    }

    /**
     * Before sending in a translated chat: translates the user's text and asks what to send.
     * Returns false when nothing needs translating, so the caller sends right away.
     */
    public static boolean askBeforeSend(BaseFragment f,int account,long dialog,CharSequence typed,Choice choice){
        if(f==null||f.getParentActivity()==null)return false;
        String to=ChatTranslation.outgoingTarget(account,dialog,typed);if(to==null)return false;
        String text=typed.toString();
        AlertDialog progress=new AlertDialog(f.getParentActivity(),AlertDialog.ALERT_TYPE_SPINNER);progress.setCanCancel(true);
        boolean[] cancelled={false};progress.setOnCancelListener(d->cancelled[0]=true);
        f.showDialog(progress);
        ChatTranslation.prepareOutgoing(text,to,out->{
            if(cancelled[0])return;progress.dismiss();if(f.getParentActivity()==null)return;
            if(out==null||out.trim().isEmpty()){
                f.showDialog(new AlertDialog.Builder(f.getParentActivity()).setTitle(getString(R.string.TetogramTranslate)).setMessage(getString(R.string.TetogramTranslateFailed))
                    .setPositiveButton(getString(R.string.TetogramTranslateSendOriginal),(d,n)->choice.send(typed,false)).setNegativeButton(getString(R.string.Cancel),null).create());
                return;
            }
            f.showDialog(new AlertDialog.Builder(f.getParentActivity()).setTitle(formatString(R.string.TetogramTranslatePreviewTitle,ChatTranslation.languageName(to))).setMessage(out)
                .setPositiveButton(getString(R.string.TetogramTranslateSendTranslation),(d,n)->choice.send(out,true))
                .setNeutralButton(getString(R.string.TetogramTranslateSendOriginal),(d,n)->choice.send(typed,false))
                .setNegativeButton(getString(R.string.Cancel),null).create());
        },()->{if(cancelled[0])return;progress.dismiss();choice.send(typed,false);});
        return true;
    }
}
