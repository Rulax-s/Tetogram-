package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class AppearancePersonalDataTest {
    private static final String SECTION="b8348b88-aac9-4f3c-85f7-bb44b7e2b967";
    private static AppearanceConfig withPersonalGlobal(){
        AppearanceConfig c=new AppearanceConfig();AppearanceConfig.Style g=c.edit(AppearanceConfig.GLOBAL);
        g.set("launcher.label","Моя Тето");g.set("launcher.emoji","🎀");g.set("launcher.image.file","0f8fad5b-d9cb-469f-a165-70867728950e.png");
        g.set("palette.recent.0","-16777216");g.set("palette.favorite.0","-65536");g.set("mode","dark");g.color(false,"chat_outBubble",1);
        return c;
    }
    @Test public void personalKeysAreLauncherAndPalette(){
        assertTrue(AppearanceConfig.isPersonalKey("launcher.label"));assertTrue(AppearanceConfig.isPersonalKey("palette.recent.3"));
        assertFalse(AppearanceConfig.isPersonalKey("light.chat_outBubble"));assertFalse(AppearanceConfig.isPersonalKey("effects.enabled"));assertFalse(AppearanceConfig.isPersonalKey("mode"));
    }
    @Test public void presetReplacesAppearanceButKeepsShortcutAndColorHistory(){
        AppearanceConfig c=withPersonalGlobal();
        AppearanceConfig.Preset preset=AppearancePresets.builtins().get(0);
        c.apply(AppearanceConfig.GLOBAL,preset);
        AppearanceConfig.Style g=c.scopes.get(AppearanceConfig.GLOBAL);
        assertEquals("Моя Тето",g.values.get("launcher.label"));assertEquals("🎀",g.values.get("launcher.emoji"));
        assertEquals("0f8fad5b-d9cb-469f-a165-70867728950e.png",g.values.get("launcher.image.file"));
        assertEquals("-16777216",g.values.get("palette.recent.0"));assertEquals("-65536",g.values.get("palette.favorite.0"));
        assertEquals(preset.style.values.get("mode"),g.values.get("mode"));
        assertTrue("shortcut image stays referenced, so cleanup keeps it",c.mediaReferences().contains("0f8fad5b-d9cb-469f-a165-70867728950e.png"));
    }
    @Test public void presetPersonalValuesNeverOverwriteTheAccount(){
        AppearanceConfig.Style current=withPersonalGlobal().scopes.get(AppearanceConfig.GLOBAL);
        AppearanceConfig.Style foreign=new AppearanceConfig.Style();foreign.set("launcher.label","Чужой");foreign.set("palette.recent.0","5");foreign.set("mode","light");
        AppearanceConfig.Style out=AppearanceConfig.applyPreset(current,foreign);
        assertEquals("Моя Тето",out.values.get("launcher.label"));assertEquals("-16777216",out.values.get("palette.recent.0"));assertEquals("light",out.values.get("mode"));
        assertNull(AppearanceConfig.applyPreset(null,foreign).values.get("launcher.label"));
    }
    @Test public void resetRemovesAppearanceEverywhereButKeepsPersonalData(){
        AppearanceConfig c=withPersonalGlobal();c.edit("chat:5").set("bubble.inRadius","4");c.edit("profile:7").set("banner.height","300");
        AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name="x";c.presets.put(p.id,p);
        c.resetAppearance();
        assertEquals(Collections.singleton(AppearanceConfig.GLOBAL),c.scopes.keySet());
        AppearanceConfig.Style g=c.scopes.get(AppearanceConfig.GLOBAL);
        assertEquals("Моя Тето",g.values.get("launcher.label"));assertNull(g.values.get("mode"));assertNull(g.values.get("light.chat_outBubble"));
        assertEquals("presets survive a reset",1,c.presets.size());
        AppearanceConfig empty=new AppearanceConfig();empty.edit("chat:1").set("x","1");empty.resetAppearance();assertTrue(empty.scopes.isEmpty());
    }
    @Test public void putScopeKeepsLatestStoredPersonalData(){
        AppearanceConfig fresh=withPersonalGlobal();fresh.remember(AppearanceConfig.RECENT_COLORS,42);
        AppearanceConfig.Style staleDraft=new AppearanceConfig.Style();staleDraft.set("palette.recent.0","-16777216");staleDraft.set("launcher.label","старое");staleDraft.set("mode","light");
        fresh.putScope(AppearanceConfig.GLOBAL,staleDraft);
        AppearanceConfig.Style g=fresh.scopes.get(AppearanceConfig.GLOBAL);
        assertEquals("light",g.values.get("mode"));assertEquals("Моя Тето",g.values.get("launcher.label"));assertEquals("42",g.values.get("palette.recent.0"));
        AppearanceConfig.Style chat=new AppearanceConfig.Style();chat.set("palette.recent.0","1");chat.set("bubble.inRadius","3");
        fresh.putScope("chat:9",chat);assertNull(fresh.scopes.get("chat:9").values.get("palette.recent.0"));
        fresh.putScope("chat:9",new AppearanceConfig.Style());assertFalse("empty draft removes the scope",fresh.scopes.containsKey("chat:9"));
    }
    @Test public void rememberPutsFirstWithoutDuplicatesAndCaps(){
        AppearanceConfig c=new AppearanceConfig();
        for(int i=0;i<20;i++)c.remember(AppearanceConfig.RECENT_COLORS,i);
        c.remember(AppearanceConfig.RECENT_COLORS,15);
        List<Integer> list=c.palette(AppearanceConfig.RECENT_COLORS);
        assertEquals(AppearanceConfig.PALETTE_SIZE,list.size());assertEquals(Integer.valueOf(15),list.get(0));assertEquals(Integer.valueOf(19),list.get(1));
        assertEquals(new HashSet<>(list).size(),list.size());
        assertTrue(c.palette(AppearanceConfig.FAVORITE_COLORS).isEmpty());
    }
    @Test public void migrationGathersOldScopedHistoryIntoAccountList(){
        AppearanceConfig c=new AppearanceConfig();
        c.edit(AppearanceConfig.GLOBAL).set("palette.recent.0","1");
        c.edit("chat:5").set("palette.recent.0","2");c.edit("chat:5").set("palette.recent.1","1");c.edit("chat:5").set("bubble.inRadius","4");
        c.edit("section:"+SECTION).set("palette.favorite.0","9");
        AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name="p";p.style.set("palette.recent.0","3");p.style.set("mode","dark");c.presets.put(p.id,p);
        long revision=c.revision;
        assertTrue(c.migratePalette());
        assertEquals(Arrays.asList(1,2,3),c.palette(AppearanceConfig.RECENT_COLORS));assertEquals(Collections.singletonList(9),c.palette(AppearanceConfig.FAVORITE_COLORS));
        assertEquals("4",c.scopes.get("chat:5").values.get("bubble.inRadius"));assertNull(c.scopes.get("chat:5").values.get("palette.recent.0"));
        assertFalse("palette-only scope disappears",c.scopes.containsKey("section:"+SECTION));
        assertEquals("dark",p.style.values.get("mode"));assertNull(p.style.values.get("palette.recent.0"));
        assertEquals("migration is not an appearance change",revision,c.revision);
        assertFalse("second run is a no-op",c.migratePalette());
        AppearanceConfig d=AppearanceConfig.decode(c.encode());assertEquals(Arrays.asList(1,2,3),d.palette(AppearanceConfig.RECENT_COLORS));
    }
    @Test public void dirtinessAndRebaseIgnorePersonalData(){
        AppearanceConfig.Style a=new AppearanceConfig.Style(),b=new AppearanceConfig.Style();a.set("mode","dark");b.set("mode","dark");b.set("palette.recent.0","5");
        assertFalse(AppearanceEditorPolicy.dirtyAppearance(a,b));assertTrue(AppearanceEditorPolicy.dirty(a,b));
        assertEquals(AppearanceEditorPolicy.Rebase.WRITE,AppearanceEditorPolicy.rebase(a,b,false));
        assertEquals(AppearanceEditorPolicy.Rebase.WRITE,AppearanceEditorPolicy.rebase(null,null,false));
        b.set("mode","light");
        assertEquals(AppearanceEditorPolicy.Rebase.CONFLICT,AppearanceEditorPolicy.rebase(a,b,false));
        assertEquals(AppearanceEditorPolicy.Rebase.WRITE,AppearanceEditorPolicy.rebase(a,b,true));
    }
    @Test public void resettingColorsKeepsFavorites(){
        AppearanceConfig.Style s=new AppearanceConfig.Style();s.set("palette.favorite.0","1");s.set("accent.primary","2");s.color(false,"windowBackgroundWhite",3);
        AppearanceConfig.Style r=AppearanceEditorPolicy.resetCategory(s,AppearanceEditorPolicy.Category.COLORS);
        assertEquals("1",r.values.get("palette.favorite.0"));assertNull(r.values.get("light.windowBackgroundWhite"));
    }
    @Test public void exportedArchiveContainsNoPersonalData()throws Exception{
        AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name="share";p.style=withPersonalGlobal().scopes.get(AppearanceConfig.GLOBAL).copy();p.style.values.remove("launcher.image.file");
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
        AppearanceArchive.write(out,p,name->null,true,true);
        String manifest;try(java.util.zip.ZipInputStream zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(out.toByteArray()))){zip.getNextEntry();java.io.ByteArrayOutputStream text=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=zip.read(b))>0)text.write(b,0,n);manifest=new String(text.toByteArray(),java.nio.charset.StandardCharsets.ISO_8859_1);}
        assertFalse(manifest.contains("launcher."));assertFalse(manifest.contains("palette."));assertTrue(manifest.contains("style.mode=dark"));
    }
}
