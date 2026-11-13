/* Tetogram, 2026. GNU GPL v2 or later. Pure Java, shared by live UI and unit tests. */
package org.telegram.messenger.tetogram;

import java.util.ArrayList;
import java.util.Collections;

public final class HistoryPagePolicy {
    private HistoryPagePolicy() {}
    public interface Id<T> { int get(T value); }
    /** Keep a bounded contiguous slice in the requested direction, preserving its next cursor. */
    public static <T> boolean trim(ArrayList<T> values,int count,int anchor,int loadType,Id<T> id) {
        int limit=Math.max(count,1);
        if((loadType==2 || loadType==3 || loadType==4) && anchor>0)
            Collections.sort(values,(x,y)->Long.compare(Math.abs((long)id.get(x)-anchor),Math.abs((long)id.get(y)-anchor)));
        else if(loadType==1) Collections.sort(values,(x,y)->Integer.compare(id.get(x),id.get(y)));
        else Collections.sort(values,(x,y)->Integer.compare(id.get(y),id.get(x)));
        boolean more=values.size()>limit;
        if(more)values.subList(limit,values.size()).clear();
        Collections.sort(values,(x,y)->Integer.compare(id.get(y),id.get(x)));
        return more;
    }
    public static String archiveOrder(int anchor,int loadType) {
        if(loadType==1)return "mid ASC";
        if((loadType==2 || loadType==3 || loadType==4) && anchor>0)return "ABS(mid-"+anchor+"),mid DESC";
        return "mid DESC";
    }
}
