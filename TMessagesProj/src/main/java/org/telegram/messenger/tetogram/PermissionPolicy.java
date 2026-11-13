/* Tetogram, 2026. GNU GPL v2 or later. Pure Android permission decisions. */
package org.telegram.messenger.tetogram;
public final class PermissionPolicy {
    private PermissionPolicy() {}
    public static boolean canReadImages(int sdk,boolean legacy,boolean images,boolean selected) {
        return sdk<23 || (sdk<33 ? legacy : images || sdk>=34 && selected);
    }
    public static boolean canReadVideos(int sdk,boolean legacy,boolean videos,boolean selected) {
        return sdk<23 || (sdk<33 ? legacy : videos || sdk>=34 && selected);
    }
    public static boolean canOpenGallery(int sdk,boolean legacy,boolean images,boolean videos,boolean selected) {
        return canReadImages(sdk,legacy,images,selected) || canReadVideos(sdk,legacy,videos,selected);
    }
    public static boolean requestNotifications(int sdk,boolean granted,boolean enabled) { return sdk>=33 && !granted && !enabled; }
}
