package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class AppearancePeopleTest {
    @Test public void tokensRoundTripUsersAndChannels(){assertEquals("u42",AppearancePeople.token(42));assertEquals("c1001234",AppearancePeople.token(-1001234));assertEquals(42,AppearancePeople.id("u42"));assertEquals(-1001234,AppearancePeople.id("c1001234"));assertEquals(0,AppearancePeople.id("x5"));assertEquals(0,AppearancePeople.id("u-5"));assertEquals(0,AppearancePeople.id(null));}
    @Test(expected=IllegalArgumentException.class) public void noSenderNoKey(){AppearancePeople.token(0);}
    @Test public void keysAreValidStyleKeys(){AppearanceConfig.Style s=new AppearanceConfig.Style();AppearancePeople.write(s,-1001234,0x80FF0000,0xff00ff00,3);assertEquals(3,s.values.size());AppearanceConfig d=new AppearanceConfig();d.scopes.put("global",s);assertEquals(s.values,AppearanceConfig.decode(d.encode()).scopes.get("global").values);}
    @Test public void readWriteFillOutlineAndWidth(){AppearanceConfig.Style s=new AppearanceConfig.Style();
        AppearancePeople.write(s,7,0xffE8577E,null,2);AppearancePeople.Style r=AppearancePeople.read(s.values::get,7);assertEquals(Integer.valueOf(0xffE8577E),r.fill);assertFalse(r.hasOutline());
        AppearancePeople.write(s,7,null,0xff3C8DD9,9);r=AppearancePeople.read(s.values::get,7);assertNull("rewrite replaces the old fill",r.fill);assertTrue(r.hasOutline());assertEquals(AppearancePeople.MAX_OUTLINE,r.width,0);
        s.set(AppearancePeople.key(7,"outlineWidth"),"oops");assertEquals(AppearancePeople.DEFAULT_OUTLINE,AppearancePeople.read(s.values::get,7).width,0);
        assertNull(AppearancePeople.read(s.values::get,8));assertNull(AppearancePeople.read(s.values::get,0));
        AppearancePeople.clear(s,7);assertTrue(s.values.isEmpty());}
    @Test public void listsPeopleInOrder(){AppearanceConfig.Style s=new AppearanceConfig.Style();AppearancePeople.write(s,5,1,null,0);AppearancePeople.write(s,-9,null,2,1);AppearancePeople.write(s,5,3,4,1);s.set("person.bad.bubble","1");s.set("mode","dark");assertEquals(Arrays.asList(-9L,5L),AppearancePeople.people(s));assertTrue(AppearancePeople.people(null).isEmpty());}
    @Test public void groupOverrideWinsOverEverywhere(){AppearanceConfig c=new AppearanceConfig();AppearancePeople.write(c.edit("global"),5,0xff111111,null,0);AppearancePeople.write(c.edit("chat:-77"),5,0xff222222,null,0);
        assertEquals(Integer.valueOf(0xff222222),AppearancePeople.read(c.resolve(AppearanceConfig.Context.direct(-77,0))::get,5).fill);
        assertEquals(Integer.valueOf(0xff111111),AppearancePeople.read(c.resolve(AppearanceConfig.Context.direct(-78,0))::get,5).fill);
        assertEquals(Integer.valueOf(0xff111111),AppearancePeople.read(c.resolve(AppearanceConfig.Context.direct(5,0))::get,5).fill);}
    @Test public void peopleNeverLeaveInPresetsButSurviveThemesAndApply(){
        AppearanceConfig c=new AppearanceConfig();AppearancePeople.write(c.edit("global"),5,0xff111111,null,0);c.edit("global").set("mode","dark");AppearancePeople.write(c.edit("chat:-77"),5,0xff222222,null,0);
        assertTrue(AppearanceConfig.isPersonalKey(AppearancePeople.key(5,"bubble")));
        assertTrue(AppearanceConfig.withoutPersonal(c.scopes.get("global")).values.keySet().stream().noneMatch(k->k.startsWith("person.")));
        c.apply("global",AppearancePresets.builtins().get(0));assertNotNull(AppearancePeople.read(c.scopes.get("global").values::get,5));
        AppearanceConfig.Style draft=new AppearanceConfig.Style();draft.set("bubble.inRadius","3");c.putScope("chat:-77",draft);
        assertEquals("3",c.scopes.get("chat:-77").values.get("bubble.inRadius"));assertNotNull("editor Apply keeps the group's people",AppearancePeople.read(c.scopes.get("chat:-77").values::get,5));
        c.edit("global").set("launcher.label","x");c.resetAppearance();
        assertNull("full reset removes person colors",AppearancePeople.read(c.scopes.get("global").values::get,5));assertEquals("x",c.scopes.get("global").values.get("launcher.label"));assertFalse(c.scopes.containsKey("chat:-77"));}
    @Test public void textTurnsReadableOnlyWhenNeeded(){assertEquals(0xff000000,AppearancePeople.text(0xffFFFFFF,0xff000000,0xffffffff));assertEquals(0xffFFFFFF,AppearancePeople.text(0xff101010,0xff000000,0xffffffff));assertEquals(0xff000000,AppearancePeople.text(0x10101010,0xff000000,0xffffffff));}
}
