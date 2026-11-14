/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.messenger.tetogram;
import android.util.SparseIntArray;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeColors;

/** Published snapshots only: no disk IO, no mutable global paints in a chat override. */
public final class AppearanceRuntime {
    private static volatile SparseIntArray light=new SparseIntArray(),dark=new SparseIntArray();
    private static volatile AppearanceConfig.Resolved global=new AppearanceConfig.Resolved();
    private AppearanceRuntime(){}
    public static SparseIntArray colors(AppearanceConfig.Resolved r,boolean dark){SparseIntArray out=new SparseIntArray();String prefix=dark?"dark.":"light.";for(java.util.Map.Entry<String,String> e:r.values.entrySet())if(e.getKey().startsWith(prefix)){int key=ThemeColors.stringKeyToInt(e.getKey().substring(prefix.length()));if(key>=0)try{out.put(key,Integer.parseInt(e.getValue()));}catch(NumberFormatException ignored){}}return out;}
    public static Integer globalColor(int key){if(light.size()==0 && dark.size()==0)return null;SparseIntArray c=Theme.isCurrentThemeDark()?dark:light;int i=c.indexOfKey(key);return i<0?null:c.valueAt(i);}
    public static float globalNumber(String key,float fallback){return global.number(key,fallback);}
    public static void activate(int account){if(account!=UserConfig.selectedAccount)return;global=AppearanceStore.resolve(account,AppearanceConfig.Context.direct(0,0));light=colors(global,false);dark=colors(global,true);}
    public static void changed(int account){activate(account);if(account==UserConfig.selectedAccount && Theme.getActiveTheme()!=null){String mode=global.get("mode");if(("dark".equals(mode)||"light".equals(mode)) && Theme.selectedAutoNightType != Theme.AUTO_NIGHT_TYPE_NONE){Theme.selectedAutoNightType=Theme.AUTO_NIGHT_TYPE_NONE;Theme.saveAutoNightThemeConfig();}if(("dark".equals(mode)||"light".equals(mode)) && Theme.isCurrentThemeDark()!= "dark".equals(mode)){Theme.ThemeInfo base=Theme.getTheme("dark".equals(mode)?"Dark Blue":"Day");if(base!=null)Theme.applyTheme(base);}Theme.refreshThemeColors(false,true);}NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.tetogramAppearanceChanged,account);}
    public static void switchAccount(int account){global=new AppearanceConfig.Resolved();light=new SparseIntArray();dark=new SparseIntArray();AppearanceStore.load(account,(ok,c)->{if(ok)changed(account);});}
}
