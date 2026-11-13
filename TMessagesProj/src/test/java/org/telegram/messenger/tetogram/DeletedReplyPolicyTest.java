package org.telegram.messenger.tetogram;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DeletedReplyPolicyTest {
    @Test public void authorIsAlwaysIncludedBeforeQuote() {
        assertEquals("Author\nfixture\n\nanswer",DeletedReplyPolicy.prepare("Author","fixture","answer").text);
        assertEquals("Author\nfixture\n\nanswer",DeletedReplyPolicy.prepare("Author","fixture","answer").text);
    }
    @Test public void shortQuoteHasTwentyCharactersIncludingEllipsis() {
        String q=DeletedReplyPolicy.shortify("1234567890123456789012345",20);
        assertEquals("1234567890123456789…",q);
        assertEquals("12345678901234567890",DeletedReplyPolicy.shortify("12345678901234567890",20));
    }
    @Test public void surrogatePairsNeverSplit() {
        String s="😀".repeat(21),q=DeletedReplyPolicy.shortify(s,20);
        assertEquals("😀".repeat(19)+"…",q);assertEquals(20,q.codePointCount(0,q.length()));
    }
    @Test public void familyFlagsAccentsAndSkinTonesStayWhole() {
        for(String cluster:new String[]{"👨‍👩‍👧‍👦","🇷🇺","e\u0301","👍🏽","1️⃣"})
            assertEquals(cluster.repeat(19)+"…",DeletedReplyPolicy.shortify(cluster.repeat(21),20));
    }
    @Test public void utf16OffsetIncludesAuthorEmojiAndBothSeparators() {
        String body="😀bold and link";
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("👩🏽‍💻 Author","source",body);
        assertEquals(body,r.text.substring(r.entityShift));
        assertEquals(r.text.indexOf(body),r.entityShift);
    }
    static class E {int offset,length;String type;E(int o,int l,String t){offset=o;length=l;type=t;}}
    static final DeletedReplyPolicy.Entity<E> ENTITIES=new DeletedReplyPolicy.Entity<E>() {
        public E copy(E e){return new E(e.offset,e.length,e.type);}public int offset(E e){return e.offset;}public void offset(E e,int o){e.offset=o;}
    };
    @Test public void formattingIsClonedAndShiftedWithoutMutatingCaller() {
        String body="😀bold link";
        ArrayList<E> original=new ArrayList<>(Arrays.asList(new E(2,4,"bold"),new E(7,4,"url")));
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source",body);
        ArrayList<E> shifted=DeletedReplyPolicy.shiftEntities(original,r.entityShift,ENTITIES);
        assertEquals("bold",r.text.substring(shifted.get(0).offset,shifted.get(0).offset+shifted.get(0).length));
        assertEquals("link",r.text.substring(shifted.get(1).offset,shifted.get(1).offset+shifted.get(1).length));
        assertNotSame(original.get(0),shifted.get(0));assertEquals(2,original.get(0).offset);assertEquals(7,original.get(1).offset);
    }
    @Test public void photoCaptionAndEmptyCaptionUseSamePrefix() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","Photo","my caption");
        assertEquals("Author\nPhoto\n\nmy caption",r.text);assertEquals("my caption",r.text.substring(r.entityShift));
        r=DeletedReplyPolicy.prepare("Author","Photo","");assertEquals("Author\nPhoto",r.text);assertEquals(0,r.entityShift);
    }
    @Test public void retryAndExistingServerMessageAreNotConverted() {
        assertTrue(DeletedReplyPolicy.shouldConvert(true,false));
        assertFalse(DeletedReplyPolicy.shouldConvert(true,true));
        assertFalse(DeletedReplyPolicy.shouldConvert(false,false));
    }
    @Test public void repeatedPreparationNeverPrefixesAnInputTwice() {
        String input="answer";E e=new E(0,6,"italic");
        for(int i=0;i<10;i++) {
            DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source",input);
            assertEquals("Author\nsource\n\nanswer",r.text);
            assertEquals(r.entityShift,DeletedReplyPolicy.shiftEntities(Arrays.asList(e),r.entityShift,ENTITIES).get(0).offset);
            assertEquals(0,e.offset);assertEquals("answer",input);
        }
    }
    @Test public void reusedAccountSlotCannotSendPreviousUsersQuote() {
        assertTrue(DeletedReplyPolicy.sameOwner(123,123));
        assertFalse(DeletedReplyPolicy.sameOwner(123,456));
        assertFalse(DeletedReplyPolicy.sameOwner(0,456));
    }
    @Test public void accountDialogSecretAndEphemeralBoundaries() {
        assertTrue(DeletedReplyPolicy.sameContext(1,-50,1,-50,false,false));
        assertFalse(DeletedReplyPolicy.sameContext(2,-50,1,-50,false,false));
        assertFalse(DeletedReplyPolicy.sameContext(1,-60,1,-50,false,false));
        assertFalse(DeletedReplyPolicy.sameContext(1,-50,1,-50,true,false));
        assertFalse(DeletedReplyPolicy.sameContext(1,-50,1,-50,false,true));
    }
    @Test public void independentRepliesKeepTheirOwnAuthorsAndText() {
        assertEquals("First\none\n\na",DeletedReplyPolicy.prepare("First","one","a").text);
        assertEquals("Second\ntwo\n\nb",DeletedReplyPolicy.prepare("Second","two","b").text);
    }
}
