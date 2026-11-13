/* Tetogram changes, 2026. Licensed under GNU GPL v2 or later. */
package org.telegram.messenger;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/** Small fork-specific helpers. Never store session data or network clients here. */
public final class TetogramConfig {
    private TetogramConfig() {}

    /** SMS Retriever hash must match this package and its actual signing certificate. */
    @SuppressWarnings("deprecation")
    public static String getSmsHash() {
        try {
            String packageName = ApplicationLoader.applicationContext.getPackageName();
            PackageInfo info = ApplicationLoader.applicationContext.getPackageManager()
                    .getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            String input = packageName + " " + info.signatures[0].toCharsString();
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(Arrays.copyOf(digest, 9), Base64.NO_PADDING | Base64.NO_WRAP)
                    .substring(0, 11);
        } catch (Exception e) {
            return "";
        }
    }
}
