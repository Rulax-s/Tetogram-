package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.telegram.messenger.tetogram.AppearanceIconPolicy.*;
public class AppearanceIconPolicyTest {
 @Test public void everyCalendarMonthHasCorrectSeason(){for(int m=1;m<=12;m++)assertEquals(m<=2||m==12?Season.WINTER:m<=5?Season.SPRING:m<=8?Season.SUMMER:Season.AUTUMN,season(m));}
 @Test public void automaticAndExplicitChoicesAreDeterministic(){assertEquals("MikuWinterIcon",alias("Mikugram",Season.AUTO,1));assertEquals("TetoAutumnIcon",alias("Tetogram",Season.AUTO,11));assertEquals("MikuSpringIcon",alias("Mikugram",Season.SPRING,12));}
 @Test public void missingArtIsNotPretendedToExist(){assertNull(alias("Tetogram",Season.SUMMER,7));assertFalse(available("other",Season.WINTER));assertNotNull(alias("Mikugram",Season.SUMMER,7));}
 @Test public void contentFitsDocumentedAdaptiveSafeZone(){assertTrue(SAFE_CONTENT_SCALE*72<=66);assertTrue(SAFE_CONTENT_SCALE*72>=48);assertEquals(SAFE_CONTENT_SCALE,scale(Float.NaN),0);assertEquals(.45f,scale(-1),0);assertEquals(1.1f,scale(10),0);}
}
