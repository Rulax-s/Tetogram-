package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceAccentTest {
 @Test public void extremeAccentsKeepCoreTextReadable(){for(int accent:new int[]{0x00ffffff,0xff000000,0xff777777,0xffff0000,0xff00ff00,0xff0000ff})for(boolean dark:new boolean[]{false,true}){AppearanceConfig c=new AppearanceConfig();AppearanceAccent.apply(c.edit("global"),dark,accent,"");AppearanceConfig.Resolved r=c.resolve(AppearanceConfig.Context.direct(0,0));assertTrue(AppearancePresets.ratio(r.color(dark,"chat_outBubble"),r.color(dark,"chat_messageTextOut"))>=4.5);assertTrue(AppearancePresets.ratio(r.color(dark,"windowBackgroundWhite"),r.color(dark,"glass_tabSelectedText"))>=4.49);}}
 @Test public void listOnlyPaletteDoesNotOverwriteChatColorsOrMedia(){AppearanceConfig c=new AppearanceConfig();AppearanceConfig.Style s=c.edit("chat:5");s.color(false,"chat_outBubble",1);s.set("wallpaper.file","kept");AppearanceAccent.apply(s,false,0xff00ff00,"list.");assertEquals(Integer.valueOf(1),c.resolve(AppearanceConfig.Context.direct(5,0)).color(false,"chat_outBubble"));assertEquals("kept",s.values.get("wallpaper.file"));assertNotNull(AppearanceSurfaces.list(c.resolve(AppearanceConfig.Context.direct(5,0))).color(false,"chats_name"));}
}
