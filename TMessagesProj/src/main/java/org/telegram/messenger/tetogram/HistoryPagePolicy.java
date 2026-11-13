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
    public interface Message<T> extends Id<T> { int date(T value); }
    /** Select regular message cursors only. Preserve the native source order and all its
     * ephemeral/pending rows, then insert local copies using the native mixed comparator.
     * Packed ephemeral IDs must never participate in regular ID bounds or count trimming.
     */
    public static <T> boolean merge(ArrayList<T> result, ArrayList<T> source, ArrayList<T> local,
                                    int count, int anchor, int loadType, Message<T> access) {
        ArrayList<T> regular = new ArrayList<>();
        java.util.HashSet<Integer> known = new java.util.HashSet<>();
        for (T value : source) {
            int id = access.get(value);
            known.add(id);
            if (id > 0 && !MessageTimelinePolicy.ephemeral(id)) regular.add(value);
        }
        ArrayList<T> copies = new ArrayList<>();
        for (T value : local) {
            int id = access.get(value);
            if (id > 0 && !MessageTimelinePolicy.ephemeral(id) && known.add(id)) {
                copies.add(value);
                regular.add(value);
            }
        }
        boolean more = trim(regular, count, anchor, loadType, access);
        java.util.HashSet<Integer> selected = new java.util.HashSet<>();
        for (T value : regular) selected.add(access.get(value));
        result.clear();
        for (T value : source) {
            int id = access.get(value);
            if (id <= 0 || MessageTimelinePolicy.ephemeral(id) || selected.contains(id)) result.add(value);
        }
        // Regular IDs are already ordered by trim; adding them oldest first also preserves
        // the source's established order for equal dates and native welcome-anchor rows.
        for (int i = regular.size() - 1; i >= 0; i--) {
            T value = regular.get(i);
            if (!copies.contains(value)) continue;
            int pos = 0;
            while (pos < result.size() && MessageTimelinePolicy.compare(access.get(value), access.date(value),
                    access.get(result.get(pos)), access.date(result.get(pos))) >= 0) pos++;
            result.add(pos, value);
        }
        return more;
    }
    public static String archiveOrder(int anchor,int loadType) {
        if(loadType==1)return "mid ASC";
        if((loadType==2 || loadType==3 || loadType==4) && anchor>0)return "ABS(mid-"+anchor+"),mid DESC";
        return "mid DESC";
    }
}
