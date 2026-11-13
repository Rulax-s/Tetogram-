/* Tetogram, 2026. GNU GPL v2 or later. No Android or Telegram dependencies. */
package org.telegram.messenger.tetogram;
import java.util.*;
import java.io.*;
/** Versioned local configuration. IDs, never positions, are persisted. */
public final class SectionConfig {
 public static final String OWN="own";
 public boolean configured;
 public String defaultId="",selectedId="";
 public int nextToken=1000000;
 public final ArrayList<Section> sections=new ArrayList<>();
 public final LinkedHashMap<String,Folder> folders=new LinkedHashMap<>();
 public static class Scope {
  public int flags;
  public final LinkedHashSet<Long> chats=new LinkedHashSet<>(),excluded=new LinkedHashSet<>();
  public void include(long id){excluded.remove(id);chats.add(id);}
  public void exclude(long id){chats.remove(id);excluded.add(id);}
 }
 public static final class Section extends Scope {
  public String id,name,icon="●",lastFolder="";
  public boolean general;
  public final ArrayList<String> tabs=new ArrayList<>();
  public ArrayList<String> visibleTabs(Collection<String> available){ArrayList<String> result=new ArrayList<>();for(String key:tabs)if(available.contains(key) && !result.contains(key))result.add(key);if(!result.isEmpty() && general)result.add(0,OWN);return result;}
 }
 public static final class Folder extends Scope {public String id,name;public int token;}
 public Section section(String id){for(Section s:sections)if(s.id.equals(id))return s;return null;}
 public Scope scope(String sectionId,String tabKey){return OWN.equals(tabKey)?section(sectionId):folders.get(tabKey);}
 public Section add(String name){Section s=new Section();s.id=UUID.randomUUID().toString();s.name=title(name);sections.add(s);configured=true;selectedId=s.id;return s;}
 public Folder addFolder(String name){if(nextToken==Integer.MAX_VALUE)throw new IllegalStateException("UI token space exhausted");Folder f=new Folder();f.id="local:"+UUID.randomUUID();f.name=title(name);f.token=nextToken++;folders.put(f.id,f);return f;}
 public static String title(String value){if(value==null || value.trim().isEmpty())throw new IllegalArgumentException("Empty name");return value.trim().replace('\n',' ').replace('\r',' ');}
 public void remove(String id){sections.removeIf(s->s.id.equals(id));if(defaultId.equals(id))defaultId="";if(selectedId.equals(id))selectedId="";}
 public void removeFolder(String id){folders.remove(id);for(Section s:sections){s.tabs.remove(id);if(id.equals(s.lastFolder))s.lastFolder="";}}
 public String initial(){if(section(defaultId)!=null)return defaultId;if(section(selectedId)!=null)return selectedId;return sections.isEmpty()?"":sections.get(0).id;}
 public static String selectedTab(Section s,ArrayList<String> visible){return visible.isEmpty()?OWN:visible.contains(s.lastFolder)?s.lastFolder:visible.get(0);}
 public static String accountNamespace(long uid){return "tetogram_sections_v1_u"+uid;}
 public static <T> void move(ArrayList<T> list,T item,int delta){int a=list.indexOf(item),b=a+delta;if(a>=0 && b>=0 && b<list.size())Collections.swap(list,a,b);}
 public static boolean includes(Scope scope,long id,int category,boolean unread,boolean muted,boolean archived){
  if(scope.excluded.contains(id))return false;
  if(scope.chats.contains(id))return true;
  return (scope.flags&category)!=0 && ((scope.flags&32)==0 || !muted) && ((scope.flags&64)==0 || unread) && ((scope.flags&128)==0 || !archived);
 }
 private static String join(Collection<?> values){StringBuilder b=new StringBuilder();for(Object v:values){if(b.length()>0)b.append(',');b.append(v);}return b.toString();}
 private static ArrayList<String> split(String v){return new ArrayList<>(v.isEmpty()?Collections.emptyList():Arrays.asList(v.split(",")));}
 private static void writeScope(Properties p,String k,Scope s){p.setProperty(k+"flags",Integer.toString(s.flags));p.setProperty(k+"chats",join(s.chats));p.setProperty(k+"excluded",join(s.excluded));}
 private static void readScope(Properties p,String k,Scope s){s.flags=Integer.parseInt(p.getProperty(k+"flags","0"));for(String v:split(p.getProperty(k+"chats","")))s.chats.add(Long.parseLong(v));for(String v:split(p.getProperty(k+"excluded","")))s.excluded.add(Long.parseLong(v));}
 public String encode(){
  Properties p=new Properties();p.setProperty("schema","1");p.setProperty("configured",Boolean.toString(configured));p.setProperty("default",defaultId);p.setProperty("selected",selectedId);p.setProperty("token",Integer.toString(nextToken));
  ArrayList<String> ids=new ArrayList<>();for(Section s:sections){ids.add(s.id);String k="s."+s.id+".";p.setProperty(k+"name",s.name);p.setProperty(k+"icon",s.icon);p.setProperty(k+"general",Boolean.toString(s.general));p.setProperty(k+"last",s.lastFolder);p.setProperty(k+"tabs",join(s.tabs));writeScope(p,k,s);}p.setProperty("sections",join(ids));p.setProperty("folders",join(folders.keySet()));
  for(Folder f:folders.values()){String k="f."+f.id+".";p.setProperty(k+"name",f.name);p.setProperty(k+"token",Integer.toString(f.token));writeScope(p,k,f);}
  try{StringWriter w=new StringWriter();p.store(w,"Tetogram local sections v1");return w.toString();}catch(IOException e){throw new IllegalStateException(e);}
 }
 public static SectionConfig decode(String value){
  SectionConfig c=new SectionConfig();if(value==null || value.isEmpty())return c;
  Properties p=new Properties();try{p.load(new StringReader(value));}catch(IOException e){throw new IllegalArgumentException(e);}
  if(!"1".equals(p.getProperty("schema")))throw new IllegalArgumentException("Unsupported section schema");
  c.configured=Boolean.parseBoolean(p.getProperty("configured","false"));c.defaultId=p.getProperty("default","");c.selectedId=p.getProperty("selected","");c.nextToken=Integer.parseInt(p.getProperty("token","1000000"));
  for(String id:split(p.getProperty("folders",""))){if(!id.startsWith("local:") || c.folders.containsKey(id))throw new IllegalArgumentException("Invalid folder ID");Folder f=new Folder();f.id=id;String k="f."+id+".";f.name=title(p.getProperty(k+"name"));f.token=Integer.parseInt(p.getProperty(k+"token"));readScope(p,k,f);c.folders.put(id,f);}
  for(String id:split(p.getProperty("sections",""))){UUID.fromString(id);if(c.section(id)!=null)throw new IllegalArgumentException("Duplicate section");Section s=new Section();s.id=id;String k="s."+id+".";s.name=title(p.getProperty(k+"name"));s.icon=p.getProperty(k+"icon","●");s.general=Boolean.parseBoolean(p.getProperty(k+"general","false"));s.lastFolder=p.getProperty(k+"last","");for(String tab:split(p.getProperty(k+"tabs","")))if(!s.tabs.contains(tab))s.tabs.add(tab);readScope(p,k,s);c.sections.add(s);}
  return c;
 }
}
