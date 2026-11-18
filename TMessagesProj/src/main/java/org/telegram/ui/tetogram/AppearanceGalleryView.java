/* Tetogram, 2026. GNU GPL v2 or later. Inline grid of the device gallery and of the account's own backgrounds. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

/** Thumbnails use the native gallery thumbnail loader; nothing is copied until a tile is chosen. */
final class AppearanceGalleryView extends LinearLayout {
    static final int COLUMNS=4;
    interface Pick{void picked(MediaController.PhotoEntry entry);}
    static final class Tile{final String thumb,badge,label;final boolean selected;final Runnable action;Tile(String thumb,String badge,String label,boolean selected,Runnable action){this.thumb=thumb;this.badge=badge;this.label=label;this.selected=selected;this.action=action;}}

    AppearanceGalleryView(Context context,ArrayList<Tile> tiles){
        super(context);setOrientation(VERTICAL);setPadding(dp(14),dp(4),dp(14),dp(8));
        LinearLayout row=null;
        for(int i=0;i<tiles.size();i++){
            if(i%COLUMNS==0){row=new LinearLayout(context);row.setOrientation(HORIZONTAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);if(i>0)lp.topMargin=dp(6);addView(row,lp);}
            Tile tile=tiles.get(i);
            FrameLayout cell=new FrameLayout(context){@Override protected void onMeasure(int w,int h){super.onMeasure(w,MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(w),MeasureSpec.EXACTLY));}};
            GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(10));bg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));cell.setBackground(bg);cell.setClipToOutline(true);
            cell.setOutlineProvider(new android.view.ViewOutlineProvider(){@Override public void getOutline(android.view.View v,android.graphics.Outline o){o.setRoundRect(0,0,v.getWidth(),v.getHeight(),dp(10));}});
            if(tile.thumb!=null){BackupImageView image=new BackupImageView(context);image.setImage(tile.thumb,null,Theme.chat_attachEmptyDrawable);image.getImageReceiver().setRoundRadius(dp(10));cell.addView(image,LayoutHelper.createFrame(-1,-1));}
            if(tile.label!=null){TextView label=new TextView(context);label.setText(tile.label);label.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);label.setGravity(Gravity.CENTER);label.setPadding(dp(4),0,dp(4),0);label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));cell.addView(label,LayoutHelper.createFrame(-1,-1));}
            if(tile.badge!=null){TextView badge=new TextView(context);badge.setText(tile.badge);badge.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);badge.setTextColor(0xffffffff);badge.setPadding(dp(5),dp(1),dp(5),dp(1));badge.setBackground(Theme.createRoundRectDrawable(dp(6),0x99000000));cell.addView(badge,LayoutHelper.createFrame(-2,-2,Gravity.LEFT|Gravity.BOTTOM,4,0,0,4));}
            if(tile.selected){android.view.View ring=new android.view.View(context);GradientDrawable stroke=new GradientDrawable();stroke.setCornerRadius(dp(10));stroke.setStroke(dp(3),AppearanceStudioViews.accent());ring.setBackground(stroke);cell.addView(ring,LayoutHelper.createFrame(-1,-1));}
            cell.setForeground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));
            cell.setContentDescription(tile.label!=null?tile.label:tile.badge!=null?tile.badge:null);
            cell.setOnClickListener(v->tile.action.run());
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(i%COLUMNS!=0)lp.leftMargin=dp(6);row.addView(cell,lp);
        }
        while(row!=null&&row.getChildCount()<COLUMNS){android.view.View filler=new android.view.View(context);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,1,1);lp.leftMargin=dp(6);row.addView(filler,lp);}
    }

    static String duration(int seconds){return String.format(java.util.Locale.ROOT,"%d:%02d",Math.max(0,seconds)/60,Math.max(0,seconds)%60);}
    static String thumb(MediaController.PhotoEntry entry){return (entry.isVideo?"vthumb://":"thumb://")+entry.imageId+":"+entry.path;}
}
