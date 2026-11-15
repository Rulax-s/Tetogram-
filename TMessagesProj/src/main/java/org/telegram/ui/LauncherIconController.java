package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    private static boolean calendarReceiverRegistered;
    private static android.content.SharedPreferences preferences(){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_launcher",Context.MODE_PRIVATE);}
    public static void refreshSeasonalIcon(){
        if(!preferences().getBoolean("seasonal",false))return;
        String brand=preferences().getString("brand","Tetogram"),choice=preferences().getString("season","AUTO");
        org.telegram.messenger.tetogram.AppearanceIconPolicy.Season season;try{season=org.telegram.messenger.tetogram.AppearanceIconPolicy.Season.valueOf(choice);}catch(Exception e){season=org.telegram.messenger.tetogram.AppearanceIconPolicy.Season.AUTO;}
        String key=org.telegram.messenger.tetogram.AppearanceIconPolicy.alias(brand,season,java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)+1);
        LauncherIcon desired=LauncherIcon.DEFAULT;for(LauncherIcon icon:LauncherIcon.values())if(icon.key.equals(key)){desired=icon;break;}
        boolean exclusive=isEnabled(desired);if(exclusive)for(LauncherIcon other:LauncherIcon.values())if(other!=desired&&isEnabled(other)){exclusive=false;break;}
        if(!exclusive)applyIcon(desired);
    }
    public static void setSeasonalIcon(String brand,org.telegram.messenger.tetogram.AppearanceIconPolicy.Season season){if(!"Tetogram".equals(brand)&&!"Mikugram".equals(brand))throw new IllegalArgumentException("Unknown brand");preferences().edit().putBoolean("seasonal",true).putString("brand",brand).putString("season",season.name()).apply();refreshSeasonalIcon();}
    public static boolean isSeasonalMode(){return preferences().getBoolean("seasonal",false);}
    public static String seasonalBrand(){return preferences().getString("brand","Tetogram");}
    public static String seasonalChoice(){return preferences().getString("season","AUTO");}
    public static void tryFixLauncherIconIfNeeded() {
        refreshSeasonalIcon();
        if(!calendarReceiverRegistered){try{android.content.IntentFilter dates=new android.content.IntentFilter(android.content.Intent.ACTION_DATE_CHANGED);dates.addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED);dates.addAction(android.content.Intent.ACTION_TIME_CHANGED);androidx.core.content.ContextCompat.registerReceiver(ApplicationLoader.applicationContext,new android.content.BroadcastReceiver(){@Override public void onReceive(Context c,android.content.Intent i){refreshSeasonalIcon();}},dates,androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);calendarReceiverRegistered=true;}catch(Exception ignored){}}

        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        preferences().edit().putBoolean("seasonal",false).apply();applyIcon(icon);
    }
    private static void applyIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        pm.setComponentEnabledSetting(icon.getComponentName(ctx),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.tetogram_icon_background, R.drawable.tetogram_mark, R.string.AppIconDefault),
        VINTAGE("VintageIcon", R.drawable.tetogram_icon_background, R.drawable.tetogram_artwork, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.tetogram_aqua_background, R.drawable.tetogram_mark, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.tetogram_icon_background, R.drawable.tetogram_mark, R.string.AppIconPremium, true),
        TURBO("TurboIcon", R.drawable.tetogram_icon_background, R.drawable.tetogram_mark, R.string.AppIconTurbo, true),
        NOX("NoxIcon", R.drawable.tetogram_icon_background, R.drawable.tetogram_mark, R.string.AppIconNox, true),
        MIKU_WINTER("MikuWinterIcon", android.R.color.black, R.drawable.tetogram_miku_winter_art, R.string.AppIconDefault),
        MIKU_AUTUMN("MikuAutumnIcon", android.R.color.black, R.drawable.tetogram_miku_autumn_art, R.string.AppIconDefault),
        MIKU_SUMMER("MikuSummerIcon", android.R.color.black, R.drawable.tetogram_miku_summer_art, R.string.AppIconDefault),
        MIKU_SPRING("MikuSpringIcon", android.R.color.black, R.drawable.tetogram_miku_spring_art, R.string.AppIconDefault),
        TETO_SPRING("TetoSpringIcon", android.R.color.black, R.drawable.tetogram_teto_spring_art, R.string.AppIconDefault),
        TETO_WINTER("TetoWinterIcon", android.R.color.black, R.drawable.tetogram_teto_winter_art, R.string.AppIconDefault),
        TETO_AUTUMN("TetoAutumnIcon", android.R.color.black, R.drawable.tetogram_teto_autumn_art, R.string.AppIconDefault);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
