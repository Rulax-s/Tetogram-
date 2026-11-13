/* Tetogram, 2026. GNU GPL v2 or later. UI entities; canonical wire entities stay unchanged. */
package org.telegram.messenger.tetogram;
import java.util.ArrayList;
import org.telegram.tgnet.*;
public final class LocalReplyProtocol {
 private LocalReplyProtocol() {}
 public static ArrayList<TLRPC.MessageEntity> bodyEntities(ArrayList<TLRPC.MessageEntity> entities,int start,int length) {
  ArrayList<TLRPC.MessageEntity> result=new ArrayList<>();
  if(entities!=null)for(TLRPC.MessageEntity e:entities) {
   if(e.offset>=start && e.length>=0 && (long)e.offset+e.length<=length) {
    TLRPC.MessageEntity copy=TLObject.deepCopy(e,TLRPC.MessageEntity::TLdeserialize);copy.offset-=start;result.add(copy);
   }
  }
  return result;
 }
}
