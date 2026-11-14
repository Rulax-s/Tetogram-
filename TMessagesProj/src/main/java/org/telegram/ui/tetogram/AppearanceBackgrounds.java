/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.ui.tetogram;
import android.graphics.drawable.*;
import android.graphics.*;
import org.telegram.messenger.tetogram.AppearanceConfig;
import org.telegram.ui.Components.MotionBackgroundDrawable;
public final class AppearanceBackgrounds {
    private AppearanceBackgrounds(){}
    public static Drawable colors(AppearanceConfig.Resolved r,boolean dark){
        Integer first=r.color(dark,"chat_wallpaper");if(first==null)return null;
        if("solid".equals(r.get("wallpaper.type")))return new ColorDrawable(first);
        Integer second=r.color(dark,"chat_wallpaper_gradient_to1");
        if(second==null || second==0)return new ColorDrawable(first);
        Integer third=r.color(dark,"chat_wallpaper_gradient_to2"),fourth=r.color(dark,"chat_wallpaper_gradient_to3");
        int angle=Math.round(r.number("wallpaper.angle",45));
        if("true".equals(r.get("wallpaper.gradientMotion"))&&third!=null&&fourth!=null&&!"false".equals(r.get("wallpaper.animate"))&&SectionMenu.animations(org.telegram.messenger.ApplicationLoader.applicationContext))return new MotionBackgroundDrawable(first,second,third,fourth,angle,false);
        int[] stops=third==null?new int[]{first,second}:fourth==null?new int[]{first,second,third}:new int[]{first,second,third,fourth};
        return new Linear(stops,angle);
    }
    private static final class Linear extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int[] colors;private final int angle;
        Linear(int[] colors,int angle){this.colors=colors;this.angle=angle;}
        @Override protected void onBoundsChange(Rect b){double radians=Math.toRadians(angle);float dx=(float)Math.cos(radians),dy=(float)Math.sin(radians),radius=(Math.abs(b.width()*dx)+Math.abs(b.height()*dy))/2f;paint.setShader(new LinearGradient(b.exactCenterX()-dx*radius,b.exactCenterY()-dy*radius,b.exactCenterX()+dx*radius,b.exactCenterY()+dy*radius,colors,null,Shader.TileMode.CLAMP));}
        @Override public void draw(Canvas c){c.drawRect(getBounds(),paint);}
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
