/* Tetogram, 2026. GNU GPL v2 or later. Pure lifecycle/resource policy. */
package org.telegram.messenger.tetogram;
public final class AppearanceMediaPolicy {
    private AppearanceMediaPolicy(){}
    public static final long MAX_SOURCE_BYTES=1024L*1024*1024;
    public static boolean play(boolean attached,boolean resumed,boolean visible,boolean screenOn,boolean foreground,boolean liteAllows,boolean batterySaver,boolean animations){return attached&&resumed&&visible&&screenOn&&foreground&&liteAllows&&!batterySaver&&animations;}
    public static boolean showFrame(boolean rendered,int videoWidth,int videoHeight,int surfaceWidth,int surfaceHeight){return rendered&&videoWidth>0&&videoHeight>0&&surfaceWidth>0&&surfaceHeight>0;}
    public static int fps(int detected,int requested){int limit=Math.max(12,Math.min(60,requested));return Math.min(detected>0?detected:30,limit);}
    /** MediaController swaps the encoded output axes before passing rotated videos to the native convertor. */
    public static boolean swapEncoderAxes(int rotation){int normalized=((rotation%360)+360)%360;return normalized==90||normalized==270;}
    public static int quality(int requested){return Math.max(360,Math.min(1920,requested));}
    public static int sample(int w,int h,int max){if(w<=0||h<=0||max<=0)throw new IllegalArgumentException("Invalid media bounds");int s=1;while(Math.max(w,h)/(long)s>max*2L && s<=1<<28)s*=2;return s;}
    public static int evenDimension(int input,int max,int longest){if(input<=0||max<2||longest<=0)throw new IllegalArgumentException("Invalid video bounds");return Math.max(2,((int)Math.floor(input*Math.min(1d,max/(double)longest)))&~1);}
    public static long endUs(long start,long end,long duration){if(start<0||duration<=0||start>=duration)throw new IllegalArgumentException("Invalid video interval");long result=end<0?duration:Math.min(end,duration);if(result<=start)throw new IllegalArgumentException("Empty video interval");return result;}
    public static boolean importDirectory(String name){if(name==null||!name.startsWith("import-"))return false;try{return java.util.UUID.fromString(name.substring(7)).toString().equalsIgnoreCase(name.substring(7));}catch(IllegalArgumentException e){return false;}}
    public static boolean discard(String file,java.util.Set<String> live,boolean pending){return AppearanceConfig.isMediaName(file)&&!pending&&!live.contains(file);}
    /**
     * Wallpaper output size. The crop editor reports a send-compressed size (often 720p) for muted videos; keep its
     * aspect ratio (it includes the crop) but scale back to the source resolution, limited by the quality setting.
     */
    public static int[] target(int resultWidth,int resultHeight,int originalWidth,int originalHeight,int requestedQuality){
        if(resultWidth<=0||resultHeight<=0){resultWidth=originalWidth;resultHeight=originalHeight;}
        if(resultWidth<=0||resultHeight<=0)throw new IllegalArgumentException("Invalid video bounds");
        int longest=Math.max(resultWidth,resultHeight),source=Math.max(originalWidth,originalHeight);
        int limit=Math.min(quality(requestedQuality),source>0?source:longest);
        double scale=limit/(double)longest;
        return new int[]{Math.max(2,((int)Math.round(resultWidth*scale))&~1),Math.max(2,((int)Math.round(resultHeight*scale))&~1)};
    }
    /** About 0.1 bit per pixel per frame (6 Mbit/s for 1080p30), never far above the source and within encoder limits. */
    public static int bitrate(int width,int height,int fps,int originalBitrate){
        long wanted=(long)width*height*Math.max(12,Math.min(60,fps))/10;
        if(originalBitrate>0)wanted=Math.min(wanted,Math.max(originalBitrate,1_500_000L));
        return (int)Math.max(1_500_000L,Math.min(16_000_000L,wanted));
    }
    /** Static cover size: large enough for a phone screen when the video is paused or Lite Mode is on. */
    public static final int COVER_SIZE=1280;
}
