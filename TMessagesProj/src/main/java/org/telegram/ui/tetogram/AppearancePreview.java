/* Tetogram, 2026. GNU GPL v2 or later. Native cells with synthetic, never sent preview messages. */
package org.telegram.ui.tetogram;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Cells.*;
import org.telegram.ui.Components.LayoutHelper;

public final class AppearancePreview extends FrameLayout {
    private final AppearanceMediaView media;
    public long positionMs(){return media==null?0:media.positionMs();}
    public void setResumed(boolean value){if(media!=null)media.setResumed(value);}
    public AppearancePreview(Context context,int account,AppearanceConfig.Resolved style,boolean dark,boolean dialogs){
        super(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);Theme.createChatResources(context,false);Theme.createDialogsResources(context);
        AppearanceResources resources=new AppearanceResources(style,dark,null);
        setBackground( AppearanceBackgrounds.colors(style,dark));if(getBackground()==null)setBackgroundColor(resources.getColor(dialogs?Theme.key_windowBackgroundWhite:Theme.key_chat_wallpaper));
        String mediaOrigin=style.origins.get("wallpaper.file");boolean rowOnly=dialogs&&mediaOrigin!=null&&(mediaOrigin.startsWith("chat:")||mediaOrigin.startsWith("topic:"));
        if(!rowOnly&&AppearanceConfig.isMediaName(style.get("wallpaper.file"))){media=new AppearanceMediaView(context);media.configure(UserConfig.getInstance(account).getClientUserId(),style);addView(media,LayoutHelper.createFrame(-1,-1));}else media=null;
        addView(content,LayoutHelper.createFrame(-1,-2));
        if(dialogs){for(int i=0;i<2;i++){DialogCell cell=new DialogCell(null,context,false,false,account,resources);DialogCell.CustomDialog d=new DialogCell.CustomDialog();cell.setTetogramAppearance(style);d.id=i+1;d.name=LocaleController.getString(R.string.TetogramPreviewName);d.message=LocaleController.getString(R.string.TetogramPreviewMessage);d.date=1700000000;d.unread_count=i==0?3:0;cell.setDialog(d);content.addView(cell,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(78)));cell.setTetogramAppearance(style);}}
        else for(int i=0;i<2;i++){
            TLRPC.TL_message m=new TLRPC.TL_message();m.id=i+1;m.date=1700000000+i*60;m.message=LocaleController.getString(i==0?R.string.TetogramPreviewMessage:R.string.TetogramPreviewAnswer);m.dialog_id=1;m.out=i==1;m.flags=259;m.media=new TLRPC.TL_messageMediaEmpty();m.peer_id=new TLRPC.TL_peerUser();m.peer_id.user_id=1;m.from_id=new TLRPC.TL_peerUser();m.from_id.user_id=m.out?UserConfig.getInstance(account).getClientUserId():1;
            MessageObject object=new MessageObject(account,m,true,false);object.eventId=1;
            ChatMessageCell cell=new ChatMessageCell(context,account,false,null,resources);cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate(){});cell.setMessageObject(object,null,false,false,false);cell.setClickable(false);content.addView(cell,new LinearLayout.LayoutParams(-1,-2));
        }
        setOnTouchListener((v,e)->true); // The preview cannot send, open a real account or mutate a dialog.
    }
}
