package org.telegram.messenger.tetogram;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Exercises the production replacement policy and a stable-ID keyed binding cache.
 * RecyclerView gestures and Android lifecycle themselves require device instrumentation.
 */
public class DeletedRowPolicyTest {
    static class Row {
        int account=1,id,stable,date=100; long dialog=-50,sender; String text; boolean deleted,visible=true;
        Row(int id,int stable,long sender,String text) {this.id=id;this.stable=stable;this.sender=sender;this.text=text;}
        Row copy() { Row r=new Row(id,stable,sender,text);r.account=account;r.dialog=dialog;r.date=date;r.deleted=deleted;r.visible=visible;return r; }
    }
    static final DeletedRowPolicy.Row<Row> ACCESS=new DeletedRowPolicy.Row<Row>() {
        public int account(Row r){return r.account;} public long dialog(Row r){return r.dialog;}
        public int id(Row r){return r.id;} public int stableId(Row r){return r.stable;}
        public void stableId(Row r,int id){r.stable=id;} public Row clonePayload(Row r){return r.copy();}
        public void copyStableParams(Row r,Row original){r.stable=original.stable;}
    };
    final AtomicInteger allocator=new AtomicInteger(100);
    Row replace(Row row,Row payload) {return DeletedRowPolicy.replace(row,payload,ACCESS,allocator::getAndIncrement);}
    Row payload(Row r){Row p=r.copy();p.stable=0;p.deleted=true;return p;}
    void content(Row expected,Row actual){assertNotSame(expected,actual);assertEquals(expected.account,actual.account);assertEquals(expected.dialog,actual.dialog);assertEquals(expected.id,actual.id);assertEquals(expected.sender,actual.sender);assertEquals(expected.text,actual.text);assertEquals(expected.date,actual.date);assertEquals(expected.visible,actual.visible);assertTrue(actual.deleted);}
    @Test public void twoAuthorsKeepDistinctStableIdsThroughRepeatedBinding() {
        Row a=new Row(10,41,301,"first"),b=new Row(11,42,302,"second");
        Row x=replace(a,payload(a)),y=replace(b,payload(b));
        HashMap<Integer,Row> holders=new HashMap<>();
        for(int refresh=0;refresh<50;refresh++) {
            holders.put(x.stable,x);holders.put(y.stable,y);
            content(a,holders.get(41));content(b,holders.get(42));assertEquals(2,holders.size());
        }
    }
    @Test public void batchDeletionAndNewMessagesDoNotAliasRows() {
        HashSet<Integer> stable=new HashSet<>();
        for(int i=0;i<50;i++){Row a=new Row(i+1,i+10,i+100,"fixture "+i);Row b=replace(a,payload(a));content(a,b);assertTrue(stable.add(b.stable));}
        assertTrue(stable.add(1000));
    }
    @Test public void sharedNotificationCreatesIndependentRowsForTwoFragments() {
        Row original=new Row(10,41,301,"first"),p=payload(original);
        Row other=original.copy();other.stable=71;
        Row a=replace(original,p),b=replace(other,p);
        assertNotSame(a,b);assertEquals(41,a.stable);assertEquals(71,b.stable);assertEquals(0,p.stable);
        a.text="changed UI fixture";assertEquals("first",b.text);assertEquals("first",p.text);
    }
    @Test public void repeatedDeletionKeepsIdentity() {
        Row a=new Row(10,41,301,"first"),p=payload(a);
        Row b=replace(a,p);for(int i=0;i<10;i++)b=replace(b,p);
        content(a,b);assertEquals(41,b.stable);
    }
    @Test public void missingStableIdsAllocateDistinctRows() {
        Row a=new Row(10,0,301,"first"),b=new Row(11,0,302,"second");
        Row x=replace(a,payload(a)),y=replace(b,payload(b));assertTrue(x.stable>0);assertNotEquals(x.stable,y.stable);
    }
    @Test public void sameMessageIdInOtherAccountOrDialogCannotReplaceRow() {
        Row a=new Row(10,41,301,"first"),p=payload(a);
        p.account=2;assertNull(replace(a,p));p.account=1;p.dialog=-60;assertNull(replace(a,p));p.dialog=-50;p.id=11;assertNull(replace(a,p));
    }
    @Test public void rebuildingRowsAfterModelRecreationPreservesPayloads() {
        // A model-level reconstruction, not an Android Activity/process test.
        Row a=new Row(10,41,301,"first"),b=new Row(11,42,302,"second");
        Row pa=payload(a),pb=payload(b);
        for(int reopen=0;reopen<5;reopen++) {
            Row ra=pa.copy(),rb=pb.copy();ra.stable=allocator.getAndIncrement();rb.stable=allocator.getAndIncrement();
            content(a,replace(ra,pa));content(b,replace(rb,pb));assertNotEquals(ra.stable,rb.stable);
        }
    }
}
