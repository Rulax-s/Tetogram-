/* Tetogram, 2026. GNU GPL v2 or later. Chat translation with Tetogram's own engines (not Telegram Premium translation). */
package org.telegram.messenger.tetogram;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.LruCache;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

/**
 * Incoming messages of a chat the user turned on are shown translated into the app language; the user's own messages
 * can be translated into the chat language before sending, while this device keeps showing what the user typed.
 * The device engine (ML Kit) keeps text on the phone; online engines use the user's own API key.
 */
public final class ChatTranslation {
    private ChatTranslation(){}
    public static final String DEVICE="device",GOOGLE="google",DEEPL="deepl";
    public static final int MAX_TEXT=4000;
    public interface Result{void done(String text);}

    private static SharedPreferences prefs(){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_translate",Context.MODE_PRIVATE);}
    private static long owner(int a){return UserConfig.getInstance(a).getClientUserId();}
    private static Set<Long> ids(String csv){HashSet<Long> out=new HashSet<>();if(csv!=null)for(String p:csv.split(","))if(!p.isEmpty())try{out.add(Long.parseLong(p));}catch(NumberFormatException ignored){}return out;}
    private static String csv(Set<Long> ids){StringBuilder b=new StringBuilder();for(long id:ids){if(b.length()>0)b.append(',');b.append(id);}return b.toString();}

    // ---- settings ----
    public static String engine(){String e=prefs().getString("engine",DEVICE);return GOOGLE.equals(e)||DEEPL.equals(e)?e:DEVICE;}
    public static void setEngine(String e){prefs().edit().putString("engine",e).apply();}
    public static String key(String engine){return prefs().getString("key."+engine,"");}
    public static void setKey(String engine,String key){prefs().edit().putString("key."+engine,key==null?"":key.trim()).apply();}
    /** Target language: chosen in settings, else the app language. */
    public static String target(){String t=prefs().getString("target","");if(!t.isEmpty())return t;return appLanguage();}
    public static void setTarget(String lang){prefs().edit().putString("target",lang==null?"":lang).apply();}
    public static String appLanguage(){try{Locale l=LocaleController.getInstance().getCurrentLocale();String lang=l==null?"en":l.getLanguage();return TextUtils.isEmpty(lang)?"en":lang;}catch(Exception e){return "en";}}
    public static boolean translateOutgoing(){return prefs().getBoolean("outgoing",true);}
    public static void setTranslateOutgoing(boolean on){prefs().edit().putBoolean("outgoing",on).apply();}

    private static String chatsCsv;private static Set<Long> chatsParsed=new HashSet<>();
    /** Parsed once per change: {@link #display} runs for every message bind. */
    private static synchronized Set<Long> chatsCached(int a){String csv=prefs().getString("u"+owner(a)+".chats","");if(!csv.equals(chatsCsv)){chatsCsv=csv;chatsParsed=ids(csv);}return chatsParsed;}
    public static Set<Long> chats(int a){return new HashSet<>(chatsCached(a));}
    public static boolean active(int a,long dialog){return dialog!=0&&chatsCached(a).contains(dialog);}
    public static void setActive(int a,long dialog,boolean on){Set<Long> s=chats(a);if(on)s.add(dialog);else s.remove(dialog);prefs().edit().putString("u"+owner(a)+".chats",csv(s)).apply();}
    /** The language the chat is written in: forced by the user, else detected from incoming messages. */
    public static String chatLanguage(int a,long dialog){String forced=prefs().getString("u"+owner(a)+".lang."+dialog,"");if(!forced.isEmpty())return forced;return detectedLanguage(dialog);}
    public static void setChatLanguage(int a,long dialog,String lang){prefs().edit().putString("u"+owner(a)+".lang."+dialog,lang==null?"":lang).apply();}

    // ---- detection of the chat language (session only) ----
    private static final HashMap<Long,HashMap<String,Integer>> VOTES=new HashMap<>();
    private static final HashMap<Long,HashSet<Integer>> SAMPLED=new HashMap<>();
    private static final HashSet<Long> SUGGESTED=new HashSet<>();
    public static String detectedLanguage(long dialog){return ChatTranslationRules.winner(VOTES.get(dialog));}
    private static void sample(long dialog,MessageObject m,String text){
        HashSet<Integer> seen=SAMPLED.get(dialog);if(seen==null){seen=new HashSet<>();SAMPLED.put(dialog,seen);}
        if(seen.size()>=12||!seen.add(m.getId())||text.length()<12)return;
        LanguageDetector.detectLanguage(text,lang->AndroidUtilities.runOnUIThread(()->{if(lang==null||"und".equals(lang))return;HashMap<String,Integer> v=VOTES.get(dialog);if(v==null){v=new HashMap<>();VOTES.put(dialog,v);}Integer n=v.get(lang);v.put(lang,n==null?1:n+1);}),e->{});
    }
    /** A chat in another language that is not translated yet; suggested once per app session. */
    public static String suggestion(int a,long dialog){if(active(a,dialog)||SUGGESTED.contains(dialog))return null;String lang=detectedLanguage(dialog);if(lang==null||sameLanguage(lang,target()))return null;return lang;}
    public static void suggested(long dialog){SUGGESTED.add(dialog);}
    static boolean sameLanguage(String a,String b){return ChatTranslationRules.sameLanguage(a,b);}

    // ---- incoming ----
    private static final LruCache<String,String> CACHE=new LruCache<>(2000);
    private static final HashSet<String> PENDING=new HashSet<>();
    private static final HashMap<String,Long> FAILED=new HashMap<>();
    // Finished translations survive restarts: a small file in the app's private storage, newest 3000 kept.
    private static final int MAX_STORED=3000;
    private static LinkedHashMap<String,String> stored;
    private static boolean saveScheduled;
    private static java.io.File storedFile(){return new java.io.File(ApplicationLoader.getFilesDirFixed(),"tetogram_translations.json");}
    private static synchronized LinkedHashMap<String,String> stored(){
        if(stored!=null)return stored;stored=new LinkedHashMap<>(256,0.75f,true);
        java.io.File f=storedFile();if(!f.exists())return stored;
        try(InputStream in=new java.io.FileInputStream(f)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)b.write(buf,0,n);
            JSONObject o=new JSONObject(b.toString("UTF-8"));JSONArray keys=o.names();if(keys!=null)for(int i=0;i<keys.length();i++){String k=keys.getString(i);stored.put(k,o.getString(k));}}
        catch(Exception e){FileLog.e("Tetogram translation cache unreadable, starting empty");}
        return stored;
    }
    private static synchronized String storedGet(String key){return stored().get(key);}
    private static synchronized void storedPut(String key,String value){
        LinkedHashMap<String,String> map=stored();map.put(key,value);
        while(map.size()>MAX_STORED){String first=map.keySet().iterator().next();map.remove(first);}
        if(saveScheduled)return;saveScheduled=true;
        Utilities.globalQueue.postRunnable(ChatTranslation::save,3000);
    }
    private static void save(){
        String json;synchronized(ChatTranslation.class){saveScheduled=false;try{JSONObject o=new JSONObject();for(Map.Entry<String,String> e:stored().entrySet())o.put(e.getKey(),e.getValue());json=o.toString();}catch(Exception e){return;}}
        java.io.File f=storedFile(),tmp=new java.io.File(f.getPath()+".tmp");
        try(OutputStream out=new java.io.FileOutputStream(tmp)){out.write(json.getBytes(StandardCharsets.UTF_8));}catch(Exception e){FileLog.e("Tetogram translation cache not saved");return;}
        if(!tmp.renameTo(f))tmp.delete();
    }
    /** Removes stored translations (settings → clear). */
    public static synchronized void clearStored(){stored=new LinkedHashMap<>(256,0.75f,true);CACHE.evictAll();storedFile().delete();}
    public static synchronized int storedCount(){return stored().size();}
    private static String text(MessageObject m){if(m==null||!(m.messageOwner instanceof TLRPC.TL_message))return null;String t=m.getDisplayText();return t==null||t.trim().isEmpty()||t.length()>MAX_TEXT?null:t;}
    private static String cacheKey(MessageObject m,String text,String to){return m.getDialogId()+":"+m.getId()+":"+to+":"+text.hashCode()+":"+text.length();}
    /** Called for messages on or near the screen; translates incoming text of active chats and samples the language. */
    public static void check(int a,MessageObject m,boolean near){
        if(!near||m==null)return;String text=text(m);if(text==null)return;long dialog=m.getDialogId();
        if(!m.isOutOwner())sample(dialog,m,text);
        if(m.isOutOwner()||!active(a,dialog))return;
        String to=target(),key=cacheKey(m,text,to);
        if(CACHE.get(key)==null){String saved=storedGet(key);if(saved!=null){CACHE.put(key,saved);NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.messageTranslated,m);return;}}
        if(CACHE.get(key)!=null||!ChatTranslationRules.retry(FAILED.get(key),System.currentTimeMillis())||!PENDING.add(key))return;
        LanguageDetector.detectLanguage(text,from->AndroidUtilities.runOnUIThread(()->{
            if(from==null||"und".equals(from)||sameLanguage(from,to)){PENDING.remove(key);CACHE.put(key,"");return;}
            translate(text,from,to,out->{PENDING.remove(key);if(out==null){FAILED.put(key,System.currentTimeMillis());return;}FAILED.remove(key);CACHE.put(key,out.trim().isEmpty()||out.equals(text)?"":out);if(!out.trim().isEmpty()&&!out.equals(text))storedPut(key,out);if(!out.trim().isEmpty()&&!out.equals(text))NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.messageTranslated,m);});
        }),e->AndroidUtilities.runOnUIThread(()->PENDING.remove(key)));
    }
    /** What this device shows instead of the message text, or null for the original. */
    public static CharSequence display(MessageObject m){
        String text=text(m);if(text==null)return null;int a=m.currentAccount;long dialog=m.getDialogId();
        if(m.isOutOwner()){String original=original(a,dialog,text);return original==null||original.equals(text)?null:original;}
        if(!active(a,dialog))return null;
        String key=cacheKey(m,text,target());String out=CACHE.get(key);
        if(out==null){out=storedGet(key);if(out!=null)CACHE.put(key,out);}
        return TextUtils.isEmpty(out)?null:out;
    }

    // ---- outgoing: what the user typed, kept for their own sent translations ----
    private static final int MAX_SENT=1000;
    private static LinkedHashMap<String,String> sentCache;private static long sentOwner;
    private static String sentKey(long dialog,String sent){return ChatTranslationRules.sentKey(dialog,sent);}
    private static synchronized LinkedHashMap<String,String> sent(int a){
        long uid=owner(a);if(sentCache!=null&&sentOwner==uid)return sentCache;sentOwner=uid;sentCache=new LinkedHashMap<>();
        try{JSONObject o=new JSONObject(prefs().getString("u"+uid+".sent","{}"));JSONArray keys=o.names();if(keys!=null)for(int i=0;i<keys.length();i++){String k=keys.getString(i);sentCache.put(k,o.getString(k));}}catch(Exception e){FileLog.e("Tetogram sent translations unreadable");}
        return sentCache;
    }
    public static synchronized void rememberSent(int a,long dialog,String sent,String original){
        LinkedHashMap<String,String> map=sent(a);map.put(sentKey(dialog,sent),original);
        while(map.size()>MAX_SENT){String first=map.keySet().iterator().next();map.remove(first);}
        try{JSONObject o=new JSONObject();for(Map.Entry<String,String> e:map.entrySet())o.put(e.getKey(),e.getValue());prefs().edit().putString("u"+owner(a)+".sent",o.toString()).apply();}catch(Exception e){FileLog.e("Tetogram sent translations not saved");}
    }
    public static synchronized String original(int a,long dialog,String sent){return sent(a).get(sentKey(dialog,sent));}
    /** The chat language the user's text should be translated into before sending, or null when it is not needed. */
    public static String outgoingTarget(int a,long dialog,CharSequence text){
        if(!active(a,dialog)||!translateOutgoing()||text==null||text.length()>MAX_TEXT||text.toString().trim().isEmpty())return null;
        String to=chatLanguage(a,dialog);return to==null||sameLanguage(to,target())?null:to;
    }
    public static String forcedLanguage(int a,long dialog){String f=prefs().getString("u"+owner(a)+".lang."+dialog,"");return f.isEmpty()?null:f;}
    /** Translates the user's text into {@code to}; {@code same} runs when the text is already in that language. */
    public static void prepareOutgoing(String text,String to,Result translated,Runnable same){
        LanguageDetector.detectLanguage(text,from->AndroidUtilities.runOnUIThread(()->{String src=from==null||"und".equals(from)?target():from;if(sameLanguage(src,to))same.run();else translate(text,src,to,translated);}),e->AndroidUtilities.runOnUIThread(()->translate(text,target(),to,translated)));
    }
    /** Languages the device engine can translate between. */
    public static java.util.List<String> languages(){try{return com.google.mlkit.nl.translate.TranslateLanguage.getAllLanguages();}catch(Throwable t){return java.util.Arrays.asList("en","ru","uk","de","fr","es","it","pt","pl","tr","zh","ja","ko","ar");}}
    public static String languageName(String code){if(code==null)return "?";Locale l=Locale.forLanguageTag(code);String name=l.getDisplayLanguage(new Locale(appLanguage()));return TextUtils.isEmpty(name)?code:name.substring(0,1).toUpperCase(Locale.ROOT)+name.substring(1);}

    // ---- engines ----
    /** Result is delivered on the UI thread; null means the engine could not translate. */
    public static void translate(String text,String from,String to,Result done){
        String engine=engine();
        if(DEVICE.equals(engine)){device(text,from,to,done);return;}
        String key=key(engine);if(key.isEmpty()){done.done(null);return;}
        Utilities.globalQueue.postRunnable(()->{String out=null;try{out=GOOGLE.equals(engine)?google(key,text,from,to):deepl(key,text,from,to);}catch(Exception e){FileLog.e("Tetogram online translation failed");}final String result=out;AndroidUtilities.runOnUIThread(()->done.done(result));});
    }
    private static final HashMap<String,com.google.mlkit.nl.translate.Translator> TRANSLATORS=new HashMap<>();
    private static void device(String text,String from,String to,Result done){
        try{
            String s=com.google.mlkit.nl.translate.TranslateLanguage.fromLanguageTag(from),t=com.google.mlkit.nl.translate.TranslateLanguage.fromLanguageTag(to);
            if(s==null||t==null||s.equals(t)){done.done(null);return;}
            String pair=s+">"+t;com.google.mlkit.nl.translate.Translator translator=TRANSLATORS.get(pair);
            if(translator==null){translator=com.google.mlkit.nl.translate.Translation.getClient(new com.google.mlkit.nl.translate.TranslatorOptions.Builder().setSourceLanguage(s).setTargetLanguage(t).build());TRANSLATORS.put(pair,translator);}
            final com.google.mlkit.nl.translate.Translator client=translator;
            client.downloadModelIfNeeded().addOnSuccessListener(v->client.translate(text).addOnSuccessListener(done::done).addOnFailureListener(e->done.done(null))).addOnFailureListener(e->done.done(null));
        }catch(Exception e){FileLog.e("Tetogram device translation unavailable");done.done(null);}
    }
    private static String post(String url,String body,String auth)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        try{c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setDoOutput(true);c.setRequestMethod("POST");c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=utf-8");if(auth!=null)c.setRequestProperty("Authorization",auth);
            try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}
            if(c.getResponseCode()/100!=2)throw new IllegalStateException("HTTP "+c.getResponseCode());
            try(InputStream in=c.getInputStream()){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0){b.write(buf,0,n);if(b.size()>1<<20)break;}return b.toString("UTF-8");}
        }finally{c.disconnect();}
    }
    private static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private static String google(String key,String text,String from,String to)throws Exception{
        String json=post("https://translation.googleapis.com/language/translate/v2?key="+enc(key),"q="+enc(text)+"&target="+enc(to)+"&format=text"+(from==null?"":"&source="+enc(from)),null);
        return new JSONObject(json).getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
    }
    private static String deepl(String key,String text,String from,String to)throws Exception{
        String host=key.endsWith(":fx")?"https://api-free.deepl.com":"https://api.deepl.com";String target=to.toUpperCase(Locale.ROOT);if(target.equals("EN"))target="EN-US";if(target.equals("PT"))target="PT-BR";
        String json=post(host+"/v2/translate","text="+enc(text)+"&target_lang="+enc(target)+(from==null?"":"&source_lang="+enc(from.split("[-_]")[0].toUpperCase(Locale.ROOT))),"DeepL-Auth-Key "+key);
        return new JSONObject(json).getJSONArray("translations").getJSONObject(0).getString("text");
    }
}
