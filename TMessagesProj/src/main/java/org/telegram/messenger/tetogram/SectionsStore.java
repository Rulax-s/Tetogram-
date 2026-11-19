/* Tetogram, 2026. GNU GPL v2 or later. Private, owner-fenced and checked persistence. */
package org.telegram.messenger.tetogram;
import android.content.*;
import org.telegram.messenger.*;
public final class SectionsStore {
 private static final Object LOCK=new Object();private SectionsStore(){}
 public static String namespace(long uid){return SectionConfig.accountNamespace(uid);}
 private static SharedPreferences prefs(long owner){if(owner==0)throw new IllegalStateException("No account");return ApplicationLoader.applicationContext.getSharedPreferences(namespace(owner),Context.MODE_PRIVATE);}
 private static final java.util.concurrent.ConcurrentHashMap<Long,Object[]> CACHE=new java.util.concurrent.ConcurrentHashMap<>();
 /**
  * Decoded once per stored text; the same instance is returned until the text changes. Screens may update their
  * own in-memory copy for display, but persistence always goes through {@link #update}, never a whole-object save.
  */
 public static SectionConfig load(int a){long owner=UserConfig.getInstance(a).getClientUserId();String raw=prefs(owner).getString("config","");Object[] cached=CACHE.get(owner);if(cached!=null&&raw.equals(cached[0]))return (SectionConfig)cached[1];SectionConfig c=SectionConfig.decode(raw);CACHE.put(owner,new Object[]{raw,c});return c;}
 public interface Mutation{void apply(SectionConfig latest);}
 /** Applies one change to the newest stored settings, so concurrent screens never roll back each other's edits. */
 public static boolean update(int a,long owner,Mutation change){
  try{synchronized(LOCK){if(owner==0||owner!=UserConfig.getInstance(a).getClientUserId())return false;SectionConfig latest=SectionConfig.decode(prefs(owner).getString("config",""));change.apply(latest);write(a,owner,latest);}return true;}
  catch(RuntimeException e){FileLog.e("Tetogram section settings could not be saved");return false;}
 }
 /** True when stored settings exist but cannot be read. */
 public static boolean unreadable(int a){try{load(a);return false;}catch(RuntimeException e){return true;}}
 /** Keeps the unreadable text under a separate key and starts with empty settings; icon files are left alone. */
 public static boolean recover(int a,long owner){
  synchronized(LOCK){if(owner==0||owner!=UserConfig.getInstance(a).getClientUserId())return false;SharedPreferences p=prefs(owner);String raw=p.getString("config","");CACHE.remove(owner);return p.edit().putString("config_corrupt_"+System.currentTimeMillis(),raw).remove("config").commit();}
 }
 private static void write(int a,long owner,SectionConfig c){if(owner==0 || owner!=UserConfig.getInstance(a).getClientUserId())throw new IllegalStateException("Account changed");SharedPreferences p=prefs(owner);String old=p.getString("config","");if(!p.edit().putString("config",c.encode()).commit()){p.edit().putString("config",old).commit();throw new IllegalStateException("Section settings could not be saved");}SectionIcons.cleanup(owner,c);}
 public static void save(int a,long owner,SectionConfig c){synchronized(LOCK){SectionConfig latest=SectionConfig.decode(prefs(owner).getString("config",""));c.preserveNewerIcons(latest);c.preserveNewerOpeningMode(latest);write(a,owner,c);}}
 public static boolean trySave(int a,long owner,SectionConfig c){try{save(a,owner,c);return true;}catch(RuntimeException e){FileLog.e("Tetogram section settings could not be saved");return false;}}
 public static boolean updateOpeningMode(int a,long owner,String mode){
  try{synchronized(LOCK){if(owner==0||owner!=UserConfig.getInstance(a).getClientUserId())return false;SectionConfig c=load(a);c.openingMode=SectionOpeningPolicy.normalize(mode);c.openingModeRevision++;write(a,owner,c);}
   AndroidUtilities.runOnUIThread(()->{if(owner==UserConfig.getInstance(a).getClientUserId())NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.tetogramSectionOpeningChanged);});return true;
  }catch(RuntimeException e){FileLog.e("Tetogram section opening mode could not be saved");return false;}
 }
 public static boolean updateIcon(int a,long owner,String id,String type,String value,String file){synchronized(LOCK){if(owner!=UserConfig.getInstance(a).getClientUserId())return false;SectionConfig c=load(a);SectionConfig.Section target=c.section(id);if(target==null || "image".equals(type) && SectionIcons.resolve(owner,id,file)==null)return false;target.iconType=type;target.icon=value;target.iconImage=file;target.iconRevision++;write(a,owner,c);return true;}}
}
