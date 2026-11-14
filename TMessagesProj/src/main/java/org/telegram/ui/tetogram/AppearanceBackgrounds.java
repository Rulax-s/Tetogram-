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
        float dim=Math.max(0,Math.min(1,r.number("wallpaper.dim",0)));first=dim(first,dim);
        if("solid".equals(r.get("wallpaper.type")))return finish(new ColorDrawable(first),r);
        Integer second=r.color(dark,"chat_wallpaper_gradient_to1");
        if(second==null || second==0)return finish(new ColorDrawable(first),r);
        Integer third=r.color(dark,"chat_wallpaper_gradient_to2"),fourth=r.color(dark,"chat_wallpaper_gradient_to3");
        second=dim(second,dim);if(third!=null)third=dim(third,dim);if(fourth!=null)fourth=dim(fourth,dim);
        int angle=Math.round(r.number("wallpaper.angle",45));
        if("true".equals(r.get("wallpaper.gradientMotion"))&&!"false".equals(r.get("wallpaper.animate"))&&SectionMenu.animations(org.telegram.messenger.ApplicationLoader.applicationContext))return finish(new Motion(first,second,third==null?first:third,fourth==null?second:fourth,angle),r);
        int[] stops=third==null?new int[]{first,second}:fourth==null?new int[]{first,second,third}:new int[]{first,second,third,fourth};
        return finish(new Linear(stops,angle),r);
    }
    private static int dim(int color,float amount){float multiplier=1-amount;return color&0xff000000|Math.round(((color>>>16)&255)*multiplier)<<16|Math.round(((color>>>8)&255)*multiplier)<<8|Math.round((color&255)*multiplier);}
    private static Drawable finish(Drawable drawable,AppearanceConfig.Resolved r){drawable.setAlpha(Math.round(255*Math.max(0,Math.min(1,r.number("wallpaper.opacity",1)))));return drawable;}
    private static final class Motion extends MotionBackgroundDrawable {
        Motion(int a,int b,int c,int d,int angle){super(a,b,c,d,angle,false);setIndeterminateAnimation(true);}
        @Override public void draw(Canvas canvas){setIndeterminateAnimation(SectionMenu.animations(org.telegram.messenger.ApplicationLoader.applicationContext)&&org.telegram.messenger.ApplicationLoader.isScreenOn&&!org.telegram.messenger.ApplicationLoader.mainInterfacePaused&&org.telegram.messenger.LiteMode.isEnabled(org.telegram.messenger.LiteMode.FLAG_CHAT_BACKGROUND));super.draw(canvas);}
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
