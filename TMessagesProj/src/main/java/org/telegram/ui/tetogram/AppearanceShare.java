/* Tetogram, 2026. GNU GPL v2 or later. Sharing appearance presets in chats as ".tetotheme" files. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.getString;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
import java.util.Locale;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;

public final class AppearanceShare {
    private AppearanceShare(){}
    public static final String EXTENSION=".tetotheme";

    public static boolean isShared(String fileName){return fileName!=null&&fileName.toLowerCase(Locale.ROOT).endsWith(EXTENSION);}

    /** Opens a downloaded ".tetotheme" from a chat in Tetogram instead of an external app. */
    public static boolean open(Activity activity,MessageObject message,File file){
        if(message==null||file==null||!file.isFile()||!isShared(message.getFileName()))return false;
        BaseFragment top=LaunchActivity.getLastFragment();if(top==null||top.getParentActivity()==null)return false;
        AppearanceActivity.openShared(top,message.currentAccount,Uri.fromFile(file));
        return true;
    }

    public static void pickChatAndSend(BaseFragment from,File file,String name){
        Bundle args=new Bundle();args.putBoolean("onlySelect",true);args.putInt("dialogsType",DialogsActivity.DIALOGS_TYPE_FORWARD);args.putBoolean("canSelectTopics",true);
        DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(from.getCurrentAccount());
        picker.setDelegate((fragment,dids,message,param,notify,schedule,repeat,topics)->{
            if(dids.isEmpty())return false;String caption="🎨 "+(name==null||name.isEmpty()?"Tetogram":name)+" — "+getString(R.string.TetogramAppearanceShareCaption);
            AccountInstance account=AccountInstance.getInstance(from.getCurrentAccount());
            for(MessagesStorage.TopicKey key:dids)SendMessagesHelper.prepareSendingDocument(account,file.getAbsolutePath(),file.getAbsolutePath(),null,caption,"application/zip",key.dialogId,null,null,null,null,null,notify,schedule,null,null,false);
            fragment.finishFragment();
            BulletinFactory.of(from).createSimpleBulletin(R.raw.forward,getString(R.string.TetogramAppearanceShared)).show();
            return true;
        });
        from.presentFragment(picker);
    }
}
