package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceGeometryTest {
 @Test public void defaultCacheRemainsNative(){assertEquals(50,AppearanceGeometry.cacheWidth(16),0);assertEquals(40,AppearanceGeometry.cacheHeight(16),0);}
 @Test public void largeCurvesFitWithoutClippingNinePatchEdges(){for(float r=0;r<=32;r+=.5f){assertTrue(AppearanceGeometry.cacheWidth(r)>=2*r+14);assertTrue(AppearanceGeometry.cacheHeight(r)>=2*r+8);}}
 @Test public void invalidInputsDoNotAllocateUnboundedBitmaps(){assertEquals(40,AppearanceGeometry.cacheHeight(Float.NaN),0);assertEquals(72,AppearanceGeometry.cacheHeight(Float.MAX_VALUE),0);assertEquals(50,AppearanceGeometry.cacheWidth(-999),0);}
}
