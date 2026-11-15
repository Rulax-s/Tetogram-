/* Tetogram, 2026. GPL v2 or later. Calendar and adaptive-icon geometry without platform dependencies. */
package org.telegram.messenger.tetogram;
public final class AppearanceIconPolicy {
    public enum Season { AUTO, SPRING, SUMMER, AUTUMN, WINTER }
    public static final float SAFE_CONTENT_SCALE=.78f; // 56.16dp content in a 108dp layer / 72dp viewport.
    private AppearanceIconPolicy(){}
    public static Season season(int month){if(month<1||month>12)throw new IllegalArgumentException("Invalid month");return month>=3&&month<=5?Season.SPRING:month>=6&&month<=8?Season.SUMMER:month>=9&&month<=11?Season.AUTUMN:Season.WINTER;}
    /** Whole source square fits inside the guaranteed central safe circle; no artwork pixels are cropped. */
    public static final float SEASONAL_CONTENT_SCALE=.645f; // 108dp * (1 - 2*.285) / 72dp viewport.
    public static boolean available(String brand,Season season){return ("Tetogram".equals(brand)||"Mikugram".equals(brand))&&season!=null&&season!=Season.AUTO;}
    public static String alias(String brand,Season choice,int month){Season actual=choice==null||choice==Season.AUTO?season(month):choice;return available(brand,actual)?brand+actual.name().substring(0,1)+actual.name().substring(1).toLowerCase(java.util.Locale.ROOT):null;}
    public static String canonical(String key){if(key==null)return "";if(key.startsWith("Miku")&&key.endsWith("Icon"))return "Mikugram"+key.substring(4,key.length()-4);if(key.startsWith("Teto")&&key.endsWith("Icon"))return "Tetogram"+key.substring(4,key.length()-4);return key;}
    public static boolean sameArtwork(String current,String desired){return desired!=null&&canonical(current).equals(desired);}
    public static float scale(float value){return Float.isNaN(value)||Float.isInfinite(value)?SAFE_CONTENT_SCALE:Math.max(.45f,Math.min(1.1f,value));}
}
