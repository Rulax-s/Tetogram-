/* Tetogram, 2026. GNU GPL v2 or later. Tappable parts of the editor preview and the color roles behind them. */
package org.telegram.messenger.tetogram;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AppearanceElements {
    public static final String WALLPAPER="wallpaper",INCOMING="incoming",OUTGOING="outgoing",HEADER="header",INPUT="input",
        LIST_ROW="listRow",LIST_BACKGROUND="listBackground",PROFILE_NAME="profileName",PROFILE_STATUS="profileStatus",BANNER="banner",SECTION="section";
    private AppearanceElements(){}

    /** A role: the stored color name and whether it is a background (true) or text drawn on it (false). */
    public static final class Role {
        public final String name;public final boolean surface;
        Role(String name,boolean surface){this.name=name;this.surface=surface;}
    }

    /** Roles in display order; the first one is the element's main color. Empty for unknown elements. */
    public static List<Role> roles(String element){
        if(element==null)return Collections.emptyList();
        switch(element){
            case WALLPAPER:return Collections.singletonList(new Role("chat_wallpaper",true));
            case INCOMING:return Arrays.asList(new Role("chat_inBubble",true),new Role("chat_messageTextIn",false),new Role("chat_inTimeText",false));
            case OUTGOING:return Arrays.asList(new Role("chat_outBubble",true),new Role("chat_messageTextOut",false),new Role("chat_outTimeText",false));
            case HEADER:return Arrays.asList(new Role("actionBarDefault",true),new Role("actionBarDefaultTitle",false),new Role("actionBarDefaultIcon",false));
            case INPUT:return Arrays.asList(new Role("chat_messagePanelBackground",true),new Role("chat_messagePanelText",false),new Role("chat_messagePanelHint",false));
            case LIST_ROW:return Arrays.asList(new Role("list.windowBackgroundWhite",true),new Role("list.chats_name",false),new Role("list.chats_message",false),new Role("list.chats_date",false),new Role("list.chats_unreadCounter",true));
            case LIST_BACKGROUND:return Collections.singletonList(new Role("list.chat_wallpaper",true));
            case PROFILE_NAME:return Collections.singletonList(new Role("banner.text",false));
            case PROFILE_STATUS:return Collections.singletonList(new Role("banner.secondaryText",false));
            case BANNER:return Collections.singletonList(new Role("banner.chat_wallpaper",true));
            case SECTION:return Arrays.asList(new Role("section.background",true),new Role("section.text",false),new Role("section.badge",true),new Role("section.badgeText",false));
            default:return Collections.emptyList();
        }
    }

    /** Whether an element belongs to a preview surface (0/3 chat, 1/4/5 list, 2 profile). */
    public static boolean onSurface(String element,int surface){
        if(element==null)return false;
        boolean chat=surface==0||surface==3,list=surface==1||surface==4||surface==5,profile=surface==2;
        switch(element){
            case WALLPAPER:case INCOMING:case OUTGOING:case HEADER:case INPUT:return chat;
            case LIST_ROW:case LIST_BACKGROUND:return list;
            case SECTION:return surface==4;
            case PROFILE_NAME:case PROFILE_STATUS:case BANNER:return profile;
            default:return false;
        }
    }

    /** Profile elements use the banner domain, which only the global and profile scopes may change. */
    public static boolean allowed(String element,String scope){
        if(element==null)return false;
        boolean profileElement=element.equals(PROFILE_NAME)||element.equals(PROFILE_STATUS)||element.equals(BANNER);
        boolean global=AppearanceConfig.GLOBAL.equals(scope),profile=scope!=null&&scope.startsWith("profile:");
        if(profileElement)return global||profile;
        if(profile)return false;
        if(element.equals(SECTION))return global||scope.startsWith("section:");
        return true;
    }

    /** Index of the role a text role is drawn on, or -1 for surfaces. */
    public static int backgroundOf(List<Role> roles,int index){if(roles.get(index).surface)return -1;for(int i=index-1;i>=0;i--)if(roles.get(i).surface)return i;return -1;}
}
