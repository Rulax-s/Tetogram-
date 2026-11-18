/* Tetogram, 2026. GNU GPL v2 or later. Draws a sender's own bubble fill/outline over the native bubble. */
package org.telegram.ui.tetogram;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.tetogram.AppearancePeople;
import org.telegram.messenger.tetogram.AppearanceRuntime;
import org.telegram.ui.ActionBar.MessageDrawable;
import org.telegram.ui.ActionBar.Theme;

/** Only incoming, single (non-album) messages with a known sender; the message, its ID and layout are untouched. */
public final class PersonBubble {
    private static final Paint FILL=new Paint(Paint.ANTI_ALIAS_FLAG),STROKE=new Paint(Paint.ANTI_ALIAS_FLAG);
    static{STROKE.setStyle(Paint.Style.STROKE);}
    private PersonBubble(){}

    public static AppearancePeople.Style style(MessageObject message,Theme.ResourcesProvider provider){
        if(message==null||message.messageOwner==null||message.isOutOwner())return null;
        long sender=message.getFromChatId();if(sender==0)return null;
        return AppearancePeople.read(key->provider==null?AppearanceRuntime.globalValue(key):provider.getAppearanceValue(key),sender);
    }

    /** Called after the native background; the canvas still has the cell's background transform. */
    public static void draw(Canvas canvas,MessageDrawable background,MessageObject message,Theme.ResourcesProvider provider,boolean grouped,float alpha){
        if(background==null||grouped)return;
        AppearancePeople.Style style=style(message,provider);if(style==null)return;
        Path path;try{path=background.makePath();}catch(Exception e){return;}if(path==null)return;
        int a=Math.round(255*Math.max(0,Math.min(1,alpha)));
        if(style.fill!=null){FILL.setColor(style.fill);FILL.setAlpha(Math.round((style.fill>>>24)*a/255f));canvas.drawPath(path,FILL);}
        if(style.hasOutline()){STROKE.setColor(style.outline);STROKE.setAlpha(Math.round((style.outline>>>24)*a/255f));STROKE.setStrokeWidth(AndroidUtilities.dpf2(style.width));canvas.drawPath(path,STROKE);}
    }

    /** Readable message text on a custom fill; null keeps the theme color. */
    public static Integer text(MessageObject message,Theme.ResourcesProvider provider,int themeText){
        AppearancePeople.Style style=style(message,provider);if(style==null||style.fill==null)return null;
        int canvas=provider==null?Theme.getColor(Theme.key_chat_inBubble):provider.getColor(Theme.key_chat_inBubble);
        int text=AppearancePeople.text(style.fill,themeText,canvas);return text==themeText?null:text;
    }
}
