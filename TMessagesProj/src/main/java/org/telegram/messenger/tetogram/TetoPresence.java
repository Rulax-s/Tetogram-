/* Tetogram, 2026. GNU GPL v2 or later. "Online in Tetogram right now", through the owner's own presence server. */
package org.telegram.messenger.tetogram;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

/**
 * While Tetogram is open on screen it tells the presence server "I'm here" every two minutes. Other Tetogram users see
 * the mark only while that signal is fresh and only if the person has "Show others" on. The server keeps nothing but
 * fresh heartbeats (Telegram ID → time) in memory; messages and contacts never leave the phone.
 */
public final class TetoPresence {
    private TetoPresence(){}
    /** Filled in once the owner deploys the server (see server/presence/README.md); can be changed in settings. */
    public static final String DEFAULT_SERVER="";
    public static final long BEAT_MS=120_000L,STATUS_TTL_MS=60_000L;

    private static final DispatchQueue QUEUE=new DispatchQueue("tetogramPresence");
    private static SharedPreferences prefs(){return ApplicationLoader.applicationContext.getSharedPreferences("tetogram_presence",Context.MODE_PRIVATE);}
    private static long uid(int a){return UserConfig.getInstance(a).getClientUserId();}

    public static String server(){String s=prefs().getString("server",DEFAULT_SERVER).trim();while(s.endsWith("/"))s=s.substring(0,s.length()-1);return s;}
    public static void setServer(String url){prefs().edit().putString("server",url==null?"":url.trim()).apply();CHECKED.clear();ONLINE.clear();beatNow();}
    public static boolean configured(){return server().startsWith("https://");}
    /** Active once a server address is set; there is no account confirmation (a deliberate simplification). */
    public static boolean connected(int a){return configured();}
    /** "Show others that I'm in Tetogram" — on by default. */
    public static boolean visible(int a){return prefs().getBoolean("visible."+uid(a),true);}
    public static void setVisible(int a,boolean on){prefs().edit().putBoolean("visible."+uid(a),on).apply();if(on)beatNow();else QUEUE.postRunnable(()->call("/beat",body(a,false)));}

    // ---- heartbeat while the app is on screen ----
    private static boolean foreground;
    private static final Runnable BEAT=new Runnable(){@Override public void run(){if(!foreground)return;for(int a=0;a<UserConfig.MAX_ACCOUNT_COUNT;a++){if(!UserConfig.getInstance(a).isClientActivated()||!connected(a)||!visible(a))continue;final int account=a;QUEUE.postRunnable(()->call("/beat",body(account,true)));}AndroidUtilities.runOnUIThread(this,BEAT_MS);}};
    public static void onForeground(){foreground=true;beatNow();}
    public static void onBackground(){foreground=false;AndroidUtilities.cancelRunOnUIThread(BEAT);}
    private static void beatNow(){AndroidUtilities.cancelRunOnUIThread(BEAT);if(foreground)AndroidUtilities.runOnUIThread(BEAT);}
    private static String body(int a,boolean visible){try{return new JSONObject().put("uid",uid(a)).put("visible",visible).toString();}catch(Exception e){return "{}";}}
    private static void call(String path,String body){try{post(server()+path,body);}catch(Exception e){FileLog.e("Tetogram presence beat failed");}}

    // ---- who is in Tetogram right now ----
    private static final HashMap<Long,Long> CHECKED=new HashMap<>();
    private static final HashSet<Long> ONLINE=new HashSet<>(),PENDING=new HashSet<>();
    /**
     * Cached answer for a person Telegram already shows as online; asks the server in the background when the answer
     * is older than a minute and refreshes the screen when it changes.
     */
    public static boolean online(int a,long user){
        if(user<=0||user==uid(a)||!connected(a))return false;
        Long at=CHECKED.get(user);long now=System.currentTimeMillis();
        if((at==null||now-at>STATUS_TTL_MS)&&PENDING.add(user)){
            QUEUE.postRunnable(()->{
                Boolean on=null;
                try{JSONObject r=new JSONObject(post(server()+"/status",new JSONObject().put("ids",new JSONArray().put(user)).toString()));JSONArray ids=r.optJSONArray("online");on=false;if(ids!=null)for(int i=0;i<ids.length();i++)if(ids.getLong(i)==user)on=true;}
                catch(Exception e){FileLog.e("Tetogram presence status failed");}
                final Boolean result=on;
                AndroidUtilities.runOnUIThread(()->{PENDING.remove(user);CHECKED.put(user,System.currentTimeMillis());if(result==null)return;
                    boolean changed=result?ONLINE.add(user):ONLINE.remove(user);
                    if(changed)NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces,MessagesController.UPDATE_MASK_STATUS);});
            });
        }
        return ONLINE.contains(user);
    }

    private static String post(String url,String json)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        try{c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setDoOutput(true);c.setRequestMethod("POST");c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("X-Tetogram","1");
            try(OutputStream o=c.getOutputStream()){o.write(json.getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode();if(code/100!=2)throw new IllegalStateException("HTTP "+code);
            try(InputStream in=c.getInputStream()){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0){b.write(buf,0,n);if(b.size()>65536)break;}return b.toString("UTF-8");}
        }finally{c.disconnect();}
    }
}
