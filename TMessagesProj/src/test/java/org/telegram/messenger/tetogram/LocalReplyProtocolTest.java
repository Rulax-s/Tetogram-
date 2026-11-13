package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import org.telegram.tgnet.TLRPC;
public class LocalReplyProtocolTest {
 @Test public void localBodyKeepsEmojiFormattingAndCanonicalQuote(){
  DeletedReplyPolicy.Result wire=DeletedReplyPolicy.prepare("Автор 😀","чай","😀Ответ");
  ArrayList<TLRPC.MessageEntity> body=new ArrayList<>();
  TLRPC.TL_messageEntityBold bold=new TLRPC.TL_messageEntityBold();bold.offset=2;bold.length=5;body.add(bold);
  TLRPC.TL_messageEntityCustomEmoji emoji=new TLRPC.TL_messageEntityCustomEmoji();emoji.offset=0;emoji.length=2;emoji.document_id=123;body.add(emoji);
  ArrayList<TLRPC.MessageEntity> entities=DeletedReplyProtocol.formatEntities(body,wire);
  ArrayList<TLRPC.MessageEntity> display=LocalReplyProtocol.bodyEntities(entities,wire.entityShift,wire.text.length());
  assertEquals(2,display.size());assertEquals(0,display.get(0).offset);assertEquals(123,((TLRPC.TL_messageEntityCustomEmoji)display.get(0)).document_id);assertEquals(2,display.get(1).offset);
  assertTrue(entities.get(0) instanceof TLRPC.TL_messageEntityBlockquote);assertEquals(0,entities.get(0).offset);assertEquals(2,bold.offset);assertNotSame(body.get(0),display.get(1));
 }
 @Test public void userBlockquoteInAnswerSurvives(){
  ArrayList<TLRPC.MessageEntity> wire=new ArrayList<>();TLRPC.TL_messageEntityBlockquote outer=new TLRPC.TL_messageEntityBlockquote();outer.offset=0;outer.length=8;wire.add(outer);
  TLRPC.TL_messageEntityBlockquote user=new TLRPC.TL_messageEntityBlockquote();user.offset=9;user.length=4;wire.add(user);
  ArrayList<TLRPC.MessageEntity> display=LocalReplyProtocol.bodyEntities(wire,9,13);assertEquals(1,display.size());assertEquals(0,display.get(0).offset);assertEquals(4,display.get(0).length);assertEquals(9,user.offset);
 }
 @Test public void invalidOrCrossingRangesDoNotLeakIntoBody(){
  ArrayList<TLRPC.MessageEntity> wire=new ArrayList<>();TLRPC.TL_messageEntityBold bad=new TLRPC.TL_messageEntityBold();bad.offset=2;bad.length=Integer.MAX_VALUE;wire.add(bad);assertTrue(LocalReplyProtocol.bodyEntities(wire,2,10).isEmpty());assertTrue(LocalReplyProtocol.bodyEntities(null,2,10).isEmpty());
 }
}
