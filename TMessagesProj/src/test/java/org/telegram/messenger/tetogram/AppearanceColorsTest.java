package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceColorsTest {
    @Test public void hexUsesAndroidArgb(){assertEquals(0xffaabbcc,AppearanceColors.parse("#abc"));assertEquals(0x11223344,AppearanceColors.parse("#1234"));assertEquals(0x00123456,AppearanceColors.parse("#00123456"));assertEquals("#FF123456",AppearanceColors.hex(0xff123456));}
    @Test public void modelsAndAlpha(){assertEquals(0x80ff0000,AppearanceColors.parse("rgba(255,0,0,0.5)"));assertEquals(0xffff0000,AppearanceColors.parse("hsl(360,100,50)"));assertEquals(0xff00ff00,AppearanceColors.parse("hsv(120,100,100)"));assertEquals(0xff0000ff,AppearanceColors.parse("hsl(240,100,50)"));assertEquals(0xff808080,AppearanceColors.parse("hsl(10,0,50)"));}
    @Test public void rejectMalformedAndNonFinite(){for(String s:new String[]{"#zzzzzz","rgb(-1,0,0)","rgb(0,0,256)","hsv(0,101,0)","hsl(NaN,0,0)","rgba(0,0,0,2)","hsv(0,0,Infinity)","rgb(1,2)","other(0,0,0)"})try{AppearanceColors.parse(s);fail(s);}catch(IllegalArgumentException expected){}}
    @Test public void transparentTextCannotPassContrast(){assertEquals(1,AppearanceColors.visibleRatio(0x00ffffff,0xff000000,0xff000000),.001);assertEquals(21,AppearanceColors.visibleRatio(0xffffffff,0xff000000,0xff000000),.001);assertEquals(0xff808080,AppearanceColors.composite(0x80ffffff,0xff000000));}
    @Test public void conversionsArePortableStyleValues(){AppearanceConfig c=new AppearanceConfig();c.edit("global").color(true,"chat_outBubble",AppearanceColors.parse("hsl(240,100,50)"));assertEquals(Integer.valueOf(0xff0000ff),AppearanceConfig.decode(c.encode()).resolve(AppearanceConfig.Context.direct(0,0)).color(true,"chat_outBubble"));}
}
