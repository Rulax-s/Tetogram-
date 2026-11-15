/* Tetogram, 2026. GNU GPL v2 or later. Reusable pointer handling; normalized math is JVM tested. */
package org.telegram.ui.tetogram;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import org.telegram.messenger.tetogram.AppearanceViewportEditor;

public final class AppearanceViewportGestures implements View.OnTouchListener {
    public interface Target {
        boolean geometry();
        void changed();
    }
    private final AppearanceViewportEditor state;
    private final Target target;
    private final ScaleGestureDetector scale;
    private final GestureDetector taps;
    private int pointer = -1;
    private float lastX, lastY, focusX, focusY;

    public AppearanceViewportGestures(Context context, AppearanceViewportEditor state, Target target) {
        this.state = state;
        this.target = target;
        scale = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                focusX = detector.getFocusX(); focusY = detector.getFocusY();
                return target.geometry();
            }
            @Override public boolean onScale(ScaleGestureDetector detector) {
                if (!target.geometry()) return false;
                state.scale(detector.getScaleFactor(), focusX, focusY, detector.getFocusX(), detector.getFocusY());
                focusX = detector.getFocusX(); focusY = detector.getFocusY();
                target.changed(); return true;
            }
        });
        scale.setQuickScaleEnabled(false);
        taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onDoubleTap(MotionEvent e) { state.reset(); target.changed(); return true; }
        });
        taps.setIsLongpressEnabled(false);
    }

    @Override public boolean onTouch(View view, MotionEvent event) {
        if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(true);
        scale.onTouchEvent(event);
        taps.onTouchEvent(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            pointer = event.getPointerId(0); lastX = event.getX(); lastY = event.getY();
        } else if (action == MotionEvent.ACTION_MOVE) {
            int index = event.findPointerIndex(pointer);
            if (index >= 0) {
                float x = event.getX(index), y = event.getY(index);
                if (event.getPointerCount() == 1 && !scale.isInProgress() && target.geometry()) {
                    state.drag(x - lastX, y - lastY); target.changed();
                }
                lastX = x; lastY = y;
            }
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            int lifted = event.getActionIndex();
            int remaining = lifted == 0 ? 1 : 0;
            pointer = event.getPointerId(remaining);
            lastX = event.getX(remaining); lastY = event.getY(remaining);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // The last rendered coordinates are final. No release-time snap, fling or asynchronous mutation.
            pointer = -1;
            if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }
}
