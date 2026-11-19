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
        public float number(String key, float fallback) { String text=values.get(key);if(text==null)return fallback;try { float v = Float.parseFloat(text); return Float.isNaN(v) || Float.isInfinite(v) ? fallback : v; } catch (Exception e) { return fallback; } }
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
    public void apply(String scope, Preset preset) { Style next=GLOBAL.equals(scope)?applyPreset(scopes.get(GLOBAL),preset.style):withoutPersonal(preset.style); scopes.put(scope, next); revision++; }

    // ---- Account-level personal data (shortcut identity and color history) ----
    public static final String RECENT_COLORS="palette.recent.",FAVORITE_COLORS="palette.favorite.";
    public static final int PALETTE_SIZE=12;
    /** Not appearance: never copied into presets/exports, never replaced by a preset or a reset. */
    public static boolean isPersonalKey(String key){return key!=null&&(isAccountKey(key)||key.startsWith(AppearancePeople.PREFIX));}
    /** Account data that even a full appearance reset keeps: the personal shortcut and color history. */
    public static boolean isAccountKey(String key){return key!=null&&(key.startsWith("launcher.")||key.startsWith("palette."));}
    public static Style withoutPersonal(Style style){Style out=new Style();if(style!=null)for(Map.Entry<String,String> e:style.values.entrySet())if(!isPersonalKey(e.getKey()))out.values.put(e.getKey(),e.getValue());return out;}
    public static Style personal(Style style){Style out=new Style();if(style!=null)for(Map.Entry<String,String> e:style.values.entrySet())if(isPersonalKey(e.getKey()))out.values.put(e.getKey(),e.getValue());return out;}
    /** Equality of appearance only; color history and shortcut data are ignored. */
    public static boolean sameAppearance(Style a,Style b){return withoutPersonal(a).values.equals(withoutPersonal(b).values);}
    /** A preset replaces the global appearance but keeps the account's personal data. */
    public static Style applyPreset(Style currentGlobal,Style preset){Style out=withoutPersonal(preset);out.values.putAll(personal(currentGlobal).values);return out;}
    /** Writes an editor draft for one scope; personal data (shortcut, color history, people) always comes from the latest stored scope. */
    public void putScope(String scope,Style draft){
        validateScope(scope);
        Style next=applyPreset(scopes.get(scope),draft);
        if(next.values.isEmpty())scopes.remove(scope);else scopes.put(scope,next);
    }
    /** Removes every local appearance override; personal data in the global scope survives. */
    public void resetAppearance(){Style keep=new Style();Style g=scopes.get(GLOBAL);if(g!=null)for(Map.Entry<String,String> e:g.values.entrySet())if(isAccountKey(e.getKey()))keep.values.put(e.getKey(),e.getValue());scopes.clear();if(!keep.values.isEmpty())scopes.put(GLOBAL,keep);}
    public List<Integer> palette(String prefix){ArrayList<Integer> out=new ArrayList<>();Style g=scopes.get(GLOBAL);if(g==null)return out;for(int i=0;i<PALETTE_SIZE;i++){String v=g.values.get(prefix+i);if(v==null)continue;try{Integer c=Integer.valueOf(v);if(!out.contains(c))out.add(c);}catch(NumberFormatException ignored){}}return out;}
    /** Puts a color first in the account-level list, without duplicates, at most {@link #PALETTE_SIZE}. */
    public void remember(String prefix,int color){ArrayList<Integer> list=new ArrayList<>(palette(prefix));list.remove(Integer.valueOf(color));list.add(0,color);writePalette(prefix,list);}
    private void writePalette(String prefix,List<Integer> list){Style g=scopes.computeIfAbsent(GLOBAL,k->new Style());for(int i=0;i<PALETTE_SIZE;i++)g.values.remove(prefix+i);for(int i=0;i<Math.min(PALETTE_SIZE,list.size());i++)g.values.put(prefix+i,Integer.toString(list.get(i)));if(g.values.isEmpty())scopes.remove(GLOBAL);}
    /** Older builds stored color history in chat/section/folder/profile scopes and presets; gather it into the account list. */
    public boolean migratePalette(){
        boolean changed=false;
        for(String prefix:new String[]{RECENT_COLORS,FAVORITE_COLORS}){
            ArrayList<Integer> list=new ArrayList<>(palette(prefix));boolean found=false;
            ArrayList<Style> sources=new ArrayList<>();
            for(Map.Entry<String,Style> e:scopes.entrySet())if(!GLOBAL.equals(e.getKey()))sources.add(e.getValue());
            for(Preset preset:presets.values())sources.add(preset.style);
            for(Style s:sources)for(int i=0;i<PALETTE_SIZE;i++){String v=s.values.remove(prefix+i);if(v==null)continue;found=true;try{Integer c=Integer.valueOf(v);if(!list.contains(c))list.add(c);}catch(NumberFormatException ignored){}}
            if(found){writePalette(prefix,list);changed=true;}
        }
        if(changed)scopes.entrySet().removeIf(e->!GLOBAL.equals(e.getKey())&&e.getValue().values.isEmpty());
        return changed;
    }
    public void reset(String scope) { scopes.remove(scope); revision++; }
    public boolean sameScopes(AppearanceConfig other){if(other==null)return false;Set<String> keys=new HashSet<>(scopes.keySet());keys.addAll(other.scopes.keySet());for(String key:keys){Style a=scopes.get(key),b=other.scopes.get(key);Map<String,String> av=a==null?Collections.emptyMap():a.values,bv=b==null?Collections.emptyMap():b.values;if(!av.equals(bv))return false;}return true;}
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
