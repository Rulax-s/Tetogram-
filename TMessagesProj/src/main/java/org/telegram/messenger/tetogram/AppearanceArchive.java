/* Tetogram, 2026. GNU GPL v2 or later. Portable styles only, no account/scope/session data. */
package org.telegram.messenger.tetogram;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
public final class AppearanceArchive {
    public static final long MAX_ASSET=256L*1024*1024,MAX_TOTAL=512L*1024*1024;
    private static final int MAX_MANIFEST=512*1024,MAX_ASSETS=64;
    public interface Media {File file(String name);}
    public static final class Imported {public final AppearanceConfig.Preset preset;public final List<File> files;Imported(AppearanceConfig.Preset p,List<File> files){preset=p;this.files=files;}}
    private AppearanceArchive(){}
    private static boolean safeName(String name){if(!AppearanceConfig.isMediaName(name))return false;try{return UUID.fromString(name.substring(0,36)).toString().equalsIgnoreCase(name.substring(0,36));}catch(Exception e){return false;}}
    private static boolean video(String name){return name.endsWith(".mp4")||name.endsWith(".webm");}
    private static void validateStyle(AppearanceConfig.Style style)throws IOException{
        if(style.values.size()>3000)throw new IOException("Too many style values");
        for(Map.Entry<String,String> e:style.values.entrySet()){
            String key=e.getKey(),value=e.getValue();
            if(key.contains("source.file"))throw new IOException("Source files are not portable");
            if(key.startsWith("light.")||key.startsWith("dark.")){try{Integer.parseInt(value);}catch(NumberFormatException ex){throw new IOException("Invalid color");}}
            else if(key.equals("mode")){if(!Arrays.asList("light","dark","auto").contains(value))throw new IOException("Invalid mode");}
            else if(!key.matches("(?:wallpaper|list|banner|bubble|section|palette|effects|launcher)\\.[A-Za-z0-9_.]+"))throw new IOException("Unsupported style field");
            if(value.length()>512)throw new IOException("Style value too long");
            if(key.endsWith(".file")&&!value.isEmpty()&&!safeName(value))throw new IOException("Invalid media reference");
        }
    }
    public static void write(OutputStream output,AppearanceConfig.Preset preset,Media media,boolean images,boolean videos)throws IOException{
        AppearanceConfig.Style style=preset.style.copy();
        for(String key:new ArrayList<>(style.values.keySet()))if(key.endsWith(".source.file"))style.values.remove(key);
        for(String domain:new String[]{"wallpaper","list.wallpaper","banner.wallpaper"}){
            String file=style.values.get(domain+".file"),cover=style.values.get(domain+".cover.file");
            if(file!=null&&safeName(file)&&video(file)&&!videos){if(images&&safeName(cover)){style.set(domain+".file",cover);style.set(domain+".type","image");}else style.values.remove(domain+".file");}
        }
        LinkedHashMap<String,File> assets=new LinkedHashMap<>();long total=0;
        for(String key:new ArrayList<>(style.values.keySet()))if(key.endsWith(".file")){
            String name=style.values.get(key);if(!safeName(name)||video(name)&&!videos||!video(name)&&!images){style.values.remove(key);continue;}
            File f=media.file(name);if(f==null||!f.isFile())throw new IOException("Referenced media unavailable");
            if(!assets.containsKey(name)){if(f.length()<=0||f.length()>MAX_ASSET||assets.size()>=MAX_ASSETS||(total+=f.length())>MAX_TOTAL)throw new IOException("Media export too large");assets.put(name,f);}
        }
        validateStyle(style);Properties manifest=new Properties();manifest.setProperty("format","TetogramAppearance");manifest.setProperty("schema","1");String title=preset.name==null?"":preset.name;if(title.length()>256)throw new IOException("Preset name too long");manifest.setProperty("name",title);
        for(Map.Entry<String,String> e:style.values.entrySet())manifest.setProperty("style."+e.getKey(),e.getValue());
        for(Map.Entry<String,File> e:assets.entrySet())manifest.setProperty("sha256."+e.getKey(),digest(e.getValue()));
        StringWriter text=new StringWriter();manifest.store(text,"Tetogram portable appearance v1");byte[] bytes=text.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_MANIFEST)throw new IOException("Manifest too large");
        try(ZipOutputStream zip=new ZipOutputStream(output)){zip.putNextEntry(new ZipEntry("appearance.properties"));zip.write(bytes);zip.closeEntry();for(Map.Entry<String,File> e:assets.entrySet()){zip.putNextEntry(new ZipEntry("media/"+e.getKey()));try(InputStream in=new FileInputStream(e.getValue())){copy(in,zip,MAX_ASSET);}zip.closeEntry();}}
    }
    public static Imported read(InputStream input,File staging)throws IOException{
        if(!staging.isDirectory()&&!staging.mkdirs())throw new IOException("Staging unavailable");
        ArrayList<File> created=new ArrayList<>();
        try(ZipInputStream zip=new ZipInputStream(input)){
            ZipEntry first=zip.getNextEntry();if(first==null||!first.getName().equals("appearance.properties")||first.isDirectory())throw new IOException("Missing manifest");
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();copy(zip,buffer,MAX_MANIFEST);Properties manifest=new Properties();manifest.load(new StringReader(buffer.toString("UTF-8")));zip.closeEntry();
            if(!"TetogramAppearance".equals(manifest.getProperty("format"))||!"1".equals(manifest.getProperty("schema")))throw new IOException("Unsupported format");
            AppearanceConfig.Preset p=new AppearanceConfig.Preset();p.name=manifest.getProperty("name","");if(p.name.length()>256)throw new IOException("Preset name too long");Map<String,String> expected=new LinkedHashMap<>();
            for(String key:manifest.stringPropertyNames()){
                if(key.startsWith("style."))p.style.set(key.substring(6),manifest.getProperty(key));
                else if(key.startsWith("sha256.")){String name=key.substring(7),hash=manifest.getProperty(key);if(!safeName(name)||!hash.matches("[a-f0-9]{64}"))throw new IOException("Invalid asset index");expected.put(name,hash);}
                else if(!Arrays.asList("format","schema","name").contains(key))throw new IOException("Unsupported metadata");
            }
            validateStyle(p.style);if(expected.size()>MAX_ASSETS)throw new IOException("Too many assets");
            Set<String> referenced=new HashSet<>();for(Map.Entry<String,String> e:p.style.values.entrySet())if(e.getKey().endsWith(".file")&&!e.getValue().isEmpty())referenced.add(e.getValue());if(!expected.keySet().equals(referenced))throw new IOException("Asset references differ");
            Map<String,String> renamed=new HashMap<>();long total=0;ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){String path=entry.getName();if(!path.startsWith("media/")||entry.isDirectory())throw new IOException("Unsafe archive path");String name=path.substring(6);if(!expected.containsKey(name)||renamed.containsKey(name)||!safeName(name))throw new IOException("Unexpected or duplicate asset");
                File target=new File(staging,UUID.randomUUID()+name.substring(36));if(!target.createNewFile())throw new IOException("Asset collision");created.add(target);
                long size;try(FileOutputStream out=new FileOutputStream(target)){size=copy(zip,out,Math.min(MAX_ASSET,MAX_TOTAL-total));out.getFD().sync();}if(size==0)throw new IOException("Empty asset");total+=size;if(!expected.get(name).equals(digest(target)))throw new IOException("Asset checksum differs");renamed.put(name,target.getName());zip.closeEntry();
            }
            if(!renamed.keySet().equals(expected.keySet()))throw new IOException("Missing media");
            for(String key:new ArrayList<>(p.style.values.keySet()))if(key.endsWith(".file")&&renamed.containsKey(p.style.values.get(key)))p.style.set(key,renamed.get(p.style.values.get(key)));
            return new Imported(p,created);
        }catch(Exception e){for(File f:created)f.delete();if(e instanceof IOException)throw(IOException)e;throw new IOException("Invalid appearance archive",e);}
    }
    private static long copy(InputStream in,OutputStream out,long limit)throws IOException{byte[] buffer=new byte[65536];long total=0;int n;while((n=in.read(buffer))!=-1){total+=n;if(total>limit)throw new IOException("Archive limit exceeded");out.write(buffer,0,n);}return total;}
    private static String digest(File file)throws IOException{try{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] bytes=new byte[65536];long total=0;int n;while((n=in.read(bytes))!=-1){total+=n;if(total>MAX_ASSET)throw new IOException("Asset too large");digest.update(bytes,0,n);}}StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}}
}
