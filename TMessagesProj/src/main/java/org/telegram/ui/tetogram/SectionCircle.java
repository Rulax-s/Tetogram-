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
 private final FrameLayout face;private final TextView emoji;private final ImageView standard;private final BackupImageView image;private final CounterView badge;private final Theme.ResourcesProvider colors;private String signature="";private int oldBackground,oldStroke,oldText,oldWidth;private AppearanceConfig.Resolved appearance=new AppearanceConfig.Resolved();private AppearanceConfig.Resolved previewAppearance;private boolean previewDark;private Theme.ResourcesProvider ownColors;private boolean styled;
 public static final String[] STANDARD={"folder","star","work","group","chat","archive"};
 public static int drawable(String key){switch(key){case "star":return R.drawable.msg_fave;case "work":return R.drawable.msg_work;case "group":return R.drawable.outline_groups_24;case "chat":return R.drawable.settings_chat;case "archive":return R.drawable.msg_archive;default:return R.drawable.msg_folders;}}
 public SectionCircle(Context c,Theme.ResourcesProvider p){super(c);colors=p;setClipChildren(false);setClipToPadding(false);face=new FrameLayout(c);face.setClipToOutline(true);addView(face,LayoutHelper.createFrame(48,48,Gravity.CENTER));
  emoji=new TextView(c);emoji.setGravity(Gravity.CENTER);emoji.setTextSize(23);face.addView(emoji,LayoutHelper.createFrame(-1,-1));standard=new ImageView(c);standard.setScaleType(ImageView.ScaleType.CENTER);face.addView(standard,LayoutHelper.createFrame(-1,-1));image=new BackupImageView(c);image.setRoundRadius(AndroidUtilities.dp(22));face.addView(image,LayoutHelper.createFrame(44,44,Gravity.CENTER));
  badge=new CounterView(c,new Theme.ResourcesProvider(){@Override public int getColor(int key){String name=key==Theme.key_chats_unreadCounterText?"section.badgeText":"section.badge";return custom(name,Theme.getColor(key,ownColors==null?colors:ownColors));}});badge.setColors(Theme.key_chats_unreadCounterText,Theme.key_chats_unreadCounter);badge.setGravity(Gravity.CENTER);addView(badge,LayoutHelper.createFrame(40,22,Gravity.TOP|Gravity.RIGHT,-4,0,-4,0));
 }
 private int custom(String key,int fallback){Integer color=appearance.color(previewAppearance==null?Theme.isCurrentThemeDark():previewDark,key);return color==null?fallback:color;}
 public void preview(AppearanceConfig.Resolved style,boolean dark){previewAppearance=style;previewDark=dark;}
 public void bind(long owner,SectionConfig.Section section,boolean selected){
  appearance=previewAppearance==null?AppearanceStore.resolveOwner(owner,new AppearanceConfig.Context(section.id,null,0,0)):previewAppearance;
  ownColors=new AppearanceResources(appearance,previewAppearance==null?Theme.isCurrentThemeDark():previewDark,null);
  setContentDescription(section.name);int text=Theme.getColor(selected?Theme.key_chats_actionIcon:Theme.key_dialogTextBlack,ownColors);int background=Theme.getColor(selected?Theme.key_chats_actionBackground:Theme.key_dialogBackground,ownColors),stroke=Theme.getColor(Theme.key_chats_actionBackground,ownColors);text=custom("section.text",text);background=custom(selected?"section.activeBackground":"section.background",background);stroke=custom(selected?"section.activeOutline":"section.outline",stroke);int width=AndroidUtilities.dp(Math.max(0,Math.min(6,appearance.number("section.outlineWidth",2))));badge.invalidate();if(!styled || oldBackground!=background || oldStroke!=stroke || oldText!=text || oldWidth!=width){GradientDrawable bg=new GradientDrawable();bg.setShape(GradientDrawable.OVAL);bg.setColor(background);bg.setStroke(width,stroke);face.setBackground(bg);emoji.setTextColor(text);standard.setColorFilter(text,PorterDuff.Mode.SRC_IN);oldWidth=width;oldBackground=background;oldStroke=stroke;oldText=text;styled=true;}setSelected(selected);
  String key=owner+":"+section.id+":"+section.iconType+":"+section.icon+":"+section.iconImage+":"+text;if(signature.equals(key))return;signature=key;emoji.setVisibility(GONE);standard.setVisibility(GONE);image.setVisibility(GONE);
  if("image".equals(section.iconType)){
   File file=SectionIcons.resolve(owner,section.id,section.iconImage);if(file!=null){Drawable fallback=getContext().getResources().getDrawable(R.drawable.msg_folders).mutate();fallback.setColorFilter(text,PorterDuff.Mode.SRC_IN);image.setImage(file.getAbsolutePath(),"48_48",fallback);image.setVisibility(VISIBLE);return;}
  }
  if("emoji".equals(section.iconType)){emoji.setText(Emoji.replaceEmoji(section.icon,emoji.getPaint().getFontMetricsInt(),false));emoji.setVisibility(VISIBLE);}else{standard.setImageResource(drawable(section.icon));standard.setVisibility(VISIBLE);}
 }
 public void count(long count,boolean show,boolean compact){if(!show || count<=0){badge.setCount(0,false);badge.setVisibility(GONE);return;}badge.counterDrawable.setText(SectionUnreadPolicy.label(count,compact),false,(int)Math.min(Integer.MAX_VALUE,count),true);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)badge.getLayoutParams();int width=Math.max(AndroidUtilities.dp(24),badge.counterDrawable.getCurrentWidth()+AndroidUtilities.dp(4));if(lp.width!=width){lp.width=width;badge.setLayoutParams(lp);}badge.setVisibility(VISIBLE);}
}
