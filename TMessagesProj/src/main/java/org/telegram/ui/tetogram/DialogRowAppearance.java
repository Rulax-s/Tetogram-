/* Tetogram, 2026. GNU GPL v2 or later. Native row geometry, no copied dialogs/messages. */
package org.telegram.ui.tetogram;
import android.graphics.*;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.ImageLocation;
import java.io.File;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.ActionBar.Theme;

public final class DialogRowAppearance implements Theme.ResourcesProvider {
    private final Theme.ResourcesProvider base;
    private final int account;
    private long owner;
    private final ImageReceiver image;
    private boolean hasImage;
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);
    private AppearanceConfig.Resolved style=new AppearanceConfig.Resolved();
    private AppearanceResources resources;
    private boolean dark;
    private final Path clip=new Path();
    private final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
    // Parsed once per configuration instead of on every frame of every row.
    private boolean uniform,separators=true,hasFill;
    private float rowOpacity=1,radius,outline,wallOpacity=1,wallDim,avatarSize=Float.NaN;
    private int sideInset,rowGap;
    private Integer outlineColor;
    private final RectF rect=new RectF();
    private PaintScope scope;
    public DialogRowAppearance(Theme.ResourcesProvider base,View parent,int account){this.base=base;this.account=account;image=new ImageReceiver(parent);image.setCurrentAccount(account);image.setAllowLoadingOnAttachedOnly(true);}
    public boolean configure(AppearanceConfig.Resolved style){long uid=AppearanceStore.owner(account);if(this.style.values.equals(style.values)&&this.style.origins.equals(style.origins)&&owner==uid)return false;owner=uid;this.style=style;refresh();String origin=style.origins.get("list.wallpaper.file");String name="video".equals(style.get("list.wallpaper.type"))?style.get("list.wallpaper.cover.file"):style.get("list.wallpaper.file");hasImage=uid>0&&origin!=null&&(origin.startsWith("chat:")||origin.startsWith("topic:"))&&AppearanceConfig.isMediaName(name);if(hasImage){File file=AppearanceMedia.file(uid,name);image.setImage(ImageLocation.getForPath(file.getAbsolutePath()),style.number("list.wallpaper.blur",0)>0?"320_80_b":"640_160",null,null,null,0);}else image.setImageBitmap((android.graphics.drawable.Drawable)null);return true;}
    private void parse(){
        uniform=AppearanceListStyle.uniform(style);separators=!"false".equals(style.get("list.separators"));
        rowOpacity=Math.max(0,Math.min(1,style.number("list.rowOpacity",1)));
        radius=AndroidUtilities.dpf2(Math.max(0,Math.min(32,style.number("list.radius",0))));
        outline=AndroidUtilities.dpf2(Math.max(0,Math.min(4,style.number("list.outlineWidth",0))));
        wallOpacity=Math.max(0,Math.min(1,style.number("list.wallpaper.opacity",1)));wallDim=Math.max(0,Math.min(1,style.number("list.wallpaper.dim",0)));
        sideInset=AndroidUtilities.dp(Math.max(0,Math.min(24,style.number("list.sideInset",0))));rowGap=AndroidUtilities.dp(Math.max(0,Math.min(20,style.number("list.rowGap",0)))/2);
        avatarSize=style.values.containsKey("list.avatarSize")?style.number("list.avatarSize",Float.NaN):Float.NaN;
    }
    private void refresh(){parse();String mode=style.get("mode");dark=!AppearanceConfig.GLOBAL.equals(style.origins.get("mode"))&&("dark".equals(mode)||!"light".equals(mode)&&Theme.isCurrentThemeDark())||AppearanceConfig.GLOBAL.equals(style.origins.get("mode"))&&Theme.isCurrentThemeDark();resources=style.values.isEmpty()?null:new AppearanceResources(AppearanceSurfaces.list(style),dark,base);outlineColor=style.color(dark,"list.outline");hasFill=style.values.containsKey("list.rowOpacity")||style.values.containsKey("list.radius")||style.color(dark,"windowBackgroundWhite")!=null||style.color(dark,"list.windowBackgroundWhite")!=null;scope=null;}
    @Override public int getColor(int key){int color=resources==null?(base==null?Theme.getColor(key):base.getColor(key)):resources.getColor(key);if(key==Theme.key_windowBackgroundWhite&&uniform)return color&0xffffff;if(key==Theme.key_windowBackgroundWhite)color=(color&0xffffff)|Math.round(Color.alpha(color)*rowOpacity)<<24;return color;}
    @Override public boolean isDark(){return resources==null?(base==null?Theme.isCurrentThemeDark():base.isDark()):dark;}
    public boolean separators(){return separators;}
    public void margins(View view){ViewGroup.LayoutParams raw=view.getLayoutParams();if(!(raw instanceof ViewGroup.MarginLayoutParams))return;ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)raw;int side=sideInset,gap=rowGap;if(lp.leftMargin!=side||lp.rightMargin!=side||lp.topMargin!=gap||lp.bottomMargin!=gap){lp.leftMargin=lp.rightMargin=side;lp.topMargin=lp.bottomMargin=gap;view.setLayoutParams(lp);}}
    public void avatar(RectF rect){float nativeSize=rect.width()/AndroidUtilities.density;float size=Math.max(24,Math.min(nativeSize,Float.isNaN(avatarSize)?nativeSize:avatarSize));float inset=(rect.width()-AndroidUtilities.dpf2(size))/2;rect.inset(inset,inset);}
    public void attached(){image.onAttachedToWindow();}
    public void detached(){image.onDetachedFromWindow();}
    public void backdrop(Canvas canvas,int width,int height,float translation){
        int save=canvas.save();canvas.translate(translation,0);
        if(hasImage){image.setImageCoords(0,0,width,height);image.setAlpha(wallOpacity);image.draw(canvas);fill.setColor(Math.round(255*wallDim)<<24);canvas.drawRect(0,0,width,height,fill);}
        if(hasImage||hasFill){fill.setColor(getColor(Theme.key_windowBackgroundWhite));canvas.drawRect(0,0,width,height,fill);}
        canvas.restoreToCount(save);
    }
    public int clip(Canvas canvas,int width,int height){int save=canvas.save();if(radius>0){clip.rewind();rect.set(0,0,width,height);clip.addRoundRect(rect,radius,radius,Path.Direction.CW);canvas.clipPath(clip);}return save;}
    public void finish(Canvas canvas,int width,int height,int save){if(outline>0){stroke.setColor(outlineColor==null?getColor(Theme.key_chats_unreadCounter):outlineColor);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(outline);float d=outline/2;rect.set(d,d,width-d,height-d);canvas.drawRoundRect(rect,radius,radius,stroke);}canvas.restoreToCount(save);}
    public PaintScope begin(){if(resources==null)return null;String mode=style.get("mode");boolean now=AppearanceConfig.GLOBAL.equals(style.origins.get("mode"))?Theme.isCurrentThemeDark():"dark".equals(mode)||!"light".equals(mode)&&Theme.isCurrentThemeDark();if(now!=dark)refresh();if(scope==null)scope=new PaintScope();return scope.open?new PaintScope().begin():scope.begin();}
    public final class PaintScope implements AutoCloseable {
        private final Paint[] paints=new Paint[12];
        private void load(){paints[0]=Theme.dialogs_timePaint;paints[1]=Theme.dialogs_timePaintBold;paints[2]=Theme.dialogs_timePaintBoldAccent;paints[3]=Theme.dialogs_countTextPaint;paints[4]=Theme.dialogs_countTextPaint2;paints[5]=Theme.dialogs_countPaint;paints[6]=Theme.dialogs_countGrayPaint;paints[7]=Theme.dialogs_pinnedPaint;paints[8]=Theme.dialogs_archiveTextPaint;paints[9]=Theme.dialogs_archiveTextPaintSmall;paints[10]=Theme.dialogs_actionMessagePaint;paints[11]=Theme.dividerPaint;}
        private final int[] keys={Theme.key_chats_date,Theme.key_chats_date_bold,Theme.key_telegram_color_text,Theme.key_chats_unreadCounterText,Theme.key_chats_unreadCounterText,Theme.key_chats_unreadCounter,Theme.key_chats_unreadCounterMuted,Theme.key_chats_pinnedOverlay,Theme.key_chats_archiveText,Theme.key_chats_archiveText,Theme.key_chats_actionMessage,Theme.key_divider};
        private final int[] old=new int[paints.length],links=new int[paints.length];
        private boolean open;
        /** Reused per row: theme paints are read when drawing starts, so a theme change is still picked up. */
        PaintScope begin(){open=true;load();for(int i=0;i<paints.length;i++){Paint paint=paints[i];if(paint!=null){old[i]=paint.getColor();if(paint instanceof TextPaint)links[i]=((TextPaint)paint).linkColor;paint.setColor(getColor(keys[i]));}}return this;}
        @Override public void close(){for(int i=0;i<paints.length;i++){Paint paint=paints[i];if(paint!=null){paint.setColor(old[i]);if(paint instanceof TextPaint)((TextPaint)paint).linkColor=links[i];}}open=false;}
    }
}
