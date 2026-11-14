/* Tetogram, 2026. GNU GPL v2 or later. Cache extents include the curve and native padding. */
package org.telegram.messenger.tetogram;
public final class AppearanceGeometry {
    private AppearanceGeometry(){}
    private static float radius(float r){return Float.isNaN(r)||Float.isInfinite(r)?16:Math.max(0,Math.min(32,r));}
    public static float cacheWidth(float r){return Math.max(50,2*radius(r)+14);}
    public static float cacheHeight(float r){return Math.max(40,2*radius(r)+8);}
}
