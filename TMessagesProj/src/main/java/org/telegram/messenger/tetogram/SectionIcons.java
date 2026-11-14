/* Tetogram, 2026. GNU GPL v2 or later. Only app-private icon files; no Base64 and no upload. */
package org.telegram.messenger.tetogram;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.util.*;
import org.telegram.messenger.*;
public final class SectionIcons {
 public static final DispatchQueue queue=new DispatchQueue("tetogram-section-icons");
 private SectionIcons(){}
 private static File root(long owner){if(owner<=0)throw new IllegalArgumentException();return new File(ApplicationLoader.getFilesDirFixed(),"tetogram_section_icons/"+SectionIconPolicy.accountDirectory(owner));}
 private static File directory(long owner,String section){if(!SectionIconPolicy.sectionId(section))throw new IllegalArgumentException();return new File(root(owner),section);}
 private static File inside(File dir,String name) throws IOException {File file=new File(dir,name);if(!file.getCanonicalFile().getParentFile().equals(dir.getCanonicalFile()))throw new IOException("Invalid icon path");return file;}
 public static File resolve(long owner,String section,String name){if(!SectionIconPolicy.imageName(name))return null;try{File f=inside(directory(owner,section),name);return f.isFile() && f.length()>0?f:null;}catch(Exception e){return null;}}
 public static File input(long owner,String section,Uri uri) throws IOException {
  File dir=directory(owner,section);if(!dir.isDirectory() && !dir.mkdirs())throw new IOException("Icon directory unavailable");File file=inside(dir,UUID.randomUUID()+".input");pending(file,true);
  try(InputStream in=ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(file)){if(in==null)throw new IOException("Image unavailable");byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);out.getFD().sync();return file;}catch(IOException | RuntimeException e){pending(file,false);file.delete();throw new IOException("Image unavailable",e);}
 }
 public static File optimize(long owner,String section,File nativeCrop) throws IOException {
  Bitmap image=null,square=null,scaled=null;File target=null;
  try {
   BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(nativeCrop.getAbsolutePath(),o);o.inSampleSize=SectionIconPolicy.sample(o.outWidth,o.outHeight,512);o.inJustDecodeBounds=false;
   image=BitmapFactory.decodeFile(nativeCrop.getAbsolutePath(),o);if(image==null)throw new IOException("Invalid cropped image");int side=Math.min(image.getWidth(),image.getHeight());square=Bitmap.createBitmap(image,(image.getWidth()-side)/2,(image.getHeight()-side)/2,side,side);scaled=Bitmap.createScaledBitmap(square,512,512,true);
   File dir=directory(owner,section);if(!dir.isDirectory() && !dir.mkdirs())throw new IOException("Icon directory unavailable");target=inside(dir,UUID.randomUUID()+".jpg");pending(target,true);
   try(FileOutputStream out=new FileOutputStream(target)){if(!scaled.compress(Bitmap.CompressFormat.JPEG,88,out))throw new IOException("Icon encoding failed");out.getFD().sync();}
   return target;
  }catch(IOException e){pending(target,false);discard(target);throw e;}catch(OutOfMemoryError | RuntimeException e){pending(target,false);discard(target);throw new IOException("Image cannot be decoded safely");}
  finally{if(scaled!=null && scaled!=square && scaled!=image)scaled.recycle();if(square!=null && square!=image)square.recycle();if(image!=null)image.recycle();}
 }
 public static void discard(File file){if(file==null)return;try{File base=new File(ApplicationLoader.getFilesDirFixed(),"tetogram_section_icons").getCanonicalFile();String path=file.getCanonicalPath();if(path.startsWith(base.getPath()+File.separator) && (SectionIconPolicy.inputName(file.getName()) || SectionIconPolicy.imageName(file.getName())))file.delete();}catch(Exception ignore){}}
 /** Runs only after preferences were successfully committed. Keep pending editors' files until explicit cancel. */
 public static void cleanup(long owner,SectionConfig config){
  queue.postRunnable(()->{File base=root(owner);File[] dirs=base.listFiles();if(dirs==null)return;for(File dir:dirs){if(!SectionIconPolicy.sectionId(dir.getName()))continue;SectionConfig.Section section=config.section(dir.getName());File[] files=dir.listFiles();if(files==null)continue;for(File f:files)if((SectionIconPolicy.imageName(f.getName()) || SectionIconPolicy.inputName(f.getName())) && (SectionIconPolicy.inputName(f.getName()) || section==null || !"image".equals(section.iconType) || !section.iconImage.equals(f.getName()))){
    // Re-read current owner configuration before removing a replaced/deleted image.
    boolean referenced=false;try{int account=UserConfig.selectedAccount;for(int a=0;a<UserConfig.MAX_ACCOUNT_COUNT;a++)if(UserConfig.getInstance(a).getClientUserId()==owner){account=a;break;}if(UserConfig.getInstance(account).getClientUserId()!=owner)continue;SectionConfig latest=SectionsStore.load(account);SectionConfig.Section current=latest.section(dir.getName());referenced=current!=null && "image".equals(current.iconType) && f.getName().equals(current.iconImage);}catch(Exception e){continue;}
    if(!referenced && !pending.contains(f.getAbsolutePath()))discard(f);
   }if(section==null && dir.list()!=null && dir.list().length==0)dir.delete();}});
 }
 private static final Set<String> pending=java.util.Collections.synchronizedSet(new HashSet<>());
 public static void pending(File f,boolean value){if(f!=null){if(value)pending.add(f.getAbsolutePath());else pending.remove(f.getAbsolutePath());}}
}
