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
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import static org.telegram.messenger.tetogram.AppearanceEditorPolicy.Category;
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
    private AppearanceConfig.Style draft,baseline;
    private Category category=Category.COLORS;
    private LinearLayout editorDock;
    private TextView applyButton;
    private View undoAction;
    private boolean leaving,hidePreview;
    private int wallpaperSurface;

    private String editingPresetId;
    private long expectedRevision;
    private boolean failed,saving,destroyed,advanced,previewDark;
    private String search="";
    private final ArrayList<Runnable> actions=new ArrayList<>();
    private final HashMap<String,String> scopeNames=new HashMap<>();
    private final ArrayDeque<AppearanceConfig.Style> undo=new ArrayDeque<>();
    private EditTextBoldCursor searchInput;
    private AppearancePreview preview;
    private FrameLayout previewHost;
    private AppearanceConfig.Resolved previewResolved;
    private int lastPreviewKind;private boolean lastPreviewDark,filterParameters;
    private boolean foreground,mediaBusy,previewDialogs;
    private int previewKind;
    private static final int PICK_APPEARANCE_MEDIA=61041,IMPORT_PRESET=61042,EXPORT_PRESET=61043,SAMPLE_COLOR=61045;
    private String sampledRole;
    private AppearanceConfig.Preset exporting;private boolean exportImages,exportVideos;
    private boolean pickingVideo;
    private AppearanceMedia.Job mediaJob;
    private final ArrayList<File> pendingMedia=new ArrayList<>();
    /** One mutation of the latest stored document; re-applied on top of a newer revision after a conflict. */
    private interface Change{boolean apply(AppearanceConfig fresh);}
    private static final class PendingSave{final Change change;final Runnable done,failed;final boolean applyMode;PendingSave(Change change,Runnable done,boolean applyMode,Runnable failed){this.change=change;this.done=done;this.applyMode=applyMode;this.failed=failed;}}
    private final ArrayDeque<PendingSave> queuedSaves=new ArrayDeque<>();
    private boolean overwriteScope,scopeConflict;
    private AppearanceConfig.Resolved fillResolved;
    private String advancedKeysFor;private final ArrayList<String> advancedKeys=new ArrayList<>();
    private final Runnable searchRefresh=this::refresh;
    // Studio layout: preview with its own header on top, category tabs above the pinned actions.
    private LinearLayout topPanel;private FrameLayout previewHeader,tabsHost;private String tabsState;
    private String selectedElement;
    public AppearanceActivity(int account){this(account,"edit",AppearanceConfig.GLOBAL,null,null,0,0);}
    public AppearanceActivity(int account,String scope,String section,String folder,long dialog,long topic){this(account,"edit",scope,section,folder,dialog,topic);}
    private AppearanceActivity(int account,String page,String scope,String section,String folder,long dialog,long topic){super(account);this.owner=UserConfig.getInstance(account).getClientUserId();this.page=page;this.scope=scope;this.section=section;this.folder=folder;this.dialog=dialog;this.topic=topic;previewKind=scope.startsWith("profile:")?2:section!=null?4:folder!=null?5:dialog<0?3:0;previewDialogs=previewKind==4||previewKind==5;category=scope.startsWith("profile:")?Category.PROFILE:section!=null||folder!=null?Category.SECTIONS:dialog!=0?Category.CHATS:Category.COLORS;wallpaperSurface=previewKind==2?2:previewDialogs?1:0;previewDark=Theme.isCurrentThemeDark();}
    @Override protected CharSequence title(){return getString("edit".equals(page)?categoryTitle(category):R.string.TetogramAppearance);}
    private boolean valid(){return !destroyed && owner>0 && owner==getUserConfig().getClientUserId();}
    @Override public View createView(Context context){
        if(preview!=null)preview.setResumed(false);preview=null;previewHost=null;previewResolved=null;topPanel=null;previewHeader=null;tabsHost=null;tabsState=null;
        if("edit".equals(page)){
            searchInput=new EditTextBoldCursor(context);searchInput.setSingleLine(true);searchInput.setTextSize(16);searchInput.setHint(getString(R.string.Search));searchInput.setPadding(AndroidUtilities.dp(16),0,AndroidUtilities.dp(16),0);ThemeBind.textColor(searchInput,Theme.key_windowBackgroundWhiteBlackText);ThemeBind.hintTextColor(searchInput,Theme.key_windowBackgroundWhiteHintText);
            searchInput.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){search=s.toString();AndroidUtilities.cancelRunOnUIThread(searchRefresh);AndroidUtilities.runOnUIThread(searchRefresh,220);}});
        }
        View root=super.createView(context);
        if("edit".equals(page))setupEditorChrome((FrameLayout)root);
        return root;
    }
    @Override public void onResume(){
        super.onResume();foreground=true;if(preview!=null)preview.setResumed(!mediaBusy&&!hidePreview);if(!valid())return;
        AppearanceStore.load(currentAccount,(ok,c)->{
            if(!valid())return;failed=!ok;
            AppearanceConfig.Style stored=editingPresetId==null?c.scopes.get(scope):c.presets.get(editingPresetId)==null?null:c.presets.get(editingPresetId).style;
            AppearanceConfig.Style saved=stored==null?new AppearanceConfig.Style():stored;
            if(draft==null||!"edit".equals(page)){config=c;expectedRevision=c.revision;draft=saved.copy();baseline=saved.copy();undo.clear();if("edit".equals(page)){AppearanceConfig.Resolved inherited=resolved();String mode=inherited.get("mode");previewDark=AppearanceConfig.GLOBAL.equals(inherited.origins.get("mode"))?Theme.isCurrentThemeDark():"dark".equals(mode)||!"light".equals(mode)&&Theme.isCurrentThemeDark();}}
            else if(baseline==null){baseline=saved.copy();config=c;expectedRevision=c.revision;}
            else if(!AppearanceEditorPolicy.dirtyAppearance(baseline,saved)){config=c;expectedRevision=c.revision;}
            else if(!hasChanges()){config=c;expectedRevision=c.revision;draft=saved.copy();baseline=saved.copy();undo.clear();}
            refresh();
        });
    }
    @Override public void onPause(){foreground=false;if(preview!=null)preview.setResumed(false);super.onPause();}
    @Override public void onFragmentDestroy(){destroyed=true;AndroidUtilities.cancelRunOnUIThread(searchRefresh);queuedSaves.clear();if(mediaJob!=null)mediaJob.cancel();if(preview!=null)preview.setResumed(false);for(File f:pendingMedia)AppearanceMedia.discard(f);pendingMedia.clear();super.onFragmentDestroy();}
    private void refresh(){refreshTabs();if(simpleActionBar!=null&&"edit".equals(page))simpleActionBar.setTitle(title());if(undoAction!=null){undoAction.setEnabled(!undo.isEmpty()&&!saving&&!mediaBusy);undoAction.setAlpha(undoAction.isEnabled()?1:.4f);}if(applyButton!=null){applyButton.setText(getString(editingPresetId==null?R.string.TetogramAppearanceApply:R.string.Save));boolean enabled=valid()&&!saving&&!mediaBusy&&AppearanceEditorPolicy.dirtyAppearance(baseline,draft);applyButton.setEnabled(enabled);applyButton.setAlpha(enabled?1:.45f);}if(list!=null)list.adapter.update(false);}
    private int categoryTitle(Category c){switch(c){case COLORS:return R.string.TetogramEditorColors;case WALLPAPERS:return R.string.TetogramAppearanceWallpapers;case LIST:return R.string.TetogramAppearanceDialogList;case CHATS:return R.string.TetogramEditorChats;case PROFILE:return R.string.TetogramEditorProfiles;case SECTIONS:return R.string.TetogramEditorSections;default:return R.string.TetogramEditorAdvanced;}}
    private void openCategory(Category c){AppearanceActivity editor=new AppearanceActivity(currentAccount,AppearanceConfig.GLOBAL,null,null,0,0);editor.selectCategory(c);presentFragment(editor);}
    private void selectCategory(Category c){if(c==Category.WALLPAPERS)wallpaperSurface=category==Category.PROFILE||previewKind==2&&!scope.startsWith("chat:")&&!scope.startsWith("topic:")?2:category==Category.LIST||category==Category.SECTIONS||previewKind==1||previewKind==4||previewKind==5?1:0;if(wallpaperSurface==2&&!scope.equals(AppearanceConfig.GLOBAL)&&!scope.startsWith("profile:"))wallpaperSurface=0;category=c;previewKind=c==Category.WALLPAPERS?wallpaperSurface:AppearanceEditorPolicy.preview(c);if(scope.startsWith("profile:"))previewKind=2;if(c==Category.CHATS&&dialog<0)previewKind=3;if(c==Category.SECTIONS&&folder!=null)previewKind=5;previewDialogs=previewKind==1||previewKind==4||previewKind==5;search="";if(searchInput!=null)searchInput.setText("");refresh();}
    private TextView editorText(CharSequence label){TextView view=new TextView(getContext());view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,16);view.setText(label);view.setGravity(Gravity.CENTER);view.setPadding(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12));ThemeBind.textColor(view,Theme.key_windowBackgroundWhiteBlueText);view.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));return view;}
    private void setupEditorChrome(FrameLayout root){
        undoAction=simpleActionBar.createMenu().addItem(9101,R.drawable.tetogram_editor_undo);undoAction.setContentDescription(getString(R.string.TetogramAppearanceUndo));simpleActionBar.createMenu().addItem(9102,R.drawable.ic_ab_other);
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();else if(id==9101){if(!undo.isEmpty()){draft=undo.pop();refresh();}}else if(id==9102)editorActions();}});
        editorDock=new LinearLayout(getContext());editorDock.setOrientation(LinearLayout.VERTICAL);editorDock.setPadding(0,AndroidUtilities.dp(8),0,AndroidUtilities.dp(6));
        tabsHost=new FrameLayout(getContext());editorDock.addView(tabsHost,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(40)));LinearLayout actionsRow=new LinearLayout(getContext());actionsRow.setOrientation(LinearLayout.HORIZONTAL);actionsRow.setPadding(AndroidUtilities.dp(8),AndroidUtilities.dp(4),AndroidUtilities.dp(8),0);editorDock.addView(actionsRow,new LinearLayout.LayoutParams(-1,-2));ThemeBind.backgroundColor(editorDock,Theme.key_windowBackgroundWhite);
        TextView cancel=editorText(getString(R.string.Cancel));cancel.setOnClickListener(v->finishFragment());actionsRow.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));applyButton=editorText(getString(R.string.TetogramAppearanceApply));applyButton.setEnabled(false);applyButton.setAlpha(.45f);applyButton.setOnClickListener(v->{if(valid()&&!saving&&!mediaBusy&&AppearanceEditorPolicy.dirtyAppearance(baseline,draft))applyDraft();});actionsRow.addView(applyButton,new LinearLayout.LayoutParams(0,-2,1));setBottomActions(editorDock);
    }
    private boolean hasChanges(){return "edit".equals(page)&&draft!=null&&baseline!=null&&AppearanceEditorPolicy.dirtyAppearance(baseline,draft);}
    @Override public boolean canBeginSlide(){return !hasChanges()&&!saving&&!mediaBusy&&super.canBeginSlide();}
    @Override public boolean onBackPressed(boolean invoked){if(!leaving&&valid()&&(hasChanges()||saving||mediaBusy)){if(invoked)requestExit();return false;}return super.onBackPressed(invoked);}
    @Override public void finishFragment(){if(!leaving&&valid()&&(hasChanges()||saving||mediaBusy)){requestExit();return;}super.finishFragment();}
    private void requestExit(){if(saving||mediaBusy){error(getString(R.string.TetogramAppearancePreparingMedia));return;}showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorUnsaved)).setMessage(getString(R.string.TetogramEditorUnsavedInfo)).setPositiveButton(getString(R.string.TetogramAppearanceApply),(d,n)->applyDraft()).setNeutralButton(getString(R.string.TetogramEditorDiscard),(d,n)->{leaving=true;finishFragment();}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void editorActions(){ArrayList<CharSequence> labels=new ArrayList<>();ArrayList<Runnable> runs=new ArrayList<>();
        labels.add(getString(R.string.TetogramAppearanceSavePreset));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),"",name->{AppearanceConfig.Preset preset=new AppearanceConfig.Preset();preset.name=name;preset.style=AppearanceConfig.withoutPersonal(draft);saveConfig(fresh->{fresh.presets.put(preset.id,preset);return true;},this::refresh,false);}));
        labels.add(getString(hidePreview?R.string.TetogramEditorShowPreview:R.string.TetogramEditorHidePreview));runs.add(()->{hidePreview=!hidePreview;if(preview!=null)preview.setResumed(!hidePreview&&foreground&&!mediaBusy);refresh();});
        labels.add(getString(R.string.TetogramEditorExpandPreview));runs.add(()->presentFragment(new AppearancePreviewPage(currentAccount,previewStyle(),previewDark,previewKind,section)));
        labels.add(getString(R.string.TetogramAppearanceMode));runs.add(()->{previewDark=!previewDark;refresh();});
        labels.add(getString(R.string.TetogramAppearancePreviewSurface));runs.add(this::choosePreview);
        labels.add(getString(R.string.TetogramEditorChangeCategory));runs.add(()->{ArrayList<Category> categories=new ArrayList<>();for(Category candidate:Category.values())if(candidate!=Category.LAUNCHER&&AppearanceEditorPolicy.supports(scope,candidate))categories.add(candidate);CharSequence[] names=new CharSequence[categories.size()];for(int i=0;i<names.length;i++)names[i]=getString(categoryTitle(categories.get(i)));showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorChangeCategory)).setItems(names,(d,n)->selectCategory(categories.get(n))).create());});
        labels.add(getString(R.string.TetogramAppearanceReset));runs.add(()->{checkpoint();draft=category==Category.WALLPAPERS?AppearanceEditorPolicy.resetWallpaper(draft,mediaPrefix()):AppearanceEditorPolicy.resetCategory(draft,category);refresh();});
        showDialog(new AlertDialog.Builder(getContext()).setTitle(title()).setItems(labels.toArray(new CharSequence[0]),(d,n)->runs.get(n).run()).create());
    }
    private void modeControl(ArrayList<UItem> items){String mode=resolved().get("mode");button(items,getString(scope.startsWith("profile:")?R.string.TetogramAppearanceBannerMode:R.string.TetogramAppearanceDisplayMode),getString("dark".equals(mode)?R.string.TetogramAppearanceDark:"light".equals(mode)?R.string.TetogramAppearanceLight:R.string.TetogramAppearanceInherited),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(scope.startsWith("profile:")?R.string.TetogramAppearanceBannerMode:R.string.TetogramAppearanceDisplayMode)).setItems(new CharSequence[]{getString(R.string.TetogramAppearanceLight),getString(R.string.TetogramAppearanceDark),getString(R.string.TetogramAppearanceInherit)},(d,n)->{checkpoint();if(n==2){draft.values.remove("mode");AppearanceConfig.Resolved inherited=resolved();String inheritedMode=inherited.get("mode");previewDark=AppearanceConfig.GLOBAL.equals(inherited.origins.get("mode"))?Theme.isCurrentThemeDark():"dark".equals(inheritedMode)||!"light".equals(inheritedMode)&&Theme.isCurrentThemeDark();}else{draft.set("mode",n==0?"light":"dark");previewDark=n==1;}refresh();}).create()));}
    private void categoryLinks(ArrayList<UItem> items){if(category==Category.CHATS&&dialog==0)button(items,getString(R.string.TetogramEditorChooseChat),this::pickChat);if(category==Category.SECTIONS){button(items,getString(R.string.TetogramAppearanceSections),()->pickSection(false));button(items,getString(R.string.TetogramAppearanceFolders),()->pickSection(true));}}
    private void wallpaperTarget(ArrayList<UItem> items){button(items,getString(R.string.TetogramEditorBackgroundTarget),getString(wallpaperSurface==1&&dialog!=0?R.string.TetogramEditorChatRow:surfaceLabel(wallpaperSurface)),()->{ArrayList<Integer> choices=new ArrayList<>();if(scope.startsWith("profile:"))choices.add(2);else{choices.add(0);choices.add(1);if(scope.equals(AppearanceConfig.GLOBAL))choices.add(2);}CharSequence[] names=new CharSequence[choices.size()];for(int i=0;i<names.length;i++)names[i]=getString(choices.get(i)==1&&dialog!=0?R.string.TetogramEditorChatRow:surfaceLabel(choices.get(i)));showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorBackgroundTarget)).setItems(names,(d,n)->{wallpaperSurface=choices.get(n);previewKind=wallpaperSurface;previewDialogs=previewKind==1;refresh();}).create());});}
    private void listStyles(ArrayList<UItem> items){button(items,getString(R.string.TetogramEditorListStyle),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorListStyle)).setItems(new CharSequence[]{getString(R.string.TetogramEditorTransparent),getString(R.string.TetogramEditorGlass),getString(R.string.TetogramEditorCards)},(d,n)->{checkpoint();draft.set("list.rowOpacity",n==0?"0":n==1?"0.55":"0.94");draft.set("list.radius",n==2?"16":"0");draft.set("list.sideInset",n==2?"10":"0");draft.set("list.rowGap",n==2?"6":"0");draft.set("list.separators",Boolean.toString(n!=2));refresh();}).create()));}
    private void updatePreview(){
        AppearanceConfig.Resolved value=previewStyle();if(preview==null)preview=new AppearancePreview(getContext(),currentAccount,value,previewDark,previewKind,section);else preview.updateSurface(value,previewDark,previewKind);
        if(previewHost==null){previewHost=new FrameLayout(getContext());previewHost.addView(preview,LayoutHelper.createFrame(-1,-1));LinearLayout tools=new LinearLayout(getContext());tools.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(18),Theme.getColor(Theme.key_dialogBackground)));ThemeBind.drawableColorFilter(tools,tools.getBackground(),Theme.key_dialogBackground,android.graphics.PorterDuff.Mode.SRC_IN);
            android.widget.ImageView expand=new android.widget.ImageView(getContext());expand.setImageResource(R.drawable.msg_expand);expand.setContentDescription(getString(R.string.TetogramEditorExpandPreview));ThemeBind.colorFilter(expand,Theme.key_dialogTextBlack,android.graphics.PorterDuff.Mode.SRC_IN);expand.setPadding(AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8));expand.setOnClickListener(v->presentFragment(new AppearancePreviewPage(currentAccount,previewStyle(),previewDark,previewKind,section)));tools.addView(expand,new LinearLayout.LayoutParams(AndroidUtilities.dp(40),AndroidUtilities.dp(40)));
            android.widget.ImageView hide=new android.widget.ImageView(getContext());hide.setImageResource(R.drawable.tetogram_editor_hide);hide.setContentDescription(getString(R.string.TetogramEditorHidePreview));ThemeBind.colorFilter(hide,Theme.key_dialogTextBlack,android.graphics.PorterDuff.Mode.SRC_IN);hide.setPadding(AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8));hide.setOnClickListener(v->{hidePreview=true;preview.setResumed(false);refresh();});tools.addView(hide,new LinearLayout.LayoutParams(AndroidUtilities.dp(40),AndroidUtilities.dp(40)));FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);position.topMargin=position.rightMargin=AndroidUtilities.dp(8);previewHost.addView(tools,position);
        }
        if(topPanel==null){
            topPanel=new LinearLayout(getContext());topPanel.setOrientation(LinearLayout.VERTICAL);ThemeBind.backgroundColor(topPanel,Theme.key_windowBackgroundGray);
            previewHeader=new FrameLayout(getContext());topPanel.addView(previewHeader,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(48)));
            AndroidUtilities.removeFromParent(previewHost);
            previewHost.setClipToOutline(true);previewHost.setOutlineProvider(new android.view.ViewOutlineProvider(){@Override public void getOutline(View view,android.graphics.Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),AndroidUtilities.dp(20));}});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);lp.leftMargin=lp.rightMargin=AndroidUtilities.dp(12);lp.bottomMargin=AndroidUtilities.dp(8);topPanel.addView(previewHost,lp);
        }
        updatePreviewHeader();
        preview.setElementListener(this::onElementPicked);
        if(selectedElement!=null&&!AppearanceElements.onSurface(selectedElement,previewKind))selectedElement=null;
        preview.setHighlighted(selectedElement);
        previewResolved=value;lastPreviewKind=previewKind;lastPreviewDark=previewDark;preview.setResumed(valid()&&foreground&&!mediaBusy&&!hidePreview);
    }
    /** What the preview shows (chat / list / profile) and where the change applies, right above the preview. */
    private void updatePreviewHeader(){
        if(previewHeader==null)return;previewHeader.removeAllViews();
        LinearLayout row=new LinearLayout(getContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(AndroidUtilities.dp(12),AndroidUtilities.dp(6),AndroidUtilities.dp(12),AndroidUtilities.dp(6));
        ArrayList<Integer> kinds=new ArrayList<>();ArrayList<CharSequence> labels=new ArrayList<>();
        boolean profileScope=scope.startsWith("profile:");
        if(!profileScope){kinds.add(dialog<0?3:0);labels.add(getString(R.string.TetogramStudioPreviewChat));kinds.add(section!=null?4:folder!=null?5:1);labels.add(getString(R.string.TetogramStudioTabList));}
        if(profileScope||scope.equals(AppearanceConfig.GLOBAL)){kinds.add(2);labels.add(getString(R.string.TetogramStudioTabProfile));}
        int selected=-1;Runnable[] runs=new Runnable[kinds.size()];
        for(int i=0;i<kinds.size();i++){int kind=kinds.get(i);boolean listKind=kind==1||kind==4||kind==5,chatKind=kind==0||kind==3;if(kind==previewKind||listKind&&(previewKind==1||previewKind==4||previewKind==5)||chatKind&&(previewKind==0||previewKind==3))selected=i;runs[i]=()->{previewKind=kind;previewDialogs=kind==1||kind==4||kind==5;if(category==Category.WALLPAPERS)wallpaperSurface=listKind?1:kind==2?2:0;refresh();};}
        if(kinds.size()>1)row.addView(AppearanceStudioViews.segments(getContext(),labels.toArray(new CharSequence[0]),selected,runs),new LinearLayout.LayoutParams(0,AndroidUtilities.dp(36),1));
        else row.addView(new View(getContext()),new LinearLayout.LayoutParams(0,1,1));
        TextView chip=AppearanceStudioViews.pill(getContext(),LocaleController.formatString(R.string.TetogramStudioFor,scopeShortLabel())+" ▾",false,true);chip.setMaxWidth(AndroidUtilities.dp(180));chip.setEllipsize(TextUtils.TruncateAt.END);chip.setOnClickListener(v->chooseScope());
        LinearLayout.LayoutParams chipParams=new LinearLayout.LayoutParams(-2,AndroidUtilities.dp(36));chipParams.leftMargin=AndroidUtilities.dp(8);row.addView(chip,chipParams);
        previewHeader.addView(row,LayoutHelper.createFrame(-1,-1));
    }
    private String scopeShortLabel(){if(editingPresetId!=null){AppearanceConfig.Preset p=config==null?null:config.presets.get(editingPresetId);return p==null?getString(R.string.TetogramAppearancePresetName):p.name;}return scope.equals(AppearanceConfig.GLOBAL)?getString(R.string.TetogramStudioAllChats):scopeLabel(scope);}
    /** Where the change applies; other places open their own editor, keeping this draft underneath. */
    private void chooseScope(){
        if(editingPresetId!=null)return;
        ArrayList<CharSequence> names=new ArrayList<>();ArrayList<Runnable> runs=new ArrayList<>();
        if(!scope.equals(AppearanceConfig.GLOBAL)){names.add(getString(R.string.TetogramStudioAllChats));runs.add(()->openCategory(category==Category.PROFILE?Category.COLORS:category));}
        names.add(getString(R.string.TetogramStudioScopeChat));runs.add(this::pickChat);
        names.add(getString(R.string.TetogramStudioScopeSection));runs.add(()->pickSection(false));
        names.add(getString(R.string.TetogramStudioScopeFolder));runs.add(()->pickSection(true));
        if(!scope.startsWith("profile:")){names.add(getString(R.string.TetogramStudioScopeProfile));runs.add(()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,"profile:"+owner,null,null,0,0);editor.selectCategory(Category.PROFILE);presentFragment(editor);});}
        showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramStudioScope)).setItems(names.toArray(new CharSequence[0]),(d,n)->runs.get(n).run()).create());
    }
    private ArrayList<Category> tabCategories(){ArrayList<Category> out=new ArrayList<>();for(Category c:new Category[]{Category.COLORS,Category.WALLPAPERS,Category.CHATS,Category.LIST,Category.PROFILE,Category.SECTIONS,Category.ADVANCED})if(AppearanceEditorPolicy.supports(scope,c))out.add(c);return out;}
    private int tabTitle(Category c){switch(c){case COLORS:return R.string.TetogramStudioTabColors;case WALLPAPERS:return R.string.TetogramStudioTabBackground;case CHATS:return R.string.TetogramStudioTabBubbles;case LIST:return R.string.TetogramStudioTabList;case PROFILE:return R.string.TetogramStudioTabProfile;case SECTIONS:return R.string.TetogramStudioTabSections;default:return R.string.TetogramStudioTabMore;}}
    private void refreshTabs(){
        if(tabsHost==null||!"edit".equals(page))return;
        String state=category+"|"+scope;if(state.equals(tabsState))return;tabsState=state;
        ArrayList<Category> tabs=tabCategories();CharSequence[] labels=new CharSequence[tabs.size()];Runnable[] runs=new Runnable[tabs.size()];int selected=-1;
        for(int i=0;i<tabs.size();i++){Category c=tabs.get(i);labels[i]=getString(tabTitle(c));if(c==category)selected=i;runs[i]=()->{if(c==category||saving||mediaBusy)return;if(c==Category.PROFILE&&scope.equals(AppearanceConfig.GLOBAL)){AppearanceActivity editor=new AppearanceActivity(currentAccount,"profile:"+owner,null,null,0,0);editor.selectCategory(Category.PROFILE);presentFragment(editor);return;}selectCategory(c);};}
        tabsHost.removeAllViews();tabsHost.addView(AppearanceStudioViews.tabs(getContext(),labels,selected,runs),LayoutHelper.createFrame(-1,-1));
    }
    // ---- Tap an element in the preview to edit its colors ----
    private void onElementPicked(String element){
        if(!valid()||saving||mediaBusy)return;
        if(!AppearanceElements.allowed(element,scope)){error(getString(R.string.TetogramStudioElementElsewhere));return;}
        selectedElement=element.equals(selectedElement)?null:element;
        if(preview!=null)preview.setHighlighted(selectedElement);
        refresh();
        if(list!=null&&selectedElement!=null)list.scrollToPosition(0);
    }
    private int elementTitle(String element){switch(element){case AppearanceElements.WALLPAPER:return R.string.TetogramStudioElementWallpaper;case AppearanceElements.INCOMING:return R.string.TetogramStudioElementIncoming;case AppearanceElements.OUTGOING:return R.string.TetogramStudioElementOutgoing;case AppearanceElements.HEADER:return R.string.TetogramStudioElementHeader;case AppearanceElements.INPUT:return R.string.TetogramStudioElementInput;case AppearanceElements.LIST_ROW:return R.string.TetogramStudioElementListRow;case AppearanceElements.LIST_BACKGROUND:return R.string.TetogramStudioElementListBackground;case AppearanceElements.PROFILE_NAME:return R.string.TetogramStudioElementProfileName;case AppearanceElements.PROFILE_STATUS:return R.string.TetogramStudioElementProfileStatus;case AppearanceElements.BANNER:return R.string.TetogramStudioElementBanner;default:return R.string.TetogramStudioElementSection;}}
    private int roleTitle(String name){
        if(name.contains("wallpaper")||name.endsWith("Bubble")||name.equals("actionBarDefault")||name.endsWith("PanelBackground")||name.endsWith("windowBackgroundWhite")||name.equals("section.background"))return R.string.TetogramStudioRoleBackground;
        if(name.endsWith("TimeText")||name.endsWith("chats_date"))return R.string.TetogramStudioRoleTime;
        if(name.endsWith("DefaultTitle"))return R.string.TetogramStudioRoleTitle;
        if(name.endsWith("DefaultIcon"))return R.string.TetogramStudioRoleIcons;
        if(name.endsWith("PanelHint"))return R.string.TetogramStudioRoleHint;
        if(name.endsWith("chats_name"))return R.string.TetogramStudioRoleName;
        if(name.endsWith("chats_message"))return R.string.TetogramStudioRoleMessage;
        if(name.endsWith("unreadCounter")||name.equals("section.badge"))return R.string.TetogramStudioRoleBadge;
        if(name.equals("section.badgeText"))return R.string.TetogramStudioRoleBadgeText;
        return R.string.TetogramStudioRoleText;
    }
    /** Current value of a role: explicit, inherited, legacy list key, or the native Telegram color. */
    private int currentColor(String name){
        AppearanceConfig.Resolved r=resolved();Integer value=r.color(previewDark,name);
        if(value==null&&name.startsWith("list."))value=r.color(previewDark,name.substring(5));
        if(value!=null)return value;
        if(name.equals("banner.text"))return AppearanceRuntime.baseColor(previewDark,Theme.key_profile_title);
        if(name.equals("banner.secondaryText"))return AppearanceRuntime.baseColor(previewDark,Theme.key_profile_status);
        if(name.startsWith("section."))return AppearanceRuntime.baseColor(previewDark,name.contains("Text")?Theme.key_chats_actionIcon:Theme.key_chats_actionBackground);
        String plain=name.startsWith("list.")?name.substring(5):name.startsWith("banner.")?name.substring(7):name;
        int key=ThemeColors.stringKeyToInt(plain);int color=key<0?0:AppearanceRuntime.baseColor(previewDark,key);
        if(plain.equals("chat_wallpaper")&&color==0)color=previewDark?0xff17212b:0xffe6ebee;
        return color;
    }
    private static final int[] QUICK_COLORS={0xff7B5CE6,0xffE8577E,0xffD93A3A,0xffF08C2E,0xff1FA88F,0xff3C8DD9,0xff2E3440,0xffFFFFFF};
    private void elementPanel(ArrayList<UItem> items){
        if(selectedElement==null){if(category==Category.COLORS&&!advanced)items.add(UItem.asShadow(getString(R.string.TetogramStudioTapHint)));return;}
        List<AppearanceElements.Role> roles=AppearanceElements.roles(selectedElement);if(roles.isEmpty())return;
        String main=roles.get(0).name;int mainColor=currentColor(main);
        LinearLayout panel=new LinearLayout(getContext());panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(AndroidUtilities.dp(16),AndroidUtilities.dp(10),AndroidUtilities.dp(16),AndroidUtilities.dp(12));
        LinearLayout title=new LinearLayout(getContext());title.setOrientation(LinearLayout.HORIZONTAL);title.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=new TextView(getContext());name.setText(getString(elementTitle(selectedElement)));name.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,16);name.setTypeface(AndroidUtilities.bold());ThemeBind.textColor(name,Theme.key_windowBackgroundWhiteBlackText);title.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        int text=-1;for(int i=0;i<roles.size();i++)if(!roles.get(i).surface){text=i;break;}
        if(text>=0){
            boolean profile=selectedElement.startsWith("profile")||selectedElement.equals(AppearanceElements.BANNER),listElement=selectedElement.equals(AppearanceElements.LIST_ROW);
            String canvasRole=profile?"banner.chat_wallpaper":listElement?"list.chat_wallpaper":"chat_wallpaper";int canvas=currentColor(canvasRole)|0xff000000;
            int bg=AppearanceElements.backgroundOf(roles,text);int surfaceColor=bg>=0?currentColor(roles.get(bg).name):canvas,textColor=currentColor(roles.get(text).name);
            boolean ok=AppearanceColors.visibleRatio(textColor,surfaceColor,canvas)>=4.5;
            TextView badge=new TextView(getContext());badge.setText(getString(ok?R.string.TetogramStudioReadable:R.string.TetogramStudioHardToRead));badge.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,12);badge.setTypeface(AndroidUtilities.bold());badge.setPadding(AndroidUtilities.dp(8),AndroidUtilities.dp(3),AndroidUtilities.dp(8),AndroidUtilities.dp(3));badge.setTextColor(ok?0xff1C6B3C:0xff8A2A12);badge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8),ok?0xffDFF3E6:0xffFCE3DA));title.addView(badge,new LinearLayout.LayoutParams(-2,-2));}
        TextView close=new TextView(getContext());close.setText("✕");close.setTextSize(18);close.setGravity(Gravity.CENTER);close.setContentDescription(getString(R.string.Close));ThemeBind.textColor(close,Theme.key_windowBackgroundWhiteGrayText);close.setOnClickListener(v->{selectedElement=null;if(preview!=null)preview.setHighlighted(null);refresh();});title.addView(close,new LinearLayout.LayoutParams(AndroidUtilities.dp(40),AndroidUtilities.dp(40)));
        panel.addView(title,new LinearLayout.LayoutParams(-1,-2));
        // quick colors for the main role
        LinearLayout swatches=new LinearLayout(getContext());swatches.setOrientation(LinearLayout.HORIZONTAL);
        for(int color:QUICK_COLORS){View dot=swatch(color,(mainColor|0xff000000)==color,v->{if(valid()&&!saving&&!mediaBusy)acceptColor(main,(mainColor&0xff000000)==0?color:(color&0xffffff)|(mainColor&0xff000000));});dot.setContentDescription(AppearanceColors.hex(color));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,AndroidUtilities.dp(34),1);lp.rightMargin=AndroidUtilities.dp(6);swatches.addView(dot,lp);}
        TextView custom=new TextView(getContext());custom.setText("+");custom.setTextSize(20);custom.setGravity(Gravity.CENTER);custom.setContentDescription(getString(R.string.TetogramStudioCustomColor));ThemeBind.textColor(custom,Theme.key_windowBackgroundWhiteGrayText);custom.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(17),Theme.getColor(Theme.key_windowBackgroundGray)));custom.setOnClickListener(v->colorDialog(main,mainColor));swatches.addView(custom,new LinearLayout.LayoutParams(0,AndroidUtilities.dp(34),1));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=AndroidUtilities.dp(10);panel.addView(swatches,sp);
        // every role of the element
        LinearLayout rolesRow=new LinearLayout(getContext());rolesRow.setOrientation(LinearLayout.HORIZONTAL);
        android.widget.HorizontalScrollView rolesScroll=new android.widget.HorizontalScrollView(getContext());rolesScroll.setHorizontalScrollBarEnabled(false);rolesScroll.addView(rolesRow);
        for(AppearanceElements.Role role:roles){int color=currentColor(role.name);LinearLayout chip=new LinearLayout(getContext());chip.setOrientation(LinearLayout.HORIZONTAL);chip.setGravity(Gravity.CENTER_VERTICAL);chip.setPadding(AndroidUtilities.dp(8),0,AndroidUtilities.dp(12),0);chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12),Theme.getColor(Theme.key_windowBackgroundGray)));
            View dot=new View(getContext());dot.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6),color|0xff000000));chip.addView(dot,new LinearLayout.LayoutParams(AndroidUtilities.dp(22),AndroidUtilities.dp(22)));
            TextView label=new TextView(getContext());label.setText(getString(roleTitle(role.name)));label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,13);ThemeBind.textColor(label,Theme.key_windowBackgroundWhiteBlackText);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=AndroidUtilities.dp(8);chip.addView(label,lp);
            chip.setContentDescription(label.getText());chip.setOnClickListener(v->colorDialog(role.name,color));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,AndroidUtilities.dp(40));cp.rightMargin=AndroidUtilities.dp(8);rolesRow.addView(chip,cp);}
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=AndroidUtilities.dp(10);panel.addView(rolesScroll,rp);
        // recent colors
        List<Integer> recent=config.palette(AppearanceConfig.RECENT_COLORS);
        if(!recent.isEmpty()){LinearLayout row=new LinearLayout(getContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=new TextView(getContext());label.setText(getString(R.string.TetogramEditorRecent));label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,12);ThemeBind.textColor(label,Theme.key_windowBackgroundWhiteGrayText);row.addView(label,new LinearLayout.LayoutParams(-2,-2));
            for(int i=0;i<Math.min(8,recent.size());i++){int color=recent.get(i);View dot=swatch(color,false,v->{if(valid()&&!saving&&!mediaBusy)acceptColor(main,color);});dot.setContentDescription(AppearanceColors.hex(color));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(AndroidUtilities.dp(26),AndroidUtilities.dp(26));lp.leftMargin=AndroidUtilities.dp(8);row.addView(dot,lp);}
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=AndroidUtilities.dp(10);panel.addView(row,lp);}
        // actions
        LinearLayout actionsRow=new LinearLayout(getContext());actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        boolean explicit=false;String prefix=previewDark?"dark.":"light.";for(AppearanceElements.Role role:roles)if(draft.values.containsKey(prefix+role.name))explicit=true;
        if(explicit){TextView inherit=editorText(getString(scope.equals(AppearanceConfig.GLOBAL)?R.string.TetogramStudioResetElement:R.string.TetogramStudioLikeParent));inherit.setTextSize(14);inherit.setOnClickListener(v->{if(!valid()||saving||mediaBusy)return;checkpoint();for(AppearanceElements.Role role:roles)draft.values.remove(prefix+role.name);refresh();});actionsRow.addView(inherit,new LinearLayout.LayoutParams(0,-2,1));}
        if(selectedElement.equals(AppearanceElements.WALLPAPER)||selectedElement.equals(AppearanceElements.LIST_BACKGROUND)||selectedElement.equals(AppearanceElements.BANNER)){TextView media=editorText(getString(R.string.TetogramStudioPhotoVideo));media.setTextSize(14);media.setOnClickListener(v->selectCategory(Category.WALLPAPERS));actionsRow.addView(media,new LinearLayout.LayoutParams(0,-2,1));}
        if(actionsRow.getChildCount()>0){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=AndroidUtilities.dp(6);panel.addView(actionsRow,lp);}
        items.add(UItem.asCustom(panel,-2));
        items.add(UItem.asShadow(null));
    }
    private View swatch(int color,boolean selected,View.OnClickListener click){
        View dot=new View(getContext());
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);shape.setColor(color|0xff000000);
        int ring=selected?Theme.getColor(Theme.key_windowBackgroundWhiteBlackText):AppearancePresets.ratio(color|0xff000000,Theme.getColor(Theme.key_windowBackgroundWhite))<1.4?Theme.getColor(Theme.key_windowBackgroundWhiteGrayText):0;
        if(ring!=0)shape.setStroke(AndroidUtilities.dp(selected?3:1),ring);
        dot.setBackground(shape);dot.setOnClickListener(click);dot.setSelected(selected);return dot;
    }
    /** Theme thumbnails: tapping one previews it in the draft; Apply saves it, personal data is kept. */
    private void themeCarousel(ArrayList<UItem> items){
        if(editingPresetId!=null||scope.startsWith("profile:"))return;
        ArrayList<AppearanceConfig.Preset> presets=new ArrayList<>(AppearancePresets.builtins());presets.addAll(config.presets.values());
        ArrayList<AppearanceStudioViews.ThemeCard> cards=new ArrayList<>();
        for(AppearanceConfig.Preset p:presets){
            AppearanceConfig sample=new AppearanceConfig();sample.scopes.put(AppearanceConfig.GLOBAL,AppearanceConfig.withoutPersonal(p.style));
            AppearanceConfig.Resolved r=sample.resolve(AppearanceConfig.Context.direct(0,0));String mode=r.get("mode");boolean dark="dark".equals(mode)||!"light".equals(mode)&&previewDark;
            android.util.SparseIntArray colors=AppearanceRuntime.colors(r,dark);
            int top=colors.get(Theme.key_chat_wallpaper,AppearanceRuntime.baseColor(dark,Theme.key_chat_wallpaper));if(top==0)top=dark?0xff17212b:0xffe6ebee;
            int bottom=colors.get(Theme.key_chat_wallpaper_gradient_to1,top);if(bottom==0)bottom=top;
            int in=colors.get(Theme.key_chat_inBubble,AppearanceRuntime.baseColor(dark,Theme.key_chat_inBubble)),out=colors.get(Theme.key_chat_outBubble,AppearanceRuntime.baseColor(dark,Theme.key_chat_outBubble));
            boolean selected=AppearanceConfig.sameAppearance(draft,p.style);
            cards.add(new AppearanceStudioViews.ThemeCard(p.name,top|0xff000000,bottom|0xff000000,in|0xff000000,out|0xff000000,selected,()->{if(!valid()||saving||mediaBusy||AppearanceConfig.sameAppearance(draft,p.style))return;checkpoint();draft=scope.equals(AppearanceConfig.GLOBAL)?AppearanceConfig.applyPreset(draft,p.style):AppearanceConfig.withoutPersonal(p.style);String m=p.style.values.get("mode");if("dark".equals(m))previewDark=true;else if("light".equals(m))previewDark=false;refresh();}));
        }
        items.add(UItem.asHeader(getString(R.string.TetogramStudioThemes)));
        items.add(UItem.asCustom(AppearanceStudioViews.themes(getContext(),cards),-2));
    }
    private void button(ArrayList<UItem> items,CharSequence title,CharSequence value,Runnable action){if(filterParameters&&!search.isEmpty()){String query=search.toLowerCase(java.util.Locale.ROOT);if(!title.toString().toLowerCase(java.util.Locale.ROOT).contains(query)&&(value==null||!value.toString().toLowerCase(java.util.Locale.ROOT).contains(query)))return;}items.add(UItem.asCustom(new AppearanceSettingRow(getContext(),title,value,!saving&&!mediaBusy,()->{if(valid()&&!saving&&!mediaBusy)action.run();}),-2));}
    private void button(ArrayList<UItem> items,CharSequence title,Runnable action){button(items,title,null,action);}
    private void checkpoint(){if(undo.size()>=64)undo.removeLast();undo.push(draft.copy());}
    /** One resolution per list rebuild: fill() only reads the draft, so every row can share it. */
    private AppearanceConfig.Resolved resolved(){if(fillResolved!=null)return fillResolved;return resolveNow();}
    private AppearanceConfig.Resolved resolveNow(){AppearanceConfig.Style old=config.scopes.put(scope,draft);try{return scope.startsWith("profile:")?config.resolveProfile(Long.parseLong(scope.substring(8))):config.resolve(new AppearanceConfig.Context(section,folder,dialog,topic));}finally{if(old==null)config.scopes.remove(scope);else config.scopes.put(scope,old);}}
    private void editScope(String key,String section,String folder,long did,long tid){presentFragment(new AppearanceActivity(currentAccount,key,section,folder,did,tid));}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        actions.clear();filterParameters=false;if(!valid())return;
        if(failed){items.add(UItem.asShadow(getString(R.string.TetogramAppearanceStorageError)));if(AppearanceStore.invalid(currentAccount))button(items,getString(R.string.TetogramEditorRecover),this::confirmRecover);return;}
        if(config==null){items.add(UItem.asShadow(getString(R.string.Loading)));return;}
        if(draft!=null)fillResolved=resolveNow();
        try{fillItems(items);}finally{fillResolved=null;}
    }
    private void confirmRecover(){showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorRecover)).setMessage(getString(R.string.TetogramEditorRecoverInfo)).setPositiveButton(getString(R.string.OK),(d,n)->AppearanceStore.recover(currentAccount,(ok,c)->{if(!valid())return;if(!ok){error(getString(R.string.TetogramAppearanceStorageError));return;}failed=false;config=c;expectedRevision=c.revision;AppearanceConfig.Style stored=c.scopes.get(scope);draft=stored==null?new AppearanceConfig.Style():stored.copy();baseline=draft.copy();undo.clear();refresh();})).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void fillItems(ArrayList<UItem> items){
        prepareScopeNames();if("edit".equals(page)&&simpleActionBar!=null)simpleActionBar.setSubtitle(editingPresetId==null?scopeLabel(scope):getString(R.string.TetogramAppearancePresetName));
        if("home".equals(page)){
            items.add(UItem.asShadow(getString(R.string.TetogramAppearanceLocal)));
            button(items,getString(R.string.TetogramAppearancePresets),()->presentFragment(new AppearanceActivity(currentAccount,"presets",AppearanceConfig.GLOBAL,null,null,0,0)));
            button(items,getString(R.string.TetogramEditorColors),()->openCategory(Category.COLORS));
            button(items,getString(R.string.TetogramAppearanceWallpapers),()->openCategory(Category.WALLPAPERS));
            button(items,getString(R.string.TetogramAppearanceDialogList),()->openCategory(Category.LIST));
            button(items,getString(R.string.TetogramEditorChats),()->openCategory(Category.CHATS));
            button(items,getString(R.string.TetogramEditorProfiles),()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,"profile:"+owner,null,null,0,0);editor.selectCategory(Category.PROFILE);presentFragment(editor);});
            button(items,getString(R.string.TetogramEditorSections),()->openCategory(Category.SECTIONS));
            button(items,getString(R.string.TetogramAppearanceLauncher),()->presentFragment(new AppearanceLauncherActivity(currentAccount)));
            button(items,getString(R.string.TetogramEditorAdvanced),()->openCategory(Category.ADVANCED));
            return;
        }
        if("presets".equals(page)){
            button(items,getString(R.string.TetogramAppearanceCreatePreset),()->input(getString(R.string.TetogramAppearancePresetName),"",name->{AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name=name;saveConfig(fresh->{p.style=AppearanceConfig.withoutPersonal(fresh.scopes.get(AppearanceConfig.GLOBAL));fresh.presets.put(p.id,p);return true;},null,false);}));
            button(items,getString(R.string.TetogramAppearanceImport),this::importPreset);
            button(items,getString(R.string.TetogramAppearanceExport),()->{AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name="Tetogram";p.style=AppearanceConfig.withoutPersonal(config.scopes.get(AppearanceConfig.GLOBAL));exportPreset(p);});
            button(items,getString(R.string.TetogramAppearanceNativeThemes),()->presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
            items.add(UItem.asHeader(getString(R.string.TetogramAppearanceBuiltins)));
            for(AppearanceConfig.Preset p:AppearancePresets.builtins())button(items,p.name,()->presetActions(p,true));
            items.add(UItem.asHeader(getString(R.string.TetogramAppearanceUserPresets)));
            for(AppearanceConfig.Preset p:config.presets.values())button(items,p.name,()->presetActions(p,false));
            button(items,getString(R.string.TetogramAppearanceReset),this::confirmReset);
            return;
        }
        if(category==Category.WALLPAPERS&&!rowMedia()&&AppearanceConfig.isMediaName(resolved().get(mediaKey("file"))))button(items,getString(R.string.TetogramWallpaperPosition),getString(R.string.TetogramWallpaperGestures),this::positionWallpaper);
        items.add(UItem.asShadow(editingPresetId==null?scopeLabel(scope)+" · "+getString(scope.equals(AppearanceConfig.GLOBAL)?R.string.TetogramEditorGlobalImpact:R.string.TetogramEditorLocalImpact):getString(R.string.TetogramAppearancePresetDraft)+"\n"+(config.presets.get(editingPresetId)==null?"":config.presets.get(editingPresetId).name)));
        if(!hidePreview){updatePreview();int windowDp=Math.round((fragmentView!=null&&fragmentView.getHeight()>0?fragmentView.getHeight():AndroidUtilities.displaySize.y)/AndroidUtilities.density);setTopContent(topPanel,Math.max(260,Math.min(520,Math.round(windowDp*0.5f))));}else setTopContent(null,0);
        LinearLayout switches=new LinearLayout(getContext());switches.setOrientation(LinearLayout.HORIZONTAL);
        for(int mode=0;mode<2;mode++){final boolean useAdvanced=mode==1;TextView tab=editorText(getString(useAdvanced?R.string.TetogramEditorModeAdvanced:R.string.TetogramEditorModeSimple));tab.setTextSize(14);tab.setAlpha(advanced==useAdvanced?1:.5f);tab.setOnClickListener(v->{advanced=useAdvanced;refresh();});switches.addView(tab,new LinearLayout.LayoutParams(0,-2,1));}items.add(UItem.asCustom(switches,-2));
        if(mediaBusy)items.add(UItem.asShadow(getString(R.string.TetogramAppearancePreparingMedia)));
        if(searchInput!=null&&advanced)items.add(UItem.asCustom(searchInput,48));filterParameters=advanced;
        elementPanel(items);
        if(category==Category.COLORS&&!advanced)themeCarousel(items);
        if(category==Category.ADVANCED){items.add(UItem.asHeader(getString(R.string.TetogramStudioTabMore)));button(items,getString(R.string.TetogramStudioPresets),()->presentFragment(new AppearanceActivity(currentAccount,"presets",AppearanceConfig.GLOBAL,null,null,0,0)));button(items,getString(R.string.TetogramAppearanceLauncher),()->presentFragment(new AppearanceLauncherActivity(currentAccount)));button(items,getString(R.string.TetogramAppearanceNativeThemes),()->presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
            items.add(UItem.asHeader(getString(R.string.TetogramAppearanceEffects)));toggle(items,"effects.enabled",getString(R.string.TetogramAppearanceAnimationsEnabled),true);number(items,"effects.durationScale",getString(R.string.TetogramAppearanceTransitionDuration),.25f,2);toggle(items,"effects.reduceOnBattery",getString(R.string.TetogramAppearanceReduceOnBattery),true);button(items,getString(R.string.TetogramAppearanceNativeEffects),()->presentFragment(new LiteModeSettingsActivity()));button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();for(String key:new ArrayList<>(draft.values.keySet()))if(key.startsWith("effects."))draft.values.remove(key);refresh();});return;}
        if(category==Category.WALLPAPERS){wallpaperTarget(items);mediaControls(items);return;}
        if(!advanced&&category!=Category.PROFILE&&category!=Category.SECTIONS){simpleSettings(items);return;}
        if(category==Category.PROFILE){AppearanceConfig.Resolved banner=resolved();for(String role:new String[]{"banner.text","banner.secondaryText"}){Integer current=banner.color(previewDark,role);button(items,getString(role.equals("banner.text")?R.string.TetogramAppearanceMainText:R.string.TetogramAppearanceHeaderSubtitle),()->colorDialog(role,current==null?AppearanceRuntime.baseColor(previewDark,role.equals("banner.text")?Theme.key_profile_title:Theme.key_profile_status):current));}number(items,"banner.height",getString(R.string.TetogramAppearanceBannerHeight),120,480);number(items,"banner.radius",getString(R.string.TetogramAppearanceRowRadius),0,40);number(items,"banner.parallax",getString(R.string.TetogramAppearanceBannerParallax),0,1);number(items,"banner.avatarAlignment",getString(R.string.TetogramAppearanceAvatarAlignment),-1,1);button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();draft=category==Category.WALLPAPERS?AppearanceEditorPolicy.resetWallpaper(draft,mediaPrefix()):AppearanceEditorPolicy.resetCategory(draft,category);refresh();});categoryLinks(items);return;}
        if(category==Category.COLORS){modeControl(items);if(scope.startsWith("profile:")){button(items,getString(R.string.TetogramEditorGlobalColors),()->openCategory(Category.COLORS));return;}}

        ArrayList<String> keys=new ArrayList<>();
        String cacheKey=category+"|"+scope+"|"+previewDialogs;
        if(advanced&&cacheKey.equals(advancedKeysFor))keys.addAll(advancedKeys);
        else if(advanced){for(int key=0;key<Theme.colorsCount;key++){String name=ThemeColors.getStringName(key);if(name!=null && !name.equals("wallpaperFileOffset") && !name.contains("rotation") && !name.toLowerCase(java.util.Locale.ROOT).contains("animated")&&(!(category==Category.LIST||category==Category.SECTIONS)||listColor(name))&&(scope.equals(AppearanceConfig.GLOBAL)||previewDialogs||name.startsWith("chat_")||name.startsWith("actionBar")||name.startsWith("windowBackground")||name.startsWith("dialog")))keys.add(category==Category.LIST||category==Category.SECTIONS?"list."+name:name);}keys.removeIf(key->AppearanceEditorPolicy.category(key)!=category);keys.sort((a,b)->{int c=Integer.compare(colorCategory(a),colorCategory(b));return c!=0?c:a.compareTo(b);});advancedKeys.clear();advancedKeys.addAll(keys);advancedKeysFor=cacheKey;}else{keys.clear();if(category==Category.SECTIONS&&folder!=null)Collections.addAll(keys,"list.actionBarTabActiveText","list.actionBarTabUnactiveText","list.actionBarTabLine");}

        String prefix=previewDark?"dark.":"light.";AppearanceConfig.Resolved r=resolved();int lastCategory=-1;
        final String query=search.toLowerCase(java.util.Locale.ROOT);
        for(String name:keys){if(!query.isEmpty() && !name.toLowerCase(java.util.Locale.ROOT).contains(query)&&!colorLabel(name).toString().toLowerCase(java.util.Locale.ROOT).contains(query))continue;
            String key=prefix+name;int nativeKey=ThemeColors.stringKeyToInt(name.startsWith("list.")?name.substring(5):name);Integer custom=r.color(previewDark,name);String sourceKey=key;if(custom==null&&name.startsWith("list.")){String legacy=name.substring(5);custom=r.color(previewDark,legacy);if(custom!=null)sourceKey=prefix+legacy;}int color=custom==null?AppearanceRuntime.baseColor(previewDark,nativeKey):custom;
            String origin=draft.values.containsKey(key)?getString(R.string.TetogramAppearanceExplicit):getString(R.string.TetogramAppearanceInherited)+" · "+scopeLabel(r.origins.getOrDefault(sourceKey,"Telegram"));
            int category=colorCategory(name);if(category!=lastCategory){items.add(UItem.asHeader(getString(category)));lastCategory=category;}
            button(items,colorLabel(name),String.format(java.util.Locale.ROOT,"#%08X",color)+" · "+origin+(advanced?" · "+name:""),()->colorDialog(name,color));
        }
        if(category==Category.CHATS&&(dialog<0||scope.equals(AppearanceConfig.GLOBAL)||scope.startsWith("section:")||scope.startsWith("folder:"))){Integer name=r.color(previewDark,"group.senderName");button(items,getString(R.string.TetogramAppearanceSenderNames),()->colorDialog("group.senderName",name==null?Theme.getColor(Theme.key_chat_inForwardedNameText):name));}
        if(category==Category.COLORS)button(items,getString(R.string.TetogramAppearanceRestoreContrast),this::restoreContrast);
        if(category==Category.LIST){
            listStyles(items);
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
        if(category==Category.SECTIONS&&(scope.equals(AppearanceConfig.GLOBAL)||scope.startsWith("section:"))){
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
        if(category!=Category.CHATS){categoryLinks(items);return;}
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

        button(items,getString(R.string.TetogramAppearanceReset),()->{checkpoint();draft=category==Category.WALLPAPERS?AppearanceEditorPolicy.resetWallpaper(draft,mediaPrefix()):AppearanceEditorPolicy.resetCategory(draft,category);refresh();});
    }
    private void simpleSettings(ArrayList<UItem> items){
        AppearanceConfig.Resolved r=resolved();
        if(category==Category.COLORS){modeControl(items);if(scope.startsWith("profile:")){button(items,getString(R.string.TetogramEditorGlobalColors),()->openCategory(Category.COLORS));return;}Integer accent=r.color(previewDark,"accent.primary");button(items,getString(R.string.TetogramAppearancePrimaryColor),()->colorDialog("accent.primary",accent==null?Theme.getColor(Theme.key_windowBackgroundWhiteBlueText):accent));}
        else if(category==Category.LIST){listStyles(items);number(items,"list.rowOpacity",getString(R.string.TetogramAppearanceRowOpacity),0,1);number(items,"list.navigationOpacity",getString(R.string.TetogramAppearanceNavigationOpacity),0,1);}
        else if(category==Category.CHATS){number(items,"bubble.inRadius",getString(R.string.TetogramAppearanceInRadius),0,32);number(items,"bubble.outRadius",getString(R.string.TetogramAppearanceOutRadius),0,32);for(String role:new String[]{"chat_inBubble","chat_outBubble","chat_messageTextIn","chat_messageTextOut"}){Integer value=r.color(previewDark,role);button(items,colorLabel(role),()->colorDialog(role,value==null?Theme.getColor(ThemeColors.stringKeyToInt(role)):value));}button(items,getString(R.string.TetogramAppearanceBubbleOpacity),this::bubbleOpacity);}
        categoryLinks(items);
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
        default:return AppearanceParameterNames.label(name,"ru".equals(LocaleController.getInstance().getCurrentLocale().getLanguage()));}
    }
    private void prepareScopeNames(){
        scopeNames.clear();scopeNames.put(AppearanceConfig.GLOBAL,getString(R.string.TetogramAppearanceGlobal));scopeNames.put("Telegram","Telegram");
        try{SectionConfig sections=SectionsStore.load(currentAccount);for(SectionConfig.Section s:sections.sections)scopeNames.put("section:"+s.id,s.name);for(SectionConfig.Folder f:sections.folders.values())scopeNames.put("folder:"+f.id,f.name);}catch(Exception ignored){}
        for(MessagesController.DialogFilter filter:getMessagesController().getDialogFilters())if(filter.id!=0)scopeNames.put("folder:tg:"+filter.id,String.valueOf(filter.name));
        if(dialog!=0){long userId=dialog;String name=null;if(DialogObject.isEncryptedDialog(dialog)){org.telegram.tgnet.TLRPC.EncryptedChat secret=getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialog));userId=secret==null?0:secret.user_id;}if(userId>0){org.telegram.tgnet.TLRPC.User user=getMessagesController().getUser(userId);if(user!=null)name=UserObject.getUserName(user);}else if(userId<0){org.telegram.tgnet.TLRPC.Chat chat=getMessagesController().getChat(-userId);if(chat!=null)name=chat.title;}if(name!=null){scopeNames.put("chat:"+dialog,name);if(topic!=0){org.telegram.tgnet.TLRPC.TL_forumTopic t=getMessagesController().getTopicsController().findTopic(-dialog,topic);scopeNames.put("topic:"+dialog+":"+topic,t==null?name:name+" / "+t.title);}}}
        if(scope.startsWith("profile:")){long target=Long.parseLong(scope.substring(8));String name=null;if(target>0){org.telegram.tgnet.TLRPC.User user=getMessagesController().getUser(target);if(user!=null)name=UserObject.getUserName(user);}else{org.telegram.tgnet.TLRPC.Chat chat=getMessagesController().getChat(-target);if(chat!=null)name=chat.title;}scopeNames.put(scope,getString(R.string.TetogramAppearanceProfile)+(name==null?"":" · "+name));}
    }
    private String scopeLabel(String key){String known=scopeNames.get(key);if(known!=null)return known;if(key.startsWith("section:"))return getString(R.string.TetogramAppearanceSections);if(key.startsWith("folder:"))return getString(R.string.TetogramAppearanceFolders);if(key.startsWith("chat:")||key.startsWith("topic:"))return getString(R.string.TetogramAppearanceChat);if(key.startsWith("profile:"))return getString(R.string.TetogramAppearanceProfile);return getString(R.string.TetogramAppearanceInherited);}
    private int surfaceLabel(int kind){switch(kind){case 1:return R.string.TetogramAppearanceDialogList;case 2:return R.string.TetogramAppearanceProfile;case 3:return R.string.TetogramAppearanceGroups;case 4:return R.string.TetogramAppearanceSections;case 5:return R.string.TetogramAppearanceFolders;default:return R.string.TetogramAppearanceChat;}}
    private void choosePreview(){ArrayList<Integer> kinds=new ArrayList<>();if(scope.startsWith("profile:"))kinds.add(2);else{Collections.addAll(kinds,0,3,1);if(scope.equals(AppearanceConfig.GLOBAL)||scope.startsWith("section:")||scope.startsWith("folder:"))Collections.addAll(kinds,4,5);if(scope.equals(AppearanceConfig.GLOBAL))kinds.add(2);}CharSequence[] names=new CharSequence[kinds.size()];for(int i=0;i<names.length;i++)names[i]=getString(surfaceLabel(kinds.get(i)));showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearancePreviewSurface)).setItems(names,(d,n)->{previewKind=kinds.get(n);previewDialogs=previewKind==1||previewKind==4||previewKind==5;refresh();}).create());}
    private int mediaSurface(){return category==Category.WALLPAPERS?wallpaperSurface:previewKind;}
    private boolean rowMedia(){return (mediaSurface()==1||mediaSurface()==4||mediaSurface()==5)&&(scope.startsWith("chat:")||scope.startsWith("topic:"));}
    private String mediaPrefix(){return mediaSurface()==2?"banner.wallpaper":mediaSurface()==1||mediaSurface()==4||mediaSurface()==5?"list.wallpaper":"wallpaper";}
    private String mediaKey(String part){return mediaPrefix()+"."+part;}
    private String wallpaperColorName(String tail){return (mediaSurface()==2?"banner.":mediaSurface()==1||mediaSurface()==4||mediaSurface()==5?"list.":"")+"chat_wallpaper"+tail;}
    private AppearanceConfig.Resolved previewStyle(){AppearanceConfig.Resolved r=resolved();return previewKind==2?AppearanceSurfaces.banner(r):previewDialogs?AppearanceSurfaces.list(r):r;}
    private float defaultNumber(String key){
        if(key.endsWith("OutlineWidth"))return 0;
        if(key.startsWith("bubble."))return resolved().number(key.startsWith("bubble.out")?"bubble.outRadius":"bubble.inRadius",SharedConfig.bubbleRadius);
        if(key.equals("list.avatarSize"))return SharedConfig.useThreeLinesLayout?56:52;
        try{return Float.parseFloat(AppearanceEditorPolicy.define(key).defaultValue);}catch(NumberFormatException ignored){return 0;}
    }
    private void number(ArrayList<UItem> items,String key,CharSequence label,float min,float max){
        AppearanceConfig.Resolved r=resolved();float current=r.number(key,defaultNumber(key));
        String origin=draft.values.containsKey(key)?getString(R.string.TetogramAppearanceExplicit):getString(R.string.TetogramAppearanceInherited)+" · "+scopeLabel(r.origins.getOrDefault(key,"Telegram"));
        button(items,label,AppearanceNumbers.format(key,current)+" · "+origin,()->numberDialog(key,label,min,max,current));
    }
    /** Slider with a live sample; exact typing stays available. Values are stored exactly as before (Float.toString). */
    private void numberDialog(String key,CharSequence label,float min,float max,float current){
        final float[] value={Math.max(min,Math.min(max,current))};
        LinearLayout body=new LinearLayout(getContext());body.setOrientation(LinearLayout.VERTICAL);
        FrameLayout sample=new FrameLayout(getContext());body.addView(sample,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(140)));final AppearancePreview[] scene={null};
        Runnable redraw=()->{if(!valid())return;AppearanceConfig.Resolved candidate=sampleStyle(style->style.set(key,Float.toString(value[0])));if(scene[0]==null){scene[0]=new AppearancePreview(getContext(),currentAccount,candidate,previewDark,previewKind,section);sample.addView(scene[0],new FrameLayout.LayoutParams(-1,-1));}else scene[0].update(candidate,previewDark);};
        TextView valueText=new TextView(getContext());valueText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,22);valueText.setTypeface(AndroidUtilities.bold());valueText.setGravity(Gravity.CENTER);valueText.setText(AppearanceNumbers.format(key,value[0]));ThemeBind.textColor(valueText,Theme.key_dialogTextBlack);
        LinearLayout.LayoutParams valueParams=new LinearLayout.LayoutParams(-1,-2);valueParams.topMargin=AndroidUtilities.dp(12);body.addView(valueText,valueParams);
        android.widget.SeekBar seek=new android.widget.SeekBar(getContext());seek.setMax(AppearanceNumbers.steps(key,min,max));seek.setProgress(AppearanceNumbers.progress(key,min,max,value[0]));
        LinearLayout.LayoutParams seekParams=new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(48));seekParams.leftMargin=seekParams.rightMargin=AndroidUtilities.dp(12);body.addView(seek,seekParams);
        LinearLayout range=new LinearLayout(getContext());range.setOrientation(LinearLayout.HORIZONTAL);
        for(float edge:new float[]{min,max}){TextView t=new TextView(getContext());t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,12);t.setText(AppearanceNumbers.format(key,edge));t.setGravity(edge==min?Gravity.LEFT:Gravity.RIGHT);ThemeBind.textColor(t,Theme.key_windowBackgroundWhiteGrayText);range.addView(t,new LinearLayout.LayoutParams(0,-2,1));}
        range.setPadding(AndroidUtilities.dp(24),0,AndroidUtilities.dp(24),0);body.addView(range);
        seek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}public void onProgressChanged(android.widget.SeekBar b,int progress,boolean user){if(!user)return;value[0]=AppearanceNumbers.value(key,min,max,progress);valueText.setText(AppearanceNumbers.format(key,value[0]));AndroidUtilities.cancelRunOnUIThread(redraw);AndroidUtilities.runOnUIThread(redraw,60);}});
        TextView exact=editorText(getString(R.string.TetogramEditorExactValue));exact.setTextSize(14);body.addView(exact,new LinearLayout.LayoutParams(-1,-2));
        android.widget.ScrollView scroll=new android.widget.ScrollView(getContext());scroll.addView(body);if(preview!=null)preview.setResumed(false);
        AlertDialog dialog=new AlertDialog.Builder(getContext()).setTitle(label).setView(scroll).setPositiveButton(getString(R.string.OK),(d,n)->{if(!valid())return;String stored=Float.toString(value[0]);if(stored.equals(draft.values.get(key)))return;checkpoint();draft.set(key,stored);refresh();}).setNeutralButton(getString(R.string.TetogramAppearanceInherit),(d,n)->{if(!valid()||!draft.values.containsKey(key))return;checkpoint();draft.values.remove(key);refresh();}).setNegativeButton(getString(R.string.Cancel),null).create();
        exact.setOnClickListener(v->{dialog.dismiss();input(label,AppearanceNumbers.trimmed(value[0]),text->{try{float parsed=Float.parseFloat(text.trim().replace(',','.'));if(Float.isNaN(parsed)||Float.isInfinite(parsed)||parsed<min||parsed>max)throw new IllegalArgumentException();checkpoint();draft.set(key,Float.toString(parsed));refresh();}catch(Exception e){error(getString(R.string.TetogramAppearanceInvalidValue));}});});
        redraw.run();showDialog(dialog,d->{AndroidUtilities.cancelRunOnUIThread(redraw);sample.removeAllViews();if(preview!=null)preview.setResumed(valid()&&foreground&&!mediaBusy&&!hidePreview);});
    }
    /** Preview style of the draft with a temporary edit; a video wallpaper is replaced by its cover so dialogs never start a second decoder. */
    private AppearanceConfig.Resolved sampleStyle(java.util.function.Consumer<AppearanceConfig.Style> edit){
        AppearanceConfig.Style old=draft;AppearanceConfig.Resolved candidate;
        try{draft=draft.copy();edit.accept(draft);candidate=previewStyle();}finally{draft=old;}
        if("video".equals(candidate.get("wallpaper.type"))){String cover=candidate.get("wallpaper.cover.file");if(AppearanceConfig.isMediaName(cover))candidate.values.put("wallpaper.file",cover);else candidate.values.remove("wallpaper.file");candidate.values.put("wallpaper.type","image");}
        return candidate;
    }

    private void colorDialog(String name,int color){pickerDialog(name,color);}
    /** Account-level color history: saved immediately, outside the draft, so it never makes the editor dirty. */
    private void rememberColor(String prefix,int color){if(config==null||!config.palette(prefix).isEmpty()&&config.palette(prefix).get(0)==color)return;saveConfig(fresh->{fresh.remember(prefix,color);return true;},null,false,()->{});}
    private void putColor(AppearanceConfig.Style target,String name,int color){
        if(name.contains("chat_wallpaper")){int fallback=AppearanceRuntime.baseColor(previewDark,Theme.key_chat_wallpaper);if(fallback==0)fallback=previewDark?0xff17212b:0xffe6ebee;AppearanceWallpaper.color(target,resolved(),previewDark,name,color,fallback);}
        target.color(previewDark,name,color);
        if(name.endsWith("accent.primary")){String domain=name.startsWith("list.")?"list.":"";AppearanceAccent.apply(target,false,color,domain);AppearanceAccent.apply(target,true,color,domain);return;}
        if(!advanced){String[] paired=name.equals("list.windowBackgroundWhite")?new String[]{"list.chats_name","list.chats_message"}:name.equals("list.actionBarDefault")?new String[]{"list.actionBarDefaultTitle","list.actionBarDefaultIcon"}:name.equals("chat_inBubble")?new String[]{"chat_messageTextIn"}:name.equals("chat_outBubble")?new String[]{"chat_messageTextOut"}:name.equals("actionBarDefault")?new String[]{"actionBarDefaultTitle","actionBarDefaultIcon"}:name.equals("windowBackgroundWhite")?new String[]{"windowBackgroundWhiteBlackText","chats_name"}:new String[0];for(String text:paired)target.color(previewDark,text,AppearancePresets.contrast(AppearanceColors.composite(color,previewDark?0xff000000:0xffffffff)));}
    }
    private void acceptColor(String name,int color){checkpoint();putColor(draft,name,color);refresh();rememberColor(AppearanceConfig.RECENT_COLORS,color);}
    private int contrastBase(boolean dark,String key){int nativeKey=ThemeColors.stringKeyToInt(key);return AppearanceRuntime.baseColor(dark,nativeKey<0?Theme.key_windowBackgroundWhite:nativeKey);}
    private void restoreContrast(){checkpoint();AppearanceConfig.Resolved r=resolved();AppearanceContrast.repair(draft,r,previewDark,key->contrastBase(previewDark,key));refresh();}
    private void applyDraft(){
        AppearanceConfig.Resolved r=resolved();ArrayList<String> issues=new ArrayList<>();for(boolean dark:new boolean[]{false,true})for(String key:AppearanceContrast.issues(draft,r,dark,name->contrastBase(dark,name)))issues.add(getString(dark?R.string.TetogramAppearanceDark:R.string.TetogramAppearanceLight)+": "+colorLabel(key));
        if(!issues.isEmpty()){showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceLowContrast)).setMessage(getString(R.string.TetogramAppearanceLowContrastInfo)+"\n\n"+String.join("\n",issues)).setPositiveButton(getString(R.string.TetogramAppearanceRestoreContrast),(d,n)->{checkpoint();for(boolean dark:new boolean[]{false,true})AppearanceContrast.repair(draft,r,dark,key->contrastBase(dark,key));refresh();applyDraft();}).setNegativeButton(getString(R.string.Edit),null).create());return;}
        final String presetId=editingPresetId;final AppearanceConfig.Style write=draft.copy(),base=baseline==null?null:baseline.copy();final boolean overwrite=overwriteScope;scopeConflict=false;
        Change change=fresh->{
            if(presetId!=null){AppearanceConfig.Preset target=fresh.presets.get(presetId);if(target==null)return false;if(AppearanceEditorPolicy.rebase(base,target.style,overwrite)==AppearanceEditorPolicy.Rebase.CONFLICT){scopeConflict=true;return false;}target.style=AppearanceConfig.withoutPersonal(write);return true;}
            if(AppearanceEditorPolicy.rebase(base,fresh.scopes.get(scope),overwrite)==AppearanceEditorPolicy.Rebase.CONFLICT){scopeConflict=true;return false;}
            fresh.putScope(scope,write);return true;
        };
        saveConfig(change,()->{overwriteScope=false;leaving=true;finishFragment();},presetId==null&&scope.equals(AppearanceConfig.GLOBAL),()->{
            if(scopeConflict)showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorChangedElsewhere)).setMessage(getString(R.string.TetogramEditorChangedElsewhereInfo)).setPositiveButton(getString(R.string.TetogramEditorOverwrite),(d,n)->{overwriteScope=true;applyDraft();}).setNegativeButton(getString(R.string.Cancel),null).create());
            else error(getString(presetId!=null&&config.presets.get(presetId)==null?R.string.TetogramEditorPresetMissing:R.string.TetogramAppearanceConflict));
        });
    }
    private void pickerDialog(String name,int color){
        final int[] value={color},alpha={Color.alpha(color)};final boolean[] changing={false};
        LinearLayout body=new LinearLayout(getContext());body.setOrientation(LinearLayout.VERTICAL);
        FrameLayout sample=new FrameLayout(getContext());body.addView(sample,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(120)));final AppearancePreview[] scene={null};
        Runnable redraw=()->{if(!valid())return;AppearanceConfig.Resolved candidate=sampleStyle(style->putColor(style,name,value[0]));if(scene[0]==null){scene[0]=new AppearancePreview(getContext(),currentAccount,candidate,previewDark,previewKind,section);sample.addView(scene[0],new FrameLayout.LayoutParams(-1,-1));}else scene[0].update(candidate,previewDark);};
        EditTextBoldCursor input=new EditTextBoldCursor(getContext());input.setSingleLine(true);input.setTextSize(16);input.setHint("HEX / RGB / HSV / HSL");input.setText(AppearanceColors.hex(color));input.setPadding(AndroidUtilities.dp(20),AndroidUtilities.dp(8),AndroidUtilities.dp(20),AndroidUtilities.dp(8));ThemeBind.textColor(input,Theme.key_dialogTextBlack);
        final android.widget.SeekBar transparency=new android.widget.SeekBar(getContext());transparency.setMax(255);transparency.setProgress(alpha[0]);
        ColorPicker picker=new ColorPicker(getContext(),false,(c,n,apply)->{if(changing[0])return;value[0]=(c&0xffffff)|(alpha[0]<<24);changing[0]=true;input.setText(AppearanceColors.hex(value[0]));input.setError(null);changing[0]=false;AndroidUtilities.cancelRunOnUIThread(redraw);AndroidUtilities.runOnUIThread(redraw,80);});picker.setResourcesProvider(getResourceProvider());picker.setType(0,true,1,1,false,0,false);picker.setColor(color,0);body.addView(picker,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(300)));body.addView(input,new LinearLayout.LayoutParams(-1,-2));
        TextView alphaTitle=editorText(getString(R.string.TetogramAppearanceOpacity));alphaTitle.setTextSize(13);body.addView(alphaTitle,new LinearLayout.LayoutParams(-1,-2));body.addView(transparency,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(40)));
        java.util.function.IntConsumer select=c->{value[0]=c;alpha[0]=Color.alpha(c);changing[0]=true;picker.setColor(c,0);input.setText(AppearanceColors.hex(c));input.setError(null);transparency.setProgress(alpha[0]);changing[0]=false;AndroidUtilities.cancelRunOnUIThread(redraw);AndroidUtilities.runOnUIThread(redraw,80);};
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable text){if(changing[0])return;try{int parsed=AppearanceColors.parse(text.toString());value[0]=parsed;alpha[0]=Color.alpha(parsed);changing[0]=true;picker.setColor(parsed,0);transparency.setProgress(alpha[0]);changing[0]=false;input.setError(null);AndroidUtilities.cancelRunOnUIThread(redraw);AndroidUtilities.runOnUIThread(redraw,80);}catch(IllegalArgumentException e){input.setError(getString(R.string.TetogramAppearanceColorFormat));}}});
        transparency.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(android.widget.SeekBar s){}public void onStopTrackingTouch(android.widget.SeekBar s){}public void onProgressChanged(android.widget.SeekBar bar,int progress,boolean user){if(user)select.accept((value[0]&0xffffff)|(progress<<24));}});
        for(String prefix:new String[]{AppearanceConfig.RECENT_COLORS,AppearanceConfig.FAVORITE_COLORS}){LinearLayout chips=new LinearLayout(getContext());chips.setOrientation(LinearLayout.HORIZONTAL);boolean any=false;for(int stored:config.palette(prefix)){try{int picked=stored;TextView chip=editorText((prefix.contains("favorite")?"★ ":"")+AppearanceColors.hex(picked));chip.setTextSize(12);chip.setTextColor(AppearancePresets.contrast(picked|0xff000000));chip.setBackgroundColor(picked|0xff000000);chip.setOnClickListener(v->select.accept(picked));chips.addView(chip,new LinearLayout.LayoutParams(-2,-2));any=true;}catch(NumberFormatException ignored){}}if(any){TextView label=editorText(getString(prefix.contains("favorite")?R.string.TetogramEditorFavorites:R.string.TetogramEditorRecent));label.setTextSize(13);body.addView(label);android.widget.HorizontalScrollView horizontal=new android.widget.HorizontalScrollView(getContext());horizontal.addView(chips);body.addView(horizontal,new LinearLayout.LayoutParams(-1,-2));}}
        LinearLayout tools=new LinearLayout(getContext());TextView pipette=editorText(getString(R.string.TetogramAppearanceEyedropper)),more=editorText("⋮");tools.addView(pipette,new LinearLayout.LayoutParams(0,-2,1));tools.addView(more,new LinearLayout.LayoutParams(-2,-2));body.addView(tools);
        android.widget.ScrollView scroll=new android.widget.ScrollView(getContext());scroll.addView(body);if(preview!=null)preview.setResumed(false);
        AlertDialog dialog=new AlertDialog.Builder(getContext()).setTitle(colorLabel(name)).setView(scroll,Math.max(AndroidUtilities.dp(240),Math.min(AndroidUtilities.dp(580),AndroidUtilities.displaySize.y-mSystemInsets.top-mSystemInsets.bottom-AndroidUtilities.dp(160)))).setPositiveButton(getString(R.string.OK),(d,n)->{if(input.getError()!=null){error(getString(R.string.TetogramAppearanceColorFormat));return;}acceptColor(name,value[0]);}).setNeutralButton(getString(R.string.TetogramAppearanceInherit),(d,n)->{checkpoint();draft.values.remove((previewDark?"dark.":"light.")+name);refresh();}).setNegativeButton(getString(R.string.Cancel),null).create();
        pipette.setOnClickListener(v->{dialog.dismiss();pickSample(name);});more.setContentDescription(getString(R.string.TetogramEditorPaletteActions));more.setOnClickListener(v->ItemOptions.makeOptions(body,getResourceProvider(),more).add(0,getString(R.string.Copy),()->AndroidUtilities.addToClipboard(AppearanceColors.hex(value[0]))).add(0,getString(R.string.TetogramAppearanceFavoriteColor),()->rememberColor(AppearanceConfig.FAVORITE_COLORS,value[0])).show());
        redraw.run();showDialog(dialog,d->{AndroidUtilities.cancelRunOnUIThread(redraw);sample.removeAllViews();if(preview!=null)preview.setResumed(valid()&&foreground&&!mediaBusy&&!hidePreview);});
    }
    private void presetActions(AppearanceConfig.Preset p,boolean builtin){ArrayList<CharSequence> names=new ArrayList<>();ArrayList<Runnable> runs=new ArrayList<>();names.add(getString(R.string.TetogramAppearanceApply));runs.add(()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,"edit",AppearanceConfig.GLOBAL,null,null,0,0);editor.config=config.copy();editor.expectedRevision=expectedRevision;editor.draft=AppearanceConfig.applyPreset(config.scopes.get(AppearanceConfig.GLOBAL),p.style);editor.previewDark="dark".equals(p.style.values.get("mode"));presentFragment(editor);});names.add(getString(R.string.TetogramAppearanceDuplicate));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),p.name,name->{AppearanceConfig.Preset duplicate=p.duplicate(name);duplicate.style=AppearanceConfig.withoutPersonal(duplicate.style);saveConfig(fresh->{fresh.presets.put(duplicate.id,duplicate);return true;},null,false);}));names.add(getString(R.string.TetogramAppearanceExport));runs.add(()->exportPreset(p));if(!builtin){names.add(getString(R.string.Edit));runs.add(()->{AppearanceActivity editor=new AppearanceActivity(currentAccount,"edit",AppearanceConfig.GLOBAL,null,null,0,0);editor.config=config.copy();editor.expectedRevision=expectedRevision;editor.draft=AppearanceConfig.withoutPersonal(p.style);editor.baseline=editor.draft.copy();editor.editingPresetId=p.id;editor.previewDark="dark".equals(p.style.values.get("mode"));presentFragment(editor);});names.add(getString(R.string.EditName));runs.add(()->input(getString(R.string.TetogramAppearancePresetName),p.name,name->saveConfig(fresh->{AppearanceConfig.Preset stored=fresh.presets.get(p.id);if(stored==null)return false;stored.name=name;return true;},null,false)));names.add(getString(R.string.Delete));runs.add(()->showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceDeletePreset)).setPositiveButton(getString(R.string.Delete),(d,n)->saveConfig(fresh->fresh.presets.remove(p.id)!=null,null,false)).setNegativeButton(getString(R.string.Cancel),null).create()));}showDialog(new AlertDialog.Builder(getContext()).setTitle(p.name).setItems(names.toArray(new CharSequence[0]),(d,n)->runs.get(n).run()).create());}
    private void confirmReset(){showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceResetConfirm)).setPositiveButton(getString(R.string.OK),(d,n)->saveConfig(fresh->{fresh.resetAppearance();return true;},null,true)).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void saveConfig(Change change,Runnable done,boolean applyMode){saveConfig(change,done,applyMode,null);}
    /**
     * Saves a mutation of the stored document. The in-memory copy is never changed before the write succeeds.
     * When another screen saved meanwhile, the newest document is loaded and the same mutation is applied to it once.
     * Saves requested while one is running are queued, so color history never races with Apply.
     */
    private void saveConfig(Change change,Runnable done,boolean applyMode,Runnable failed){
        if(!valid()||config==null)return;
        if(saving){queuedSaves.add(new PendingSave(change,done,applyMode,failed));return;}
        AppearanceConfig next=config.copy();
        if(!change.apply(next)){if(failed!=null)failed.run();else error(getString(R.string.TetogramAppearanceConflict));return;}
        saving=true;refresh();
        AppearanceStore.save(currentAccount,owner,next,expectedRevision,applyMode,(ok,c)->{
            if(!valid())return;
            if(ok){saved(c,done);return;}
            AppearanceStore.load(currentAccount,(loaded,fresh)->{
                if(!valid())return;
                AppearanceConfig retry=loaded?fresh.copy():null;
                if(retry==null||!change.apply(retry)){saving=false;if(loaded){config=fresh;expectedRevision=fresh.revision;}refresh();if(failed!=null)failed.run();else error(getString(R.string.TetogramAppearanceConflict));flushSaves();return;}
                AppearanceStore.save(currentAccount,owner,retry,fresh.revision,applyMode,(ok2,c2)->{if(!valid())return;if(ok2)saved(c2,done);else{saving=false;refresh();if(failed!=null)failed.run();else error(getString(R.string.TetogramAppearanceConflict));flushSaves();}});
            });
        });
    }
    private void saved(AppearanceConfig c,Runnable done){saving=false;config=c;expectedRevision=c.revision;if(done!=null)done.run();refresh();flushSaves();}
    private void flushSaves(){if(saving||!valid())return;PendingSave next=queuedSaves.poll();if(next!=null)saveConfig(next.change,next.done,next.applyMode,next.failed);}
    private void input(CharSequence title,String initial,java.util.function.Consumer<String> done){EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(18);field.setText(initial);field.setSingleLine(true);field.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(8),AndroidUtilities.dp(24),AndroidUtilities.dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);showDialog(new AlertDialog.Builder(getContext()).setTitle(title).setView(field).setPositiveButton(getString(R.string.OK),(d,n)->{String value=field.getText().toString().trim();if(value.length()<=256&&!value.isEmpty())done.accept(value);else error(getString(R.string.TetogramAppearanceInvalidValue));}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void error(CharSequence text){if(getContext()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(text).setPositiveButton(getString(R.string.OK),null).create());}
    private String wallpaperSource(){AppearanceConfig.Resolved r=resolved();String key=mediaKey("file");if(r.get(key)==null||r.get(key).isEmpty())key=(previewDark?"dark.":"light.")+wallpaperColorName("");return getString(draft.values.containsKey(key)?R.string.TetogramAppearanceExplicit:R.string.TetogramAppearanceInherited)+" · "+scopeLabel(r.origins.getOrDefault(key,"Telegram"));}
    private void positionWallpaper(){
        if(!valid()||saving||mediaBusy||draft==null)return;
        final String prefix=mediaPrefix();final AppearanceConfig.Style openedDraft=draft;
        AppearanceConfig.Resolved source=resolved();
        if("list.wallpaper".equals(prefix))source=AppearanceSurfaces.list(source);
        else if("banner.wallpaper".equals(prefix))source=AppearanceSurfaces.banner(source);
        if(!AppearanceConfig.isMediaName(source.get("wallpaper.file")))return;
        presentFragment(new AppearanceWallpaperActivity(currentAccount,source,previewDark,"banner.wallpaper".equals(prefix),(zoom,panX,panY)->{
            if(!valid()||draft!=openedDraft||saving||mediaBusy)return;
            checkpoint();draft.set(prefix+".zoom",Float.toString(zoom));draft.set(prefix+".panX",Float.toString(panX));draft.set(prefix+".panY",Float.toString(panY));refresh();
        }));
    }
    private void mediaControls(ArrayList<UItem> items){
        items.add(UItem.asHeader(getString(R.string.TetogramAppearanceWallpapers)));
        button(items,getString(R.string.TetogramAppearanceChooseImage),()->pickMedia(false));
        if(!rowMedia())button(items,getString(R.string.TetogramAppearanceChooseVideo),()->pickMedia(true));
        else items.add(UItem.asShadow(getString(R.string.TetogramAppearanceRowImageOnly)));
        AppearanceConfig.Resolved r=resolved();
        if(AppearanceConfig.isMediaName(r.get(mediaKey("file"))))button(items,getString(R.string.TetogramAppearanceEditMedia),()->{mediaBusy=true;if(preview!=null)preview.setResumed(false);refresh();mediaJob=AppearanceMedia.inspect(currentAccount,owner,r.get(mediaKey("source.file")),r.get(mediaKey("file")),"video".equals(r.get(mediaKey("type"))),this::editSelectedMedia);});
        button(items,getString(R.string.TetogramAppearanceWallpaperBase),wallpaperSource(),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceWallpaperBase)).setItems(new CharSequence[]{getString(R.string.TetogramAppearanceInherit),getString(R.string.TetogramAppearanceGlobalWallpaper),getString(R.string.TetogramAppearanceTelegramWallpaper)},(d,n)->{checkpoint();for(String key:new ArrayList<>(draft.values.keySet()))if(AppearanceConfig.isSurfaceWallpaperKey(key,mediaPrefix()))draft.values.remove(key);if(n!=0)draft.set(mediaKey("base"),n==1?"global":"telegram");refresh();}).create()));
        button(items,getString(R.string.TetogramAppearanceSolidWallpaper),()->colorDialog(wallpaperColorName(""),Theme.getColor(Theme.key_chat_wallpaper)));
        for(int stop=1;stop<=3;stop++){final String name=wallpaperColorName("_gradient_to"+stop);Integer current=r.color(previewDark,name);button(items,getString(R.string.TetogramAppearanceGradientStop)+" "+(stop+1),()->colorDialog(name,current==null?Theme.getColor(Theme.key_chat_wallpaper):current));}
        if(!rowMedia()){
            button(items,getString(R.string.TetogramAppearanceDefaultPattern),getString("true".equals(r.get(mediaKey("pattern")))?R.string.TetogramAppearancePatternEnabled:R.string.TetogramAppearancePatternDisabled),()->{checkpoint();boolean on=!"true".equals(r.get(mediaKey("pattern")));draft.set(mediaKey("pattern"),Boolean.toString(on));if(on&&r.color(previewDark,wallpaperColorName(""))==null){int base=AppearanceRuntime.baseColor(previewDark,Theme.key_chat_wallpaper);draft.color(previewDark,wallpaperColorName(""),base==0?(previewDark?0xff17212b:0xffe6ebee):base);}refresh();});
            if("true".equals(r.get(mediaKey("pattern"))))number(items,mediaKey("patternIntensity"),getString(R.string.TetogramAppearancePatternIntensity),0,100);
        }
        number(items,mediaKey("angle"),getString(R.string.TetogramAppearanceGradientAngle),0,360);
        toggle(items,mediaKey("gradientMotion"),getString(R.string.TetogramAppearanceGradientMotion),false);
        number(items,mediaKey("opacity"),getString(R.string.TetogramAppearanceOpacity),0,1);
        number(items,mediaKey("dim"),getString(R.string.TetogramAppearanceDimming),0,1);
        number(items,mediaKey("blur"),getString(Build.VERSION.SDK_INT>=31&&!rowMedia()?R.string.TetogramAppearanceBlur:R.string.TetogramAppearanceStaticBlur),0,Build.VERSION.SDK_INT>=31&&!rowMedia()?32:1);
        if(!rowMedia()&&AppearanceConfig.isMediaName(r.get(mediaKey("file")))){
            if(getContext().getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_ACCELEROMETER))toggle(items,mediaKey("parallax"),getString(R.string.TetogramAppearanceWallpaperParallax),false);
            if(advanced){items.add(UItem.asHeader(getString(R.string.TetogramWallpaperPrecise)));
                number(items,mediaKey("zoom"),getString(R.string.TetogramAppearanceZoom),1,4);
                number(items,mediaKey("panX"),getString(R.string.TetogramAppearancePanX),-1,1);
                number(items,mediaKey("panY"),getString(R.string.TetogramAppearancePanY),-1,1);
            }
            button(items,getString(R.string.TetogramAppearanceFit),getString("fit".equals(r.get(mediaKey("fit")))?R.string.TetogramEditorFit:R.string.TetogramEditorFill),()->{checkpoint();draft.set(mediaKey("fit"),"fit".equals(r.get(mediaKey("fit")))?"fill":"fit");refresh();});
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
        AppearanceConfig.Resolved resolved=resolved();String value=resolved.get(key);boolean checked=value==null?fallback:Boolean.parseBoolean(value);boolean explicit=draft.values.containsKey(key);
        String state=getString(checked?R.string.TetogramEditorEnabled:R.string.TetogramEditorDisabled)+" · "+getString(explicit?R.string.TetogramAppearanceExplicit:R.string.TetogramAppearanceInherited)+(!explicit?" · "+scopeLabel(resolved.origins.getOrDefault(key,"Telegram")):" · "+getString(R.string.TetogramEditorHoldToInherit));
        AppearanceSettingRow row=new AppearanceSettingRow(getContext(),label,state,!saving&&!mediaBusy,checked,()->{if(valid()&&!saving&&!mediaBusy){checkpoint();draft.set(key,Boolean.toString(!checked));refresh();}});
        if(explicit)row.setOnLongClickListener(v->{checkpoint();draft.values.remove(key);refresh();return true;});items.add(UItem.asCustom(row,-2));
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
        else if(request==IMPORT_PRESET){mediaJob=AppearanceMedia.importPreset(currentAccount,owner,data.getData(),imported->{mediaBusy=false;if(!valid()){if(imported!=null)for(File f:imported.files)AppearanceMedia.discard(f);return;}if(imported==null)error(getString(R.string.TetogramAppearanceArchiveError));else{AppearanceActivity editor=new AppearanceActivity(currentAccount,"edit",AppearanceConfig.GLOBAL,null,null,0,0);editor.config=config.copy();editor.expectedRevision=expectedRevision;editor.draft=AppearanceConfig.applyPreset(config.scopes.get(AppearanceConfig.GLOBAL),imported.preset.style);editor.previewDark="dark".equals(editor.draft.values.get("mode"));editor.pendingMedia.addAll(imported.files);if(!presentFragment(editor))for(File f:imported.files)AppearanceMedia.discard(f);}refresh();});}
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
            @Override public void sendButtonPressed(int index,VideoEditedInfo info,boolean notify,int schedule,int repeat,boolean forceDocument){if(!valid()||applied[0])return;applied[0]=true;mediaBusy=true;refresh();AppearanceConfig.Resolved current=resolved();mediaJob=AppearanceMedia.prepare(currentAccount,owner,source,entry.imagePath,info,Math.round(current.number(mediaKey("fps"),30)),Math.round(current.number(mediaKey("quality"),1920)),entry.coverPath,asset->{mediaBusy=false;if(!valid()){if(asset!=null){AppearanceMedia.discard(asset.file);AppearanceMedia.discard(asset.cover);}return;}if(asset==null){error(getString(R.string.TetogramAppearanceMediaError));refresh();return;}checkpoint();pendingMedia.add(asset.file);if(asset.cover!=null)pendingMedia.add(asset.cover);draft.set(mediaKey("file"),asset.file.getName());draft.set(mediaKey("type"),asset.video?"video":"image");if(previewDialogs&&!draft.values.containsKey("list.rowOpacity"))draft.set("list.rowOpacity","0.85");draft.set(mediaKey("source.file"),source.file.getName());if(asset.cover!=null)draft.set(mediaKey("cover.file"),asset.cover.getName());else draft.set(mediaKey("cover.file"),"");refresh();});}
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
