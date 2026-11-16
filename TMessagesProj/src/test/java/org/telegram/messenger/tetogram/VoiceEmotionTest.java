package org.telegram.messenger.tetogram;

import org.junit.Test;
import static org.junit.Assert.*;

public class VoiceEmotionTest {
    @Test public void stripsOnlyKnownControlEmoji() {
        VoiceEmotion.Parsed p = VoiceEmotion.parse("😊 Привет! 🤫 Секрет ❤️");
        assertEquals("happy", p.emotion);
        assertEquals("Привет! Секрет ❤️", p.text);
    }
    @Test public void keepsPlainText() {
        VoiceEmotion.Parsed p = VoiceEmotion.parse("Привет, мир!");
        assertEquals("neutral", p.emotion);
        assertEquals("Привет, мир!", p.text);
    }
    @Test public void handlesNull() {
        assertEquals("", VoiceEmotion.parse(null).text);
    }
    @Test public void sadSpeechIsSlower() {
        assertTrue(VoiceEmotion.speed("sad") < VoiceEmotion.speed("neutral"));
    }
}
