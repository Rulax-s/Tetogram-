package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChatTagsTest {
    @Test public void addAssignAndOrder(){ChatTags c=new ChatTags();ChatTags.Tag work=c.add("Работа",1),wait=c.add("Ждёт ответа",-1);
        assertEquals(6,wait.color);c.set(-5,wait.id,true);c.set(-5,work.id,true);c.set(-5,work.id,true);
        assertEquals(2,c.of(-5).size());assertEquals("Работа",c.of(-5).get(0).name);c.move(1,0);assertEquals("Ждёт ответа",c.of(-5).get(0).name);
        assertEquals(1,c.chatsWith(work.id).size());assertEquals(Integer.valueOf(1),c.counts().get(work.id));}
    @Test public void unsetAndDeleteCleanUp(){ChatTags c=new ChatTags();ChatTags.Tag a=c.add("a",0),b=c.add("b",0);c.set(1,a.id,true);c.set(2,a.id,true);c.set(2,b.id,true);
        c.set(1,a.id,false);assertFalse(c.chats.containsKey(1L));c.delete(a.id);assertNull(c.tag(a.id));assertEquals(1,c.of(2).size());c.delete(b.id);assertTrue(c.chats.isEmpty());}
    @Test public void unknownTagIsIgnored(){ChatTags c=new ChatTags();c.set(1,99,true);assertTrue(c.chats.isEmpty());}
    @Test(expected=IllegalArgumentException.class) public void emptyNameRejected(){new ChatTags().add("   ",0);}
    @Test public void nameClippedOnCodePoints(){String s=new String(new char[40]).replace("\0","🙂");ChatTags.Tag t=new ChatTags().add(s,0);assertEquals(ChatTags.MAX_NAME,t.name.codePointCount(0,t.name.length()));}
    @Test public void roundTripKeepsIdsAndNeverReusesThem(){ChatTags c=new ChatTags();ChatTags.Tag a=c.add("Семья 👨‍👩‍👧",3);ChatTags.Tag b=c.add("x=y",5);c.set(-1001234567890L,b.id,true);c.set(-1001234567890L,a.id,true);c.delete(b.id);c.set(7,a.id,true);
        ChatTags d=ChatTags.decode(c.encode());assertEquals(1,d.tags.size());assertEquals("Семья 👨‍👩‍👧",d.tag(a.id).name);assertEquals(3,d.tag(a.id).color);assertTrue(d.has(-1001234567890L,a.id));assertTrue(d.has(7,a.id));
        assertTrue(d.add("new",0).id>b.id);}
    @Test public void damagedEntriesSkipped(){ChatTags c=new ChatTags();ChatTags.Tag a=c.add("a",0);c.set(1,a.id,true);String data=c.encode().replace("c.0.d=1","c.0.d=oops");ChatTags d=ChatTags.decode(data);assertEquals(1,d.tags.size());assertTrue(d.chats.isEmpty());}
    @Test(expected=IllegalArgumentException.class) public void futureSchemaRejected(){ChatTags.decode("schema=9\n");}
    @Test public void emptyIsEmpty(){assertTrue(ChatTags.decode("").tags.isEmpty());assertTrue(ChatTags.decode(null).chats.isEmpty());}
}
