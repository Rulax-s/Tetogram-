/* Tetogram changes, 2026. Licensed under GNU GPL v2 or later. */
package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;

/** Text branding drawn in the existing wordmark slot, with the existing theme tint. */
public final class TetogramWordmarkDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int width;
    private final int height;

    public TetogramWordmarkDrawable(int widthDp, int heightDp) {
        width = AndroidUtilities.dp(widthDp);
        height = AndroidUtilities.dp(heightDp);
        paint.setColor(Color.WHITE);
        paint.setTypeface(AndroidUtilities.bold());
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        paint.setTextSize(bounds.height() * 0.85f);
        float measured = paint.measureText("Tetogram");
        if (measured > bounds.width()) {
            paint.setTextSize(paint.getTextSize() * bounds.width() / measured);
        }
        float baseline = bounds.exactCenterY() - (paint.ascent() + paint.descent()) / 2;
        canvas.drawText("Tetogram", bounds.left, baseline, paint);
    }

    @Override public int getIntrinsicWidth() { return width; }
    @Override public int getIntrinsicHeight() { return height; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
