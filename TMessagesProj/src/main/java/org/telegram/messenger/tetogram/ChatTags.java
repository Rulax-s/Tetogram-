/* Tetogram, 2026. GNU GPL v2 or later. Local chat tags; Android-independent model and codec. */
package org.telegram.messenger.tetogram;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Coloured labels the user puts on chats; stored on this device only. */
public final class ChatTags {
    public static final int SCHEMA=1,MAX_TAGS=30,MAX_NAME=24,COLORS=7,MAX_CHATS=5000;

    public static final class Tag { public int id,color;public String name=""; }

    /** In the user's order. */
    public final ArrayList<Tag> tags=new ArrayList<>();
    /** Chat → tag ids, in the order the tags were put on. */
    public final LinkedHashMap<Long,ArrayList<Integer>> chats=new LinkedHashMap<>();
    private int nextId=1;

    public Tag tag(int id){for(Tag t:tags)if(t.id==id)return t;return null;}
    public Tag add(String name,int color){
        String n=clip(name);if(n.isEmpty())throw new IllegalArgumentException("Empty tag name");
        if(tags.size()>=MAX_TAGS)throw new IllegalStateException("Too many tags");
        Tag t=new Tag();t.id=nextId++;t.name=n;t.color=color(color);tags.add(t);return t;
    }
    public void rename(int id,String name,int color){Tag t=tag(id);String n=clip(name);if(t==null||n.isEmpty())return;t.name=n;t.color=color(color);}
    public void delete(int id){tags.remove(tag(id));for(java.util.Iterator<Map.Entry<Long,ArrayList<Integer>>> it=chats.entrySet().iterator();it.hasNext();){ArrayList<Integer> ids=it.next().getValue();ids.remove(Integer.valueOf(id));if(ids.isEmpty())it.remove();}}
    public void move(int from,int to){if(from<0||to<0||from>=tags.size()||to>=tags.size())return;tags.add(to,tags.remove(from));}

    public boolean has(long dialog,int id){ArrayList<Integer> ids=chats.get(dialog);return ids!=null&&ids.contains(id);}
    public void set(long dialog,int id,boolean on){
        if(dialog==0||tag(id)==null)return;ArrayList<Integer> ids=chats.get(dialog);
        if(on){if(ids==null){if(chats.size()>=MAX_CHATS)return;ids=new ArrayList<>();chats.put(dialog,ids);}if(!ids.contains(id))ids.add(id);}
        else if(ids!=null){ids.remove(Integer.valueOf(id));if(ids.isEmpty())chats.remove(dialog);}
    }
    /** Tags of a chat in the user's tag order. */
    public List<Tag> of(long dialog){ArrayList<Tag> out=new ArrayList<>();ArrayList<Integer> ids=chats.get(dialog);if(ids==null)return out;for(Tag t:tags)if(ids.contains(t.id))out.add(t);return out;}
    public List<Long> chatsWith(int id){ArrayList<Long> out=new ArrayList<>();for(Map.Entry<Long,ArrayList<Integer>> e:chats.entrySet())if(e.getValue().contains(id))out.add(e.getKey());return out;}
    public Map<Integer,Integer> counts(){HashMap<Integer,Integer> out=new HashMap<>();for(ArrayList<Integer> ids:chats.values())for(int id:ids){Integer n=out.get(id);out.put(id,n==null?1:n+1);}return out;}

    /** Own modulo: Math.floorMod is not on every supported Android level. */
    static int color(int c){int m=c%COLORS;return m<0?m+COLORS:m;}
    static String clip(String s){if(s==null)return "";String t=s.replace('\n',' ').replace('\r',' ').trim();return t.codePointCount(0,t.length())<=MAX_NAME?t:t.substring(0,t.offsetByCodePoints(0,MAX_NAME));}

    public String encode(){
        Properties p=new Properties();p.setProperty("schema",Integer.toString(SCHEMA));p.setProperty("next",Integer.toString(nextId));p.setProperty("tags",Integer.toString(tags.size()));
        for(int i=0;i<tags.size();i++){Tag t=tags.get(i);p.setProperty("t."+i+".id",Integer.toString(t.id));p.setProperty("t."+i+".c",Integer.toString(t.color));p.setProperty("t."+i+".n",t.name);}
        int n=0;for(Map.Entry<Long,ArrayList<Integer>> e:chats.entrySet()){StringBuilder b=new StringBuilder();for(int id:e.getValue()){if(b.length()>0)b.append(',');b.append(id);}p.setProperty("c."+n+".d",Long.toString(e.getKey()));p.setProperty("c."+n+".t",b.toString());n++;}
        p.setProperty("chats",Integer.toString(n));
        try{StringWriter w=new StringWriter();p.store(w,"Tetogram chat tags");return w.toString();}catch(IOException e){throw new IllegalStateException(e);}
    }
    /** Unknown schema is rejected so it is never overwritten; damaged entries are skipped. */
    public static ChatTags decode(String data){
        ChatTags c=new ChatTags();if(data==null||data.isEmpty())return c;
        Properties p=new Properties();try{p.load(new StringReader(data));}catch(IOException e){throw new IllegalArgumentException(e);}
        if(!Integer.toString(SCHEMA).equals(p.getProperty("schema")))throw new IllegalArgumentException("Unsupported tags schema");
        int maxId=0;int count=Math.min(MAX_TAGS,num(p.getProperty("tags"),0));
        for(int i=0;i<count;i++){try{Tag t=new Tag();t.id=Integer.parseInt(p.getProperty("t."+i+".id"));t.color=color(num(p.getProperty("t."+i+".c"),0));t.name=clip(p.getProperty("t."+i+".n",""));if(t.id<=0||t.name.isEmpty()||c.tag(t.id)!=null)continue;c.tags.add(t);maxId=Math.max(maxId,t.id);}catch(RuntimeException ignored){}}
        c.nextId=Math.max(maxId+1,num(p.getProperty("next"),1));
        int chats=Math.min(MAX_CHATS,num(p.getProperty("chats"),0));
        for(int i=0;i<chats;i++){try{long d=Long.parseLong(p.getProperty("c."+i+".d"));if(d==0)continue;ArrayList<Integer> ids=new ArrayList<>();for(String s:p.getProperty("c."+i+".t","").split(",")){if(s.isEmpty())continue;int id=Integer.parseInt(s);if(c.tag(id)!=null&&!ids.contains(id))ids.add(id);}if(!ids.isEmpty())c.chats.put(d,ids);}catch(RuntimeException ignored){}}
        return c;
    }
    private static int num(String s,int fallback){try{return s==null?fallback:Integer.parseInt(s.trim());}catch(NumberFormatException e){return fallback;}}
}
