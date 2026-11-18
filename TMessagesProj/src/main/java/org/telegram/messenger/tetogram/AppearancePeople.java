/* Tetogram, 2026. GNU GPL v2 or later. Per-person bubble fill and outline, keyed by sender ID. */
package org.telegram.messenger.tetogram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Keys live in a normal appearance scope: global for "in every chat", chat:&lt;id&gt; for one group, so the usual
 * inheritance decides. They are personal data (sender IDs): never in presets or exports.
 * person.u&lt;user id&gt;.bubble / .outline (ARGB) and .outlineWidth (dp); channels and groups as senders use c&lt;id&gt;.
 */
public final class AppearancePeople {
    public static final String PREFIX="person.";
    public static final float DEFAULT_OUTLINE=2f,MAX_OUTLINE=4f;
    private AppearancePeople(){}

    public static final class Style {
        public final Integer fill,outline;public final float width;
        Style(Integer fill,Integer outline,float width){this.fill=fill;this.outline=outline;this.width=width;}
        public boolean hasOutline(){return outline!=null&&width>0;}
    }

    public static String token(long id){if(id==0)throw new IllegalArgumentException("No sender");return id>0?"u"+id:"c"+(-id);}
    public static long id(String token){if(token==null||token.length()<2)return 0;try{long v=Long.parseLong(token.substring(1));if(v<=0)return 0;return token.charAt(0)=='u'?v:token.charAt(0)=='c'?-v:0;}catch(NumberFormatException e){return 0;}}
    public static String key(long id,String part){return PREFIX+token(id)+"."+part;}

    /** Reads a person's style through any lookup (resolved style, resources provider); null when nothing is set. */
    public static Style read(Function<String,String> lookup,long id){
        if(id==0||lookup==null)return null;
        Integer fill=color(lookup.apply(key(id,"bubble"))),outline=color(lookup.apply(key(id,"outline")));
        if(fill==null&&outline==null)return null;
        float width=0;
        if(outline!=null){width=DEFAULT_OUTLINE;String w=lookup.apply(key(id,"outlineWidth"));if(w!=null)try{float v=Float.parseFloat(w);if(!Float.isNaN(v)&&!Float.isInfinite(v))width=Math.max(0,Math.min(MAX_OUTLINE,v));}catch(NumberFormatException ignored){}}
        return new Style(fill,outline,width);
    }

    public static void write(AppearanceConfig.Style target,long id,Integer fill,Integer outline,float width){
        clear(target,id);
        if(fill!=null)target.set(key(id,"bubble"),Integer.toString(fill));
        if(outline!=null){target.set(key(id,"outline"),Integer.toString(outline));target.set(key(id,"outlineWidth"),Float.toString(Math.max(.5f,Math.min(MAX_OUTLINE,width))));}
    }
    public static void clear(AppearanceConfig.Style target,long id){String prefix=PREFIX+token(id)+".";target.values.keySet().removeIf(k->k.startsWith(prefix));}

    /** People that have a style in this scope, in insertion order. */
    public static List<Long> people(AppearanceConfig.Style style){
        ArrayList<Long> out=new ArrayList<>();if(style==null)return out;
        for(Map.Entry<String,String> e:style.values.entrySet()){String k=e.getKey();if(!k.startsWith(PREFIX))continue;int dot=k.indexOf('.',PREFIX.length());if(dot<0)continue;long id=id(k.substring(PREFIX.length(),dot));if(id!=0&&!out.contains(id))out.add(id);}
        return out;
    }

    /** Text drawn on a custom fill: keep the theme color when readable, otherwise black or white. */
    public static int text(int fill,int themeText,int canvas){
        int bubble=AppearanceColors.composite(fill,canvas|0xff000000);
        return AppearancePresets.ratio(AppearanceColors.composite(themeText,bubble),bubble)>=4.5?themeText:AppearancePresets.contrast(bubble);
    }

    private static Integer color(String value){if(value==null)return null;try{return Integer.valueOf(value);}catch(NumberFormatException e){return null;}}
}
