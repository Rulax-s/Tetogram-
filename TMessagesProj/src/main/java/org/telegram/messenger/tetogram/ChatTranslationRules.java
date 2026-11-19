/* Tetogram, 2026. GNU GPL v2 or later. Android-independent rules of chat translation. */
package org.telegram.messenger.tetogram;

import java.util.Map;

public final class ChatTranslationRules {
    private ChatTranslationRules(){}
    public static final int MIN_VOTES=3;
    /** "pt-BR" and "pt" are the same language for translation purposes. */
    public static boolean sameLanguage(String a,String b){if(a==null||b==null||a.isEmpty()||b.isEmpty())return false;return a.split("[-_]")[0].equalsIgnoreCase(b.split("[-_]")[0]);}
    /** The chat language: at least {@link #MIN_VOTES} detected messages and a strict majority, else null. */
    public static String winner(Map<String,Integer> votes){
        if(votes==null)return null;String best=null;int max=0,total=0;
        for(Map.Entry<String,Integer> e:votes.entrySet()){total+=e.getValue();if(e.getValue()>max){max=e.getValue();best=e.getKey();}}
        return total>=MIN_VOTES&&max*2>total?best:null;
    }
    /** Key of a sent translation; whitespace the server may trim does not matter. */
    public static String sentKey(long dialog,String sent){String t=sent==null?"":sent.trim();return dialog+":"+t.hashCode()+":"+t.length();}
    /** Whether a failed translation may be tried again. */
    public static boolean retry(Long failedAt,long now){return failedAt==null||now-failedAt>=60_000L;}
}
