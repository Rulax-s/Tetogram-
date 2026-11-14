/* Tetogram, 2026. GNU GPL v2 or later. Local header layer; never an uploaded avatar. */
package org.telegram.ui.tetogram;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.capture.IBlur3Capture;
import org.telegram.ui.ActionBar.Theme;
public final class AppearanceBanner extends FrameLayout implements IBlur3Capture {
    private final AppearanceMediaView media;
    private AppearanceConfig.Resolved style=new AppearanceConfig.Resolved();
    private final Rect crop=new Rect();
    private int visibleHeight;
    private boolean foreground,playing;
    public AppearanceBanner(Context context){super(context);setClickable(false);setFocusable(false);setClipChildren(true);media=new AppearanceMediaView(context);addView(media,LayoutHelper.createFrame(-1,-1));setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(android.view.View view,Outline out){out.setRoundRect(0,0,getWidth(),visibleHeight,AndroidUtilities.dpf2(Math.max(0,Math.min(40,style.number("banner.radius",0)))));}});setClipToOutline(true);}
    public void configure(long owner,AppearanceConfig.Resolved value){style=value;AppearanceConfig.Resolved mapped=AppearanceSurfaces.banner(value);Drawable background=AppearanceBackgrounds.colors(mapped,!AppearanceConfig.GLOBAL.equals(mapped.origins.get("mode"))&&"dark".equals(mapped.get("mode"))||!"light".equals(mapped.get("mode"))&&Theme.isCurrentThemeDark());setBackground(background);media.configure(owner,mapped);media.setVisibility(AppearanceConfig.isMediaName(mapped.get("wallpaper.file"))?VISIBLE:GONE);invalidateOutline();}
    public void viewport(float height,float alpha){int h=Math.max(0,Math.min(getHeight(),Math.round(height)));visibleHeight=h;crop.set(0,0,getWidth(),h);setClipBounds(crop);setAlpha(Math.max(0,Math.min(1,alpha)));invalidateOutline();updatePlayback();}
    public void setResumed(boolean resumed){foreground=resumed;updatePlayback();}
    private void updatePlayback(){boolean allowed=foreground&&visibleHeight>0&&getAlpha()>0;if(playing!=allowed){playing=allowed;media.setResumed(allowed);}}
    @Override public void capture(Canvas canvas,RectF position){int save=canvas.save();canvas.clipRect(0,0,getWidth(),visibleHeight);if(getBackground()!=null){getBackground().setBounds(0,0,getWidth(),getHeight());getBackground().draw(canvas);}media.capture(canvas,position);canvas.restoreToCount(save);}
}
