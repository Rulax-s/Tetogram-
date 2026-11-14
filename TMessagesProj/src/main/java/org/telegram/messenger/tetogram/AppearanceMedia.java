/* Tetogram, 2026. GNU GPL v2 or later. All file/codec operations run outside the UI thread. */
package org.telegram.messenger.tetogram;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.telegram.messenger.*;
import org.telegram.messenger.video.MediaCodecVideoConvertor;

public final class AppearanceMedia {
    private static final DispatchQueue MEDIA=new DispatchQueue("tetogram-appearance-media");
    private static final Set<String> PENDING=ConcurrentHashMap.newKeySet();
    public static final class Job {public final AtomicBoolean cancelled=new AtomicBoolean();public void cancel(){cancelled.set(true);}}
    public static final class Asset {public File file,cover;public int width,height,rotation;public long durationMs;public boolean video;}
    public interface Result {void done(Asset asset);}
    private AppearanceMedia(){}
    public static File file(long owner,String name){if(!AppearanceConfig.isMediaName(name))return null;return new File(AppearanceStore.directory(owner),"media/"+name);}
    private static File fresh(long owner,String suffix)throws IOException{File dir=new File(AppearanceStore.directory(owner),"media");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Media storage unavailable");File f=new File(dir,UUID.randomUUID()+suffix);PENDING.add(f.getAbsolutePath());return f;}
    private static void check(Job job,long owner,int account)throws IOException{if(job.cancelled.get()||AppearanceStore.owner(account)!=owner)throw new IOException("Media job cancelled");}
    private static void writeBitmap(File f,Bitmap bitmap)throws IOException{if(bitmap==null)throw new IOException("Image decoder failed");try(FileOutputStream out=new FileOutputStream(f)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,88,out))throw new IOException("Image encoding failed");out.getFD().sync();}}
    public static Job importUri(int account,long owner,Uri uri,boolean video,Result result){Job job=new Job();MEDIA.postRunnable(()->{
        File input=null,output=null;Asset asset=null;
        try{check(job,owner,account);input=fresh(owner,video?".mp4":".jpg");
            try(InputStream in=ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(input)){
                if(in==null)throw new IOException("Media URI unavailable");byte[] b=new byte[256*1024];int n;long total=0;while((n=in.read(b))!=-1){check(job,owner,account);total+=n;if(total>AppearanceMediaPolicy.MAX_SOURCE_BYTES)throw new IOException("Media source exceeds storage limit");out.write(b,0,n);}out.getFD().sync();
            }
            asset=new Asset();asset.video=video;
            if(video){readVideo(input,asset);asset.file=input;input=null;}
            else{Bitmap bitmap=ImageLoader.loadBitmap(input.getAbsolutePath(),null,2048,2048,true);output=fresh(owner,".jpg");try{writeBitmap(output,bitmap);asset.width=bitmap.getWidth();asset.height=bitmap.getHeight();}finally{if(bitmap!=null)bitmap.recycle();}asset.file=output;output=null;}
            check(job,owner,account);
        }catch(Exception|OutOfMemoryError e){if(asset!=null)discard(asset.file);asset=null;FileLog.e("Tetogram appearance media import failed");}
        finally{discard(input);discard(output);}
        final Asset ready=asset;AndroidUtilities.runOnUIThread(()->{if(job.cancelled.get()||owner!=AppearanceStore.owner(account)){if(ready!=null)discard(ready.file);return;}result.done(ready);});
    });return job;}
    private static void readVideo(File f,Asset asset)throws Exception{MediaMetadataRetriever retriever=new MediaMetadataRetriever();try{retriever.setDataSource(f.getAbsolutePath());asset.durationMs=Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));asset.width=Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));asset.height=Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));String rotate=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);asset.rotation=rotate==null?0:Integer.parseInt(rotate);if(asset.durationMs<=0||asset.width<=0||asset.height<=0)throw new IOException("Invalid video metadata");}finally{retriever.release();}}
    public static Job inspect(int account,long owner,String name,boolean video,Result result){Job job=new Job();MEDIA.postRunnable(()->{Asset asset=null;File f=file(owner,name);try{check(job,owner,account);if(f==null||!f.isFile())throw new IOException("Media missing");PENDING.add(f.getAbsolutePath());asset=new Asset();asset.file=f;asset.video=video;if(video)readVideo(f,asset);else{android.graphics.BitmapFactory.Options o=new android.graphics.BitmapFactory.Options();o.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(),o);asset.width=o.outWidth;asset.height=o.outHeight;if(o.outWidth<=0||o.outHeight<=0)throw new IOException("Invalid image");}}catch(Exception e){release(f);asset=null;}final Asset ready=asset;AndroidUtilities.runOnUIThread(()->{if(job.cancelled.get()||owner!=AppearanceStore.owner(account)){if(ready!=null)discard(ready.file);return;}result.done(ready);});});return job;}
    public static Job coverAt(int account,long owner,String name,long timeMs,Result result){Job job=new Job();MEDIA.postRunnable(()->{Asset asset=null;File output=null;Bitmap frame=null;MediaMetadataRetriever retriever=new MediaMetadataRetriever();try{check(job,owner,account);File source=file(owner,name);if(source==null)throw new IOException("Video missing");retriever.setDataSource(source.getAbsolutePath());frame=android.os.Build.VERSION.SDK_INT>=27?retriever.getScaledFrameAtTime(Math.max(0,timeMs)*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,640,640):retriever.getFrameAtTime(Math.max(0,timeMs)*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);output=fresh(owner,".jpg");writeBitmap(output,frame);asset=new Asset();asset.file=output;output=null;check(job,owner,account);}catch(Exception|OutOfMemoryError e){if(asset!=null)discard(asset.file);asset=null;}finally{try{retriever.release();}catch(Exception ignored){}if(frame!=null)frame.recycle();discard(output);}final Asset ready=asset;AndroidUtilities.runOnUIThread(()->{if(job.cancelled.get()||owner!=AppearanceStore.owner(account)){if(ready!=null)discard(ready.file);return;}result.done(ready);});});return job;}
    public static Job prepare(int account,long owner,Asset source,String editedImage,VideoEditedInfo edited,int fps,int quality,String selectedCover,Result result){Job job=new Job();MEDIA.postRunnable(()->{
        File output=null,cover=null;Asset prepared=null;
        try{check(job,owner,account);output=fresh(owner,source.video?".mp4":".jpg");
            if(source.video){
                VideoEditedInfo info=edited==null?new VideoEditedInfo():edited;
                if(edited==null){info.originalWidth=source.width;info.originalHeight=source.height;info.originalDuration=source.durationMs*1000L;info.startTime=0;info.endTime=-1;info.rotationValue=source.rotation;info.resultWidth=source.width;info.resultHeight=source.height;}
                info.account=account;info.muted=true;info.volume=0;info.framerate=Math.max(1,Math.min(Math.max(1,info.framerate),fps));
                int longest=Math.max(info.resultWidth,info.resultHeight);info.resultWidth=AppearanceMediaPolicy.evenDimension(info.resultWidth,quality,longest);info.resultHeight=AppearanceMediaPolicy.evenDimension(info.resultHeight,quality,longest);
                long end=AppearanceMediaPolicy.endUs(Math.max(0,info.startTime),info.endTime,source.durationMs*1000L);info.endTime=end;
                MediaController.VideoConvertorListener callback=new MediaController.VideoConvertorListener(){public boolean checkConversionCanceled(){return job.cancelled.get()||owner!=AppearanceStore.owner(account);}public void didWriteData(long size,float progress){}};
                MediaCodecVideoConvertor.ConvertVideoParams params=MediaCodecVideoConvertor.ConvertVideoParams.of(source.file.getAbsolutePath(),output,info.videoOffset,info.rotationValue,false,info.originalWidth,info.originalHeight,info.resultWidth,info.resultHeight,info.framerate,Math.max(600000,info.resultWidth*info.resultHeight*3),info.originalBitrate,info.startTime,end,-1,true,end-info.startTime,callback,info);
                if(new MediaCodecVideoConvertor().convertVideo(params))throw new IOException("Video conversion failed");
                prepared=new Asset();prepared.video=true;prepared.file=output;readVideo(output,prepared);cover=fresh(owner,".jpg");Bitmap frame=null;
                try{if(selectedCover!=null)frame=ImageLoader.loadBitmap(selectedCover,null,640,640,true);if(frame==null){MediaMetadataRetriever retriever=new MediaMetadataRetriever();try{retriever.setDataSource(output.getAbsolutePath());frame=android.os.Build.VERSION.SDK_INT>=27?retriever.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,640,640):retriever.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);}finally{retriever.release();}}writeBitmap(cover,frame);}finally{if(frame!=null)frame.recycle();}prepared.cover=cover;cover=null;output=null;
            }else{Bitmap bitmap=ImageLoader.loadBitmap(editedImage==null?source.file.getAbsolutePath():editedImage,null,2048,2048,true);try{writeBitmap(output,bitmap);prepared=new Asset();prepared.file=output;prepared.width=bitmap.getWidth();prepared.height=bitmap.getHeight();output=null;}finally{if(bitmap!=null)bitmap.recycle();}}
            check(job,owner,account);
        }catch(Exception|OutOfMemoryError e){if(prepared!=null){discard(prepared.file);discard(prepared.cover);}prepared=null;FileLog.e("Tetogram appearance media preparation failed");}
        finally{discard(output);discard(cover);}
        final Asset ready=prepared;AndroidUtilities.runOnUIThread(()->{if(job.cancelled.get()||owner!=AppearanceStore.owner(account)){if(ready!=null){discard(ready.file);discard(ready.cover);}return;}result.done(ready);});
    });return job;}
    public static void release(File f){if(f!=null)PENDING.remove(f.getAbsolutePath());}
    public static void discard(File f){if(f==null)return;AppearanceStore.io(()->{release(f);try{File mediaDir=f.getParentFile();File uidDir=mediaDir==null?null:mediaDir.getParentFile();if(uidDir==null||!"media".equals(mediaDir.getName())||!uidDir.getName().matches("u[1-9][0-9]*"))return;long uid=Long.parseLong(uidDir.getName().substring(1));File expected=file(uid,f.getName());if(expected!=null&&expected.getCanonicalFile().equals(f.getCanonicalFile())&&AppearanceMediaPolicy.discard(f.getName(),AppearanceStore.references(uid),false))f.delete();}catch(Exception ignored){}});}
    public static void cleanup(long uid,AppearanceConfig config){File dir=new File(AppearanceStore.directory(uid),"media");File[] files=dir.listFiles();if(files==null)return;Set<String> refs=config.mediaReferences();for(File f:files)if(AppearanceMediaPolicy.discard(f.getName(),refs,PENDING.contains(f.getAbsolutePath())))f.delete();}
}
