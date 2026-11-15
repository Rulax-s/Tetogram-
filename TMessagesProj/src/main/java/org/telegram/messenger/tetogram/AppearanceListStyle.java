/* Tetogram, 2026. GPL v2 or later. One opacity source for a continuous flat surface or separate cards. */
package org.telegram.messenger.tetogram;
public final class AppearanceListStyle {
    private AppearanceListStyle(){}
    public static boolean uniform(AppearanceConfig.Resolved style){String origin=style.origins.get("list.rowOpacity");if(origin!=null&&(origin.startsWith("chat:")||origin.startsWith("topic:")))return false;return style.get("list.rowOpacity")!=null&&style.number("list.radius",0)<=0&&style.number("list.sideInset",0)<=0&&style.number("list.rowGap",0)<=0;}
    public static int tint(int color,float opacity){if(Float.isNaN(opacity)||Float.isInfinite(opacity))opacity=1;return color&0xffffff|Math.round((color>>>24)*Math.max(0,Math.min(1,opacity)))<<24;}
}
