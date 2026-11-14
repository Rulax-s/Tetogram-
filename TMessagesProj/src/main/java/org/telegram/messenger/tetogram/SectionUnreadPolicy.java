/* Tetogram, 2026. GNU GPL v2 or later. Native unread/mute snapshots in, no independent message storage. */
package org.telegram.messenger.tetogram;
import java.util.*;
public final class SectionUnreadPolicy {
 public static final String SYSTEM_ID="system:unread";
 private SectionUnreadPolicy(){}
 public static final class Chat {public long id;public int unread;public boolean marked,muted,archived,mentioned;public int category;public Chat(long id,int unread,boolean marked,boolean muted){this.id=id;this.unread=Math.max(0,unread);this.marked=marked;this.muted=muted;}}
 public static long count(Collection<Chat> chats,java.util.function.LongPredicate member,boolean includeMuted){long sum=0;HashSet<Long> seen=new HashSet<>();for(Chat c:chats)if(seen.add(c.id) && (!c.muted || includeMuted) && member.test(c.id))sum+=c.unread;return sum;}
 public static boolean member(SectionConfig.Scope scope,Chat c){return SectionConfig.includes(scope,c.id,c.category,c.unread>0 || c.marked || c.mentioned,c.muted && !c.mentioned,c.archived);}
 public static long sectionCount(SectionConfig.Section section,Collection<Chat> chats,Map<String,Set<Long>> folders){
  HashSet<Long> ids=new HashSet<>();long total=0;
  for(Chat c:chats)if(ids.add(c.id) && !c.muted && !section.excluded.contains(c.id)){
   boolean included=member(section,c);if(!included)for(String key:section.tabs){Set<Long> folder=folders.get(key);if(folder!=null && folder.contains(c.id)){included=true;break;}}
   if(included)total+=c.unread;
  }
  return total;
 }
 public static boolean matches(Chat c,boolean includeMuted){return (!c.muted || includeMuted) && (c.unread>0 || c.marked);}
 public static boolean showSystem(boolean enabled,boolean any,boolean active){return enabled && (any || active);}
 public static String label(long value,boolean compact){return compact && value>99?"99+":Long.toString(value);}
}
