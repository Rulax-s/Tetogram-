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
    private AppearanceMediaView media;
    private FrameLayout scene;
    private View listTint;
    private int nativeGeneration=-1;
    private final int account;private int surface;
    private final String sectionId;
    private AppearanceConfig.Resolved currentStyle;
    private boolean currentDark;
    private AppearanceResources resources;
    public void updateSurface(AppearanceConfig.Resolved value,boolean dark,int surface){if(this.surface!=surface){this.surface=surface;currentStyle=null;}update(value,dark);}
    public void update(AppearanceConfig.Resolved value,boolean dark){
        if(nativeGeneration==ThemeBind.generation()&&currentStyle!=null&&currentDark==dark&&currentStyle.values.equals(value.values)&&currentStyle.origins.equals(value.origins))return;
        currentStyle=value;currentDark=dark;nativeGeneration=ThemeBind.generation();
        setBackground(AppearanceBackgrounds.colors(value,dark));if(getBackground()==null)setBackgroundColor(AppearanceRuntime.baseColor(dark,surface==1||surface==4||surface==5?Theme.key_windowBackgroundWhite:Theme.key_chat_wallpaper));
        String origin=value.origins.get("wallpaper.file");boolean rowOnly=(surface==1||surface==4||surface==5)&&origin!=null&&(origin.startsWith("chat:")||origin.startsWith("topic:"));
        media.configure(UserConfig.getInstance(account).getClientUserId(),value);media.setVisibility(!rowOnly&&AppearanceConfig.isMediaName(value.get("wallpaper.file"))?VISIBLE:GONE);
        resources.update(value,dark);boolean flat=(surface==1||surface==4||surface==5)&&AppearanceListStyle.uniform(value);listTint.setBackgroundColor(flat?AppearanceListStyle.tint(resources.getColor(Theme.key_windowBackgroundWhite),value.number("list.rowOpacity",1)):0);scene.removeAllViews();profileAvatar=null;buildScene(value,dark);
    }
    private boolean resumed;
    private View profileAvatar;private float profileAlignment;
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);if(profileAvatar!=null)profileAvatar.setTranslationX(AppearanceProfileGeometry.horizontal(w,AndroidUtilities.dp(64),AndroidUtilities.dp(16),profileAlignment,1));}
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){return true;}
    public long positionMs(){return media==null?0:media.positionMs();}
    public void setViewport(float zoom,float panX,float panY){media.setViewport(zoom,panX,panY);}
    public float sourceWidth(){return media.sourceWidth();}
    public float sourceHeight(){return media.sourceHeight();}
    public void setPlaybackPaused(boolean paused){media.setPlaybackPaused(paused);}
    public void showScene(boolean visible){scene.setVisibility(visible?VISIBLE:GONE);listTint.setVisibility(visible?VISIBLE:GONE);}
    public void sceneInsets(int top,int bottom){scene.setPadding(0,top,0,bottom);}
    /** SimpleGlass samples the static cover, never reads back TextureView or opens another decoder. */
    public void captureForGlass(android.graphics.Canvas canvas,android.graphics.RectF position){int save=canvas.save();canvas.clipRect(position);if(getBackground()!=null)getBackground().draw(canvas);media.capture(canvas,position);if(scene.getVisibility()==VISIBLE){listTint.draw(canvas);scene.draw(canvas);}canvas.restoreToCount(save);}
    public void setResumed(boolean value){if(resumed==value)return;resumed=value;if(media!=null)media.setResumed(value);}
    public AppearancePreview(Context context,int account,AppearanceConfig.Resolved style,boolean dark,boolean dialogs){
        this(context,account,style,dark,dialogs?1:0);
    }
    public AppearancePreview(Context context,int account,AppearanceConfig.Resolved style,boolean dark,int surface){this(context,account,style,dark,surface,null);}
    public AppearancePreview(Context context,int account,AppearanceConfig.Resolved style,boolean dark,int surface,String sectionId){
        super(context);this.account=account;this.surface=surface;this.sectionId=sectionId;
        resources=new AppearanceResources(style,dark,null);media=new AppearanceMediaView(context);addView(media,LayoutHelper.createFrame(-1,-1));listTint=new View(context);addView(listTint,LayoutHelper.createFrame(-1,-1));scene=new FrameLayout(context);addView(scene,LayoutHelper.createFrame(-1,-1));update(style,dark);setOnTouchListener((v,e)->true);
    }
    private void buildScene(AppearanceConfig.Resolved style,boolean dark){
        Context context=getContext();boolean dialogs=surface==1||surface==4||surface==5;LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);Theme.createChatResources(context,false);Theme.createDialogsResources(context);scene.addView(content,LayoutHelper.createFrame(-1,-2));
        if(surface==4){SectionCircle circle=new SectionCircle(context,resources);SectionConfig.Section section=new SectionConfig.Section();section.id="preview";section.name=LocaleController.getString(R.string.TetogramAppearanceSections);section.iconType="emoji";section.icon="🎨";if(sectionId!=null){SectionConfig config=SectionsStore.load(account);for(SectionConfig.Section candidate:config.sections)if(sectionId.equals(candidate.id)){section=candidate;break;}}circle.preview(style,dark);circle.bind(UserConfig.getInstance(account).getClientUserId(),section,true);circle.count(3,true,true);content.addView(circle,new LinearLayout.LayoutParams(AndroidUtilities.dp(64),AndroidUtilities.dp(64)));}
        if(surface==5){org.telegram.ui.Components.ScrollSlidingTextTabStrip tabs=new org.telegram.ui.Components.ScrollSlidingTextTabStrip(context,resources);tabs.addTextTab(0,LocaleController.getString(R.string.TetogramAppearanceFolders));tabs.addTextTab(1,LocaleController.getString(R.string.TetogramPreviewName));tabs.finishAddingTabs();tabs.setEnabled(false);content.addView(tabs,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(44)));}
        if(surface==2){
            org.telegram.ui.ProfileActivity.AvatarImageView avatar=new org.telegram.ui.ProfileActivity.AvatarImageView(context);android.graphics.drawable.Drawable person=getResources().getDrawable(R.drawable.msg_openprofile).mutate();person.setTint(resources.getColor(Theme.key_profile_title));avatar.setImageDrawable(person);avatar.setRoundRadius(AndroidUtilities.dp(32));android.graphics.drawable.GradientDrawable circle=new android.graphics.drawable.GradientDrawable();circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);circle.setColor(resources.getColor(Theme.key_chats_actionBackground));avatar.setBackground(circle);profileAvatar=avatar;profileAlignment=style.number("banner.avatarAlignment",0);avatar.setTranslationX(AppearanceProfileGeometry.horizontal(getWidth(),AndroidUtilities.dp(64),AndroidUtilities.dp(16),profileAlignment,1));LinearLayout.LayoutParams avatarLayout=new LinearLayout.LayoutParams(AndroidUtilities.dp(64),AndroidUtilities.dp(64));avatarLayout.topMargin=AndroidUtilities.dp(12);content.addView(avatar,avatarLayout);
            android.widget.TextView title=new android.widget.TextView(context);title.setText(LocaleController.getString(R.string.TetogramEditorDemoName));title.setTextSize(18);title.setGravity(android.view.Gravity.CENTER);Integer text=style.color(dark,"banner.text");title.setTextColor(text==null?resources.getColor(Theme.key_profile_title):text);title.setPadding(AndroidUtilities.dp(16),AndroidUtilities.dp(8),AndroidUtilities.dp(16),0);content.addView(title,new LinearLayout.LayoutParams(-1,-2));
            android.widget.TextView subtitle=new android.widget.TextView(context);subtitle.setText(LocaleController.getString(R.string.TetogramEditorDemoProfile));subtitle.setTextSize(13);subtitle.setGravity(android.view.Gravity.CENTER);Integer secondary=style.color(dark,"banner.secondaryText");subtitle.setTextColor(secondary==null?resources.getColor(Theme.key_profile_status):secondary);subtitle.setPadding(AndroidUtilities.dp(16),AndroidUtilities.dp(4),AndroidUtilities.dp(16),AndroidUtilities.dp(8));content.addView(subtitle,new LinearLayout.LayoutParams(-1,-2));
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
