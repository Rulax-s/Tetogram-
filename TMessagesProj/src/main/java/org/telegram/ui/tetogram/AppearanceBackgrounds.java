/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.ui.tetogram;
import android.graphics.drawable.*;
import org.telegram.messenger.tetogram.AppearanceConfig;
public final class AppearanceBackgrounds {
    private AppearanceBackgrounds(){}
    public static Drawable colors(AppearanceConfig.Resolved r,boolean dark){
        Integer first=r.color(dark,"chat_wallpaper");if(first==null)return null;
        Integer second=r.color(dark,"chat_wallpaper_gradient_to1");
        if(second==null || second==0)return new ColorDrawable(first);
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{first,second});
    }
}
