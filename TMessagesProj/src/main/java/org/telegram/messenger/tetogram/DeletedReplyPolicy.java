/* Tetogram, 2026. GNU GPL v2 or later. Inspired by AyuGram c9c317cf (GPL).
 * Independent adaptation: UTF-16 entity offsets, Unicode clusters, no mutable input. */
package org.telegram.messenger.tetogram;

public final class DeletedReplyPolicy {
    private DeletedReplyPolicy() {}
    public static final class Result {
        public final String text;
        public final int entityShift, authorLength, quoteOffset, quoteLength;
        Result(String text,int shift,int authorLength,int quoteOffset,int quoteLength) {
            this.text=text;entityShift=shift;this.authorLength=authorLength;
            this.quoteOffset=quoteOffset;this.quoteLength=quoteLength;
        }
    }
    public static Result prepare(String author,String source,String body,boolean group) {
        String name=group ? trimWhitespace(author.replace('\n',' ').replace('\r',' ')) : "";
        String quote=trimWhitespace(shortify(source==null ? "" : source.replace("\r\n","\n").replace('\r','\n'),20));
        if(quote.isEmpty())quote="…"; // a valid visible range, including whitespace-only media captions
        String authorLine=name.isEmpty() ? "" : name+"\n";
        String prefix=authorLine+quote;
        boolean hasBody=body!=null && !body.isEmpty();
        return new Result(hasBody ? prefix+"\n\n"+body : prefix,hasBody ? prefix.length()+2 : 0,
                name.length(),authorLine.length(),quote.length());
    }
    private static String trimWhitespace(String value) {
        int start=0,end=value.length();
        while(start<end) {
            int cp=value.codePointAt(start);
            if(!Character.isWhitespace(cp) && !Character.isSpaceChar(cp))break;
            start+=Character.charCount(cp);
        }
        while(end>start) {
            int cp=value.codePointBefore(end);
            if(!Character.isWhitespace(cp) && !Character.isSpaceChar(cp))break;
            end-=Character.charCount(cp);
        }
        return value.substring(start,end);
    }
    /** Extended emoji/combining clusters; never split a UTF-16 pair, ZWJ chain or flag. */
    public static String shortify(String text,int limit) {
        if(text==null)return "";
        int at=0,kept=0,count=0;
        while(at<text.length()) {
            int first=text.codePointAt(at);at+=Character.charCount(first);
            if(first>=0x1f1e6 && first<=0x1f1ff && at<text.length()) {
                int next=text.codePointAt(at);if(next>=0x1f1e6 && next<=0x1f1ff)at+=Character.charCount(next);
            }
            while(at<text.length()) {
                int cp=text.codePointAt(at),type=Character.getType(cp);
                if(type==Character.NON_SPACING_MARK || type==Character.COMBINING_SPACING_MARK || type==Character.ENCLOSING_MARK
                        || cp==0xfe0e || cp==0xfe0f || cp>=0x1f3fb && cp<=0x1f3ff || cp>=0xe0020 && cp<=0xe007f) at+=Character.charCount(cp);
                else if(cp==0x200d && at+1<text.length()) {at++;at+=Character.charCount(text.codePointAt(at));}
                else break;
            }
            count++;if(count<limit)kept=at;
            if(count>limit)return text.substring(0,kept)+"…";
        }
        return text;
    }
    public interface Entity<E> {
        E copy(E value);
        int offset(E value);
        void offset(E value,int offset);
    }
    public static <E> java.util.ArrayList<E> shiftEntities(java.util.List<E> entities,int shift,Entity<E> access) {
        if(entities==null)return null;
        java.util.ArrayList<E> result=new java.util.ArrayList<>();
        for(E value:entities) {
            E copy=access.copy(value);
            access.offset(copy,access.offset(value)+shift);
            result.add(copy);
        }
        return result;
    }
    public static boolean shouldConvert(boolean deleted,boolean retry) { return deleted && !retry; }
    public static boolean sameOwner(long owner,long currentUser) { return owner!=0 && owner==currentUser; }
    public static boolean sameContext(int account,long dialog,int sourceAccount,long sourceDialog,boolean ephemeral,boolean secret) {
        return account==sourceAccount && dialog==sourceDialog && !ephemeral && !secret;
    }
}
