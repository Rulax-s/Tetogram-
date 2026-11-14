/* Tetogram, 2026. GNU GPL v2 or later. Duration policy, no system settings writes. */
package org.telegram.messenger.tetogram;
public final class AppearanceEffects {
    private AppearanceEffects(){}
    public static int duration(int base,float scale,boolean allowed){if(!allowed)return 1;if(Float.isNaN(scale)||Float.isInfinite(scale))scale=1;return Math.max(1,Math.round(base*Math.max(.25f,Math.min(2,scale))));}
    public static boolean allowed(boolean enabled,boolean systemAnimations,boolean reduceOnBattery,boolean battery){return enabled&&systemAnimations&&(!reduceOnBattery||!battery);}
}
