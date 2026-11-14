/* Tetogram, 2026. GNU GPL v2 or later. Native Labs settings, local account configuration. */
package org.telegram.ui.tetogram;
import static org.telegram.messenger.LocaleController.getString;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.provider.MediaStore;
import android.os.Build;
import java.io.File;
import android.view.View;
import java.util.*;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.*;
public final class SectionsActivity extends LabsPage {
 private final String sectionId,folderId;
 private final long owner;
 private SectionConfig config;
 private static final int PICK_ICON=61031;
 private final SectionIconPolicy.Edit iconEdit=new SectionIconPolicy.Edit();
 private String draftType,draftValue,draftImage;
 private File pendingImage,inputImage;
 private ImageUpdater iconUpdater;
 private boolean savingIcon,destroyed;
 private void resetDraft(){if(savingIcon)return;iconEdit.cancel();draftType=draftValue=draftImage=null;SectionIcons.pending(pendingImage,false);SectionIcons.discard(pendingImage);pendingImage=null;}

 private final ArrayList<Runnable> actions=new ArrayList<>();
 public SectionsActivity(int account){this(account,null,null);}
 public SectionsActivity(int account,String section){this(account,section,null);}
 private SectionsActivity(int account,String section,String folder){super(account);sectionId=section;folderId=folder;owner=UserConfig.getInstance(account).getClientUserId();}
 @Override protected CharSequence title(){return getString(folderId!=null?R.string.TetogramSectionFolderName:R.string.TetogramSections);}
 private boolean valid(){if(destroyed)return false;if(owner==0 || owner!=getUserConfig().getClientUserId()){finishFragment();return false;}return true;}
 private boolean persist(){if(!valid())return false;if(SectionsStore.trySave(currentAccount,owner,config))return true;showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramStorageError)).setPositiveButton(getString(R.string.OK),null).create());return false;}
 private void save(){if(persist())refresh();}
 private void refresh(){if(list!=null)list.adapter.update(true);}
 private void button(ArrayList<UItem> items,CharSequence text,CharSequence value,Runnable run){actions.add(run);items.add(UItem.asButton(actions.size(),text,value));}
 private void button(ArrayList<UItem> items,CharSequence text,Runnable run){button(items,text,null,run);}
 private void check(ArrayList<UItem> items,CharSequence text,boolean checked,Runnable run){actions.add(run);items.add(UItem.asCheck(actions.size(),text).setChecked(checked));}
 @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
  actions.clear();if(!valid())return;
  try{config=SectionsStore.load(currentAccount);}catch(Exception e){items.add(UItem.asShadow(getString(R.string.TetogramStorageError)));return;}
  if(sectionId==null && folderId==null){
   button(items,getString(R.string.TetogramSectionAdd),()->input(getString(R.string.TetogramSectionName),"",name->{config.add(name);save();}));
   items.add(UItem.asShadow(getString(R.string.TetogramSectionInfo)));
   check(items,getString(R.string.TetogramShowBadges),config.showBadges,()->{config.showBadges=!config.showBadges;save();});
   check(items,getString(R.string.TetogramCompactBadges),config.compactBadges,()->{config.compactBadges=!config.compactBadges;save();});
   items.add(UItem.asCheck(getString(R.string.TetogramHideMutedBadges)).setChecked(true).setEnabled(false));
   items.add(UItem.asShadow(getString(R.string.TetogramMutedBadgesFixed)));
   check(items,getString(R.string.TetogramTemporaryUnread),config.unreadEnabled,()->{config.unreadEnabled=!config.unreadEnabled;save();});
   check(items,getString(R.string.TetogramUnreadIncludeMuted),config.unreadIncludeMuted,()->{config.unreadIncludeMuted=!config.unreadIncludeMuted;save();});
   for(SectionConfig.Section s:config.sections)button(items,("emoji".equals(s.iconType)?s.icon+"  ":"")+s.name,s.id.equals(config.defaultId)?getString(R.string.TetogramSectionDefault):null,()->presentFragment(new SectionsActivity(currentAccount,s.id)));
   items.add(UItem.asHeader(getString(R.string.TetogramSectionLocal)));
   for(SectionConfig.Folder f:config.folders.values())button(items,f.name,()->presentFragment(new SectionsActivity(currentAccount,null,f.id)));
   return;
  }
  SectionConfig.Section s=config.section(sectionId);SectionConfig.Folder f=config.folders.get(folderId);
  if(s==null && f==null){items.add(UItem.asShadow(getString(R.string.TetogramSectionUnavailable)));return;}
  SectionConfig.Scope scope=s!=null?s:f;
  if(s!=null)items.add(UItem.asHeader(getString(R.string.TetogramSectionAppearance)));
  button(items,s!=null?s.name:f.name,()->input(getString(s!=null?R.string.TetogramSectionName:R.string.TetogramSectionFolderName),s!=null?s.name:f.name,name->{if(s!=null)s.name=name;else f.name=name;save();}));
  if(s!=null){
   android.widget.FrameLayout preview=new android.widget.FrameLayout(getContext());
   SectionCircle circle=new SectionCircle(getContext(),getResourceProvider());SectionConfig.Section displayed=new SectionConfig.Section();displayed.id=s.id;displayed.name=s.name;displayed.iconType=draftType==null?s.iconType:draftType;displayed.icon=draftType==null?s.icon:draftValue;displayed.iconImage=draftType==null?s.iconImage:draftImage;
   circle.bind(owner,displayed,true);preview.addView(circle,LayoutHelper.createFrame(56,56,android.view.Gravity.CENTER));items.add(UItem.asCustom(preview,80));
   button(items,getString(R.string.TetogramChangeIcon),()->chooseIcon(s));
   if(draftType!=null){items.add(UItem.asShadow(getString(R.string.TetogramIconPending)));button(items,getString(R.string.TetogramSaveIcon),this::commitIcon);button(items,getString(R.string.Cancel),()->{resetDraft();refresh();});}
   items.add(UItem.asShadow(getString(R.string.TetogramIconPhotoFormats)));
   check(items,getString(R.string.TetogramSectionDefault),s.id.equals(config.defaultId),()->{config.defaultId=s.id.equals(config.defaultId)?"":s.id;save();});
   button(items,getString(R.string.TetogramSectionUp),()->{SectionConfig.move(config.sections,s,-1);save();});
   button(items,getString(R.string.TetogramSectionDown),()->{SectionConfig.move(config.sections,s,1);save();});
  }
  items.add(UItem.asHeader(getString(R.string.TetogramSectionChats)));
  button(items,getString(R.string.TetogramSectionAddChats),()->pick(scope,false));
  button(items,getString(R.string.TetogramSectionExclude),()->pick(scope,true));
  for(long id:scope.chats)button(items,chatName(id),()->confirm(getString(R.string.TetogramSectionRemoveChat),()->{scope.exclude(id);save();}));
  if(!scope.excluded.isEmpty())items.add(UItem.asHeader(getString(R.string.TetogramSectionExcluded)));
  for(long id:scope.excluded)button(items,chatName(id),getString(R.string.TetogramSectionRestoreChat),()->{scope.excluded.remove(id);save();});
  items.add(UItem.asHeader(getString(R.string.TetogramSectionRules)));
  int[] labels={R.string.TetogramSectionContacts,R.string.TetogramSectionOther,R.string.TetogramSectionGroups,R.string.TetogramSectionChannels,R.string.TetogramSectionBots,R.string.TetogramSectionMuted,R.string.TetogramSectionRead,R.string.TetogramSectionArchived};
  for(int i=0;i<labels.length;i++){final int flag=1<<i;check(items,getString(labels[i]),(scope.flags&flag)!=0,()->{scope.flags^=flag;save();});}
  items.add(UItem.asShadow(getString(R.string.TetogramSectionRulesInfo)));
  if(s!=null){
   items.add(UItem.asHeader(getString(R.string.TetogramSectionFolders)));
   check(items,getString(R.string.TetogramSectionGeneral),s.general,()->{s.general=!s.general;save();});
   button(items,getString(R.string.TetogramSectionAttach),()->attach(s));
   button(items,getString(R.string.TetogramSectionLocalAdd),()->input(getString(R.string.TetogramSectionFolderName),"",name->{SectionConfig.Folder added=config.addFolder(name);s.tabs.add(added.id);save();presentFragment(new SectionsActivity(currentAccount,null,added.id));}));
   for(String key:s.tabs)button(items,folderName(key),key.startsWith("tg:")?getString(R.string.TetogramSectionFolderServerLabel):getString(R.string.TetogramSectionFolderLocalLabel),()->folderActions(s,key));
   button(items,getString(R.string.Delete),()->confirm(getString(R.string.TetogramSectionDeleteConfirm),()->{config.remove(s.id);if(persist())finishFragment();}));
  }else button(items,getString(R.string.Delete),()->confirm(getString(R.string.TetogramSectionFolderDeleteConfirm),()->{config.removeFolder(f.id);if(persist())finishFragment();}));
 }
 private void chooseIcon(SectionConfig.Section section){
  if(savingIcon)return;
  CharSequence[] options={getString(R.string.TetogramChooseEmoji),getString(R.string.TetogramStandardIcons),getString(R.string.TetogramChooseImage),getString(R.string.TetogramResetIcon)};
  showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramChangeIcon)).setItems(options,(d,n)->{
   if(!valid())return;
   if(n==0)input(getString(R.string.TetogramChooseEmoji),"emoji".equals(section.iconType)?section.icon:"",value->{resetDraft();draftType="emoji";draftValue=value;draftImage="";refresh();});
   else if(n==1){CharSequence[] symbols={getString(R.string.TetogramIconFolder),getString(R.string.TetogramIconStar),getString(R.string.TetogramIconWork),getString(R.string.TetogramIconGroup),getString(R.string.TetogramIconChat),getString(R.string.TetogramIconArchive)};int[] icons=new int[SectionCircle.STANDARD.length];for(int i=0;i<icons.length;i++)icons[i]=SectionCircle.drawable(SectionCircle.STANDARD[i]);showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramStandardIcons)).setItems(symbols,icons,(dialog,index)->{resetDraft();draftType="standard";draftValue=SectionCircle.STANDARD[index];draftImage="";refresh();}).create());}
   else if(n==2)pickIcon();
   else {resetDraft();draftType="standard";draftValue="folder";draftImage="";refresh();}
  }).create());
 }
 private void iconError(){if(getParentActivity()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramIconError)).setPositiveButton(getString(R.string.OK),null).create());}
 private void pickIcon(){
  if(!valid() || savingIcon)return;iconEdit.begin();
  try{Intent picker=new Intent(Build.VERSION.SDK_INT>=33?MediaStore.ACTION_PICK_IMAGES:Intent.ACTION_OPEN_DOCUMENT);picker.setType("image/*");if(Build.VERSION.SDK_INT<33)picker.addCategory(Intent.CATEGORY_OPENABLE);if(picker.resolveActivity(getParentActivity().getPackageManager())==null){picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);}startActivityForResult(picker,PICK_ICON);}catch(Exception e){iconError();}
 }
 @Override public void onActivityResultFragment(int request,int result,Intent data){
  super.onActivityResultFragment(request,result,data);if(request!=PICK_ICON || result!=Activity.RESULT_OK || data==null || data.getData()==null || !valid())return;
  long token=iconEdit.begin();android.net.Uri uri=data.getData();
  SectionIcons.queue.postRunnable(()->{
   File file=null;try{file=SectionIcons.input(owner,sectionId,uri);}catch(Exception e){}final File source=file;
   AndroidUtilities.runOnUIThread(()->{if(!iconEdit.current(token) || !valid()){SectionIcons.pending(source,false);SectionIcons.discard(source);return;}if(source==null){iconError();return;}
    SectionIcons.pending(inputImage,false);SectionIcons.discard(inputImage);inputImage=source;
    if(iconUpdater!=null){iconUpdater.setDelegate(null);iconUpdater.parentFragment=null;iconUpdater.clear();}
    iconUpdater=new ImageUpdater(currentAccount,false,ImageUpdater.FOR_TYPE_USER,false);iconUpdater.parentFragment=this;iconUpdater.setUploadAfterSelect(false);
    iconUpdater.setDelegate((photo,video,stamp,path,big,small,isVideo,markup)->{
     if(big==null || !iconEdit.current(token) || !valid())return;
     File crop=FileLoader.getInstance(currentAccount).getPathToAttach(big,true);
     SectionIcons.queue.postRunnable(()->{File optimized=null;try{optimized=SectionIcons.optimize(owner,sectionId,crop);}catch(Exception e){}final File ready=optimized;
      SectionIcons.pending(source,false);SectionIcons.discard(source);
      AndroidUtilities.runOnUIThread(()->{if(!iconEdit.current(token) || !valid()){SectionIcons.pending(ready,false);SectionIcons.discard(ready);return;}if(ready==null){iconError();return;}SectionIcons.pending(pendingImage,false);SectionIcons.discard(pendingImage);pendingImage=ready;draftType="image";draftValue="folder";draftImage=ready.getName();refresh();});
     });
    });
    try{iconUpdater.openPhotoForEdit(source.getAbsolutePath(),null,AndroidUtilities.getImageOrientation(source.getAbsolutePath()),false);}catch(Exception e){iconError();}
   });
  });
 }
 private void commitIcon(){
  if(!valid() || savingIcon || draftType==null)return;
  String type=draftType,value=draftValue,image=draftImage;File file=pendingImage;long token=iconEdit.begin();savingIcon=true;
  SectionIcons.queue.postRunnable(()->{
   boolean ok=false;
   try{if(owner==UserConfig.getInstance(currentAccount).getClientUserId()){
    ok=SectionsStore.updateIcon(currentAccount,owner,sectionId,type,value,image);
   }}catch(Exception e){}
   if(ok)SectionIcons.pending(file,false);
   final boolean saved=ok;AndroidUtilities.runOnUIThread(()->{savingIcon=false;if(!valid()){SectionIcons.pending(file,false);if(!saved)SectionIcons.discard(file);return;}if(saved){SectionIcons.pending(file,false);if(pendingImage==file)pendingImage=null;draftType=draftValue=draftImage=null;refresh();}else iconError();});
  });
 }
 @Override public void onFragmentDestroy(){destroyed=true;iconEdit.cancel();SectionIcons.pending(inputImage,false);SectionIcons.discard(inputImage);if(!savingIcon){SectionIcons.pending(pendingImage,false);SectionIcons.discard(pendingImage);}if(iconUpdater!=null){iconUpdater.setDelegate(null);iconUpdater.parentFragment=null;iconUpdater.clear();iconUpdater=null;}super.onFragmentDestroy();}
 private String folderName(String key){MessagesController.DialogFilter tg=SectionDialogs.server(currentAccount,key);SectionConfig.Folder local=config.folders.get(key);return tg!=null?tg.name:local!=null?local.name:getString(R.string.TetogramSectionUnavailable)+" ("+key+")";}
 private String chatName(long id){MessagesController m=getMessagesController();if(DialogObject.isEncryptedDialog(id)){TLRPC.EncryptedChat e=m.getEncryptedChat(DialogObject.getEncryptedChatId(id));if(e!=null)id=e.user_id;}if(id>0){TLRPC.User u=m.getUser(id);if(u!=null)return UserObject.getUserName(u);}else{TLRPC.Chat c=m.getChat(-id);if(c!=null)return c.title;}return Long.toString(id);}
 private void folderActions(SectionConfig.Section s,String key){
  ArrayList<CharSequence> names=new ArrayList<>();ArrayList<Runnable> runs=new ArrayList<>();
  if(config.folders.containsKey(key)){names.add(getString(R.string.Edit));runs.add(()->presentFragment(new SectionsActivity(currentAccount,null,key)));}
  names.add(getString(R.string.TetogramSectionUp));runs.add(()->{SectionConfig.move(s.tabs,key,-1);save();});
  names.add(getString(R.string.TetogramSectionDown));runs.add(()->{SectionConfig.move(s.tabs,key,1);save();});
  names.add(getString(R.string.TetogramSectionDetach));runs.add(()->{s.tabs.remove(key);if(key.equals(s.lastFolder))s.lastFolder="";save();});
  showDialog(new AlertDialog.Builder(getContext()).setTitle(folderName(key)).setItems(names.toArray(new CharSequence[0]),(d,n)->runs.get(n).run()).create());
 }
 private void attach(SectionConfig.Section s){
  ArrayList<String> keys=new ArrayList<>();ArrayList<CharSequence> names=new ArrayList<>();
  for(MessagesController.DialogFilter f:getMessagesController().getDialogFilters())if(!f.isDefault() && !s.tabs.contains("tg:"+f.id)){keys.add("tg:"+f.id);names.add(f.name+" · Telegram");}
  for(SectionConfig.Folder f:config.folders.values())if(!s.tabs.contains(f.id)){keys.add(f.id);names.add(f.name+" · "+getString(R.string.TetogramSectionFolderLocalLabel));}
  if(keys.isEmpty()){showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramSectionLocalAdd)).setPositiveButton(getString(R.string.OK),null).create());return;}
  showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramSectionAttach)).setItems(names.toArray(new CharSequence[0]),(d,n)->{s.tabs.add(keys.get(n));save();}).create());
 }
 private void pick(SectionConfig.Scope scope,boolean exclude){
  Bundle args=new Bundle();args.putBoolean("onlySelect",true);args.putInt("dialogsType",0);DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(currentAccount);
  picker.setDelegate((fragment,dids,message,param,notify,schedule,repeat,topics)->{if(!valid() || dids.isEmpty())return false;for(MessagesStorage.TopicKey key:dids){if(exclude)scope.exclude(key.dialogId);else scope.include(key.dialogId);}if(!persist())return false;fragment.finishFragment();refresh();return true;});presentFragment(picker);
 }
 private void confirm(CharSequence message,Runnable action){showDialog(new AlertDialog.Builder(getContext()).setMessage(message).setPositiveButton(getString(R.string.OK),(d,w)->{if(valid())action.run();}).setNegativeButton(getString(R.string.Cancel),null).create());}
 private void input(CharSequence title,String initial,java.util.function.Consumer<String> changed){
  EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(18);field.setSingleLine(true);field.setText(initial);field.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(8),AndroidUtilities.dp(24),AndroidUtilities.dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);
  showDialog(new AlertDialog.Builder(getContext()).setTitle(title).setView(field).setPositiveButton(getString(R.string.Save),(d,w)->{if(valid() && !field.getText().toString().trim().isEmpty())changed.accept(SectionConfig.title(field.getText().toString()));}).setNegativeButton(getString(R.string.Cancel),null).create());field.requestFocus();AndroidUtilities.showKeyboard(field);
 }
 @Override protected void click(UItem item,View view,int p,float x,float y){if(!savingIcon && valid() && item.id>0 && item.id<=actions.size())actions.get(item.id-1).run();}
}
