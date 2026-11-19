/* Tetogram, 2026. GNU GPL v2 or later. Local message bookmarks; Android-independent model and codec. */
package org.telegram.messenger.tetogram;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

/** Only references (chat, message ID) plus a short preview for the list; message contents stay in Telegram. */
public final class Bookmarks {
    public static final int SCHEMA=1,MAX_ITEMS=5000,PREVIEW=200,NOTE=500;

    public static final class Item {
        public long dialog,sender;public int message,date;public long added;public String preview="",note="";
        public boolean same(long dialog,int message){return this.dialog==dialog&&this.message==message;}
    }

    /** Newest first. */
    public final ArrayList<Item> items=new ArrayList<>();

    public Item find(long dialog,int message){for(Item i:items)if(i.same(dialog,message))return i;return null;}
    public boolean contains(long dialog,int message){return find(dialog,message)!=null;}

    /** Adds or refreshes a bookmark and puts it first; the oldest ones beyond {@link #MAX_ITEMS} are dropped. */
    public Item add(long dialog,int message,long sender,int date,String preview,long now){
        if(dialog==0||message<=0)throw new IllegalArgumentException("No message");
        Item item=find(dialog,message);
        if(item==null){item=new Item();item.dialog=dialog;item.message=message;}else items.remove(item);
        item.sender=sender;item.date=date;item.added=now;item.preview=clip(preview,PREVIEW);
        items.add(0,item);
        while(items.size()>MAX_ITEMS)items.remove(items.size()-1);
        return item;
    }
    public boolean remove(long dialog,int message){Item i=find(dialog,message);return i!=null&&items.remove(i);}
    public void setNote(long dialog,int message,String note){Item i=find(dialog,message);if(i!=null)i.note=clip(note,NOTE);}
    public int removeDialog(long dialog){int before=items.size();items.removeIf(i->i.dialog==dialog);return before-items.size();}

    public ArrayList<Item> forDialog(long dialog){ArrayList<Item> out=new ArrayList<>();for(Item i:items)if(i.dialog==dialog)out.add(i);return out;}
    /** Case-insensitive match on preview and note; an empty query returns everything. */
    public ArrayList<Item> search(String query,long dialog){
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);ArrayList<Item> out=new ArrayList<>();
        for(Item i:items){if(dialog!=0&&i.dialog!=dialog)continue;if(q.isEmpty()||i.preview.toLowerCase(Locale.ROOT).contains(q)||i.note.toLowerCase(Locale.ROOT).contains(q))out.add(i);}
        return out;
    }

    static String clip(String text,int max){
        if(text==null)return "";String t=text.replace('\n',' ').replace('\r',' ').trim();
        if(t.codePointCount(0,t.length())<=max)return t;
        return t.substring(0,t.offsetByCodePoints(0,max))+"…";
    }

    public String encode(){
        Properties p=new Properties();p.setProperty("schema",Integer.toString(SCHEMA));p.setProperty("count",Integer.toString(items.size()));
        for(int n=0;n<items.size();n++){Item i=items.get(n);String k="b."+n+".";
            p.setProperty(k+"d",Long.toString(i.dialog));p.setProperty(k+"m",Integer.toString(i.message));p.setProperty(k+"s",Long.toString(i.sender));
            p.setProperty(k+"t",Integer.toString(i.date));p.setProperty(k+"a",Long.toString(i.added));p.setProperty(k+"p",i.preview);if(!i.note.isEmpty())p.setProperty(k+"n",i.note);}
        try{StringWriter w=new StringWriter();p.store(w,"Tetogram bookmarks");return w.toString();}catch(IOException e){throw new IllegalStateException(e);}
    }
    /** Unknown schema is rejected so it is never overwritten; a single damaged entry is skipped, not fatal. */
    public static Bookmarks decode(String data){
        Bookmarks b=new Bookmarks();if(data==null||data.isEmpty())return b;
        Properties p=new Properties();try{p.load(new StringReader(data));}catch(IOException e){throw new IllegalArgumentException(e);}
        if(!Integer.toString(SCHEMA).equals(p.getProperty("schema")))throw new IllegalArgumentException("Unsupported bookmarks schema");
        int count=Math.min(MAX_ITEMS,Integer.parseInt(p.getProperty("count","0")));
        for(int n=0;n<count;n++){String k="b."+n+".";
            try{Item i=new Item();i.dialog=Long.parseLong(p.getProperty(k+"d"));i.message=Integer.parseInt(p.getProperty(k+"m"));
                if(i.dialog==0||i.message<=0||b.contains(i.dialog,i.message))continue;
                i.sender=Long.parseLong(p.getProperty(k+"s","0"));i.date=Integer.parseInt(p.getProperty(k+"t","0"));i.added=Long.parseLong(p.getProperty(k+"a","0"));
                i.preview=clip(p.getProperty(k+"p",""),PREVIEW);i.note=clip(p.getProperty(k+"n",""),NOTE);b.items.add(i);
            }catch(RuntimeException ignored){}
        }
        return b;
    }
}
