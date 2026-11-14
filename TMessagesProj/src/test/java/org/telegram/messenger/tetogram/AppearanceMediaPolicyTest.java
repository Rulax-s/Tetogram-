package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class AppearanceMediaPolicyTest {
    @Test public void onlyVisibleForegroundSurfacePlays(){assertTrue(AppearanceMediaPolicy.play(true,true,true,true,true,true,false,true));for(int missing=0;missing<7;missing++){boolean[] a={true,true,true,true,true,true,true};a[missing]=false;assertFalse(AppearanceMediaPolicy.play(a[0],a[1],a[2],a[3],a[4],a[5],false,a[6]));}}
    @Test public void batterySaverAlwaysStopsVideo(){assertFalse(AppearanceMediaPolicy.play(true,true,true,true,true,true,true,true));}
    @Test public void largePortraitAndLandscapeUseBoundedDecode(){assertEquals(8,AppearanceMediaPolicy.sample(32000,16000,2048));assertEquals(8,AppearanceMediaPolicy.sample(16000,32000,2048));}
    @Test public void videoDimensionsStayEvenAndDoNotUpscale(){assertEquals(720,AppearanceMediaPolicy.evenDimension(1920,720,1920));assertEquals(404,AppearanceMediaPolicy.evenDimension(1080,720,1920));assertEquals(640,AppearanceMediaPolicy.evenDimension(640,1080,640));}
    @Test public void selectedRangeIsBoundedBySource(){assertEquals(1000,AppearanceMediaPolicy.endUs(200,-1,1000));assertEquals(900,AppearanceMediaPolicy.endUs(200,900,1000));assertEquals(1000,AppearanceMediaPolicy.endUs(200,5000,1000));}
    @Test(expected=IllegalArgumentException.class)public void rejectsEmptyRange(){AppearanceMediaPolicy.endUs(900,300,1000);}
    @Test public void cleanupProtectsSharedPresetReferencesAndPendingDrafts(){String f=UUID.randomUUID()+".mp4";assertFalse(AppearanceMediaPolicy.discard(f,Collections.singleton(f),false));assertFalse(AppearanceMediaPolicy.discard(f,Collections.emptySet(),true));assertTrue(AppearanceMediaPolicy.discard(f,Collections.emptySet(),false));}
    @Test public void cleanupDoesNotTouchForeignFiles(){assertFalse(AppearanceMediaPolicy.discard("session.dat",Collections.emptySet(),false));assertFalse(AppearanceMediaPolicy.discard("../private.jpg",Collections.emptySet(),false));}
}
