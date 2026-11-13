/* Tetogram, 2026. GNU GPL v2 or later. Private account-owned preferences only. */
package org.telegram.messenger.tetogram;
import android.content.*;
import org.telegram.messenger.*;
public final class SectionsStore {
 private SectionsStore(){}
 public static String namespace(long uid){return SectionConfig.accountNamespace(uid);}
 private static SharedPreferences prefs(int a){long uid=UserConfig.getInstance(a).getClientUserId();if(uid==0)throw new IllegalStateException("No account");return ApplicationLoader.applicationContext.getSharedPreferences(namespace(uid),Context.MODE_PRIVATE);}
 public static SectionConfig load(int a){return SectionConfig.decode(prefs(a).getString("config",""));}
 public static void save(int a,long owner,SectionConfig c){if(owner==0 || owner!=UserConfig.getInstance(a).getClientUserId())throw new IllegalStateException("Account changed");prefs(a).edit().putString("config",c.encode()).apply();}
}
