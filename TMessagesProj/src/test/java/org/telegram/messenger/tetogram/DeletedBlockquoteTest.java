package org.telegram.messenger.tetogram;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

public class DeletedBlockquoteTest {
    private String span(String text,TLRPC.MessageEntity e) {return text.substring(e.offset,e.offset+e.length);}
    private TLRPC.MessageEntity entity(ArrayList<TLRPC.MessageEntity> list,Class<?> type,int offset) {
        for(TLRPC.MessageEntity e:list)if(type.isInstance(e) && e.offset==offset)return e;
        throw new AssertionError("Entity missing: "+type.getSimpleName()+" at "+offset);
    }
    @Test public void quoteContainsAuthorAndSourceThenOneSeparator() {
        String source="tea 😀",body="answer";
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author 😀",source,body);
        ArrayList<TLRPC.MessageEntity> all=DeletedReplyProtocol.formatEntities(null,r);
        assertEquals("Author 😀\ntea 😀\nanswer",r.text);assertEquals(2,all.size());
        assertEquals("Author 😀",span(r.text,entity(all,TLRPC.TL_messageEntityBold.class,0)));
        TLRPC.MessageEntity quote=entity(all,TLRPC.TL_messageEntityBlockquote.class,r.quoteOffset);
        assertEquals("Author 😀\n"+source,span(r.text,quote));assertFalse(quote.collapsed);
        assertEquals("Author 😀\n".length()+source.length(),quote.length);assertEquals(quote.offset+quote.length+1,r.entityShift);
        assertEquals(body,r.text.substring(r.entityShift));assertEquals("tea 😀",source);
    }
    @Test public void multilineQuoteUsesUtf16WithAuthorInEveryChat() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Name 😀","😀\nчай","О");
        ArrayList<TLRPC.MessageEntity> all=DeletedReplyProtocol.formatEntities(null,r);
        assertEquals(2,all.size());
        TLRPC.MessageEntity q=entity(all,TLRPC.TL_messageEntityBlockquote.class,0);
        assertEquals("Name 😀",span(r.text,entity(all,TLRPC.TL_messageEntityBold.class,0)));
        assertEquals(0,q.offset);assertEquals(14,q.length);
        assertEquals("Name 😀\n😀\nчай",span(r.text,q));assertEquals("О",r.text.substring(r.entityShift));
    }
    @Test public void usersCodeQuoteAndBoldRemainOutsideGeneratedQuote() {
        String body="code\nquote\nbold";
        ArrayList<TLRPC.MessageEntity> original=new ArrayList<>();
        TLRPC.TL_messageEntityPre pre=new TLRPC.TL_messageEntityPre();pre.offset=0;pre.length=4;pre.language="java";original.add(pre);
        TLRPC.TL_messageEntityBlockquote ownQuote=new TLRPC.TL_messageEntityBlockquote();ownQuote.offset=5;ownQuote.length=5;ownQuote.collapsed=true;original.add(ownQuote);
        TLRPC.TL_messageEntityBold bold=new TLRPC.TL_messageEntityBold();bold.offset=11;bold.length=4;original.add(bold);
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source",body);
        ArrayList<TLRPC.MessageEntity> all=DeletedReplyProtocol.formatEntities(original,r);
        assertEquals(5,all.size());assertEquals(body,r.text.substring(r.entityShift));
        for(int i=0;i<original.size();i++) {
            TLRPC.MessageEntity old=original.get(i),copy=entity(all,old.getClass(),old.offset+r.entityShift);
            assertNotSame(old,copy);assertEquals(old.length,copy.length);
            assertEquals(body.substring(old.offset,old.offset+old.length),span(r.text,copy));
            assertTrue(copy.offset>=r.entityShift);assertTrue(copy.offset>=r.quoteOffset+r.quoteLength+1);
        }
        assertEquals("java",entity(all,TLRPC.TL_messageEntityPre.class,r.entityShift).language);
        assertTrue(entity(all,TLRPC.TL_messageEntityBlockquote.class,r.entityShift+5).collapsed);
        assertEquals(0,pre.offset);assertEquals(5,ownQuote.offset);assertEquals(11,bold.offset);
    }
    @Test public void trailingWhitespaceIsNotIncludedAndInternalLineBreaksRemain() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author  ","one\r\ntwo \n\t","\nanswer\n");
        ArrayList<TLRPC.MessageEntity> all=DeletedReplyProtocol.formatEntities(null,r);
        assertEquals("Author",span(r.text,entity(all,TLRPC.TL_messageEntityBold.class,0)));
        assertEquals("Author\none\ntwo",span(r.text,entity(all,TLRPC.TL_messageEntityBlockquote.class,0)));
        assertEquals("\nanswer\n",r.text.substring(r.entityShift));
    }
    @Test public void emptyCaptionStillContainsRealBlockquoteEntity() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","Photo","");
        ArrayList<TLRPC.MessageEntity> all=DeletedReplyProtocol.formatEntities(null,r);
        assertEquals("Author\nPhoto",r.text);assertEquals(2,all.size());
        assertEquals("Author\nPhoto",span(r.text,entity(all,TLRPC.TL_messageEntityBlockquote.class,0)));
        assertEquals(0,r.entityShift);
    }
    @Test public void generatedEntitiesRoundTripThroughTelegramTlSerialization() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Name 😀","source 😀","body");
        for(TLRPC.MessageEntity e:DeletedReplyProtocol.formatEntities(null,r)) {
            SerializedData out=new SerializedData();e.serializeToStream(out);
            SerializedData in=new SerializedData(out.toByteArray());
            TLRPC.MessageEntity restored=TLRPC.MessageEntity.TLdeserialize(in,in.readInt32(true),true);
            assertEquals(e.getClass(),restored.getClass());assertEquals(e.offset,restored.offset);assertEquals(e.length,restored.length);
            assertEquals(span(r.text,e),span(r.text,restored));
        }
    }
    @Test public void repeatedPreparationDoesNotAccumulateEntitiesOrModifySource() {
        TLRPC.TL_messageEntityItalic italic=new TLRPC.TL_messageEntityItalic();italic.offset=0;italic.length=4;
        ArrayList<TLRPC.MessageEntity> original=new ArrayList<>();original.add(italic);
        for(int i=0;i<5;i++) {
            DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source","body");
            ArrayList<TLRPC.MessageEntity> all=DeletedReplyProtocol.formatEntities(original,r);
            assertEquals(3,all.size());assertEquals("body",span(r.text,entity(all,TLRPC.TL_messageEntityItalic.class,r.entityShift)));
            assertEquals(1,original.size());assertEquals(0,italic.offset);
        }
        assertFalse(DeletedReplyPolicy.shouldConvert(false,false));assertFalse(DeletedReplyPolicy.shouldConvert(true,true));
    }
    @Test public void whitespaceOnlySnippetHasNonemptyVisibleRange() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author"," \n\t","body");
        assertEquals("Author\n…",span(r.text,entity(DeletedReplyProtocol.formatEntities(null,r),TLRPC.TL_messageEntityBlockquote.class,0)));
    }
}
