package org.telegram.messenger.tetogram;

import org.junit.Test;
import static org.junit.Assert.*;

public class HistoryPolicyTest {
    @Test public void ordinaryIncomingIsAllowedOnlyWithConsent() {
        assertTrue(HistoryPolicy.maySave(true,true,false,false,false,false,10,0,0,0,0));
        assertFalse(HistoryPolicy.maySave(false,true,false,false,false,false,10,0,0,0,0));
    }
    @Test public void secretOutgoingAndExcludedNeverSave() {
        assertFalse(HistoryPolicy.maySave(true,true,true,false,false,false,10,0,0,0,0));
        assertFalse(HistoryPolicy.maySave(true,true,false,true,false,false,10,0,0,0,0));
        assertFalse(HistoryPolicy.maySave(true,true,false,false,false,true,10,0,0,0,0));
        assertFalse(HistoryPolicy.maySave(true,true,false,false,true,false,10,0,0,0,0));
    }
    @Test public void selfDestructionAndViewOnceAreExcluded() {
        assertFalse(HistoryPolicy.maySave(true,true,false,false,false,false,10,30,0,0,0));
        assertFalse(HistoryPolicy.maySave(true,true,false,false,false,false,10,0,86400,0,0));
        assertFalse(HistoryPolicy.maySave(true,true,false,false,false,false,10,0,0,Integer.MAX_VALUE,0));
        assertFalse(HistoryPolicy.maySave(true,true,false,false,false,false,10,0,0,0,123));
        assertFalse(HistoryPolicy.maySave(true,false,false,false,false,false,10,0,0,0,0));
        assertFalse(HistoryPolicy.maySave(true,true,false,false,false,false,-1,0,0,0,0));
    }
    @Test public void retentionSurvivesUnknownFutureOrCorruptValues() {
        for(int days:new int[]{0,7,30,90})assertEquals(days,HistoryPolicy.retention(days));
        assertEquals(30,HistoryPolicy.retention(-1));assertEquals(30,HistoryPolicy.retention(365));
    }
}
