/* Tetogram, 2026. GPL v2 or later. No Android dependency. */
package org.telegram.messenger.tetogram;
public final class EditHistoryPolicy {
    private EditHistoryPolicy() {}
    public static boolean changed(int previousEdit,int nextEdit,String before,String after) {
        return nextEdit>0 && nextEdit>=previousEdit && before!=null && after!=null && !before.equals(after);
    }
    public static boolean visible(int versions,boolean mark,boolean eligible) { return versions>=2 && mark && eligible; }
    public static boolean append(int previousEdit,String before,int nextEdit,String after){return after!=null&&(before==null||nextEdit>=previousEdit&&!before.equals(after));}
    public static String key(long owner,long did,int mid) { return owner+":"+did+":"+mid; }
}
