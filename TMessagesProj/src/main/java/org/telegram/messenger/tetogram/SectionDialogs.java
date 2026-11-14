/* Tetogram, 2026. GNU GPL v2 or later. Projections contain original Dialog references, never messages. */
package org.telegram.messenger.tetogram;
import java.util.*;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
public final class SectionDialogs {
 private SectionDialogs(){}
 public static MessagesController.DialogFilter server(int account,String key){if(key!=null && key.startsWith("tg:"))for(MessagesController.DialogFilter f:MessagesController.getInstance(account).getDialogFilters())if(key.equals("tg:"+f.id) && !f.isDefault())return f;return null;}
 public static int category(int a,TLRPC.Dialog d){
  MessagesController m=MessagesController.getInstance(a);int category=0;
  if(DialogObject.isEncryptedDialog(d.id)){TLRPC.EncryptedChat e=m.getEncryptedChat(DialogObject.getEncryptedChatId(d.id));TLRPC.User u=e==null?null:m.getUser(e.user_id);if(u!=null)category=u.bot?16:u.self || u.contact || ContactsController.getInstance(a).isContact(u.id)?1:2;}
  else if(d.id>0){TLRPC.User u=m.getUser(d.id);if(u!=null)category=u.bot?16:u.self || u.contact || ContactsController.getInstance(a).isContact(u.id)?1:2;}
  else {TLRPC.Chat chat=m.getChat(-d.id);if(chat!=null)category=ChatObject.isChannelAndNotMegaGroup(chat)?8:4;}
  return category;
 }
 public static boolean includes(int a,SectionConfig.Scope s,TLRPC.Dialog d){
  MessagesController m=MessagesController.getInstance(a);
  return SectionConfig.includes(s,d.id,category(a,d),m.getDialogUnreadCount(d)>0 || d.unread_mark || d.unread_mentions_count>0,m.isDialogMuted(d.id,0) && d.unread_mentions_count==0,d.folder_id!=0);
 }
 public static ArrayList<TLRPC.Dialog> project(int a,SectionConfig.Scope scope){
  MessagesController m=MessagesController.getInstance(a);ArrayList<TLRPC.Dialog> result=new ArrayList<>();if(scope==null)return result;
  boolean archived=false;for(TLRPC.Dialog d:m.getDialogs(1))if(!(d instanceof TLRPC.TL_dialogFolder) && includes(a,scope,d)){archived=true;break;}
  for(TLRPC.Dialog d:m.getDialogs(0)){if(d instanceof TLRPC.TL_dialogFolder){if(archived)result.add(d);}else if(includes(a,scope,d))result.add(d);}
  return result;
 }
}
