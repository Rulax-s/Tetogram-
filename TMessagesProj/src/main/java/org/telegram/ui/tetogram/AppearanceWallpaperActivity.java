/* Tetogram, 2026. GNU GPL v2 or later. Full-window photo/video positioning, private until Done. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.tetogram.AppearanceConfig;
import org.telegram.messenger.tetogram.AppearanceViewportEditor;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.LayoutHelper;

public final class AppearanceWallpaperActivity extends BaseFragment {
    public interface Result { void done(float zoom, float panX, float panY); }
    private final long owner;
    private final AppearanceConfig.Resolved style;
    private final AppearanceViewportEditor state;
    private final boolean dark, video, banner;
    private Result result;
    private AppearancePreview preview;
    private LinearLayout controls, dock;
    private TextView sceneButton, pauseButton;
    private boolean showChat, paused;

    public AppearanceWallpaperActivity(int account, AppearanceConfig.Resolved source, boolean dark, boolean banner, Result result) {
        setCurrentAccount(account);
        owner = UserConfig.getInstance(account).getClientUserId();
        style = new AppearanceConfig.Resolved();
        style.values.putAll(source.values); style.origins.putAll(source.origins);
        this.dark = dark; this.banner = banner; this.result = result;
        video = "video".equals(style.get("wallpaper.type"));
        state = new AppearanceViewportEditor(style.number("wallpaper.zoom", 1), style.number("wallpaper.panX", 0), style.number("wallpaper.panY", 0));
    }
    private boolean valid() { return owner > 0 && owner == getUserConfig().getClientUserId(); }
    @Override protected boolean useSimpleActionBar() { return true; }
    @Override public boolean isSupportEdgeToEdge() { return true; }
    @Override public boolean canBeginSlide() { return false; } // One-finger positioning must never become swipe-back.

    @Override public View createView(Context context) {
        if (preview != null) preview.setResumed(false);
        FrameLayout root = new FrameLayout(context) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                if (banner && preview != null) {
                    int w = MeasureSpec.getSize(widthSpec), h = MeasureSpec.getSize(heightSpec);
                    float targetHeight = AndroidUtilities.dp(Math.round(style.number("banner.height", 280)));
                    float ratio = Math.min(1, h / Math.max(1, targetHeight));
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) preview.getLayoutParams();
                    lp.width = Math.round(w * ratio); lp.height = Math.round(targetHeight * ratio);
                }
                super.onMeasure(widthSpec, heightSpec);
            }
        };
        fragmentView = root; setHasOwnBackground(true);
        // The media occupies the full native fragment viewport, not a compressed settings-row rectangle.
        preview = new AppearancePreview(context, currentAccount, style, dark, 0);
        preview.showScene(showChat); preview.setViewport(state.zoom, state.panX, state.panY);
        preview.setPlaybackPaused(paused);
        root.addView(preview, LayoutHelper.createFrame(-1, -1, Gravity.CENTER));
        preview.setOnTouchListener(new AppearanceViewportGestures(context, state, new AppearanceViewportGestures.Target() {
            @Override public boolean geometry() {
                return valid() && preview.sourceWidth() > 1 && preview.sourceHeight() > 1 && state.geometry(preview.getWidth(), preview.getHeight(), preview.sourceWidth(), preview.sourceHeight(), "fit".equals(style.get("wallpaper.fit")));
            }
            @Override public void changed() { preview.setViewport(state.zoom, state.panX, state.panY); }
        }));
        controls = new LinearLayout(context); controls.setOrientation(LinearLayout.VERTICAL);
        ThemeBind.backgroundColor(controls, Theme.key_windowBackgroundWhite);
        sceneButton = button(getString(showChat ? R.string.TetogramWallpaperWithChat : R.string.TetogramWallpaperOnly));
        sceneButton.setOnClickListener(v -> { showChat = !showChat; preview.showScene(showChat); sceneButton.setText(getString(showChat ? R.string.TetogramWallpaperWithChat : R.string.TetogramWallpaperOnly)); });
        controls.addView(sceneButton, new LinearLayout.LayoutParams(-1, -2));
        if (video) {
            pauseButton = button(getString(paused ? R.string.TetogramWallpaperPlay : R.string.TetogramWallpaperPause));
            pauseButton.setOnClickListener(v -> { paused = !paused; preview.setPlaybackPaused(paused); pauseButton.setText(getString(paused ? R.string.TetogramWallpaperPlay : R.string.TetogramWallpaperPause)); });
            controls.addView(pauseButton, new LinearLayout.LayoutParams(-1, -2));
        }
        root.addView(controls, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        dock = new LinearLayout(context); ThemeBind.backgroundColor(dock, Theme.key_windowBackgroundWhite);
        TextView cancel = button(getString(R.string.Cancel)); cancel.setOnClickListener(v -> finishFragment());
        TextView reset = button(getString(R.string.TetogramWallpaperReset)); reset.setOnClickListener(v -> { state.reset(); preview.setViewport(state.zoom, state.panX, state.panY); });
        TextView done = button(getString(R.string.Done)); done.setOnClickListener(v -> {
            if (!valid() || result == null) return;
            Result callback = result; result = null;
            callback.done(state.zoom, state.panX, state.panY); finishFragment();
        });
        for (TextView button : new TextView[]{cancel, reset, done}) dock.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(dock, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        controls.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> updateInsets());
        dock.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> updateInsets());
        simpleActionBar.setTitle(getString(R.string.TetogramWallpaperPosition));
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { @Override public void onItemClick(int id) { if (id == -1) finishFragment(); } });
        // Native non-list SimpleGlass integration: the cover is used for capture, never a second video player.
        getBaseSimpleGlass().setViews(root, simpleActionBar, getResourceProvider());
        getBaseSimpleGlass().setCapture((canvas, position) -> {
            int save = canvas.save(); canvas.translate(preview.getX(), preview.getY());
            android.graphics.RectF local = new android.graphics.RectF(position); local.offset(-preview.getX(), -preview.getY());
            preview.captureForGlass(canvas, local); canvas.restoreToCount(save);
        });
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> updateInsets());
        onInsets(mSystemInsets.left, mSystemInsets.top, mSystemInsets.right, mSystemInsets.bottom);
        return root;
    }
    private TextView button(CharSequence text) {
        TextView view = new TextView(getContext()); view.setText(text); view.setTextSize(15); view.setGravity(Gravity.CENTER);
        view.setMinHeight(AndroidUtilities.dp(48));
        view.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10));
        ThemeBind.textColor(view, Theme.key_windowBackgroundWhiteBlueText);
        view.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        return view;
    }
    @Override public void onInsets(int left, int top, int right, int bottom) { super.onInsets(left, top, right, bottom); updateInsets(); }
    private void updateInsets() {
        if (controls == null || dock == null) return;
        FrameLayout.LayoutParams top = (FrameLayout.LayoutParams) controls.getLayoutParams();
        int header = Math.max(simpleActionBar.getMeasuredHeight(), mSystemInsets.top + ActionBar.getCurrentActionBarHeight());
        if (top.topMargin != header || top.leftMargin != mSystemInsets.left || top.rightMargin != mSystemInsets.right) {
            top.topMargin = header; top.leftMargin = mSystemInsets.left; top.rightMargin = mSystemInsets.right; controls.setLayoutParams(top);
        }
        FrameLayout.LayoutParams bottom = (FrameLayout.LayoutParams) dock.getLayoutParams();
        if (bottom.bottomMargin != mSystemInsets.bottom || bottom.leftMargin != mSystemInsets.left || bottom.rightMargin != mSystemInsets.right) {
            bottom.bottomMargin = mSystemInsets.bottom; bottom.leftMargin = mSystemInsets.left; bottom.rightMargin = mSystemInsets.right; dock.setLayoutParams(bottom);
        }
        preview.sceneInsets(Math.max(0, header + controls.getMeasuredHeight() - preview.getTop()),
                Math.max(0, preview.getBottom() - (fragmentView.getHeight() - mSystemInsets.bottom - dock.getMeasuredHeight())));
    }
    @Override public void onResume() { super.onResume(); if (preview != null) preview.setResumed(valid()); }
    @Override public void onPause() { if (preview != null) preview.setResumed(false); super.onPause(); }
    @Override public void onFragmentDestroy() { result = null; if (preview != null) preview.setResumed(false); getBaseSimpleGlass().setCapture(null); super.onFragmentDestroy(); }
}
