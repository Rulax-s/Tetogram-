/* Tetogram, 2026. GNU GPL v2 or later. Android-independent parts of voice transcription. */
package org.telegram.messenger.tetogram;

public final class VoiceTranscriptionRules {
    private VoiceTranscriptionRules(){}
    public static final int TARGET_RATE=16000;

    /** Collapses whitespace and starts the text with a capital letter (small models give lower case without punctuation). */
    public static String tidy(String raw){
        if(raw==null)return "";String t=raw.trim().replaceAll("\\s+"," ");if(t.isEmpty())return t;
        int first=t.codePointAt(0);return new String(Character.toChars(Character.toUpperCase(first)))+t.substring(Character.charCount(first));
    }

    /** Interleaved PCM of any rate and channel count → 16 kHz mono, keeping the phase across chunks. */
    public static final class Resampler {
        private final int rate,channels;private double position;private short previous;private boolean hasPrevious;
        public Resampler(int rate,int channels){if(rate<=0||channels<=0)throw new IllegalArgumentException("Bad format");this.rate=rate;this.channels=channels;}
        public short[] process(short[] interleaved){
            int frames=interleaved.length/channels;if(frames==0)return new short[0];
            short[] mono=new short[frames];
            for(int f=0;f<frames;f++){int sum=0;for(int c=0;c<channels;c++)sum+=interleaved[f*channels+c];mono[f]=(short)(sum/channels);}
            double step=(double)rate/TARGET_RATE;
            // position is relative to this chunk's first frame; values in [-1,0) lie between the previous chunk's last frame and mono[0]
            short[] out=new short[(int)Math.ceil((frames-position)/step)+2];int n=0;
            while(position<=frames-1){
                int i=(int)Math.floor(position);double frac=position-i;
                short a=i<0?(hasPrevious?previous:mono[0]):mono[i];short b=i+1<frames?mono[i+1]:a;
                if(n==out.length){short[] grow=new short[out.length*2];System.arraycopy(out,0,grow,0,n);out=grow;}
                out[n++]=(short)Math.round(a+(b-a)*frac);position+=step;
            }
            position-=frames;previous=mono[frames-1];hasPrevious=true;
            if(n==out.length)return out;short[] exact=new short[n];System.arraycopy(out,0,exact,0,n);return exact;
        }
    }
}
