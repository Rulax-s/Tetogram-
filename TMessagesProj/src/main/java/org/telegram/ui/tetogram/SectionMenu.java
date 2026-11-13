/* Tetogram, 2026. GNU GPL v2 or later. Scrollable vertical circular menu, never a drawer. */
package org.telegram.ui.tetogram;
import android.content.Context;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.SectionConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.*;
public final class SectionMenu extends ScrollView {
 private final LinearLayout column;
 private final Theme.ResourcesProvider colors;
 public SectionMenu(Context context,Theme.ResourcesProvider provider){super(context);colors=provider;setFillViewport(true);setVerticalScrollBarEnabled(false);setClipToPadding(false);setPadding(AndroidUtilities.dp(4),0,AndroidUtilities.dp(4),0);column=new LinearLayout(context);column.setOrientation(LinearLayout.VERTICAL);column.setGravity(Gravity.BOTTOM);addView(column,new ScrollView.LayoutParams(-1,-2));setVisibility(GONE);}
 public void populate(SectionConfig config,String selected,java.util.function.Consumer<String> choose){
  column.removeAllViews();
  for(int i=config.sections.size()-1;i>=0;i--){SectionConfig.Section section=config.sections.get(i);TextView button=new TextView(getContext());button.setGravity(Gravity.CENTER);button.setTextSize(23);button.setText(Emoji.replaceEmoji(section.icon,button.getPaint().getFontMetricsInt(),false));button.setContentDescription(section.name);if(android.os.Build.VERSION.SDK_INT>=26)button.setTooltipText(section.name);
   GradientDrawable bg=new GradientDrawable();bg.setShape(GradientDrawable.OVAL);bg.setColor(Theme.getColor(section.id.equals(selected)?Theme.key_chats_actionBackground:Theme.key_dialogBackground,colors));bg.setStroke(AndroidUtilities.dp(2),Theme.getColor(Theme.key_chats_actionBackground,colors));button.setBackground(bg);button.setTextColor(Theme.getColor(section.id.equals(selected)?Theme.key_chats_actionIcon:Theme.key_dialogTextBlack,colors));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(AndroidUtilities.dp(48),AndroidUtilities.dp(48));lp.gravity=Gravity.CENTER_HORIZONTAL;lp.topMargin=AndroidUtilities.dp(8);column.addView(button,lp);button.setOnClickListener(v->{collapse();choose.accept(section.id);});button.setOnLongClickListener(v->{Toast.makeText(getContext(),section.name,Toast.LENGTH_SHORT).show();return true;});
  }
 }
 public void expand(){animate().cancel();setVisibility(VISIBLE);setAlpha(0);setTranslationX(0);animate().alpha(1).setDuration(180).setInterpolator(CubicBezierInterpolator.DEFAULT).start();post(()->fullScroll(FOCUS_DOWN));}
 public void collapse(){animate().cancel();setVisibility(GONE);setAlpha(1);}
 public boolean expanded(){return getVisibility()==VISIBLE;}
}
