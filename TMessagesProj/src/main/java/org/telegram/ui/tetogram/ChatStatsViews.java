/* Tetogram, 2026. GNU GPL v2 or later. Lightweight themed charts for conversation statistics. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import androidx.core.graphics.ColorUtils;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;

final class ChatStatsViews {
    private ChatStatsViews(){}

    static int accent(){return Theme.getColor(Theme.key_featuredStickers_addButton);}
    static int text(){return Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);}
    static int gray(){return Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);}
    static int palette(int i){
        if(i==0)return accent();
        int key=Theme.keys_avatar_background[(i*3+1)%Theme.keys_avatar_background.length];
        return Theme.getColor(key);
    }
    static int shiftHue(int color,float degrees){float[] hsl=new float[3];ColorUtils.colorToHSL(color,hsl);hsl[0]=(hsl[0]+degrees+360)%360;return ColorUtils.HSLToColor(hsl);}

    /** Grows its content once when first shown; respects system animator scale. */
    abstract static class Animated extends View {
        protected float progress=0f;
        private boolean started;
        Animated(Context context){super(context);}
        @Override protected void onAttachedToWindow(){
            super.onAttachedToWindow();
            if(started)return;
            started=true;
            ValueAnimator a=ValueAnimator.ofFloat(0,1);a.setDuration(700);a.setStartDelay(80);a.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            a.addUpdateListener(v->{progress=(float)v.getAnimatedValue();invalidate();});a.start();
        }
    }

    /** Gradient summary card with the main numbers. */
    static final class Hero extends Animated {
        private final String title,subtitle,big,bigLabel;
        private final String[] values,labels;
        private final Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG),bubble=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint titlePaint=new TextPaint(Paint.ANTI_ALIAS_FLAG),subPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG),bigPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG),valuePaint=new TextPaint(Paint.ANTI_ALIAS_FLAG),labelPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect=new RectF();
        private LinearGradient shader;private int shaderAccent,shaderWidth,shaderHeight;
        Hero(Context context,String title,String subtitle,String big,String bigLabel,String[] values,String[] labels){
            super(context);this.title=title;this.subtitle=subtitle;this.big=big;this.bigLabel=bigLabel;this.values=values;this.labels=labels;
            titlePaint.setTypeface(AndroidUtilities.bold());titlePaint.setTextSize(dp(18));titlePaint.setColor(Color.WHITE);
            subPaint.setTextSize(dp(13));subPaint.setColor(0xd8ffffff);
            bigPaint.setTypeface(AndroidUtilities.bold());bigPaint.setTextSize(dp(44));bigPaint.setColor(Color.WHITE);
            valuePaint.setTypeface(AndroidUtilities.bold());valuePaint.setTextSize(dp(18));valuePaint.setColor(Color.WHITE);
            labelPaint.setTextSize(dp(11.5f));labelPaint.setColor(0xccffffff);
            setContentDescription(title+". "+subtitle+". "+big+" "+bigLabel);
        }
        String title(){return title;}String subtitle(){return subtitle;}String big(){return big;}String bigLabel(){return bigLabel;}String[] values(){return values;}String[] labels(){return labels;}
        @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),dp(252));}
        @Override protected void onDraw(Canvas c){
            float pad=dp(14);rect.set(pad,dp(10),getWidth()-pad,getHeight()-dp(10));
            int a=accent();
            if(shader==null||shaderAccent!=a||shaderWidth!=getWidth()||shaderHeight!=getHeight()){shader=new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{shiftHue(a,-25),a,shiftHue(a,45)},null,Shader.TileMode.CLAMP);shaderAccent=a;shaderWidth=getWidth();shaderHeight=getHeight();}
            bg.setShader(shader);
            c.drawRoundRect(rect,dp(22),dp(22),bg);
            c.save();c.clipRect(rect);
            bubble.setColor(0x1affffff);
            c.drawCircle(rect.right-dp(30),rect.top+dp(20),dp(80)*(0.6f+0.4f*progress),bubble);
            c.drawCircle(rect.left+dp(40),rect.bottom+dp(10),dp(60)*(0.6f+0.4f*progress),bubble);
            c.restore();
            float x=rect.left+dp(20),y=rect.top+dp(34);
            c.drawText(TextUtils.ellipsize(title,titlePaint,rect.width()-dp(40),TextUtils.TruncateAt.END).toString(),x,y,titlePaint);
            y+=dp(20);c.drawText(TextUtils.ellipsize(subtitle,subPaint,rect.width()-dp(40),TextUtils.TruncateAt.END).toString(),x,y,subPaint);
            y+=dp(56);
            bigPaint.setAlpha((int)(255*progress));c.drawText(big,x,y+dp(8)*(1-progress),bigPaint);
            float bw=bigPaint.measureText(big);
            labelPaint.setTextSize(dp(14));c.drawText(TextUtils.ellipsize(bigLabel,labelPaint,rect.right-x-bw-dp(30),TextUtils.TruncateAt.END).toString(),x+bw+dp(10),y,labelPaint);labelPaint.setTextSize(dp(11.5f));
            int n=values.length;float col=(rect.width()-dp(40))/Math.max(1,n);y=rect.bottom-dp(44);
            bubble.setColor(0x33ffffff);c.drawRect(x,y-dp(30),rect.right-dp(20),y-dp(30)+1,bubble);
            for(int i=0;i<n;i++){
                float cx=x+col*i;
                c.drawText(TextUtils.ellipsize(values[i],valuePaint,col-dp(6),TextUtils.TruncateAt.END).toString(),cx,y,valuePaint);
                c.drawText(TextUtils.ellipsize(labels[i],labelPaint,col-dp(6),TextUtils.TruncateAt.END).toString(),cx,y+dp(18),labelPaint);
            }
        }
    }

    /** Vertical bars, e.g. hours or months. */
    static final class Bars extends Animated {
        private final int[] values;private final String[] ticks;private final int highlight;
        private final Paint bar=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tick=new TextPaint(Paint.ANTI_ALIAS_FLAG),top=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r=new RectF();
        private final int color;
        Bars(Context context,int[] values,String[] ticks,int highlight,int color){
            super(context);this.values=values;this.ticks=ticks;this.highlight=highlight;this.color=color;
            tick.setTextSize(dp(10.5f));tick.setTextAlign(Paint.Align.CENTER);
            top.setTextSize(dp(11));top.setTypeface(AndroidUtilities.bold());top.setTextAlign(Paint.Align.CENTER);
        }
        @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),dp(170));}
        @Override protected void onDraw(Canvas c){
            int n=values.length;if(n==0)return;
            int max=1;for(int v:values)max=Math.max(max,v);
            float left=dp(20),right=getWidth()-dp(20),bottom=getHeight()-dp(26),height=bottom-dp(26);
            float slot=(right-left)/n,gap=Math.min(dp(4),slot*0.25f),radius=Math.min(dp(4),(slot-gap)/2);
            tick.setColor(gray());
            for(int i=0;i<n;i++){
                float h=Math.max(values[i]>0?dp(3):dp(1.5f),height*values[i]/max*progress);
                r.set(left+slot*i+gap/2,bottom-h,left+slot*(i+1)-gap/2,bottom);
                int col=i==highlight?color:ColorUtils.setAlphaComponent(color,values[i]==0?40:150);
                bar.setColor(col);c.drawRoundRect(r,radius,radius,bar);
                if(ticks!=null&&i<ticks.length&&ticks[i]!=null)c.drawText(ticks[i],r.centerX(),bottom+dp(16),tick);
                if(i==highlight&&values[i]>0){top.setColor(color);top.setAlpha((int)(255*progress));c.drawText(AndroidUtilities.formatWholeNumber(values[i],0),Math.max(left+dp(12),Math.min(right-dp(12),r.centerX())),r.top-dp(6),top);}
            }
        }
    }

    static final class RankRow{final String name,value;final float share;final int color;RankRow(String name,String value,float share,int color){this.name=name;this.value=value;this.share=share;this.color=color;}}

    /** Horizontal ranked bars with a name on top. */
    static final class Ranking extends Animated {
        private ArrayList<RankRow> rows;
        private final Paint track=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint name=new TextPaint(Paint.ANTI_ALIAS_FLAG),value=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r=new RectF();
        Ranking(Context context,ArrayList<RankRow> rows){
            super(context);this.rows=rows;
            name.setTextSize(dp(14.5f));value.setTextSize(dp(13));value.setTextAlign(Paint.Align.RIGHT);value.setTypeface(AndroidUtilities.bold());
            StringBuilder d=new StringBuilder();for(RankRow row:rows)d.append(row.name).append(": ").append(row.value).append(". ");setContentDescription(d);
        }
        void setRows(ArrayList<RankRow> rows){boolean resize=rows.size()!=this.rows.size();this.rows=rows;progress=1f;if(resize)requestLayout();invalidate();}
        @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),dp(12)+rows.size()*dp(50));}
        @Override protected void onDraw(Canvas c){
            float left=dp(20),right=getWidth()-dp(20),y=dp(8);
            name.setColor(text());
            for(RankRow row:rows){
                value.setColor(row.color);
                float vw=value.measureText(row.value);
                c.drawText(row.value,right,y+dp(18),value);
                c.drawText(TextUtils.ellipsize(row.name,name,right-left-vw-dp(12),TextUtils.TruncateAt.END).toString(),left,y+dp(18),name);
                r.set(left,y+dp(28),right,y+dp(36));
                track.setColor(ColorUtils.setAlphaComponent(row.color,36));c.drawRoundRect(r,dp(4),dp(4),track);
                r.right=left+(right-left)*Math.max(0.015f,Math.min(1,row.share))*progress;
                fill.setColor(row.color);c.drawRoundRect(r,dp(4),dp(4),fill);
                y+=dp(50);
            }
        }
    }

    /** Weekday × hour heat map. */
    static final class Heatmap extends Animated {
        private final int[][] heat;private final String[] days;
        private final Paint cell=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint label=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r=new RectF();
        Heatmap(Context context,int[][] heat,String[] days){super(context);this.heat=heat;this.days=days;label.setTextSize(dp(10.5f));}
        @Override protected void onMeasure(int w,int h){
            int width=MeasureSpec.getSize(w);float cellSize=(width-dp(40)-dp(30))/24f;
            setMeasuredDimension(width,(int)(cellSize*7+dp(40)));
        }
        @Override protected void onDraw(Canvas c){
            int max=1;for(int[] row:heat)for(int v:row)max=Math.max(max,v);
            float left=dp(20)+dp(30),size=(getWidth()-dp(40)-dp(30))/24f,top=dp(8),gap=Math.max(1,size*0.12f);
            label.setColor(gray());int a=accent();
            for(int d=0;d<7;d++){
                c.drawText(days[d],dp(20),top+size*d+size*0.72f,label);
                for(int h=0;h<24;h++){
                    float t=heat[d][h]/(float)max;
                    int alpha=heat[d][h]==0?18:(int)(50+205*Math.sqrt(t)*progress);
                    cell.setColor(ColorUtils.setAlphaComponent(heat[d][h]==0?gray():a,Math.min(255,alpha)));
                    r.set(left+size*h+gap/2,top+size*d+gap/2,left+size*(h+1)-gap/2,top+size*(d+1)-gap/2);
                    c.drawRoundRect(r,gap*1.5f,gap*1.5f,cell);
                }
            }
            label.setTextAlign(Paint.Align.CENTER);
            for(int h=0;h<24;h+=6)c.drawText(String.format(java.util.Locale.ROOT,"%02d",h),left+size*h+size/2,top+size*7+dp(16),label);
            label.setTextAlign(Paint.Align.LEFT);
        }
    }

    /** Wrapping pill chips: "word · 12". */
    static final class Chips extends Animated {
        private final ArrayList<String[]> items;private final boolean large;
        private final Paint pill=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint main=new TextPaint(Paint.ANTI_ALIAS_FLAG),count=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r=new RectF();
        private int measuredFor=-1,heightPx;
        Chips(Context context,ArrayList<String[]> items,boolean large){
            super(context);this.items=items;this.large=large;
            main.setTextSize(dp(large?20:14));count.setTextSize(dp(12));count.setTypeface(AndroidUtilities.bold());
            StringBuilder d=new StringBuilder();for(String[] i:items)d.append(i[0]).append(' ').append(i[1]).append(", ");setContentDescription(d);
        }
        private float chipWidth(String[] i){return dp(14)+main.measureText(i[0])+dp(8)+count.measureText(i[1])+dp(14);}
        private float chipHeight(){return dp(large?40:34);}
        @Override protected void onMeasure(int w,int h){
            int width=MeasureSpec.getSize(w);
            if(width!=measuredFor){
                measuredFor=width;float x=dp(20),y=dp(6),max=width-dp(20);
                for(String[] i:items){float cw=Math.min(chipWidth(i),max-dp(20));if(x+cw>max&&x>dp(20)){x=dp(20);y+=chipHeight()+dp(8);}x+=cw+dp(8);}
                heightPx=(int)(y+chipHeight()+dp(10));
            }
            setMeasuredDimension(width,heightPx);
        }
        @Override protected void onDraw(Canvas c){
            float x=dp(20),y=dp(6),max=getWidth()-dp(20),h=chipHeight();int a=accent();
            main.setColor(text());count.setColor(a);
            int index=0;
            for(String[] i:items){
                float cw=Math.min(chipWidth(i),max-dp(20));
                if(x+cw>max&&x>dp(20)){x=dp(20);y+=h+dp(8);}
                float p=Math.max(0,Math.min(1,progress*1.6f-index*0.04f));
                pill.setColor(ColorUtils.setAlphaComponent(a,(int)(28*p+4)));
                r.set(x,y,x+cw,y+h);c.drawRoundRect(r,h/2,h/2,pill);
                main.setAlpha((int)(255*p));count.setAlpha((int)(255*p));
                float base=y+h/2+main.getTextSize()*0.36f;
                String label=TextUtils.ellipsize(i[0],main,cw-dp(36)-count.measureText(i[1]),TextUtils.TruncateAt.END).toString();
                c.drawText(label,x+dp(14),base,main);
                c.drawText(i[1],x+dp(14)+main.measureText(label)+dp(8),y+h/2+count.getTextSize()*0.36f,count);
                x+=cw+dp(8);index++;
            }
        }
    }

    /** Grid of small metric tiles: icon, value, caption. */
    static final class Tiles extends Animated {
        private final ArrayList<String[]> tiles;
        private final Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint icon=new TextPaint(Paint.ANTI_ALIAS_FLAG),value=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint caption=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r=new RectF();
        private static final int COLUMNS=2;
        Tiles(Context context,ArrayList<String[]> tiles){
            super(context);this.tiles=tiles;
            icon.setTextSize(dp(22));value.setTextSize(dp(19));value.setTypeface(AndroidUtilities.bold());caption.setTextSize(dp(12.5f));
            StringBuilder d=new StringBuilder();for(String[] t:tiles)d.append(t[2]).append(": ").append(t[1]).append(". ");setContentDescription(d);
        }
        private int rows(){return (tiles.size()+COLUMNS-1)/COLUMNS;}
        @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),dp(8)+rows()*dp(96));}
        @Override protected void onDraw(Canvas c){
            float left=dp(16),gap=dp(10),cw=(getWidth()-left*2-gap*(COLUMNS-1))/COLUMNS;int a=accent();
            value.setColor(text());caption.setColor(gray());
            for(int i=0;i<tiles.size();i++){
                String[] t=tiles.get(i);int col=i%COLUMNS,row=i/COLUMNS;
                float x=left+col*(cw+gap),y=dp(6)+row*dp(96);
                r.set(x,y,x+cw,y+dp(86));
                bg.setColor(ColorUtils.setAlphaComponent(a,22));c.drawRoundRect(r,dp(16),dp(16),bg);
                float p=Math.max(0,Math.min(1,progress*1.5f-i*0.06f));
                icon.setAlpha((int)(255*p));value.setAlpha((int)(255*p));caption.setAlpha((int)(255*p));
                c.drawText(t[0],x+dp(14),y+dp(32),icon);
                c.drawText(TextUtils.ellipsize(t[1],value,cw-dp(28),TextUtils.TruncateAt.END).toString(),x+dp(14),y+dp(56),value);
                StaticLayout layout=StaticLayout.Builder.obtain(t[2],0,t[2].length(),caption,(int)Math.max(1,cw-dp(28))).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
                c.save();c.translate(x+dp(14),y+dp(62));layout.draw(c);c.restore();
            }
        }
    }

    /** Big two-sided "who writes more" split bar for private chats. */
    static final class Split extends Animated {
        private final String leftName,rightName,leftValue,rightValue;private final float share;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint name=new TextPaint(Paint.ANTI_ALIAS_FLAG),pct=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r=new RectF();
        Split(Context context,String leftName,String rightName,String leftValue,String rightValue,float share){
            super(context);this.leftName=leftName;this.rightName=rightName;this.leftValue=leftValue;this.rightValue=rightValue;this.share=share;
            name.setTextSize(dp(13.5f));pct.setTextSize(dp(24));pct.setTypeface(AndroidUtilities.bold());
            setContentDescription(leftName+" "+leftValue+", "+rightName+" "+rightValue);
        }
        @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),dp(112));}
        @Override protected void onDraw(Canvas c){
            float left=dp(20),right=getWidth()-dp(20),half=(right-left)/2-dp(6);
            int a=palette(0),b=palette(1);
            name.setColor(gray());
            pct.setColor(a);pct.setTextAlign(Paint.Align.LEFT);c.drawText(leftValue,left,dp(34),pct);
            pct.setColor(b);pct.setTextAlign(Paint.Align.RIGHT);c.drawText(rightValue,right,dp(34),pct);
            name.setTextAlign(Paint.Align.LEFT);c.drawText(TextUtils.ellipsize(leftName,name,half,TextUtils.TruncateAt.END).toString(),left,dp(56),name);
            name.setTextAlign(Paint.Align.RIGHT);c.drawText(TextUtils.ellipsize(rightName,name,half,TextUtils.TruncateAt.END).toString(),right,dp(56),name);
            float mid=left+(right-left)*(0.5f+(share-0.5f)*progress);
            r.set(left,dp(70),right,dp(86));paint.setColor(b);c.drawRoundRect(r,dp(8),dp(8),paint);
            r.right=mid;paint.setColor(a);c.drawRoundRect(r,dp(8),dp(8),paint);
            if(mid-left>dp(8)&&right-mid>dp(8)){r.left=mid-dp(8);r.right=mid;c.drawRect(r,paint);}
            paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));c.drawRect(mid-dp(1),dp(68),mid+dp(1),dp(88),paint);
        }
    }
}
