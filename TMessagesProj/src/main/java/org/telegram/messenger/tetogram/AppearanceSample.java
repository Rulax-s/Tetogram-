/* Tetogram, 2026. GNU GPL v2 or later. Fit-center pixel selection, without sampling letterbox space. */
package org.telegram.messenger.tetogram;
public final class AppearanceSample {
    private AppearanceSample(){}
    public static int[] pixel(float x,float y,int viewWidth,int viewHeight,int imageWidth,int imageHeight){if(viewWidth<=0||viewHeight<=0||imageWidth<=0||imageHeight<=0||Float.isNaN(x)||Float.isNaN(y)||Float.isInfinite(x)||Float.isInfinite(y))return null;float scale=Math.min(viewWidth/(float)imageWidth,viewHeight/(float)imageHeight),left=(viewWidth-imageWidth*scale)/2,top=(viewHeight-imageHeight*scale)/2;if(x<left||y<top||x>=left+imageWidth*scale||y>=top+imageHeight*scale)return null;return new int[]{Math.max(0,Math.min(imageWidth-1,(int)((x-left)/scale))),Math.max(0,Math.min(imageHeight-1,(int)((y-top)/scale)))};}
}
