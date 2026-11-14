package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceEffectsTest {
 @Test public void noAnimationAvoidsZeroDivisions(){assertEquals(1,AppearanceEffects.duration(200,1,false));assertEquals(200,AppearanceEffects.duration(200,1,true));assertEquals(400,AppearanceEffects.duration(200,99,true));assertEquals(200,AppearanceEffects.duration(200,Float.NaN,true));}
 @Test public void systemAndBatteryGatesAreRespected(){assertFalse(AppearanceEffects.allowed(true,false,false,false));assertFalse(AppearanceEffects.allowed(false,true,false,false));assertFalse(AppearanceEffects.allowed(true,true,true,true));assertTrue(AppearanceEffects.allowed(true,true,false,true));assertTrue(AppearanceEffects.allowed(true,true,true,false));}
}
