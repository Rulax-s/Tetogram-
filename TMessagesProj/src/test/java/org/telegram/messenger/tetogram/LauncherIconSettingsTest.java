package org.telegram.messenger.tetogram;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.telegram.messenger.tetogram.AppearanceIconPolicy.Season;
public class LauncherIconSettingsTest {
 @Test public void emptyPreferencesDoNotOptIntoSeasonalSwitching(){LauncherIconSettings s=LauncherIconSettings.read(Collections.emptyMap());assertFalse(s.seasonal);assertTrue(s.automatic);assertFalse(s.corrupt);assertEquals("Tetogram",s.brand);}
 @Test public void legacyManualChoiceMigratesWithoutChangingBrand(){Map<String,Object> raw=new HashMap<>();raw.put("seasonal",true);raw.put("brand","Mikugram");raw.put("season","WINTER");LauncherIconSettings s=LauncherIconSettings.read(raw);assertTrue(s.seasonal);assertFalse(s.automatic);assertEquals(Season.WINTER,s.manual);assertEquals("Mikugram",s.brand);}
 @Test public void legacyAutoKeepsItsMode(){Map<String,Object> raw=new HashMap<>();raw.put("seasonal",true);raw.put("season","AUTO");assertTrue(LauncherIconSettings.read(raw).automatic);}
 @Test public void switchingBackToAutoRemembersManualSeason(){LauncherIconSettings s=new LauncherIconSettings();s.select(Season.SUMMER);s.select(Season.AUTO);assertTrue(s.automatic);assertEquals(Season.SUMMER,s.manual);s.select(s.manual);assertFalse(s.automatic);assertEquals(Season.SUMMER,s.choice());}
 @Test public void characterChangeDoesNotChangeModeOrManualSeason(){LauncherIconSettings s=new LauncherIconSettings();s.select(Season.WINTER);s.brand="Mikugram";assertEquals(Season.WINTER,s.choice());s.select(Season.AUTO);s.brand="Tetogram";assertTrue(s.automatic);assertEquals(Season.WINTER,s.manual);}
 @Test public void versionTwoReadsLastAliasRevisionAndRememberedManual(){Map<String,Object> raw=new HashMap<>();raw.put("schema",2);raw.put("automatic",true);raw.put("manualSeason","SPRING");raw.put("lastAlias","MikugramAutumn");raw.put("revision",9L);LauncherIconSettings s=LauncherIconSettings.read(raw);assertEquals("MikugramAutumn",s.lastAlias);assertEquals(9,s.revision);assertEquals(Season.SPRING,s.manual);assertTrue(s.automatic);}
 @Test public void corruptedTypesAndUnknownSelectionsHaveSafeTetogramDefaults(){for(Map<String,?> raw:Arrays.asList(Collections.singletonMap("brand","unknown"),Collections.singletonMap("seasonal","yes"),Collections.singletonMap("manualSeason","AUTO"),Collections.singletonMap("season","bad"),Collections.singletonMap("schema",999),Collections.singletonMap("revision",-2L))){LauncherIconSettings s=LauncherIconSettings.read(raw);assertTrue(s.corrupt);assertFalse(s.seasonal);assertEquals("Tetogram",s.brand);}}
 @Test public void editingDraftDoesNotMutateSavedDeviceChoice(){LauncherIconSettings saved=new LauncherIconSettings();LauncherIconSettings draft=saved.copy();draft.brand="Mikugram";draft.select(Season.WINTER);assertEquals("Tetogram",saved.brand);assertFalse(saved.seasonal);assertFalse(draft.sameChoice(saved));}
 @Test public void automaticAliasBookkeepingDoesNotCountAsAUserChoiceChange(){LauncherIconSettings a=new LauncherIconSettings(),b=a.copy();b.lastAlias="TetogramSpring";assertTrue(a.sameChoice(b));b.select(Season.WINTER);assertFalse(a.sameChoice(b));}
}
