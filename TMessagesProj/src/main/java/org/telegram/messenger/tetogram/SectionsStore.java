/* Tetogram, 2026. GNU GPL v2 or later. Private, owner-fenced and checked persistence. */
package org.telegram.messenger.tetogram;
import android.content.*;
import org.telegram.messenger.*;
public final class SectionsStore {
 private static final Object LOCK=new Object();private SectionsStore(){}
 public static String namespace(long uid){return SectionConfig.accountNamespace(uid);}
 private static SharedPreferences prefs(long owner){if(owner==0)throw new IllegalStateException("No account");return ApplicationLoader.applicationContext.getSharedPreferences(namespace(owner),Context.MODE_PRIVATE);}
 public static SectionConfig load(int a){return SectionConfig.decode(prefs(UserConfig.getInstance(a).getClientUserId()).getString("config",""));}
 private static void write(int a,long owner,SectionConfig c){if(owner==0 || owner!=UserConfig.getInstance(a).getClientUserId())throw new IllegalStateException("Account changed");SharedPreferences p=prefs(owner);String old=p.getString("config","");if(!p.edit().putString("config",c.encode()).commit()){p.edit().putString("config",old).commit();throw new IllegalStateException("Section settings could not be saved");}SectionIcons.cleanup(owner,c);}
 public static void save(int a,long owner,SectionConfig c){synchronized(LOCK){SectionConfig latest=SectionConfig.decode(prefs(owner).getString("config",""));c.preserveNewerIcons(latest);write(a,owner,c);}}
 public static boolean trySave(int a,long owner,SectionConfig c){try{save(a,owner,c);return true;}catch(RuntimeException e){FileLog.e("Tetogram section settings could not be saved");return false;}}
 public static boolean updateIcon(int a,long owner,String id,String type,String value,String file){synchronized(LOCK){if(owner!=UserConfig.getInstance(a).getClientUserId())return false;SectionConfig c=load(a);SectionConfig.Section target=c.section(id);if(target==null || "image".equals(type) && SectionIcons.resolve(owner,id,file)==null)return false;target.iconType=type;target.icon=value;target.iconImage=file;target.iconRevision++;write(a,owner,c);return true;}}
}
