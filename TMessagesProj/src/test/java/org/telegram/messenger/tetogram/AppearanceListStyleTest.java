package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceListStyleTest {
 @Test public void nativeListDoesNotGainAnOverlay(){assertFalse(AppearanceListStyle.uniform(new AppearanceConfig.Resolved()));}
 @Test public void flatGlassUsesOneContinuousSurface(){AppearanceConfig.Resolved r=new AppearanceConfig.Resolved();r.values.put("list.rowOpacity","0.55");assertTrue(AppearanceListStyle.uniform(r));assertEquals(140,AppearanceListStyle.tint(0xff203040,.55f)>>>24);}
 @Test public void transparentRowsAndEmptySpaceHaveTheSameTint(){assertEquals(0x00203040,AppearanceListStyle.tint(0xff203040,0));}
 @Test public void cardsKeepIndependentFill(){AppearanceConfig.Resolved r=new AppearanceConfig.Resolved();r.values.put("list.rowOpacity","0.9");for(String key:new String[]{"list.radius","list.sideInset","list.rowGap"}){r.values.put(key,"8");assertFalse(AppearanceListStyle.uniform(r));r.values.remove(key);}}
 @Test public void individualChatOpacityIsNotMovedIntoGlobalBackground(){AppearanceConfig.Resolved r=new AppearanceConfig.Resolved();r.values.put("list.rowOpacity","0.5");r.origins.put("list.rowOpacity","chat:42");assertFalse(AppearanceListStyle.uniform(r));r.origins.put("list.rowOpacity","section:preview");assertTrue(AppearanceListStyle.uniform(r));}
}
