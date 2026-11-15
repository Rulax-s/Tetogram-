/* Tetogram, 2026. GNU GPL v2 or later. Gesture math in the existing normalized wallpaper coordinates. */
package org.telegram.messenger.tetogram;

public final class AppearanceViewportEditor {
    private final AppearanceViewport bounds = new AppearanceViewport();
    private float viewportWidth, viewportHeight, sourceWidth, sourceHeight;
    private boolean fit;
    public float zoom, panX, panY;

    public AppearanceViewportEditor(float zoom, float panX, float panY) {
        this.zoom = bound(zoom, 1, 1, 4);
        this.panX = bound(panX, 0, -1, 1);
        this.panY = bound(panY, 0, -1, 1);
    }

    private static float bound(float value, float fallback, float min, float max) {
        return Float.isNaN(value) || Float.isInfinite(value) ? fallback : Math.max(min, Math.min(max, value));
    }

    public boolean geometry(float width, float height, float sourceWidth, float sourceHeight, boolean fit) {
        if (!bounds.set(width, height, sourceWidth, sourceHeight, fit, zoom, panX, panY)) return false;
        this.viewportWidth = width;
        this.viewportHeight = height;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.fit = fit;
        return true;
    }

    private void position(float x, float y) {
        float halfX = Math.abs(viewportWidth - bounds.width) / 2;
        float halfY = Math.abs(viewportHeight - bounds.height) / 2;
        panX = halfX < .001f ? 0 : bound((x - (viewportWidth - bounds.width) / 2) / halfX, 0, -1, 1);
        panY = halfY < .001f ? 0 : bound((y - (viewportHeight - bounds.height) / 2) / halfY, 0, -1, 1);
    }

    public void drag(float dx, float dy) {
        if (!geometry(viewportWidth, viewportHeight, sourceWidth, sourceHeight, fit)) return;
        position(bounds.x + bound(dx, 0, -viewportWidth * 4, viewportWidth * 4),
                bounds.y + bound(dy, 0, -viewportHeight * 4, viewportHeight * 4));
    }

    /** Keep the source point below the old centroid under the new centroid. No timestamp/velocity or release snap. */
    public void scale(float factor, float oldFocusX, float oldFocusY, float newFocusX, float newFocusY) {
        if (!geometry(viewportWidth, viewportHeight, sourceWidth, sourceHeight, fit)) return;
        float oldZoom = zoom, x = bounds.x, y = bounds.y;
        zoom = bound(oldZoom * bound(factor, 1, .01f, 100), oldZoom, 1, 4);
        float ratio = zoom / oldZoom;
        bounds.set(viewportWidth, viewportHeight, sourceWidth, sourceHeight, fit, zoom, panX, panY);
        position(newFocusX - (oldFocusX - x) * ratio, newFocusY - (oldFocusY - y) * ratio);
    }

    public void reset() { zoom = 1; panX = panY = 0; }
}
