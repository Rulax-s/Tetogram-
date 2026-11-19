/* Tetogram, 2026. GNU GPL v2 or later. Offline voice message transcription with Vosk (Apache 2.0). */
package org.telegram.messenger.tetogram;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;

/**
 * Turns voice and round video messages into text on the phone. Nothing is sent anywhere: the language model is
 * downloaded once from the Vosk project, audio is decoded with the system decoder and recognised locally.
 */
public final class VoiceTranscription {
    private VoiceTranscription(){}

    /** Small Vosk models (about 40–130 MB each). */
    public static final String[][] MODELS={
        {"ru","vosk-model-small-ru-0.22","45"},{"en","vosk-model-small-en-us-0.15","40"},{"uk","vosk-model-small-uk-v3-small","133"},
        {"de","vosk-model-small-de-0.15","45"},{"es","vosk-model-small-es-0.42","39"},{"fr","vosk-model-small-fr-0.22","41"},
    };
    public interface Progress{void update(int percent);}
    public interface Done{void done(String text,String error);}

    private static final DispatchQueue QUEUE=new DispatchQueue("tetogramVoice");
    private static SharedPreferences prefs(){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_voice",Context.MODE_PRIVATE);}
    private static File root(){return new File(ApplicationLoader.getFilesDirFixed(),"tetogram_vosk");}
    public static File modelDir(String lang){return new File(root(),lang);}
    public static boolean installed(String lang){return new File(modelDir(lang),".ready").exists();}
    public static String[] model(String lang){for(String[] m:MODELS)if(m[0].equals(lang))return m;return null;}
    /** Chosen language, else the app language when a model exists for it, else Russian. */
    public static String language(){String l=prefs().getString("lang","");if(model(l)!=null)return l;String app=LocaleController.getInstance().getCurrentLocale().getLanguage();return model(app)!=null?app:"ru";}
    public static void setLanguage(String lang){prefs().edit().putString("lang",lang).apply();}
    public static long size(String lang){return folderSize(modelDir(lang));}
    private static long folderSize(File f){if(f.isFile())return f.length();long s=0;File[] list=f.listFiles();if(list!=null)for(File c:list)s+=folderSize(c);return s;}
    private static void delete(File f){File[] list=f.listFiles();if(list!=null)for(File c:list)delete(c);f.delete();}

    // ---- model download ----
    private static volatile boolean cancelDownload;
    public static void cancelDownload(){cancelDownload=true;}
    public static void download(String lang,Progress progress,Done done){
        String[] m=model(lang);if(m==null){done.done(null,"unknown");return;}
        cancelDownload=false;
        QUEUE.postRunnable(()->{
            File dir=modelDir(lang),tmp=new File(root(),lang+".part");
            try{
                delete(tmp);delete(dir);root().mkdirs();tmp.mkdirs();
                HttpURLConnection c=(HttpURLConnection)new URL("https://alphacephei.com/vosk/models/"+m[1]+".zip").openConnection();
                c.setConnectTimeout(20000);c.setReadTimeout(30000);
                if(c.getResponseCode()!=200)throw new IllegalStateException("HTTP "+c.getResponseCode());
                long total=c.getContentLengthLong();long[] read={0};int[] last={-1};
                String prefix=m[1]+"/";
                try(InputStream raw=c.getInputStream();ZipInputStream zip=new ZipInputStream(new CountingStream(raw,n->{read[0]+=n;int p=total>0?(int)(read[0]*100/total):0;if(p!=last[0]){last[0]=p;AndroidUtilities.runOnUIThread(()->progress.update(p));}}))){
                    ZipEntry e;byte[] buf=new byte[64*1024];String canonical=tmp.getCanonicalPath()+File.separator;
                    while((e=zip.getNextEntry())!=null){
                        if(cancelDownload)throw new InterruptedException();
                        String name=e.getName();if(name.startsWith(prefix))name=name.substring(prefix.length());if(name.isEmpty())continue;
                        File out=new File(tmp,name);if(!out.getCanonicalPath().startsWith(canonical))throw new SecurityException("Bad entry");
                        if(e.isDirectory()){out.mkdirs();continue;}
                        out.getParentFile().mkdirs();
                        try(OutputStream o=new FileOutputStream(out)){int n;while((n=zip.read(buf))>0){if(cancelDownload)throw new InterruptedException();o.write(buf,0,n);}}
                    }
                }finally{c.disconnect();}
                if(!new File(tmp,"am").exists()&&!new File(tmp,"conf").exists())throw new IllegalStateException("Not a model");
                new File(tmp,".ready").createNewFile();
                if(!tmp.renameTo(dir))throw new IllegalStateException("rename");
                AndroidUtilities.runOnUIThread(()->done.done("",null));
            }catch(InterruptedException e){delete(tmp);AndroidUtilities.runOnUIThread(()->done.done(null,"cancel"));}
            catch(Throwable e){FileLog.e("Tetogram voice model download failed");delete(tmp);AndroidUtilities.runOnUIThread(()->done.done(null,"network"));}
        });
    }
    public static void remove(String lang){QUEUE.postRunnable(()->{closeModel();delete(modelDir(lang));});}

    // ---- recognition ----
    private static org.vosk.Model loaded;private static String loadedLang;
    private static void closeModel(){if(loaded!=null){try{loaded.close();}catch(Throwable ignored){}loaded=null;loadedLang=null;}}
    private static org.vosk.Model loadModel(String lang)throws Exception{
        if(loaded!=null&&lang.equals(loadedLang))return loaded;
        closeModel();org.vosk.LibVosk.setLogLevel(org.vosk.LogLevel.WARNINGS);
        loaded=new org.vosk.Model(modelDir(lang).getAbsolutePath());loadedLang=lang;return loaded;
    }
    /** Transcribes an audio file (voice note or round video); results are cached by {@code key}. */
    public static void transcribe(String key,File audio,Progress progress,Done done){
        String cached=cached(key);if(cached!=null){done.done(cached,null);return;}
        String lang=language();if(!installed(lang)){done.done(null,"model");return;}
        QUEUE.postRunnable(()->{
            String result=null,error=null;
            try(org.vosk.Recognizer rec=new org.vosk.Recognizer(loadModel(lang),16000f)){
                StringBuilder text=new StringBuilder();
                decode(audio,pcm16k->{if(rec.acceptWaveForm(pcm16k,pcm16k.length))append(text,rec.getResult());},p->AndroidUtilities.runOnUIThread(()->progress.update(p)));
                append(text,rec.getFinalResult());
                result=VoiceTranscriptionRules.tidy(text.toString());
                if(result.isEmpty())error="empty";else remember(key,result);
            }catch(Throwable e){FileLog.e("Tetogram voice transcription failed");error="decode";}
            final String r=result,er=error;AndroidUtilities.runOnUIThread(()->done.done(r,er));
        });
    }
    private static void append(StringBuilder out,String json){try{String t=new JSONObject(json).optString("text","").trim();if(!t.isEmpty()){if(out.length()>0)out.append(' ');out.append(t);}}catch(Exception ignored){}}

    interface Chunk{void accept(short[] pcm16kMono)throws Exception;}
    /** Decodes any system-supported audio track to 16 kHz mono 16-bit PCM in chunks. */
    private static void decode(File file,Chunk sink,Progress progress)throws Exception{
        MediaExtractor ex=new MediaExtractor();MediaCodec codec=null;
        try{
            ex.setDataSource(file.getAbsolutePath());int track=-1;MediaFormat format=null;
            for(int i=0;i<ex.getTrackCount();i++){MediaFormat f=ex.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith("audio/")){track=i;format=f;break;}}
            if(track<0)throw new IllegalStateException("No audio");
            ex.selectTrack(track);long duration=format.containsKey(MediaFormat.KEY_DURATION)?format.getLong(MediaFormat.KEY_DURATION):0;
            codec=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));codec.configure(format,null,null,0);codec.start();
            int rate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE),channels=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            VoiceTranscriptionRules.Resampler resampler=new VoiceTranscriptionRules.Resampler(rate,channels);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();boolean inputDone=false,outputDone=false;int lastPercent=-1;
            while(!outputDone){
                if(!inputDone){int in=codec.dequeueInputBuffer(10000);if(in>=0){ByteBuffer b=codec.getInputBuffer(in);int n=ex.readSampleData(b,0);
                    if(n<0){codec.queueInputBuffer(in,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true;}else{codec.queueInputBuffer(in,0,n,ex.getSampleTime(),0);ex.advance();}}}
                int out=codec.dequeueOutputBuffer(info,10000);
                if(out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat f=codec.getOutputFormat();resampler=new VoiceTranscriptionRules.Resampler(f.getInteger(MediaFormat.KEY_SAMPLE_RATE),f.getInteger(MediaFormat.KEY_CHANNEL_COUNT));}
                else if(out>=0){
                    if(info.size>0){ByteBuffer b=codec.getOutputBuffer(out);b.position(info.offset);b.limit(info.offset+info.size);short[] pcm=new short[info.size/2];b.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm);short[] mono=resampler.process(pcm);if(mono.length>0)sink.accept(mono);
                        if(duration>0){int p=(int)Math.min(99,info.presentationTimeUs*100/duration);if(p!=lastPercent){lastPercent=p;progress.update(p);}}}
                    codec.releaseOutputBuffer(out,false);
                    if((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0)outputDone=true;
                }
            }
        }finally{if(codec!=null){try{codec.stop();}catch(Throwable ignored){}codec.release();}ex.release();}
    }

    // ---- results cache: small file, newest 500 kept ----
    private static final int MAX_CACHED=500;
    private static LinkedHashMap<String,String> cache;
    private static File cacheFile(){return new File(ApplicationLoader.getFilesDirFixed(),"tetogram_transcriptions.json");}
    private static synchronized LinkedHashMap<String,String> cache(){
        if(cache!=null)return cache;cache=new LinkedHashMap<>(64,0.75f,true);File f=cacheFile();if(!f.exists())return cache;
        try(InputStream in=new FileInputStream(f)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)b.write(buf,0,n);
            JSONObject o=new JSONObject(b.toString("UTF-8"));JSONArray names=o.names();if(names!=null)for(int i=0;i<names.length();i++){String k=names.getString(i);cache.put(k,o.getString(k));}}
        catch(Exception e){FileLog.e("Tetogram transcriptions unreadable");}
        return cache;
    }
    public static synchronized String cached(String key){return key==null?null:cache().get(key);}
    private static synchronized void remember(String key,String text){
        if(key==null)return;LinkedHashMap<String,String> map=cache();map.put(key,text);while(map.size()>MAX_CACHED)map.remove(map.keySet().iterator().next());
        try{JSONObject o=new JSONObject();for(Map.Entry<String,String> e:map.entrySet())o.put(e.getKey(),e.getValue());File f=cacheFile(),tmp=new File(f.getPath()+".tmp");
            try(OutputStream out=new FileOutputStream(tmp)){out.write(o.toString().getBytes(StandardCharsets.UTF_8));}if(!tmp.renameTo(f))tmp.delete();}
        catch(Exception e){FileLog.e("Tetogram transcriptions not saved");}
    }
    public static synchronized int cachedCount(){return cache().size();}
    public static synchronized void clearCache(){cache=new LinkedHashMap<>(64,0.75f,true);cacheFile().delete();}

    private interface Count{void read(int n);}
    private static final class CountingStream extends java.io.FilterInputStream {
        private final Count count;CountingStream(InputStream in,Count count){super(in);this.count=count;}
        @Override public int read()throws java.io.IOException{int b=super.read();if(b>=0)count.read(1);return b;}
        @Override public int read(byte[] b,int off,int len)throws java.io.IOException{int n=super.read(b,off,len);if(n>0)count.read(n);return n;}
    }
}
