package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceProfilePolicyTest {
 @Test public void userAndGroupTargetsHaveDifferentNamespaces(){assertEquals(10,AppearanceProfilePolicy.target(10,0));assertEquals(-10,AppearanceProfilePolicy.target(0,10));}
 @Test public void zeroTargetDoesNotCreateAnInvalidScope(){assertEquals(0,AppearanceProfilePolicy.target(0,0));assertFalse(AppearanceProfilePolicy.active(10,0,true));}
 @Test public void ownProfileKeepsGlobalBannerCompatibility(){assertTrue(AppearanceProfilePolicy.active(10,10,false));}
 @Test public void otherProfilesStayNativeUntilExplicitlyCustomized(){assertFalse(AppearanceProfilePolicy.active(10,11,false));assertFalse(AppearanceProfilePolicy.active(10,-11,false));assertTrue(AppearanceProfilePolicy.active(10,11,true));assertTrue(AppearanceProfilePolicy.active(10,-11,true));}
 @Test public void logoutCannotActivateAnotherOwnersProfile(){assertFalse(AppearanceProfilePolicy.active(0,11,true));}
 @Test public void overridesAndResetDoNotChangeOwnProfileOrChat(){AppearanceConfig c=new AppearanceConfig();c.edit("global").set("banner.height","280");c.edit("profile:11").set("banner.height","320");c.edit("profile:-11").set("banner.height","240");assertEquals("320",c.resolveProfile(11).get("banner.height"));assertEquals("240",c.resolveProfile(-11).get("banner.height"));assertEquals("280",c.resolveProfile(10).get("banner.height"));c.reset("profile:11");assertEquals("280",c.resolveProfile(11).get("banner.height"));assertEquals("280",c.resolve(AppearanceConfig.Context.direct(11,0)).get("banner.height"));}
}
