/* Tetogram, 2026. GNU GPL v2 or later. Safe names, bounded decoding and edit transactions. */
package org.telegram.messenger.tetogram;
import java.util.UUID;
public final class SectionIconPolicy {
 private SectionIconPolicy(){}
 public static boolean sectionId(String id){try{return id!=null && UUID.fromString(id).toString().equals(id);}catch(Exception e){return false;}}
 public static String accountDirectory(long owner){if(owner<=0)throw new IllegalArgumentException();return "u"+owner;}
 public static boolean inputName(String name){return name!=null && name.endsWith(".input") && sectionId(name.substring(0,name.length()-6));}
 public static boolean imageName(String name){return name!=null && name.matches("[a-f0-9-]{36}\\.jpg") && sectionId(name.substring(0,36));}
 public static int sample(int width,int height,int size){if(width<=0 || height<=0 || size<=0)throw new IllegalArgumentException();int n=1;while(Math.max(width,height)/(long)n>size*2L)n*=2;return n;}
 public static final class Edit {private volatile long ticket;public long begin(){return ++ticket;}public void cancel(){ticket++;}public boolean current(long t){return ticket==t;}}
}
