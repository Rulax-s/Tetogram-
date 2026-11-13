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
    /** One outer quote contains the author mention/bold; the answer stays outside it. */
    public static ArrayList<TLRPC.MessageEntity> formatEntities(ArrayList<TLRPC.MessageEntity> entities,DeletedReplyPolicy.Result result) {
        return formatEntities(entities,result,0,0,null);
    }
    public static ArrayList<TLRPC.MessageEntity> formatEntities(ArrayList<TLRPC.MessageEntity> entities,DeletedReplyPolicy.Result result,
                                                               long senderId,long currentUserId,TLRPC.InputUser inputUser) {
        ArrayList<TLRPC.MessageEntity> formatted=shiftEntities(entities,result.entityShift);
        if(formatted==null)formatted=new ArrayList<>();
        TLRPC.TL_messageEntityBlockquote quote=new TLRPC.TL_messageEntityBlockquote();
        quote.offset=result.quoteOffset;
        quote.length=result.quoteLength;
        quote.collapsed=false;
        formatted.add(quote);
        if(result.authorLength>0) {
            TLRPC.InputUser resolved=mentionUser(senderId,currentUserId,inputUser);
            if(resolved!=null) {
                TLRPC.TL_inputMessageEntityMentionName mention=new TLRPC.TL_inputMessageEntityMentionName();
                mention.offset=0;mention.length=result.authorLength;mention.user_id=resolved;
                formatted.add(mention);
            }
            TLRPC.TL_messageEntityBold author=new TLRPC.TL_messageEntityBold();
            author.offset=0;author.length=result.authorLength;
            formatted.add(author);
        }
        // Outer quote, then continuous mention, then splittable bold (TDLib canonical nesting).
        java.util.Collections.sort(formatted,(a,b)-> {
            if(a.offset!=b.offset)return Integer.compare(a.offset,b.offset);
            if(a.length!=b.length)return Integer.compare(b.length,a.length);
            return Integer.compare(nestingPriority(a),nestingPriority(b));
        });
        return formatted;
    }
    private static int nestingPriority(TLRPC.MessageEntity e) {
        if(e instanceof TLRPC.TL_messageEntityBlockquote)return 0;
        if(e instanceof TLRPC.TL_inputMessageEntityMentionName || e instanceof TLRPC.TL_messageEntityMentionName)return 1;
        return 2;
    }
    private static TLRPC.InputUser mentionUser(long senderId,long currentUserId,TLRPC.InputUser input) {
        if(senderId<=0 || input==null)return null;
        if(input instanceof TLRPC.TL_inputUser && input.user_id==senderId && input.access_hash!=0)
            return TLObject.deepCopy(input,TLRPC.InputUser::TLdeserialize);
        if(input instanceof TLRPC.TL_inputUserSelf && senderId==currentUserId) {
            TLRPC.InputUser self=TLObject.deepCopy(input,TLRPC.InputUser::TLdeserialize);
            self.user_id=senderId; // used by native outgoing URLSpanUserMention; Self wire format stays unchanged
            return self;
        }
        // Empty/min peers and FromMessage refs to deleted content are not a valid profile link.
        return null;
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
