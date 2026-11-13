/* Tetogram, 2026. GNU GPL v2 or later. Native Labs settings, local account configuration. */
package org.telegram.ui.tetogram;
import static org.telegram.messenger.LocaleController.getString;
import android.os.Bundle;
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
 private final ArrayList<Runnable> actions=new ArrayList<>();
 public SectionsActivity(int account){this(account,null,null);}
 public SectionsActivity(int account,String section){this(account,section,null);}
 private SectionsActivity(int account,String section,String folder){super(account);sectionId=section;folderId=folder;owner=UserConfig.getInstance(account).getClientUserId();}
 @Override protected CharSequence title(){return getString(folderId!=null?R.string.TetogramSectionFolderName:R.string.TetogramSections);}
 private boolean valid(){if(owner==0 || owner!=getUserConfig().getClientUserId()){finishFragment();return false;}return true;}
 private void save(){if(valid()){SectionsStore.save(currentAccount,owner,config);refresh();}}
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
   for(SectionConfig.Section s:config.sections)button(items,s.icon+"  "+s.name,s.id.equals(config.defaultId)?getString(R.string.TetogramSectionDefault):null,()->presentFragment(new SectionsActivity(currentAccount,s.id)));
   items.add(UItem.asHeader(getString(R.string.TetogramSectionLocal)));
   for(SectionConfig.Folder f:config.folders.values())button(items,f.name,()->presentFragment(new SectionsActivity(currentAccount,null,f.id)));
   return;
  }
  SectionConfig.Section s=config.section(sectionId);SectionConfig.Folder f=config.folders.get(folderId);
  if(s==null && f==null){items.add(UItem.asShadow(getString(R.string.TetogramSectionUnavailable)));return;}
  SectionConfig.Scope scope=s!=null?s:f;
  button(items,s!=null?s.name:f.name,()->input(getString(s!=null?R.string.TetogramSectionName:R.string.TetogramSectionFolderName),s!=null?s.name:f.name,name->{if(s!=null)s.name=name;else f.name=name;save();}));
  if(s!=null){
   button(items,getString(R.string.TetogramSectionIcon),s.icon,()->{
    CharSequence[] icons={"★","♥","🎮","💼","👥","🎵",getString(R.string.Edit)};
    showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramSectionIcon)).setItems(icons,(d,n)->{if(n==6)input(getString(R.string.TetogramSectionIcon),s.icon,icon->{s.icon=icon;save();});else{s.icon=icons[n].toString();save();}}).create());
   });
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
   button(items,getString(R.string.Delete),()->confirm(getString(R.string.TetogramSectionDeleteConfirm),()->{config.remove(s.id);SectionsStore.save(currentAccount,owner,config);finishFragment();}));
  }else button(items,getString(R.string.Delete),()->confirm(getString(R.string.TetogramSectionFolderDeleteConfirm),()->{config.removeFolder(f.id);SectionsStore.save(currentAccount,owner,config);finishFragment();}));
 }
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
  picker.setDelegate((fragment,dids,message,param,notify,schedule,repeat,topics)->{if(!valid() || dids.isEmpty())return false;for(MessagesStorage.TopicKey key:dids){if(exclude)scope.exclude(key.dialogId);else scope.include(key.dialogId);}SectionsStore.save(currentAccount,owner,config);fragment.finishFragment();refresh();return true;});presentFragment(picker);
 }
 private void confirm(CharSequence message,Runnable action){showDialog(new AlertDialog.Builder(getContext()).setMessage(message).setPositiveButton(getString(R.string.OK),(d,w)->{if(valid())action.run();}).setNegativeButton(getString(R.string.Cancel),null).create());}
 private void input(CharSequence title,String initial,java.util.function.Consumer<String> changed){
  EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(18);field.setSingleLine(true);field.setText(initial);field.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(8),AndroidUtilities.dp(24),AndroidUtilities.dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);
  showDialog(new AlertDialog.Builder(getContext()).setTitle(title).setView(field).setPositiveButton(getString(R.string.Save),(d,w)->{if(valid() && !field.getText().toString().trim().isEmpty())changed.accept(SectionConfig.title(field.getText().toString()));}).setNegativeButton(getString(R.string.Cancel),null).create());field.requestFocus();AndroidUtilities.showKeyboard(field);
 }
 @Override protected void click(UItem item,View view,int p,float x,float y){if(valid() && item.id>0 && item.id<=actions.size())actions.get(item.id-1).run();}
}
