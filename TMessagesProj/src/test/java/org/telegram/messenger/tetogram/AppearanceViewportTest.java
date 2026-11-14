package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceViewportTest {
 @Test public void portraitAndLandscapeFillWithoutStretch(){AppearanceViewport r=new AppearanceViewport();assertTrue(r.set(100,200,400,200,false,1,0,0));assertEquals(400,r.width,.001);assertEquals(200,r.height,.001);assertEquals(-150,r.x,.001);assertTrue(r.set(200,100,100,200,false,1,0,0));assertEquals(400,r.height,.001);assertEquals(-150,r.y,.001);}
 @Test public void fitAndZoomApplyToImagesAndVideoCovers(){AppearanceViewport r=new AppearanceViewport();r.set(100,200,400,200,true,1,0,0);assertEquals(100,r.width,.001);assertEquals(50,r.height,.001);assertEquals(75,r.y,.001);r.set(100,200,400,200,true,2,1,-1);assertEquals(200,r.width,.001);assertEquals(0,r.x,.001);assertEquals(0,r.y,.001);}
 @Test public void cropPanningIsBoundedAtEdges(){AppearanceViewport r=new AppearanceViewport();r.set(100,100,200,100,false,1,99,0);assertEquals(0,r.x,.001);r.set(100,100,200,100,false,1,-99,0);assertEquals(-100,r.x,.001);}
 @Test public void invalidGeometryAndNonFiniteControlsAreSafe(){AppearanceViewport r=new AppearanceViewport();assertFalse(r.set(0,100,200,100,false,1,0,0));assertFalse(r.set(Float.POSITIVE_INFINITY,100,200,100,false,1,0,0));assertTrue(r.set(100,100,200,100,false,Float.NaN,Float.NaN,Float.POSITIVE_INFINITY));assertEquals(-50,r.x,.001);assertEquals(0,r.y,.001);}
}
