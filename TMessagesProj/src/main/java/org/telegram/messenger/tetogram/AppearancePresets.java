/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.messenger.tetogram;
import java.util.*;

/** Working palettes use native ThemeColors string names, never unstable resource integers. */
public final class AppearancePresets {
    private AppearancePresets() {}
    public static List<AppearanceConfig.Preset> builtins() {
        ArrayList<AppearanceConfig.Preset> out = new ArrayList<>();
        out.add(make("Tetogram Classic", 0xff3388cc, 0xffe6ebee, 0xffefffde, 0xff17212b, 0xff2b5278));
        out.add(make("Tetogram Dark", 0xff64b5ef, 0xffedf3fa, 0xffd5e8ff, 0xff111b26, 0xff284b6d));
        out.add(make("AMOLED", 0xff78b9ff, 0xfff4f4f4, 0xffe0ecff, 0xff000000, 0xff132a43));
        out.add(make("Pastel", 0xffa6639b, 0xfff7ecf4, 0xffead6ef, 0xff261d2b, 0xff644a70));
        out.add(make("Glass", 0xff4a9baa, 0xffdeedf0, 0xffc6e6e9, 0xff11252b, 0xff26505d));
        out.add(make("Minimal", 0xff555555, 0xfff5f5f5, 0xffe6e6e6, 0xff171717, 0xff333333));
        out.add(make("Neon", 0xffa546e8, 0xfff0e8ff, 0xffe6cbff, 0xff140c23, 0xff552d7c));
        return out;
    }
    private static AppearanceConfig.Preset make(String name, int accent, int lightBg, int lightOut, int darkBg, int darkOut) {
        AppearanceConfig.Preset p = new AppearanceConfig.Preset(); p.name = name;
        p.id = UUID.nameUUIDFromBytes(("Tetogram builtin:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        for (boolean dark : new boolean[]{false, true}) {
            int bg = dark ? darkBg : lightBg, surface = dark ? name.equals("AMOLED") ? 0xff000000 : mix(darkBg, 0xffffffff, .06f) : 0xffffffff, text = contrast(surface), out = dark ? darkOut : lightOut;
            colors(p.style, dark, bg, "windowBackgroundGray", "chat_wallpaper");
            colors(p.style, dark, surface, "windowBackgroundWhite", "actionBarDefault", "chat_messagePanelBackground", "chat_inBubble", "chat_inBubbleMedia");
            colors(p.style, dark, text, "windowBackgroundWhiteBlackText", "actionBarDefaultTitle", "actionBarDefaultIcon", "chat_messageTextIn", "chats_name", "chat_messagePanelText");
            colors(p.style, dark, out, "chat_outBubble", "chat_outBubbleMedia");
            colors(p.style, dark, contrast(out), "chat_messageTextOut");
            colors(p.style, dark, accent, "chats_actionBackground", "windowBackgroundWhiteBlueText", "windowBackgroundWhiteBlueText4", "chat_messageLinkIn", "chat_messageLinkOut", "chats_unreadCounter", "chat_inReplyLine", "chat_outReplyLine");
            colors(p.style,dark,contrast(accent),"chats_actionIcon","chats_unreadCounterText");
            if(name.equals("Glass")){colors(p.style,dark,bg,"list.chat_wallpaper");colors(p.style,dark,mix(bg,accent,.35f),"list.chat_wallpaper_gradient_to1");}
        }
        if(name.equals("Glass")){p.style.set("list.wallpaper.type","gradient");p.style.set("list.rowOpacity","0.76");p.style.set("list.radius","14");p.style.set("list.sideInset","8");p.style.set("list.rowGap","4");}
        if(name.equals("Minimal")){p.style.set("list.separators","false");p.style.set("effects.enabled","false");}
        p.style.set("mode", name.equals("Tetogram Dark") || name.equals("AMOLED") || name.equals("Neon") ? "dark" : name.equals("Tetogram Classic") || name.equals("Pastel") || name.equals("Minimal") ? "light" : "auto");
        p.style.set("bubble.inRadius", "16"); p.style.set("bubble.outRadius", "16");
        return p;
    }
    private static void colors(AppearanceConfig.Style s, boolean d, int c, String... names) { for (String n : names) s.color(d, n, c); }
    public static int mix(int a, int b, float t) { int c = 0; for (int shift : new int[]{0,8,16,24}) c |= Math.round(((a >>> shift) & 255) * (1-t) + ((b >>> shift) & 255) * t) << shift; return c; }
    public static double luminance(int argb) { double[] rgb = new double[3]; for (int i=0;i<3;i++) { double v=((argb >>> (16-i*8)) & 255)/255.; rgb[i]=v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4); } return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722; }
    public static double ratio(int a,int b) { double x=luminance(a)+.05,y=luminance(b)+.05;return Math.max(x,y)/Math.min(x,y); }
    public static int contrast(int bg) { return ratio(0xff000000,bg)>=ratio(0xffffffff,bg)?0xff000000:0xffffffff; }
}
