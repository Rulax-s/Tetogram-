/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.messenger.tetogram;

import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import java.util.HashSet;
import java.util.Set;

/** Versioned preferences belong to the Telegram user, never to a reusable account slot. */
public final class LabsSettings {
    private LabsSettings() {}
    public static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences(
                "tetogram_labs_v1_" + UserConfig.getInstance(account).getClientUserId(), 0);
    }
    public static boolean enabled(int account) {
        return UserConfig.getInstance(account).getClientUserId() != 0 && prefs(account).getBoolean("save_deleted", false);
    }
    public static boolean visible(int account) { return enabled(account) && prefs(account).getBoolean("show_deleted", true); }
    public static boolean mark(int account) { return prefs(account).getBoolean("deletion_mark", true); }
    public static int days(int account) {
        int value = prefs(account).getInt("retention_days", 30);
        return HistoryPolicy.retention(value);
    }
    public static boolean excluded(int account, long did) { return exclusions(account).contains(Long.toString(did)); }
    public static Set<String> exclusions(int account) { return new HashSet<>(prefs(account).getStringSet("excluded_dialogs", new HashSet<>())); }
    public static void exclude(int account, long did, boolean excluded) {
        Set<String> values = exclusions(account);
        if (excluded) values.add(Long.toString(did)); else values.remove(Long.toString(did));
        prefs(account).edit().putStringSet("excluded_dialogs", values).apply();
    }
}
