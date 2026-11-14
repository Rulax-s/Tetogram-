/* Tetogram, 2026. GNU GPL v2 or later. Pure lifecycle/resource policy. */
package org.telegram.messenger.tetogram;
public final class AppearanceMediaPolicy {
    private AppearanceMediaPolicy(){}
    public static final long MAX_SOURCE_BYTES=1024L*1024*1024;
    public static boolean play(boolean attached,boolean resumed,boolean visible,boolean screenOn,boolean foreground,boolean liteAllows,boolean batterySaver,boolean animations){return attached&&resumed&&visible&&screenOn&&foreground&&liteAllows&&!batterySaver&&animations;}
    public static int sample(int w,int h,int max){if(w<=0||h<=0||max<=0)throw new IllegalArgumentException("Invalid media bounds");int s=1;while(Math.max(w,h)/(long)s>max*2L && s<=1<<28)s*=2;return s;}
    public static int evenDimension(int input,int max,int longest){if(input<=0||max<2||longest<=0)throw new IllegalArgumentException("Invalid video bounds");return Math.max(2,((int)Math.floor(input*Math.min(1d,max/(double)longest)))&~1);}
    public static long endUs(long start,long end,long duration){if(start<0||duration<=0||start>=duration)throw new IllegalArgumentException("Invalid video interval");long result=end<0?duration:Math.min(end,duration);if(result<=start)throw new IllegalArgumentException("Empty video interval");return result;}
    public static boolean discard(String file,java.util.Set<String> live,boolean pending){return AppearanceConfig.isMediaName(file)&&!pending&&!live.contains(file);}
}
