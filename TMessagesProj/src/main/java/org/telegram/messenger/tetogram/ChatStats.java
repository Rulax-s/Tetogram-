/* Tetogram, 2026. GNU GPL v2 or later. Read-only conversation statistics; no Android or network types. */
package org.telegram.messenger.tetogram;

import java.util.*;

/**
 * Accumulates messages in any order and derives conversation statistics.
 * Pure Java so it can be tested on the JVM; the caller maps Telegram messages to primitives.
 */
public final class ChatStats {
    public static final int MEDIA_PHOTO=0,MEDIA_VIDEO=1,MEDIA_VOICE=2,MEDIA_ROUND=3,MEDIA_STICKER=4,MEDIA_GIF=5,MEDIA_FILE=6,MEDIA_MUSIC=7,MEDIA_LINK=8,MEDIA_POLL=9,MEDIA_LOCATION=10,MEDIA_OTHER=11,MEDIA_COUNT=12,MEDIA_NONE=-1;
    /** A new conversation starts after this silence. */
    public static final int CONVERSATION_GAP=6*3600;
    /** Replies slower than this are not counted as a response. */
    public static final int RESPONSE_WINDOW=12*3600;
    private static final int DAY=86400;

    public static final class Sender {
        public final long id;
        public int messages,words,media,voiceSeconds,starts,longest,questions,laughs;
        public long chars;
        /** Median seconds to answer the other side, -1 when unknown. */
        public int medianResponse=-1;
        final IntList responses=new IntList();
        Sender(long id){this.id=id;}
    }
    public static final class Entry {
        public final String key;public final int count;
        Entry(String key,int count){this.key=key;this.count=count;}
        @Override public String toString(){return key+"="+count;}
    }
    public static final class Result {
        public int total,activeDays,longestStreak,currentStreak,busiestDayCount,questions,laughs,night,forwarded;
        public long chars;
        /** Local day number (days since epoch in the chosen time zone), -1 when empty. */
        public long busiestDay=-1,firstDay=-1,lastDay=-1,streakStartDay=-1;
        public int firstDate,lastDate,peakHour=-1,peakWeekday=-1;
        public int longestMessage;public long longestMessageSender;
        public final int[] hours=new int[24],weekdays=new int[7],media=new int[MEDIA_COUNT];
        public final int[][] heat=new int[7][24];
        /** Consecutive months starting at {@link #firstMonth} (year*12+month0). */
        public int[] months=new int[0];public int firstMonth;
        public int voiceSeconds;
        public final ArrayList<Sender> senders=new ArrayList<>();
        public final ArrayList<Entry> words=new ArrayList<>(),emoji=new ArrayList<>();
        public double perActiveDay(){return activeDays==0?0:(double)total/activeDays;}
        public int heatMax(){int max=0;for(int[] row:heat)for(int v:row)max=Math.max(max,v);return max;}
    }

    private final TimeZone zone;
    private final LinkedHashMap<Long,Sender> senders=new LinkedHashMap<>();
    private final HashMap<String,int[]> words=new HashMap<>(),emoji=new HashMap<>();
    private final IntList dates=new IntList();
    private final ArrayList<Sender> order=new ArrayList<>();
    private final Result r=new Result();

    public ChatStats(TimeZone zone){this.zone=zone==null?TimeZone.getDefault():zone;}

    public int size(){return r.total;}

    /**
     * @param sender peer id of the author
     * @param date unix seconds (UTC)
     * @param text message text or caption, may be null
     * @param media one of MEDIA_* or MEDIA_NONE
     * @param duration seconds for voice/round/video, otherwise 0
     */
    public void add(long sender,int date,String text,int media,int duration,boolean forwarded){
        if(date<=0)return;
        Sender s=senders.get(sender);
        if(s==null){s=new Sender(sender);senders.put(sender,s);}
        s.messages++;r.total++;
        if(forwarded)r.forwarded++;
        if(media>=0&&media<MEDIA_COUNT){r.media[media]++;s.media++;}
        if((media==MEDIA_VOICE||media==MEDIA_ROUND)&&duration>0){s.voiceSeconds+=duration;r.voiceSeconds+=duration;}
        long local=date+zone.getOffset(date*1000L)/1000L;
        int hour=(int)fmod(local,DAY)/3600;
        long day=fdiv(local,DAY);
        int weekday=(int)fmod(day+3,7);// 1970-01-01 was Thursday; 0 = Monday
        r.hours[hour]++;r.weekdays[weekday]++;r.heat[weekday][hour]++;
        if(hour<5)r.night++;
        if(r.firstDate==0||date<r.firstDate)r.firstDate=date;
        if(date>r.lastDate)r.lastDate=date;
        dates.add(date);order.add(s);
        if(text!=null&&!text.isEmpty()&&!forwarded)text(s,text);
    }

    private void text(Sender s,String text){
        int length=text.codePointCount(0,text.length());
        s.chars+=length;r.chars+=length;
        if(length>s.longest)s.longest=length;
        if(length>r.longestMessage){r.longestMessage=length;r.longestMessageSender=s.id;}
        if(text.indexOf('?')>=0){s.questions++;r.questions++;}
        boolean laughed=false;
        StringBuilder word=new StringBuilder();
        int i=0,n=text.length();
        while(i<=n){
            int cp=i<n?text.codePointAt(i):' ';
            int width=i<n?Character.charCount(cp):1;
            if(Character.isLetterOrDigit(cp)||(cp=='\''||cp=='’'||cp=='-')&&word.length()>0&&i+width<n&&Character.isLetter(text.codePointAt(i+width))){word.appendCodePoint(cp);i+=width;continue;}
            if(word.length()>0){
                String w=word.toString().toLowerCase(Locale.ROOT).replace('ё','е');word.setLength(0);
                s.words++;
                if(isLaugh(w))laughed=true;
                else if(countable(w))bump(words,w);
            }
            if(i<n&&isEmojiStart(cp)){int end=emojiEnd(text,i);bump(emoji,text.substring(i,end));i=end;continue;}
            i+=width;
        }
        if(laughed){s.laughs++;r.laughs++;}
    }

    private static void bump(HashMap<String,int[]> map,String key){int[] c=map.get(key);if(c==null)map.put(key,new int[]{1});else c[0]++;}

    public Result finish(long nowSeconds){
        Result out=r;
        out.senders.clear();out.senders.addAll(senders.values());
        Collections.sort(out.senders,(a,b)->a.messages!=b.messages?Integer.compare(b.messages,a.messages):Long.compare(a.id,b.id));
        out.words.clear();out.words.addAll(top(words,30));
        out.emoji.clear();out.emoji.addAll(top(emoji,20));
        out.peakHour=argmax(out.hours);out.peakWeekday=argmax(out.weekdays);
        if(out.total==0)return out;
        // chronological pass: days, streaks, months, initiators, response time
        int n=dates.size();Integer[] idx=new Integer[n];for(int i=0;i<n;i++)idx[i]=i;
        Arrays.sort(idx,(a,b)->dates.get(a)!=dates.get(b)?Integer.compare(dates.get(a),dates.get(b)):Integer.compare(b,a));
        for(Sender s:senders.values()){s.starts=0;s.responses.clear();}
        long prevDay=Long.MIN_VALUE,runStart=-1;int run=0,dayCount=0;
        int firstMonth=month(dates.get(idx[0])),lastMonth=month(dates.get(idx[n-1]));
        out.firstMonth=firstMonth;out.months=new int[lastMonth-firstMonth+1];
        out.activeDays=0;out.longestStreak=0;out.busiestDayCount=0;
        int prevDate=0;Sender prev=null;
        for(int k=0;k<n;k++){
            int date=dates.get(idx[k]);Sender s=order.get(idx[k]);
            long day=localDay(date);
            out.months[month(date)-firstMonth]++;
            if(day!=prevDay){
                if(prevDay!=Long.MIN_VALUE&&dayCount>out.busiestDayCount){out.busiestDayCount=dayCount;out.busiestDay=prevDay;}
                dayCount=0;out.activeDays++;
                if(day==prevDay+1)run++;else{run=1;runStart=day;}
                if(run>out.longestStreak){out.longestStreak=run;out.streakStartDay=runStart;}
                prevDay=day;
            }
            dayCount++;
            if(prev==null||date-prevDate>CONVERSATION_GAP)s.starts++;
            else if(prev!=s&&date-prevDate<=RESPONSE_WINDOW)s.responses.add(date-prevDate);
            prev=s;prevDate=date;
        }
        if(dayCount>out.busiestDayCount){out.busiestDayCount=dayCount;out.busiestDay=prevDay;}
        out.firstDay=localDay(out.firstDate);out.lastDay=prevDay;
        long today=localDay((int)Math.min(Integer.MAX_VALUE,Math.max(0,nowSeconds)));
        out.currentStreak=nowSeconds>0&&(prevDay==today||prevDay==today-1)?run:0;
        for(Sender s:senders.values())s.medianResponse=s.responses.median();
        return out;
    }

    public long localDay(int date){return fdiv(date+zone.getOffset(date*1000L)/1000L,DAY);}
    private int month(int date){Calendar c=Calendar.getInstance(zone);c.setTimeInMillis(date*1000L);return c.get(Calendar.YEAR)*12+c.get(Calendar.MONTH);}
    // Explicit helpers: Math.floorMod/floorDiv overloads differ across Android API levels.
    static long fdiv(long a,long b){long q=a/b;if((a%b!=0)&&((a<0)!=(b<0)))q--;return q;}
    static long fmod(long a,long b){return a-fdiv(a,b)*b;}
    private static int argmax(int[] v){int best=-1,max=0;for(int i=0;i<v.length;i++)if(v[i]>max){max=v[i];best=i;}return best;}
    private static List<Entry> top(HashMap<String,int[]> map,int limit){
        ArrayList<Entry> all=new ArrayList<>(map.size());
        for(Map.Entry<String,int[]> e:map.entrySet())if(e.getValue()[0]>1||map.size()<limit)all.add(new Entry(e.getKey(),e.getValue()[0]));
        Collections.sort(all,(a,b)->a.count!=b.count?Integer.compare(b.count,a.count):a.key.compareTo(b.key));
        return all.size()>limit?new ArrayList<>(all.subList(0,limit)):all;
    }

    static boolean countable(String w){
        int len=w.codePointCount(0,w.length());
        if(len<3||len>32||STOP.contains(w))return false;
        for(int i=0;i<w.length();i++)if(!Character.isDigit(w.charAt(i)))return !w.startsWith("http");
        return false;
    }
    /** ахаха, хахах, ахпхах, haha, lol, lmao, ржу, хд */
    static boolean isLaugh(String w){
        if(w.equals("lol")||w.equals("lmao")||w.equals("rofl")||w.equals("ржу")||w.equals("ржака")||w.equals("хд")||w.equals("xd")||w.equals("кек"))return true;
        if(w.length()<4)return false;
        boolean ru=true,en=true;int x=0,a=0,h=0,ea=0;
        for(int i=0;i<w.length();i++){char c=w.charAt(i);
            if(c=='х'){x++;}else if(c=='а'||c=='е'||c=='ы'){a++;}else if(c!='п'&&c!='з'&&c!='ъ'&&c!='в')ru=false;
            if(c=='h'){h++;}else if(c=='a'||c=='e'){ea++;}else en=false;}
        return ru&&x>=2&&a>=1||en&&h>=2&&ea>=1;
    }
    static boolean isEmojiStart(int cp){
        return cp>=0x1F300&&cp<=0x1FAFF||cp>=0x2600&&cp<=0x27BF||cp>=0x1F1E6&&cp<=0x1F1FF||cp==0x2764||cp==0x2B50||cp==0x2B55||cp==0x203C||cp==0x2049||cp>=0x1F000&&cp<=0x1F2FF&&cp!=0x1F1E5;
    }
    /** End of one emoji cluster: modifiers, variation selectors, ZWJ sequences and flag pairs. */
    static int emojiEnd(String s,int start){
        int cp=s.codePointAt(start);int i=start+Character.charCount(cp);
        if(cp>=0x1F1E6&&cp<=0x1F1FF){if(i<s.length()){int next=s.codePointAt(i);if(next>=0x1F1E6&&next<=0x1F1FF)i+=Character.charCount(next);}return i;}
        while(i<s.length()){
            int next=s.codePointAt(i);
            if(next==0xFE0F||next==0x20E3||next>=0x1F3FB&&next<=0x1F3FF||next>=0xE0020&&next<=0xE007F){i+=Character.charCount(next);continue;}
            if(next==0x200D&&i+1<s.length()){int after=s.codePointAt(i+1);if(isEmojiStart(after)){i+=1+Character.charCount(after);continue;}}
            break;
        }
        return i;
    }

    static final class IntList{
        private int[] v=new int[64];private int n;
        void add(int x){if(n==v.length)v=Arrays.copyOf(v,n*2);v[n++]=x;}
        int get(int i){return v[i];}
        int size(){return n;}
        void clear(){n=0;}
        int median(){if(n==0)return -1;int[] c=Arrays.copyOf(v,n);Arrays.sort(c);return n%2==1?c[n/2]:(int)(((long)c[n/2-1]+c[n/2])/2);}
    }

    private static final HashSet<String> STOP=new HashSet<>(Arrays.asList(
        // Russian
        "это","что","как","так","вот","был","была","были","было","быть","для","его","она","они","оно","мне","меня","мой","моя","мои","мое","тебя","тебе","твой","твоя","твои","ты","вы","вас","вам","ваш","наш","нас","нам","кто","где","когда","там","тут","здесь","уже","еще","ещё","или","если","только","тоже","также","даже","чем","чтобы","чтоб","потому","почему","нет","ага","угу","ну","да","не","но","на","по","из","за","от","до","со","во","ко","об","при","про","над","под","без","через","после","перед","между","все","всё","всех","весь","вся","этот","эта","эти","эту","этого","этой","том","тот","та","те","то","той","тем","него","нее","неё","ней","ним","них","ему","ей","им","их","себя","себе","сам","сама","свой","своя","свои","может","можно","надо","нужно","очень","просто","вообще","сейчас","потом","тогда","теперь","будет","буду","будем","есть","нету","раз","два","хорошо","ладно","кстати","типа","короче","блин","прям","ведь","лишь","какой","какая","какие","какое","такой","такая","такие","такое","который","которая","которые","которое","него","чего","кого","кому","чему","ничего","никто","всем","всего","нибудь","либо","будто","разве","конечно","спасибо","пожалуйста","привет","пока","давай","щас","че","чё","шо","вроде","всегда","никогда","иногда","много","мало","больше","меньше","один","одна","одно","куда","откуда","зачем","сколько","сегодня","завтра","вчера","него","есть","нем","нём","мной","тобой","ними","нами","вами","собой","эх","ох","ах","ой","мм","ммм","хм","хмм",
        // English
        "the","and","you","that","for","are","with","this","have","not","but","was","what","all","can","your","just","like","get","out","they","about","there","one","its","it's","i'm","don't","dont","can't","im","will","would","could","should","from","his","her","she","him","them","then","than","how","who","why","when","where","which","also","into","our","were","been","has","had","did","does","doing","yes","yeah","okay","too","very","some","any","more","now","here","well","much","even","only","really","know","think","want","going","got","let","say","said","see","there's","that's","thats","because","cause","lol","haha"));
}
