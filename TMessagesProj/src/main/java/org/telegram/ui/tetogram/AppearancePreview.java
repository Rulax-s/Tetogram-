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
        // Wallpaper-only changes (slider drags) keep the existing message/list cells; only background and media update.
        java.util.Map<String,String> scene=sceneValues(value);boolean sameScene=lastScene!=null&&scene.equals(lastScene)&&currentDark==dark&&nativeGeneration==ThemeBind.generation()&&lastSceneSurface==surface;
        currentStyle=value;currentDark=dark;nativeGeneration=ThemeBind.generation();
        setBackground(AppearanceBackgrounds.colors(value,dark));if(getBackground()==null)setBackgroundColor(AppearanceRuntime.baseColor(dark,surface==1||surface==4||surface==5?Theme.key_windowBackgroundWhite:Theme.key_chat_wallpaper));
        String origin=value.origins.get("wallpaper.file");boolean rowOnly=(surface==1||surface==4||surface==5)&&origin!=null&&(origin.startsWith("chat:")||origin.startsWith("topic:"));
        media.configure(UserConfig.getInstance(account).getClientUserId(),value);media.setVisibility(!rowOnly&&AppearanceConfig.isMediaName(value.get("wallpaper.file"))?VISIBLE:GONE);
        if(sameScene)return;
        lastScene=scene;lastSceneSurface=surface;
        resources.update(value,dark);boolean flat=(surface==1||surface==4||surface==5)&&AppearanceListStyle.uniform(value);listTint.setBackgroundColor(flat?AppearanceListStyle.tint(resources.getColor(Theme.key_windowBackgroundWhite),value.number("list.rowOpacity",1)):0);this.scene.removeAllViews();profileAvatar=null;buildScene(value,dark);
    }
    private boolean resumed;
    private java.util.Map<String,String> lastScene;private int lastSceneSurface=-1;
    private static java.util.Map<String,String> sceneValues(AppearanceConfig.Resolved value){java.util.HashMap<String,String> out=new java.util.HashMap<>();for(java.util.Map.Entry<String,String> e:value.values.entrySet()){String k=e.getKey();if(k.startsWith("wallpaper.")||k.contains("chat_wallpaper"))continue;out.put(k,e.getValue());}return out;}
    // ---- Element picking (editor preview only) ----
    public interface ElementListener{void picked(String element);}
    private ElementListener elementListener;
    private long sampleSender;
    /** Incoming sample messages come from this sender so per-person colors show up; 0 keeps the neutral sample. */
    public void setSampleSender(long id){if(sampleSender==id)return;sampleSender=id;lastScene=null;if(currentStyle!=null){AppearanceConfig.Resolved style=currentStyle;currentStyle=null;update(style,currentDark);}}
    private String highlighted;
    private final java.util.ArrayList<View> regionViews=new java.util.ArrayList<>();
    private final java.util.ArrayList<String> regionNames=new java.util.ArrayList<>();
    private final android.graphics.Paint outline=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.RectF bounds=new android.graphics.RectF();
    private float downX,downY;
    /** Enables the chat header/input chrome and tap-to-pick; the preview still never opens real screens. */
    public void setElementListener(ElementListener listener){boolean changed=(elementListener==null)!=(listener==null);elementListener=listener;if(changed)lastScene=null;if(changed&&currentStyle!=null){AppearanceConfig.Resolved style=currentStyle;currentStyle=null;update(style,currentDark);}}
    public void setHighlighted(String element){if(java.util.Objects.equals(highlighted,element))return;highlighted=element;invalidate();}
    private void region(View view,String name){regionViews.add(view);regionNames.add(name);}
    private String defaultElement(){return surface==2?AppearanceElements.BANNER:surface==1||surface==4||surface==5?AppearanceElements.LIST_BACKGROUND:AppearanceElements.WALLPAPER;}
    private boolean boundsOf(View view,android.graphics.RectF out){
        if(view.getVisibility()!=VISIBLE||view.getWidth()==0)return false;
        float x=0,y=0;View v=view;while(v!=null&&v!=this){x+=v.getLeft()-v.getScrollX()+v.getTranslationX();y+=v.getTop()-v.getScrollY()+v.getTranslationY();android.view.ViewParent parent=v.getParent();v=parent instanceof View?(View)parent:null;}
        if(v!=this)return false;
        if(view instanceof ChatMessageCell){ChatMessageCell cell=(ChatMessageCell)view;int l=cell.getBackgroundDrawableLeft(),r=cell.getBackgroundDrawableRight(),t=cell.getBackgroundDrawableTop(),b=cell.getBackgroundDrawableBottom();if(r>l&&b>t){out.set(x+l,y+t,x+r,y+b);return true;}}
        out.set(x,y,x+view.getWidth(),y+view.getHeight());return true;
    }
    private String elementAt(float px,float py){for(int i=regionViews.size()-1;i>=0;i--)if(boundsOf(regionViews.get(i),bounds)&&bounds.contains(px,py))return regionNames.get(i);return defaultElement();}
    private boolean onPreviewTouch(android.view.MotionEvent e){
        if(elementListener==null)return true;
        int action=e.getActionMasked();
        if(action==android.view.MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();}
        else if(action==android.view.MotionEvent.ACTION_UP){float slop=android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();if(Math.abs(e.getX()-downX)<slop&&Math.abs(e.getY()-downY)<slop){String element=elementAt(e.getX(),e.getY());playSoundEffect(android.view.SoundEffectConstants.CLICK);elementListener.picked(element);}}
        return true;
    }
    @Override protected void dispatchDraw(android.graphics.Canvas canvas){
        super.dispatchDraw(canvas);
        if(highlighted==null||elementListener==null)return;
        boolean found=false;
        for(int i=0;i<regionViews.size();i++)if(highlighted.equals(regionNames.get(i))&&boundsOf(regionViews.get(i),bounds)){found=true;break;}
        if(!found){if(!highlighted.equals(defaultElement()))return;bounds.set(0,0,getWidth(),getHeight());bounds.inset(AndroidUtilities.dp(6),AndroidUtilities.dp(6));}
        else bounds.inset(-AndroidUtilities.dp(4),-AndroidUtilities.dp(4));
        float r=AndroidUtilities.dp(14);
        outline.setStyle(android.graphics.Paint.Style.STROKE);outline.setPathEffect(null);outline.setStrokeWidth(AndroidUtilities.dp(4));outline.setColor(0x66000000);canvas.drawRoundRect(bounds,r,r,outline);
        outline.setStrokeWidth(AndroidUtilities.dp(2));outline.setColor(0xffffffff);outline.setPathEffect(new android.graphics.DashPathEffect(new float[]{AndroidUtilities.dp(6),AndroidUtilities.dp(4)},0));canvas.drawRoundRect(bounds,r,r,outline);
    }
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
        resources=new AppearanceResources(style,dark,null);media=new AppearanceMediaView(context);addView(media,LayoutHelper.createFrame(-1,-1));listTint=new View(context);addView(listTint,LayoutHelper.createFrame(-1,-1));scene=new FrameLayout(context);addView(scene,LayoutHelper.createFrame(-1,-1));update(style,dark);setOnTouchListener((v,e)->onPreviewTouch(e));
    }
    private void buildScene(AppearanceConfig.Resolved style,boolean dark){
        Context context=getContext();boolean dialogs=surface==1||surface==4||surface==5;LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);Theme.createChatResources(context,false);Theme.createDialogsResources(context);
        regionViews.clear();regionNames.clear();
        boolean chatChrome=elementListener!=null&&!dialogs&&surface!=2;
        if(chatChrome){scene.addView(content,LayoutHelper.createFrame(-1,-2,android.view.Gravity.BOTTOM,0,52,0,60));chatHeader(context);chatInput(context);}
        else scene.addView(content,LayoutHelper.createFrame(-1,-2));
        if(surface==4){SectionCircle circle=new SectionCircle(context,resources);SectionConfig.Section section=new SectionConfig.Section();section.id="preview";section.name=LocaleController.getString(R.string.TetogramAppearanceSections);section.iconType="emoji";section.icon="🎨";if(sectionId!=null){SectionConfig config=SectionsStore.load(account);for(SectionConfig.Section candidate:config.sections)if(sectionId.equals(candidate.id)){section=candidate;break;}}circle.preview(style,dark);circle.bind(UserConfig.getInstance(account).getClientUserId(),section,true);circle.count(3,true,true);content.addView(circle,new LinearLayout.LayoutParams(AndroidUtilities.dp(64),AndroidUtilities.dp(64)));region(circle,AppearanceElements.SECTION);}
        if(surface==5){org.telegram.ui.Components.ScrollSlidingTextTabStrip tabs=new org.telegram.ui.Components.ScrollSlidingTextTabStrip(context,resources);tabs.addTextTab(0,LocaleController.getString(R.string.TetogramAppearanceFolders));tabs.addTextTab(1,LocaleController.getString(R.string.TetogramPreviewName));tabs.finishAddingTabs();tabs.setEnabled(false);content.addView(tabs,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(44)));}
        if(surface==2){
            org.telegram.ui.ProfileActivity.AvatarImageView avatar=new org.telegram.ui.ProfileActivity.AvatarImageView(context);android.graphics.drawable.Drawable person=getResources().getDrawable(R.drawable.msg_openprofile).mutate();person.setTint(resources.getColor(Theme.key_profile_title));avatar.setImageDrawable(person);avatar.setRoundRadius(AndroidUtilities.dp(32));android.graphics.drawable.GradientDrawable circle=new android.graphics.drawable.GradientDrawable();circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);circle.setColor(resources.getColor(Theme.key_chats_actionBackground));avatar.setBackground(circle);profileAvatar=avatar;profileAlignment=style.number("banner.avatarAlignment",0);avatar.setTranslationX(AppearanceProfileGeometry.horizontal(getWidth(),AndroidUtilities.dp(64),AndroidUtilities.dp(16),profileAlignment,1));LinearLayout.LayoutParams avatarLayout=new LinearLayout.LayoutParams(AndroidUtilities.dp(64),AndroidUtilities.dp(64));avatarLayout.topMargin=AndroidUtilities.dp(12);content.addView(avatar,avatarLayout);
            android.widget.TextView title=new android.widget.TextView(context);title.setText(LocaleController.getString(R.string.TetogramEditorDemoName));title.setTextSize(18);title.setGravity(android.view.Gravity.CENTER);Integer text=style.color(dark,"banner.text");title.setTextColor(text==null?resources.getColor(Theme.key_profile_title):text);title.setPadding(AndroidUtilities.dp(16),AndroidUtilities.dp(8),AndroidUtilities.dp(16),0);content.addView(title,new LinearLayout.LayoutParams(-1,-2));region(title,AppearanceElements.PROFILE_NAME);
            android.widget.TextView subtitle=new android.widget.TextView(context);subtitle.setText(LocaleController.getString(R.string.TetogramEditorDemoProfile));subtitle.setTextSize(13);subtitle.setGravity(android.view.Gravity.CENTER);Integer secondary=style.color(dark,"banner.secondaryText");subtitle.setTextColor(secondary==null?resources.getColor(Theme.key_profile_status):secondary);subtitle.setPadding(AndroidUtilities.dp(16),AndroidUtilities.dp(4),AndroidUtilities.dp(16),AndroidUtilities.dp(8));content.addView(subtitle,new LinearLayout.LayoutParams(-1,-2));region(subtitle,AppearanceElements.PROFILE_STATUS);
        }else if(dialogs){for(int i=0;i<2;i++){DialogCell cell=new DialogCell(null,context,false,false,account,resources);DialogCell.CustomDialog d=new DialogCell.CustomDialog();cell.setTetogramAppearance(style);d.id=i+1;d.name=LocaleController.getString(R.string.TetogramPreviewName);d.message=LocaleController.getString(R.string.TetogramPreviewMessage);d.date=1700000000;d.unread_count=i==0?3:0;cell.setDialog(d);content.addView(cell,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(78)));cell.setTetogramAppearance(style);region(cell,AppearanceElements.LIST_ROW);}}
        else for(int i=0;i<2;i++){
            TLRPC.TL_message m=new TLRPC.TL_message();m.id=i+1;m.date=1700000000+i*60;m.message=LocaleController.getString(i==0?R.string.TetogramPreviewMessage:R.string.TetogramPreviewAnswer);m.dialog_id=1;m.out=i==1;m.flags=259;m.media=new TLRPC.TL_messageMediaEmpty();m.peer_id=new TLRPC.TL_peerUser();m.peer_id.user_id=1;if(!m.out&&sampleSender<0){TLRPC.TL_peerChannel peer=new TLRPC.TL_peerChannel();peer.channel_id=-sampleSender;m.from_id=peer;}else{m.from_id=new TLRPC.TL_peerUser();m.from_id.user_id=m.out?UserConfig.getInstance(account).getClientUserId():sampleSender>0?sampleSender:1;}
            if(surface==3){m.dialog_id=-1;TLRPC.TL_peerChat group=new TLRPC.TL_peerChat();group.chat_id=1;m.peer_id=group;}
            MessageObject object=new MessageObject(account,m,true,false);object.eventId=1;object.customName=LocaleController.getString(R.string.TetogramPreviewName);object.customAvatarDrawable=new org.telegram.ui.Components.AvatarDrawable();((org.telegram.ui.Components.AvatarDrawable)object.customAvatarDrawable).setInfo(0,"Tetogram","Preview");
            ChatMessageCell cell=new ChatMessageCell(context,account,false,null,resources);cell.isChat=surface==3;cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate(){});cell.setMessageObject(object,null,false,false,false);cell.setClickable(false);content.addView(cell,new LinearLayout.LayoutParams(-1,-2));region(cell,m.out?AppearanceElements.OUTGOING:AppearanceElements.INCOMING);
        }
        setOnTouchListener((v,e)->onPreviewTouch(e)); // The preview cannot send, open a real account or mutate a dialog.
    }
    /** Synthetic chat header: colors come from the same isolated resources as the bubbles. */
    private void chatHeader(Context context){
        LinearLayout header=new LinearLayout(context);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(android.view.Gravity.CENTER_VERTICAL);header.setPadding(AndroidUtilities.dp(10),0,AndroidUtilities.dp(10),0);header.setBackgroundColor(resources.getColor(Theme.key_actionBarDefault));
        android.widget.ImageView back=new android.widget.ImageView(context);back.setImageResource(R.drawable.ic_ab_back);back.setColorFilter(new android.graphics.PorterDuffColorFilter(resources.getColor(Theme.key_actionBarDefaultIcon),android.graphics.PorterDuff.Mode.SRC_IN));header.addView(back,new LinearLayout.LayoutParams(AndroidUtilities.dp(28),AndroidUtilities.dp(28)));
        org.telegram.ui.Components.BackupImageView avatar=new org.telegram.ui.Components.BackupImageView(context);avatar.setRoundRadius(AndroidUtilities.dp(17));org.telegram.ui.Components.AvatarDrawable drawable=new org.telegram.ui.Components.AvatarDrawable();drawable.setInfo(0,LocaleController.getString(R.string.TetogramPreviewName),null);avatar.setImageDrawable(drawable);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(AndroidUtilities.dp(34),AndroidUtilities.dp(34));ap.leftMargin=AndroidUtilities.dp(10);header.addView(avatar,ap);
        LinearLayout titles=new LinearLayout(context);titles.setOrientation(LinearLayout.VERTICAL);
        android.widget.TextView name=new android.widget.TextView(context);name.setText(LocaleController.getString(R.string.TetogramPreviewName));name.setTextSize(15);name.setTypeface(AndroidUtilities.bold());name.setSingleLine(true);name.setTextColor(resources.getColor(Theme.key_actionBarDefaultTitle));titles.addView(name);
        android.widget.TextView status=new android.widget.TextView(context);status.setText(LocaleController.getString(R.string.Online));status.setTextSize(12);status.setSingleLine(true);status.setTextColor(resources.getColor(Theme.key_actionBarDefaultSubtitle));titles.addView(status);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=AndroidUtilities.dp(10);header.addView(titles,tp);
        scene.addView(header,LayoutHelper.createFrame(-1,52,android.view.Gravity.TOP));region(header,AppearanceElements.HEADER);
    }
    /** Synthetic message input; it is drawn only, never focusable. */
    private void chatInput(Context context){
        android.widget.TextView input=new android.widget.TextView(context);input.setText(LocaleController.getString(R.string.TypeMessage));input.setTextSize(15);input.setSingleLine(true);input.setGravity(android.view.Gravity.CENTER_VERTICAL);input.setPadding(AndroidUtilities.dp(16),0,AndroidUtilities.dp(16),0);input.setTextColor(resources.getColor(Theme.key_chat_messagePanelHint));input.setFocusable(false);
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22),resources.getColor(Theme.key_chat_messagePanelBackground)));
        scene.addView(input,LayoutHelper.createFrame(-1,44,android.view.Gravity.BOTTOM,8,0,8,8));region(input,AppearanceElements.INPUT);
    }
}
