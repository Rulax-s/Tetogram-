/* Tetogram, 2026. GNU GPL v2 or later. Music playlists; Android-independent model and codec. */
package org.telegram.messenger.tetogram;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

/** Playlists keep references (chat, message) plus title/performer for the list; the audio stays in Telegram. */
public final class Playlists {
    public static final int SCHEMA=1,MAX_PLAYLISTS=100,MAX_TRACKS=2000,MAX_RECENT=100,MAX_TEXT=120,COLORS=7;

    public static final class Track {
        public long dialog;public int message;public String title="",performer="";public int duration;public long added;
        public boolean same(long d,int m){return dialog==d&&message==m;}
        public String key(){return dialog+":"+message;}
        Track copy(){Track t=new Track();t.dialog=dialog;t.message=message;t.title=title;t.performer=performer;t.duration=duration;t.added=added;return t;}
    }
    public static final class Playlist {
        public int id,color;public String name="";public long created;public final ArrayList<Track> tracks=new ArrayList<>();
        public int indexOf(long d,int m){for(int i=0;i<tracks.size();i++)if(tracks.get(i).same(d,m))return i;return -1;}
        public boolean contains(long d,int m){return indexOf(d,m)>=0;}
        public int totalSeconds(){int s=0;for(Track t:tracks)s+=Math.max(0,t.duration);return s;}
    }

    public final ArrayList<Playlist> playlists=new ArrayList<>();
    /** Newest first; each track once. */
    public final ArrayList<Track> recent=new ArrayList<>();
    /** Times each track ("dialog:message") was started. */
    public final HashMap<String,Integer> plays=new HashMap<>();
    private int nextId=1;

    public static Track track(long dialog,int message,String title,String performer,int duration,long now){
        if(dialog==0||message<=0)throw new IllegalArgumentException("No message");
        Track t=new Track();t.dialog=dialog;t.message=message;t.title=clip(title);t.performer=clip(performer);t.duration=Math.max(0,duration);t.added=now;return t;
    }

    public Playlist playlist(int id){for(Playlist p:playlists)if(p.id==id)return p;return null;}
    public Playlist create(String name,int color,long now){
        String n=clip(name);if(n.isEmpty())throw new IllegalArgumentException("Empty name");
        if(playlists.size()>=MAX_PLAYLISTS)throw new IllegalStateException("Too many playlists");
        Playlist p=new Playlist();p.id=nextId++;p.name=n;p.color=color(color);p.created=now;playlists.add(p);return p;
    }
    public void rename(int id,String name,int color){Playlist p=playlist(id);String n=clip(name);if(p==null||n.isEmpty())return;p.name=n;p.color=color(color);}
    public void delete(int id){playlists.remove(playlist(id));}
    /** Adds at the end; a track already in the playlist is not duplicated. Returns false when nothing changed. */
    public boolean add(int id,Track t){Playlist p=playlist(id);if(p==null||t==null||p.contains(t.dialog,t.message)||p.tracks.size()>=MAX_TRACKS)return false;p.tracks.add(t.copy());return true;}
    public boolean remove(int id,long dialog,int message){Playlist p=playlist(id);if(p==null)return false;int i=p.indexOf(dialog,message);if(i<0)return false;p.tracks.remove(i);return true;}
    public void move(int id,int from,int to){Playlist p=playlist(id);if(p==null||from<0||to<0||from>=p.tracks.size()||to>=p.tracks.size())return;p.tracks.add(to,p.tracks.remove(from));}
    public List<Playlist> containing(long dialog,int message){ArrayList<Playlist> out=new ArrayList<>();for(Playlist p:playlists)if(p.contains(dialog,message))out.add(p);return out;}

    /** A track started playing: it moves to the top of "Recently played" and its play count grows. */
    public void played(Track t){
        if(t==null)return;for(int i=0;i<recent.size();i++)if(recent.get(i).same(t.dialog,t.message)){recent.remove(i);break;}
        recent.add(0,t.copy());while(recent.size()>MAX_RECENT)recent.remove(recent.size()-1);
        Integer n=plays.get(t.key());plays.put(t.key(),n==null?1:n+1);
        if(plays.size()>MAX_TRACKS*2){String least=null;int min=Integer.MAX_VALUE;for(java.util.Map.Entry<String,Integer> e:plays.entrySet())if(e.getValue()<min){min=e.getValue();least=e.getKey();}plays.remove(least);}
    }
    /** "Played most": recent tracks played at least twice, by play count. */
    public List<Track> mostPlayed(int limit){
        ArrayList<Track> out=new ArrayList<>();for(Track t:recent){Integer n=plays.get(t.key());if(n!=null&&n>=2)out.add(t);}
        Collections.sort(out,(a,b)->Integer.compare(plays.get(b.key()),plays.get(a.key())));
        return out.size()>limit?new ArrayList<>(out.subList(0,limit)):out;
    }
    /** Every track in any playlist, newest added first, each once. */
    public List<Track> allTracks(){
        ArrayList<Track> out=new ArrayList<>();java.util.HashSet<String> seen=new java.util.HashSet<>();
        for(Playlist p:playlists)for(Track t:p.tracks)if(seen.add(t.key()))out.add(t);
        Collections.sort(out,Comparator.comparingLong((Track t)->t.added).reversed());return out;
    }
    public static List<Track> search(List<Track> tracks,String query){
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);if(q.isEmpty())return new ArrayList<>(tracks);
        ArrayList<Track> out=new ArrayList<>();for(Track t:tracks)if(t.title.toLowerCase(Locale.ROOT).contains(q)||t.performer.toLowerCase(Locale.ROOT).contains(q))out.add(t);return out;
    }
    /** Shuffled play order that starts with {@code first} when it is given. */
    public static List<Track> shuffled(List<Track> tracks,Track first,long seed){
        ArrayList<Track> out=new ArrayList<>(tracks);Collections.shuffle(out,new Random(seed));
        if(first!=null)for(int i=0;i<out.size();i++)if(out.get(i).same(first.dialog,first.message)){out.add(0,out.remove(i));break;}
        return out;
    }
    public static String duration(int seconds){int s=Math.max(0,seconds);int h=s/3600,m=s%3600/60,r=s%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,r):String.format(Locale.ROOT,"%d:%02d",m,r);}

    static int color(int c){int m=c%COLORS;return m<0?m+COLORS:m;}
    static String clip(String s){if(s==null)return "";String t=s.replace('\n',' ').replace('\r',' ').trim();return t.codePointCount(0,t.length())<=MAX_TEXT?t:t.substring(0,t.offsetByCodePoints(0,MAX_TEXT));}

    // ---- codec ----
    private static void putTrack(Properties p,String k,Track t){p.setProperty(k+"d",Long.toString(t.dialog));p.setProperty(k+"m",Integer.toString(t.message));p.setProperty(k+"t",t.title);p.setProperty(k+"p",t.performer);p.setProperty(k+"s",Integer.toString(t.duration));p.setProperty(k+"a",Long.toString(t.added));}
    private static Track getTrack(Properties p,String k){Track t=new Track();t.dialog=Long.parseLong(p.getProperty(k+"d"));t.message=Integer.parseInt(p.getProperty(k+"m"));if(t.dialog==0||t.message<=0)throw new IllegalArgumentException();t.title=clip(p.getProperty(k+"t",""));t.performer=clip(p.getProperty(k+"p",""));t.duration=Math.max(0,num(p.getProperty(k+"s"),0));t.added=Long.parseLong(p.getProperty(k+"a","0"));return t;}
    public String encode(){
        Properties p=new Properties();p.setProperty("schema",Integer.toString(SCHEMA));p.setProperty("next",Integer.toString(nextId));p.setProperty("lists",Integer.toString(playlists.size()));
        for(int i=0;i<playlists.size();i++){Playlist l=playlists.get(i);String k="l."+i+".";p.setProperty(k+"id",Integer.toString(l.id));p.setProperty(k+"n",l.name);p.setProperty(k+"c",Integer.toString(l.color));p.setProperty(k+"a",Long.toString(l.created));p.setProperty(k+"count",Integer.toString(l.tracks.size()));
            for(int j=0;j<l.tracks.size();j++)putTrack(p,k+j+".",l.tracks.get(j));}
        p.setProperty("recent",Integer.toString(recent.size()));for(int i=0;i<recent.size();i++)putTrack(p,"r."+i+".",recent.get(i));
        int n=0;for(java.util.Map.Entry<String,Integer> e:plays.entrySet()){p.setProperty("c."+n+".k",e.getKey());p.setProperty("c."+n+".n",Integer.toString(e.getValue()));n++;}p.setProperty("plays",Integer.toString(n));
        try{StringWriter w=new StringWriter();p.store(w,"Tetogram playlists");return w.toString();}catch(IOException e){throw new IllegalStateException(e);}
    }
    /** Unknown schema is rejected so it is never overwritten; damaged entries are skipped. */
    public static Playlists decode(String data){
        Playlists out=new Playlists();if(data==null||data.isEmpty())return out;
        Properties p=new Properties();try{p.load(new StringReader(data));}catch(IOException e){throw new IllegalArgumentException(e);}
        if(!Integer.toString(SCHEMA).equals(p.getProperty("schema")))throw new IllegalArgumentException("Unsupported playlists schema");
        int maxId=0,lists=Math.min(MAX_PLAYLISTS,num(p.getProperty("lists"),0));
        for(int i=0;i<lists;i++){String k="l."+i+".";try{Playlist l=new Playlist();l.id=Integer.parseInt(p.getProperty(k+"id"));l.name=clip(p.getProperty(k+"n",""));if(l.id<=0||l.name.isEmpty()||out.playlist(l.id)!=null)continue;
            l.color=color(num(p.getProperty(k+"c"),0));l.created=Long.parseLong(p.getProperty(k+"a","0"));int count=Math.min(MAX_TRACKS,num(p.getProperty(k+"count"),0));java.util.HashSet<String> seen=new java.util.HashSet<>();
            for(int j=0;j<count;j++){try{Track t=getTrack(p,k+j+".");if(seen.add(t.key()))l.tracks.add(t);}catch(RuntimeException ignored){}}
            out.playlists.add(l);maxId=Math.max(maxId,l.id);}catch(RuntimeException ignored){}}
        out.nextId=Math.max(maxId+1,num(p.getProperty("next"),1));
        int recent=Math.min(MAX_RECENT,num(p.getProperty("recent"),0));for(int i=0;i<recent;i++){try{out.recent.add(getTrack(p,"r."+i+"."));}catch(RuntimeException ignored){}}
        int plays=num(p.getProperty("plays"),0);for(int i=0;i<plays&&i<MAX_TRACKS*2;i++){String k=p.getProperty("c."+i+".k");int n=num(p.getProperty("c."+i+".n"),0);if(k!=null&&n>0)out.plays.put(k,n);}
        return out;
    }
    private static int num(String s,int fallback){try{return s==null?fallback:Integer.parseInt(s.trim());}catch(NumberFormatException e){return fallback;}}
}
