/* Tetogram, 2026. GNU GPL v2 or later. Building blocks of the appearance studio layout. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

final class AppearanceStudioViews {
    private AppearanceStudioViews(){}

    static int accent(){return Theme.getColor(Theme.key_featuredStickers_addButton);}

    /** A pill used for tabs and segments; selected pills are filled with the accent. */
    static TextView pill(Context context,CharSequence text,boolean selected,boolean compact){
        TextView view=new TextView(context);
        view.setText(text);view.setSingleLine(true);view.setGravity(Gravity.CENTER);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP,compact?13:14);
        view.setTypeface(selected?AndroidUtilities.bold():null);
        view.setPadding(dp(compact?12:14),0,dp(compact?12:14),0);
        view.setMinHeight(dp(36));view.setMinimumHeight(dp(36));
        int a=accent();
        view.setTextColor(selected?(ColorUtils.calculateLuminance(a)>.5?0xff000000:0xffffffff):Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        view.setBackground(Theme.createRoundRectDrawable(dp(18),selected?a:ColorUtils.setAlphaComponent(a,28)));
        view.setSelected(selected);
        view.setContentDescription(text);
        return view;
    }

    /** Equal-width segments: {@code labels[i]} → {@code actions[i]}. */
    static LinearLayout segments(Context context,CharSequence[] labels,int selected,Runnable[] actions){
        LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<labels.length;i++){
            TextView pill=pill(context,labels[i],i==selected,true);final Runnable action=actions[i];pill.setOnClickListener(v->action.run());
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(36),1);if(i>0)lp.leftMargin=dp(6);row.addView(pill,lp);
        }
        return row;
    }

    /** Horizontally scrolling pills that keep the selected one in view. */
    static HorizontalScrollView tabs(Context context,CharSequence[] labels,int selected,Runnable[] actions){
        HorizontalScrollView scroll=new HorizontalScrollView(context);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(8),0,dp(8),0);
        for(int i=0;i<labels.length;i++){
            TextView pill=pill(context,labels[i],i==selected,false);final Runnable action=actions[i];pill.setOnClickListener(v->action.run());
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(36));if(i>0)lp.leftMargin=dp(6);row.addView(pill,lp);
        }
        scroll.addView(row);
        if(selected>=0)scroll.post(()->{View child=row.getChildAt(selected);if(child!=null)scroll.smoothScrollTo(Math.max(0,child.getLeft()-dp(24)),0);});
        return scroll;
    }

    static final class ThemeCard{
        final String title;final int wallTop,wallBottom,incoming,outgoing;final boolean selected;final Runnable action;
        ThemeCard(String title,int wallTop,int wallBottom,int incoming,int outgoing,boolean selected,Runnable action){this.title=title;this.wallTop=wallTop;this.wallBottom=wallBottom;this.incoming=incoming;this.outgoing=outgoing;this.selected=selected;this.action=action;}
    }

    /** Mini chat thumbnails of themes; tapping one previews it immediately. */
    static HorizontalScrollView themes(Context context,ArrayList<ThemeCard> cards){
        HorizontalScrollView scroll=new HorizontalScrollView(context);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(14),dp(4),dp(14),dp(8));
        int selectedIndex=-1;
        for(int i=0;i<cards.size();i++){
            ThemeCard card=cards.get(i);if(card.selected)selectedIndex=i;
            CardView view=new CardView(context,card);view.setOnClickListener(v->card.action.run());
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(78),dp(118));if(i>0)lp.leftMargin=dp(10);row.addView(view,lp);
        }
        scroll.addView(row);
        final int target=selectedIndex;
        if(target>0)scroll.post(()->{View child=row.getChildAt(target);if(child!=null)scroll.scrollTo(Math.max(0,child.getLeft()-dp(24)),0);});
        return scroll;
    }

    private static final class CardView extends View {
        private final ThemeCard card;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG),ring=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint label=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect=new RectF();
        private LinearGradient shader;private int shaderWidth=-1;
        CardView(Context context,ThemeCard card){
            super(context);this.card=card;
            label.setTextSize(dp(11.5f));label.setTextAlign(Paint.Align.CENTER);
            ring.setStyle(Paint.Style.STROKE);ring.setStrokeWidth(dp(2.5f));
            setContentDescription(card.title);setSelected(card.selected);
            setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));
        }
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),cardH=dp(92),pad=dp(2);
            rect.set(pad,pad,w-pad,cardH-pad);
            if(shader==null||shaderWidth!=getWidth()){shader=new LinearGradient(0,0,w,cardH,card.wallTop,card.wallBottom,Shader.TileMode.CLAMP);shaderWidth=getWidth();}
            paint.setShader(shader);
            c.drawRoundRect(rect,dp(14),dp(14),paint);paint.setShader(null);
            if(card.selected){ring.setColor(accent());c.drawRoundRect(rect,dp(14),dp(14),ring);}
            else{ring.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText),60));ring.setStrokeWidth(dp(1));c.drawRoundRect(rect,dp(14),dp(14),ring);ring.setStrokeWidth(dp(2.5f));}
            paint.setColor(card.incoming);rect.set(dp(10),cardH-dp(44),dp(10)+w*0.5f,cardH-dp(32));c.drawRoundRect(rect,dp(6),dp(6),paint);
            paint.setColor(card.outgoing);rect.set(w-dp(10)-w*0.55f,cardH-dp(26),w-dp(10),cardH-dp(14));c.drawRoundRect(rect,dp(6),dp(6),paint);
            label.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));label.setTypeface(card.selected?AndroidUtilities.bold():null);
            c.drawText(TextUtils.ellipsize(card.title,label,w-dp(4),TextUtils.TruncateAt.END).toString(),w/2,cardH+dp(18),label);
        }
    }
}
