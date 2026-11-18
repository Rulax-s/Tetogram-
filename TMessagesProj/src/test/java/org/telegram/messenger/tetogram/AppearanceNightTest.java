package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceNightTest {
    private static final int SCHEDULED=1,SYSTEM=3;
    @Test public void forcedModeRemembersTheUserSchedule(){assertArrayEquals(new int[]{AppearanceNight.NONE,SCHEDULED},AppearanceNight.decide("dark",SCHEDULED,AppearanceNight.UNSET));assertArrayEquals(new int[]{AppearanceNight.NONE,SYSTEM},AppearanceNight.decide("light",SYSTEM,AppearanceNight.UNSET));}
    @Test public void repeatedForcingKeepsTheOriginalValue(){assertArrayEquals(new int[]{AppearanceNight.NONE,SCHEDULED},AppearanceNight.decide("dark",AppearanceNight.NONE,SCHEDULED));assertArrayEquals(new int[]{AppearanceNight.NONE,SCHEDULED},AppearanceNight.decide("light",SYSTEM,SCHEDULED));}
    @Test public void inheritingRestoresAndForgets(){assertArrayEquals(new int[]{SCHEDULED,AppearanceNight.UNSET},AppearanceNight.decide(null,AppearanceNight.NONE,SCHEDULED));assertArrayEquals(new int[]{SYSTEM,AppearanceNight.UNSET},AppearanceNight.decide("auto",AppearanceNight.NONE,SYSTEM));}
    @Test public void manualChoiceWhileInheritingWins(){assertArrayEquals(new int[]{SYSTEM,AppearanceNight.UNSET},AppearanceNight.decide(null,SYSTEM,SCHEDULED));}
    @Test public void userWithoutAutoNightIsUntouched(){assertArrayEquals(new int[]{AppearanceNight.NONE,AppearanceNight.UNSET},AppearanceNight.decide("dark",AppearanceNight.NONE,AppearanceNight.UNSET));assertArrayEquals(new int[]{AppearanceNight.NONE,AppearanceNight.UNSET},AppearanceNight.decide(null,AppearanceNight.NONE,AppearanceNight.UNSET));}
}
