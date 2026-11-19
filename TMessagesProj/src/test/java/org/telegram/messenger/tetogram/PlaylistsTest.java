package org.telegram.messenger.tetogram;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistsTest {
    static Playlists.Track t(long d,int m,String title){return Playlists.track(d,m,title,"Тето",180,m);}
    @Test public void createAddNoDuplicatesRemoveMove(){Playlists p=new Playlists();Playlists.Playlist l=p.create("Вечер",-1,1);assertEquals(6,l.color);
        assertTrue(p.add(l.id,t(-10,1,"a")));assertFalse(p.add(l.id,t(-10,1,"a")));p.add(l.id,t(-10,2,"b"));p.add(l.id,t(-11,3,"c"));
        p.move(l.id,2,0);assertEquals("c",l.tracks.get(0).title);assertTrue(p.remove(l.id,-10,1));assertFalse(p.remove(l.id,-10,1));assertEquals(2,l.tracks.size());assertEquals(360,l.totalSeconds());
        assertEquals(1,p.containing(-11,3).size());p.delete(l.id);assertNull(p.playlist(l.id));}
    @Test(expected=IllegalArgumentException.class) public void emptyNameRejected(){new Playlists().create("  ",0,1);}
    @Test(expected=IllegalArgumentException.class) public void trackNeedsMessage(){Playlists.track(-1,0,"x","",1,1);}
    @Test public void recentMovesToTopAndCountsPlays(){Playlists p=new Playlists();p.played(t(1,1,"a"));p.played(t(1,2,"b"));p.played(t(1,1,"a"));p.played(t(1,1,"a"));p.played(t(1,2,"b"));
        assertEquals(2,p.recent.size());assertEquals("b",p.recent.get(0).title);List<Playlists.Track> most=p.mostPlayed(10);assertEquals("a",most.get(0).title);assertEquals(2,most.size());
        for(int i=3;i<200;i++)p.played(t(1,i,"x"));assertEquals(Playlists.MAX_RECENT,p.recent.size());}
    @Test public void searchAndShuffle(){Playlists p=new Playlists();Playlists.Playlist l=p.create("x",0,1);for(int i=1;i<=20;i++)p.add(l.id,t(1,i,"Song "+i));
        assertEquals(11,Playlists.search(l.tracks,"song 1").size());assertEquals(20,Playlists.search(l.tracks,"тето").size());
        List<Playlists.Track> s=Playlists.shuffled(l.tracks,l.tracks.get(7),42);assertEquals(20,s.size());assertEquals(8,s.get(0).message);assertEquals(s,Playlists.shuffled(l.tracks,l.tracks.get(7),42));}
    @Test public void allTracksDedupesAcrossPlaylists(){Playlists p=new Playlists();int a=p.create("a",0,1).id,b=p.create("b",0,1).id;p.add(a,t(1,1,"x"));p.add(b,t(1,1,"x"));p.add(b,t(1,2,"y"));assertEquals(2,p.allTracks().size());assertEquals(2,p.allTracks().get(0).message);}
    @Test public void durationFormat(){assertEquals("3:05",Playlists.duration(185));assertEquals("1:00:01",Playlists.duration(3601));assertEquals("0:00",Playlists.duration(-5));}
    @Test public void roundTrip(){Playlists p=new Playlists();Playlists.Playlist l=p.create("Мой = плейлист 🎧",3,5);p.add(l.id,Playlists.track(-1001234567890L,77,"Песня\nдва","Исп",241,9));p.played(l.tracks.get(0));p.played(l.tracks.get(0));int deleted=p.create("tmp",0,1).id;p.delete(deleted);
        Playlists d=Playlists.decode(p.encode());Playlists.Playlist l2=d.playlist(l.id);assertEquals("Мой = плейлист 🎧",l2.name);assertEquals(3,l2.color);assertEquals("Песня два",l2.tracks.get(0).title);assertEquals(241,l2.tracks.get(0).duration);
        assertEquals(1,d.recent.size());assertEquals(Integer.valueOf(2),d.plays.get("-1001234567890:77"));assertTrue(d.create("new",0,1).id>deleted);}
    @Test public void damagedTrackSkipped(){Playlists p=new Playlists();int id=p.create("x",0,1).id;p.add(id,t(1,1,"a"));p.add(id,t(1,2,"b"));Playlists d=Playlists.decode(p.encode().replace("l.0.0.m=1","l.0.0.m=bad"));assertEquals(1,d.playlist(id).tracks.size());}
    @Test(expected=IllegalArgumentException.class) public void futureSchemaRejected(){Playlists.decode("schema=7\n");}
}
