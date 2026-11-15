package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
import org.telegram.tgnet.TLRPC;
public class EditHistoryContentTest {
 private TLRPC.Message text(String text){TLRPC.TL_message m=new TLRPC.TL_message();m.message=text;m.media=new TLRPC.TL_messageMediaEmpty();return m;}
 @Test public void textChangeIsDistinct() throws Exception{assertNotEquals(EditHistoryContent.fingerprint(text("A")),EditHistoryContent.fingerprint(text("B")));}
 @Test public void emojiAndCombiningTextSurviveDigestWithoutMutation() throws Exception{String value="🙂 👨‍👩‍👧‍👦 ё\nnew";TLRPC.Message m=text(value);assertEquals(EditHistoryContent.fingerprint(m),EditHistoryContent.fingerprint(text(value)));assertEquals(value,m.message);}
 @Test public void sameTextWithBoldIsARealChange() throws Exception{TLRPC.Message a=text("hello"),b=text("hello");TLRPC.TL_messageEntityBold bold=new TLRPC.TL_messageEntityBold();bold.offset=0;bold.length=5;b.entities.add(bold);assertNotEquals(EditHistoryContent.fingerprint(a),EditHistoryContent.fingerprint(b));assertEquals(0,bold.offset);assertEquals(5,bold.length);}
 @Test public void urlsAndMentionsPreserveEntityPayload() throws Exception{TLRPC.Message a=text("link"),b=text("link");TLRPC.TL_messageEntityTextUrl link=new TLRPC.TL_messageEntityTextUrl();link.offset=0;link.length=4;link.url="https://example.com/one";b.entities.add(link);String before=EditHistoryContent.fingerprint(b);link.url="https://example.com/two";assertNotEquals(before,EditHistoryContent.fingerprint(b));assertNotEquals(EditHistoryContent.fingerprint(a),before);}
 @Test public void deliveryReadAndViewMetadataDoNotCreateVersions() throws Exception{TLRPC.Message m=text("same");String before=EditHistoryContent.fingerprint(m);m.views=999;m.unread=true;m.edit_date=100;m.id=321;m.date=987;assertEquals(before,EditHistoryContent.fingerprint(m));}
 @Test public void fileReferenceRefreshIsIgnored() throws Exception{TLRPC.Message m=text("caption");m.media=new TLRPC.TL_messageMediaPhoto();m.media.photo=new TLRPC.TL_photo();m.media.photo.id=99;String before=EditHistoryContent.fingerprint(m);m.media.photo.file_reference=new byte[]{1,2,3};assertEquals(before,EditHistoryContent.fingerprint(m));}
 @Test public void changedAttachmentWithSameCaptionIsDistinct() throws Exception{TLRPC.Message m=text("caption");m.media=new TLRPC.TL_messageMediaDocument();m.media.document=new TLRPC.TL_document();m.media.document.id=77;String before=EditHistoryContent.fingerprint(m);m.media.document.id=78;assertNotEquals(before,EditHistoryContent.fingerprint(m));}
 @Test public void webpageArrivalDoesNotCreateFalseEdit() throws Exception{TLRPC.Message m=text("https://example.com/");String before=EditHistoryContent.fingerprint(m);m.media=new TLRPC.TL_messageMediaWebPage();assertEquals(before,EditHistoryContent.fingerprint(m));}
 @Test public void nullAndEmptyTextAreEquivalent() throws Exception{assertEquals(EditHistoryContent.fingerprint(text(null)),EditHistoryContent.fingerprint(text("")));}
 @Test public void longMultilineTextIsNotTruncated() throws Exception{String text=String.join("\n",java.util.Collections.nCopies(500,"version🙂"));TLRPC.Message m=text(text);String before=EditHistoryContent.fingerprint(m);m.message=text+"!";assertNotEquals(before,EditHistoryContent.fingerprint(m));}
}
