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
import org.telegram.messenger.tetogram.AppearanceConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;

public final class AppearanceShare {
    private AppearanceShare(){}
    public static final String EXTENSION=".tetotheme";

    private static final java.util.concurrent.ConcurrentHashMap<String,android.graphics.Bitmap> COVERS=new java.util.concurrent.ConcurrentHashMap<>();
    /** A copy of the preview picture for the file being sent (the sender recycles what it gets). */
    public static android.graphics.Bitmap cover(String path){android.graphics.Bitmap b=COVERS.get(path);return b==null||b.isRecycled()?null:b.copy(android.graphics.Bitmap.Config.ARGB_8888,false);}
    public static void setCover(String path,android.graphics.Bitmap bitmap){android.graphics.Bitmap old=bitmap==null?COVERS.remove(path):COVERS.put(path,bitmap);if(old!=null&&old!=bitmap)old.recycle();}

    /** Renders the preset in a small off-screen chat preview and returns it as a picture. */
    public static void renderCover(BaseFragment f,AppearanceConfig.Resolved style,boolean dark,java.util.function.Consumer<android.graphics.Bitmap> done){
        android.view.View root=f.getFragmentView();if(!(root instanceof android.widget.FrameLayout)||f.getParentActivity()==null){done.accept(null);return;}
        int w=org.telegram.messenger.AndroidUtilities.dp(300),h=org.telegram.messenger.AndroidUtilities.dp(420);
        AppearancePreview preview=new AppearancePreview(f.getParentActivity(),f.getCurrentAccount(),style,dark,0);
        preview.setTranslationX(-w*4);((android.widget.FrameLayout)root).addView(preview,new android.widget.FrameLayout.LayoutParams(w,h));
        preview.setResumed(true);
        org.telegram.messenger.AndroidUtilities.runOnUIThread(()->{
            android.graphics.Bitmap bitmap=null;
            try{if(preview.getWidth()>0&&preview.getHeight()>0){float scale=Math.min(1f,480f/preview.getWidth());bitmap=android.graphics.Bitmap.createBitmap(Math.round(preview.getWidth()*scale),Math.round(preview.getHeight()*scale),android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas c=new android.graphics.Canvas(bitmap);c.scale(scale,scale);preview.draw(c);}}
            catch(Throwable e){org.telegram.messenger.FileLog.e("Tetogram appearance cover failed");bitmap=null;}
            preview.setResumed(false);((android.widget.FrameLayout)root).removeView(preview);
            done.accept(bitmap);
        },900);
    }

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
