/* Tetogram, 2026. GNU GPL v2 or later. Read-only Telegram state, no API requests. */
package org.telegram.messenger.tetogram;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import java.util.*;
public final class SectionUnread {
 private SectionUnread(){}
 public static final class Snapshot {public final HashMap<String,Long> counts=new HashMap<>();public final ArrayList<TLRPC.Dialog> unreadDialogs=new ArrayList<>();public boolean any;}
 public static Snapshot snapshot(int a,SectionConfig config){
  Snapshot out=new Snapshot();MessagesController m=MessagesController.getInstance(a);ArrayList<SectionUnreadPolicy.Chat> states=new ArrayList<>();HashMap<Long,TLRPC.Dialog> dialogs=new HashMap<>();
  for(TLRPC.Dialog d:m.getAllDialogs())if(!(d instanceof TLRPC.TL_dialogFolder) && !dialogs.containsKey(d.id)){
   dialogs.put(d.id,d);SectionUnreadPolicy.Chat c=new SectionUnreadPolicy.Chat(d.id,m.getDialogUnreadCount(d),d.unread_mark,m.isDialogMuted(d.id,0));c.category=SectionDialogs.category(a,d);c.archived=d.folder_id!=0;c.mentioned=d.unread_mentions_count>0;states.add(c);
   if(SectionUnreadPolicy.matches(c,config.unreadIncludeMuted)){out.unreadDialogs.add(d);out.any=true;}
  }
  out.counts.put(SectionUnreadPolicy.SYSTEM_ID,SectionUnreadPolicy.count(states,id->{TLRPC.Dialog d=dialogs.get(id);return d!=null;},config.unreadIncludeMuted));
  HashMap<String,Set<Long>> folderMembers=new HashMap<>();
  for(SectionConfig.Section section:config.sections)for(String key:section.tabs)if(!folderMembers.containsKey(key)){
   HashSet<Long> members=new HashSet<>();SectionConfig.Folder local=config.folders.get(key);MessagesController.DialogFilter server=SectionDialogs.server(a,key);
   if(local!=null){for(SectionUnreadPolicy.Chat c:states)if(SectionUnreadPolicy.member(local,c))members.add(c.id);}
   else if(server!=null && !server.locked){for(TLRPC.Dialog d:dialogs.values())if(server.includesDialog(AccountInstance.getInstance(a),d.id,d))members.add(d.id);}
   folderMembers.put(key,members);
  }
  for(SectionConfig.Section section:config.sections)out.counts.put(section.id,SectionUnreadPolicy.sectionCount(section,states,folderMembers));
  return out;
 }
 public static ArrayList<TLRPC.Dialog> dialogs(int a,SectionConfig config){MessagesController m=MessagesController.getInstance(a);ArrayList<TLRPC.Dialog> result=new ArrayList<>();HashSet<Long> seen=new HashSet<>();for(TLRPC.Dialog d:m.getAllDialogs())if(!(d instanceof TLRPC.TL_dialogFolder) && seen.add(d.id) && SectionUnreadPolicy.matches(new SectionUnreadPolicy.Chat(d.id,m.getDialogUnreadCount(d),d.unread_mark,m.isDialogMuted(d.id,0)),config.unreadIncludeMuted))result.add(d);return result;}
}
