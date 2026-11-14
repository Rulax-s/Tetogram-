package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceProfileGeometryTest {
 @Test public void defaultAndCollapsedEndpointsStayNative(){assertEquals(132,AppearanceProfileGeometry.horizontal(360,96,16,0,1),.001);assertEquals(132,AppearanceProfileGeometry.horizontal(360,96,16,1,0),.001);}
 @Test public void avatarAlignmentStaysInsideSafeHorizontalMargins(){assertEquals(16,AppearanceProfileGeometry.horizontal(360,96,16,-1,1),.001);assertEquals(248,AppearanceProfileGeometry.horizontal(360,96,16,1,1),.001);}
 @Test public void expandedPhotoDoesNotReceiveAnExtraOffset(){assertEquals(0,AppearanceProfileGeometry.horizontal(360,360,16,1,1),.001);assertEquals(-20,AppearanceProfileGeometry.horizontal(360,400,16,-1,1),.001);}
 @Test public void recomputationDoesNotAccumulateOffsetsAndInvalidControlsFallBack(){float p=AppearanceProfileGeometry.horizontal(360,96,16,.6f,.75f);for(int i=0;i<50;i++)assertEquals(p,AppearanceProfileGeometry.horizontal(360,96,16,.6f,.75f),0);assertEquals(132,AppearanceProfileGeometry.horizontal(360,96,16,Float.NaN,1),.001);}
}
