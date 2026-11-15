/* Tetogram, 2026. GPL v2 or later. One preview mask, no baked mask in generated adaptive bitmaps. */
package org.telegram.ui.tetogram;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import android.view.Gravity;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.AppearanceIconPolicy;
public final class AppearanceIconPreview extends View {
    private final Bitmap bitmap;private final Drawable backdrop,fallback;private final int shape;private final float scale;
    private final Path mask=new Path();private final RectF rect=new RectF();private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final TextView emoji;
    public AppearanceIconPreview(Context context,Bitmap bitmap,String text,float scale,int shape){this(context,bitmap,text,scale,shape,bitmap==null&&(text==null||text.isEmpty())?R.drawable.tetogram_icon_background:android.R.color.black);}
    public AppearanceIconPreview(Context context,Bitmap bitmap,String text,float scale,int shape,int backgroundResource){super(context);backdrop=context.getResources().getDrawable(backgroundResource).mutate();this.bitmap=bitmap;this.scale=AppearanceIconPolicy.scale(scale);this.shape=shape;fallback=context.getResources().getDrawable(R.drawable.tetogram_mark).mutate();if(text!=null&&!text.isEmpty()){emoji=new TextView(context);emoji.setGravity(Gravity.CENTER);emoji.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,224);emoji.setText(Emoji.replaceEmoji(text,emoji.getPaint().getFontMetricsInt(),false));}else emoji=null;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);int save=canvas.save();rect.set(0,0,getWidth(),getHeight());mask.rewind();if(shape==0)mask.addOval(rect,Path.Direction.CW);else mask.addRoundRect(rect,shape==2?getWidth()*.24f:0,shape==2?getHeight()*.24f:0,Path.Direction.CW);canvas.clipPath(mask);backdrop.setBounds(0,0,getWidth(),getHeight());backdrop.draw(canvas);float side=Math.min(getWidth(),getHeight())*scale;
        if(bitmap!=null&&!bitmap.isRecycled()){float ratio=Math.min(side/bitmap.getWidth(),side/bitmap.getHeight());float w=bitmap.getWidth()*ratio,h=bitmap.getHeight()*ratio;rect.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);canvas.drawBitmap(bitmap,null,rect,paint);}
        else if(emoji!=null){int c=canvas.save();float zoom=side/256;canvas.translate((getWidth()-side)/2,(getHeight()-side)/2);canvas.scale(zoom,zoom);emoji.measure(MeasureSpec.makeMeasureSpec(256,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(256,MeasureSpec.EXACTLY));emoji.layout(0,0,256,256);emoji.draw(canvas);canvas.restoreToCount(c);}
        else{int d=Math.round(Math.min(getWidth(),getHeight())*1.5f*scale/AppearanceIconPolicy.SAFE_CONTENT_SCALE);fallback.setBounds((getWidth()-d)/2,(getHeight()-d)/2,(getWidth()+d)/2,(getHeight()+d)/2);fallback.draw(canvas);}canvas.restoreToCount(save);
    }
}
