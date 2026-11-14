/* Tetogram, 2026. GNU GPL v2 or later. Native avatar endpoints, no accumulated translations. */
package org.telegram.messenger.tetogram;
public final class AppearanceProfileGeometry {
    private AppearanceProfileGeometry(){}
    private static float clamp(float v,float min,float max){return Float.isNaN(v)||Float.isInfinite(v)?0:Math.max(min,Math.min(max,v));}
    public static float horizontal(float width,float avatarWidth,float margin,float alignment,float expanded){
        float center=(width-avatarWidth)/2;
        float range=Math.max(0,center-Math.max(0,margin));
        return center+range*clamp(alignment,-1,1)*clamp(expanded,0,1);
    }
}
