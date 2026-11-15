/* Tetogram, 2026. GPL v2 or later. Local profile target and media ownership are different identities. */
package org.telegram.messenger.tetogram;
public final class AppearanceProfilePolicy {
    private AppearanceProfilePolicy() {}
    public static long target(long user,long chat){return user>0?user:chat>0?-chat:0;}
    public static boolean active(long owner,long target,boolean explicit){return owner>0&&target!=0&&(owner==target||explicit);}
}
