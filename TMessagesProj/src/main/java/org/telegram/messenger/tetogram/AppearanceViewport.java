/* Tetogram, 2026. GNU GPL v2 or later. Shared image/video geometry, no Android dependency. */
package org.telegram.messenger.tetogram;
public final class AppearanceViewport {
    public float x,y,width,height;
    private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
    private static float bound(float value,float fallback,float min,float max){return Float.isNaN(value)||Float.isInfinite(value)?fallback:Math.max(min,Math.min(max,value));}
    public boolean set(float viewportWidth,float viewportHeight,float sourceWidth,float sourceHeight,boolean fit,float zoom,float panX,float panY){
        if(!(viewportWidth>0&&viewportHeight>0&&sourceWidth>0&&sourceHeight>0)||!finite(viewportWidth)||!finite(viewportHeight)||!finite(sourceWidth)||!finite(sourceHeight))return false;
        float scale=fit?Math.min(viewportWidth/sourceWidth,viewportHeight/sourceHeight):Math.max(viewportWidth/sourceWidth,viewportHeight/sourceHeight);
        scale*=bound(zoom,1,1,4);width=sourceWidth*scale;height=sourceHeight*scale;
        x=(viewportWidth-width)/2+bound(panX,0,-1,1)*Math.abs(viewportWidth-width)/2;
        y=(viewportHeight-height)/2+bound(panY,0,-1,1)*Math.abs(viewportHeight-height)/2;
        return true;
    }
}
