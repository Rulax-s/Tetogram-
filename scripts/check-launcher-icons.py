#!/usr/bin/env python3
"""Launcher source/resource contracts. Never claims actual PackageManager or Samsung UI execution."""
import json,subprocess,sys,unittest,xml.etree.ElementTree as E
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];JAVA=ROOT/'TMessagesProj/src/main/java/org/telegram';RES=ROOT/'TMessagesProj/src/main/res';A='{http://schemas.android.com/apk/res/android}'
def src(path):return (JAVA/path).read_text()
class LauncherContracts(unittest.TestCase):
 def test_generated_catalog_and_manifest_are_reproducible(self):subprocess.run([sys.executable,str(ROOT/'scripts/prepare-seasonal-icons.py'),'--check'],check=True,stdout=subprocess.PIPE)
 def test_all_eight_canonical_components_exist_and_target_original_activity(self):
  expected=set(json.loads((ROOT/'assets/launcher/seasonal/catalog.json').read_text()));self.assertEqual(8,len(expected))
  root=E.parse(RES.parent/'AndroidManifest.xml').getroot();aliases={e.get(A+'name').split('.')[-1]:e for e in root.iter('activity-alias')}
  for key in expected:
   node=aliases[key];self.assertEqual('false',node.get(A+'enabled'));self.assertEqual('org.telegram.ui.LaunchActivity',node.get(A+'targetActivity'));self.assertEqual('Mikugram' if key.startswith('Miku') else 'Tetogram',node.get(A+'label'))
 def test_only_real_artwork_can_be_selected_no_fake_summer_icon(self):
  catalog=src('ui/tetogram/SeasonalIconCatalog.java');controller=src('ui/LauncherIconController.java');ui=src('ui/tetogram/AppearanceLauncherActivity.java')
  self.assertIn('SeasonalIconCatalog.resource(icon.key)!=0',controller);self.assertIn('if(!hasArtwork(desired))throw',controller);self.assertIn('LauncherIconController.hasArtwork(target())',ui)
  manifest=E.parse(RES.parent/'AndroidManifest.xml').getroot();aliases={e.get(A+'name').split('.')[-1]:e for e in manifest.iter('activity-alias')}
  for key in json.loads((ROOT/'assets/launcher/seasonal/catalog.json').read_text()):
   absent=f'case "{key}":return 0;' in catalog
   if absent:self.assertIsNone(aliases[key].get(A+'icon'));self.assertIsNone(aliases[key].find('intent-filter'));self.assertEqual('false',aliases[key].get(A+'exported'))
   else:self.assertIsNotNone(aliases[key].get(A+'icon'));self.assertIsNotNone(aliases[key].find('intent-filter'))
 def test_safe_square_has_one_inset_and_no_extra_opaque_frame(self):
  manifest=E.parse(RES.parent/'AndroidManifest.xml').getroot()
  for node in manifest.iter('activity-alias'):
   key=node.get(A+'name','').split('.')[-1]
   if not key.startswith(('Tetogram','Mikugram')) or node.find('intent-filter') is None:continue
   name=node.get(A+'icon').split('/')[-1];root=E.parse(RES/'mipmap-anydpi-v26'/(name+'.xml')).getroot();self.assertEqual(1,len(root.findall('.//inset')))
   inset=root.find('foreground/inset')
   for edge in ('Left','Top','Right','Bottom'):self.assertEqual('28.5%',inset.get(A+'inset'+edge))
   self.assertNotIn('stroke',E.tostring(root,encoding='unicode'));legacy=E.parse(RES/'mipmap-anydpi'/(name+'.xml')).getroot();self.assertIsNone(legacy.find('.//corners'));self.assertEqual('46.44dp',legacy.findall('item')[1].get(A+'width'))
 def test_alias_transaction_is_enable_first_and_never_disables_main_activity(self):
  policy=src('messenger/tetogram/LauncherIconSwitch.java');self.assertLess(policy.index('access.enable(desired)'),policy.index('access.disable(key)'));self.assertIn('if(!known.contains(desired))',policy)
  controller=src('ui/LauncherIconController.java');self.assertIn('LauncherIconSwitch.apply(icon.key,known',controller);self.assertIn('PackageManager.DONT_KILL_APP',controller);self.assertNotIn('new ComponentName(context, LaunchActivity.class)',controller)
 def test_device_settings_do_not_depend_on_account_appearance_storage(self):
  controller=src('ui/LauncherIconController.java');ui=src('ui/tetogram/AppearanceLauncherActivity.java');shortcut=src('ui/tetogram/AppearanceShortcutActivity.java')
  self.assertIn('getSharedPreferences("tetogram_launcher",Context.MODE_PRIVATE)',controller);self.assertNotIn('UserConfig',controller);self.assertNotIn('AppearanceStore',ui);self.assertNotIn('LauncherIconController.setSeasonalIcon(',shortcut);self.assertNotIn('LauncherIconController.applySelection(',shortcut);self.assertNotIn('LauncherIconController.setIcon(',shortcut);self.assertIn('AppearanceStore.save(',shortcut)
 def test_calendar_uses_allowed_events_without_polling_or_new_services(self):
  controller=src('ui/LauncherIconController.java')
  for event in ('ACTION_DATE_CHANGED','ACTION_TIMEZONE_CHANGED','ACTION_TIME_CHANGED'):self.assertIn(event,controller)
  self.assertIn('LauncherIconController.refreshSeasonalIcon()',src('ui/LaunchActivity.java'));self.assertIn('LauncherIconController.tryFixLauncherIconIfNeeded()',src('messenger/ApplicationLoader.java'));self.assertNotIn('AlarmManager',controller);self.assertNotIn('startService(',controller);self.assertNotIn('postDelayed',controller)
 def test_migration_preserves_equivalent_active_alias_until_user_action(self):
  controller=src('ui/LauncherIconController.java');self.assertIn('AppearanceIconPolicy.sameArtwork(enabled.get(0).key',controller);self.assertIn('rememberLast(enabled.get(0));return;',controller);self.assertIn('if(s.corrupt)',controller);self.assertIn('next.revision=previous.revision+1',controller)
 def test_both_gallery_and_personal_shortcut_have_distinct_screens(self):
  ui=src('ui/tetogram/AppearanceLauncherActivity.java');shortcut=src('ui/tetogram/AppearanceShortcutActivity.java');self.assertIn('HorizontalScrollView gallery=',ui);self.assertIn('new AppearanceShortcutActivity(currentAccount)',ui);self.assertNotIn('new AppIconsSelectorCell',ui);self.assertIn('setUploadAfterSelect(false)',shortcut);self.assertIn('IconCompat.createWithAdaptiveBitmap',shortcut);self.assertIn('draft.brand=brand',ui);self.assertIn('draft.select(season)',ui)
if __name__=='__main__':unittest.main(verbosity=2)
