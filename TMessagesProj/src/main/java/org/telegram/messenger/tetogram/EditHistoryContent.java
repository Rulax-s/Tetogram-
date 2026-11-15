/* Tetogram, 2026. GPL v2 or later. Stable visible-content digest, no JNI or Android dependencies. */
package org.telegram.messenger.tetogram;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
public final class EditHistoryContent {
    private EditHistoryContent() {}
    private static void part(MessageDigest digest,byte[] bytes){int length=bytes.length;for(int shift=24;shift>=0;shift-=8)digest.update((byte)(length>>shift));digest.update(bytes);}
    public static String fingerprint(TLRPC.Message message) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        part(digest,(message.message==null?"":message.message).getBytes(StandardCharsets.UTF_8));
        if(message.entities!=null)for(TLRPC.MessageEntity entity:message.entities){SerializedData data=new SerializedData();try{entity.serializeToStream(data);part(digest,data.toByteArray());}finally{data.cleanup();}}
        TLRPC.MessageMedia media=message.media;
        String attachment="";
        if(media!=null&&!(media instanceof TLRPC.TL_messageMediaEmpty)&&!(media instanceof TLRPC.TL_messageMediaWebPage))attachment=media.getClass().getName()+":"+(media.photo==null?0:media.photo.id)+":"+(media.document==null?0:media.document.id);
        part(digest,attachment.getBytes(StandardCharsets.UTF_8));
        byte[] bytes=digest.digest();char[] hex=new char[bytes.length*2];String alphabet="0123456789abcdef";for(int i=0;i<bytes.length;i++){hex[i*2]=alphabet.charAt((bytes[i]>>>4)&15);hex[i*2+1]=alphabet.charAt(bytes[i]&15);}return new String(hex);
    }
}
