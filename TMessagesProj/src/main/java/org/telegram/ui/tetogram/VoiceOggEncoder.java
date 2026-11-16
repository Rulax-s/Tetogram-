/* Tetogram, 2026. GNU GPL v2 or later. Encodes local TTS PCM into a native Telegram Ogg/Opus voice. */
package org.telegram.ui.tetogram;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Android 10+ only: MediaCodec Opus + MediaMuxer OGG. Never uploads local synthesis data. */
public final class VoiceOggEncoder {
    private VoiceOggEncoder() {}
    public static final class Result {
        public final File file;
        public final double durationSeconds;
        public Result(File file, double durationSeconds) { this.file=file; this.durationSeconds=durationSeconds; }
    }
    private static final int TARGET_RATE = 48000;
    private static final int MAX_SECONDS = 40;

    public static Result encode(File wav, File ogg) throws Exception {
        if (Build.VERSION.SDK_INT < 29) throw new IOException("Для голосового необходим Android 10+");
        Samples samples=readWav(wav);
        if (samples.data.length == 0) throw new IOException("Синтез вернул пустой звук");
        short[] pcm=resample(samples);
        MediaCodec codec=null;
        MediaMuxer muxer=null;
        boolean started=false;
        try {
            MediaFormat format=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS,TARGET_RATE,1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 32000);
            codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS);
            codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();
            muxer=new MediaMuxer(ogg.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            int track=-1,position=0;
            boolean inputEnded=false, outputEnded=false;
            long deadline=System.nanoTime()+120_000_000_000L;
            while (!outputEnded) {
                if (System.nanoTime()>deadline || Thread.currentThread().isInterrupted()) throw new IOException("Opus encoder timeout");
                if (!inputEnded) {
                    int inputIndex=codec.dequeueInputBuffer(10000);
                    if (inputIndex>=0) {
                        ByteBuffer input=codec.getInputBuffer(inputIndex);
                        if (input==null) throw new IOException("Opus input buffer unavailable");
                        input.clear();input.order(ByteOrder.LITTLE_ENDIAN);
                        int amount=Math.min(Math.min(960,input.remaining()/2),pcm.length-position);
                        long pts=position*1_000_000L/TARGET_RATE;
                        if (amount<=0) {
                            codec.queueInputBuffer(inputIndex,0,0,pts,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputEnded=true;
                        } else {
                            for(int i=0;i<amount;i++)input.putShort(pcm[position+i]);
                            codec.queueInputBuffer(inputIndex,0,amount*2,pts,0);
                            position+=amount;
                        }
                    }
                }
                int outputIndex=codec.dequeueOutputBuffer(info,10000);
                if (outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if(started)throw new IOException("Opus output changed twice");
                    track=muxer.addTrack(codec.getOutputFormat());
                    muxer.start();started=true;
                } else if (outputIndex>=0) {
                    ByteBuffer buffer=codec.getOutputBuffer(outputIndex);
                    if(buffer==null)throw new IOException("Opus output buffer unavailable");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0 && info.size>0) {
                        if(!started)throw new IOException("Opus muxer not started");
                        buffer.position(info.offset);buffer.limit(info.offset+info.size);
                        muxer.writeSampleData(track,buffer,info);
                    }
                    outputEnded=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    codec.releaseOutputBuffer(outputIndex,false);
                }
            }
            if (!started) throw new IOException("OGG muxer failed to start");
        } finally {
            if(codec!=null){try{codec.stop();}catch(Exception ignored){}try{codec.release();}catch(Exception ignored){}}
            if(muxer!=null){if(started)try{muxer.stop();}catch(Exception ignored){}try{muxer.release();}catch(Exception ignored){}}
        }
        if(ogg.length()<100) throw new IOException("Пустой или неподдерживаемый OGG");
        return new Result(ogg, pcm.length/(double)TARGET_RATE);
    }
    private static final class Samples {
        final short[] data; final int rate;
        Samples(short[] data,int rate){this.data=data;this.rate=rate;}
    }
    private static int littleInt(byte[] b,int start){return (b[start]&255)|((b[start+1]&255)<<8)|((b[start+2]&255)<<16)|((b[start+3]&255)<<24);}
    private static int littleShort(byte[] b,int start){return (b[start]&255)|((b[start+1]&255)<<8);}
    private static Samples readWav(File f) throws IOException {
        try(RandomAccessFile raf=new RandomAccessFile(f,"r")) {
            if(raf.length()<44||raf.length()>24*1024*1024)throw new IOException("Некорректный размер WAV");
            byte[] header=new byte[12];raf.readFully(header);
            if(!"RIFF".equals(new String(header,0,4,"US-ASCII"))||!"WAVE".equals(new String(header,8,4,"US-ASCII")))throw new IOException("TTS не вернул WAV PCM");
            int channels=0,rate=0,bits=0,encoding=0;long dataAt=-1;int dataSize=0;
            while (raf.getFilePointer()+8<=raf.length()) {
                byte[] h=new byte[8];raf.readFully(h);String chunk=new String(h,0,4,"US-ASCII");
                long size=littleInt(h,4)&0xffffffffL;long at=raf.getFilePointer();
                if(size>raf.length()-at)throw new IOException("Повреждённый WAV");
                if("fmt ".equals(chunk) && size>=16){byte[] fmt=new byte[16];raf.readFully(fmt);encoding=littleShort(fmt,0);channels=littleShort(fmt,2);rate=littleInt(fmt,4);bits=littleShort(fmt,14);}
                else if("data".equals(chunk)){dataAt=at;dataSize=(int)size;}
                raf.seek(at+size+(size&1));
                if(dataAt>=0&&rate>0)break;
            }
            if(encoding!=1||(bits!=16&&bits!=8)||(channels!=1&&channels!=2)||rate<8000||rate>96000||dataAt<0)throw new IOException("Неподдерживаемый формат WAV от TTS");
            int bytesPerSample=channels*(bits/8);int frameCount=dataSize/bytesPerSample;
            if(frameCount<1||frameCount>(long)MAX_SECONDS*rate)throw new IOException("Голосовое слишком длинное (максимум 40 секунд)");
            byte[] source=new byte[frameCount*bytesPerSample];raf.seek(dataAt);raf.readFully(source);
            short[] out=new short[frameCount];
            for(int i=0;i<frameCount;i++){
                int total=0;
                for(int ch=0;ch<channels;ch++){
                    int ix=i*bytesPerSample+ch*(bits/8);
                    total+=bits==16?(short)littleShort(source,ix):((source[ix]&255)-128)*256;
                }
                out[i]=(short)(total/channels);
            }
            return new Samples(out,rate);
        }
    }
    private static short[] resample(Samples s){
        if(s.rate==TARGET_RATE)return s.data;
        int n=(int)((long)s.data.length*TARGET_RATE/s.rate);short[] out=new short[n];
        for(int i=0;i<n;i++){
            double position=i*(double)s.rate/TARGET_RATE;int a=(int)position;
            int b=Math.min(s.data.length-1,a+1);double mix=position-a;
            out[i]=(short)Math.round(s.data[a]*(1-mix)+s.data[b]*mix);
        }
        return out;
    }
}
