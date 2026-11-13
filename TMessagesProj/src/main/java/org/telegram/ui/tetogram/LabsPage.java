/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.ui.tetogram;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.messenger.R;
import java.util.ArrayList;

/** A settings namespace, not a Gradle module or a replacement for native navigation. */
public abstract class LabsPage extends BaseFragment {
    protected UniversalRecyclerView list;
    protected abstract CharSequence title();
    protected abstract void fill(ArrayList<UItem> items, UniversalAdapter adapter);
    protected abstract void click(UItem item, View view, int position, float x, float y);
    protected LabsPage(int account) { setCurrentAccount(account); }
    @Override protected boolean useSimpleActionBar() { return true; }
    @Override public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setTitle(title());
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if(id==-1) finishFragment(); }
        });
        FrameLayout frame=new FrameLayout(context);
        fragmentView=frame;
        ThemeBind.backgroundColor(frame,Theme.key_windowBackgroundGray);
        list=new UniversalRecyclerView(this,this::fill,this::click,null);
        list.setSectionsDrawBackground(true);
        frame.addView(list,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
        return frame;
    }
    @Override public void onResume() { super.onResume(); if(list!=null) list.adapter.update(false); }
    @Override public void onFragmentDestroy() { super.onFragmentDestroy(); list=null; }
}
