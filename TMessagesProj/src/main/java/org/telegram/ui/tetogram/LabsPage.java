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
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.messenger.R;
import java.util.ArrayList;

/** A settings namespace, not a Gradle module or a replacement for native navigation. */
public abstract class LabsPage extends BaseFragment {
    protected UniversalRecyclerView list;
    private View bottomActions,topContent;
    private int topContentHeight;
    protected void setTopContent(View content,int heightDp){
        int height=org.telegram.messenger.AndroidUtilities.dp(heightDp);
        if(topContent==content&&topContentHeight==height)return;
        if(topContent!=null)org.telegram.messenger.AndroidUtilities.removeFromParent(topContent);
        topContent=content;topContentHeight=content==null?0:height;
        if(content!=null){org.telegram.messenger.AndroidUtilities.removeFromParent(content);((FrameLayout)fragmentView).addView(content,new FrameLayout.LayoutParams(-1,height,android.view.Gravity.TOP));}
        updateBottomActionsInsets();
    }
    /** Shared native-insets dock. Screens supply actions, never their own status-bar compensation. */
    protected void setBottomActions(View actions){
        if(bottomActions!=null)org.telegram.messenger.AndroidUtilities.removeFromParent(bottomActions);
        bottomActions=actions;if(actions==null)return;
        ((FrameLayout)fragmentView).addView(actions,new FrameLayout.LayoutParams(-1,-2,android.view.Gravity.BOTTOM));
        actions.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot)updateBottomActionsInsets();});updateBottomActionsInsets();
    }
    private void updateBottomActionsInsets(){
        if(list==null)return;
        if(topContent!=null){FrameLayout.LayoutParams top=(FrameLayout.LayoutParams)topContent.getLayoutParams();int y=mSystemInsets.top+ActionBar.getCurrentActionBarHeight();if(top.topMargin!=y||top.leftMargin!=mSystemInsets.left||top.rightMargin!=mSystemInsets.right){top.topMargin=y;top.leftMargin=mSystemInsets.left;top.rightMargin=mSystemInsets.right;topContent.setLayoutParams(top);}}
        if(bottomActions!=null){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)bottomActions.getLayoutParams();
        if(lp.bottomMargin!=mSystemInsets.bottom||lp.leftMargin!=mSystemInsets.left||lp.rightMargin!=mSystemInsets.right){lp.bottomMargin=mSystemInsets.bottom;lp.leftMargin=mSystemInsets.left;lp.rightMargin=mSystemInsets.right;bottomActions.setLayoutParams(lp);}}
        org.telegram.utils.glass.BaseFragmentSimpleGlass.setListViewParams(list,mSystemInsets.top,mSystemInsets.bottom,ActionBar.getCurrentActionBarHeight()+topContentHeight,bottomActions==null?0:bottomActions.getMeasuredHeight());
    }
    @Override public void onInsets(int left,int top,int right,int bottom){super.onInsets(left,top,right,bottom);updateBottomActionsInsets();}
    protected abstract CharSequence title();
    protected abstract void fill(ArrayList<UItem> items, UniversalAdapter adapter);
    protected abstract void click(UItem item, View view, int position, float x, float y);
    protected LabsPage(int account) { setCurrentAccount(account); }
    @Override protected boolean useSimpleActionBar() { return true; }
    @Override public boolean isSupportEdgeToEdge() { return true; }
    // BaseFragment queries this after createView, then owns header placement and
    // reapplies native top/bottom list insets on subsequent WindowInsets updates.
    @Override protected RecyclerListView getListViewForSimpleGlass() { return list; }
    @Override public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setTitle(title());
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if(id==-1) finishFragment(); }
        });
        bottomActions=null;topContent=null;topContentHeight=0;
        FrameLayout frame=new FrameLayout(context);
        fragmentView=frame;
        ThemeBind.backgroundColor(frame,Theme.key_windowBackgroundGray);
        list=new UniversalRecyclerView(this,this::fill,this::click,null);
        list.setSectionsDrawBackground(true);
        frame.addView(list,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
        if(topContent!=null)topContent.bringToFront();updateBottomActionsInsets();
        return frame;
    }
    @Override public void onResume() { super.onResume(); if(list!=null) list.adapter.update(false); }
    @Override public void onFragmentDestroy() { super.onFragmentDestroy(); list=null; }
}
