/* Tetogram, 2026. GNU GPL v2 or later. Drafts are local until Apply. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.os.Build;
import android.provider.MediaStore;
import java.io.File;
import android.graphics.Color;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.LinearLayout;
import java.util.*;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.*;

public final class AppearanceActivity extends LabsPage {
    private final long owner;
    private final String page,scope,section,folder;
    private final long dialog,topic;
    private AppearanceConfig config;
    private AppearanceConfig.Style draft;
    private String editingPresetId;
    private long expectedRevision;
    private boolean failed,saving,destroyed,advanced,previewDark;
    private String search="";
    private final ArrayList<Runnable> actions=new ArrayList<>();
    private final HashMap<String,String> scopeNames=new HashMap<>();
    private final ArrayDeque<AppearanceConfig.Style> undo=new ArrayDeque<>();
    private EditTextBoldCursor searchInput;
    private AppearancePreview preview;
    private AppearanceConfig.Resolved previewResolved;
    private int lastPreviewKind;private boolean lastPreviewDark,filterParameters;
    private boolean foreground,mediaBusy,previewDialogs,effectsPage;
    private int previewKind;
    private static final int PICK_APPEARANCE_MEDIA=61041,IMPORT_PRESET=61042,EXPORT_PRESET=61043,SAMPLE_COLOR=61045;
    private String sampledRole;
    private AppearanceConfig.Preset exporting;private boolean exportImages,exportVideos;
    private boolean pickingVideo;
    private AppearanceMedia.Job mediaJob;
    private final ArrayList<File> pendingMedia=new ArrayList<>();
    public AppearanceActivity(int account){this(account,"home",AppearanceConfig.GLOBAL,null,null,0,0);}
    public AppearanceActivity(int account,String scope,String section,String folder,long dialog,long topic){this(account,"edit",scope,section,folder,dialog,topic);}
    private AppearanceActivity(int account,String page,String scope,String section,String folder,long dialog,long topic){super(account);this.owner=UserConfig.getInstance(account).getClientUserId();this.page=page;this.scope=scope;this.section=section;this.folder=folder;this.dialog=dialog;this.topic=topic;previewKind=scope.startsWith("profile:")?2:section!=null?4:folder!=null?5:dialog<0?3:0;previewDialogs=previewKind==4||previewKind==5;previewDark=Theme.isCurrentThemeDark();}
    @Override protected CharSequence title(){return getString(R.string.TetogramAppearance);}
    private boolean valid(){return !destroyed && owner>0 && owner==getUserConfig().getClientUserId();}
    @Override public View createView(Context context){
        if(preview!=null)preview.setResumed(false);preview=null;previewResolved=null;
        if("edit".equals(page)){
            searchInput=new EditTextBoldCursor(context);searchInput.setSingleLine(true);searchInput.setTextSize(16);searchInput.setHint(getString(R.string.Search));searchInput.setPadding(AndroidUtilities.dp(16),0,AndroidUtilities.dp(16),0);ThemeBind.textColor(searchInput,Theme.key_windowBackgroundWhiteBlackText);ThemeBind.hintTextColor(searchInput,Theme.key_windowBackgroundWhiteHintText);
            searchInput.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){search=s.toString();refresh();}});
        }
        return super.createView(context);
    }
    @Override public void onResume(){super.onResume();foreground=true;if(preview!=null)preview.setResumed(!mediaBusy);if(!valid())return;AppearanceStore.load(currentAccount,(ok,c)->{if(!valid())return;failed=!ok;if(draft==null || !"edit".equals(page)){config=c;expectedRevision=c.revision;draft=c.edit(scope).copy();}refresh();});}
    @Override public void onPause(){foreground=false;if(preview!=null)preview.setResumed(false);super.onPause();}
    @Override public void onFragmentDestroy(){destroyed=true;if(mediaJob!=null)mediaJob.cancel();if(preview!=null)preview.setResumed(false);for(File f:pendingMedia)AppearanceMedia.discard(f);pendingMedia.clear();super.onFragmentDestroy();}
    private void refresh(){if(list!=null)list.adapter.update(false);}
    private void button(ArrayList<UItem> items,CharSequence title,CharSequence value,Runnable action){if(filterParameters&&!search.isEmpty()){String query=search.toLowerCase(java.util.Locale.ROOT);if(!title.toString().toLowerCase(java.util.Locale.ROOT).contains(query)&&(value==null||!value.toString().toLowerCase(java.util.Locale.ROOT).contains(query)))return;}actions.add(action);items.add(UItem.asButton(actions.size(),title,value).setEnabled(!saving&&!mediaBusy));}
    private void button(ArrayList<UItem> items,CharSequence title,Runnable action){button(items,title,null,action);}
    private void checkpoint(){undo.push(draft.copy());}
    private AppearanceConfig.Resolved resolved(){AppearanceConfig.Style old=config.scopes.put(scope,draft);try{return scope.startsWith("profile:")?config.resolveProfile(Long.parseLong(scope.substring(8))):config.resolve(new AppearanceConfig.Context(section,folder,dialog,topic));}finally{if(old==null)config.scopes.remove(scope);else config.scopes.put(scope,old);}}
    private void editScope(String key,String section,String folder,long did,long tid){presentFragment(new AppearanceActivity(currentAccount,key,section,folder,did,tid));}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        actions.clear();filterParameters=false;if(!valid())return;
        if(failed){items.add(UItem.asShadow(getString(R.string.TetogramAppearanceStorageError)));return;}
        if(config==null){items.add(UItem.asShadow(getString(R.string.Loading)));return;}
        prepareScopeNames();
        if("home".equals(page)){
            items.add(UItem.asShadow(getString(R.string.TetogramAppearanceLocal)));
            button(items,getString(R.string.TetogramAppearancePresets),()->presentFragment(new AppearanceActivity(currentAccount,"presets",AppearanceConfig.GLOBAL,null,null,0,0)));
            button(items,getString(R.string.TetogramAppearanceWallpapers),()->editScope(AppearanceConfig.GLOBAL,null,null,0,0));
            button(items,getString(R.string.TetogramAppearanceSimple),()->editScope(AppearanceConfig.GLOBAL,null,null,0,0));
            button(items,getString(R.string.TetogramAppearanceInterfaceColors),()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,AppearanceConfig.GLOBAL,null,null,0,0);editor.advanced=true;presentFragment(editor);});
            button(items,getString(R.string.TetogramAppearanceNativeThemes),()->presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
            button(items,getString(R.string.TetogramAppearanceChat),()->pickChat());
            button(items,getString(R.string.TetogramAppearanceGroups),()->pickChat(true));
            button(items,getString(R.string.TetogramAppearanceDialogList),()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,AppearanceConfig.GLOBAL,null,null,0,0);editor.previewDialogs=true;editor.previewKind=1;presentFragment(editor);});
            button(items,getString(R.string.TetogramAppearanceSections),()->pickSection(false));
            button(items,getString(R.string.TetogramAppearanceFolders),()->pickSection(true));
            button(items,getString(R.string.TetogramAppearanceProfile),()->editScope("profile:"+owner,null,null,0,0));
            button(items,getString(R.string.TetogramAppearanceLauncher),()->presentFragment(new AppearanceLauncherActivity(currentAccount)));
            button(items,getString(R.string.TetogramAppearanceEffects),()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,AppearanceConfig.GLOBAL,null,null,0,0);editor.effectsPage=true;presentFragment(editor);});
            button(items,getString(R.string.TetogramAppearanceImport),this::importPreset);
            button(items,getString(R.string.TetogramAppearanceExport),()->{AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name="Tetogram";p.style=config.edit(AppearanceConfig.GLOBAL).copy();exportPreset(p);});
            button(items,getString(R.string.TetogramAppearanceReset),()->confirmReset());
            return;
        }
        if("presets".equals(page)){
            button(items,getString(R.string.TetogramAppearanceCreatePreset),()->input(getString(R.string.TetogramAppearancePresetName),"",name->{AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name=name;p.style=config.edit(AppearanceConfig.GLOBAL).copy();config.presets.put(p.id,p);saveConfig(()->{});}));
            button(items,getString(R.string.TetogramAppearanceImport),this::importPreset);
            items.add(UItem.asHeader(getString(R.string.TetogramAppearanceBuiltins)));
            for(AppearanceConfig.Preset p:AppearancePresets.builtins())button(items,p.name,()->presetActions(p,true));
            items.add(UItem.asHeader(getString(R.string.TetogramAppearanceUserPresets)));
            for(AppearanceConfig.Preset p:config.presets.values())button(items,p.name,()->presetActions(p,false));
            return;
        }
        items.add(UItem.asShadow(getString(editingPresetId==null?R.string.TetogramAppearanceDraft:R.string.TetogramAppearancePresetDraft)+"\n"+(editingPresetId==null?scopeLabel(scope):config.presets.get(editingPresetId).name)));
        button(items,getString(editingPresetId==null?R.string.TetogramAppearanceApply:R.string.Save),this::applyDraft);
        button(items,getString(R.string.Cancel),this::finishFragment);
        button(items,getString(R.string.TetogramAppearanceUndo),()->{if(!undo.isEmpty()){draft=undo.pop();refresh();}});
        button(items,getString(R.string.TetogramAppearanceSavePreset),()->input(getString(R.string.TetogramAppearancePresetName),"",name->{AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name=name;p.style=draft.copy();config.presets.put(p.id,p);saveConfig(()->refresh());}));
        button(items,getString(R.string.TetogramAppearancePresets),()->{ArrayList<AppearanceConfig.Preset> all=new ArrayList<>(AppearancePresets.builtins());all.addAll(config.presets.values());CharSequence[] names=new CharSequence[all.size()];for(int i=0;i<all.size();i++)names[i]=all.get(i).name;showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearancePresets)).setItems(names,(d,n)->{checkpoint();draft=all.get(n).style.copy();previewDark="dark".equals(draft.values.get("mode"));refresh();}).create());});
        button(items,getString(scope.startsWith("profile:")?R.string.TetogramAppearanceBannerMode:R.string.TetogramAppearanceDisplayMode),draft.values.getOrDefault("mode",getString(R.string.TetogramAppearanceInherited)),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceDisplayMode)).setItems(new CharSequence[]{getString(R.string.TetogramAppearanceLight),getString(R.string.TetogramAppearanceDark),getString(R.string.TetogramAppearanceInherit)},(d,n)->{checkpoint();if(n==2)draft.values.remove("mode");else draft.set("mode",n==0?"light":"dark");previewDark=n==1;refresh();}).create()));
        button(items,getString(R.string.TetogramAppearanceMode),previewDark?getString(R.string.TetogramAppearanceDark):getString(R.string.TetogramAppearanceLight),()->{previewDark=!previewDark;refresh();});
        button(items,getString(R.string.TetogramAppearanceAdvanced),advanced?getString(R.string.TetogramAppearanceAdvanced):getString(R.string.TetogramAppearanceSimple),()->{advanced=!advanced;refresh();});
        button(items,getString(R.string.TetogramAppearancePreviewSurface),getString(surfaceLabel(previewKind)),this::choosePreview);
        AppearanceConfig.Resolved previewValue=previewStyle();if(preview==null||lastPreviewKind!=previewKind||lastPreviewDark!=previewDark||!previewResolved.values.equals(previewValue.values)||!previewResolved.origins.equals(previewValue.origins)){if(preview!=null)preview.setResumed(false);preview=new AppearancePreview(getContext(),currentAccount,previewValue,previewDark,previewKind,section);previewResolved=previewValue;lastPreviewKind=previewKind;lastPreviewDark=previewDark;}preview.setResumed(valid()&&foreground&&!mediaBusy);items.add(UItem.asCustom(preview,240));
        if(mediaBusy)items.add(UItem.asShadow(getString(R.string.TetogramAppearancePreparingMedia)));
        if(searchInput!=null)items.add(UItem.asCustom(searchInput,48));filterParameters=true;
        if(effectsPage){items.add(UItem.asHeader(getString(R.string.TetogramAppearanceEffects)));toggle(items,"effects.enabled",getString(R.string.TetogramAppearanceAnimationsEnabled),true);number(items,"effects.durationScale",getString(R.string.TetogramAppearanceTransitionDuration),.25f,2);toggle(items,"effects.reduceOnBattery",getString(R.string.TetogramAppearanceReduceOnBattery),true);button(items,getString(R.string.TetogramAppearanceNativeEffects),()->presentFragment(new LiteModeSettingsActivity()));button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();for(String key:new ArrayList<>(draft.values.keySet()))if(key.startsWith("effects."))draft.values.remove(key);refresh();});return;}
        if(!advanced){simpleSettings(items);return;}
        mediaControls(items);
        if(previewKind==2){AppearanceConfig.Resolved banner=resolved();for(String role:new String[]{"banner.text","banner.secondaryText"}){Integer current=banner.color(previewDark,role);button(items,getString(role.equals("banner.text")?R.string.TetogramAppearanceMainText:R.string.TetogramAppearanceHeaderSubtitle),()->colorDialog(role,current==null?AppearanceRuntime.baseColor(previewDark,role.equals("banner.text")?Theme.key_profile_title:Theme.key_profile_status):current));}number(items,"banner.height",getString(R.string.TetogramAppearanceBannerHeight),120,480);number(items,"banner.radius",getString(R.string.TetogramAppearanceRowRadius),0,40);number(items,"banner.parallax",getString(R.string.TetogramAppearanceBannerParallax),0,1);number(items,"banner.avatarAlignment",getString(R.string.TetogramAppearanceAvatarAlignment),-1,1);button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();draft=new AppearanceConfig.Style();refresh();});return;}
        String[] simple=previewDialogs?new String[]{"list.chat_wallpaper","list.windowBackgroundWhite","list.chats_name","list.chats_message","list.chats_date","list.chats_unreadCounter","list.chats_unreadCounterText","list.actionBarDefault"}:new String[]{"chat_wallpaper","chat_inBubble","chat_outBubble","chat_messageTextIn","chat_messageTextOut","actionBarDefault","chats_name","chats_unreadCounter"};
        ArrayList<String> keys=new ArrayList<>();
        if(advanced){for(int key=0;key<Theme.colorsCount;key++){String name=ThemeColors.getStringName(key);if(name!=null && !name.equals("wallpaperFileOffset") && !name.contains("rotation") && !name.toLowerCase(java.util.Locale.ROOT).contains("animated")&&(!previewDialogs||listColor(name))&&(scope.equals(AppearanceConfig.GLOBAL)||previewDialogs||name.startsWith("chat_")||name.startsWith("actionBar")||name.startsWith("windowBackground")||name.startsWith("dialog")))keys.add(previewDialogs?"list."+name:name);}keys.sort((a,b)->{int c=Integer.compare(colorCategory(a),colorCategory(b));return c!=0?c:a.compareTo(b);});}else Collections.addAll(keys,simple);
        if(previewDialogs&&!keys.contains("list.chat_wallpaper"))keys.add("list.chat_wallpaper");
        String prefix=previewDark?"dark.":"light.";AppearanceConfig.Resolved r=resolved();int lastCategory=-1;
        for(String name:keys){if(!search.isEmpty() && !name.toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))&&!colorLabel(name).toString().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT)))continue;
            String key=prefix+name;int nativeKey=ThemeColors.stringKeyToInt(name.startsWith("list.")?name.substring(5):name);Integer custom=r.color(previewDark,name);int color=custom==null?Theme.getColor(nativeKey):custom;
            String origin=draft.values.containsKey(key)?getString(R.string.TetogramAppearanceExplicit):getString(R.string.TetogramAppearanceInherited)+" · "+scopeLabel(r.origins.getOrDefault(key,"Telegram"));
            int category=colorCategory(name);if(category!=lastCategory){items.add(UItem.asHeader(getString(category)));lastCategory=category;}
            button(items,colorLabel(name),String.format(java.util.Locale.ROOT,"#%08X",color)+" · "+origin+(advanced?" · "+name:""),()->colorDialog(name,color));
        }
        if(dialog<0||scope.equals(AppearanceConfig.GLOBAL)||scope.startsWith("section:")||scope.startsWith("folder:")){Integer name=r.color(previewDark,"group.senderName");button(items,getString(R.string.TetogramAppearanceSenderNames),()->colorDialog("group.senderName",name==null?Theme.getColor(Theme.key_chat_inForwardedNameText):name));}
        button(items,getString(R.string.TetogramAppearanceRestoreContrast),this::restoreContrast);
        if(previewDialogs){
            number(items,"list.navigationOpacity",getString(R.string.TetogramAppearanceNavigationOpacity),0,1);
            number(items,"list.rowOpacity",getString(R.string.TetogramAppearanceRowOpacity),0,1);
            number(items,"list.radius",getString(R.string.TetogramAppearanceRowRadius),0,32);
            number(items,"list.sideInset",getString(R.string.TetogramAppearanceSideInset),0,24);
            number(items,"list.rowGap",getString(R.string.TetogramAppearanceRowGap),0,20);
            number(items,"list.avatarSize",getString(R.string.TetogramAppearanceAvatarSize),24,SharedConfig.useThreeLinesLayout?56:52);
            number(items,"list.outlineWidth",getString(R.string.TetogramAppearanceOutlineWidth),0,4);
            toggle(items,"list.separators",getString(R.string.TetogramAppearanceSeparators),true);
            Integer outline=resolved().color(previewDark,"list.outline");button(items,getString(R.string.TetogramAppearanceOutlineColor),()->colorDialog("list.outline",outline==null?Theme.getColor(Theme.key_chats_unreadCounter):outline));
        }
        if(scope.equals(AppearanceConfig.GLOBAL)||scope.startsWith("section:")){
            items.add(UItem.asHeader(getString(R.string.TetogramAppearanceCircleStyle)));
            SectionCircle circle=new SectionCircle(getContext(),getResourceProvider());
            SectionConfig.Section circleSection=new SectionConfig.Section();circleSection.id=section==null?"preview":section;circleSection.name=getString(R.string.TetogramAppearanceSections);circleSection.iconType="emoji";circleSection.icon="🎨";
            if(section!=null){SectionConfig sections=SectionsStore.load(currentAccount);for(SectionConfig.Section existing:sections.sections)if(section.equals(existing.id)){circleSection=existing;break;}}
            circle.preview(r,previewDark);circle.bind(owner,circleSection,true);circle.count(12,true,true);items.add(UItem.asCustom(circle,76));
            String[] circleKeys={"section.background","section.activeBackground","section.text","section.activeText","section.outline","section.activeOutline","section.badge","section.badgeText"};
            int[] circleLabels={R.string.TetogramAppearanceCircleBackground,R.string.TetogramAppearanceCircleActive,R.string.TetogramAppearanceCircleText,R.string.TetogramAppearanceCircleActiveText,R.string.TetogramAppearanceCircleOutline,R.string.TetogramAppearanceCircleActiveOutline,R.string.TetogramAppearanceCircleBadge,R.string.TetogramAppearanceCircleBadgeText};
            for(int index=0;index<circleKeys.length;index++){String key=circleKeys[index];Integer value=r.color(previewDark,key);button(items,getString(circleLabels[index]),()->colorDialog(key,value==null?Theme.getColor(Theme.key_chats_actionBackground):value));}
            number(items,"section.outlineWidth",getString(R.string.TetogramAppearanceOutlineWidth),0,6);
        }
        for(int stop=1;stop<=3;stop++){
            final String incoming="bubble.inGradient"+stop,outgoing="chat_outBubbleGradient"+stop;
            Integer in=r.color(previewDark,incoming),out=r.color(previewDark,outgoing);
            button(items,getString(R.string.TetogramAppearanceInGradient)+" "+stop,()->colorDialog(incoming,in==null?Theme.getColor(Theme.key_chat_inBubble):in));
            button(items,getString(R.string.TetogramAppearanceOutGradient)+" "+stop,()->colorDialog(outgoing,out==null?Theme.getColor(Theme.key_chat_outBubble):out));
        }
        number(items,"bubble.inRadius",getString(R.string.TetogramAppearanceInRadius),0,32);
        number(items,"bubble.outRadius",getString(R.string.TetogramAppearanceOutRadius),0,32);
        for(boolean out:new boolean[]{false,true}){
            items.add(UItem.asHeader(getString(out?R.string.TetogramAppearanceOutgoing:R.string.TetogramAppearanceIncoming)));
            String base=out?"bubble.out":"bubble.in";
            toggle(items,base+"Tail",getString(R.string.TetogramAppearanceBubbleTail),true);
            toggle(items,base+"Shadow",getString(R.string.TetogramAppearanceBubbleShadow),true);
            String[] corners={"TopLeft","TopRight","BottomLeft","BottomRight"};int[] labels={R.string.TetogramAppearanceTopLeft,R.string.TetogramAppearanceTopRight,R.string.TetogramAppearanceBottomLeft,R.string.TetogramAppearanceBottomRight};
            for(int corner=0;corner<corners.length;corner++)if(!"true".equals(r.get(base+"Tail"))&&r.get(base+"Tail")!=null||corner!=(out?3:2))number(items,base+corners[corner],getString(labels[corner]),0,32);
            number(items,base+"OutlineWidth",getString(R.string.TetogramAppearanceOutlineWidth),0,4);
            String outlineKey=base+"Outline";Integer outline=r.color(previewDark,outlineKey);button(items,getString(R.string.TetogramAppearanceOutlineColor),()->colorDialog(outlineKey,outline==null?Theme.getColor(Theme.key_chats_unreadCounter):outline));
        }

        button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();draft=new AppearanceConfig.Style();refresh();});
    }
    private void simpleSettings(ArrayList<UItem> items){
        AppearanceConfig.Resolved r=resolved();button(items,getString(R.string.TetogramAppearanceChooseImage),()->pickMedia(false));if(!rowMedia())button(items,getString(R.string.TetogramAppearanceChooseVideo),()->pickMedia(true));
        Integer background=r.color(previewDark,wallpaperColorName(""));button(items,getString(R.string.TetogramAppearanceSolidWallpaper),()->colorDialog(wallpaperColorName(""),background==null?AppearanceRuntime.baseColor(previewDark,Theme.key_chat_wallpaper):background));
        if(previewKind!=2){String primary=(previewDialogs?"list.":"")+"accent.primary";Integer accent=r.color(previewDark,primary);button(items,getString(R.string.TetogramAppearancePrimaryColor),()->colorDialog(primary,accent==null?Theme.getColor(Theme.key_windowBackgroundWhiteBlueText):accent));}
        if(previewDialogs)number(items,"list.rowOpacity",getString(R.string.TetogramAppearanceRowOpacity),0,1);
        else if(previewKind!=2){button(items,getString(R.string.TetogramAppearanceBubbleStyle),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceBubbleStyle)).setItems(new CharSequence[]{getString(R.string.TetogramAppearanceInherit),getString(R.string.TetogramAppearanceSoftBubbles),getString(R.string.TetogramAppearanceMinimalBubbles)},(d,n)->{checkpoint();for(String side:new String[]{"bubble.in","bubble.out"}){for(String suffix:new String[]{"TopLeft","TopRight","BottomLeft","BottomRight","Radius","Tail","Shadow","OutlineWidth"})draft.values.remove(side+suffix);if(n!=0){draft.set(side+"Radius",n==1?"24":"6");for(String corner:new String[]{"TopLeft","TopRight","BottomLeft","BottomRight"})draft.set(side+corner,n==1?"24":"6");draft.set(side+"OutlineWidth","0");draft.set(side+"Tail",Boolean.toString(n==1));draft.set(side+"Shadow",Boolean.toString(n==1));}}refresh();}).create()));button(items,getString(R.string.TetogramAppearanceBubbleOpacity),this::bubbleOpacity);}
        if(AppearanceConfig.isMediaName(r.get(mediaKey("file")))||background!=null){number(items,mediaKey("opacity"),getString(R.string.TetogramAppearanceOpacity),0,1);number(items,mediaKey("dim"),getString(R.string.TetogramAppearanceDimming),0,1);}
        if(AppearanceConfig.isMediaName(r.get(mediaKey("file"))))button(items,getString(R.string.TetogramAppearanceEditMedia),()->{mediaBusy=true;if(preview!=null)preview.setResumed(false);refresh();mediaJob=AppearanceMedia.inspect(currentAccount,owner,r.get(mediaKey("source.file")),r.get(mediaKey("file")),"video".equals(r.get(mediaKey("type"))),this::editSelectedMedia);});
        if(section!=null)button(items,getString(R.string.TetogramSectionIcon),()->presentFragment(new SectionsActivity(currentAccount,section)));
        button(items,getString(R.string.TetogramAppearanceRestoreContrast),this::restoreContrast);button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();draft=new AppearanceConfig.Style();refresh();});
    }
    private void bubbleOpacity(){AppearanceConfig.Resolved r=resolved();Integer color=r.color(previewDark,"chat_outBubble");input(getString(R.string.TetogramAppearanceBubbleOpacity),Float.toString(color==null?1:Color.alpha(color)/255f),text->{try{float alpha=Float.parseFloat(text.trim().replace(',','.'));if(Float.isNaN(alpha)||Float.isInfinite(alpha)||alpha<0||alpha>1)throw new IllegalArgumentException();checkpoint();for(String role:new String[]{"chat_inBubble","chat_inBubbleMedia","chat_outBubble","chat_outBubbleMedia","chat_outBubbleGradient1","chat_outBubbleGradient2","chat_outBubbleGradient3","bubble.inGradient1","bubble.inGradient2","bubble.inGradient3"}){Integer old=r.color(previewDark,role);int key=ThemeColors.stringKeyToInt(role);if(old==null&&key<0||role.contains("Gradient")&&(old==null||old==0))continue;draft.color(previewDark,role,((old==null?AppearanceRuntime.baseColor(previewDark,key):old)&0xffffff)|Math.round(alpha*255)<<24);}refresh();}catch(IllegalArgumentException e){error(getString(R.string.TetogramAppearanceInvalidValue));}});}
    private boolean listColor(String key){return key.startsWith("chats_")||key.startsWith("windowBackground")||key.startsWith("actionBar")||key.startsWith("dialog")||key.equals("divider")||key.startsWith("graySection")||key.startsWith("glass_")||key.startsWith("telegram_color");}
    private int colorCategory(String key){String name=key.startsWith("list.")?key.substring(5):key;if(name.startsWith("chat_out")||name.startsWith("bubble.out"))return R.string.TetogramAppearanceCategoryOutgoing;if(name.startsWith("chat_in")||name.startsWith("bubble.in"))return R.string.TetogramAppearanceCategoryIncoming;if(name.startsWith("chats_"))return R.string.TetogramAppearanceCategoryList;if(name.startsWith("actionBar"))return R.string.TetogramAppearanceCategoryPanels;if(name.startsWith("glass_")||name.startsWith("telegram_color"))return R.string.TetogramAppearanceCategoryNavigation;if(name.startsWith("profile_")||name.startsWith("avatar_"))return R.string.TetogramAppearanceCategoryProfile;if(name.startsWith("windowBackground"))return R.string.TetogramAppearanceCategorySurfaces;return R.string.TetogramAppearanceCategoryOther;}
    private CharSequence colorLabel(String name){String plain=name.startsWith("list.")?name.substring(5):name.startsWith("banner.")?name.substring(7):name;
        if(plain.equals("chat_wallpaper"))return getString(name.startsWith("list.")?R.string.TetogramAppearanceListBackground:name.startsWith("banner.")?R.string.TetogramAppearanceBannerBackground:R.string.TetogramAppearanceBackground);
        switch(plain){case "accent.primary":return getString(R.string.TetogramAppearancePrimaryColor);case "chat_inBubble":return getString(R.string.TetogramAppearanceIncoming);case "chat_outBubble":return getString(R.string.TetogramAppearanceOutgoing);case "chat_messageTextIn":return getString(R.string.TetogramAppearanceIncomingText);case "chat_messageTextOut":return getString(R.string.TetogramAppearanceOutgoingText);
case "windowBackgroundWhite":return getString(R.string.TetogramAppearanceSurface);
case "windowBackgroundGray":return getString(R.string.TetogramAppearanceSecondarySurface);
case "windowBackgroundWhiteBlackText":return getString(R.string.TetogramAppearanceMainText);
case "chats_name":return getString(R.string.TetogramAppearanceChatNames);
case "chats_message":return getString(R.string.TetogramAppearanceMessagePreviewText);
case "chats_date":return getString(R.string.TetogramAppearanceChatTimes);
case "chats_unreadCounter":return getString(R.string.TetogramAppearanceCircleBadge);
case "chats_unreadCounterText":return getString(R.string.TetogramAppearanceCircleBadgeText);
case "actionBarDefault":return getString(R.string.TetogramAppearanceHeaderBackground);
case "actionBarDefaultTitle":return getString(R.string.TetogramAppearanceHeaderText);
case "actionBarDefaultIcon":return getString(R.string.TetogramAppearanceHeaderIcons);
case "chat_messagePanelBackground":return getString(R.string.TetogramAppearanceInputBackground);
case "chat_messagePanelText":return getString(R.string.TetogramAppearanceInputText);
case "group.senderName":return getString(R.string.TetogramAppearanceSenderNames);
case "section.background":return getString(R.string.TetogramAppearanceCircleBackground);
case "section.activeBackground":return getString(R.string.TetogramAppearanceCircleActive);
case "section.text":return getString(R.string.TetogramAppearanceCircleText);
case "section.activeText":return getString(R.string.TetogramAppearanceCircleActiveText);
case "section.badge":return getString(R.string.TetogramAppearanceCircleBadge);
case "section.badgeText":return getString(R.string.TetogramAppearanceCircleBadgeText);
        default:return name.replace('_',' ').replaceAll("([a-z])([A-Z])","$1 $2");}
    }
    private void prepareScopeNames(){
        scopeNames.clear();scopeNames.put(AppearanceConfig.GLOBAL,getString(R.string.TetogramAppearanceGlobal));scopeNames.put("Telegram","Telegram");
        try{SectionConfig sections=SectionsStore.load(currentAccount);for(SectionConfig.Section s:sections.sections)scopeNames.put("section:"+s.id,s.name);for(SectionConfig.Folder f:sections.folders.values())scopeNames.put("folder:"+f.id,f.name);}catch(Exception ignored){}
        for(MessagesController.DialogFilter filter:getMessagesController().getDialogFilters())if(filter.id!=0)scopeNames.put("folder:tg:"+filter.id,String.valueOf(filter.name));
        if(dialog!=0){long userId=dialog;String name=null;if(DialogObject.isEncryptedDialog(dialog)){org.telegram.tgnet.TLRPC.EncryptedChat secret=getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialog));userId=secret==null?0:secret.user_id;}if(userId>0){org.telegram.tgnet.TLRPC.User user=getMessagesController().getUser(userId);if(user!=null)name=UserObject.getUserName(user);}else if(userId<0){org.telegram.tgnet.TLRPC.Chat chat=getMessagesController().getChat(-userId);if(chat!=null)name=chat.title;}if(name!=null){scopeNames.put("chat:"+dialog,name);if(topic!=0){org.telegram.tgnet.TLRPC.TL_forumTopic t=getMessagesController().getTopicsController().findTopic(-dialog,topic);scopeNames.put("topic:"+dialog+":"+topic,t==null?name:name+" / "+t.title);}}}
        if(scope.startsWith("profile:"))scopeNames.put(scope,getString(R.string.TetogramAppearanceProfile));
    }
    private String scopeLabel(String key){String known=scopeNames.get(key);if(known!=null)return known;if(key.startsWith("section:"))return getString(R.string.TetogramAppearanceSections);if(key.startsWith("folder:"))return getString(R.string.TetogramAppearanceFolders);if(key.startsWith("chat:")||key.startsWith("topic:"))return getString(R.string.TetogramAppearanceChat);if(key.startsWith("profile:"))return getString(R.string.TetogramAppearanceProfile);return getString(R.string.TetogramAppearanceInherited);}
    private int surfaceLabel(int kind){switch(kind){case 1:return R.string.TetogramAppearanceDialogList;case 2:return R.string.TetogramAppearanceProfile;case 3:return R.string.TetogramAppearanceGroups;case 4:return R.string.TetogramAppearanceSections;case 5:return R.string.TetogramAppearanceFolders;default:return R.string.TetogramAppearanceChat;}}
    private void choosePreview(){ArrayList<Integer> kinds=new ArrayList<>();if(scope.startsWith("profile:"))kinds.add(2);else{Collections.addAll(kinds,0,3,1);if(scope.equals(AppearanceConfig.GLOBAL)||scope.startsWith("section:")||scope.startsWith("folder:"))Collections.addAll(kinds,4,5);if(scope.equals(AppearanceConfig.GLOBAL))kinds.add(2);}CharSequence[] names=new CharSequence[kinds.size()];for(int i=0;i<names.length;i++)names[i]=getString(surfaceLabel(kinds.get(i)));showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearancePreviewSurface)).setItems(names,(d,n)->{previewKind=kinds.get(n);previewDialogs=previewKind==1||previewKind==4||previewKind==5;refresh();}).create());}
    private boolean rowMedia(){return previewDialogs&&(scope.startsWith("chat:")||scope.startsWith("topic:"));}
    private String mediaPrefix(){return previewKind==2?"banner.wallpaper":previewDialogs?"list.wallpaper":"wallpaper";}
    private String mediaKey(String part){return mediaPrefix()+"."+part;}
    private String wallpaperColorName(String tail){return (previewKind==2?"banner.":previewDialogs?"list.":"")+"chat_wallpaper"+tail;}
    private AppearanceConfig.Resolved previewStyle(){AppearanceConfig.Resolved r=resolved();return previewKind==2?AppearanceSurfaces.banner(r):previewDialogs?AppearanceSurfaces.list(r):r;}
    private float defaultNumber(String key){if(key.equals("effects.durationScale"))return 1;if(key.endsWith("OutlineWidth"))return 0;if(key.startsWith("bubble."))return resolved().number(key.startsWith("bubble.out")?"bubble.outRadius":"bubble.inRadius",SharedConfig.bubbleRadius);if(key.equals(mediaKey("fps")))return 30;if(key.equals(mediaKey("quality")))return 1280;if(key.equals("list.rowOpacity")||key.equals("list.navigationOpacity"))return 1;if(key.equals("list.avatarSize"))return SharedConfig.useThreeLinesLayout?56:52;if(key.endsWith("angle"))return 45;if(key.endsWith("opacity")||key.endsWith("speed")||key.endsWith("zoom"))return 1;return 0;}
    private void number(ArrayList<UItem> items,String key,CharSequence label,float min,float max){String value=Float.toString(resolved().number(key,defaultNumber(key)));button(items,label,value+(draft.values.containsKey(key)?"":" · "+getString(R.string.TetogramAppearanceInherited)),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(label).setItems(new CharSequence[]{getString(R.string.Edit),getString(R.string.TetogramAppearanceInherit)},(d,n)->{if(n==1){checkpoint();draft.values.remove(key);refresh();return;}input(label,value,text->{try{float parsed=Float.parseFloat(text.trim().replace(',','.'));if(Float.isNaN(parsed)||Float.isInfinite(parsed)||parsed<min||parsed>max)throw new IllegalArgumentException();checkpoint();draft.set(key,Float.toString(parsed));refresh();}catch(Exception e){error(getString(R.string.TetogramAppearanceInvalidValue));}});}).create()));}

    private void colorDialog(String name,int color){
        ArrayList<CharSequence> labels=new ArrayList<>();ArrayList<Runnable> runs=new ArrayList<>();
        labels.add(getString(R.string.TetogramAppearanceColorPicker));runs.add(()->pickerDialog(name,color));
        labels.add(getString(R.string.TetogramAppearanceColorInput));runs.add(()->input(getString(R.string.TetogramAppearanceColorInput),AppearanceColors.hex(color),text->{try{acceptColor(name,AppearanceColors.parse(text));}catch(IllegalArgumentException e){error(getString(R.string.TetogramAppearanceColorFormat));}}));
        labels.add(getString(R.string.TetogramAppearanceEyedropper));runs.add(()->pickSample(name));
        labels.add(getString(R.string.Copy));runs.add(()->AndroidUtilities.addToClipboard(AppearanceColors.hex(color)));
        labels.add(getString(R.string.TetogramAppearanceFavoriteColor));runs.add(()->{checkpoint();rememberColor("palette.favorite.",color);refresh();});
        AppearanceConfig.Resolved palette=resolved();
        for(String group:new String[]{"palette.favorite.","palette.recent."})for(int index=0;index<12;index++){String value=palette.get(group+index);if(value==null)continue;try{int c=Integer.parseInt(value);labels.add((group.contains("favorite")?"★ ":"◷ ")+AppearanceColors.hex(c));runs.add(()->acceptColor(name,c));}catch(NumberFormatException ignored){}}
        labels.add(getString(R.string.TetogramAppearanceInherit));runs.add(()->{checkpoint();draft.values.remove((previewDark?"dark.":"light.")+name);refresh();});
        showDialog(new AlertDialog.Builder(getContext()).setTitle(colorLabel(name)).setMessage(AppearanceColors.description(color)).setItems(labels.toArray(new CharSequence[0]),(d,n)->runs.get(n).run()).create());
    }
    private void rememberColor(String prefix,int color){ArrayList<String> colors=new ArrayList<>();colors.add(Integer.toString(color));AppearanceConfig.Resolved r=resolved();for(int i=0;i<12;i++){String c=r.get(prefix+i);if(c!=null&&!colors.contains(c))colors.add(c);}for(int i=0;i<Math.min(12,colors.size());i++)draft.set(prefix+i,colors.get(i));}
    private void putColor(AppearanceConfig.Style target,String name,int color){
        if(name.contains("chat_wallpaper")){int fallback=AppearanceRuntime.baseColor(previewDark,Theme.key_chat_wallpaper);if(fallback==0)fallback=previewDark?0xff17212b:0xffe6ebee;AppearanceWallpaper.color(target,resolved(),previewDark,name,color,fallback);}
        target.color(previewDark,name,color);
        if(name.endsWith("accent.primary")){String domain=name.startsWith("list.")?"list.":"";AppearanceAccent.apply(target,false,color,domain);AppearanceAccent.apply(target,true,color,domain);return;}
        if(!advanced){String[] paired=name.equals("list.windowBackgroundWhite")?new String[]{"list.chats_name","list.chats_message"}:name.equals("list.actionBarDefault")?new String[]{"list.actionBarDefaultTitle","list.actionBarDefaultIcon"}:name.equals("chat_inBubble")?new String[]{"chat_messageTextIn"}:name.equals("chat_outBubble")?new String[]{"chat_messageTextOut"}:name.equals("actionBarDefault")?new String[]{"actionBarDefaultTitle","actionBarDefaultIcon"}:name.equals("windowBackgroundWhite")?new String[]{"windowBackgroundWhiteBlackText","chats_name"}:new String[0];for(String text:paired)target.color(previewDark,text,AppearancePresets.contrast(AppearanceColors.composite(color,previewDark?0xff000000:0xffffffff)));}
    }
    private void acceptColor(String name,int color){checkpoint();putColor(draft,name,color);rememberColor("palette.recent.",color);refresh();}
    private int contrastBase(boolean dark,String key){int nativeKey=ThemeColors.stringKeyToInt(key);return AppearanceRuntime.baseColor(dark,nativeKey<0?Theme.key_windowBackgroundWhite:nativeKey);}
    private void restoreContrast(){checkpoint();AppearanceConfig.Resolved r=resolved();AppearanceContrast.repair(draft,r,previewDark,key->contrastBase(previewDark,key));refresh();}
    private void applyDraft(){
        AppearanceConfig.Resolved r=resolved();ArrayList<String> issues=new ArrayList<>();for(boolean dark:new boolean[]{false,true})for(String key:AppearanceContrast.issues(draft,r,dark,name->contrastBase(dark,name)))issues.add(getString(dark?R.string.TetogramAppearanceDark:R.string.TetogramAppearanceLight)+": "+colorLabel(key));
        if(!issues.isEmpty()){showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceLowContrast)).setMessage(getString(R.string.TetogramAppearanceLowContrastInfo)+"\n\n"+String.join("\n",issues)).setPositiveButton(getString(R.string.TetogramAppearanceRestoreContrast),(d,n)->{checkpoint();for(boolean dark:new boolean[]{false,true})AppearanceContrast.repair(draft,r,dark,key->contrastBase(dark,key));refresh();applyDraft();}).setNegativeButton(getString(R.string.Edit),null).create());return;}
        if(editingPresetId!=null){AppearanceConfig.Preset target=config.presets.get(editingPresetId);if(target==null){error(getString(R.string.TetogramAppearanceConflict));return;}target.style=draft.copy();saveConfig(this::finishFragment);}else{config.scopes.put(scope,draft.copy());saveConfig(this::finishFragment,scope.equals(AppearanceConfig.GLOBAL));}
    }
    private void pickerDialog(String name,int color){
        final int[] value={color};
        LinearLayout body=new LinearLayout(getContext());body.setOrientation(LinearLayout.VERTICAL);
        android.widget.FrameLayout sample=new android.widget.FrameLayout(getContext());body.addView(sample,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(200)));
        Runnable redraw=()->{if(!valid())return;AppearanceConfig.Style old=draft;AppearanceConfig.Resolved candidate;try{draft=draft.copy();putColor(draft,name,value[0]);candidate=previewStyle();}finally{draft=old;}if("video".equals(candidate.get("wallpaper.type"))){String cover=candidate.get("wallpaper.cover.file");if(AppearanceConfig.isMediaName(cover))candidate.values.put("wallpaper.file",cover);else candidate.values.remove("wallpaper.file");candidate.values.put("wallpaper.type","image");}sample.removeAllViews();AppearancePreview scene=new AppearancePreview(getContext(),currentAccount,candidate,previewDark,previewKind);sample.addView(scene,new android.widget.FrameLayout.LayoutParams(-1,-1));};
        ColorPicker picker=new ColorPicker(getContext(),false,(c,n,apply)->{value[0]=c;AndroidUtilities.cancelRunOnUIThread(redraw);AndroidUtilities.runOnUIThread(redraw,80);});picker.setResourcesProvider(getResourceProvider());picker.setType(0,true,1,1,false,0,false);picker.setColor(color,0);body.addView(picker,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(320)));redraw.run();
        android.widget.ScrollView scroll=new android.widget.ScrollView(getContext());scroll.setFillViewport(false);scroll.addView(body);if(preview!=null)preview.setResumed(false);
        AlertDialog dialog=new AlertDialog.Builder(getContext()).setTitle(colorLabel(name)).setView(scroll,AndroidUtilities.dp(480)).setPositiveButton(getString(R.string.OK),(d,n)->acceptColor(name,value[0])).setNeutralButton(getString(R.string.TetogramAppearanceInherit),(d,n)->{checkpoint();draft.values.remove((previewDark?"dark.":"light.")+name);refresh();}).setNegativeButton(getString(R.string.Cancel),null).create();
        showDialog(dialog,d->{AndroidUtilities.cancelRunOnUIThread(redraw);sample.removeAllViews();if(preview!=null)preview.setResumed(valid()&&foreground&&!mediaBusy);});
    }
    private void presetActions(AppearanceConfig.Preset p,boolean builtin){ArrayList<CharSequence> names=new ArrayList<>();ArrayList<Runnable> runs=new ArrayList<>();names.add(getString(R.string.TetogramAppearanceApply));runs.add(()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,"edit",AppearanceConfig.GLOBAL,null,null,0,0);editor.config=config.copy();editor.expectedRevision=expectedRevision;editor.draft=p.style.copy();editor.previewDark="dark".equals(p.style.values.get("mode"));presentFragment(editor);});names.add(getString(R.string.TetogramAppearanceDuplicate));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),p.name,name->{AppearanceConfig.Preset duplicate=p.duplicate(name);config.presets.put(duplicate.id,duplicate);saveConfig(()->{});}));names.add(getString(R.string.TetogramAppearanceExport));runs.add(()->exportPreset(p));if(!builtin){names.add(getString(R.string.Edit));runs.add(()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,"edit",AppearanceConfig.GLOBAL,null,null,0,0);editor.config=config.copy();editor.expectedRevision=expectedRevision;editor.draft=p.style.copy();editor.editingPresetId=p.id;editor.previewDark="dark".equals(p.style.values.get("mode"));presentFragment(editor);});names.add(getString(R.string.EditName));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),p.name,name->{p.name=name;saveConfig(()->{});}));names.add(getString(R.string.Delete));runs.add(()->showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceDeletePreset)).setPositiveButton(getString(R.string.Delete),(d,n)->{config.presets.remove(p.id);saveConfig(()->{});}).setNegativeButton(getString(R.string.Cancel),null).create()));}showDialog(new AlertDialog.Builder(getContext()).setTitle(p.name).setItems(names.toArray(new CharSequence[0]),(d,n)->runs.get(n).run()).create());}
    private void confirmReset(){showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceResetConfirm)).setPositiveButton(getString(R.string.OK),(d,n)->{config.scopes.clear();saveConfig(()->{});}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void saveConfig(Runnable done){saveConfig(done,false);}
    private void saveConfig(Runnable done,boolean applyMode){if(saving||!valid())return;saving=true;AppearanceStore.save(currentAccount,owner,config,expectedRevision,applyMode,(ok,c)->{if(!valid())return;saving=false;if(ok){config=c;expectedRevision=c.revision;done.run();refresh();}else error(getString(R.string.TetogramAppearanceConflict));});refresh();}
    private void input(CharSequence title,String initial,java.util.function.Consumer<String> done){EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(18);field.setText(initial);field.setSingleLine(true);field.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(8),AndroidUtilities.dp(24),AndroidUtilities.dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);showDialog(new AlertDialog.Builder(getContext()).setTitle(title).setView(field).setPositiveButton(getString(R.string.OK),(d,n)->{String value=field.getText().toString().trim();if(value.length()<=256&&!value.isEmpty())done.accept(value);else error(getString(R.string.TetogramAppearanceInvalidValue));}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void error(CharSequence text){if(getContext()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(text).setPositiveButton(getString(R.string.OK),null).create());}
    private void mediaControls(ArrayList<UItem> items){
        items.add(UItem.asHeader(getString(R.string.TetogramAppearanceWallpapers)));
        button(items,getString(R.string.TetogramAppearanceChooseImage),()->pickMedia(false));
        if(!rowMedia())button(items,getString(R.string.TetogramAppearanceChooseVideo),()->pickMedia(true));
        else items.add(UItem.asShadow(getString(R.string.TetogramAppearanceRowImageOnly)));
        AppearanceConfig.Resolved r=resolved();
        if(AppearanceConfig.isMediaName(r.get(mediaKey("file"))))button(items,getString(R.string.TetogramAppearanceEditMedia),()->{mediaBusy=true;if(preview!=null)preview.setResumed(false);refresh();mediaJob=AppearanceMedia.inspect(currentAccount,owner,r.get(mediaKey("source.file")),r.get(mediaKey("file")),"video".equals(r.get(mediaKey("type"))),this::editSelectedMedia);});
        button(items,getString(R.string.TetogramAppearanceWallpaperBase),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceWallpaperBase)).setItems(new CharSequence[]{getString(R.string.TetogramAppearanceInherit),getString(R.string.TetogramAppearanceGlobalWallpaper),getString(R.string.TetogramAppearanceTelegramWallpaper)},(d,n)->{checkpoint();for(String key:new ArrayList<>(draft.values.keySet()))if(AppearanceConfig.isSurfaceWallpaperKey(key,mediaPrefix()))draft.values.remove(key);if(n!=0)draft.set(mediaKey("base"),n==1?"global":"telegram");refresh();}).create()));
        button(items,getString(R.string.TetogramAppearanceSolidWallpaper),()->colorDialog(wallpaperColorName(""),Theme.getColor(Theme.key_chat_wallpaper)));
        for(int stop=1;stop<=3;stop++){final String name=wallpaperColorName("_gradient_to"+stop);Integer current=r.color(previewDark,name);button(items,getString(R.string.TetogramAppearanceGradientStop)+" "+(stop+1),()->colorDialog(name,current==null?Theme.getColor(Theme.key_chat_wallpaper):current));}
        number(items,mediaKey("angle"),getString(R.string.TetogramAppearanceGradientAngle),0,360);
        toggle(items,mediaKey("gradientMotion"),getString(R.string.TetogramAppearanceGradientMotion),false);
        number(items,mediaKey("opacity"),getString(R.string.TetogramAppearanceOpacity),0,1);
        number(items,mediaKey("dim"),getString(R.string.TetogramAppearanceDimming),0,1);
        number(items,mediaKey("blur"),getString(Build.VERSION.SDK_INT>=31&&!rowMedia()?R.string.TetogramAppearanceBlur:R.string.TetogramAppearanceStaticBlur),0,Build.VERSION.SDK_INT>=31&&!rowMedia()?32:1);
        if(!rowMedia()&&AppearanceConfig.isMediaName(r.get(mediaKey("file")))){
            number(items,mediaKey("zoom"),getString(R.string.TetogramAppearanceZoom),1,4);
            number(items,mediaKey("panX"),getString(R.string.TetogramAppearancePanX),-1,1);
            number(items,mediaKey("panY"),getString(R.string.TetogramAppearancePanY),-1,1);
            button(items,getString(R.string.TetogramAppearanceFit),r.get(mediaKey("fit")),()->{checkpoint();draft.set(mediaKey("fit"),"fit".equals(r.get(mediaKey("fit")))?"fill":"fit");refresh();});
        }
        if(!rowMedia()&&"video".equals(r.get(mediaKey("type")))){
            number(items,mediaKey("speed"),getString(R.string.TetogramAppearancePlaybackSpeed),.25f,2);
            toggle(items,mediaKey("loop"),getString(R.string.TetogramAppearanceLoop),true);
            toggle(items,mediaKey("animate"),getString(R.string.TetogramAppearanceAnimateWallpaper),true);
            button(items,getString(R.string.TetogramAppearanceCoverFirst),()->setCover(0));
            button(items,getString(R.string.TetogramAppearanceCoverCurrent),()->setCover(preview==null?0:preview.positionMs()));
        }
        if(!rowMedia()){items.add(UItem.asShadow(getString(R.string.TetogramAppearanceVideoPreparation)));
        number(items,mediaKey("fps"),getString(R.string.TetogramAppearanceFps),12,60);
        number(items,mediaKey("quality"),getString(R.string.TetogramAppearanceQuality),360,1920);}
    }
    private void toggle(ArrayList<UItem> items,String key,CharSequence label,boolean fallback){
        if(filterParameters&&!search.isEmpty()&&!label.toString().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))&&!key.toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT)))return;
        String value=resolved().get(key);boolean checked=value==null?fallback:Boolean.parseBoolean(value);boolean explicit=draft.values.containsKey(key);
        actions.add(()->{checkpoint();draft.set(key,Boolean.toString(!checked));refresh();});items.add(UItem.asCheck(actions.size(),label+(explicit?"":" · "+getString(R.string.TetogramAppearanceInherited))).setChecked(checked).setEnabled(!saving&&!mediaBusy));
        if(explicit)button(items,getString(R.string.TetogramAppearanceInherit)+" — "+label,()->{checkpoint();draft.values.remove(key);refresh();});
    }
    private void pickMedia(boolean video){
        if(mediaBusy||!valid())return;pickingVideo=video;Intent intent;
        if(Build.VERSION.SDK_INT>=33)intent=new Intent(MediaStore.ACTION_PICK_IMAGES).setType(video?"video/*":"image/*");else intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(video?"video/*":"image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(intent,PICK_APPEARANCE_MEDIA);}catch(Exception e){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(video?"video/*":"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),PICK_APPEARANCE_MEDIA);}catch(Exception ex){error(getString(R.string.TetogramAppearanceMediaError));}}
    }
    @Override public void onActivityResultFragment(int request,int result,Intent data){
        super.onActivityResultFragment(request,result,data);if(result!=Activity.RESULT_OK||data==null||data.getData()==null||!valid())return;
        if(request!=PICK_APPEARANCE_MEDIA&&request!=IMPORT_PRESET&&request!=EXPORT_PRESET&&request!=SAMPLE_COLOR)return;
        mediaBusy=true;if(preview!=null)preview.setResumed(false);refresh();
        if(request==SAMPLE_COLOR){String role=sampledRole;mediaJob=AppearanceMedia.sampleImage(currentAccount,owner,data.getData(),bitmap->{if(!valid()){if(bitmap!=null)bitmap.recycle();return;}if(bitmap==null||role==null){mediaBusy=false;error(getString(R.string.TetogramAppearanceMediaError));refresh();}else showSample(role,bitmap);});}
        else if(request==IMPORT_PRESET){mediaJob=AppearanceMedia.importPreset(currentAccount,owner,data.getData(),imported->{mediaBusy=false;if(!valid()){if(imported!=null)for(File f:imported.files)AppearanceMedia.discard(f);return;}if(imported==null)error(getString(R.string.TetogramAppearanceArchiveError));else{AppearanceActivity editor=new AppearanceActivity(currentAccount,"edit",AppearanceConfig.GLOBAL,null,null,0,0);editor.config=config.copy();editor.expectedRevision=expectedRevision;editor.draft=imported.preset.style.copy();editor.previewDark="dark".equals(editor.draft.values.get("mode"));editor.pendingMedia.addAll(imported.files);if(!presentFragment(editor))for(File f:imported.files)AppearanceMedia.discard(f);}refresh();});}
        else if(request==EXPORT_PRESET){if(exporting==null){mediaBusy=false;refresh();return;}mediaJob=AppearanceMedia.exportPreset(currentAccount,owner,data.getData(),exporting,exportImages,exportVideos,ok->{mediaBusy=false;exporting=null;if(!ok)error(getString(R.string.TetogramAppearanceArchiveError));else error(getString(R.string.TetogramAppearanceExported));if(preview!=null)preview.setResumed(valid()&&foreground);refresh();});}
        else mediaJob=AppearanceMedia.importUri(currentAccount,owner,data.getData(),pickingVideo,this::editSelectedMedia);
    }
    private void pickSample(String name){sampledRole=name;try{Intent intent=new Intent(Build.VERSION.SDK_INT>=33?MediaStore.ACTION_PICK_IMAGES:Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);if(Build.VERSION.SDK_INT<33)intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,SAMPLE_COLOR);}catch(Exception e){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),SAMPLE_COLOR);}catch(Exception ignored){error(getString(R.string.TetogramAppearanceMediaError));}}}
    private void showSample(String role,android.graphics.Bitmap bitmap){
        final boolean[] active={true},selecting={false};final int[] selected={bitmap.getPixel(bitmap.getWidth()/2,bitmap.getHeight()/2)};final AlertDialog[] dialog={null};
        android.widget.ImageView image=new android.widget.ImageView(getContext());image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);image.setImageBitmap(bitmap);
        org.telegram.ui.Components.Paint.Views.PipettePickerView pipette=new org.telegram.ui.Components.Paint.Views.PipettePickerView(getContext(),bitmap){
            @Override public boolean onTouchEvent(android.view.MotionEvent event){int action=event.getActionMasked();if(action==android.view.MotionEvent.ACTION_DOWN||action==android.view.MotionEvent.ACTION_MOVE||action==android.view.MotionEvent.ACTION_UP){int[] pixel=AppearanceSample.pixel(event.getX(),event.getY(),getWidth(),getHeight(),bitmap.getWidth(),bitmap.getHeight());if(pixel!=null)selected[0]=bitmap.getPixel(pixel[0],pixel[1]);}if(action==android.view.MotionEvent.ACTION_UP)selecting[0]=true;if(action==android.view.MotionEvent.ACTION_CANCEL)selecting[0]=false;return super.onTouchEvent(event);}
            @Override protected void onStopPipette(){image.setImageDrawable(null);if(!selecting[0])AndroidUtilities.runOnUIThread(()->{if(active[0]&&dialog[0]!=null)dialog[0].dismiss();});}
        };
        pipette.setVisibility(View.INVISIBLE);
        android.widget.FrameLayout frame=new android.widget.FrameLayout(getContext()){
            @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);if(bitmap.isRecycled())return;float scale=Math.min(w/(float)bitmap.getWidth(),h/(float)bitmap.getHeight());pipette.setLayoutParams(LayoutHelper.createFrame(Math.max(1,Math.round(bitmap.getWidth()*scale)/AndroidUtilities.density),Math.max(1,Math.round(bitmap.getHeight()*scale)/AndroidUtilities.density),android.view.Gravity.CENTER));}
        };
        frame.addView(image,LayoutHelper.createFrame(-1,-1));frame.addView(pipette,LayoutHelper.createFrame(-1,-1));
        pipette.setColorListener(color->{if(active[0]&&valid())acceptColor(role,selected[0]);if(dialog[0]!=null)dialog[0].dismiss();});
        dialog[0]=new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceEyedropper)).setMessage(getString(R.string.TetogramAppearanceEyedropperInfo)).setView(frame,AndroidUtilities.dp(280)).setNegativeButton(getString(R.string.Cancel),null).create();
        frame.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(pipette.getWidth()<=0||pipette.getHeight()<=0||frame.isLayoutRequested())return true;frame.getViewTreeObserver().removeOnPreDrawListener(this);pipette.setVisibility(View.VISIBLE);pipette.animateShow();return true;}});
        if(showDialog(dialog[0],d->{active[0]=false;image.setImageDrawable(null);frame.removeView(pipette);mediaBusy=false;if(!bitmap.isRecycled())bitmap.recycle();if(preview!=null)preview.setResumed(valid()&&foreground);refresh();})==null){active[0]=false;image.setImageDrawable(null);if(!bitmap.isRecycled())bitmap.recycle();mediaBusy=false;refresh();}
    }
    private void importPreset(){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),IMPORT_PRESET);}catch(Exception e){error(getString(R.string.TetogramAppearanceArchiveError));}}
    private void exportPreset(AppearanceConfig.Preset preset){showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceExport)).setMessage(getString(R.string.TetogramAppearanceExportPrivacy)).setItems(new CharSequence[]{getString(R.string.TetogramAppearanceExportColors),getString(R.string.TetogramAppearanceExportImages),getString(R.string.TetogramAppearanceExportVideos)},(d,n)->{exporting=preset.duplicate(preset.name);exportImages=n>=1;exportVideos=n==2;try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip").putExtra(Intent.EXTRA_TITLE,"Tetogram-appearance.zip").addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION),EXPORT_PRESET);}catch(Exception e){exporting=null;error(getString(R.string.TetogramAppearanceArchiveError));}}).create());}

    private void editSelectedMedia(AppearanceMedia.Asset source){
        mediaBusy=false;if(!valid()){if(source!=null)AppearanceMedia.discard(source.file);return;}if(source==null){error(getString(R.string.TetogramAppearanceMediaError));refresh();return;}
        pendingMedia.add(source.file);if(preview!=null)preview.setResumed(false);
        MediaController.PhotoEntry entry=new MediaController.PhotoEntry(0,0,0,source.file.getAbsolutePath(),source.video?(int)Math.min(Integer.MAX_VALUE,source.durationMs/1000):0,source.video,source.width,source.height,source.file.length());
        ArrayList<Object> photos=new ArrayList<>();photos.add(entry);final boolean[] applied={false};
        PhotoViewer viewer=PhotoViewer.getInstance();viewer.setParentActivity(this,getResourceProvider());viewer.setAvatarFor(null);
        boolean opened=viewer.openPhotoForSelect(photos,0,PhotoViewer.SELECT_TYPE_WALLPAPER,false,new PhotoViewer.EmptyPhotoViewerProvider(){
            @Override public boolean canEdit(int index){return true;}
            @Override public boolean canScrollAway(){return false;}
            @Override public boolean allowCaption(){return false;}
            @Override public boolean allowSendingSubmenu(){return false;}
            @Override public boolean canSendAsFile(){return false;}
            @Override public boolean forceMuteVideo(){return true;}
            @Override public void sendButtonPressed(int index,VideoEditedInfo info,boolean notify,int schedule,int repeat,boolean forceDocument){if(!valid()||applied[0])return;applied[0]=true;mediaBusy=true;refresh();AppearanceConfig.Resolved current=resolved();mediaJob=AppearanceMedia.prepare(currentAccount,owner,source,entry.imagePath,info,Math.round(current.number(mediaKey("fps"),30)),Math.round(current.number(mediaKey("quality"),1280)),entry.coverPath,asset->{mediaBusy=false;if(!valid()){if(asset!=null){AppearanceMedia.discard(asset.file);AppearanceMedia.discard(asset.cover);}return;}if(asset==null){error(getString(R.string.TetogramAppearanceMediaError));refresh();return;}checkpoint();pendingMedia.add(asset.file);if(asset.cover!=null)pendingMedia.add(asset.cover);draft.set(mediaKey("file"),asset.file.getName());draft.set(mediaKey("type"),asset.video?"video":"image");if(previewDialogs&&!draft.values.containsKey("list.rowOpacity"))draft.set("list.rowOpacity","0.85");draft.set(mediaKey("source.file"),source.file.getName());if(asset.cover!=null)draft.set(mediaKey("cover.file"),asset.cover.getName());else draft.set(mediaKey("cover.file"),"");refresh();});}
            @Override public void onClose(){if(!applied[0]){pendingMedia.remove(source.file);AppearanceMedia.discard(source.file);}if(preview!=null)preview.setResumed(valid()&&foreground&&!mediaBusy);}
        },null);
        if(opened)viewer.setTitle(getString(R.string.TetogramAppearanceWallpapers));
        if(!opened){pendingMedia.remove(source.file);AppearanceMedia.discard(source.file);if(preview!=null)preview.setResumed(valid()&&foreground);error(getString(R.string.TetogramAppearanceMediaError));}
    }
    private void setCover(long time){AppearanceConfig.Resolved r=resolved();mediaBusy=true;if(preview!=null)preview.setResumed(false);refresh();mediaJob=AppearanceMedia.coverAt(currentAccount,owner,r.get(mediaKey("file")),time,asset->{mediaBusy=false;if(!valid()){if(asset!=null)AppearanceMedia.discard(asset.file);return;}if(asset==null)error(getString(R.string.TetogramAppearanceMediaError));else{checkpoint();pendingMedia.add(asset.file);draft.set(mediaKey("cover.file"),asset.file.getName());}refresh();});}
    private void pickChat(){pickChat(false);}
    private void pickChat(boolean groups){Bundle args=new Bundle();if(groups){args.putBoolean("allowUsers",false);args.putBoolean("allowBots",false);}args.putBoolean("onlySelect",true);args.putInt("dialogsType",0);DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(currentAccount);picker.setDelegate((f,dids,m,p,n,s,r,t)->{if(dids.isEmpty())return false;long did=dids.get(0).dialogId;f.finishFragment();editScope("chat:"+did,null,null,did,0);return true;});presentFragment(picker);}
    private void pickSection(boolean folders){try{SectionConfig c=SectionsStore.load(currentAccount);ArrayList<CharSequence> names=new ArrayList<>();ArrayList<String> ids=new ArrayList<>();if(folders){for(SectionConfig.Folder f:c.folders.values()){names.add(f.name);ids.add(f.id);}for(org.telegram.messenger.MessagesController.DialogFilter f:getMessagesController().getDialogFilters())if(f.id!=0){names.add(f.name);ids.add("tg:"+f.id);}}else for(SectionConfig.Section s:c.sections){names.add(s.name);ids.add(s.id);}if(ids.isEmpty()){presentFragment(new SectionsActivity(currentAccount));return;}showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(folders?R.string.TetogramAppearanceFolders:R.string.TetogramAppearanceSections)).setItems(names.toArray(new CharSequence[0]),(d,n)->{String id=ids.get(n);editScope((folders?"folder:":"section:")+id,folders?null:id,folders?id:null,0,0);}).create());}catch(Exception e){error(getString(R.string.TetogramStorageError));}}
    @Override protected void click(UItem item,View v,int p,float x,float y){if(item.id>0 && item.id<=actions.size() && valid()&&!saving)actions.get(item.id-1).run();}
}
