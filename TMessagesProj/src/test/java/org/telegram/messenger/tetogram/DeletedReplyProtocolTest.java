package org.telegram.messenger.tetogram;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

/** Real Telegram TL entities/serialization, no account, SDK UI or network. */
public class DeletedReplyProtocolTest {
    @Test public void actualEntitiesKeepFormattingAndPayloadAfterUnicodePrefix() {
        String body="😀bold link";
        DeletedReplyPolicy.Result result=DeletedReplyPolicy.prepare("Author 😀","source",body,true);
        ArrayList<TLRPC.MessageEntity> entities=new ArrayList<>();
        TLRPC.TL_messageEntityBold bold=new TLRPC.TL_messageEntityBold();bold.offset=2;bold.length=4;entities.add(bold);
        TLRPC.TL_messageEntityTextUrl link=new TLRPC.TL_messageEntityTextUrl();link.offset=7;link.length=4;link.url="https://example.com/fixture";entities.add(link);
        TLRPC.TL_messageEntityCustomEmoji emoji=new TLRPC.TL_messageEntityCustomEmoji();emoji.offset=0;emoji.length=2;emoji.document_id=12345;entities.add(emoji);
        ArrayList<TLRPC.MessageEntity> shifted=DeletedReplyProtocol.shiftEntities(entities,result.entityShift);
        assertEquals("bold",result.text.substring(shifted.get(0).offset,shifted.get(0).offset+shifted.get(0).length));
        assertEquals(link.url,shifted.get(1).url);assertEquals(12345L,((TLRPC.TL_messageEntityCustomEmoji)shifted.get(2)).document_id);
        assertTrue(shifted.get(0) instanceof TLRPC.TL_messageEntityBold);assertTrue(shifted.get(2) instanceof TLRPC.TL_messageEntityCustomEmoji);
        assertEquals(2,bold.offset);assertEquals(7,link.offset);assertEquals(0,emoji.offset);assertNotSame(emoji,shifted.get(2));
    }
    @Test public void forumRootSurvivesNativeSerializationAndDeletedIdIsAbsent() {
        TLRPC.TL_messageReplyHeader header=DeletedReplyProtocol.forumReply(77);
        SerializedData out=new SerializedData();header.serializeToStream(out);
        SerializedData in=new SerializedData(out.toByteArray());
        TLRPC.MessageReplyHeader restored=TLRPC.MessageReplyHeader.TLdeserialize(in,in.readInt32(true),true);
        assertEquals(77,restored.reply_to_msg_id);assertEquals(77,restored.reply_to_top_id);assertTrue(restored.forum_topic);
        assertEquals(16|8|2,restored.flags & (16|8|2));
    }
    @Test(expected=IllegalArgumentException.class) public void packedEphemeralIdCannotBecomeForumRoot() {
        DeletedReplyProtocol.forumReply(0x60000001);
    }
}
