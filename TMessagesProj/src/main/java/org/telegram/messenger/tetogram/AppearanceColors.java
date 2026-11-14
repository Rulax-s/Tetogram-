/* Tetogram, 2026. GNU GPL v2 or later. Pure color conversions; Android colors use ARGB. */
package org.telegram.messenger.tetogram;
import java.util.Locale;
public final class AppearanceColors {
    private AppearanceColors() {}
    public static int parse(String text) {
        String s=text.trim().toLowerCase(Locale.ROOT);
        if(s.startsWith("#")){
            String h=s.substring(1);
            if(h.length()==3||h.length()==4){StringBuilder b=new StringBuilder();for(char c:h.toCharArray())b.append(c).append(c);h=b.toString();}
            if(!h.matches("[0-9a-f]{6}|[0-9a-f]{8}"))throw new IllegalArgumentException("HEX");
            return (int)(Long.parseLong(h,16)|(h.length()==6?0xff000000L:0));
        }
        int open=s.indexOf('(');if(open<0||!s.endsWith(")"))throw new IllegalArgumentException("Color");
        String mode=s.substring(0,open),parts[]=s.substring(open+1,s.length()-1).split(",",-1);
        if(parts.length!=3&&parts.length!=4)throw new IllegalArgumentException("Components");
        double[] v=new double[parts.length];for(int i=0;i<v.length;i++){v[i]=Double.parseDouble(parts[i].trim());if(Double.isNaN(v[i])||Double.isInfinite(v[i]))throw new IllegalArgumentException("Finite");}
        int alpha=parts.length==4?(int)Math.round(bound(v[3],0,1)*255):255;
        if(mode.equals("rgb")||mode.equals("rgba"))return alpha<<24|channel(v[0])<<16|channel(v[1])<<8|channel(v[2]);
        if(!mode.equals("hsv")&&!mode.equals("hsl"))throw new IllegalArgumentException("Model");
        double h=bound(v[0],0,360)%360, saturation=bound(v[1],0,100)/100,third=bound(v[2],0,100)/100;
        double chroma=mode.equals("hsv")?third*saturation:(1-Math.abs(2*third-1))*saturation;
        double x=chroma*(1-Math.abs((h/60)%2-1)),m=mode.equals("hsv")?third-chroma:third-chroma/2;
        double r=0,g=0,b=0;
        if(h<60){r=chroma;g=x;}else if(h<120){r=x;g=chroma;}else if(h<180){g=chroma;b=x;}else if(h<240){g=x;b=chroma;}else if(h<300){r=x;b=chroma;}else{r=chroma;b=x;}
        return alpha<<24|(int)Math.round((r+m)*255)<<16|(int)Math.round((g+m)*255)<<8|(int)Math.round((b+m)*255);
    }
    private static int channel(double n){return (int)Math.round(bound(n,0,255));}
    private static double bound(double n,double min,double max){if(n<min||n>max)throw new IllegalArgumentException("Range");return n;}
    public static String hex(int color){return String.format(Locale.ROOT,"#%08X",color);}
    public static int composite(int foreground,int background){double a=(foreground>>>24)/255.;int out=0xff000000;for(int shift:new int[]{0,8,16})out|=(int)Math.round(((foreground>>>shift)&255)*a+((background>>>shift)&255)*(1-a))<<shift;return out;}
    public static double visibleRatio(int text,int background,int canvas){return AppearancePresets.ratio(composite(text,composite(background,canvas)),composite(background,canvas));}
    public static String description(int color){double r=((color>>>16)&255)/255.,g=((color>>>8)&255)/255.,b=(color&255)/255.,max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),d=max-min,h=d==0?0:max==r?60*(((g-b)/d)%6):max==g?60*((b-r)/d+2):60*((r-g)/d+4);if(h<0)h+=360;double l=(max+min)/2,sHsl=d==0?0:d/(1-Math.abs(2*l-1));return String.format(Locale.ROOT,"%s\nRGB(%d, %d, %d, %.3f)\nHSV(%.1f, %.1f, %.1f)\nHSL(%.1f, %.1f, %.1f)",hex(color),(color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)/255.,h,max==0?0:d/max*100,max*100,h,sHsl*100,l*100);}
}
