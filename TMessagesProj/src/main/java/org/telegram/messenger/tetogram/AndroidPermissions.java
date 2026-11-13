/* Tetogram, 2026. GNU GPL v2 or later. OS state is authoritative, never cached grants. */
package org.telegram.messenger.tetogram;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import org.telegram.messenger.FileLog;
public final class AndroidPermissions {
    private AndroidPermissions() {}
    private static boolean granted(Context c,String p) { return Build.VERSION.SDK_INT<23 || c!=null && c.checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED; }
    public static boolean images(Context c) { return PermissionPolicy.canReadImages(Build.VERSION.SDK_INT,granted(c,Manifest.permission.READ_EXTERNAL_STORAGE),granted(c,Manifest.permission.READ_MEDIA_IMAGES),selected(c)); }
    public static boolean videos(Context c) { return PermissionPolicy.canReadVideos(Build.VERSION.SDK_INT,granted(c,Manifest.permission.READ_EXTERNAL_STORAGE),granted(c,Manifest.permission.READ_MEDIA_VIDEO),selected(c)); }
    public static boolean selected(Context c) { return Build.VERSION.SDK_INT>=34 && granted(c,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED); }
    public static boolean gallery(Context c) { return c!=null && (images(c)||videos(c)); }
    public static boolean notifications(Context c) { return c!=null && NotificationManagerCompat.from(c).areNotificationsEnabled(); }
    public static boolean shouldRequestNotifications(Context c) { return c!=null && PermissionPolicy.requestNotifications(Build.VERSION.SDK_INT,granted(c,Manifest.permission.POST_NOTIFICATIONS),notifications(c)); }
    public static String[] mediaRequest() {
        if(Build.VERSION.SDK_INT>=34)return new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};
        if(Build.VERSION.SDK_INT>=33)return new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO};
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }
    public static void requestGallery(Activity activity,int code,boolean reselect) {
        if(activity==null || Build.VERSION.SDK_INT<23 || !reselect && gallery(activity) || Build.VERSION.SDK_INT>=33 && granted(activity,Manifest.permission.READ_MEDIA_IMAGES) && granted(activity,Manifest.permission.READ_MEDIA_VIDEO))return;
        activity.requestPermissions(mediaRequest(),code);
    }
    /** Opt-in diagnostics: OS booleans only, no identifiers, paths, contacts, channels or session data. */
    public static void diagnose(Context c) {
        if(c==null || !c.getSharedPreferences("tetogram_diagnostics",0).getBoolean("permissions",false))return;
        FileLog.d("Tetogram permissions sdk="+Build.VERSION.SDK_INT+" images="+images(c)+" videos="+videos(c)+" selected="+selected(c)+" post="+granted(c,Manifest.permission.POST_NOTIFICATIONS)+" notifications="+notifications(c));
    }
}
