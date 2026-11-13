package org.telegram.messenger.tetogram;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MessageTimelinePolicyTest {
    static class M {
        final int id,date; final String text; final long sender; final boolean visible;
        M(int id,int date){this.id=id;this.date=date;this.text="fixture "+id;this.sender=id;this.visible=true;}
    }
    static final HistoryPagePolicy.Message<M> ACCESS=new HistoryPagePolicy.Message<M>() {
        public int get(M m){return m.id;} public int date(M m){return m.date;}
    };
    static M e(int id,int date){return new M(0x60000000|id,date);}
    static int compare(M a,M b){return MessageTimelinePolicy.compare(a.id,a.date,b.id,b.date);}
    ArrayList<M> merge(List<M> source,List<M> archived,int count) {
        ArrayList<M> out=new ArrayList<>();HistoryPagePolicy.merge(out,new ArrayList<>(source),new ArrayList<>(archived),count,0,0,ACCESS);return out;
    }
    void intact(M expected,M actual){assertSame(expected,actual);assertEquals(expected.sender,actual.sender);assertEquals(expected.text,actual.text);assertEquals(expected.visible,actual.visible);}
    @Test public void fiveNewerMessagesStayAheadOfEphemeralAcrossRefreshes() {
        M ephemeral=e(1,917);ArrayList<M> source=new ArrayList<>();source.add(ephemeral);
        for(int i=1;i<=5;i++)source.add(0,new M(100+i,917+i));
        for(int i=0;i<20;i++){source=merge(source,Collections.emptyList(),30);assertEquals(6,source.size());intact(ephemeral,source.get(5));}
    }
    @Test public void archivedMessagesMergeWithoutMovingEphemeralToNewest() {
        M ephemeral=e(1,917),newer=new M(12,922),old=new M(10,916),deleted=new M(11,919);
        ArrayList<M> result=merge(Arrays.asList(newer,ephemeral,old),Arrays.asList(deleted),30);
        assertEquals(Arrays.asList(newer,deleted,ephemeral,old),result);intact(ephemeral,result.get(2));
    }
    @Test public void multipleEphemeralsAndEqualDatesUseNativeTieBreakers() {
        M a=e(1,917),b=e(2,917),ordinary=new M(20,917);
        assertTrue(compare(a,b)<0);assertTrue(compare(a,ordinary)<0);
        assertTrue(compare(new M(21,917),ordinary)<0);
        assertEquals(Arrays.asList(a,b,ordinary),merge(Arrays.asList(a,b,ordinary),Collections.emptyList(),30));
    }
    @Test public void delayedEphemeralUsesDateNotPackedIdForInsertion() {
        M delayed=e(1,917),newer=new M(20,922);
        assertTrue(compare(delayed,newer)>0);assertTrue(compare(newer,delayed)<0);
        M equal=e(2,917);assertTrue(compare(equal,delayed)>0);
    }
    @Test public void deletingAnotherMessageAndReloadingDoesNotChangeVisibilityOrOrder() {
        M newer=new M(12,922),ephemeral=e(1,917),old=new M(10,916);
        ArrayList<M> before=merge(Arrays.asList(newer,ephemeral,old),Collections.emptyList(),30);
        ArrayList<M> after=merge(Arrays.asList(ephemeral,old),Arrays.asList(newer),30);
        assertEquals(before,after);for(int i=0;i<before.size();i++)intact(before.get(i),after.get(i));
    }
    @Test public void ephemeralIdsNeverConsumeRegularPageSlotsOrBecomeCursors() {
        M ephemeral=e(1,917),pending=new M(-1,923),a=new M(12,922),b=new M(11,919);
        ArrayList<M> out=merge(Arrays.asList(pending,a,ephemeral),Arrays.asList(b),2);
        assertEquals(Arrays.asList(pending,a,b,ephemeral),out);
    }
    @Test public void nativeOrderIsPreservedForOrdinaryIdsDespiteClockAnomalies() {
        M a=new M(12,900),b=new M(11,922);assertTrue(compare(a,b)<0);
        assertEquals(Arrays.asList(a,b),merge(Arrays.asList(a,b),Collections.emptyList(),30));
    }
    @Test public void duplicateArchiveEventCannotDuplicateMessage() {
        M a=new M(12,922),copy=new M(12,922),ephemeral=e(1,917);
        assertEquals(Arrays.asList(a,ephemeral),merge(Arrays.asList(a,ephemeral),Arrays.asList(copy,copy),30));
    }
    @Test public void restoredStorageOrderingMatchesMixedTimeline() {
        ArrayList<M> restored=new ArrayList<>(Arrays.asList(e(2,917),new M(12,922),e(1,917),new M(10,916)));
        restored.sort(MessageTimelinePolicyTest::compare);
        assertEquals(12,restored.get(0).id);assertEquals(0x60000001,restored.get(1).id);assertEquals(0x60000002,restored.get(2).id);assertEquals(10,restored.get(3).id);
        assertEquals(restored,merge(restored,Collections.emptyList(),30));
    }
}
