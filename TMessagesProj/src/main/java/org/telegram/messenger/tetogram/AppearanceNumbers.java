/* Tetogram, 2026. GNU GPL v2 or later. Units, steps and labels for numeric appearance parameters. */
package org.telegram.messenger.tetogram;

import java.util.Locale;

public final class AppearanceNumbers {
    public enum Unit { DP, FRACTION_PERCENT, PERCENT, TIMES, DEGREES, FPS, PIXELS, PLAIN }
    private AppearanceNumbers(){}

    public static Unit unit(String key){
        String name=key.substring(key.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        if(name.endsWith("opacity")||name.equals("dim")||key.equals("banner.parallax")||key.equals("launcher.iconScale"))return Unit.FRACTION_PERCENT;
        if(name.equals("patternintensity"))return Unit.PERCENT;
        if(name.equals("zoom")||name.equals("speed")||name.equals("durationscale"))return Unit.TIMES;
        if(name.equals("angle"))return Unit.DEGREES;
        if(name.equals("fps"))return Unit.FPS;
        if(name.equals("quality"))return Unit.PIXELS;
        if(name.equals("panx")||name.equals("pany")||name.equals("avataralignment"))return Unit.PLAIN;
        if(name.equals("blur"))return Unit.PLAIN;
        return Unit.DP;
    }

    /** Slider resolution; integer-valued parameters snap to whole numbers. */
    public static float step(String key,float min,float max){
        Unit unit=unit(key);
        if(unit==Unit.FRACTION_PERCENT||unit==Unit.PLAIN&&max-min<=2)return .01f;
        if(unit==Unit.TIMES)return .05f;
        if(key.endsWith("OutlineWidth")||key.endsWith("outlineWidth"))return .5f;
        if(max-min<=2)return .01f;
        return 1f;
    }

    public static int steps(String key,float min,float max){return Math.max(1,Math.round((max-min)/step(key,min,max)));}
    public static float value(String key,float min,float max,int progress){float s=step(key,min,max);int p=Math.max(0,Math.min(steps(key,min,max),progress));if(p==steps(key,min,max))return max;float v=min+p*s;return clamp(round(v,s),min,max);}
    public static int progress(String key,float min,float max,float value){return Math.max(0,Math.min(steps(key,min,max),Math.round((clamp(value,min,max)-min)/step(key,min,max))));}

    public static String format(String key,float value){
        switch(unit(key)){
            case FRACTION_PERCENT:return Math.round(value*100)+" %";
            case PERCENT:return Math.round(value)+" %";
            case TIMES:return trim(value,2)+"×";
            case DEGREES:return Math.round(value)+"°";
            case FPS:return Math.round(value)+" fps";
            case PIXELS:return Math.round(value)+" px";
            case PLAIN:return trim(value,2);
            default:return trim(value,1)+" dp";
        }
    }

    /** Plain editable text for the exact-value field. */
    public static String trimmed(float value){return trim(value,3);}
    static String trim(float value,int decimals){
        String text=String.format(Locale.ROOT,"%."+decimals+"f",value);
        if(text.indexOf('.')>=0){text=text.replaceAll("0+$","");if(text.endsWith("."))text=text.substring(0,text.length()-1);}
        return text.equals("-0")?"0":text;
    }
    // Divide by the whole number of steps per unit so stored text stays short (0.3, not 0.29999998).
    private static float round(float v,float step){if(step>=1)return Math.round(v);int perUnit=Math.round(1/step);return Math.round(v*perUnit)/(float)perUnit;}
    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
}
