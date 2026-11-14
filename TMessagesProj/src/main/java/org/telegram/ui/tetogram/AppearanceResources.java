/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.ui.tetogram;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.SparseIntArray;
import java.util.*;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.ActionBar.*;

/** Reusable isolated native resources for previews, rows and locally styled surfaces. */
public final class AppearanceResources implements Theme.ResourcesProvider {
    private AppearanceConfig.Resolved style;
    private final Theme.ResourcesProvider base;
    private boolean dark;
    private SparseIntArray colors;
    private final HashMap<String,Drawable> drawables=new HashMap<>();
    private final HashMap<String,Paint> paints=new HashMap<>();
    public AppearanceResources(AppearanceConfig.Resolved style,boolean dark,Theme.ResourcesProvider base){this.base=base;update(style,dark);}
    public void update(AppearanceConfig.Resolved style,boolean dark){this.style=style;this.dark=dark;colors=AppearanceRuntime.colors(style,dark);drawables.clear();paints.clear();}
    @Override public boolean isDark(){return dark;}
    @Override public int getColor(int key){int i=colors.indexOfKey(key);return i<0?(base==null?AppearanceRuntime.baseColor(dark,key):base.getColor(key)):colors.valueAt(i);}
    @Override public float getBubbleRadius(boolean outgoing){return Math.max(0,Math.min(32,style.number(outgoing?"bubble.outRadius":"bubble.inRadius",SharedConfig.bubbleRadius)));}
    @Override public Paint getPaint(String key){
        if(paints.containsKey(key))return paints.get(key);Paint original=base==null?Theme.getThemePaint(key):base.getPaint(key);if(original==null)original=Theme.getThemePaint(key);if(original==null)return null;
        Paint copy=original instanceof TextPaint?new TextPaint(original):new Paint(original);int colorKey=Theme.getThemePaintsMap().containsKey(key)?Theme.getThemePaintColorKey(key):-1;if(colorKey>=0)copy.setColor(getColor(colorKey));paints.put(key,copy);return copy;
    }
    @Override public Drawable getDrawable(String key){
        if(drawables.containsKey(key))return drawables.get(key);Drawable result;
        boolean out=key.equals(Theme.key_drawable_msgOut)||key.equals(Theme.key_drawable_msgOutSelected)||key.equals(Theme.key_drawable_msgOutMedia)||key.equals(Theme.key_drawable_msgOutMediaSelected);
        boolean in=key.equals(Theme.key_drawable_msgIn)||key.equals(Theme.key_drawable_msgInSelected)||key.equals(Theme.key_drawable_msgInMedia)||key.equals(Theme.key_drawable_msgInMediaSelected);
        if(out||in)result=new MessageDrawable(key.endsWith("Media")||key.contains("MediaSelected")?MessageDrawable.TYPE_MEDIA:MessageDrawable.TYPE_TEXT,out,key.endsWith("Selected"),this);
        else{Drawable original=base==null?Theme.getThemeDrawable(key):base.getDrawable(key);if(original==null)original=Theme.getThemeDrawable(key);if(original==null||original.getConstantState()==null)return null;result=original.getConstantState().newDrawable().mutate();int colorKey=Theme.getThemeDrawablesMap().containsKey(key)?Theme.getThemeDrawableColorKey(key):-1;if(colorKey>=0)Theme.setDrawableColor(result,getColor(colorKey));}
        drawables.put(key,result);return result;
    }
}
