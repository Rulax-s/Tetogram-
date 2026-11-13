package org.telegram.messenger.tetogram;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import static org.junit.Assert.*;

public class HistoryPagePolicyTest {
    @Test public void olderPagesNeverSkipLargeDeletedGaps() {
        ArrayList<Integer> values=new ArrayList<>();
        for(int i=1;i<=1000;i++)values.add(i);
        HashSet<Integer> seen=new HashSet<>();
        int anchor=1001;
        while(anchor>1) {
            ArrayList<Integer> page=new ArrayList<>();for(int id:values)if(id<anchor)page.add(id);
            HistoryPagePolicy.trim(page,30,anchor,0,id->id);
            for(int id:page)assertTrue(seen.add(id));
            anchor=page.get(page.size()-1);
        }
        assertEquals(1000,seen.size());
    }
    @Test public void newerPagesChooseClosestNotNewest() {
        ArrayList<Integer> page=new ArrayList<>(Arrays.asList(1000,2,3,4,5));
        assertTrue(HistoryPagePolicy.trim(page,3,1,1,id->id));
        assertEquals(Arrays.asList(4,3,2),page);
        assertEquals("mid ASC",HistoryPagePolicy.archiveOrder(1,1));
    }
    @Test public void aroundReplyKeepsAnchorAndOriginalOrder() {
        ArrayList<Integer> page=new ArrayList<>(Arrays.asList(100,49,50,51,1));
        HistoryPagePolicy.trim(page,3,50,3,id->id);
        assertEquals(Arrays.asList(51,50,49),page);
        assertEquals("ABS(mid-50),mid DESC",HistoryPagePolicy.archiveOrder(50,3));
    }
    @Test public void finalSmallPageDoesNotInventMoreData() {
        ArrayList<Integer> page=new ArrayList<>(Arrays.asList(2,1));
        assertFalse(HistoryPagePolicy.trim(page,30,0,0,id->id));
        assertEquals(Arrays.asList(2,1),page);
    }
}
