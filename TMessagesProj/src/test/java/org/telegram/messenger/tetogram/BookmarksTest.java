package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class BookmarksTest {
    @Test public void addPutsNewestFirstAndRefreshesDuplicates(){
        Bookmarks b=new Bookmarks();b.add(-5,10,1,100,"first",1);b.add(-5,11,2,101,"second",2);b.add(-5,10,1,100,"first again",3);
        assertEquals(2,b.items.size());assertEquals(10,b.items.get(0).message);assertEquals("first again",b.items.get(0).preview);assertEquals(3,b.items.get(0).added);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsMissingMessage(){new Bookmarks().add(-5,0,1,1,"x",1);}
    @Test public void removeAndNote(){Bookmarks b=new Bookmarks();b.add(7,1,7,1,"hi",1);b.setNote(7,1,"важно");assertEquals("важно",b.find(7,1).note);assertTrue(b.remove(7,1));assertFalse(b.remove(7,1));assertTrue(b.items.isEmpty());}
    @Test public void perChatAndSearch(){Bookmarks b=new Bookmarks();b.add(-1,1,0,0,"Адрес встречи",1);b.add(-2,2,0,0,"пароль от wifi",2);b.add(-1,3,0,0,"мем",3);b.setNote(-1,3,"Смешной АДРЕС");
        assertEquals(2,b.forDialog(-1).size());assertEquals(2,b.search("адрес",0).size());assertEquals(1,b.search("WIFI",0).size());assertEquals(2,b.search("адрес",-1).size());assertEquals(3,b.search("  ",0).size());assertEquals(2,b.removeDialog(-1));}
    @Test public void previewIsClippedOnCodePointsAndFlattened(){String emoji=new String(new char[300]).replace("\0","🙂");Bookmarks b=new Bookmarks();b.add(1,1,0,0,"a\nb",1);assertEquals("a b",b.items.get(0).preview);b.add(1,2,0,0,emoji,2);String p=b.items.get(0).preview;assertTrue(p.endsWith("…"));assertEquals(Bookmarks.PREVIEW+1,p.codePointCount(0,p.length()));}
    @Test public void capsTheList(){Bookmarks b=new Bookmarks();for(int i=1;i<=Bookmarks.MAX_ITEMS+10;i++)b.add(1,i,0,0,"",i);assertEquals(Bookmarks.MAX_ITEMS,b.items.size());assertEquals(Bookmarks.MAX_ITEMS+10,b.items.get(0).message);assertFalse(b.contains(1,1));}
    @Test public void roundTripKeepsEverything(){Bookmarks b=new Bookmarks();b.add(-1001234567890L,42,99,1700000000,"Тето 👨‍👩‍👧 = 100%",5);b.setNote(-1001234567890L,42,"заметка\nвторая строка");b.add(5,1,5,1,"x",6);
        Bookmarks d=Bookmarks.decode(b.encode());assertEquals(2,d.items.size());Bookmarks.Item i=d.find(-1001234567890L,42);assertEquals("Тето 👨‍👩‍👧 = 100%",i.preview);assertEquals("заметка вторая строка",i.note);assertEquals(99,i.sender);assertEquals(1700000000,i.date);assertEquals(5,d.items.get(0).dialog);}
    @Test public void damagedEntryIsSkippedNotFatal(){Bookmarks b=new Bookmarks();b.add(1,1,0,0,"ok",1);b.add(2,2,0,0,"bad",2);String data=b.encode().replace("b.0.m=2","b.0.m=oops");Bookmarks d=Bookmarks.decode(data);assertEquals(1,d.items.size());assertEquals(1,d.items.get(0).dialog);}
    @Test(expected=IllegalArgumentException.class) public void futureSchemaIsNotOverwritten(){Bookmarks.decode("schema=2\ncount=0\n");}
    @Test public void emptyIsEmpty(){assertTrue(Bookmarks.decode(null).items.isEmpty());assertTrue(Bookmarks.decode("").items.isEmpty());}
}
