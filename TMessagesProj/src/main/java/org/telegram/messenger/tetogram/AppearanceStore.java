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
    static void io(Runnable task) { IO.postRunnable(task); }
    static java.util.Set<String> references(long uid) { AppearanceConfig c=CACHE.get(uid);return c==null?java.util.Collections.emptySet():c.mediaReferences(); }
    public static boolean loaded(int account) { return CACHE.containsKey(owner(account)); }
    public static void load(int account, Result callback) {
        final long uid=owner(account);if(uid<=0){if(callback!=null)callback.done(false,new AppearanceConfig());return;}
        IO.postRunnable(()->{
            AppearanceConfig c=CACHE.get(uid);boolean ok=true;
            if(c==null)try{
                File dir=directory(uid);AtomicFile file=new AtomicFile(new File(dir,"appearance.properties"));
                if(file.getBaseFile().exists() || new File(file.getBaseFile()+".bak").exists()){
                    try(FileInputStream in=file.openRead()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Appearance too large");out.write(b,0,n);}c=AppearanceConfig.decode(out.toString("UTF-8"));}
                }else c=new AppearanceConfig();
                CACHE.put(uid,c);
            }catch(Exception e){INVALID.add(uid);ok=false;FileLog.e("Tetogram appearance configuration unavailable");}
            final AppearanceConfig result=c==null?new AppearanceConfig():c.copy();final boolean success=ok;
            AndroidUtilities.runOnUIThread(()->{if(uid!=owner(account))return;if(success){AppearanceRuntime.activate(account);AppearanceRuntime.prepareBaseColors();}if(callback!=null)callback.done(success,result);});
        });
    }
    public static void save(int account,long uid,AppearanceConfig draft,long expectedRevision,Result callback) {
        final AppearanceConfig copy=draft.copy();
        IO.postRunnable(()->{
            boolean ok=false;
            if(uid>0 && uid==owner(account) && !INVALID.contains(uid)){
                AppearanceConfig old=CACHE.get(uid);
                if(old!=null && old.revision==expectedRevision){
                    AtomicFile file=new AtomicFile(new File(directory(uid),"appearance.properties"));FileOutputStream out=null;
                    try{if(!directory(uid).isDirectory() && !directory(uid).mkdirs())throw new IOException("Appearance storage unavailable");copy.revision=old.revision+1;byte[] data=copy.encode().getBytes(StandardCharsets.UTF_8);out=file.startWrite();out.write(data);if(uid!=owner(account))throw new IOException("Account changed");file.finishWrite(out);out=null;CACHE.put(uid,copy);AppearanceMedia.cleanup(uid,copy);ok=true;}
                    catch(Exception e){if(out!=null)file.failWrite(out);FileLog.e("Tetogram appearance save failed");}
                }
            }
            final boolean success=ok;final AppearanceConfig result=snapshot(account);
            AndroidUtilities.runOnUIThread(()->{if(uid!=owner(account))return;if(success)AppearanceRuntime.changed(account);if(callback!=null)callback.done(success,result);});
        });
    }
}
