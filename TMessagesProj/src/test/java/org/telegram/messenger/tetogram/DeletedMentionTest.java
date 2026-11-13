package org.telegram.messenger.tetogram;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

/** Tests actual wire entities and the user ID read by native URLSpanUserMention. */
public class DeletedMentionTest {
    private TLRPC.TL_inputUser user(long id,long hash) {
        TLRPC.TL_inputUser u=new TLRPC.TL_inputUser();u.user_id=id;u.access_hash=hash;return u;
    }
    private TLRPC.MessageEntity mention(ArrayList<TLRPC.MessageEntity> entities) {
        for(TLRPC.MessageEntity e:entities)if(e instanceof TLRPC.TL_inputMessageEntityMentionName)return e;
        return null;
    }
    @Test public void authorMentionIsNestedInsideOneOuterQuote() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Name 😀","tea","answer");
        ArrayList<TLRPC.MessageEntity> all=DeletedReplyProtocol.formatEntities(null,r,123,456,user(123,789));
        assertEquals(3,all.size());assertTrue(all.get(0) instanceof TLRPC.TL_messageEntityBlockquote);
        assertTrue(all.get(1) instanceof TLRPC.TL_inputMessageEntityMentionName);assertTrue(all.get(2) instanceof TLRPC.TL_messageEntityBold);
        TLRPC.MessageEntity quote=all.get(0),m=all.get(1);
        assertEquals("Name 😀\ntea",r.text.substring(quote.offset,quote.offset+quote.length));
        assertEquals("Name 😀",r.text.substring(m.offset,m.offset+m.length));assertEquals(7,m.length);
        assertTrue(m.offset>=quote.offset);assertTrue(m.offset+m.length<=quote.offset+quote.length);
        assertEquals(quote.offset+quote.length+1,r.entityShift);assertEquals("answer",r.text.substring(r.entityShift));
        assertFalse(r.text.contains("\n\n"));
    }
    @Test public void wireMentionCarriesOriginalSenderIdAndAccessHash() {
        long id=123456789012L;
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source","body");
        TLRPC.TL_inputMessageEntityMentionName m=(TLRPC.TL_inputMessageEntityMentionName)mention(DeletedReplyProtocol.formatEntities(null,r,id,456,user(id,987)));
        SerializedData out=new SerializedData();m.serializeToStream(out);
        SerializedData in=new SerializedData(out.toByteArray());
        TLRPC.TL_inputMessageEntityMentionName restored=(TLRPC.TL_inputMessageEntityMentionName)TLRPC.MessageEntity.TLdeserialize(in,in.readInt32(true),true);
        assertEquals(id,restored.user_id.user_id);assertEquals(987,restored.user_id.access_hash);
        assertEquals(0,restored.offset);assertEquals(6,restored.length); // no newline/source/answer in clickable range
    }
    @Test public void unavailableWrongAndNonUserSendersHaveNoFakeMention() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source","body");
        assertNull(mention(DeletedReplyProtocol.formatEntities(null,r,0,456,null)));
        assertNull(mention(DeletedReplyProtocol.formatEntities(null,r,-123,456,user(123,789))));
        assertNull(mention(DeletedReplyProtocol.formatEntities(null,r,123,456,user(999,789))));
        assertNull(mention(DeletedReplyProtocol.formatEntities(null,r,123,456,user(123,0))));
        assertNull(mention(DeletedReplyProtocol.formatEntities(null,r,123,456,new TLRPC.TL_inputUserEmpty())));
        assertEquals("Author\nsource\nbody",r.text);
    }
    @Test public void deletedFromMessageReferenceIsNotUsedForMention() {
        TLRPC.TL_inputUserFromMessage ref=new TLRPC.TL_inputUserFromMessage();
        ref.user_id=123;ref.msg_id=77;ref.peer=new TLRPC.TL_inputPeerSelf();
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source","body");
        assertNull(mention(DeletedReplyProtocol.formatEntities(null,r,123,456,ref)));
    }
    @Test public void selfInputIsBoundToOwnUserAndHasIdForNativeLocalClick() {
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source","body");
        assertNull(mention(DeletedReplyProtocol.formatEntities(null,r,123,456,new TLRPC.TL_inputUserSelf())));
        TLRPC.TL_inputMessageEntityMentionName m=(TLRPC.TL_inputMessageEntityMentionName)mention(DeletedReplyProtocol.formatEntities(null,r,123,123,new TLRPC.TL_inputUserSelf()));
        assertTrue(m.user_id instanceof TLRPC.TL_inputUserSelf);assertEquals(123,m.user_id.user_id);
    }
    @Test public void originalInputUserAndAnswerEntitiesAreNotMutated() {
        TLRPC.TL_inputUser u=user(123,789);
        TLRPC.TL_messageEntityItalic answer=new TLRPC.TL_messageEntityItalic();answer.offset=0;answer.length=4;
        ArrayList<TLRPC.MessageEntity> body=new ArrayList<>();body.add(answer);
        DeletedReplyPolicy.Result r=DeletedReplyPolicy.prepare("Author","source","body");
        ArrayList<TLRPC.MessageEntity> all=DeletedReplyProtocol.formatEntities(body,r,123,456,u);
        TLRPC.TL_inputMessageEntityMentionName m=(TLRPC.TL_inputMessageEntityMentionName)mention(all);
        assertNotSame(u,m.user_id);assertEquals(123,u.user_id);assertEquals(789,u.access_hash);assertEquals(0,answer.offset);
        assertEquals(4,all.size());assertEquals(r.entityShift,all.get(3).offset);
    }
    @Test public void receivedMentionWireTypeKeepsTheSameProfileId() {
        // MTProto sends inputMessageEntityMentionName and receives messageEntityMentionName.
        TLRPC.TL_messageEntityMentionName received=new TLRPC.TL_messageEntityMentionName();
        received.offset=0;received.length=6;received.user_id=123;
        SerializedData out=new SerializedData();received.serializeToStream(out);
        SerializedData in=new SerializedData(out.toByteArray());
        TLRPC.TL_messageEntityMentionName restored=(TLRPC.TL_messageEntityMentionName)TLRPC.MessageEntity.TLdeserialize(in,in.readInt32(true),true);
        assertEquals(123,restored.user_id);assertEquals(6,restored.length);
    }
}
