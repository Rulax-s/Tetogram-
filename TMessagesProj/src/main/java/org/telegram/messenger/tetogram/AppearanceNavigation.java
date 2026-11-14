/* Tetogram, 2026. GNU GPL v2 or later. Only presentation origin, never Telegram routing metadata. */
package org.telegram.messenger.tetogram;
import android.os.Bundle;
public final class AppearanceNavigation {
    private AppearanceNavigation(){}
    public static void copySameDialog(Bundle from,Bundle to){
        if(from==null||to==null)return;
        long chat=to.getLong("chat_id",0),user=to.getLong("user_id",0);
        if(chat!=0?chat!=from.getLong("chat_id",0):user==0||user!=from.getLong("user_id",0))return;
        String section=from.getString("tetogram_appearance_section"),folder=from.getString("tetogram_appearance_folder");
        if(section!=null&&section.matches("[0-9a-fA-F-]{36}"))to.putString("tetogram_appearance_section",section);
        if(folder!=null&&folder.matches("(?:tg:[1-9][0-9]*|local:[0-9a-fA-F-]{36})"))to.putString("tetogram_appearance_folder",folder);
    }
}
