/* Tetogram, 2026. GPL v2 or later. Device-wide settings; legacy preferences are read without mutating them. */
package org.telegram.messenger.tetogram;
import java.util.*;
public final class LauncherIconSettings {
 public boolean seasonal,automatic=true,corrupt;
 public String brand="Tetogram",lastAlias="";
 public AppearanceIconPolicy.Season manual=AppearanceIconPolicy.Season.WINTER;
 public long revision;
 public LauncherIconSettings copy(){LauncherIconSettings c=new LauncherIconSettings();c.seasonal=seasonal;c.automatic=automatic;c.corrupt=corrupt;c.brand=brand;c.lastAlias=lastAlias;c.manual=manual;c.revision=revision;return c;}
 public boolean sameChoice(LauncherIconSettings c){return c!=null&&seasonal==c.seasonal&&automatic==c.automatic&&manual==c.manual&&brand.equals(c.brand);}
 public AppearanceIconPolicy.Season choice(){return automatic?AppearanceIconPolicy.Season.AUTO:manual;}
 public void select(AppearanceIconPolicy.Season season){if(season==null)throw new IllegalArgumentException("Invalid season");if(season==AppearanceIconPolicy.Season.AUTO)automatic=true;else{automatic=false;manual=season;}seasonal=true;}
 private static boolean bool(Map<String,?> raw,String key,boolean fallback){Object o=raw.get(key);if(o==null)return fallback;if(!(o instanceof Boolean))throw new IllegalArgumentException();return (Boolean)o;}
 private static String text(Map<String,?> raw,String key,String fallback){Object o=raw.get(key);if(o==null)return fallback;if(!(o instanceof String))throw new IllegalArgumentException();return (String)o;}
 public static LauncherIconSettings read(Map<String,?> raw){LauncherIconSettings s=new LauncherIconSettings();try{
  Object schema=raw.get("schema");if(schema!=null&&(!(schema instanceof Number)||((Number)schema).intValue()!=2))throw new IllegalArgumentException();
  s.seasonal=bool(raw,"seasonal",false);s.brand=text(raw,"brand","Tetogram");if(!s.brand.equals("Tetogram")&&!s.brand.equals("Mikugram"))throw new IllegalArgumentException();
  AppearanceIconPolicy.Season old=AppearanceIconPolicy.Season.valueOf(text(raw,"season","AUTO"));s.automatic=bool(raw,"automatic",old==AppearanceIconPolicy.Season.AUTO);
  s.manual=AppearanceIconPolicy.Season.valueOf(text(raw,"manualSeason",old==AppearanceIconPolicy.Season.AUTO?"WINTER":old.name()));if(s.manual==AppearanceIconPolicy.Season.AUTO)throw new IllegalArgumentException();
  s.lastAlias=text(raw,"lastAlias","");Object revision=raw.get("revision");if(revision!=null&&!(revision instanceof Number))throw new IllegalArgumentException();s.revision=revision==null?0:((Number)revision).longValue();if(s.revision<0||s.revision==Long.MAX_VALUE)throw new IllegalArgumentException();
 }catch(RuntimeException e){s=new LauncherIconSettings();s.corrupt=true;}return s;}
}
