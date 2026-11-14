#!/usr/bin/env python3
"""Source integration contracts, NOT Android layout/gesture tests."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main/java/org/telegram'


def source(path):
    return (ROOT / path).read_text()


class SettingsInsetsContracts(unittest.TestCase):
    def test_common_page_registers_actual_list_and_enables_insets(self):
        page = source('ui/tetogram/LabsPage.java')
        self.assertRegex(page, r'protected RecyclerListView getListViewForSimpleGlass\(\)\s*\{\s*return list;\s*\}')
        self.assertRegex(page, r'public boolean isSupportEdgeToEdge\(\)\s*\{\s*return true;\s*\}')
        self.assertIn('protected boolean useSimpleActionBar() { return true; }', page)
        self.assertIn('fragmentView=frame;', page)
        self.assertIn('list=new UniversalRecyclerView', page)
        self.assertIn('frame.addView(list,', page)

    def test_native_lifecycle_queries_only_after_list_creation(self):
        base = source('ui/ActionBar/BaseFragment.java')
        start = base.index('private View performCreateViewImpl(')
        finish = base.index('onViewCreated(view);', start)
        create = base[start:finish]
        self.assertLess(create.index('createView(context)'), create.index('getListViewForSimpleGlass()'))
        self.assertIn('simpleGlass.setViews((FrameLayout) view, listViewForGlass, header, resourceProvider)', create)
        self.assertIn('mSystemInsets.top, mSystemInsets.bottom, ActionBar.getCurrentActionBarHeight()', create)

    def test_native_insets_are_reapplied_by_navigation_layout(self):
        base = source('ui/ActionBar/BaseFragment.java')
        on_insets = base.split('public WindowInsetsCompat onInsetsInternal(', 1)[1].split('private int bottomInset;', 1)[0]
        self.assertIn('AndroidUtilities.getDefaultWindowInsets(windowInsets, false)', on_insets)
        self.assertIn('BaseFragmentSimpleGlass.setListViewParams(', on_insets)
        self.assertIn('mSystemInsets.top, mSystemInsets.bottom', on_insets)
        navigation = source('ui/ActionBar/ActionBarLayout.java')
        self.assertRegex(navigation, r'fragment\.isSupportEdgeToEdge\(\) && fragment\.drawEdgeNavigationBar\(\)[\s\S]{0,150}fragment::onInsetsInternal')

    def test_all_custom_settings_inherit_shared_contract(self):
        for name in ('LabsActivity', 'SectionsActivity', 'HistoryActivity', 'ExcludedChatsActivity'):
            with self.subTest(screen=name):
                screen = source('ui/tetogram/' + name + '.java')
                self.assertIn('extends LabsPage', screen)
                self.assertNotRegex(screen, r'(?:boolean isSupportEdgeToEdge|RecyclerListView getListViewForSimpleGlass|void onInsets)\(')
                self.assertNotRegex(screen, r'list\.setPadding\(')
        sections = source('ui/tetogram/SectionsActivity.java')
        self.assertIn('new SectionsActivity(currentAccount,s.id)', sections)
        self.assertIn('new SectionsActivity(currentAccount,null,f.id)', sections)

    def test_history_search_is_a_list_row_not_an_absolute_overlay(self):
        history = source('ui/tetogram/HistoryActivity.java')
        self.assertIn('UItem.asCustom(searchInput,48)', history)
        self.assertNotIn('fragmentView).addView(input', history)
        self.assertLess(history.index('searchInput=input;'), history.index('return super.createView(context);'))
        self.assertLess(history.index('input.setText(query);'), history.index('input.addTextChangedListener('))

    def test_no_manual_system_padding_in_common_page(self):
        page = source('ui/tetogram/LabsPage.java')
        self.assertNotRegex(page, r'(?:setPadding|setMargins|onInsets|setDecorFitsSystemWindows)\(')
        self.assertNotIn('statusBarHeight', page)
        self.assertIn('extends RecyclerListView', source('ui/Components/UniversalRecyclerView.java'))


if __name__ == '__main__':
    unittest.main(verbosity=2)
