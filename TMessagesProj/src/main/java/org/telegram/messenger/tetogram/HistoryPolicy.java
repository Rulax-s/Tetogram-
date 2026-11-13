/* Tetogram, 2026. GNU GPL v2 or later. No Android dependencies: unit-tested in CI. */
package org.telegram.messenger.tetogram;

public final class HistoryPolicy {
    private HistoryPolicy() {}
    public static boolean maySave(boolean enabled, boolean regular, boolean encrypted, boolean outgoing,
            boolean protectedContent, boolean excluded, int messageId, int ttl, int ttlPeriod, int mediaTtl, int expireDate) {
        return enabled && regular && !encrypted && !outgoing && !protectedContent && !excluded
                && messageId>0 && ttl==0 && ttlPeriod==0 && mediaTtl==0 && expireDate==0;
    }
    public static int retention(int value) { return value==0 || value==7 || value==30 || value==90 ? value : 30; }
}
