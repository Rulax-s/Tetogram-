/* Tetogram, 2026. GNU GPL v2 or later. Shared native-style circular menu/settings preview. */
package org.telegram.ui.tetogram;
import android.content.Context;
import android.view.*;
import android.widget.*;
import android.graphics.PorterDuff;
import android.graphics.drawable.*;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.*;
import java.io.File;
public final class SectionCircle extends FrameLayout {
 private final FrameLayout face;private final TextView emoji;private final ImageView standard;private final BackupImageView image;private final CounterView badge;private final Theme.ResourcesProvider colors;private String signature="";private int oldBackground,oldStroke,oldText;private boolean styled;
 public static final String[] STANDARD={"folder","star","work","group","chat","archive"};
 public static int drawable(String key){switch(key){case "star":return R.drawable.msg_fave;case "work":return R.drawable.msg_work;case "group":return R.drawable.outline_groups_24;case "chat":return R.drawable.settings_chat;case "archive":return R.drawable.msg_archive;default:return R.drawable.msg_folders;}}
 public SectionCircle(Context c,Theme.ResourcesProvider p){super(c);colors=p;setClipChildren(false);setClipToPadding(false);face=new FrameLayout(c);face.setClipToOutline(true);addView(face,LayoutHelper.createFrame(48,48,Gravity.CENTER));
  emoji=new TextView(c);emoji.setGravity(Gravity.CENTER);emoji.setTextSize(23);face.addView(emoji,LayoutHelper.createFrame(-1,-1));standard=new ImageView(c);standard.setScaleType(ImageView.ScaleType.CENTER);face.addView(standard,LayoutHelper.createFrame(-1,-1));image=new BackupImageView(c);image.setRoundRadius(AndroidUtilities.dp(22));face.addView(image,LayoutHelper.createFrame(44,44,Gravity.CENTER));
  badge=new CounterView(c,p);badge.setColors(Theme.key_chats_unreadCounterText,Theme.key_chats_unreadCounter);badge.setGravity(Gravity.CENTER);addView(badge,LayoutHelper.createFrame(40,22,Gravity.TOP|Gravity.RIGHT,-4,0,-4,0));
 }
 public void bind(long owner,SectionConfig.Section section,boolean selected){
  setContentDescription(section.name);int text=Theme.getColor(selected?Theme.key_chats_actionIcon:Theme.key_dialogTextBlack,colors);int background=Theme.getColor(selected?Theme.key_chats_actionBackground:Theme.key_dialogBackground,colors),stroke=Theme.getColor(Theme.key_chats_actionBackground,colors);if(!styled || oldBackground!=background || oldStroke!=stroke || oldText!=text){GradientDrawable bg=new GradientDrawable();bg.setShape(GradientDrawable.OVAL);bg.setColor(background);bg.setStroke(AndroidUtilities.dp(2),stroke);face.setBackground(bg);emoji.setTextColor(text);standard.setColorFilter(text,PorterDuff.Mode.SRC_IN);oldBackground=background;oldStroke=stroke;oldText=text;styled=true;}setSelected(selected);
  String key=owner+":"+section.id+":"+section.iconType+":"+section.icon+":"+section.iconImage+":"+text;if(signature.equals(key))return;signature=key;emoji.setVisibility(GONE);standard.setVisibility(GONE);image.setVisibility(GONE);
  if("image".equals(section.iconType)){
   File file=SectionIcons.resolve(owner,section.id,section.iconImage);if(file!=null){Drawable fallback=getContext().getResources().getDrawable(R.drawable.msg_folders).mutate();fallback.setColorFilter(text,PorterDuff.Mode.SRC_IN);image.setImage(file.getAbsolutePath(),"48_48",fallback);image.setVisibility(VISIBLE);return;}
  }
  if("emoji".equals(section.iconType)){emoji.setText(Emoji.replaceEmoji(section.icon,emoji.getPaint().getFontMetricsInt(),false));emoji.setVisibility(VISIBLE);}else{standard.setImageResource(drawable(section.icon));standard.setVisibility(VISIBLE);}
 }
 public void count(long count,boolean show,boolean compact){if(!show || count<=0){badge.setCount(0,false);badge.setVisibility(GONE);return;}badge.counterDrawable.setText(SectionUnreadPolicy.label(count,compact),false,(int)Math.min(Integer.MAX_VALUE,count),true);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)badge.getLayoutParams();int width=Math.max(AndroidUtilities.dp(24),badge.counterDrawable.getCurrentWidth()+AndroidUtilities.dp(4));if(lp.width!=width){lp.width=width;badge.setLayoutParams(lp);}badge.setVisibility(VISIBLE);}
}
