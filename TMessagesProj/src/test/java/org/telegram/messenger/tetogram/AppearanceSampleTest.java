package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceSampleTest {
 @Test public void letterboxIsNotAColor(){assertNull(AppearanceSample.pixel(20,20,400,400,200,100));assertArrayEquals(new int[]{100,50},AppearanceSample.pixel(200,200,400,400,200,100));}
 @Test public void portraitAndBoundaries(){assertNull(AppearanceSample.pixel(0,100,400,200,100,200));assertArrayEquals(new int[]{0,0},AppearanceSample.pixel(150,0,400,200,100,200));assertArrayEquals(new int[]{99,199},AppearanceSample.pixel(249.9f,199.9f,400,200,100,200));assertNull(AppearanceSample.pixel(250,200,400,200,100,200));}
 @Test public void invalidBoundsAreSafe(){assertNull(AppearanceSample.pixel(0,0,0,1,1,1));assertNull(AppearanceSample.pixel(Float.NaN,0,1,1,1,1));assertNull(AppearanceSample.pixel(Float.POSITIVE_INFINITY,0,1,1,1,1));}
}
