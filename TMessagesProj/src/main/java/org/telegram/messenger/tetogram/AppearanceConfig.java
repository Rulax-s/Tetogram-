/* Tetogram, 2026. GNU GPL v2 or later. Local presentation only. */
package org.telegram.messenger.tetogram;

import java.io.*;
import java.util.*;

/** Versioned, Android-independent appearance document. No Telegram data in presets. */
public final class AppearanceConfig {
    public static final int SCHEMA = 1;
    public static final String GLOBAL = "global";
    public final LinkedHashMap<String, Style> scopes = new LinkedHashMap<>();
    public final LinkedHashMap<String, Preset> presets = new LinkedHashMap<>();
    public long revision;

    public static final class Style {
        public final LinkedHashMap<String, String> values = new LinkedHashMap<>();
        public Style copy() { Style s = new Style(); s.values.putAll(values); return s; }
        public void set(String key, String value) {
            if (!key.matches("[A-Za-z][A-Za-z0-9_.]{0,100}") || value == null || value.length() > 4096) throw new IllegalArgumentException("Invalid appearance value");
            values.put(key, value);
        }
        public void color(boolean dark, String name, int argb) { set((dark ? "dark." : "light.") + name, Integer.toString(argb)); }
    }
    public static final class Preset {
        public String id = UUID.randomUUID().toString(), name = "";
        public Style style = new Style();
        public Preset duplicate(String title) { Preset p = new Preset(); p.name = title; p.style = style.copy(); return p; }
    }
    public static final class Context {
        public final String section, folder;
        public final long dialog, topic;
        public Context(String section, String folder, long dialog, long topic) {
            this.section = section; this.folder = folder; this.dialog = dialog; this.topic = topic;
        }
        /** Search, notifications and deep links deliberately have no arbitrary folder origin. */
        public static Context direct(long dialog, long topic) { return new Context(null, null, dialog, topic); }
    }
    public static final class Resolved {
        public final LinkedHashMap<String, String> values = new LinkedHashMap<>(), origins = new LinkedHashMap<>();
        public String get(String key) { return values.get(key); }
        public float number(String key, float fallback) { try { float v = Float.parseFloat(values.get(key)); return Float.isNaN(v) || Float.isInfinite(v) ? fallback : v; } catch (Exception e) { return fallback; } }
        public Integer color(boolean dark, String name) { try { String s = get((dark ? "dark." : "light.") + name); return s == null ? null : Integer.valueOf(s); } catch (Exception e) { return null; } }
    }
    public Style edit(String scope) { validateScope(scope); return scopes.computeIfAbsent(scope, k -> new Style()); }
    public Resolved resolve(Context c) {
        Resolved r = new Resolved(); merge(r, GLOBAL);
        if (c.section != null) merge(r, "section:" + c.section);
        if (c.folder != null) merge(r, "folder:" + c.folder);
        if (c.dialog != 0) merge(r, "chat:" + c.dialog);
        if (c.dialog != 0 && c.topic != 0) merge(r, "topic:" + c.dialog + ":" + c.topic);
        return r;
    }
    public Resolved resolveProfile(long dialog) { Resolved r = resolve(Context.direct(0, 0)); merge(r, "profile:" + dialog); return r; }
    private void merge(Resolved r, String scope) {
        Style s = scopes.get(scope); if (s == null) return;
        for(String domain:new String[]{"wallpaper","list.wallpaper","banner.wallpaper"}){
        String base=s.values.get(domain+".base");
        if(!GLOBAL.equals(scope) && ("global".equals(base)||"telegram".equals(base))){
            for(String key:new ArrayList<>(r.values.keySet()))if(isSurfaceWallpaperKey(key,domain)){r.values.remove(key);r.origins.remove(key);}
            Style global=scopes.get(GLOBAL);
            if("global".equals(base)&&global!=null)for(Map.Entry<String,String> e:global.values.entrySet())if(isSurfaceWallpaperKey(e.getKey(),domain)){r.values.put(e.getKey(),e.getValue());r.origins.put(e.getKey(),GLOBAL);}
        }
        }
        for (Map.Entry<String,String> e : s.values.entrySet()) { r.values.put(e.getKey(), e.getValue()); r.origins.put(e.getKey(), scope); }
    }
    public static boolean isWallpaperKey(String key){return key.startsWith("wallpaper.")||key.startsWith("light.chat_wallpaper")||key.startsWith("dark.chat_wallpaper");}
    public static boolean isSurfaceWallpaperKey(String key,String domain){if("wallpaper".equals(domain))return isWallpaperKey(key);String prefix=domain.substring(0,domain.length()-"wallpaper".length());return key.startsWith(domain+".")||key.startsWith("light."+prefix+"chat_wallpaper")||key.startsWith("dark."+prefix+"chat_wallpaper");}
    public void apply(String scope, Preset preset) { scopes.put(scope, preset.style.copy()); revision++; }
    public void reset(String scope) { scopes.remove(scope); revision++; }
    public AppearanceConfig copy() { return decode(encode()); }
    public static String accountDirectory(long uid) { if (uid <= 0) throw new IllegalArgumentException("Account unavailable"); return "u" + uid; }
    public static void validateScope(String s) {
        if (s == null || !(GLOBAL.equals(s) || s.matches("section:[0-9a-fA-F-]{36}") || s.matches("folder:(?:tg:[1-9][0-9]*|local:[0-9a-fA-F-]{36})") || s.matches("(?:chat|profile):-?[1-9][0-9]*") || s.matches("topic:-?[1-9][0-9]*:[1-9][0-9]*"))) throw new IllegalArgumentException("Invalid appearance scope");
    }
    public String encode() {
        Properties p = new Properties(); p.setProperty("schema", "" + SCHEMA); p.setProperty("revision", "" + revision);
        p.setProperty("scopes", String.join(",", scopes.keySet())); p.setProperty("presets", String.join(",", presets.keySet()));
        for (Map.Entry<String,Style> e : scopes.entrySet()) { validateScope(e.getKey()); write(p, "s." + e.getKey() + ".", e.getValue()); }
        for (Preset preset : presets.values()) { UUID.fromString(preset.id); p.setProperty("p." + preset.id + ".name", preset.name); write(p, "p." + preset.id + ".", preset.style); }
        try { StringWriter out = new StringWriter(); p.store(out, "Tetogram local appearance"); return out.toString(); } catch (IOException e) { throw new IllegalStateException(e); }
    }
    private static void write(Properties p, String prefix, Style s) { for (Map.Entry<String,String> e : s.values.entrySet()) p.setProperty(prefix + "v." + e.getKey(), e.getValue()); }
    private static Style read(Properties p, String prefix) { Style s = new Style(); for (String key : p.stringPropertyNames()) if (key.startsWith(prefix + "v.")) s.set(key.substring(prefix.length() + 2), p.getProperty(key)); return s; }
    public static AppearanceConfig decode(String data) {
        if (data == null || data.isEmpty()) return new AppearanceConfig();
        if (data.length() > 4 * 1024 * 1024) throw new IllegalArgumentException("Appearance document too large");
        try {
            Properties p = new Properties(); p.load(new StringReader(data));
            if (!Integer.toString(SCHEMA).equals(p.getProperty("schema"))) throw new IllegalArgumentException("Unsupported appearance schema");
            AppearanceConfig c = new AppearanceConfig(); c.revision = Long.parseLong(p.getProperty("revision", "0"));
            for (String scope : split(p.getProperty("scopes", ""))) { validateScope(scope); c.scopes.put(scope, read(p, "s." + scope + ".")); }
            for (String id : split(p.getProperty("presets", ""))) { UUID.fromString(id); Preset preset = new Preset(); preset.id = id; preset.name = p.getProperty("p." + id + ".name", ""); if (preset.name.length() > 256) throw new IllegalArgumentException("Preset name too long"); preset.style = read(p, "p." + id + "."); c.presets.put(id, preset); }
            return c;
        } catch (IOException e) { throw new IllegalArgumentException("Invalid appearance document", e); }
    }
    private static List<String> split(String s) { return s.isEmpty() ? Collections.emptyList() : Arrays.asList(s.split(",", -1)); }
    /** All references, not just the active theme: shared preset assets must survive cleanup. */
    public Set<String> mediaReferences() { Set<String> refs = new HashSet<>(); for (Style s : scopes.values()) refs(s, refs); for (Preset p : presets.values()) refs(p.style, refs); return refs; }
    private static void refs(Style s, Set<String> out) { for (Map.Entry<String,String> e : s.values.entrySet()) if (e.getKey().endsWith(".file") && isMediaName(e.getValue())) out.add(e.getValue()); }
    public static boolean isMediaName(String s) { return s != null && s.matches("[0-9a-fA-F-]{36}\\.(?:jpg|png|webp|mp4|webm)"); }
}
