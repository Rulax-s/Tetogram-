package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceTranscriptionRulesTest {
    @Test public void tidyCapitalisesAndCollapses(){assertEquals("Привет как дела",VoiceTranscriptionRules.tidy("  привет   как\nдела "));assertEquals("",VoiceTranscriptionRules.tidy(null));assertEquals("",VoiceTranscriptionRules.tidy("   "));}
    @Test public void downsamples48kStereoToMono16k(){VoiceTranscriptionRules.Resampler r=new VoiceTranscriptionRules.Resampler(48000,2);int total=0;
        for(int chunk=0;chunk<10;chunk++){short[] in=new short[960*2];for(int i=0;i<in.length;i+=2){in[i]=1000;in[i+1]=3000;}short[] out=r.process(in);for(short v:out)assertEquals(2000,v);total+=out.length;}
        assertEquals(3200,total,1);}
    @Test public void keepsPhaseAcrossOddChunks(){VoiceTranscriptionRules.Resampler a=new VoiceTranscriptionRules.Resampler(44100,1),b=new VoiceTranscriptionRules.Resampler(44100,1);
        short[] all=new short[44100];for(int i=0;i<all.length;i++)all[i]=(short)(i%2000);
        short[] whole=a.process(all);int n=0;short[] parts=new short[whole.length+4];
        for(int off=0;off<all.length;off+=777){int len=Math.min(777,all.length-off);short[] c=new short[len];System.arraycopy(all,off,c,0,len);for(short v:b.process(c))parts[n++]=v;}
        assertEquals(whole.length,n,1);for(int i=0;i<Math.min(n,whole.length);i++)assertEquals(whole[i],parts[i],2);}
    @Test public void upsamples8k(){VoiceTranscriptionRules.Resampler r=new VoiceTranscriptionRules.Resampler(8000,1);short[] out=r.process(new short[]{0,100,200,300});assertEquals(7,out.length);assertEquals(50,out[1]);}
    @Test(expected=IllegalArgumentException.class) public void rejectsBadFormat(){new VoiceTranscriptionRules.Resampler(0,1);}
}
