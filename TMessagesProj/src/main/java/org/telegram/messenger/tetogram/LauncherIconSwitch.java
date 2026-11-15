/* Tetogram, 2026. GPL v2 or later. Tested enable-first transaction over known launcher aliases only. */
package org.telegram.messenger.tetogram;
import java.util.*;
public final class LauncherIconSwitch {
 public interface Access {boolean enabled(String key);void enable(String key);void disable(String key);}
 private LauncherIconSwitch(){}
 public static void apply(String desired,List<String> known,Access access){
  if(!known.contains(desired))throw new IllegalArgumentException("Unknown launcher alias");
  access.enable(desired);if(!access.enabled(desired))throw new IllegalStateException("Launcher alias was not enabled");
  for(String key:known)if(!key.equals(desired)&&access.enabled(key))access.disable(key);
  for(String key:known)if(access.enabled(key)!=key.equals(desired))throw new IllegalStateException("Launcher aliases could not be made exclusive");
 }
}
