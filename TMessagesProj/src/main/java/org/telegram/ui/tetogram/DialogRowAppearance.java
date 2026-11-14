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
    public DialogRowAppearance(Theme.ResourcesProvider base,View parent,int account){this.base=base;this.account=account;image=new ImageReceiver(parent);image.setCurrentAccount(account);image.setAllowLoadingOnAttachedOnly(true);}
    public boolean configure(AppearanceConfig.Resolved style){long uid=AppearanceStore.owner(account);if(this.style.values.equals(style.values)&&this.style.origins.equals(style.origins)&&owner==uid)return false;owner=uid;this.style=style;refresh();String origin=style.origins.get("list.wallpaper.file");String name="video".equals(style.get("list.wallpaper.type"))?style.get("list.wallpaper.cover.file"):style.get("list.wallpaper.file");hasImage=uid>0&&origin!=null&&(origin.startsWith("chat:")||origin.startsWith("topic:"))&&AppearanceConfig.isMediaName(name);if(hasImage){File file=AppearanceMedia.file(uid,name);image.setImage(ImageLocation.getForPath(file.getAbsolutePath()),style.number("list.wallpaper.blur",0)>0?"320_80_b":"640_160",null,null,null,0);}else image.setImageBitmap((android.graphics.drawable.Drawable)null);return true;}
    private void refresh(){String mode=style.get("mode");dark=!AppearanceConfig.GLOBAL.equals(style.origins.get("mode"))&&("dark".equals(mode)||!"light".equals(mode)&&Theme.isCurrentThemeDark())||AppearanceConfig.GLOBAL.equals(style.origins.get("mode"))&&Theme.isCurrentThemeDark();resources=style.values.isEmpty()?null:new AppearanceResources(style,dark,base);}
    @Override public int getColor(int key){int color=resources==null?(base==null?Theme.getColor(key):base.getColor(key)):resources.getColor(key);if(key==Theme.key_windowBackgroundWhite)color=(color&0xffffff)|Math.round(Color.alpha(color)*Math.max(0,Math.min(1,style.number("list.rowOpacity",1))))<<24;return color;}
    @Override public boolean isDark(){return resources==null?(base==null?Theme.isCurrentThemeDark():base.isDark()):dark;}
    public boolean separators(){return !"false".equals(style.get("list.separators"));}
    public void margins(View view){ViewGroup.LayoutParams raw=view.getLayoutParams();if(!(raw instanceof ViewGroup.MarginLayoutParams))return;ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)raw;int side=AndroidUtilities.dp(Math.max(0,Math.min(24,style.number("list.sideInset",0)))),gap=AndroidUtilities.dp(Math.max(0,Math.min(20,style.number("list.rowGap",0)))/2);if(lp.leftMargin!=side||lp.rightMargin!=side||lp.topMargin!=gap||lp.bottomMargin!=gap){lp.leftMargin=lp.rightMargin=side;lp.topMargin=lp.bottomMargin=gap;view.setLayoutParams(lp);}}
    public void avatar(RectF rect){float nativeSize=rect.width()/AndroidUtilities.density;float size=Math.max(24,Math.min(nativeSize,style.number("list.avatarSize",nativeSize)));float inset=(rect.width()-AndroidUtilities.dpf2(size))/2;rect.inset(inset,inset);}
    public void attached(){image.onAttachedToWindow();}
    public void detached(){image.onDetachedFromWindow();}
    public void backdrop(Canvas canvas,int width,int height,float translation){
        int save=canvas.save();canvas.translate(translation,0);
        if(hasImage){image.setImageCoords(0,0,width,height);image.setAlpha(Math.max(0,Math.min(1,style.number("list.wallpaper.opacity",1))));image.draw(canvas);fill.setColor(Math.round(255*Math.max(0,Math.min(1,style.number("list.wallpaper.dim",0))))<<24);canvas.drawRect(0,0,width,height,fill);}
        if(hasImage||style.values.containsKey("list.rowOpacity")||style.values.containsKey("list.radius")||style.color(dark,"windowBackgroundWhite")!=null){fill.setColor(getColor(Theme.key_windowBackgroundWhite));canvas.drawRect(0,0,width,height,fill);}
        canvas.restoreToCount(save);
    }
    public int clip(Canvas canvas,int width,int height){float radius=AndroidUtilities.dpf2(Math.max(0,Math.min(32,style.number("list.radius",0))));int save=canvas.save();if(radius>0){clip.rewind();clip.addRoundRect(new RectF(0,0,width,height),radius,radius,Path.Direction.CW);canvas.clipPath(clip);}return save;}
    public void finish(Canvas canvas,int width,int height,int save){float line=Math.max(0,Math.min(4,style.number("list.outlineWidth",0)));if(line>0){Integer color=style.color(dark,"list.outline");stroke.setColor(color==null?getColor(Theme.key_chats_unreadCounter):color);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(AndroidUtilities.dpf2(line));float d=stroke.getStrokeWidth()/2,r=AndroidUtilities.dpf2(Math.max(0,Math.min(32,style.number("list.radius",0))));canvas.drawRoundRect(new RectF(d,d,width-d,height-d),r,r,stroke);}canvas.restoreToCount(save);}
    public PaintScope begin(){if(resources==null)return null;String mode=style.get("mode");boolean now=AppearanceConfig.GLOBAL.equals(style.origins.get("mode"))?Theme.isCurrentThemeDark():"dark".equals(mode)||!"light".equals(mode)&&Theme.isCurrentThemeDark();if(now!=dark)refresh();return new PaintScope();}
    public final class PaintScope implements AutoCloseable {
        private final Paint[] paints={Theme.dialogs_timePaint,Theme.dialogs_timePaintBold,Theme.dialogs_timePaintBoldAccent,Theme.dialogs_countTextPaint,Theme.dialogs_countTextPaint2,Theme.dialogs_countPaint,Theme.dialogs_countGrayPaint,Theme.dialogs_pinnedPaint,Theme.dialogs_archiveTextPaint,Theme.dialogs_archiveTextPaintSmall,Theme.dialogs_actionMessagePaint,Theme.dividerPaint};
        private final int[] keys={Theme.key_chats_date,Theme.key_chats_date_bold,Theme.key_telegram_color_text,Theme.key_chats_unreadCounterText,Theme.key_chats_unreadCounterText,Theme.key_chats_unreadCounter,Theme.key_chats_unreadCounterMuted,Theme.key_chats_pinnedOverlay,Theme.key_chats_archiveText,Theme.key_chats_archiveText,Theme.key_chats_actionMessage,Theme.key_divider};
        private final int[] old=new int[paints.length],links=new int[paints.length];
        PaintScope(){for(int i=0;i<paints.length;i++)if(paints[i]!=null){old[i]=paints[i].getColor();if(paints[i] instanceof TextPaint)links[i]=((TextPaint)paints[i]).linkColor;paints[i].setColor(getColor(keys[i]));}}
        @Override public void close(){for(int i=0;i<paints.length;i++)if(paints[i]!=null){paints[i].setColor(old[i]);if(paints[i] instanceof TextPaint)((TextPaint)paints[i]).linkColor=links[i];}}
    }
}
