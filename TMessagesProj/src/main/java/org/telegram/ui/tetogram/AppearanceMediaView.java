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
    private final AppearanceViewport videoViewport=new AppearanceViewport();
    private final Matrix videoMatrix=new Matrix();
    private final TextureView texture;
    private final View dim;
    private final PowerManager power;
    private final android.graphics.Rect visibleRect=new android.graphics.Rect();
    private VideoPlayer player;
    private AppearanceConfig.Resolved style=new AppearanceConfig.Resolved();
    private File media;
    private long owner,resumePosition;
    private float parallaxStrength,parallaxProgress,sensorX,sensorY,sensorZoom=1;
    private WallpaperParallaxEffect sensorParallax;
    private boolean sensorEnabled;
    private boolean editingViewport,playbackPaused;
    private float editZoom=1,editPanX,editPanY;
    private void disableSensorParallax(){if(sensorParallax!=null)sensorParallax.setEnabled(false);if(sensorEnabled||sensorX!=0||sensorY!=0){sensorEnabled=false;sensorX=sensorY=0;sensorZoom=1;updateTransform();}}
    private void updateSensorParallax(boolean canRun){
        boolean enabled=!editingViewport&&canRun&&media!=null&&"true".equals(style.get("wallpaper.parallax"))&&getContext().getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        if(!enabled){disableSensorParallax();return;}
        try{
            if(sensorParallax==null){sensorParallax=new WallpaperParallaxEffect(getContext());sensorParallax.setCallback((x,y,angle)->{if(!sensorEnabled)return;float bound=Math.max(1,AndroidUtilities.dp(16));sensorX=Math.max(-1,Math.min(1,x/bound));sensorY=Math.max(-1,Math.min(1,y/bound));updateTransform();});}
            if(stableWidth>0&&stableHeight>0)sensorZoom=sensorParallax.getScale(stableWidth,stableHeight);
            sensorEnabled=true;sensorParallax.setEnabled(true);
        }catch(Exception e){disableSensorParallax();}
    }
    private float mediaZoom(){return editingViewport?editZoom:Math.max(1,Math.min(4,style.number("wallpaper.zoom",1)))*Math.max(1+.1f*parallaxStrength,sensorZoom);}
    private float mediaPanX(){return editingViewport?editPanX:style.number("wallpaper.panX",0)+sensorX;}
    private float mediaPanY(){return editingViewport?editPanY:style.number("wallpaper.panY",0)+parallaxStrength*(parallaxProgress*2-1)+sensorY;}
    /** Gesture hot path: no configure(), image request, player creation, resource rebuild or file access. */
    public void setViewport(float zoom,float panX,float panY){editingViewport=true;editZoom=Math.max(1,Math.min(4,zoom));editPanX=Math.max(-1,Math.min(1,panX));editPanY=Math.max(-1,Math.min(1,panY));disableSensorParallax();updateTransform();}
    public float sourceWidth(){return video&&videoWidth>0?videoWidth:cover.getImageReceiver().getBitmapWidth();}
    public float sourceHeight(){return video&&videoHeight>0?videoHeight:cover.getImageReceiver().getBitmapHeight();}
    public void setPlaybackPaused(boolean value){playbackPaused=value;if(player!=null){if(value)player.pause();else if(allowed())player.play();}}
    public void setScrollParallax(float strength,float progress){strength=Math.max(0,Math.min(1,strength));progress=Math.max(0,Math.min(1,progress));if(strength==parallaxStrength&&progress==parallaxProgress)return;parallaxStrength=strength;parallaxProgress=progress;updateTransform();}
    private boolean attached,resumed,failed,video,firstFrame;
    private int stableWidth,stableHeight,videoWidth,videoHeight;
    private String identity="";
    private final Runnable tick=new Runnable(){public void run(){reconcile();if(attached&&resumed)AndroidUtilities.runOnUIThread(this,1000);}};
    private final BroadcastReceiver powerReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){reconcile();}};
    public AppearanceMediaView(Context context){
        super(context);setClipChildren(true);setClickable(false);setFocusable(false);power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        cover=new BackupImageView(context){
            private final AppearanceViewport viewport=new AppearanceViewport();
            @Override protected void onDraw(Canvas canvas){
                if(viewport.set(getWidth(),getHeight(),imageReceiver.getBitmapWidth(),imageReceiver.getBitmapHeight(),"fit".equals(style.get("wallpaper.fit")),mediaZoom(),mediaPanX(),mediaPanY())){
                    imageReceiver.setImageCoords(viewport.x,viewport.y,viewport.width,viewport.height);imageReceiver.draw(canvas);
                }
            }
        };addView(cover,LayoutHelper.createFrame(-1,-1));
        texture=new TextureView(context);texture.setOpaque(false);texture.setAlpha(0);texture.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{updateTransform();showFrame();});addView(texture,LayoutHelper.createFrame(-1,-1));
        dim=new View(context);addView(dim,LayoutHelper.createFrame(-1,-1));
    }
    public void configure(long owner,AppearanceConfig.Resolved value){
        if(owner<=0){stop();identity="";this.owner=0;style=new AppearanceConfig.Resolved();media=null;video=false;cover.setImageDrawable(new ColorDrawable(0));return;}
        String id=owner+":"+value.values.toString();if(identity.equals(id))return;identity=id;File next=AppearanceMedia.file(owner,value.get("wallpaper.file"));boolean nextVideo="video".equals(value.get("wallpaper.type"));boolean sameSource=this.owner==owner&&java.util.Objects.equals(media,next)&&video==nextVideo;
        if(!sameSource){stop();resumePosition=0;videoWidth=videoHeight=0;failed=false;}this.owner=owner;style=value;media=next;video=nextVideo;
        File thumb=video?AppearanceMedia.file(owner,value.get("wallpaper.cover.file")):media;
        ColorDrawable fallback=new ColorDrawable(0x00000000);String filter="1080_1920";
        if(Build.VERSION.SDK_INT<31 && style.number("wallpaper.blur",0)>0)filter="640_640_b";
        if(thumb!=null)cover.setImage(thumb.getAbsolutePath(),filter,fallback);else cover.setImageDrawable(fallback);
        setAlpha(Math.max(0,Math.min(1,style.number("wallpaper.opacity",1))));dim.setBackgroundColor(((int)(255*Math.max(0,Math.min(1,style.number("wallpaper.dim",0)))))<<24);
        if(Build.VERSION.SDK_INT>=31){float blur=Math.max(0,Math.min(32,style.number("wallpaper.blur",0)));android.graphics.RenderEffect effect=blur==0?null:android.graphics.RenderEffect.createBlurEffect(blur,blur,android.graphics.Shader.TileMode.CLAMP);cover.setRenderEffect(effect);texture.setRenderEffect(effect);}
        if(player!=null&&sameSource){player.setLooping(!"false".equals(style.get("wallpaper.loop")));player.setPlaybackSpeed(Math.max(.25f,Math.min(2,style.number("wallpaper.speed",1))));}
        updateTransform();reconcile();
    }
    public void setResumed(boolean value){if(value&&!resumed)failed=false;resumed=value;AndroidUtilities.cancelRunOnUIThread(tick);if(value){AppearanceMediaView previous=active.get();if(previous!=null&&previous!=this)previous.stop();active=new WeakReference<>(this);if(attached)AndroidUtilities.runOnUIThread(tick);}else stop();}
    private boolean surfaceVisible(){if(!isShown()||getWindowVisibility()!=VISIBLE||getAlpha()<=0||!getGlobalVisibleRect(visibleRect)||visibleRect.isEmpty())return false;if(org.telegram.ui.PhotoViewer.hasInstance()&&org.telegram.ui.PhotoViewer.getInstance().isVisible())return false;if(org.telegram.ui.SecretMediaViewer.hasInstance()&&org.telegram.ui.SecretMediaViewer.getInstance().isVisible())return false;for(org.telegram.ui.Stories.StoryViewer viewer:org.telegram.ui.Stories.StoryViewer.globalInstances)if(viewer.isShown())return false;return true;}
    private boolean allowed(){boolean battery=power!=null&&power.isPowerSaveMode()||LiteMode.isPowerSaverApplied();boolean animations=!"false".equals(style.get("effects.enabled"))&&!"false".equals(style.get("wallpaper.animate")) && SectionMenu.animations(getContext());return active.get()==this&&AppearanceMediaPolicy.play(attached,resumed,surfaceVisible(),ApplicationLoader.isScreenOn,!ApplicationLoader.mainInterfacePaused,LiteMode.isEnabled(LiteMode.FLAG_CHAT_BACKGROUND),battery,animations) && !(Build.VERSION.SDK_INT<31&&style.number("wallpaper.blur",0)>0);}
    private void reconcile(){boolean canRun=allowed();updateSensorParallax(canRun);if(!video||failed||media==null||!canRun){stopVideo();return;}if(player!=null)return;
        try{
            VideoPlayer instance=new VideoPlayer(false,true);player=instance;
            instance.setDelegate(new VideoPlayer.VideoPlayerDelegate(){
                public void onStateChanged(boolean ready,int state){}
                public void onError(VideoPlayer p,Exception e){if(player==instance){failed=true;stop();FileLog.e("Tetogram local video background unavailable");}}
                public void onVideoSizeChanged(int w,int h,int rotation,float ratio){if(player!=instance)return;videoWidth=(rotation==90||rotation==270)?h:w;videoHeight=(rotation==90||rotation==270)?w:h;updateTransform();showFrame();}
                public void onRenderedFirstFrame(){if(player==instance){firstFrame=true;showFrame();}}
            });
            instance.setTextureView(texture);instance.setMute(true);instance.setLooping(!"false".equals(style.get("wallpaper.loop")));instance.preparePlayer(android.net.Uri.fromFile(media),"other");instance.setPlaybackSpeed(Math.max(.25f,Math.min(2,style.number("wallpaper.speed",1))));if(resumePosition>0)instance.seekTo(resumePosition);if(!playbackPaused)instance.play();else instance.pause();
        }catch(Exception e){failed=true;stop();FileLog.e("Tetogram video background initialization failed");}
    }
    private void showFrame(){if(!AppearanceMediaPolicy.showFrame(firstFrame,videoWidth,videoHeight,texture.getWidth(),texture.getHeight())||texture.getAlpha()!=0)return;if(SectionMenu.animations(getContext()))texture.animate().alpha(1).setDuration(org.telegram.messenger.tetogram.AppearanceRuntime.effectDuration(150)).start();else texture.setAlpha(1);}
    private void stop(){disableSensorParallax();stopVideo();}
    private void stopVideo(){firstFrame=false;texture.animate().cancel();texture.setAlpha(0);if(player!=null){VideoPlayer previous=player;player=null;try{resumePosition=Math.max(0,previous.getCurrentPosition());previous.pause();previous.releasePlayer(false);}catch(Exception ignored){}}}
    public void retry(){failed=false;reconcile();}
    public long positionMs(){return player==null?0:Math.max(0,player.getCurrentPosition());}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;NotificationCenter.getGlobalInstance().addObserver(this,NotificationCenter.screenStateChanged);ContextCompat.registerReceiver(getContext().getApplicationContext(),powerReceiver,new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),ContextCompat.RECEIVER_NOT_EXPORTED);if(resumed)AndroidUtilities.runOnUIThread(tick);}
    @Override protected void onDetachedFromWindow(){attached=false;stop();AndroidUtilities.cancelRunOnUIThread(tick);NotificationCenter.getGlobalInstance().removeObserver(this,NotificationCenter.screenStateChanged);try{getContext().getApplicationContext().unregisterReceiver(powerReceiver);}catch(Exception ignored){}if(active.get()==this)active=new WeakReference<>(null);super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(texture!=null)reconcile();}
    @Override public void didReceivedNotification(int id,int account,Object...args){reconcile();}
    public void resetViewport(){stableWidth=stableHeight=0;requestLayout();}
    private boolean imeVisible(){if(Build.VERSION.SDK_INT>=30){android.view.WindowInsets insets=getRootWindowInsets();return insets!=null&&insets.isVisible(android.view.WindowInsets.Type.ime());}return true;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(stableWidth!=w||!imeVisible()){stableWidth=w;stableHeight=h;}else stableHeight=Math.max(stableHeight,h);for(View child:new View[]{cover,texture,dim})if(child!=null){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)child.getLayoutParams();if(lp.height!=stableHeight){lp.height=stableHeight;child.setLayoutParams(lp);}}updateTransform();}
    private void updateTransform(){
        if(cover!=null)cover.invalidate();
        if(texture==null||!videoViewport.set(texture.getWidth(),texture.getHeight(),videoWidth,videoHeight,"fit".equals(style.get("wallpaper.fit")),mediaZoom(),mediaPanX(),mediaPanY()))return;
        videoMatrix.setScale(videoViewport.width/texture.getWidth(),videoViewport.height/texture.getHeight());videoMatrix.postTranslate(videoViewport.x,videoViewport.y);texture.setTransform(videoMatrix);
    }
    @Override public void capture(Canvas canvas,RectF position){int save=canvas.save();canvas.clipRect(position);if(getAlpha()<1)canvas.saveLayerAlpha(position,Math.round(getAlpha()*255));cover.draw(canvas);dim.draw(canvas);canvas.restoreToCount(save);}
    @Override protected void dispatchDraw(Canvas canvas){if(SizeNotifierFrameLayout.drawingBlur)capture(canvas,new RectF(0,0,getWidth(),getHeight()));else super.dispatchDraw(canvas);}
}
