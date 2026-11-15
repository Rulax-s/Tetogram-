package org.telegram.messenger.tetogram;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class LauncherIconSwitchTest {
 static class Backend implements LauncherIconSwitch.Access {Set<String> active=new HashSet<>();List<String> calls=new ArrayList<>();boolean refuseEnable,refuseDisable;public boolean enabled(String k){return active.contains(k);}public void enable(String k){calls.add("+"+k);if(refuseEnable)throw new IllegalStateException();active.add(k);}public void disable(String k){assertFalse(active.isEmpty());calls.add("-"+k);if(refuseDisable)throw new IllegalStateException();active.remove(k);assertFalse(active.isEmpty());}}
 final List<String> keys=Arrays.asList("DefaultIcon","MikuWinterIcon","MikugramWinter","TetogramSpring");
 @Test public void enableHappensBeforeAnyDisable(){Backend b=new Backend();b.active.add("DefaultIcon");LauncherIconSwitch.apply("MikugramWinter",keys,b);assertEquals("+MikugramWinter",b.calls.get(0));assertEquals(Collections.singleton("MikugramWinter"),b.active);}
 @Test public void legacyAndDuplicateComponentsAreMadeExclusive(){Backend b=new Backend();b.active.addAll(keys);LauncherIconSwitch.apply("TetogramSpring",keys,b);assertEquals(Collections.singleton("TetogramSpring"),b.active);}
 @Test public void emptyStateCanBeRecoveredToTetogramDefault(){Backend b=new Backend();LauncherIconSwitch.apply("DefaultIcon",keys,b);assertEquals(Collections.singleton("DefaultIcon"),b.active);}
 @Test public void enableFailureNeverDisablesExistingIcon(){Backend b=new Backend();b.active.add("DefaultIcon");b.refuseEnable=true;try{LauncherIconSwitch.apply("MikugramWinter",keys,b);fail();}catch(IllegalStateException expected){}assertEquals(Collections.singleton("DefaultIcon"),b.active);assertEquals(1,b.calls.size());}
 @Test public void disableFailureStillLeavesLaunchableComponents(){Backend b=new Backend();b.active.add("DefaultIcon");b.refuseDisable=true;try{LauncherIconSwitch.apply("MikugramWinter",keys,b);fail();}catch(IllegalStateException expected){}assertTrue(b.active.contains("MikugramWinter"));assertFalse(b.active.isEmpty());}
 @Test public void actualLaunchActivityIsNotAnAllowedComponentTarget(){Backend b=new Backend();b.active.add("DefaultIcon");try{LauncherIconSwitch.apply("org.telegram.ui.LaunchActivity",keys,b);fail();}catch(IllegalArgumentException expected){}assertTrue(b.calls.isEmpty());assertEquals(Collections.singleton("DefaultIcon"),b.active);}
}
