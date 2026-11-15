package org.telegram.ui;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.tetogram.SeasonalIconCatalog;
import java.util.*;

public class LauncherIconController {
    private static boolean calendarReceiverRegistered;
    private static android.content.SharedPreferences preferences(){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_launcher",Context.MODE_PRIVATE);}
    public static LauncherIconSettings settings(){try{return LauncherIconSettings.read(preferences().getAll());}catch(RuntimeException e){LauncherIconSettings c=new LauncherIconSettings();c.corrupt=true;return c;}}
    public static LauncherIcon find(String key){for(LauncherIcon icon:LauncherIcon.values())if(icon.key.equals(key))return icon;return null;}
    public static boolean hasArtwork(LauncherIcon icon){return icon!=null&&(!isSeasonal(icon)||SeasonalIconCatalog.resource(icon.key)!=0);}
    public static boolean isSeasonal(LauncherIcon icon){String key=AppearanceIconPolicy.canonical(icon.key);return key.startsWith("Tetogram")||key.startsWith("Mikugram");}
    public static LauncherIcon currentIcon(){for(LauncherIcon icon:LauncherIcon.values())if(hasArtwork(icon)&&isEnabled(icon))return icon;return LauncherIcon.DEFAULT;}
    private static ArrayList<LauncherIcon> active(){ArrayList<LauncherIcon> a=new ArrayList<>();for(LauncherIcon icon:LauncherIcon.values())if(isEnabled(icon))a.add(icon);return a;}
    private static void changed(){AndroidUtilities.runOnUIThread(()->NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.tetogramLauncherIconChanged));}
    private static void rememberLast(LauncherIcon icon){if(!icon.key.equals(settings().lastAlias))preferences().edit().putString("lastAlias",icon.key).apply();}
    private static boolean persist(LauncherIconSettings s){try{return preferences().edit().putInt("schema",2).putBoolean("seasonal",s.seasonal).putString("brand",s.brand).putBoolean("automatic",s.automatic).putString("manualSeason",s.manual.name()).putString("season",s.choice().name()).putString("lastAlias",s.lastAlias).putLong("revision",s.revision).commit();}catch(RuntimeException e){FileLog.e("Tetogram launcher settings could not be saved");return false;}}
    public static synchronized void refreshSeasonalIcon(){try{
        LauncherIconSettings s=settings();ArrayList<LauncherIcon> enabled=active();
        if(s.corrupt){LauncherIconSettings clean=new LauncherIconSettings();applyIcon(LauncherIcon.DEFAULT);clean.lastAlias=LauncherIcon.DEFAULT.key;clean.revision=1;persist(clean);changed();return;}
        LauncherIcon desired=s.seasonal?find(AppearanceIconPolicy.alias(s.brand,s.choice(),Calendar.getInstance().get(Calendar.MONTH)+1)):null;
        if(enabled.size()==1&&hasArtwork(enabled.get(0))&&(!s.seasonal||AppearanceIconPolicy.sameArtwork(enabled.get(0).key,desired==null?null:desired.key)||!hasArtwork(desired))){rememberLast(enabled.get(0));return;}
        if(!hasArtwork(desired)){LauncherIcon last=find(s.lastAlias);desired=hasArtwork(last)&&(enabled.isEmpty()||enabled.contains(last))?last:currentIcon();}
        applyIcon(desired);rememberLast(desired);changed();
    }catch(RuntimeException e){FileLog.e("Tetogram launcher icon update unavailable; existing component state retained");}}
    /** Explicit user action. Component operations succeed before device preferences are committed. */
    public static synchronized void applySelection(LauncherIconSettings choice,LauncherIcon classic,long expectedRevision){
        LauncherIconSettings previous=settings();if(previous.revision!=expectedRevision)throw new IllegalStateException("Launcher settings changed");
        LauncherIconSettings next=choice.copy();if(next.corrupt||!("Tetogram".equals(next.brand)||"Mikugram".equals(next.brand))||next.manual==null||next.manual==AppearanceIconPolicy.Season.AUTO)throw new IllegalArgumentException("Invalid launcher settings");
        LauncherIcon desired=next.seasonal?find(AppearanceIconPolicy.alias(next.brand,next.choice(),Calendar.getInstance().get(Calendar.MONTH)+1)):classic==null?LauncherIcon.DEFAULT:classic;
        if(!hasArtwork(desired))throw new IllegalArgumentException("Prepared seasonal artwork missing");
        LauncherIcon old=currentIcon();applyIcon(desired);next.lastAlias=desired.key;next.revision=previous.revision+1;
        if(!persist(next)){try{applyIcon(old);}catch(RuntimeException ignored){}throw new IllegalStateException("Launcher settings could not be saved");}changed();
    }
    public static void setSeasonalIcon(String brand,AppearanceIconPolicy.Season season){LauncherIconSettings s=settings();long revision=s.revision;s.brand=brand;s.select(season);applySelection(s,null,revision);}
    public static boolean isSeasonalMode(){return settings().seasonal;}
    public static String seasonalBrand(){return settings().brand;}
    public static String seasonalChoice(){return settings().choice().name();}
    public static void tryFixLauncherIconIfNeeded(){
        refreshSeasonalIcon();
        if(!calendarReceiverRegistered){try{android.content.IntentFilter dates=new android.content.IntentFilter(android.content.Intent.ACTION_DATE_CHANGED);dates.addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED);dates.addAction(android.content.Intent.ACTION_TIME_CHANGED);androidx.core.content.ContextCompat.registerReceiver(ApplicationLoader.applicationContext,new android.content.BroadcastReceiver(){@Override public void onReceive(Context c,android.content.Intent i){refreshSeasonalIcon();}},dates,androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);calendarReceiverRegistered=true;}catch(RuntimeException e){FileLog.e("Tetogram calendar receiver unavailable");}}
    }
    public static boolean isEnabled(LauncherIcon icon){int value=ApplicationLoader.applicationContext.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ApplicationLoader.applicationContext));return value==PackageManager.COMPONENT_ENABLED_STATE_ENABLED||value==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT&&icon==LauncherIcon.DEFAULT;}
    public static void setIcon(LauncherIcon icon){LauncherIconSettings s=settings();long revision=s.revision;s.corrupt=false;s.seasonal=false;s.brand="Tetogram";applySelection(s,icon,revision);}
    private static void applyIcon(LauncherIcon icon){
        final Context context=ApplicationLoader.applicationContext;final PackageManager pm=context.getPackageManager();ArrayList<String> known=new ArrayList<>();for(LauncherIcon value:LauncherIcon.values())known.add(value.key);
        LauncherIconSwitch.apply(icon.key,known,new LauncherIconSwitch.Access(){
            public boolean enabled(String key){return isEnabled(find(key));}
            public void enable(String key){pm.setComponentEnabledSetting(find(key).getComponentName(context),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);}
            public void disable(String key){pm.setComponentEnabledSetting(find(key).getComponentName(context),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);}
        });
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
        TETO_AUTUMN("TetoAutumnIcon", android.R.color.black, R.drawable.tetogram_teto_autumn_art, R.string.AppIconDefault),
        TETOGRAMSPRING("TetogramSpring", android.R.color.black, SeasonalIconCatalog.resource("TetogramSpring"), R.string.AppIconDefault),
        TETOGRAMSUMMER("TetogramSummer", android.R.color.black, SeasonalIconCatalog.resource("TetogramSummer"), R.string.AppIconDefault),
        TETOGRAMAUTUMN("TetogramAutumn", android.R.color.black, SeasonalIconCatalog.resource("TetogramAutumn"), R.string.AppIconDefault),
        TETOGRAMWINTER("TetogramWinter", android.R.color.black, SeasonalIconCatalog.resource("TetogramWinter"), R.string.AppIconDefault),
        MIKUGRAMSPRING("MikugramSpring", android.R.color.black, SeasonalIconCatalog.resource("MikugramSpring"), R.string.AppIconDefault),
        MIKUGRAMSUMMER("MikugramSummer", android.R.color.black, SeasonalIconCatalog.resource("MikugramSummer"), R.string.AppIconDefault),
        MIKUGRAMAUTUMN("MikugramAutumn", android.R.color.black, SeasonalIconCatalog.resource("MikugramAutumn"), R.string.AppIconDefault),
        MIKUGRAMWINTER("MikugramWinter", android.R.color.black, SeasonalIconCatalog.resource("MikugramWinter"), R.string.AppIconDefault);

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
