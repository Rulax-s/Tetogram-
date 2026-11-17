/* Tetogram GPL-2.0-or-later. Direct, on-device FAISS IVF-Flat retrieval. */
package org.telegram.ui.tetogram;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * Direct reader for the exact floating-point IVF-Flat indexes produced by RVC.
 * No Python/FAISS runtime, no training-vector truncation and no heap copy of
 * the 245+ MiB dataset. Uses mmap and searches the original IVF posting lists.
 * Supports current FAISS IwFl/ilar and flat IxF2/IxFl/IxFI formats.
 * Does NOT silently accept PQ/SQ/compressed or other incompatible indices.
 *
 * Serialization rules follow the FAISS MIT-licensed index I/O specification.
 * Index must match the voice model's ContentVec (768d) or HuBERT (256d).
 */
public final class RvcFaissIndex implements AutoCloseable {
    private final RandomAccessFile raf;
    private final FileChannel channel;
    private final MappedByteBuffer map;
    private final int dim, metric, lists;
    private final long total;
    private final float[] centroids;
    private final long[] listData;
    private final int[] listSizes;
    private final int codeSize;
    private final int defaultNprobe;

    private RvcFaissIndex(File file) throws IOException {
        if (!file.isFile() || file.length() < 64 || file.length() > 900L * 1024 * 1024)
            throw new IOException("Индекс RVC отсутствует или превышает 900 МиБ");
        raf = new RandomAccessFile(file, "r");
        channel = raf.getChannel();
        try {
            map = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            map.order(ByteOrder.LITTLE_ENDIAN);
            String kind = fourcc();
            Header header = header();
            dim = header.dim;
            metric = header.metric;
            total = header.total;
            if (total < 1) throw new IOException("Индекс пуст: нет добавленных речевых векторов");
            if (kind.equals("IwFl")) {
                long n = u64();
                long probe = u64();
                if (n < 1 || n > 8192 || probe < 1 || probe > n) throw bad();
                lists = (int)n;
                defaultNprobe = (int)Math.min(probe, 8);
                String coarseType = fourcc();
                if (!coarseType.equals("IxF2") && !coarseType.equals("IxFl") && !coarseType.equals("IxFI"))
                    throw new IOException("Неподдерживаемый квантователь FAISS: " + coarseType);
                Header quant = header();
                if (quant.dim != dim || quant.total != lists || quant.metric != metric) throw bad();
                long floats = u64();
                if (floats != (long)lists * dim) throw bad();
                centroids = new float[Math.toIntExact(floats)];
                for (int i=0; i<centroids.length; i++) centroids[i]=finiteFloat();
                int directType = map.get() & 255;
                long arrayLen = u64();
                skip(checkedBytes(arrayLen, 8));
                if (directType == 2) {
                    long pairs = u64();
                    skip(checkedBytes(pairs,16));
                } else if (directType != 0 && directType != 1) throw bad();
                if (!fourcc().equals("ilar")) throw new IOException("RVC индекс использует неподдерживаемый тип inverted lists");
                if (u64() != lists) throw bad();
                long cs = u64();
                if (cs != (long)dim * 4) throw bad();
                codeSize = (int)cs;
                listData = new long[lists];
                listSizes = new int[lists];
                String layout = fourcc();
                if (layout.equals("full")) {
                    if (u64()!=lists) throw bad();
                    for (int i=0; i<lists; i++) listSizes[i]=count(u64());
                } else if (layout.equals("sprs")) {
                    long sparseEntries = u64();
                    if ((sparseEntries & 1)!=0 || sparseEntries > (long)lists * 2) throw bad();
                    for (int i=0; i<sparseEntries/2; i++) {
                        long index=u64();
                        if (index < 0 || index >= lists) throw bad();
                        listSizes[(int)index]=count(u64());
                    }
                } else throw new IOException("Неподдерживаемая сериализация inverted lists: "+layout);
                long actual = 0;
                for (int i=0; i<lists; i++) {
                    actual += listSizes[i];
                    listData[i]=map.position();
                    skip(checkedBytes(listSizes[i],codeSize)); // all code vectors, no truncation
                    skip(checkedBytes(listSizes[i],8));        // FAISS vector IDs
                }
                if (actual != total) throw new IOException("Индекс RVC содержит не все заявленные векторы");
            } else if (kind.equals("IxF2") || kind.equals("IxFl") || kind.equals("IxFI")) {
                if (u64() != total*dim) throw bad();
                lists = 1;
                defaultNprobe = 1;
                codeSize=dim*4;
                listData=new long[]{map.position()};
                listSizes=new int[]{count(total)};
                centroids=null;
                skip(checkedBytes(total,codeSize));
            } else throw new IOException("Неподдерживаемый тип FAISS: "+kind+" (поддерживается RVC IVF-Flat)");
            if (map.position() > channel.size()) throw bad();
        } catch (IOException | RuntimeException e) {
            try {channel.close();} finally {raf.close();}
            if (e instanceof IOException) throw (IOException)e;
            throw new IOException("Повреждённый/несовместимый индекс FAISS: "+e.getMessage(), e);
        }
    }

    public static void validate(File file) throws IOException {
        try (RvcFaissIndex index = new RvcFaissIndex(file)) {
            if (index.total<1) throw new IOException("Индекс пуст");
        }
    }

    public int dimension() {return dim;}
    public long count() {return total;}
    private IOException bad() {return new IOException("Неправильная структура или длина индекса FAISS");}
    private String fourcc() throws IOException {
        require(4);
        byte[] b = new byte[4]; map.get(b);
        return new String(b, java.nio.charset.StandardCharsets.US_ASCII);
    }
    private void require(long bytes) throws IOException {
        if (bytes<0 || bytes > (long)map.limit()-map.position()) throw bad();
    }
    private int count(long n) throws IOException {
        if (n < 0 || n > Integer.MAX_VALUE) throw bad();
        return (int)n;
    }
    private long u64() throws IOException {
        require(8);
        long result=map.getLong();
        if (result<0) throw bad();
        return result;
    }
    private long checkedBytes(long n,long unit) throws IOException {
        if (n<0 || unit<0 || n>Long.MAX_VALUE/unit) throw bad();
        return n*unit;
    }
    private void skip(long n) throws IOException {
        require(n);
        map.position((int)(map.position()+n));
    }
    private float finiteFloat() throws IOException {
        require(4);
        float x=map.getFloat();
        if (!Float.isFinite(x)) throw bad();
        return x;
    }
    private static final class Header {
        final int dim, metric;
        final long total;
        Header(int d, int m, long t) {dim=d;metric=m;total=t;}
    }
    private Header header() throws IOException {
        require(4+8+8+8+1+4);
        int d = map.getInt();
        long n = map.getLong();
        map.getLong(); map.getLong();
        int trained=map.get()&255;
        int m=map.getInt();
        if (d != 256 && d != 768 || n<0 || trained==0 || m>1 || m<0) throw bad();
        return new Header(d,m,n);
    }
    private static final int K=8;
    /** Blend all original index vectors via IVF nearest-neighbor retrieval (FAISS nprobe). */
    public static float[] blend(File file, float[] features, int frames, int dims,
                                float[] pitch, int percent, int nprobe, int protectPercent) throws IOException {
        if (percent<=0 || !file.isFile()) return features;
        if (frames<=0 || dims<=0 || features.length!=frames*dims) throw new IOException("Invalid RVC features shape");
        try (RvcFaissIndex index=new RvcFaissIndex(file)) {
            if (index.dim!=dims) throw new IOException("Размерность индекса ("+index.dim+") отличается от ContentVec ("+dims+")");
            float[] out=features.clone();
            float strength=Math.min(1f,Math.max(0f,percent/100f));
            float consonantProtect=Math.min(1f,Math.max(0f,protectPercent/100f));
            int probes=Math.max(1,Math.min(index.lists,nprobe));
            int[] centroidsTop=new int[probes];
            float[] centroidsDist=new float[probes];
            int[] bestOffsets=new int[K];
            float[] bestDistances=new float[K];
            for(int frame=0;frame<frames;frame++) {
                int base=frame*dims;
                float mix=strength;
                // Reduce retrieval on unvoiced consonants; protect=100 -> original features.
                if(pitch != null && pitch.length>0 &&
                    (pitch[Math.min(pitch.length-1,frame*2)]<=0.0f)) mix*=1f-consonantProtect;
                if(mix<=0.001f) continue;
                Arrays.fill(bestOffsets,-1);
                Arrays.fill(bestDistances,Float.POSITIVE_INFINITY);
                if(index.centroids!=null) {
                    Arrays.fill(centroidsTop,-1);
                    Arrays.fill(centroidsDist,Float.POSITIVE_INFINITY);
                    for(int l=0;l<index.lists;l++) {
                        float dist=0f;
                        int cbase=l*dims;
                        for(int d=0;d<dims;d++) {
                            float v=features[base+d];
                            float x=index.centroids[cbase+d];
                            dist+=index.metric==1?(v-x)*(v-x): -v*x;
                        }
                        if(dist>=centroidsDist[probes-1]) continue;
                        int j=probes-1;
                        while(j>0 && dist<centroidsDist[j-1]) {centroidsDist[j]=centroidsDist[j-1];centroidsTop[j]=centroidsTop[j-1];j--;}
                        centroidsDist[j]=dist;centroidsTop[j]=l;
                    }
                } else centroidsTop[0]=0;
                for(int pi=0;pi<probes;pi++) {
                    int l=centroidsTop[pi];
                    if(l<0) continue;
                    int n=index.listSizes[l];
                    long offset=index.listData[l];
                    for(int v=0;v<n;v++) {
                        int vbase=(int)(offset+(long)v*index.codeSize);
                        float dist=0f;
                        for(int d=0;d<dims;d++) {
                            float fv=features[base+d];
                            float iv=index.map.getFloat(vbase+d*4);
                            float delta=fv-iv;
                            dist+=index.metric==1?delta*delta: -fv*iv;
                        }
                        if(dist>=bestDistances[K-1]) continue;
                        int j=K-1;
                        while(j>0 && dist<bestDistances[j-1]) {bestDistances[j]=bestDistances[j-1];bestOffsets[j]=bestOffsets[j-1];j--;}
                        bestDistances[j]=dist;bestOffsets[j]=vbase;
                    }
                }
                // RVC uses inverse-square-distance weights for its eight nearest neighbours.
                double sum=0;
                double[] weights=new double[K];
                for(int j=0;j<K;j++) {
                    if(bestOffsets[j]<0) continue;
                    float dist=bestDistances[j];
                    double inv=1.0/Math.max(1.0e-8, Math.abs((double)dist)*Math.abs((double)dist));
                    weights[j]=inv;sum+=inv;
                }
                if(sum<=0 || !Double.isFinite(sum)) continue;
                for(int d=0;d<dims;d++) {
                    double retrieved=0;
                    for(int j=0;j<K;j++) if(bestOffsets[j]>=0) {
                        retrieved+=(weights[j]/sum)*index.map.getFloat(bestOffsets[j]+d*4);
                    }
                    out[base+d]=(float)(features[base+d]*(1-mix)+retrieved*mix);
                }
            }
            return out;
        }
    }
    @Override public void close() throws IOException {
        try {channel.close();} finally {raf.close();}
    }
}
