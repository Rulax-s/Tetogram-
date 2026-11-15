/* Pure JVM tests: coordinate/draft contracts, not Android gestures, playback or screenshots. */
package org.telegram.messenger.tetogram;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppearanceViewportEditorTest {
    private static final float EPS = .001f;
    private AppearanceViewportEditor editor(float zoom, float x, float y) {
        AppearanceViewportEditor editor = new AppearanceViewportEditor(zoom, x, y);
        assertTrue(editor.geometry(300, 600, 1200, 800, false));
        return editor;
    }
    private AppearanceViewport view(AppearanceViewportEditor e, float w, float h, float sw, float sh, boolean fit) {
        AppearanceViewport v = new AppearanceViewport(); assertTrue(v.set(w, h, sw, sh, fit, e.zoom, e.panX, e.panY)); return v;
    }
    @Test public void oneFingerMovesExactlyInPixels() {
        AppearanceViewportEditor e = editor(2, 0, 0); AppearanceViewport before = view(e,300,600,1200,800,false);
        e.drag(35,-24); AppearanceViewport after = view(e,300,600,1200,800,false);
        assertEquals(before.x+35,after.x,EPS); assertEquals(before.y-24,after.y,EPS);
    }
    @Test public void fillClampsAtActualImageEdgesWithoutReleaseSnap() {
        AppearanceViewportEditor e=editor(2,0,0);e.drag(100000,-100000);AppearanceViewport v=view(e,300,600,1200,800,false);
        assertEquals(0,v.x,EPS);assertEquals(600-v.height,v.y,EPS);
        float x=e.panX,y=e.panY;e.drag(0,0);assertEquals(x,e.panX,EPS);assertEquals(y,e.panY,EPS);
    }
    @Test public void zoomKeepsSourcePointUnderFingers() {
        AppearanceViewportEditor e=editor(1.5f,.1f,-.1f);AppearanceViewport before=view(e,300,600,1200,800,false);
        float u=(110-before.x)/before.width,v=(270-before.y)/before.height;
        e.scale(1.3f,110,270,110,270);AppearanceViewport after=view(e,300,600,1200,800,false);
        assertEquals(u,(110-after.x)/after.width,EPS);assertEquals(v,(270-after.y)/after.height,EPS);
    }
    @Test public void movingPinchCentroidTranslatesAndZooms() {
        AppearanceViewportEditor e=editor(2,0,0);AppearanceViewport before=view(e,300,600,1200,800,false);
        e.scale(1.1f,120,240,150,260);AppearanceViewport after=view(e,300,600,1200,800,false);
        assertEquals((120-before.x)/before.width,(150-after.x)/after.width,EPS);
        assertEquals((240-before.y)/before.height,(260-after.y)/after.height,EPS);
    }
    @Test public void maximumAndMinimumZoomAreStable() {
        AppearanceViewportEditor e=editor(1,0,0);e.scale(100,150,300,150,300);assertEquals(4,e.zoom,EPS);
        e.scale(.001f,150,300,150,300);assertEquals(1,e.zoom,EPS);
    }
    @Test public void zeroTravelAxisCannotAcquirePhantomPan() {
        AppearanceViewportEditor e=editor(1,0,0);e.drag(0,100);assertEquals(0,e.panY,EPS);
    }
    @Test public void fitKeepsLetterboxAndUniformAspectRatio() {
        AppearanceViewportEditor e=new AppearanceViewportEditor(1,0,0);assertTrue(e.geometry(300,600,1200,800,true));
        e.drag(20,70);AppearanceViewport v=view(e,300,600,1200,800,true);
        assertEquals(1.5f,v.width/v.height,EPS);assertEquals(300,v.width,EPS);assertEquals(270,v.y,EPS);
    }
    @Test public void portraitPhotoAndRotatedVideoUseSameMath() {
        AppearanceViewportEditor e=new AppearanceViewportEditor(2,0,0);assertTrue(e.geometry(600,300,800,1200,false));
        e.drag(-20,30);AppearanceViewport v=view(e,600,300,800,1200,false);
        assertEquals(800f/1200,v.width/v.height,EPS);assertTrue(v.x<=0&&v.y<=0);
    }
    @Test public void resizeRetainsNormalizedValuesWithoutStretch() {
        AppearanceViewportEditor e=editor(2,.3f,-.2f);assertTrue(e.geometry(600,300,1200,800,false));
        assertEquals(2,e.zoom,EPS);assertEquals(.3f,e.panX,EPS);assertEquals(-.2f,e.panY,EPS);
        AppearanceViewport v=view(e,600,300,1200,800,false);assertEquals(1.5f,v.width/v.height,EPS);
    }
    @Test public void invalidSourceDoesNotMutateCoordinates() {
        AppearanceViewportEditor e=editor(2,.3f,-.2f);assertFalse(e.geometry(0,600,1200,800,false));
        assertFalse(e.geometry(300,600,Float.POSITIVE_INFINITY,800,false));assertEquals(.3f,e.panX,EPS);
    }
    @Test public void repeatedSmallGesturesMatchOneContinuousDrag() {
        AppearanceViewportEditor e=editor(2,0,0),other=editor(2,0,0);
        for(int i=0;i<100;i++)e.drag(.1f,.2f);other.drag(10,20);
        assertEquals(other.panX,e.panX,EPS);assertEquals(other.panY,e.panY,EPS);
    }
    @Test public void doubleTapAndResetHaveDeterministicDefaults() {
        AppearanceViewportEditor e=editor(3,.7f,-.8f);e.reset();assertEquals(1,e.zoom,EPS);assertEquals(0,e.panX,EPS);assertEquals(0,e.panY,EPS);
    }
    @Test public void corruptStoredControlsAreBounded() {
        AppearanceViewportEditor e=editor(Float.NaN,Float.POSITIVE_INFINITY,-9);assertEquals(1,e.zoom,EPS);assertEquals(0,e.panX,EPS);assertEquals(-1,e.panY,EPS);
    }
    @Test public void cancelledEditorCannotWriteIntoOriginalStyle() {
        AppearanceConfig.Style original=new AppearanceConfig.Style();original.set("wallpaper.zoom","2");
        AppearanceViewportEditor e=editor(2,0,0);e.drag(50,50);e.reset();assertEquals("2",original.values.get("wallpaper.zoom"));
    }
    @Test public void allThreeDomainsSurviveSerializationAndKeepMediaUnchanged() {
        AppearanceConfig c=new AppearanceConfig();AppearanceConfig.Style draft=c.edit("global");
        for(String prefix:new String[]{"wallpaper","list.wallpaper","banner.wallpaper"}){draft.set(prefix+".zoom","2.5");draft.set(prefix+".panX","0.4");draft.set(prefix+".panY","-0.2");}
        AppearanceConfig restored=AppearanceConfig.decode(c.encode());
        for(String prefix:new String[]{"wallpaper","list.wallpaper","banner.wallpaper"}){assertEquals("2.5",restored.edit("global").values.get(prefix+".zoom"));assertEquals("0.4",restored.edit("global").values.get(prefix+".panX"));assertEquals("-0.2",restored.edit("global").values.get(prefix+".panY"));}
        assertFalse(restored.edit("global").values.containsKey("wallpaper.file"));
    }
    @Test public void localPositionDoesNotChangeOtherChatsOrInheritedMedia() {
        AppearanceConfig c=new AppearanceConfig();c.edit("global").set("wallpaper.zoom","1.2");c.edit("chat:123").set("wallpaper.panX","0.5");
        assertEquals("0.5",c.resolve(AppearanceConfig.Context.direct(123,0)).get("wallpaper.panX"));
        assertNull(c.resolve(AppearanceConfig.Context.direct(456,0)).get("wallpaper.panX"));
        assertEquals("1.2",c.resolve(AppearanceConfig.Context.direct(123,0)).get("wallpaper.zoom"));
        AppearanceConfig other=new AppearanceConfig();assertNull(other.resolve(AppearanceConfig.Context.direct(123,0)).get("wallpaper.panX"));
    }
}
