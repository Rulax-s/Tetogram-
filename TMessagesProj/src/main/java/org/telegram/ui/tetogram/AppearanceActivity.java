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
    private long expectedRevision;
    private boolean failed,saving,destroyed,advanced,previewDark;
    private String search="";
    private final ArrayList<Runnable> actions=new ArrayList<>();
    private final ArrayDeque<AppearanceConfig.Style> undo=new ArrayDeque<>();
    private EditTextBoldCursor searchInput;
    private AppearancePreview preview;
    private boolean foreground,mediaBusy,previewDialogs;
    private static final int PICK_APPEARANCE_MEDIA=61041;
    private boolean pickingVideo;
    private AppearanceMedia.Job mediaJob;
    private final ArrayList<File> pendingMedia=new ArrayList<>();
    public AppearanceActivity(int account){this(account,"home",AppearanceConfig.GLOBAL,null,null,0,0);}
    public AppearanceActivity(int account,String scope,String section,String folder,long dialog,long topic){this(account,"edit",scope,section,folder,dialog,topic);}
    private AppearanceActivity(int account,String page,String scope,String section,String folder,long dialog,long topic){super(account);this.owner=UserConfig.getInstance(account).getClientUserId();this.page=page;this.scope=scope;this.section=section;this.folder=folder;this.dialog=dialog;this.topic=topic;previewDark=Theme.isCurrentThemeDark();}
    @Override protected CharSequence title(){return getString(R.string.TetogramAppearance);}
    private boolean valid(){return !destroyed && owner>0 && owner==getUserConfig().getClientUserId();}
    @Override public View createView(Context context){
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
    private void button(ArrayList<UItem> items,CharSequence title,CharSequence value,Runnable action){actions.add(action);items.add(UItem.asButton(actions.size(),title,value).setEnabled(!saving&&!mediaBusy));}
    private void button(ArrayList<UItem> items,CharSequence title,Runnable action){button(items,title,null,action);}
    private void checkpoint(){undo.push(draft.copy());}
    private AppearanceConfig.Resolved resolved(){AppearanceConfig.Style old=config.scopes.put(scope,draft);try{return scope.startsWith("profile:")?config.resolveProfile(Long.parseLong(scope.substring(8))):config.resolve(new AppearanceConfig.Context(section,folder,dialog,topic));}finally{if(old==null)config.scopes.remove(scope);else config.scopes.put(scope,old);}}
    private void editScope(String key,String section,String folder,long did,long tid){presentFragment(new AppearanceActivity(currentAccount,key,section,folder,did,tid));}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        actions.clear();if(!valid())return;
        if(failed){items.add(UItem.asShadow(getString(R.string.TetogramAppearanceStorageError)));return;}
        if(config==null){items.add(UItem.asShadow(getString(R.string.Loading)));return;}
        if("home".equals(page)){
            items.add(UItem.asShadow(getString(R.string.TetogramAppearanceLocal)));
            button(items,getString(R.string.TetogramAppearancePresets),()->presentFragment(new AppearanceActivity(currentAccount,"presets",AppearanceConfig.GLOBAL,null,null,0,0)));
            button(items,getString(R.string.TetogramAppearanceWallpapers),()->editScope(AppearanceConfig.GLOBAL,null,null,0,0));
            button(items,getString(R.string.TetogramAppearanceSimple),()->editScope(AppearanceConfig.GLOBAL,null,null,0,0));
            button(items,getString(R.string.TetogramAppearanceNativeThemes),()->presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
            button(items,getString(R.string.TetogramAppearanceChat),()->pickChat());
            button(items,getString(R.string.TetogramAppearanceGroups),()->pickChat(true));
            button(items,getString(R.string.TetogramAppearanceDialogList),()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,AppearanceConfig.GLOBAL,null,null,0,0);editor.previewDialogs=true;presentFragment(editor);});
            button(items,getString(R.string.TetogramAppearanceSections),()->pickSection(false));
            button(items,getString(R.string.TetogramAppearanceFolders),()->pickSection(true));
            button(items,getString(R.string.TetogramAppearanceEffects),()->presentFragment(new LiteModeSettingsActivity()));
            button(items,getString(R.string.TetogramAppearanceReset),()->confirmReset());
            return;
        }
        if("presets".equals(page)){
            button(items,getString(R.string.TetogramAppearanceCreatePreset),()->input(getString(R.string.TetogramAppearancePresetName),"",name->{AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name=name;p.style=config.edit(AppearanceConfig.GLOBAL).copy();config.presets.put(p.id,p);saveConfig(()->{});}));
            items.add(UItem.asHeader(getString(R.string.TetogramAppearanceBuiltins)));
            for(AppearanceConfig.Preset p:AppearancePresets.builtins())button(items,p.name,()->presetActions(p,true));
            items.add(UItem.asHeader(getString(R.string.TetogramAppearanceUserPresets)));
            for(AppearanceConfig.Preset p:config.presets.values())button(items,p.name,()->presetActions(p,false));
            return;
        }
        items.add(UItem.asShadow(getString(R.string.TetogramAppearanceDraft)+"\n"+scope));
        button(items,getString(R.string.TetogramAppearanceApply),()->{config.scopes.put(scope,draft.copy());saveConfig(this::finishFragment);});
        button(items,getString(R.string.Cancel),this::finishFragment);
        button(items,getString(R.string.TetogramAppearanceUndo),()->{if(!undo.isEmpty()){draft=undo.pop();refresh();}});
        button(items,getString(R.string.TetogramAppearanceSavePreset),()->input(getString(R.string.TetogramAppearancePresetName),"",name->{AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name=name;p.style=draft.copy();config.presets.put(p.id,p);saveConfig(()->refresh());}));
        button(items,getString(R.string.TetogramAppearancePresets),()->{ArrayList<AppearanceConfig.Preset> all=new ArrayList<>(AppearancePresets.builtins());all.addAll(config.presets.values());CharSequence[] names=new CharSequence[all.size()];for(int i=0;i<all.size();i++)names[i]=all.get(i).name;showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearancePresets)).setItems(names,(d,n)->{checkpoint();draft=all.get(n).style.copy();previewDark="dark".equals(draft.values.get("mode"));refresh();}).create());});
        button(items,getString(R.string.TetogramAppearanceDisplayMode),draft.values.getOrDefault("mode",getString(R.string.TetogramAppearanceInherited)),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceDisplayMode)).setItems(new CharSequence[]{getString(R.string.TetogramAppearanceLight),getString(R.string.TetogramAppearanceDark),getString(R.string.TetogramAppearanceInherit)},(d,n)->{checkpoint();if(n==2)draft.values.remove("mode");else draft.set("mode",n==0?"light":"dark");previewDark=n==1;refresh();}).create()));
        button(items,getString(R.string.TetogramAppearanceMode),previewDark?getString(R.string.TetogramAppearanceDark):getString(R.string.TetogramAppearanceLight),()->{previewDark=!previewDark;refresh();});
        button(items,getString(R.string.TetogramAppearanceAdvanced),advanced?getString(R.string.TetogramAppearanceAdvanced):getString(R.string.TetogramAppearanceSimple),()->{advanced=!advanced;refresh();});
        button(items,getString(R.string.TetogramAppearancePreviewSurface),getString(previewDialogs?R.string.TetogramAppearanceDialogList:R.string.TetogramAppearanceChat),()->{previewDialogs=!previewDialogs;refresh();});
        preview=new AppearancePreview(getContext(),currentAccount,previewStyle(),previewDark,previewDialogs);preview.setResumed(foreground&&!mediaBusy);items.add(UItem.asCustom(preview,240));
        if(mediaBusy)items.add(UItem.asShadow(getString(R.string.TetogramAppearancePreparingMedia)));
        mediaControls(items);
        if(searchInput!=null)items.add(UItem.asCustom(searchInput,48));
        String[] simple=previewDialogs?new String[]{"list.chat_wallpaper","windowBackgroundWhite","chats_name","chats_message","chats_date","chats_unreadCounter","chats_unreadCounterText","actionBarDefault"}:new String[]{"chat_wallpaper","chat_inBubble","chat_outBubble","chat_messageTextIn","chat_messageTextOut","actionBarDefault","chats_name","chats_unreadCounter"};
        ArrayList<String> keys=new ArrayList<>();
        if(advanced){for(int key=0;key<Theme.colorsCount;key++){String name=ThemeColors.getStringName(key);if(name!=null && !name.equals("wallpaperFileOffset") && !name.contains("rotation") && !name.toLowerCase(java.util.Locale.ROOT).contains("animated"))keys.add(name);}Collections.sort(keys);}else Collections.addAll(keys,simple);
        if(previewDialogs&&!keys.contains("list.chat_wallpaper"))keys.add("list.chat_wallpaper");
        String prefix=previewDark?"dark.":"light.";AppearanceConfig.Resolved r=resolved();
        for(String name:keys){if(!search.isEmpty() && !name.toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT)))continue;
            String key=prefix+name;int nativeKey=ThemeColors.stringKeyToInt(name.startsWith("list.")?name.substring(5):name);Integer custom=r.color(previewDark,name);int color=custom==null?Theme.getColor(nativeKey):custom;
            String origin=draft.values.containsKey(key)?getString(R.string.TetogramAppearanceExplicit):getString(R.string.TetogramAppearanceInherited)+" · "+r.origins.getOrDefault(key,"Telegram");
            button(items,colorLabel(name),String.format(java.util.Locale.ROOT,"#%08X",color)+" · "+origin,()->colorDialog(name,color));
        }
        if(previewDialogs){
            number(items,"list.rowOpacity",getString(R.string.TetogramAppearanceRowOpacity),0,1);
            number(items,"list.radius",getString(R.string.TetogramAppearanceRowRadius),0,32);
            number(items,"list.sideInset",getString(R.string.TetogramAppearanceSideInset),0,24);
            number(items,"list.rowGap",getString(R.string.TetogramAppearanceRowGap),0,20);
            number(items,"list.avatarSize",getString(R.string.TetogramAppearanceAvatarSize),24,SharedConfig.useThreeLinesLayout?56:52);
            number(items,"list.outlineWidth",getString(R.string.TetogramAppearanceOutlineWidth),0,4);
            toggle(items,"list.separators",getString(R.string.TetogramAppearanceSeparators),true);
            Integer outline=resolved().color(previewDark,"list.outline");button(items,getString(R.string.TetogramAppearanceOutlineColor),()->colorDialog("list.outline",outline==null?Theme.getColor(Theme.key_chats_unreadCounter):outline));
        }
        number(items,"bubble.inRadius",getString(R.string.TetogramAppearanceInRadius),0,32);
        number(items,"bubble.outRadius",getString(R.string.TetogramAppearanceOutRadius),0,32);
        button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();draft=new AppearanceConfig.Style();refresh();});
    }
    private CharSequence colorLabel(String name){switch(name){case "chat_wallpaper":return getString(R.string.TetogramAppearanceBackground);case "chat_inBubble":return getString(R.string.TetogramAppearanceIncoming);case "chat_outBubble":return getString(R.string.TetogramAppearanceOutgoing);case "chat_messageTextIn":return getString(R.string.TetogramAppearanceIncomingText);case "chat_messageTextOut":return getString(R.string.TetogramAppearanceOutgoingText);default:return name;}}
    private boolean rowMedia(){return previewDialogs&&(scope.startsWith("chat:")||scope.startsWith("topic:"));}
    private String mediaPrefix(){return previewDialogs?"list.wallpaper":"wallpaper";}
    private String mediaKey(String part){return mediaPrefix()+"."+part;}
    private String wallpaperColorName(String tail){return (previewDialogs?"list.":"")+"chat_wallpaper"+tail;}
    private AppearanceConfig.Resolved previewStyle(){AppearanceConfig.Resolved r=resolved();return previewDialogs?AppearanceSurfaces.list(r):r;}
    private float defaultNumber(String key){if(key.startsWith("bubble."))return SharedConfig.bubbleRadius;if(key.equals(mediaKey("fps")))return 30;if(key.equals(mediaKey("quality")))return 1280;if(key.equals("list.rowOpacity"))return 1;if(key.equals("list.avatarSize"))return SharedConfig.useThreeLinesLayout?56:52;if(key.endsWith("angle"))return 45;if(key.endsWith("opacity")||key.endsWith("speed")||key.endsWith("zoom"))return 1;return 0;}
    private void number(ArrayList<UItem> items,String key,CharSequence label,float min,float max){String value=Float.toString(resolved().number(key,defaultNumber(key)));button(items,label,value+(draft.values.containsKey(key)?"":" · "+getString(R.string.TetogramAppearanceInherited)),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(label).setItems(new CharSequence[]{getString(R.string.Edit),getString(R.string.TetogramAppearanceInherit)},(d,n)->{if(n==1){checkpoint();draft.values.remove(key);refresh();return;}input(label,value,text->{try{float parsed=Float.parseFloat(text);if(Float.isNaN(parsed)||Float.isInfinite(parsed)||parsed<min||parsed>max)throw new IllegalArgumentException();checkpoint();draft.set(key,Float.toString(parsed));refresh();}catch(Exception e){error(getString(R.string.TetogramAppearanceInvalidValue));}});}).create()));}

    private void colorDialog(String name,int color){
        final int[] value={color};ColorPicker picker=new ColorPicker(getContext(),false,(c,n,apply)->value[0]=c);picker.setResourcesProvider(getResourceProvider());picker.setType(0,true,1,1,false,0,false);picker.setColor(color,0);
        showDialog(new AlertDialog.Builder(getContext()).setTitle(colorLabel(name)).setView(picker, AndroidUtilities.dp(320)).setPositiveButton(getString(R.string.OK),(d,n)->{checkpoint();if(name.contains("chat_wallpaper")){String domain=name.startsWith("list.")?"list.wallpaper":"wallpaper";draft.set(domain+".file","");draft.set(domain+".cover.file","");draft.set(domain+".type",name.contains("_gradient_")?"gradient":"solid");}draft.color(previewDark,name,value[0]);refresh();}).setNeutralButton(getString(R.string.TetogramAppearanceInherit),(d,n)->{checkpoint();draft.values.remove((previewDark?"dark.":"light.")+name);refresh();}).setNegativeButton(getString(R.string.Cancel),null).create());
    }
    private void presetActions(AppearanceConfig.Preset p,boolean builtin){ArrayList<CharSequence> names=new ArrayList<>();ArrayList<Runnable> runs=new ArrayList<>();names.add(getString(R.string.TetogramAppearanceApply));runs.add(()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,"edit",AppearanceConfig.GLOBAL,null,null,0,0);editor.config=config.copy();editor.expectedRevision=expectedRevision;editor.draft=p.style.copy();editor.previewDark="dark".equals(p.style.values.get("mode"));presentFragment(editor);});names.add(getString(R.string.TetogramAppearanceDuplicate));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),p.name,name->{AppearanceConfig.Preset duplicate=p.duplicate(name);config.presets.put(duplicate.id,duplicate);saveConfig(()->{});}));if(!builtin){names.add(getString(R.string.EditName));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),p.name,name->{p.name=name;saveConfig(()->{});}));names.add(getString(R.string.Delete));runs.add(()->showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceDeletePreset)).setPositiveButton(getString(R.string.Delete),(d,n)->{config.presets.remove(p.id);saveConfig(()->{});}).setNegativeButton(getString(R.string.Cancel),null).create()));}showDialog(new AlertDialog.Builder(getContext()).setTitle(p.name).setItems(names.toArray(new CharSequence[0]),(d,n)->runs.get(n).run()).create());}
    private void confirmReset(){showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceResetConfirm)).setPositiveButton(getString(R.string.OK),(d,n)->{config.scopes.clear();saveConfig(()->{});}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void saveConfig(Runnable done){if(saving||!valid())return;saving=true;AppearanceStore.save(currentAccount,owner,config,expectedRevision,(ok,c)->{if(!valid())return;saving=false;if(ok){config=c;expectedRevision=c.revision;done.run();refresh();}else error(getString(R.string.TetogramAppearanceConflict));});refresh();}
    private void input(CharSequence title,String initial,java.util.function.Consumer<String> done){EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(18);field.setText(initial);field.setSingleLine(true);field.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(8),AndroidUtilities.dp(24),AndroidUtilities.dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);showDialog(new AlertDialog.Builder(getContext()).setTitle(title).setView(field).setPositiveButton(getString(R.string.OK),(d,n)->{String value=field.getText().toString().trim();if(value.length()<=256&&!value.isEmpty())done.accept(value);}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void error(CharSequence text){if(getContext()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(text).setPositiveButton(getString(R.string.OK),null).create());}
    private void mediaControls(ArrayList<UItem> items){
        items.add(UItem.asHeader(getString(R.string.TetogramAppearanceWallpapers)));
        button(items,getString(R.string.TetogramAppearanceChooseImage),()->pickMedia(false));
        if(!rowMedia())button(items,getString(R.string.TetogramAppearanceChooseVideo),()->pickMedia(true));
        else items.add(UItem.asShadow(getString(R.string.TetogramAppearanceRowImageOnly)));
        AppearanceConfig.Resolved r=resolved();
        if(AppearanceConfig.isMediaName(r.get(mediaKey("file"))))button(items,getString(R.string.TetogramAppearanceEditMedia),()->{mediaBusy=true;if(preview!=null)preview.setResumed(false);refresh();mediaJob=AppearanceMedia.inspect(currentAccount,owner,r.get(mediaKey("file")),"video".equals(r.get(mediaKey("type"))),this::editSelectedMedia);});
        button(items,getString(R.string.TetogramAppearanceWallpaperBase),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceWallpaperBase)).setItems(new CharSequence[]{getString(R.string.TetogramAppearanceInherit),getString(R.string.TetogramAppearanceGlobalWallpaper),getString(R.string.TetogramAppearanceTelegramWallpaper)},(d,n)->{checkpoint();for(String key:new ArrayList<>(draft.values.keySet()))if(AppearanceConfig.isSurfaceWallpaperKey(key,mediaPrefix()))draft.values.remove(key);if(n!=0)draft.set(mediaKey("base"),n==1?"global":"telegram");refresh();}).create()));
        button(items,getString(R.string.TetogramAppearanceSolidWallpaper),()->colorDialog(wallpaperColorName(""),Theme.getColor(Theme.key_chat_wallpaper)));
        for(int stop=1;stop<=3;stop++){final String name=wallpaperColorName("_gradient_to"+stop);Integer current=r.color(previewDark,name);button(items,getString(R.string.TetogramAppearanceGradientStop)+" "+(stop+1),()->colorDialog(name,current==null?Theme.getColor(Theme.key_chat_wallpaper):current));}
        number(items,mediaKey("angle"),getString(R.string.TetogramAppearanceGradientAngle),0,360);
        toggle(items,mediaKey("gradientMotion"),getString(R.string.TetogramAppearanceGradientMotion),false);
        number(items,mediaKey("opacity"),getString(R.string.TetogramAppearanceOpacity),0,1);
        number(items,mediaKey("dim"),getString(R.string.TetogramAppearanceDimming),0,1);
        number(items,mediaKey("blur"),getString(Build.VERSION.SDK_INT>=31&&!rowMedia()?R.string.TetogramAppearanceBlur:R.string.TetogramAppearanceStaticBlur),0,Build.VERSION.SDK_INT>=31&&!rowMedia()?32:1);
        if(!rowMedia()&&"video".equals(r.get(mediaKey("type")))){
            number(items,mediaKey("speed"),getString(R.string.TetogramAppearancePlaybackSpeed),.25f,2);
            number(items,mediaKey("zoom"),getString(R.string.TetogramAppearanceZoom),1,4);
            number(items,mediaKey("panX"),getString(R.string.TetogramAppearancePanX),-1,1);
            number(items,mediaKey("panY"),getString(R.string.TetogramAppearancePanY),-1,1);
            toggle(items,mediaKey("loop"),getString(R.string.TetogramAppearanceLoop),true);
            toggle(items,mediaKey("animate"),getString(R.string.TetogramAppearanceAnimateWallpaper),true);
            button(items,getString(R.string.TetogramAppearanceFit),r.get(mediaKey("fit")),()->{checkpoint();draft.set(mediaKey("fit"),"fit".equals(r.get(mediaKey("fit")))?"fill":"fit");refresh();});
            button(items,getString(R.string.TetogramAppearanceCoverFirst),()->setCover(0));
            button(items,getString(R.string.TetogramAppearanceCoverCurrent),()->setCover(preview==null?0:preview.positionMs()));
        }
        if(!rowMedia()){items.add(UItem.asShadow(getString(R.string.TetogramAppearanceVideoPreparation)));
        number(items,mediaKey("fps"),getString(R.string.TetogramAppearanceFps),12,60);
        number(items,mediaKey("quality"),getString(R.string.TetogramAppearanceQuality),360,1920);}
    }
    private void toggle(ArrayList<UItem> items,String key,CharSequence label,boolean fallback){String value=resolved().get(key);if(value==null)value=Boolean.toString(fallback);final String selected=value;button(items,label,value+(draft.values.containsKey(key)?"":" · "+getString(R.string.TetogramAppearanceInherited)),()->{checkpoint();draft.set(key,Boolean.toString(!Boolean.parseBoolean(selected)));refresh();});}
    private void pickMedia(boolean video){
        if(mediaBusy||!valid())return;pickingVideo=video;Intent intent;
        if(Build.VERSION.SDK_INT>=33)intent=new Intent(MediaStore.ACTION_PICK_IMAGES).setType(video?"video/*":"image/*");else intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(video?"video/*":"image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(intent,PICK_APPEARANCE_MEDIA);}catch(Exception e){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(video?"video/*":"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),PICK_APPEARANCE_MEDIA);}catch(Exception ex){error(getString(R.string.TetogramAppearanceMediaError));}}
    }
    @Override public void onActivityResultFragment(int request,int result,Intent data){super.onActivityResultFragment(request,result,data);if(request!=PICK_APPEARANCE_MEDIA||result!=Activity.RESULT_OK||data==null||data.getData()==null||!valid())return;mediaBusy=true;if(preview!=null)preview.setResumed(false);refresh();mediaJob=AppearanceMedia.importUri(currentAccount,owner,data.getData(),pickingVideo,this::editSelectedMedia);}
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
            @Override public void onClose(){if(!applied[0]){pendingMedia.remove(source.file);AppearanceMedia.discard(source.file);}if(preview!=null)preview.setResumed(foreground&&!mediaBusy);}
        },null);
        if(opened)viewer.setTitle(getString(R.string.TetogramAppearanceWallpapers));
        if(!opened){pendingMedia.remove(source.file);AppearanceMedia.discard(source.file);if(preview!=null)preview.setResumed(foreground);error(getString(R.string.TetogramAppearanceMediaError));}
    }
    private void setCover(long time){AppearanceConfig.Resolved r=resolved();mediaBusy=true;if(preview!=null)preview.setResumed(false);refresh();mediaJob=AppearanceMedia.coverAt(currentAccount,owner,r.get(mediaKey("file")),time,asset->{mediaBusy=false;if(!valid()){if(asset!=null)AppearanceMedia.discard(asset.file);return;}if(asset==null)error(getString(R.string.TetogramAppearanceMediaError));else{checkpoint();pendingMedia.add(asset.file);draft.set(mediaKey("cover.file"),asset.file.getName());}refresh();});}
    private void pickChat(){pickChat(false);}
    private void pickChat(boolean groups){Bundle args=new Bundle();if(groups){args.putBoolean("allowUsers",false);args.putBoolean("allowBots",false);}args.putBoolean("onlySelect",true);args.putInt("dialogsType",0);DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(currentAccount);picker.setDelegate((f,dids,m,p,n,s,r,t)->{if(dids.isEmpty())return false;long did=dids.get(0).dialogId;f.finishFragment();editScope("chat:"+did,null,null,did,0);return true;});presentFragment(picker);}
    private void pickSection(boolean folders){try{SectionConfig c=SectionsStore.load(currentAccount);ArrayList<CharSequence> names=new ArrayList<>();ArrayList<String> ids=new ArrayList<>();if(folders){for(SectionConfig.Folder f:c.folders.values()){names.add(f.name);ids.add(f.id);}for(org.telegram.messenger.MessagesController.DialogFilter f:getMessagesController().getDialogFilters())if(f.id!=0){names.add(f.name);ids.add("tg:"+f.id);}}else for(SectionConfig.Section s:c.sections){names.add(s.name);ids.add(s.id);}if(ids.isEmpty()){presentFragment(new SectionsActivity(currentAccount));return;}showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(folders?R.string.TetogramAppearanceFolders:R.string.TetogramAppearanceSections)).setItems(names.toArray(new CharSequence[0]),(d,n)->{String id=ids.get(n);editScope((folders?"folder:":"section:")+id,folders?null:id,folders?id:null,0,0);}).create());}catch(Exception e){error(getString(R.string.TetogramStorageError));}}
    @Override protected void click(UItem item,View v,int p,float x,float y){if(item.id>0 && item.id<=actions.size() && valid()&&!saving)actions.get(item.id-1).run();}
}
