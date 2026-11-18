#!/usr/bin/env python3
"""Source integration contracts. These do not execute Android views, gestures or a video decoder."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
def source(name):return (ROOT/'TMessagesProj/src/main/java/org/telegram'/name).read_text()
class EditorContracts(unittest.TestCase):
 def test_nine_distinct_home_routes(self):
  src=source('ui/tetogram/AppearanceActivity.java');home=src.split('if("home".equals(page)){',1)[1].split('if("presets".equals(page))',1)[0]
  self.assertEqual(9,home.count('            button(items,'))
  for category in ('COLORS','WALLPAPERS','LIST','CHATS','SECTIONS','ADVANCED'):self.assertIn('openCategory(Category.'+category+')',home)
  self.assertNotIn('TetogramAppearanceSimple',home)
 def test_pinned_actions_and_preview_are_not_list_rows(self):
  src=source('ui/tetogram/AppearanceActivity.java');fill=src.split('@Override protected void fill(',1)[1].split('private void simpleSettings(',1)[0]
  self.assertNotIn('this::applyDraft',fill);self.assertNotIn('TetogramAppearanceSavePreset',fill);self.assertNotIn('UItem.asCustom(preview',fill)
  self.assertIn('setTopContent(topPanel',fill);self.assertIn('setBottomActions(editorDock)',src);self.assertIn('AppearanceEditorPolicy.dirtyAppearance(baseline,draft)',src)
 def test_saves_never_mutate_before_success_and_keep_personal_data(self):
  src=source('ui/tetogram/AppearanceActivity.java')
  for bad in ('config.scopes.clear()','config.presets.remove(','config.presets.put(','config.scopes.put(scope,draft.copy())'):self.assertNotIn(bad,src)
  self.assertIn('AppearanceConfig.applyPreset(config.scopes.get(AppearanceConfig.GLOBAL),p.style)',src);self.assertIn('fresh.resetAppearance()',src);self.assertIn('fresh.putScope(scope,write)',src)
  self.assertIn('AppearanceConfig.withoutPersonal(preset.style)',source('messenger/tetogram/AppearanceArchive.java'))
  self.assertIn('AppearanceStore.resolveShared(',source('ui/Adapters/DialogsAdapter.java'))
  self.assertIn('syncAutoNight(mode)',source('messenger/tetogram/AppearanceRuntime.java'));self.assertNotIn('Theme.selectedAutoNightType=Theme.AUTO_NIGHT_TYPE_NONE',source('messenger/tetogram/AppearanceRuntime.java'))
 def test_preview_media_has_one_owner(self):
  src=source('ui/tetogram/AppearancePreview.java');update=src.split('public void update(',1)[1].split('private boolean resumed',1)[0]
  self.assertNotIn('new AppearanceMediaView',update);self.assertNotIn('removeAllViews();media',update);self.assertIn('media.configure(',update)
  media=source('ui/tetogram/AppearanceMediaView.java');self.assertIn('if(!sameSource){stop();resumePosition=0;',media);self.assertIn('instance.seekTo(resumePosition)',media)
  dialogs=source('ui/DialogsActivity.java');self.assertIn('tetogramListMedia.getParent()!=root',dialogs);self.assertIn('tetogramListTint.getParent()!=root',dialogs);self.assertIn('tetogramResetDetachedListLayers(root)',dialogs)
 def test_accessible_rows_grow_with_font_scale(self):
  row=source('ui/tetogram/AppearanceSettingRow.java');self.assertIn('COMPLEX_UNIT_SP',row);self.assertIn('setSingleLine(false)',row);self.assertIn('setMaxLines(Integer.MAX_VALUE)',row);self.assertNotIn('setEllipsize',row)
 def test_seasonal_icons_do_not_use_old_premium_selector(self):
  src=source('ui/tetogram/AppearanceLauncherActivity.java');self.assertNotIn('new AppIconsSelectorCell',src);self.assertIn('AppearanceIconPolicy.Season.values()',src);self.assertIn('setBottomActions(dock)',src);self.assertIn('new AppearanceIconPreview',src)
 def test_color_opens_picker_directly(self):
  src=source('ui/tetogram/AppearanceActivity.java');self.assertIn('private void colorDialog(String name,int color){pickerDialog(name,color);}',src);self.assertIn('new android.widget.SeekBar',src);self.assertIn('AppearanceColors.parse(text.toString())',src)
 def test_packaged_aliases_have_safe_layers_and_matching_names(self):
  import xml.etree.ElementTree as ET
  android='{http://schemas.android.com/apk/res/android}'
  manifest=ET.parse(ROOT/'TMessagesProj/src/main/AndroidManifest.xml').getroot()
  aliases=[a for a in manifest.iter('activity-alias') if a.get(android+'name','').split('.')[-1].startswith(('Teto','Miku'))]
  renderer=source('ui/tetogram/AppearanceIconPreview.java');self.assertIn('backdrop.draw(canvas)',renderer)
  self.assertIn('shape,icon.background',source('ui/tetogram/AppearanceLauncherActivity.java'));self.assertIn('iconBackground(true)',source('ui/tetogram/AppearanceShortcutActivity.java'))
  self.assertEqual(15,len(aliases))
  controller=source('ui/LauncherIconController.java')
  for alias in aliases:
   self.assertIn('"'+alias.get(android+'name').split('.')[-1]+'"',controller)
   if alias.find('intent-filter') is None:
    self.assertIsNone(alias.get(android+'icon'));self.assertEqual('false',alias.get(android+'exported'));continue
   icon=alias.get(android+'icon').split('/')[-1]
   drawable=ET.parse(ROOT/'TMessagesProj/src/main/res/mipmap-anydpi-v26'/str(icon+'.xml')).getroot()
   inset=drawable.find('foreground/inset');self.assertIsNotNone(inset)
   for edge in ('Left','Top','Right','Bottom'):self.assertEqual('24%' if alias.get(android+'name').endswith('Icon') else '28.5%',inset.get(android+'inset'+edge))
   self.assertEqual('false',alias.get(android+'enabled'))
   self.assertEqual('org.telegram.ui.LaunchActivity',alias.get(android+'targetActivity'))

if __name__=='__main__':unittest.main(verbosity=2)
