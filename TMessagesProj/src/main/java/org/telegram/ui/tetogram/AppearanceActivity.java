/* Tetogram, 2026. GNU GPL v2 or later. Drafts are local until Apply. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
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
    @Override public void onResume(){super.onResume();if(!valid())return;AppearanceStore.load(currentAccount,(ok,c)->{if(!valid())return;failed=!ok;if(draft==null || !"edit".equals(page)){config=c;expectedRevision=c.revision;draft=c.edit(scope).copy();}refresh();});}
    @Override public void onFragmentDestroy(){destroyed=true;super.onFragmentDestroy();}
    private void refresh(){if(list!=null)list.adapter.update(false);}
    private void button(ArrayList<UItem> items,CharSequence title,CharSequence value,Runnable action){actions.add(action);items.add(UItem.asButton(actions.size(),title,value).setEnabled(!saving));}
    private void button(ArrayList<UItem> items,CharSequence title,Runnable action){button(items,title,null,action);}
    private void checkpoint(){undo.push(draft.copy());}
    private AppearanceConfig.Resolved resolved(){AppearanceConfig c=config.copy();c.scopes.put(scope,draft.copy());return scope.startsWith("profile:")?c.resolveProfile(Long.parseLong(scope.substring(8))):c.resolve(new AppearanceConfig.Context(section,folder,dialog,topic));}
    private void editScope(String key,String section,String folder,long did,long tid){presentFragment(new AppearanceActivity(currentAccount,key,section,folder,did,tid));}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        actions.clear();if(!valid())return;
        if(failed){items.add(UItem.asShadow(getString(R.string.TetogramAppearanceStorageError)));return;}
        if(config==null){items.add(UItem.asShadow(getString(R.string.Loading)));return;}
        if("home".equals(page)){
            items.add(UItem.asShadow(getString(R.string.TetogramAppearanceLocal)));
            button(items,getString(R.string.TetogramAppearancePresets),()->presentFragment(new AppearanceActivity(currentAccount,"presets",AppearanceConfig.GLOBAL,null,null,0,0)));
            button(items,getString(R.string.TetogramAppearanceSimple),()->editScope(AppearanceConfig.GLOBAL,null,null,0,0));
            button(items,getString(R.string.TetogramAppearanceNativeThemes),()->presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
            button(items,getString(R.string.TetogramAppearanceChat),()->pickChat());
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
        items.add(UItem.asCustom(new AppearancePreview(getContext(),currentAccount,resolved(),previewDark,false),180));
        items.add(UItem.asCustom(new AppearancePreview(getContext(),currentAccount,resolved(),previewDark,true),156));
        if(searchInput!=null)items.add(UItem.asCustom(searchInput,48));
        String[] simple={"chat_wallpaper","chat_inBubble","chat_outBubble","chat_messageTextIn","chat_messageTextOut","actionBarDefault","chats_name","chats_unreadCounter"};
        ArrayList<String> keys=new ArrayList<>();
        if(advanced){for(int key=0;key<Theme.colorsCount;key++){String name=ThemeColors.getStringName(key);if(name!=null && !name.equals("wallpaperFileOffset") && !name.contains("rotation") && !name.toLowerCase(java.util.Locale.ROOT).contains("animated"))keys.add(name);}Collections.sort(keys);}else Collections.addAll(keys,simple);
        String prefix=previewDark?"dark.":"light.";AppearanceConfig.Resolved r=resolved();
        for(String name:keys){if(!search.isEmpty() && !name.toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT)))continue;
            String key=prefix+name;int nativeKey=ThemeColors.stringKeyToInt(name);Integer custom=r.color(previewDark,name);int color=custom==null?Theme.getColor(nativeKey):custom;
            String origin=draft.values.containsKey(key)?getString(R.string.TetogramAppearanceExplicit):getString(R.string.TetogramAppearanceInherited)+" · "+r.origins.getOrDefault(key,"Telegram");
            button(items,colorLabel(name),String.format(java.util.Locale.ROOT,"#%08X",color)+" · "+origin,()->colorDialog(name,color));
        }
        number(items,"bubble.inRadius",getString(R.string.TetogramAppearanceInRadius),0,32);
        number(items,"bubble.outRadius",getString(R.string.TetogramAppearanceOutRadius),0,32);
        button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();draft=new AppearanceConfig.Style();refresh();});
    }
    private CharSequence colorLabel(String name){switch(name){case "chat_wallpaper":return getString(R.string.TetogramAppearanceBackground);case "chat_inBubble":return getString(R.string.TetogramAppearanceIncoming);case "chat_outBubble":return getString(R.string.TetogramAppearanceOutgoing);case "chat_messageTextIn":return getString(R.string.TetogramAppearanceIncomingText);case "chat_messageTextOut":return getString(R.string.TetogramAppearanceOutgoingText);default:return name;}}
    private void number(ArrayList<UItem> items,String key,CharSequence label,int min,int max){button(items,label,draft.values.getOrDefault(key,getString(R.string.TetogramAppearanceInherited)),()->input(label,draft.values.getOrDefault(key,""),text->{try{float value=Float.parseFloat(text);if(Float.isNaN(value)||Float.isInfinite(value)||value<min||value>max)throw new IllegalArgumentException();checkpoint();draft.set(key,Float.toString(value));refresh();}catch(Exception e){error(getString(R.string.TetogramAppearanceInvalidValue));}}));}
    private void colorDialog(String name,int color){
        final int[] value={color};ColorPicker picker=new ColorPicker(getContext(),false,(c,n,apply)->value[0]=c);picker.setResourcesProvider(getResourceProvider());picker.setType(0,true,1,1,false,0,false);picker.setColor(color,0);
        showDialog(new AlertDialog.Builder(getContext()).setTitle(colorLabel(name)).setView(picker, AndroidUtilities.dp(320)).setPositiveButton(getString(R.string.OK),(d,n)->{checkpoint();draft.color(previewDark,name,value[0]);refresh();}).setNeutralButton(getString(R.string.TetogramAppearanceInherit),(d,n)->{checkpoint();draft.values.remove((previewDark?"dark.":"light.")+name);refresh();}).setNegativeButton(getString(R.string.Cancel),null).create());
    }
    private void presetActions(AppearanceConfig.Preset p,boolean builtin){ArrayList<CharSequence> names=new ArrayList<>();ArrayList<Runnable> runs=new ArrayList<>();names.add(getString(R.string.TetogramAppearanceApply));runs.add(()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,"edit",AppearanceConfig.GLOBAL,null,null,0,0);editor.config=config.copy();editor.expectedRevision=expectedRevision;editor.draft=p.style.copy();editor.previewDark="dark".equals(p.style.values.get("mode"));presentFragment(editor);});names.add(getString(R.string.TetogramAppearanceDuplicate));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),p.name,name->{AppearanceConfig.Preset duplicate=p.duplicate(name);config.presets.put(duplicate.id,duplicate);saveConfig(()->{});}));if(!builtin){names.add(getString(R.string.EditName));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),p.name,name->{p.name=name;saveConfig(()->{});}));names.add(getString(R.string.Delete));runs.add(()->showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceDeletePreset)).setPositiveButton(getString(R.string.Delete),(d,n)->{config.presets.remove(p.id);saveConfig(()->{});}).setNegativeButton(getString(R.string.Cancel),null).create()));}showDialog(new AlertDialog.Builder(getContext()).setTitle(p.name).setItems(names.toArray(new CharSequence[0]),(d,n)->runs.get(n).run()).create());}
    private void confirmReset(){showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceResetConfirm)).setPositiveButton(getString(R.string.OK),(d,n)->{config.scopes.clear();saveConfig(()->{});}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void saveConfig(Runnable done){if(saving||!valid())return;saving=true;AppearanceStore.save(currentAccount,owner,config,expectedRevision,(ok,c)->{if(!valid())return;saving=false;if(ok){config=c;expectedRevision=c.revision;done.run();refresh();}else error(getString(R.string.TetogramAppearanceConflict));});refresh();}
    private void input(CharSequence title,String initial,java.util.function.Consumer<String> done){EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(18);field.setText(initial);field.setSingleLine(true);field.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(8),AndroidUtilities.dp(24),AndroidUtilities.dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);showDialog(new AlertDialog.Builder(getContext()).setTitle(title).setView(field).setPositiveButton(getString(R.string.OK),(d,n)->{String value=field.getText().toString().trim();if(value.length()<=256&&!value.isEmpty())done.accept(value);}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void error(CharSequence text){if(getContext()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(text).setPositiveButton(getString(R.string.OK),null).create());}
    private void pickChat(){Bundle args=new Bundle();args.putBoolean("onlySelect",true);args.putInt("dialogsType",0);DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(currentAccount);picker.setDelegate((f,dids,m,p,n,s,r,t)->{if(dids.isEmpty())return false;long did=dids.get(0).dialogId;f.finishFragment();editScope("chat:"+did,null,null,did,0);return true;});presentFragment(picker);}
    private void pickSection(boolean folders){try{SectionConfig c=SectionsStore.load(currentAccount);ArrayList<CharSequence> names=new ArrayList<>();ArrayList<String> ids=new ArrayList<>();if(folders){for(SectionConfig.Folder f:c.folders.values()){names.add(f.name);ids.add(f.id);}for(org.telegram.messenger.MessagesController.DialogFilter f:getMessagesController().getDialogFilters())if(f.id!=0){names.add(f.name);ids.add("tg:"+f.id);}}else for(SectionConfig.Section s:c.sections){names.add(s.name);ids.add(s.id);}if(ids.isEmpty()){presentFragment(new SectionsActivity(currentAccount));return;}showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(folders?R.string.TetogramAppearanceFolders:R.string.TetogramAppearanceSections)).setItems(names.toArray(new CharSequence[0]),(d,n)->{String id=ids.get(n);editScope((folders?"folder:":"section:")+id,folders?null:id,folders?id:null,0,0);}).create());}catch(Exception e){error(getString(R.string.TetogramStorageError));}}
    @Override protected void click(UItem item,View v,int p,float x,float y){if(item.id>0 && item.id<=actions.size() && valid()&&!saving)actions.get(item.id-1).run();}
}
