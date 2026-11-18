/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.messenger.tetogram;

import android.util.AtomicFile;
import org.telegram.messenger.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/** One serialized IO queue, atomic documents, UID ownership and optimistic revision checks. */
public final class AppearanceStore {
    private static final DispatchQueue IO = new DispatchQueue("tetogram-appearance");
    private static final ConcurrentHashMap<Long,AppearanceConfig> CACHE = new ConcurrentHashMap<>();
    private static final java.util.Set<Long> INVALID = ConcurrentHashMap.newKeySet();
    public interface Result { void done(boolean ok, AppearanceConfig config); }
    private AppearanceStore() {}
    public static long owner(int account) { return UserConfig.getInstance(account).getClientUserId(); }
    public static File directory(long uid) { return new File(ApplicationLoader.getFilesDirFixed(), "tetogram_appearance/" + AppearanceConfig.accountDirectory(uid)); }
    public static AppearanceConfig snapshot(int account) { AppearanceConfig c=CACHE.get(owner(account));return c==null?new AppearanceConfig():c.copy(); }
    public static AppearanceConfig.Resolved resolve(int account,AppearanceConfig.Context context) { AppearanceConfig c=CACHE.get(owner(account));return c==null?new AppearanceConfig.Resolved():c.resolve(context); }
    public static AppearanceConfig.Resolved resolveOwner(long uid,AppearanceConfig.Context context) { AppearanceConfig c=CACHE.get(uid);return c==null?new AppearanceConfig.Resolved():c.resolve(context); }
    static void io(Runnable task) { IO.postRunnable(task); }
    static java.util.Set<String> references(long uid) { AppearanceConfig c=CACHE.get(uid);return c==null?java.util.Collections.emptySet():c.mediaReferences(); }
    public static boolean loaded(int account) { return CACHE.containsKey(owner(account)); }
    /** True when the stored document could not be read; {@link #recover} can start over. */
    public static boolean invalid(int account) { return INVALID.contains(owner(account)); }
    /**
     * Starts a clean appearance document after an unreadable one. The old document and its media folder are kept
     * side by side under a "corrupt-" name, so nothing is deleted and media cleanup cannot touch them.
     */
    public static void recover(int account, Result callback) {
        final long uid=owner(account);if(uid<=0){if(callback!=null)callback.done(false,new AppearanceConfig());return;}
        IO.postRunnable(()->{
            boolean ok=false;
            try{
                File dir=directory(uid);String stamp="corrupt-"+System.currentTimeMillis();
                File base=new File(dir,"appearance.properties"),backup=new File(dir,"appearance.properties.bak"),media=new File(dir,"media");
                if(base.exists()&&!base.renameTo(new File(dir,"appearance."+stamp+".properties")))throw new IOException("Rename failed");
                if(backup.exists()&&!backup.renameTo(new File(dir,"appearance."+stamp+".properties.bak")))throw new IOException("Rename failed");
                if(media.exists()&&!media.renameTo(new File(dir,"media."+stamp)))throw new IOException("Rename failed");
                CACHE.put(uid,new AppearanceConfig());INVALID.remove(uid);SHARED.clear();ok=true;
            }catch(Exception e){FileLog.e("Tetogram appearance recovery failed");}
            final boolean success=ok;final AppearanceConfig result=snapshot(account);
            AndroidUtilities.runOnUIThread(()->{if(uid!=owner(account))return;if(success)AppearanceRuntime.changed(account,false);if(callback!=null)callback.done(success,result);});
        });
    }
    private static final class Shared{final AppearanceConfig config;final java.util.HashMap<String,AppearanceConfig.Resolved> values=new java.util.HashMap<>();Shared(AppearanceConfig config){this.config=config;}}
    private static final ConcurrentHashMap<Long,Shared> SHARED=new ConcurrentHashMap<>();
    /**
     * Memoized resolution for hot paths (chat-list binds). The result is shared and must be treated as read-only.
     * Invalidated automatically when a new document is published to the cache.
     */
    public static AppearanceConfig.Resolved resolveShared(int account,AppearanceConfig.Context context){
        long uid=owner(account);AppearanceConfig c=CACHE.get(uid);if(c==null)return new AppearanceConfig.Resolved();
        Shared shared=SHARED.get(uid);if(shared==null||shared.config!=c){shared=new Shared(c);SHARED.put(uid,shared);}
        String key=context.section+"|"+context.folder+"|"+context.dialog+"|"+context.topic;
        synchronized(shared){AppearanceConfig.Resolved r=shared.values.get(key);if(r==null){if(shared.values.size()>512)shared.values.clear();r=c.resolve(context);shared.values.put(key,r);}return r;}
    }
    public static void load(int account, Result callback) {
        final long uid=owner(account);if(uid<=0){if(callback!=null)callback.done(false,new AppearanceConfig());return;}
        IO.postRunnable(()->{
            AppearanceConfig c=CACHE.get(uid);boolean ok=true;
            if(c==null)try{
                File dir=directory(uid);AtomicFile file=new AtomicFile(new File(dir,"appearance.properties"));
                if(file.getBaseFile().exists() || new File(file.getBaseFile()+".bak").exists()){
                    try(FileInputStream in=file.openRead()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Appearance too large");out.write(b,0,n);}c=AppearanceConfig.decode(out.toString("UTF-8"));c.migratePalette();}
                }else c=new AppearanceConfig();
                CACHE.put(uid,c);
                try{AppearanceMedia.cleanup(uid,c);AppearanceMedia.cleanupImports(uid);}catch(Exception ignored){FileLog.e("Tetogram appearance cleanup deferred");}
            }catch(Exception e){INVALID.add(uid);ok=false;FileLog.e("Tetogram appearance configuration unavailable");}
            final AppearanceConfig result=c==null?new AppearanceConfig():c.copy();final boolean success=ok;
            AndroidUtilities.runOnUIThread(()->{if(uid!=owner(account))return;if(success){AppearanceRuntime.activate(account);AppearanceRuntime.prepareBaseColors();}if(callback!=null)callback.done(success,result);});
        });
    }
    public static void save(int account,long uid,AppearanceConfig draft,long expectedRevision,Result callback) {save(account,uid,draft,expectedRevision,false,callback);}
    public static void save(int account,long uid,AppearanceConfig draft,long expectedRevision,boolean applyGlobalMode,Result callback) {
        final AppearanceConfig copy=draft.copy();
        IO.postRunnable(()->{
            boolean ok=false,appearanceChanged=false,modeChanged=false;
            if(uid>0 && uid==owner(account) && !INVALID.contains(uid)){
                AppearanceConfig old=CACHE.get(uid);
                if(old!=null && old.revision==expectedRevision){
                    appearanceChanged=!old.sameScopes(copy);AppearanceConfig.Style before=old.scopes.get(AppearanceConfig.GLOBAL),after=copy.scopes.get(AppearanceConfig.GLOBAL);modeChanged=!java.util.Objects.equals(before==null?null:before.values.get("mode"),after==null?null:after.values.get("mode"));
                    AtomicFile file=new AtomicFile(new File(directory(uid),"appearance.properties"));FileOutputStream out=null;
                    try{if(!directory(uid).isDirectory() && !directory(uid).mkdirs())throw new IOException("Appearance storage unavailable");copy.revision=old.revision+1;byte[] data=copy.encode().getBytes(StandardCharsets.UTF_8);out=file.startWrite();out.write(data);if(uid!=owner(account))throw new IOException("Account changed");file.finishWrite(out);out=null;CACHE.put(uid,copy);ok=true;try{AppearanceMedia.cleanup(uid,copy);}catch(Exception ignored){FileLog.e("Tetogram appearance cleanup deferred");}}
                    catch(Exception e){if(out!=null)file.failWrite(out);FileLog.e("Tetogram appearance save failed");}
                }
            }
            final boolean success=ok,notifyAppearance=appearanceChanged||applyGlobalMode,forceMode=modeChanged||applyGlobalMode;final AppearanceConfig result=snapshot(account);
            AndroidUtilities.runOnUIThread(()->{if(uid!=owner(account))return;if(success&&notifyAppearance)AppearanceRuntime.changed(account,forceMode);if(callback!=null)callback.done(success,result);});
        });
    }
}
