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
    private boolean resumed;
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){return true;}
    public long positionMs(){return media==null?0:media.positionMs();}
    public void setResumed(boolean value){if(resumed==value)return;resumed=value;if(media!=null)media.setResumed(value);}
    public AppearancePreview(Context context,int account,AppearanceConfig.Resolved style,boolean dark,boolean dialogs){
        this(context,account,style,dark,dialogs?1:0);
    }
    public AppearancePreview(Context context,int account,AppearanceConfig.Resolved style,boolean dark,int surface){this(context,account,style,dark,surface,null);}
    public AppearancePreview(Context context,int account,AppearanceConfig.Resolved style,boolean dark,int surface,String sectionId){
        super(context);boolean dialogs=surface==1||surface==4||surface==5;LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);Theme.createChatResources(context,false);Theme.createDialogsResources(context);
        AppearanceResources resources=new AppearanceResources(style,dark,null);
        setBackground( AppearanceBackgrounds.colors(style,dark));if(getBackground()==null)setBackgroundColor(resources.getColor(dialogs?Theme.key_windowBackgroundWhite:Theme.key_chat_wallpaper));
        String mediaOrigin=style.origins.get("wallpaper.file");boolean rowOnly=dialogs&&mediaOrigin!=null&&(mediaOrigin.startsWith("chat:")||mediaOrigin.startsWith("topic:"));
        if(!rowOnly&&AppearanceConfig.isMediaName(style.get("wallpaper.file"))){media=new AppearanceMediaView(context);media.configure(UserConfig.getInstance(account).getClientUserId(),style);addView(media,LayoutHelper.createFrame(-1,-1));}else media=null;
        addView(content,LayoutHelper.createFrame(-1,-2));
        if(surface==4){SectionCircle circle=new SectionCircle(context,resources);SectionConfig.Section section=new SectionConfig.Section();section.id="preview";section.name=LocaleController.getString(R.string.TetogramAppearanceSections);section.iconType="emoji";section.icon="🎨";if(sectionId!=null){SectionConfig config=SectionsStore.load(account);for(SectionConfig.Section candidate:config.sections)if(sectionId.equals(candidate.id)){section=candidate;break;}}circle.preview(style,dark);circle.bind(UserConfig.getInstance(account).getClientUserId(),section,true);circle.count(3,true,true);content.addView(circle,new LinearLayout.LayoutParams(AndroidUtilities.dp(64),AndroidUtilities.dp(64)));}
        if(surface==5){org.telegram.ui.Components.ScrollSlidingTextTabStrip tabs=new org.telegram.ui.Components.ScrollSlidingTextTabStrip(context,resources);tabs.addTextTab(0,LocaleController.getString(R.string.TetogramAppearanceFolders));tabs.addTextTab(1,LocaleController.getString(R.string.TetogramPreviewName));tabs.finishAddingTabs();tabs.setEnabled(false);content.addView(tabs,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(44)));}
        if(surface==2){
            org.telegram.ui.ProfileActivity.AvatarImageView avatar=new org.telegram.ui.ProfileActivity.AvatarImageView(context);org.telegram.ui.Components.AvatarDrawable placeholder=new org.telegram.ui.Components.AvatarDrawable();placeholder.setInfo(0,"Tetogram","Preview");avatar.setImageDrawable(placeholder);avatar.setRoundRadius(AndroidUtilities.dp(30));addView(avatar,LayoutHelper.createFrame(60,60,android.view.Gravity.TOP,24,50,0,0));
            SimpleTextView title=new SimpleTextView(context);title.setText("Tetogram");title.setTextSize(22);Integer text=style.color(dark,"banner.text");title.setTextColor(text==null?resources.getColor(Theme.key_profile_title):text);addView(title,LayoutHelper.createFrame(-1,36,android.view.Gravity.TOP,104,50,20,0));
            SimpleTextView subtitle=new SimpleTextView(context);subtitle.setText(LocaleController.getString(R.string.TetogramAppearanceLocal));subtitle.setTextSize(13);Integer secondary=style.color(dark,"banner.secondaryText");subtitle.setTextColor(secondary==null?resources.getColor(Theme.key_profile_status):secondary);addView(subtitle,LayoutHelper.createFrame(-1,32,android.view.Gravity.TOP,104,85,20,0));
        }else if(dialogs){for(int i=0;i<2;i++){DialogCell cell=new DialogCell(null,context,false,false,account,resources);DialogCell.CustomDialog d=new DialogCell.CustomDialog();cell.setTetogramAppearance(style);d.id=i+1;d.name=LocaleController.getString(R.string.TetogramPreviewName);d.message=LocaleController.getString(R.string.TetogramPreviewMessage);d.date=1700000000;d.unread_count=i==0?3:0;cell.setDialog(d);content.addView(cell,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(78)));cell.setTetogramAppearance(style);}}
        else for(int i=0;i<2;i++){
            TLRPC.TL_message m=new TLRPC.TL_message();m.id=i+1;m.date=1700000000+i*60;m.message=LocaleController.getString(i==0?R.string.TetogramPreviewMessage:R.string.TetogramPreviewAnswer);m.dialog_id=1;m.out=i==1;m.flags=259;m.media=new TLRPC.TL_messageMediaEmpty();m.peer_id=new TLRPC.TL_peerUser();m.peer_id.user_id=1;m.from_id=new TLRPC.TL_peerUser();m.from_id.user_id=m.out?UserConfig.getInstance(account).getClientUserId():1;
            if(surface==3){m.dialog_id=-1;TLRPC.TL_peerChat group=new TLRPC.TL_peerChat();group.chat_id=1;m.peer_id=group;}
            MessageObject object=new MessageObject(account,m,true,false);object.eventId=1;object.customName=LocaleController.getString(R.string.TetogramPreviewName);object.customAvatarDrawable=new org.telegram.ui.Components.AvatarDrawable();((org.telegram.ui.Components.AvatarDrawable)object.customAvatarDrawable).setInfo(0,"Tetogram","Preview");
            ChatMessageCell cell=new ChatMessageCell(context,account,false,null,resources);cell.isChat=surface==3;cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate(){});cell.setMessageObject(object,null,false,false,false);cell.setClickable(false);content.addView(cell,new LinearLayout.LayoutParams(-1,-2));
        }
        setOnTouchListener((v,e)->true); // The preview cannot send, open a real account or mutate a dialog.
    }
}
