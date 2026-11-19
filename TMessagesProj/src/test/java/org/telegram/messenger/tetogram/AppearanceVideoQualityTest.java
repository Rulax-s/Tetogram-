package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceVideoQualityTest {
    @Test public void sendCompressedSizeIsScaledBackToSource(){assertArrayEquals(new int[]{1920,1080},AppearanceMediaPolicy.target(1280,720,1920,1080,1920));assertArrayEquals(new int[]{1080,1920},AppearanceMediaPolicy.target(720,1280,1080,1920,1920));}
    @Test public void cropAspectIsKept(){int[] s=AppearanceMediaPolicy.target(540,960,3840,2160,1920);assertEquals(1080,s[0]);assertEquals(1920,s[1]);int[] square=AppearanceMediaPolicy.target(640,640,1920,1080,1920);assertEquals(square[0],square[1]);assertEquals(1920,square[0]);}
    @Test public void qualitySettingStillLimits(){assertArrayEquals(new int[]{1280,720},AppearanceMediaPolicy.target(1280,720,3840,2160,1280));assertArrayEquals("quality never drops below 360",new int[]{360,202},AppearanceMediaPolicy.target(1920,1080,1920,1080,100));}
    @Test public void smallSourcesAreNotUpscaled(){assertArrayEquals(new int[]{640,360},AppearanceMediaPolicy.target(640,360,640,360,1920));assertArrayEquals(new int[]{854,480},AppearanceMediaPolicy.target(426,240,854,480,1920));}
    @Test public void dimensionsAreEvenAndMissingResultFallsBack(){int[] s=AppearanceMediaPolicy.target(0,0,1001,777,1920);assertEquals(0,s[0]%2);assertEquals(0,s[1]%2);assertEquals(1000,s[0]);int[] odd=AppearanceMediaPolicy.target(333,777,1080,1920,1920);assertEquals(0,odd[0]%2);assertEquals(0,odd[1]%2);}
    @Test(expected=IllegalArgumentException.class) public void noBoundsRejected(){AppearanceMediaPolicy.target(0,0,0,0,1920);}
    @Test public void bitrateScalesWithPixelsAndFpsWithinLimits(){int hd=AppearanceMediaPolicy.bitrate(1920,1080,30,0);assertEquals(6220800,hd);assertTrue(AppearanceMediaPolicy.bitrate(1920,1080,60,0)>hd);assertEquals(1_500_000,AppearanceMediaPolicy.bitrate(320,240,30,0));assertEquals(16_000_000,AppearanceMediaPolicy.bitrate(3840,2160,60,0));assertEquals(4_000_000,AppearanceMediaPolicy.bitrate(1920,1080,30,4_000_000));assertEquals(1_500_000,AppearanceMediaPolicy.bitrate(1920,1080,30,500_000));}
}
