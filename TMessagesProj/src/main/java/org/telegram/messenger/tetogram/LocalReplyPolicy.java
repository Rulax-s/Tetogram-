/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.messenger.tetogram;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public final class LocalReplyPolicy {
    private LocalReplyPolicy() {}
    public static String prefix(String text,int start) {
        if(text==null || start<=0 || start>=text.length() || start<text.length() && Character.isLowSurrogate(text.charAt(start)))return "";
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.substring(0,start).getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    public static boolean valid(String text,int start,String hash) { return hash!=null && !hash.isEmpty() && hash.equals(prefix(text,start)); }
}
