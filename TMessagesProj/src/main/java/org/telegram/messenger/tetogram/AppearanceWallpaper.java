/* Tetogram, 2026. GNU GPL v2 or later. Shared draft transformation for actual apply and live preview. */
package org.telegram.messenger.tetogram;
public final class AppearanceWallpaper {
    private AppearanceWallpaper(){}
    public static void color(AppearanceConfig.Style target,AppearanceConfig.Resolved current,boolean dark,String name,int color,int fallback){
        String prefix=name.startsWith("banner.")?"banner.":name.startsWith("list.")?"list.":"",domain=prefix+"wallpaper";
        target.set(domain+".file","");target.set(domain+".cover.file","");target.set(domain+".source.file","");boolean gradient=name.contains("_gradient_");target.set(domain+".type",gradient?"gradient":"solid");
        if(gradient&&current.color(dark,prefix+"chat_wallpaper")==null)target.color(dark,prefix+"chat_wallpaper",fallback);
        target.color(dark,name,color);
    }
}
