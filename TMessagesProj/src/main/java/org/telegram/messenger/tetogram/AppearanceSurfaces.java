/* Tetogram, 2026. GNU GPL v2 or later. Separate chat/list/banner media domains. */
package org.telegram.messenger.tetogram;
import java.util.*;
public final class AppearanceSurfaces {
    private AppearanceSurfaces(){}
    public static AppearanceConfig.Resolved list(AppearanceConfig.Resolved source){return map(source,"list",true);}
    public static AppearanceConfig.Resolved banner(AppearanceConfig.Resolved source){return map(source,"banner",false);}
    private static AppearanceConfig.Resolved map(AppearanceConfig.Resolved source,String domain,boolean sectionFallback){
        AppearanceConfig.Resolved out=new AppearanceConfig.Resolved();
        String base=source.get(domain+".wallpaper.base");sectionFallback=sectionFallback&&!"global".equals(base)&&!"telegram".equals(base);
        for(Map.Entry<String,String> e:source.values.entrySet()){
            String origin=source.origins.getOrDefault(e.getKey(),AppearanceConfig.GLOBAL);
            if(AppearanceConfig.isWallpaperKey(e.getKey()) && !(sectionFallback&&(origin.startsWith("section:")||origin.startsWith("folder:"))))continue;
            out.values.put(e.getKey(),e.getValue());out.origins.put(e.getKey(),origin);
        }
        for(Map.Entry<String,String> e:source.values.entrySet()){
            String target=null,key=e.getKey();
            if(key.startsWith(domain+".wallpaper."))target=key.substring(domain.length()+1);
            if(key.startsWith("light."+domain+".chat_wallpaper"))target="light."+key.substring(7+domain.length());
            if(key.startsWith("dark."+domain+".chat_wallpaper"))target="dark."+key.substring(6+domain.length());
            if(target!=null){out.values.put(target,e.getValue());out.origins.put(target,source.origins.getOrDefault(key,AppearanceConfig.GLOBAL));}
        }
        return out;
    }
}
