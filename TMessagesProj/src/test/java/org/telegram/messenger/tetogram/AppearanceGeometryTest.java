package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceGeometryTest {
 @Test public void defaultCacheRemainsNative(){assertEquals(50,AppearanceGeometry.cacheWidth(16),0);assertEquals(40,AppearanceGeometry.cacheHeight(16),0);}
 @Test public void largeCurvesFitWithoutClippingNinePatchEdges(){for(float r=0;r<=32;r+=.5f){assertTrue(AppearanceGeometry.cacheWidth(r)>=2*r+14);assertTrue(AppearanceGeometry.cacheHeight(r)>=2*r+8);}}
 @Test public void invalidInputsDoNotAllocateUnboundedBitmaps(){assertEquals(40,AppearanceGeometry.cacheHeight(Float.NaN),0);assertEquals(72,AppearanceGeometry.cacheHeight(Float.MAX_VALUE),0);assertEquals(50,AppearanceGeometry.cacheWidth(-999),0);}
 @Test public void shapeSettingsRoundTripWithoutSharingChatOverrides(){AppearanceConfig c=new AppearanceConfig();c.edit("global").set("bubble.outTail","false");c.edit("chat:10").set("bubble.outTopLeft","32");c.edit("chat:10").set("bubble.outOutlineWidth","2");c.edit("chat:10").color(true,"bubble.outOutline",0xff112233);AppearanceConfig restored=AppearanceConfig.decode(c.encode());AppearanceConfig.Resolved a=restored.resolve(AppearanceConfig.Context.direct(10,0)),b=restored.resolve(AppearanceConfig.Context.direct(11,0));assertEquals("false",a.get("bubble.outTail"));assertEquals(32,a.number("bubble.outTopLeft",0),0);assertEquals(Integer.valueOf(0xff112233),a.color(true,"bubble.outOutline"));assertNull(b.get("bubble.outTopLeft"));assertEquals(16,b.number("bubble.outTopLeft",16),0);}
}
