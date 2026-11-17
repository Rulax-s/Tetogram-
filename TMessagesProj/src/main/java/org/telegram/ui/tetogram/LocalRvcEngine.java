/* Tetogram. GPL-2.0-or-later. Original, on-device RVC/ONNX integration. */
package org.telegram.ui.tetogram;

import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import android.content.Context;
import android.util.Half;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Offline RVC pipeline. Accepts voice-changer-format ONNX synthesizers +
 * ContentVec/HuBERT + RMVPE. Loads sessions sequentially to limit peak memory.
 * An experimental engine, not yet hardware-tested with every ONNX export.
 */
public final class LocalRvcEngine {
    private static final int INPUT_RATE = 16000;
    private static final int MAX_SECONDS = 15;
    private static final float MIN_F0 = 50f, MAX_F0 = 1100f;
    private LocalRvcEngine() {}

    public static void convert(Context ctx, File sourceWav, File outputWav, int character, int transpose) throws Exception {
        if (!RvcModelStore.available(ctx, character)) throw new IOException("Нет моделей RVC. Откройте настройки моделей.");
        float[] input = readPcmWav16k(sourceWav);
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Отменено");
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        File synth = RvcModelStore.path(ctx, character == 0 ? RvcModelStore.TETO : RvcModelStore.MIKU);
        ModelMeta meta;
        try (ManagedSession holder = open(env, synth)) {meta = readMeta(holder.session);}
        String embOutput = meta.outputName();
        FeatureMatrix features;
        try (ManagedSession hubertHolder = open(env, RvcModelStore.path(ctx, RvcModelStore.HUBERT))) {
            OrtSession hubert = hubertHolder.session;
            try (OnnxTensor inputTensor = floatTensor(env, input, new long[]{1, input.length});
                 OrtSession.Result result = hubert.run(Collections.singletonMap("audio", inputTensor), Collections.singleton(embOutput))) {
                OnnxTensor encoded = tensor(result, embOutput);
                long[] shape = ((TensorInfo)encoded.getInfo()).getShape();
                if (shape.length != 3 || shape[0] != 1 || shape[1] <= 0 || shape[2] != meta.embChannels) {
                    throw new IOException("Несовместимые выходы HuBERT и Synth");
                }
                features = new FeatureMatrix(floatData(encoded), (int)shape[1], (int)shape[2]);
            }
        }
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Отменено");
        float[] pitchF = null;
        long[] pitch = null;
        if (meta.needsPitch) {
            try (ManagedSession rmvpeHolder = open(env, RvcModelStore.path(ctx, RvcModelStore.RMVPE));
                 OnnxTensor wave = floatTensor(env, input, new long[]{1, input.length});
                 OnnxTensor threshold = floatTensor(env, new float[]{0.3f}, new long[]{1})) {
                OrtSession rmvpe = rmvpeHolder.session;
                Map<String, OnnxTensor> ins = new LinkedHashMap<>();
                ins.put("waveform", wave); ins.put("threshold", threshold);
                try (OrtSession.Result result = rmvpe.run(ins)) {
                    pitchF = floatData((OnnxTensor)result.get(0));
                }
            }
            float factor = (float)Math.pow(2.0, transpose / 12.0);
            pitch = new long[pitchF.length];
            for (int i = 0; i < pitchF.length; i++) {
                float f = pitchF[i] * factor;
                pitchF[i] = f;
                pitch[i] = quantizedPitch(f);
            }
        }
        int frames = features.frames * 2;
        if (pitchF != null) frames = Math.min(frames, pitchF.length);
        if (frames <= 0) throw new IOException("Модель не вернула аудиокадры");
        float[] expanded = new float[frames * features.channels];
        for (int i = 0; i < frames; i++) {
            System.arraycopy(features.data, (i / 2) * features.channels, expanded, i * features.channels, features.channels);
        }
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Отменено");
        if (pitchF != null && pitchF.length != frames) {
            float[] sliced = new float[frames];
            long[] slicedInt = new long[frames];
            System.arraycopy(pitchF, 0, sliced, 0, frames);
            System.arraycopy(pitch, 0, slicedInt, 0, frames);
            pitchF = sliced; pitch = slicedInt;
        }
        float[] generated;
        try (ManagedSession synthHolder = open(env, synth)) {
            OrtSession synthSession = synthHolder.session;
            Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
            try {
                // voice-changer uses feats/p_len/pitchf; Applio/RVC v2 uses
                // phone/phone_lengths/nsff0 plus an optional rate tensor.
                // The rate means the fraction of audio to KEEP, so 1.0 keeps
                // the full utterance (0.0 would trim almost everything).
                String featsName = meta.applio ? "phone" : "feats";
                String lenName = meta.applio ? "phone_lengths" : "p_len";
                String pitchfName = meta.applio ? "nsff0" : "pitchf";
                OnnxJavaType featsType = ((TensorInfo)synthSession.getInputInfo().get(featsName).getInfo()).type;
                if (featsType != OnnxJavaType.FLOAT && featsType != OnnxJavaType.FLOAT16) {
                    throw new IOException("Неподдерживаемый тип речевых признаков: " + featsType);
                }
                inputs.put(featsName, featsType == OnnxJavaType.FLOAT16
                    ? halfTensor(env, expanded, new long[]{1, frames, features.channels})
                    : floatTensor(env, expanded, new long[]{1, frames, features.channels}));
                inputs.put(lenName, longTensor(env, new long[]{frames}, new long[]{1}));
                inputs.put("sid", longTensor(env, new long[]{0}, new long[]{1}));
                if (meta.needsPitch) {
                    inputs.put("pitch", longTensor(env, pitch, new long[]{1, frames}));
                    inputs.put(pitchfName, floatTensor(env, pitchF, new long[]{1, frames}));
                }
                if (meta.applio && synthSession.getInputInfo().containsKey("rate")) {
                    OnnxJavaType rateType = ((TensorInfo)synthSession.getInputInfo().get("rate").getInfo()).type;
                    if (rateType == OnnxJavaType.FLOAT) {
                        inputs.put("rate", floatTensor(env, new float[]{1.0f}, new long[]{1}));
                    } else if (rateType == OnnxJavaType.FLOAT16) {
                        inputs.put("rate", halfTensor(env, new float[]{1.0f}, new long[]{1}));
                    } else if (rateType == OnnxJavaType.DOUBLE) {
                        inputs.put("rate", OnnxTensor.createTensor(env, java.nio.DoubleBuffer.wrap(new double[]{1.0}), new long[]{1}));
                    } else {
                        throw new IOException("Неподдерживаемый ONNX-тип входа rate: " + rateType);
                    }
                }
                try (OrtSession.Result result = synthSession.run(inputs, Collections.singleton("audio"))) {
                    generated = floatData(tensor(result, "audio"));
                }
            } finally {for (OnnxTensor t : inputs.values()) t.close();}
        }
        if (generated.length == 0 || generated.length > meta.sampleRate * (MAX_SECONDS + 5L)) {
            throw new IOException("Некорректная длина результата синтезатора");
        }
        writePcmWav(outputWav, generated, meta.sampleRate);
    }

    private static final class FeatureMatrix {
        final float[] data; final int frames, channels;
        FeatureMatrix(float[] d, int t, int c) {data = d; frames = t; channels = c;}
    }
    private static final class ModelMeta {
        final int sampleRate, embChannels, layer;
        final boolean needsPitch, finalProjection, applio;
        ModelMeta(int rate, int channels, int layer, boolean f0, boolean proj, boolean isApplio) {
            sampleRate=rate; embChannels=channels; this.layer=layer;
            needsPitch=f0; finalProjection=proj; applio=isApplio;
        }
        String outputName() throws IOException {
            if (layer==12 && !finalProjection) return "unit12";
            if (layer==12) return "unit12s";
            if (layer==9 && finalProjection) return "units9";
            throw new IOException("Неподдерживаемый формат HuBERT в метаданных Synth");
        }
    }
    private static ModelMeta readMeta(OrtSession s) throws Exception {
        Map<String, ai.onnxruntime.NodeInfo> inputs = s.getInputInfo();
        boolean applio = inputs.containsKey("phone") && inputs.containsKey("phone_lengths");
        boolean voiceChanger = inputs.containsKey("feats") && inputs.containsKey("p_len");
        if (!applio && !voiceChanger) {
            throw new IOException("Неизвестные входы Synth: " + inputs.keySet());
        }
        if (!inputs.containsKey("sid")) throw new IOException("У Synth отсутствует вход sid");
        Map<String, String> props = s.getMetadata().getCustomMetadata();
        int rate, channels, layer;
        boolean f0, proj;
        if (applio) {
            // Applio exports metadata as model_info, not voice-changer metadata.
            // Never blindly assume this layout for an unrelated ONNX model.
            String raw = props.get("model_info");
            if (raw == null) throw new IOException("Applio Synth не содержит model_info");
            JSONObject obj = new JSONObject(raw);
            if (!"v1".equals(obj.optString("version")) && !"v2".equals(obj.optString("version"))) {
                throw new IOException("Неизвестная версия Applio Synth");
            }
            rate = obj.getInt("sr");
            ai.onnxruntime.NodeInfo node = inputs.get("phone");
            if (!(node.getInfo() instanceof TensorInfo)) throw new IOException("Некорректный тип phone");
            long[] phoneShape = ((TensorInfo)node.getInfo()).getShape();
            if (phoneShape.length != 3) throw new IOException("Ожидался phone [B,T,C]");
            channels = (int)phoneShape[2];
            // RVC v1 uses 256d final-proj layer 9; v2 uses 768d raw layer 12.
            layer = channels == 768 ? 12 : 9;
            proj = channels == 256;
            f0 = obj.optBoolean("f0", inputs.containsKey("pitch"));
            if (f0 && (!inputs.containsKey("pitch") || !inputs.containsKey("nsff0"))) {
                throw new IOException("Applio Synth требует входы pitch и nsff0");
            }
        } else {
            String raw = props.get("metadata");
            if (raw == null) throw new IOException("voice-changer Synth не содержит metadata");
            JSONObject obj = new JSONObject(raw);
            rate = obj.getInt("samplingRate");
            channels = obj.getInt("embChannels");
            layer = obj.getInt("embOutputLayer");
            proj = obj.optBoolean("useFinalProj", false);
            f0 = obj.getBoolean("f0");
        }
        if (rate < 16000 || rate > 48000 || (channels != 256 && channels != 768)) {
            throw new IOException("Неподдерживаемые параметры Synth: " + rate + "Hz, " + channels + "d");
        }
        return new ModelMeta(rate, channels, layer, f0, proj, applio);
    }
    private static final class ManagedSession implements AutoCloseable {
        final OrtSession.SessionOptions options;
        final OrtSession session;
        ManagedSession(OrtSession.SessionOptions options, OrtSession session) {this.options=options;this.session=session;}
        @Override public void close() throws Exception {try {session.close();} finally {options.close();}}
    }
    private static ManagedSession open(OrtEnvironment env, File file) throws Exception {
        if (!file.isFile() || file.length() < 1024) throw new IOException("Модель отсутствует: " + file.getName());
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        try {
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            options.setIntraOpNumThreads(2);
            return new ManagedSession(options, env.createSession(file.getAbsolutePath(), options));
        } catch (Exception e) {
            options.close();
            throw e;
        }
    }
    private static OnnxTensor tensor(OrtSession.Result result, String name) throws IOException {
        OnnxValue value = result.get(name).orElse(null);
        if (!(value instanceof OnnxTensor)) throw new IOException("Нет ONNX-выхода " + name);
        return (OnnxTensor)value;
    }
    private static OnnxTensor floatTensor(OrtEnvironment env, float[] values, long[] shape) throws Exception {
        return OnnxTensor.createTensor(env, FloatBuffer.wrap(values), shape);
    }
    private static OnnxTensor longTensor(OrtEnvironment env, long[] values, long[] shape) throws Exception {
        return OnnxTensor.createTensor(env, LongBuffer.wrap(values), shape);
    }
    private static OnnxTensor halfTensor(OrtEnvironment env, float[] values, long[] shape) throws Exception {
        short[] encoded = new short[values.length];
        for (int i=0;i<values.length;i++) encoded[i] = Half.toHalf(values[i]);
        return OnnxTensor.createTensor(env, ShortBuffer.wrap(encoded), shape, OnnxJavaType.FLOAT16);
    }
    private static float[] floatData(OnnxTensor tensor) throws IOException {
        TensorInfo info = (TensorInfo)tensor.getInfo();
        if (info.type == OnnxJavaType.FLOAT) {
            FloatBuffer buf = tensor.getFloatBuffer();
            float[] data = new float[buf.remaining()]; buf.get(data);
            return data;
        }
        if (info.type == OnnxJavaType.FLOAT16) {
            ShortBuffer buf = tensor.getByteBuffer().order(ByteOrder.nativeOrder()).asShortBuffer();
            float[] data = new float[buf.remaining()];
            for (int i=0;i<data.length;i++) data[i] = Half.toFloat(buf.get());
            return data;
        }
        throw new IOException("Ожидался ONNX FLOAT/FLOAT16, получено " + info.type);
    }
    private static long quantizedPitch(float frequency) {
        if (!(frequency > 0)) return 1;
        double mel = 1127 * Math.log(1 + frequency / 700);
        double low = 1127 * Math.log(1 + MIN_F0 / 700);
        double high = 1127 * Math.log(1 + MAX_F0 / 700);
        return Math.round(Math.max(1, Math.min(255, (mel - low) * 254 / (high - low) + 1)));
    }
    private static float[] readPcmWav16k(File f) throws Exception {
        try (RandomAccessFile file = new RandomAccessFile(f, "r")) {
            if (file.length()<44 || file.length()>40L*1024*1024) throw new IOException("Повреждённый WAV от TTS");
            byte[] header=new byte[12];file.readFully(header);
            if (str(header,0,4).equals("RIFF") == false || !str(header,8,4).equals("WAVE")) throw new IOException("Не WAV RIFF");
            int format = -1, channels = 0, bits = 0, rate = 0;
            long dataOffset=-1, dataLength=0;
            while(file.getFilePointer()+8<=file.length()) {
                byte[] chunk = new byte[8];file.readFully(chunk);
                String name=str(chunk,0,4);
                long size=uint32(chunk,4);
                long start=file.getFilePointer();
                if (size>file.length()-start) throw new IOException("WAV chunk за пределами файла");
                if (name.equals("fmt ") && size>=16) {
                    byte[] data=new byte[16]; file.readFully(data);
                    format=uint16(data,0);channels=uint16(data,2);rate=(int)uint32(data,4);bits=uint16(data,14);
                }
                if (name.equals("data")) {dataOffset=start;dataLength=size;}
                long next=start+size+(size&1);
                if (next>file.length()) next=file.length();
                file.seek(next);
                if (dataOffset>=0 && format!=-1) break;
            }
            if (format!=1 || bits!=16 || channels<1 || channels>2 || rate<8000 || rate>96000 || dataOffset<0) {
                throw new IOException("TTS WAV должен быть PCM16 mono/stereo");
            }
            long frames=dataLength/(2L*channels);
            if (frames<1 || frames > (long)MAX_SECONDS*rate) throw new IOException("Для RVC максимум 15 секунд речи");
            file.seek(dataOffset);
            float[] pcm=new float[(int)frames];
            for(int i=0;i<pcm.length;i++) {
                int sum=0;
                for (int ch=0;ch<channels;ch++) sum+=(short)Short.reverseBytes(file.readShort());
                pcm[i]=(sum/(float)channels)/32768f;
            }
            if (rate == INPUT_RATE) return pcm;
            int count=(int)Math.round(frames*(double)INPUT_RATE/rate);
            float[] resized=new float[count];
            for(int i=0;i<count;i++) {
                double pos=Math.min(pcm.length-1, i*(double)rate/INPUT_RATE);
                int a=Math.min((int)pos, pcm.length-1);int b=Math.min(a+1,pcm.length-1);
                resized[i]=(float)(pcm[a]*(1-(pos-a))+pcm[b]*(pos-a));
            }
            return resized;
        }
    }
    private static String str(byte[] b,int start,int len) throws Exception {return new String(b,start,len,"US-ASCII");}
    private static int uint16(byte[] b,int i) {return (b[i]&255)|((b[i+1]&255)<<8);}
    private static long uint32(byte[] b,int i) {return (b[i]&255L)|((b[i+1]&255L)<<8)|((b[i+2]&255L)<<16)|((b[i+3]&255L)<<24);}
    private static void writePcmWav(File output, float[] audio, int rate) throws IOException {
        long bytes = 2L * audio.length;
        if (bytes > Integer.MAX_VALUE) throw new IOException("Слишком длинный файл");
        try (FileOutputStream out = new FileOutputStream(output)) {
            ByteBuffer head = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
            head.put(new byte[]{'R','I','F','F'});head.putInt(36+(int)bytes);
            head.put(new byte[]{'W','A','V','E','f','m','t',' '});head.putInt(16);
            head.putShort((short)1);head.putShort((short)1);head.putInt(rate);
            head.putInt(rate*2);head.putShort((short)2);head.putShort((short)16);
            head.put(new byte[]{'d','a','t','a'});head.putInt((int)bytes);
            out.write(head.array());
            ByteBuffer block=ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
            for (float v : audio) {
                if (block.remaining() < 2) {out.write(block.array(),0,block.position());block.clear();}
                int pcm=(int)(Math.max(-1,Math.min(1,v))*32767f);
                block.putShort((short)pcm);
            }
            if (block.position()>0) out.write(block.array(),0,block.position());
        }
    }
}
