package org.telegram.messenger.tetogram;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceWallpaperTest {
 @Test public void firstGradientStopMaterializesTheBaseColor(){AppearanceConfig c=new AppearanceConfig();AppearanceConfig.Style s=c.edit("global");AppearanceWallpaper.color(s,c.resolve(AppearanceConfig.Context.direct(0,0)),false,"chat_wallpaper_gradient_to1",2,1);AppearanceConfig.Resolved r=c.resolve(AppearanceConfig.Context.direct(0,0));assertEquals(Integer.valueOf(1),r.color(false,"chat_wallpaper"));assertEquals(Integer.valueOf(2),r.color(false,"chat_wallpaper_gradient_to1"));assertEquals("gradient",r.get("wallpaper.type"));}
 @Test public void colorReplacementReleasesAllObsoleteMediaReferences(){AppearanceConfig c=new AppearanceConfig();AppearanceConfig.Style s=c.edit("global");s.set("list.wallpaper.file",UUID.randomUUID()+".mp4");s.set("list.wallpaper.source.file",UUID.randomUUID()+".mp4");s.set("list.wallpaper.cover.file",UUID.randomUUID()+".jpg");AppearanceWallpaper.color(s,c.resolve(AppearanceConfig.Context.direct(0,0)),true,"list.chat_wallpaper",123,1);assertTrue(c.mediaReferences().isEmpty());assertEquals("solid",s.values.get("list.wallpaper.type"));assertNull(c.resolve(AppearanceConfig.Context.direct(0,0)).color(true,"chat_wallpaper"));}
 @Test public void inheritedBaseAndTransparentBlackArePreserved(){AppearanceConfig c=new AppearanceConfig();c.edit("global").color(false,"banner.chat_wallpaper",0);AppearanceConfig.Style s=c.edit("profile:42");AppearanceWallpaper.color(s,c.resolveProfile(42),false,"banner.chat_wallpaper_gradient_to1",456,123);assertNull(s.values.get("light.banner.chat_wallpaper"));assertEquals(Integer.valueOf(0),c.resolveProfile(42).color(false,"banner.chat_wallpaper"));}
}
