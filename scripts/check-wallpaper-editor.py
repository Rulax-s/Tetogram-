#!/usr/bin/env python3
"""Integration contracts only. Does NOT execute Android touch events, TextureView or One UI."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main/java/org/telegram'
def source(path): return (ROOT / path).read_text()

class WallpaperEditorContracts(unittest.TestCase):
    def test_gesture_hot_path_does_not_configure_or_recreate_media(self):
        media = source('ui/tetogram/AppearanceMediaView.java')
        hot = media.split('public void setViewport(', 1)[1].split('public float sourceWidth()', 1)[0]
        for forbidden in ('configure(', 'setImage(', 'new VideoPlayer', 'preparePlayer', 'new File', 'requestLayout'):
            self.assertNotIn(forbidden, hot)
        self.assertIn('updateTransform()', hot)
        preview = source('ui/tetogram/AppearancePreview.java')
        hot = preview.split('public void setViewport(', 1)[1].split('public float sourceWidth()', 1)[0]
        self.assertNotIn('update(', hot); self.assertNotIn('removeAllViews', hot)

    def test_only_done_returns_position_and_parent_writes_only_draft(self):
        screen = source('ui/tetogram/AppearanceWallpaperActivity.java')
        self.assertEqual(1, screen.count('callback.done('))
        self.assertNotIn('AppearanceStore.save', screen)
        parent = source('ui/tetogram/AppearanceActivity.java').split('private void positionWallpaper()', 1)[1].split('private void mediaControls(', 1)[0]
        self.assertIn('draft!=openedDraft', parent); self.assertIn('if(!valid()', parent)
        self.assertIn('checkpoint()', parent)
        for key in ('zoom', 'panX', 'panY'): self.assertIn('draft.set(prefix+".' + key + '"', parent)
        self.assertNotIn('AppearanceStore.save', parent); self.assertNotIn('AppearanceMedia.prepare', parent)

    def test_native_non_list_glass_and_runtime_insets(self):
        screen = source('ui/tetogram/AppearanceWallpaperActivity.java')
        self.assertIn('getBaseSimpleGlass().setViews(root, simpleActionBar, getResourceProvider())', screen)
        self.assertIn('mSystemInsets.bottom', screen); self.assertIn('simpleActionBar.getMeasuredHeight()', screen)
        self.assertIn('super.onInsets(left, top, right, bottom)', screen)
        self.assertIn('public boolean canBeginSlide() { return false; }', screen)
        self.assertNotIn('statusBarHeight', screen)

    def test_pause_keeps_decoder_and_hidden_screen_releases_it(self):
        media = source('ui/tetogram/AppearanceMediaView.java')
        pause = media.split('public void setPlaybackPaused(', 1)[1].split('public void setScrollParallax(', 1)[0]
        self.assertIn('player.pause()', pause); self.assertNotIn('stopVideo()', pause); self.assertNotIn('releasePlayer', pause)
        screen = source('ui/tetogram/AppearanceWallpaperActivity.java')
        self.assertIn('preview.setResumed(false)', screen)
        self.assertIn('preview.captureForGlass', screen)

    def test_existing_domains_and_advanced_numeric_controls(self):
        parent = source('ui/tetogram/AppearanceActivity.java')
        self.assertIn('AppearanceSurfaces.list(source)', parent); self.assertIn('AppearanceSurfaces.banner(source)', parent)
        precise = parent.split('if(advanced){items.add(UItem.asHeader(getString(R.string.TetogramWallpaperPrecise)))', 1)[1].split('button(items,getString(R.string.TetogramAppearanceFit)', 1)[0]
        for key in ('zoom', 'panX', 'panY'): self.assertIn('mediaKey("'+key+'")', precise)
        self.assertIn('ACTION_POINTER_UP', source('ui/tetogram/AppearanceViewportGestures.java'))

if __name__ == '__main__': unittest.main(verbosity=2)
