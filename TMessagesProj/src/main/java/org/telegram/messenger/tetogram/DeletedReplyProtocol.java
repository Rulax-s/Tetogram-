/* Tetogram, 2026. GNU GPL v2 or later. Native TL boundary for deleted replies. */
package org.telegram.messenger.tetogram;

import java.util.ArrayList;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public final class DeletedReplyProtocol {
    private DeletedReplyProtocol() {}
    public static ArrayList<TLRPC.MessageEntity> shiftEntities(ArrayList<TLRPC.MessageEntity> entities,int shift) {
        return DeletedReplyPolicy.shiftEntities(entities,shift,new DeletedReplyPolicy.Entity<TLRPC.MessageEntity>() {
            public TLRPC.MessageEntity copy(TLRPC.MessageEntity e) { return TLObject.deepCopy(e,TLRPC.MessageEntity::TLdeserialize); }
            public int offset(TLRPC.MessageEntity e) { return e.offset; }
            public void offset(TLRPC.MessageEntity e,int value) { e.offset=value; }
        });
    }
    public static TLRPC.TL_messageReplyHeader forumReply(int rootId) {
        if(rootId<=0 || MessageTimelinePolicy.ephemeral(rootId))throw new IllegalArgumentException("Invalid forum root");
        if(rootId==1)return null; // Native Telegram routes General implicitly; message 1 need not exist.
        TLRPC.TL_messageReplyHeader header=new TLRPC.TL_messageReplyHeader();
        header.reply_to_msg_id=rootId;
        header.reply_to_top_id=rootId;
        header.forum_topic=true;
        header.flags|=2|8|16;
        return header;
    }
}
