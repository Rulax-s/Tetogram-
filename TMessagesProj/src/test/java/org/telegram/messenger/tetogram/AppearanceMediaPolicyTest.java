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
 @org.junit.Test public void firstFrameRequiresMetadataAndLaidOutSurface(){org.junit.Assert.assertFalse(AppearanceMediaPolicy.showFrame(false,1920,1080,400,800));org.junit.Assert.assertFalse(AppearanceMediaPolicy.showFrame(true,0,0,400,800));org.junit.Assert.assertFalse(AppearanceMediaPolicy.showFrame(true,1920,1080,0,0));org.junit.Assert.assertTrue(AppearanceMediaPolicy.showFrame(true,1920,1080,400,800));}
 @org.junit.Test public void encodingCapsDoNotTrustImportedStyleRanges(){org.junit.Assert.assertEquals(30,AppearanceMediaPolicy.fps(0,60));org.junit.Assert.assertEquals(24,AppearanceMediaPolicy.fps(0,24));org.junit.Assert.assertEquals(60,AppearanceMediaPolicy.fps(240,Integer.MAX_VALUE));org.junit.Assert.assertEquals(5,AppearanceMediaPolicy.fps(5,30));org.junit.Assert.assertEquals(12,AppearanceMediaPolicy.fps(30,-99));org.junit.Assert.assertEquals(1920,AppearanceMediaPolicy.quality(Integer.MAX_VALUE));org.junit.Assert.assertEquals(360,AppearanceMediaPolicy.quality(-1));}
 @org.junit.Test public void abandonedImportCleanupNeverTargetsForeignDirectories(){org.junit.Assert.assertTrue(AppearanceMediaPolicy.importDirectory("import-"+java.util.UUID.randomUUID()));for(String dir:new String[]{"sessions","media","tetogram_sections_v1_u1","import-../sessions","import-invalid","import-000000000000000000000000000000000000"})org.junit.Assert.assertFalse(AppearanceMediaPolicy.importDirectory(dir));}
    @org.junit.Test public void rotatedEncoderAxesMatchNativeMediaController(){
        for(int rotation:new int[]{90,270,-90,450})org.junit.Assert.assertTrue(AppearanceMediaPolicy.swapEncoderAxes(rotation));
        for(int rotation:new int[]{0,180,360,-180})org.junit.Assert.assertFalse(AppearanceMediaPolicy.swapEncoderAxes(rotation));
        int width=AppearanceMediaPolicy.evenDimension(1920,1280,1920),height=AppearanceMediaPolicy.evenDimension(1080,1280,1920);
        org.junit.Assert.assertEquals(1280,width);org.junit.Assert.assertEquals(720,height);
        org.junit.Assert.assertEquals(720,AppearanceMediaPolicy.swapEncoderAxes(90)?height:width);
        org.junit.Assert.assertEquals(1280,AppearanceMediaPolicy.swapEncoderAxes(90)?width:height);
    }
}
