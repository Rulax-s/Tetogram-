/* Tetogram, 2026. GNU GPL v2 or later. Native property animations, circular editor links and badges. */
package org.telegram.ui.tetogram;
import android.animation.*;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.*;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.*;
public final class SectionMenu extends ScrollView {
 private final LinearLayout column;private final Theme.ResourcesProvider colors;private final LinkedHashMap<String,SectionCircle> views=new LinkedHashMap<>();
 private final SectionMenuState state=new SectionMenuState();private AnimatorSet animation;private Runnable afterClose;private BiConsumer<Boolean,Boolean> listener;private long accountOwner;private ViewTreeObserver.OnPreDrawListener layoutStart;
 public SectionMenu(Context context,Theme.ResourcesProvider p){super(context);colors=p;setFillViewport(true);setClipChildren(false);setClipToPadding(false);setVerticalScrollBarEnabled(false);setPadding(AndroidUtilities.dp(4),0,AndroidUtilities.dp(4),0);column=new LinearLayout(context);column.setOrientation(LinearLayout.VERTICAL);column.setGravity(Gravity.BOTTOM);column.setClipChildren(false);addView(column,new ScrollView.LayoutParams(-1,-2));setVisibility(GONE);}
 public void setStateListener(BiConsumer<Boolean,Boolean> value){listener=value;}
 public static boolean animations(Context c){try{return Build.VERSION.SDK_INT>=26?ValueAnimator.areAnimatorsEnabled():Settings.Global.getFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)>0;}catch(Exception e){return true;}}
 public void populate(long owner,SectionConfig config,String selected,SectionUnread.Snapshot snapshot,Consumer<String> choose,Consumer<String> edit){
  if(accountOwner!=0 && accountOwner!=owner)cancelAndHide();accountOwner=owner;
  ArrayList<SectionConfig.Section> entries=new ArrayList<>();
  if(SectionUnreadPolicy.showSystem(config.unreadEnabled,snapshot.any,SectionUnreadPolicy.SYSTEM_ID.equals(selected))){SectionConfig.Section sys=new SectionConfig.Section();sys.id=SectionUnreadPolicy.SYSTEM_ID;sys.name=LocaleController.getString(R.string.TetogramSystemUnread);sys.icon="📩";entries.add(sys);}
  for(int i=config.sections.size()-1;i>=0;i--)entries.add(config.sections.get(i));
  HashSet<String> wanted=new HashSet<>();int position=0;
  for(SectionConfig.Section s:entries){wanted.add(s.id);SectionCircle view=views.get(s.id);boolean added=view==null;
   if(added){view=new SectionCircle(getContext(),colors);views.put(s.id,view);}
   if(column.indexOfChild(view)!=position){if(view.getParent()!=null)column.removeView(view);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(AndroidUtilities.dp(56),AndroidUtilities.dp(56));lp.gravity=Gravity.CENTER_HORIZONTAL;column.addView(view,position,lp);}position++;
   view.bind(owner,s,s.id.equals(selected));view.count(snapshot.counts.getOrDefault(s.id,0L),config.showBadges,config.compactBadges);view.setEnabled(state.phase==SectionMenuState.Phase.OPEN);
   final String id=s.id;view.setOnClickListener(v->collapse(()->choose.accept(id)));view.setHapticFeedbackEnabled(false);view.setOnLongClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);collapse(()->edit.accept(id));return true;});
   if(added && state.targetOpen()){view.setAlpha(1);view.setScaleX(1);view.setScaleY(1);view.setTranslationY(0);}
  }
  for(String id:new ArrayList<>(views.keySet()))if(!wanted.contains(id)){SectionCircle v=views.remove(id);v.animate().cancel();v.setEnabled(false);column.removeView(v);}
 }
 public int entries(){return views.size();}
 public void expand(){if(state.targetOpen())return;afterClose=null;boolean wasGone=getVisibility()!=VISIBLE;long token=state.request(true);stopAnimation();setVisibility(VISIBLE);for(SectionCircle v:views.values()){v.setEnabled(false);if(wasGone){v.setAlpha(0);v.setScaleX(.8f);v.setScaleY(.8f);v.setTranslationY(AndroidUtilities.dp(14));}}boolean animate=animations(getContext());if(listener!=null)listener.accept(true,animate);
  layoutStart=()->{if(getViewTreeObserver().isAlive())getViewTreeObserver().removeOnPreDrawListener(layoutStart);layoutStart=null;if(state.current(token)){scrollTo(0,Math.max(0,column.getHeight()-getHeight()));runAnimation(token,true,animate);}return true;};getViewTreeObserver().addOnPreDrawListener(layoutStart);requestLayout();
 }

 public void dismiss(){afterClose=null;collapse();}
 public void collapse(){collapse(null);}
 public void collapse(Runnable done){
  if(state.phase==SectionMenuState.Phase.CLOSED){if(done!=null)done.run();return;}
  if(state.phase==SectionMenuState.Phase.CLOSING){if(done!=null)afterClose=done;return;}
  afterClose=done;long token=state.request(false);stopAnimation();for(SectionCircle v:views.values())v.setEnabled(false);boolean animate=animations(getContext());if(listener!=null)listener.accept(false,animate);runAnimation(token,false,animate);
 }
 private void runAnimation(long token,boolean opening,boolean animate){
  if(!state.current(token))return;
  ArrayList<View> visible=new ArrayList<>();for(int i=0;i<column.getChildCount();i++){View v=column.getChildAt(i);if(v.getBottom()>getScrollY() && v.getTop()<getScrollY()+getHeight())visible.add(v);else reset(v,opening);}
  if(!animate || visible.isEmpty()){for(SectionCircle v:views.values())reset(v,opening);finish(token,opening);return;}
  ArrayList<Animator> list=new ArrayList<>();for(int i=0;i<visible.size();i++){View v=visible.get(i);AnimatorSet item=new AnimatorSet();item.playTogether(ObjectAnimator.ofFloat(v,View.ALPHA,opening?1:0),ObjectAnimator.ofFloat(v,View.SCALE_X,opening?1:.8f),ObjectAnimator.ofFloat(v,View.SCALE_Y,opening?1:.8f),ObjectAnimator.ofFloat(v,View.TRANSLATION_Y,opening?0:AndroidUtilities.dp(14)));item.setStartDelay(SectionMenuState.delay(opening?visible.size()-1-i:i,visible.size()));item.setDuration(210);item.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);list.add(item);}
  AnimatorSet set=new AnimatorSet();animation=set;set.playTogether(list);set.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(state.current(token)){animation=null;finish(token,opening);}}});set.start();
 }
 private void finish(long token,boolean opening){if(!state.finish(token))return;for(SectionCircle v:views.values()){reset(v,opening);v.setEnabled(opening);}if(!opening){setVisibility(GONE);Runnable action=afterClose;afterClose=null;if(action!=null)action.run();}}
 private void reset(View v,boolean open){v.setAlpha(open?1:0);v.setScaleX(open?1:.8f);v.setScaleY(open?1:.8f);v.setTranslationY(open?0:AndroidUtilities.dp(14));}
 private void stopAnimation(){if(layoutStart!=null){if(getViewTreeObserver().isAlive())getViewTreeObserver().removeOnPreDrawListener(layoutStart);layoutStart=null;}if(animation!=null){AnimatorSet old=animation;animation=null;old.cancel();}}
 public void cancelAndHide(){state.cancel();stopAnimation();afterClose=null;setVisibility(GONE);for(SectionCircle v:views.values()){v.setEnabled(false);reset(v,false);}if(listener!=null)listener.accept(false,false);}
 public boolean expanded(){return state.targetOpen();}
 @Override protected void onDetachedFromWindow(){cancelAndHide();super.onDetachedFromWindow();}
}
