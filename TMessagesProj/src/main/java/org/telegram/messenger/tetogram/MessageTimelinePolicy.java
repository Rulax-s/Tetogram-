/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.messenger.tetogram;

/** Telegram's MessagesStorage history ordering, including its reserved ephemeral ID space.
 * IDs remain IDs; dates are used only in the same mixed/ephemeral cases as upstream.
 */
public final class MessageTimelinePolicy {
    private MessageTimelinePolicy() {}
    public static boolean ephemeral(int id) { return (id & 0x70000000) == 0x60000000; }
    public static int compare(int left, int leftDate, int right, int rightDate) {
        boolean le = ephemeral(left), re = ephemeral(right);
        if (le || re) {
            int byDate = Integer.compare(rightDate, leftDate);
            if (byDate != 0) return byDate;
            if (le != re) return le ? -1 : 1;
            return Integer.compare(left, right);
        }
        if (left > 0 && right > 0) return Integer.compare(right, left);
        if (left < 0 && right < 0) return Integer.compare(left, right);
        return Integer.compare(rightDate, leftDate);
    }
    public static boolean sameMessage(int account, long dialog, int id,
                                      int otherAccount, long otherDialog, int otherId) {
        return account == otherAccount && dialog == otherDialog && id == otherId;
    }
}
