#!/usr/bin/env python3
"""Source integration contracts, NOT Android navigation/gesture/UI tests."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/'TMessagesProj/src/main/java/org/telegram'
def src(name):return (ROOT/name).read_text()
class SectionOpeningContracts(unittest.TestCase):
 def test_single_menu_and_existing_long_press_are_preserved(self):
  main=src('ui/MainTabsActivity.java');dialogs=src('ui/DialogsActivity.java')
  self.assertNotIn('new SectionMenu(',main);self.assertEqual(1,dialogs.count('new SectionMenu('))
  self.assertIn('setOnLongClickListener(this::openFoldersSelector)',main)
 def test_repeat_is_intercepted_only_inside_active_tab_branch(self):
  main=src('ui/MainTabsActivity.java');block=main.split('if (viewPager.getCurrentPosition() == position) {',1)[1].split('viewPager.scrollToPosition(position);',1)[0]
  self.assertIn('position==POSITION_CHATS',block);self.assertIn('fragment==dialogsActivity',block);self.assertIn('fragment.getCurrentAccount()==currentAccount',block)
  self.assertLess(block.index('toggleTetogramSectionsFromChats()'),block.index('onParentScrollToTop()'))
  self.assertNotIn('toggleTetogramSectionsFromChats()',block.split('onParentScrollToTop()',1)[1])
 def test_stale_or_ineligible_fragment_cannot_open(self):
  dialogs=src('ui/DialogsActivity.java').split('public boolean toggleTetogramSectionsFromChats()',1)[1].split('public void closeTetogramSectionsFromTabs',1)[0]
  for guard in ('isFinished','isPaused','isAttachedToWindow()','!isTetogramSectionList()','searching','!hasMainTabs','tetogramSectionsOwner==getUserConfig().getClientUserId()'):self.assertIn(guard,dialogs)
 def test_mode_two_has_no_hidden_fab_hit_target(self):
  text=src('ui/DialogsActivity.java');self.assertIn('setButtonVisible(isVisible&&!tabMode,tabMode?false:animated)',text);self.assertIn('setClickable(!tabMode)',text);self.assertIn('setEnabled(!tabMode)',text)
 def test_real_tab_anchor_not_old_right_fab(self):
  main=src('ui/MainTabsActivity.java');dialogs=src('ui/DialogsActivity.java')
  self.assertIn('tab.getLocationInWindow(point)',main);self.assertIn('tabsView.getLocationInWindow(bar)',main);self.assertIn('tab.sectionsAnchorCenterX()',main)
  self.assertIn('getSectionsAnchor(this,anchor)',dialogs);self.assertIn('getRootWindowInsets(fragmentView)',dialogs);self.assertIn('SectionOpeningPolicy.left(',dialogs);self.assertIn('SectionOpeningPolicy.height(',dialogs);self.assertIn('tetogramSectionMenu.setTranslationY(0)',dialogs)
 def test_mode_change_is_single_setting_with_account_fenced_atomic_update(self):
  ui=src('ui/tetogram/SectionsActivity.java');store=src('messenger/tetogram/SectionsStore.java')
  self.assertEqual(1,ui.count('R.string.TetogramSectionsOpening),getString('));self.assertIn('sectionId==null && folderId==null',ui)
  for text in ('synchronized(LOCK)','owner!=UserConfig.getInstance(a).getClientUserId()','SectionConfig c=load(a)','preserveNewerOpeningMode(latest)','NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.tetogramSectionOpeningChanged)'):self.assertIn(text,store)
  for name in ('AppearanceActivity','AppearanceLauncherActivity'):self.assertNotIn('TetogramSectionsOpening',src('ui/tetogram/'+name+'.java'))
 def test_arrow_is_decorative_and_separate_from_counter(self):
  tab=src('ui/Components/glass/GlassTabView.java');self.assertIn('sectionsArrow.setClickable(false)',tab);self.assertIn('sectionsArrow.setFocusable(false)',tab);self.assertIn('sectionsArrow.setColorFilter(filter)',tab);self.assertIn('0,20,0,0',tab);self.assertIn('sectionsArrow.animate().cancel()',tab);self.assertIn('sectionsIndicatorVisible==visible&&sectionsIndicatorOpen==open',tab)
 def test_navigation_and_lifecycle_close_menu(self):
  main=src('ui/MainTabsActivity.java');self.assertIn('if(position!=POSITION_CHATS)tetogramCloseSections()',main);self.assertIn('if(!visible)tetogramCloseSections()',main);self.assertIn('.add(NotificationCenter.tetogramSectionOpeningChanged)',main);self.assertIn('source!=dialogsActivity||source.isFinished||source.isPaused()',main)
if __name__=='__main__':unittest.main(verbosity=2)
