/* Tetogram, 2026. GPL v2 or later. Single ownership of editor parameters; storage keys are unchanged. */
package org.telegram.messenger.tetogram;
import java.util.*;
public final class AppearanceEditorPolicy {
    public enum Category { COLORS, WALLPAPERS, LIST, CHATS, PROFILE, SECTIONS, ADVANCED, LAUNCHER }
    public enum Kind { COLOR, BOOLEAN, NUMBER, TEXT, MEDIA }
    public static final class Definition {
        public final String key;public final Category category;public final Kind kind;public final String defaultValue;public final int preview;
        Definition(String key,Category category,Kind kind,String fallback){this.key=key;this.category=category;this.kind=kind;defaultValue=fallback;preview=preview(category);}
        public boolean supports(String scope){return AppearanceEditorPolicy.supports(scope,category);}
        public String inheritance(){return plain(key).startsWith("banner.")?"global/profile":"global/section/folder/chat/topic";}
    }
    private AppearanceEditorPolicy(){}
    public static String plain(String key){return key.startsWith("light.")?key.substring(6):key.startsWith("dark.")?key.substring(5):key;}
    public static Category category(String key){
        String name=plain(key);
        if(name.startsWith("launcher."))return Category.LAUNCHER;
        if(name.startsWith("effects."))return Category.ADVANCED;
        if(name.contains("wallpaper")||name.startsWith("wallpaper."))return Category.WALLPAPERS;
        if(name.startsWith("banner."))return Category.PROFILE;
        if(name.startsWith("section.")||name.contains("chats_tab")||name.contains("actionBarTab"))return Category.SECTIONS;
        if(name.startsWith("list.")||name.startsWith("chats_"))return Category.LIST;
        if(name.startsWith("bubble.")||name.startsWith("chat_")||name.startsWith("group."))return Category.CHATS;
        return Category.COLORS;
    }
    public static boolean supports(String scope,Category category){boolean global=AppearanceConfig.GLOBAL.equals(scope),profile=scope.startsWith("profile:");switch(category){case LAUNCHER:case ADVANCED:return global;case PROFILE:return global||profile;case SECTIONS:return global||scope.startsWith("section:")||scope.startsWith("folder:");case CHATS:case LIST:return !profile;default:return true;}}
    public static Definition define(String key){
        boolean color=key.startsWith("light.")||key.startsWith("dark.");String name=plain(key),part=name.substring(name.lastIndexOf('.')+1);
        boolean bool=Arrays.asList("enabled","loop","animate","gradientMotion","pattern","separators","reduceOnBattery").contains(part)||name.endsWith("Tail")||name.endsWith("Shadow")||part.equals("parallax")&&!name.equals("banner.parallax");
        Kind kind=color?Kind.COLOR:name.endsWith(".file")?Kind.MEDIA:bool?Kind.BOOLEAN:name.equals("mode")||name.endsWith(".base")||name.endsWith(".type")||name.endsWith(".fit")||name.equals("launcher.label")||name.equals("launcher.emoji")?Kind.TEXT:Kind.NUMBER;
        String fallback="inherit";if(kind==Kind.BOOLEAN)fallback=Boolean.toString(!Arrays.asList("parallax","gradientMotion","pattern").contains(part));else if(kind==Kind.NUMBER)fallback=part.equals("fps")?"30":part.equals("quality")?"1920":part.equals("angle")?"45":part.equals("patternIntensity")?"34":name.equals("launcher.iconScale")?"0.78":name.equals("banner.height")?"280":name.equals("section.outlineWidth")?"2":name.startsWith("bubble.")?"native":part.toLowerCase(Locale.ROOT).endsWith("opacity")||part.equals("speed")||part.equals("zoom")||part.equals("durationScale")?"1":"0";
        return new Definition(key,category(key),kind,fallback);
    }
    public static int preview(Category category){switch(category){case LIST:return 1;case PROFILE:return 2;case SECTIONS:return 4;default:return 0;}}
    public static boolean dirty(AppearanceConfig.Style saved,AppearanceConfig.Style draft){return !((saved==null?Collections.emptyMap():saved.values).equals(draft==null?Collections.emptyMap():draft.values));}
    /** Selecting another category/mode is a view operation, never a storage migration. */
    public static AppearanceConfig.Style resetCategory(AppearanceConfig.Style style,Category category){AppearanceConfig.Style result=style.copy();for(String key:new ArrayList<>(result.values.keySet()))if(category(key)==category)result.values.remove(key);return result;}
    public static AppearanceConfig.Style resetWallpaper(AppearanceConfig.Style style,String domain){AppearanceConfig.Style result=style.copy();for(String key:new ArrayList<>(result.values.keySet()))if(AppearanceConfig.isSurfaceWallpaperKey(key,domain))result.values.remove(key);return result;}
    public static boolean sameMedia(AppearanceConfig.Resolved a,AppearanceConfig.Resolved b){for(String key:new String[]{"wallpaper.file","wallpaper.type","wallpaper.cover.file"})if(!Objects.equals(a.get(key),b.get(key)))return false;return true;}
}
