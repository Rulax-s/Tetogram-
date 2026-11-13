package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class LocalReplyPolicyTest {
 @Test public void prefixChecksDoNotModifyWireOrUnicode(){String wire="Автор 😀\nчай ☕\nОтвет 😀";int start=wire.indexOf("Ответ");String hash=LocalReplyPolicy.prefix(wire,start);assertEquals(64,hash.length());assertTrue(LocalReplyPolicy.valid(wire,start,hash));assertEquals("Ответ 😀",wire.substring(start));}
 @Test public void bodyEditKeepsOriginalLink(){String prefix="Author\nquote\n";String hash=LocalReplyPolicy.prefix(prefix+"first",prefix.length());assertTrue(LocalReplyPolicy.valid(prefix+"second",prefix.length(),hash));assertFalse(LocalReplyPolicy.valid("Other!\nquote\nsecond",prefix.length(),hash));}
 @Test public void surrogateBoundaryAndInvalidOffsetsFallBack(){assertEquals("",LocalReplyPolicy.prefix("😀x",1));assertEquals("",LocalReplyPolicy.prefix("x",-1));assertEquals("",LocalReplyPolicy.prefix("x",2));assertFalse(LocalReplyPolicy.valid("x",0,""));}
 @Test public void removedAnswerFallsBackToCanonicalQuote(){String prefix="Name\nquote\n";String hash=LocalReplyPolicy.prefix(prefix+"body",prefix.length());assertFalse(LocalReplyPolicy.valid(prefix,prefix.length(),hash));}
 @Test public void multipleRepliesHaveIndependentFingerprints(){assertNotEquals(LocalReplyPolicy.prefix("A\nfirst\nanswer",8),LocalReplyPolicy.prefix("B\nother\nanswer",8));}
}
