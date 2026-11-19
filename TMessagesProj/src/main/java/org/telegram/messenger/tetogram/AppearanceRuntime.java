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
    private static volatile SparseIntArray nativeLight,nativeDark;
    private static boolean preparingBase;
    private static volatile long generation;
    private static long activeOwner;
    public static long generation(){return generation;}
    public static String globalValue(String key){return global.get(key);}
    private AppearanceRuntime(){}
    public static int baseColor(boolean dark,int key){
        if(Theme.isCurrentThemeDark()==dark)return Theme.getNativeColor(key);
        SparseIntArray palette=dark?nativeDark:nativeLight;return palette==null?Theme.getNativeColor(key):palette.get(key,Theme.getDefaultColor(key));
    }
    public static void prepareBaseColors(){
        if(preparingBase||nativeLight!=null)return;preparingBase=true;
        Utilities.themeQueue.postRunnable(()->{
            SparseIntArray day=null,night=null;
            try{day=org.telegram.ui.ActionBar.EmojiThemes.getPreviewColors(Theme.getTheme("Day"));night=org.telegram.ui.ActionBar.EmojiThemes.getPreviewColors(Theme.getTheme("Dark Blue"));}catch(Exception e){FileLog.e("Tetogram native preview palettes unavailable");}
            final SparseIntArray d=day,n=night;AndroidUtilities.runOnUIThread(()->{nativeLight=d;nativeDark=n;preparingBase=false;NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.tetogramAppearanceChanged,-1);});
        });
    }
    public static SparseIntArray colors(AppearanceConfig.Resolved r,boolean dark){SparseIntArray out=new SparseIntArray();String prefix=dark?"dark.":"light.";for(java.util.Map.Entry<String,String> e:r.values.entrySet())if(e.getKey().startsWith(prefix)){int key=ThemeColors.stringKeyToInt(e.getKey().substring(prefix.length()));if(key>=0)try{out.put(key,Integer.parseInt(e.getValue()));}catch(NumberFormatException ignored){}}return out;}
    public static Integer globalColor(int key){if(light.size()==0 && dark.size()==0)return null;SparseIntArray c=Theme.isCurrentThemeDark()?dark:light;int i=c.indexOfKey(key);return i<0?null:c.valueAt(i);}
    public static boolean effectsAllowed(){boolean system=true;try{if(android.os.Build.VERSION.SDK_INT>=26)system=android.animation.ValueAnimator.areAnimatorsEnabled();}catch(Exception ignored){}return AppearanceEffects.allowed(!"false".equals(global.get("effects.enabled")),system,!"false".equals(global.get("effects.reduceOnBattery")),LiteMode.isPowerSaverApplied());}
    public static int effectDuration(int base){return AppearanceEffects.duration(base,global.number("effects.durationScale",1),effectsAllowed());}
    public static Integer globalNamedColor(String key){return global.color(Theme.isCurrentThemeDark(),key);}
    public static float globalNumber(String key,float fallback){return global.number(key,fallback);}
    public static void activate(int account){if(account!=UserConfig.selectedAccount)return;AppearanceConfig.Resolved next=AppearanceStore.resolve(account,AppearanceConfig.Context.direct(0,0));long uid=AppearanceStore.owner(account);if(activeOwner!=uid||!global.values.equals(next.values))generation++;activeOwner=uid;global=next;light=colors(global,false);dark=colors(global,true);}
    public static void changed(int account){changed(account,true);}
    public static void changed(int account,boolean applyMode){activate(account);if(account==UserConfig.selectedAccount && Theme.getActiveTheme()!=null){String mode=global.get("mode");if(applyMode)syncAutoNight(mode);if(applyMode && ("dark".equals(mode)||"light".equals(mode)) && Theme.isCurrentThemeDark()!= "dark".equals(mode)){Theme.ThemeInfo base=Theme.getTheme("dark".equals(mode)?"Dark Blue":"Day");if(base!=null)Theme.applyTheme(base);}Theme.refreshThemeColors(false,true);}NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.tetogramAppearanceChanged,account);}
    /** A forced light/dark mode pauses Telegram's auto-night; inheriting again restores the user's own schedule. */
    private static void syncAutoNight(String mode){
        android.content.SharedPreferences prefs=ApplicationLoader.applicationContext.getSharedPreferences("tetogram_appearance_device",android.content.Context.MODE_PRIVATE);
        int remembered=prefs.getInt("native_auto_night",AppearanceNight.UNSET);
        int[] next=AppearanceNight.decide(mode,Theme.selectedAutoNightType,remembered);
        if(next[1]!=remembered){if(next[1]==AppearanceNight.UNSET)prefs.edit().remove("native_auto_night").apply();else prefs.edit().putInt("native_auto_night",next[1]).apply();}
        if(next[0]!=Theme.selectedAutoNightType){Theme.selectedAutoNightType=next[0];Theme.saveAutoNightThemeConfig();if(next[0]!=AppearanceNight.NONE)Theme.checkAutoNightThemeConditions(true);}
    }
    public static void switchAccount(int account){global=new AppearanceConfig.Resolved();light=new SparseIntArray();dark=new SparseIntArray();AppearanceStore.load(account,(ok,c)->{if(ok)changed(account);});}
}
