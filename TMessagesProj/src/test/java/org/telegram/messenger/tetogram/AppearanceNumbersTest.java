package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceNumbersTest {
    @Test public void unitsMatchParameterMeaning(){
        assertEquals("16 dp",AppearanceNumbers.format("bubble.inRadius",16));assertEquals("2.5 dp",AppearanceNumbers.format("list.outlineWidth",2.5f));
        assertEquals("55 %",AppearanceNumbers.format("list.rowOpacity",.55f));assertEquals("30 %",AppearanceNumbers.format("wallpaper.dim",.3f));
        assertEquals("34 %",AppearanceNumbers.format("wallpaper.patternIntensity",34));assertEquals("1.25×",AppearanceNumbers.format("wallpaper.speed",1.25f));
        assertEquals("45°",AppearanceNumbers.format("wallpaper.angle",45));assertEquals("30 fps",AppearanceNumbers.format("wallpaper.fps",30));
        assertEquals("1920 px",AppearanceNumbers.format("wallpaper.quality",1920));assertEquals("-0.5",AppearanceNumbers.format("wallpaper.panX",-.5f));
        assertEquals("0",AppearanceNumbers.format("banner.avatarAlignment",-0.001f));assertEquals("2×",AppearanceNumbers.format("effects.durationScale",2));
        assertEquals("40 %",AppearanceNumbers.format("banner.parallax",.4f));
    }
    @Test public void sliderRoundTripsAndClamps(){
        String[] keys={"bubble.inRadius","list.rowOpacity","wallpaper.speed","wallpaper.angle","list.outlineWidth","wallpaper.panY","wallpaper.blur"};
        float[][] ranges={{0,32},{0,1},{.25f,2},{0,360},{0,4},{-1,1},{0,32}};
        for(int i=0;i<keys.length;i++){float min=ranges[i][0],max=ranges[i][1];int steps=AppearanceNumbers.steps(keys[i],min,max);
            assertEquals(keys[i],min,AppearanceNumbers.value(keys[i],min,max,0),1e-4);assertEquals(keys[i],max,AppearanceNumbers.value(keys[i],min,max,steps),1e-4);
            for(int p=0;p<=steps;p+=Math.max(1,steps/7))assertEquals(keys[i],p,AppearanceNumbers.progress(keys[i],min,max,AppearanceNumbers.value(keys[i],min,max,p)));
            assertEquals(0,AppearanceNumbers.progress(keys[i],min,max,min-100));assertEquals(steps,AppearanceNumbers.progress(keys[i],min,max,max+100));
            assertEquals(max,AppearanceNumbers.value(keys[i],min,max,steps+50),1e-4);}
    }
    @Test public void integerParametersSnapToWholeNumbers(){assertEquals(1f,AppearanceNumbers.step("bubble.inRadius",0,32),0);assertEquals(16f,AppearanceNumbers.value("bubble.inRadius",0,32,16),0);assertEquals(0.5f,AppearanceNumbers.step("bubble.inOutlineWidth",0,4),0);assertEquals(.01f,AppearanceNumbers.step("list.rowOpacity",0,1),0);}
    @Test public void outOfRangeStoredValueShowsAtEdge(){assertEquals(AppearanceNumbers.steps("list.radius",0,32),AppearanceNumbers.progress("list.radius",0,32,99));}
    @Test public void storedTextStaysShort(){for(int p=0;p<=100;p++){String text=Float.toString(AppearanceNumbers.value("list.rowOpacity",0,1,p));assertTrue(text,text.length()<=4);}assertEquals("0.3",Float.toString(AppearanceNumbers.value("list.rowOpacity",0,1,30)));assertEquals("1.25",Float.toString(AppearanceNumbers.value("wallpaper.speed",.25f,2,20)));}
    @Test public void exactFieldText(){assertEquals("0.55",AppearanceNumbers.trimmed(.55f));assertEquals("16",AppearanceNumbers.trimmed(16));}
}
