/* Tetogram, 2026. GNU GPL v2 or later. Simple-mode palettes use native roles and keep media references. */
package org.telegram.messenger.tetogram;
public final class AppearanceAccent {
    private AppearanceAccent(){}
    public static int readable(int color,int background){int opaque=color|0xff000000;if(AppearancePresets.ratio(opaque,background)>=4.5)return opaque;int end=AppearancePresets.contrast(background);float low=0,high=1;for(int i=0;i<16;i++){float mid=(low+high)/2;int candidate=AppearancePresets.mix(opaque,end,mid);if(AppearancePresets.ratio(candidate,background)>=4.5)high=mid;else low=mid;}return AppearancePresets.mix(opaque,end,high);}
    private static void put(AppearanceConfig.Style style,boolean dark,String domain,int color,String...roles){for(String role:roles)style.color(dark,domain+role,color);}
    public static void apply(AppearanceConfig.Style style,boolean dark,int accent,String domain){
        accent|=0xff000000;int base=dark?0xff17212b:0xffffffff,surface=dark?AppearancePresets.mix(base,accent,.08f):0xffffffff,wall=AppearancePresets.mix(base,accent,dark?.14f:.07f),text=AppearancePresets.contrast(surface),out=AppearancePresets.mix(base,accent,dark?.38f:.18f);
        put(style,dark,domain,accent,"accent.primary","chats_actionBackground","chats_unreadCounter","glass_tabSelected");put(style,dark,domain,AppearancePresets.contrast(accent),"chats_actionIcon","chats_unreadCounterText");
        put(style,dark,domain,surface,"windowBackgroundWhite","actionBarDefault","chat_messagePanelBackground","chat_inBubble","chat_inBubbleMedia","glass_targetMainTabs");
        put(style,dark,domain,text,"windowBackgroundWhiteBlackText","actionBarDefaultTitle","actionBarDefaultIcon","chat_messagePanelText","chat_messageTextIn","chats_name");
        put(style,dark,domain,AppearancePresets.mix(surface,text,.65f),"chats_message","chats_date","glass_tabUnselected");
        put(style,dark,domain,wall,"chat_wallpaper","windowBackgroundGray");put(style,dark,domain,out,"chat_outBubble","chat_outBubbleMedia");put(style,dark,domain,AppearancePresets.contrast(out),"chat_messageTextOut");
        put(style,dark,domain,readable(accent,surface),"windowBackgroundWhiteBlueText","windowBackgroundWhiteBlueText4","chat_messageLinkIn","glass_tabSelectedText");put(style,dark,domain,readable(accent,out),"chat_messageLinkOut");put(style,dark,domain,accent,"chat_inReplyLine","chat_outReplyLine");
    }
}
