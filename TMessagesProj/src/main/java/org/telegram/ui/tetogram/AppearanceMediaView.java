/* Tetogram, 2026. GNU GPL v2 or later. One active silent player, static frame for glass capture. */
package org.telegram.ui.tetogram;
import android.content.*;
import android.graphics.Canvas;
import android.graphics.RectF;
import org.telegram.ui.Components.blur3.capture.IBlur3Capture;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.view.*;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.lang.ref.WeakReference;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.Components.*;

public final class AppearanceMediaView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate, IBlur3Capture {
    private static WeakReference<AppearanceMediaView> active=new WeakReference<>(null);
    private final BackupImageView cover;
    private final TextureView texture;
    private final View dim;
    private final PowerManager power;
    private VideoPlayer player;
    private AppearanceConfig.Resolved style=new AppearanceConfig.Resolved();
    private File media;
    private long owner;
    private boolean attached,resumed,failed,video;
    private int stableWidth,stableHeight,videoWidth,videoHeight;
    private String identity="";
    private final Runnable tick=new Runnable(){public void run(){reconcile();if(attached&&resumed)AndroidUtilities.runOnUIThread(this,1000);}};
    private final BroadcastReceiver powerReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){reconcile();}};
    public AppearanceMediaView(Context context){
        super(context);setClipChildren(true);setClickable(false);setFocusable(false);power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        cover=new BackupImageView(context);addView(cover,LayoutHelper.createFrame(-1,-1));
        texture=new TextureView(context);texture.setOpaque(false);texture.setAlpha(0);addView(texture,LayoutHelper.createFrame(-1,-1));
        dim=new View(context);addView(dim,LayoutHelper.createFrame(-1,-1));
    }
    public void configure(long owner,AppearanceConfig.Resolved value){
        this.owner=owner;String id=owner+":"+value.values.toString();if(identity.equals(id))return;identity=id;stop();failed=false;style=value;media=AppearanceMedia.file(owner,value.get("wallpaper.file"));video="video".equals(value.get("wallpaper.type"));
        File thumb=video?AppearanceMedia.file(owner,value.get("wallpaper.cover.file")):media;
        ColorDrawable fallback=new ColorDrawable(0x00000000);String filter="1080_1920";
        if(Build.VERSION.SDK_INT<31 && style.number("wallpaper.blur",0)>0)filter="640_640_b";
        if(thumb!=null)cover.setImage(thumb.getAbsolutePath(),filter,fallback);else cover.setImageDrawable(fallback);
        setAlpha(Math.max(0,Math.min(1,style.number("wallpaper.opacity",1))));dim.setBackgroundColor(((int)(255*Math.max(0,Math.min(1,style.number("wallpaper.dim",0)))))<<24);
        if(Build.VERSION.SDK_INT>=31){float blur=Math.max(0,Math.min(32,style.number("wallpaper.blur",0)));android.graphics.RenderEffect effect=blur==0?null:android.graphics.RenderEffect.createBlurEffect(blur,blur,android.graphics.Shader.TileMode.CLAMP);cover.setRenderEffect(effect);texture.setRenderEffect(effect);}
        updateTransform();reconcile();
    }
    public void setResumed(boolean value){resumed=value;AndroidUtilities.cancelRunOnUIThread(tick);if(value){AppearanceMediaView previous=active.get();if(previous!=null&&previous!=this)previous.stop();active=new WeakReference<>(this);if(attached)AndroidUtilities.runOnUIThread(tick);}else stop();}
    private boolean allowed(){boolean battery=power!=null&&power.isPowerSaveMode()||LiteMode.isPowerSaverApplied();boolean animations=!"false".equals(style.get("wallpaper.animate")) && SectionMenu.animations(getContext());return active.get()==this&&AppearanceMediaPolicy.play(attached,resumed,isShown()&&getWindowVisibility()==VISIBLE&&getAlpha()>0,ApplicationLoader.isScreenOn,!ApplicationLoader.mainInterfacePaused,LiteMode.isEnabled(LiteMode.FLAG_CHAT_BACKGROUND),battery,animations) && !(Build.VERSION.SDK_INT<31&&style.number("wallpaper.blur",0)>0);}
    private void reconcile(){if(!video||failed||media==null||!allowed()){stop();return;}if(player!=null)return;
        try{
            VideoPlayer instance=new VideoPlayer(false,true);player=instance;
            instance.setDelegate(new VideoPlayer.VideoPlayerDelegate(){
                public void onStateChanged(boolean ready,int state){}
                public void onError(VideoPlayer p,Exception e){if(player==instance){failed=true;stop();FileLog.e("Tetogram local video background unavailable");}}
                public void onVideoSizeChanged(int w,int h,int rotation,float ratio){if(player!=instance)return;videoWidth=(rotation==90||rotation==270)?h:w;videoHeight=(rotation==90||rotation==270)?w:h;updateTransform();}
                public void onRenderedFirstFrame(){if(player==instance)texture.setAlpha(1);}
            });
            instance.setTextureView(texture);instance.setMute(true);instance.setLooping(!"false".equals(style.get("wallpaper.loop")));instance.preparePlayer(android.net.Uri.fromFile(media),"other");instance.setPlaybackSpeed(Math.max(.25f,Math.min(2,style.number("wallpaper.speed",1))));instance.play();
        }catch(Exception e){failed=true;stop();FileLog.e("Tetogram video background initialization failed");}
    }
    private void stop(){texture.setAlpha(0);if(player!=null){VideoPlayer previous=player;player=null;try{previous.pause();previous.releasePlayer(false);}catch(Exception ignored){}}}
    public void retry(){failed=false;reconcile();}
    public long positionMs(){return player==null?0:Math.max(0,player.getCurrentPosition());}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;NotificationCenter.getGlobalInstance().addObserver(this,NotificationCenter.screenStateChanged);ContextCompat.registerReceiver(getContext().getApplicationContext(),powerReceiver,new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),ContextCompat.RECEIVER_NOT_EXPORTED);if(resumed)AndroidUtilities.runOnUIThread(tick);}
    @Override protected void onDetachedFromWindow(){attached=false;stop();AndroidUtilities.cancelRunOnUIThread(tick);NotificationCenter.getGlobalInstance().removeObserver(this,NotificationCenter.screenStateChanged);try{getContext().getApplicationContext().unregisterReceiver(powerReceiver);}catch(Exception ignored){}if(active.get()==this)active=new WeakReference<>(null);super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(texture!=null)reconcile();}
    @Override public void didReceivedNotification(int id,int account,Object...args){reconcile();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(stableWidth!=w){stableWidth=w;stableHeight=h;}else stableHeight=Math.max(stableHeight,h);for(View child:new View[]{cover,texture,dim})if(child!=null){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)child.getLayoutParams();if(lp.height!=stableHeight){lp.height=stableHeight;child.setLayoutParams(lp);}}updateTransform();}
    private void updateTransform(){if(texture==null||texture.getWidth()==0||texture.getHeight()==0||videoWidth<=0||videoHeight<=0)return;float w=texture.getWidth(),h=texture.getHeight();float scale="fit".equals(style.get("wallpaper.fit"))?Math.min(w/videoWidth,h/videoHeight):Math.max(w/videoWidth,h/videoHeight);scale*=Math.max(1,Math.min(4,style.number("wallpaper.zoom",1)));float dw=videoWidth*scale,dh=videoHeight*scale;float x=(w-dw)/2+style.number("wallpaper.panX",0)*Math.abs(w-dw)/2,y=(h-dh)/2+style.number("wallpaper.panY",0)*Math.abs(h-dh)/2;Matrix matrix=new Matrix();matrix.setScale(dw/w,dh/h);matrix.postTranslate(x,y);texture.setTransform(matrix);}
    @Override public void capture(Canvas canvas,RectF position){int save=canvas.save();canvas.clipRect(position);if(getAlpha()<1)canvas.saveLayerAlpha(position,Math.round(getAlpha()*255));cover.draw(canvas);dim.draw(canvas);canvas.restoreToCount(save);}
    @Override protected void dispatchDraw(Canvas canvas){if(SizeNotifierFrameLayout.drawingBlur)capture(canvas,new RectF(0,0,getWidth(),getHeight()));else super.dispatchDraw(canvas);}
}
