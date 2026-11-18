/* Tetogram, 2026. GNU GPL v2 or later. "Wrapped"-style conversation statistics computed on the device. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.tetogram.ChatStats;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class ChatStatsActivity extends LabsPage {
    private static final int QUICK=1,DEEP=2,ALL=3,STOP=4,SHARE=5,AGAIN=6;
    private final long dialog;
    private ChatStatsLoader loader;
    private ChatStats.Result result;
    private boolean complete,loading;
    private String error;
    private int loaded,total=-1,wait,counterRequest=-1,countRequest=-1;
    private int[] counters;
    private ChatStatsViews.Ranking progressView;
    private final ArrayList<UItem> resultItems=new ArrayList<>();
    private ChatStatsViews.Hero hero;

    public ChatStatsActivity(int account,long dialog){super(account);this.dialog=dialog;}

    @Override protected CharSequence title(){return getString(R.string.TetogramStats);}

    @Override public boolean onFragmentCreate(){
        if(!DialogObject.isEncryptedDialog(dialog)){
            countRequest=ChatStatsLoader.requestTotal(currentAccount,dialog,count->{countRequest=-1;total=count;if(list!=null&&!loading&&result==null)list.adapter.update(false);});
            counterRequest=ChatStatsLoader.requestCounters(currentAccount,dialog,values->{counterRequest=-1;counters=values;if(result!=null)rebuild();});
        }
        return super.onFragmentCreate();
    }

    @Override public void onFragmentDestroy(){
        if(loader!=null)loader.cancel();loader=null;
        if(counterRequest>=0)getConnectionsManager().cancelRequest(counterRequest,true);
        if(countRequest>=0)getConnectionsManager().cancelRequest(countRequest,true);
        resultItems.clear();result=null;
        super.onFragmentDestroy();
    }

    private String chatName(){
        if(DialogObject.isUserDialog(dialog)){TLRPC.User u=getMessagesController().getUser(dialog);if(u!=null)return UserObject.isUserSelf(u)?getString(R.string.SavedMessages):UserObject.getUserName(u);}
        else{TLRPC.Chat c=getMessagesController().getChat(-dialog);if(c!=null)return c.title;}
        return getString(R.string.TetogramStats);
    }
    private String senderName(long id){
        if(id==getUserConfig().getClientUserId())return getString(R.string.TetogramStatsYou);
        if(id>0){TLRPC.User u=getMessagesController().getUser(id);if(u!=null)return UserObject.getUserName(u);}
        else if(id<0){TLRPC.Chat c=getMessagesController().getChat(-id);if(c!=null)return c.title;}
        return "ID "+id;
    }

    private Locale locale(){Locale l=LocaleController.getInstance().getCurrentLocale();return l==null?Locale.getDefault():l;}
    private String num(long v){return NumberFormat.getIntegerInstance(locale()).format(v);}
    private String day(long localDay){SimpleDateFormat f=new SimpleDateFormat("d MMM yyyy",locale());f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date(localDay*86400000L));}
    private String hour(int h){return String.format(Locale.ROOT,"%02d:00",h);}
    private String duration(int seconds){
        if(seconds<60)return formatString(R.string.TetogramStatsDurationS,Math.max(0,seconds));
        int m=seconds/60;if(m<60)return formatString(R.string.TetogramStatsDurationM,m);
        return formatString(R.string.TetogramStatsDurationHM,m/60,m%60);
    }
    private String[] weekdays(){String[] s=new DateFormatSymbols(locale()).getShortWeekdays();String[] out=new String[7];int[] map={2,3,4,5,6,7,1};for(int i=0;i<7;i++)out[i]=cap(s[map[i]]);return out;}
    private String weekdayLong(int i){String[] s=new DateFormatSymbols(locale()).getWeekdays();return s[new int[]{2,3,4,5,6,7,1}[i]];}
    private static String cap(String s){return s==null||s.isEmpty()?"":s.substring(0,1).toUpperCase()+s.substring(1).replace(".","");}

    // ---- list ----

    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        if(DialogObject.isEncryptedDialog(dialog)){items.add(UItem.asShadow(getString(R.string.TetogramStatsSecret)));return;}
        if(loading){
            items.add(UItem.asHeader(getString(R.string.TetogramStatsLoading)));
            if(progressView==null)progressView=new ChatStatsViews.Ranking(getContext(),progressRows());
            items.add(UItem.asCustom(progressView,-2));
            items.add(UItem.asShadow(wait>0?formatString(R.string.TetogramStatsWait,wait):getString(R.string.TetogramStatsAllInfo)));
            items.add(UItem.asButton(STOP,getString(R.string.TetogramStatsStop)));
            return;
        }
        if(result!=null){items.addAll(resultItems);return;}
        items.add(UItem.asShadow(getString(R.string.TetogramStatsIntro)));
        if(error!=null)items.add(UItem.asShadow(formatString(R.string.TetogramStatsError,error)));
        items.add(UItem.asHeader(getString(R.string.TetogramStatsDepth)));
        items.add(UItem.asButton(QUICK,getString(R.string.TetogramStatsQuick)));
        items.add(UItem.asButton(DEEP,getString(R.string.TetogramStatsDeep)));
        items.add(UItem.asButton(ALL,getString(R.string.TetogramStatsAll),total>=0?num(total):""));
        items.add(UItem.asShadow(getString(R.string.TetogramStatsAllInfo)+"\n\n"+getString(R.string.TetogramStatsPrivacy)));
    }

    private ArrayList<ChatStatsViews.RankRow> progressRows(){
        ArrayList<ChatStatsViews.RankRow> rows=new ArrayList<>();
        float share=total>0?Math.min(1f,loaded/(float)total):0;
        rows.add(new ChatStatsViews.RankRow(chatName(),formatString(R.string.TetogramStatsLoaded,num(loaded),total>=0?num(total):"…"),share,ChatStatsViews.accent()));
        return rows;
    }

    @Override protected void click(UItem item,View view,int position,float x,float y){
        switch(item.id){
            case QUICK:start(3000);break;
            case DEEP:start(20000);break;
            case ALL:start(0);break;
            case STOP:if(loader!=null)loader.stopEarly();break;
            case SHARE:share();break;
            case AGAIN:result=null;resultItems.clear();hero=null;error=null;if(list!=null)list.adapter.update(true);break;
        }
    }

    private void start(int limit){
        if(loading)return;
        loading=true;error=null;loaded=0;wait=0;progressView=null;
        loader=new ChatStatsLoader(currentAccount,dialog,limit,new ChatStatsLoader.Listener(){
            @Override public void progress(int l,int t,int w){loaded=l;if(t>0)total=t;wait=w;updateProgress();}
            @Override public void done(ChatStats.Result r,boolean all,String e){
                loading=false;loader=null;progressView=null;
                if(r.total==0&&e!=null){error=e;result=null;}
                else{result=r;complete=all;if(all)total=Math.max(total,r.total);rebuild();}
                if(list!=null)list.adapter.update(true);
            }
        });
        if(list!=null)list.adapter.update(true);
        loader.start();
    }

    private void updateProgress(){
        if(list==null)return;
        if(progressView!=null&&wait==0){progressView.setRows(progressRows());}
        list.adapter.update(false);
    }

    // ---- result ----

    private void rebuild(){
        ChatStats.Result r=result;if(r==null||getContext()==null)return;
        Context ctx=getContext();
        resultItems.clear();
        if(r.total==0){resultItems.add(UItem.asShadow(getString(R.string.TetogramStatsEmpty)));resultItems.add(UItem.asButton(AGAIN,getString(R.string.TetogramStatsAgain)));if(list!=null)list.adapter.update(true);return;}
        String range=day(r.firstDay)+" — "+day(r.lastDay);
        hero=new ChatStatsViews.Hero(ctx,chatName(),range,num(r.total),formatPluralString("TetogramStatsMessageWord",r.total),
            new String[]{num(r.activeDays),formatPluralString("TetogramStatsDays",r.longestStreak),String.format(locale(),"%.1f",r.perActiveDay())},
            new String[]{getString(R.string.TetogramStatsActiveDays),getString(R.string.TetogramStatsBestStreak),getString(R.string.TetogramStatsPerDay)});
        resultItems.add(UItem.asCustom(hero,-2));
        if(!complete)resultItems.add(UItem.asShadow(formatString(R.string.TetogramStatsPartial,num(r.total),num(Math.max(total,r.total)))));
        else resultItems.add(UItem.asShadow(null));

        // who writes more
        if(r.senders.size()>=2){
            resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsWho)));
            if(r.senders.size()==2){
                ChatStats.Sender a=r.senders.get(0),b=r.senders.get(1);
                if(b.id==getUserConfig().getClientUserId()){ChatStats.Sender t=a;a=b;b=t;}
                float share=a.messages/(float)(a.messages+b.messages);
                resultItems.add(UItem.asCustom(new ChatStatsViews.Split(ctx,senderName(a.id),senderName(b.id),Math.round(share*100)+"%",(100-Math.round(share*100))+"%",share),-2));
                ArrayList<ChatStatsViews.RankRow> rows=new ArrayList<>();
                int max=Math.max(a.messages,b.messages);
                rows.add(new ChatStatsViews.RankRow(senderName(a.id),formatPluralString("TetogramStatsMsgs",a.messages),a.messages/(float)max,ChatStatsViews.palette(0)));
                rows.add(new ChatStatsViews.RankRow(senderName(b.id),formatPluralString("TetogramStatsMsgs",b.messages),b.messages/(float)max,ChatStatsViews.palette(1)));
                resultItems.add(UItem.asCustom(new ChatStatsViews.Ranking(ctx,rows),-2));
                ChatStats.Sender more=a.chars>=b.chars?a:b,less=more==a?b:a;
                resultItems.add(UItem.asShadow(more.chars>less.chars?formatString(R.string.TetogramStatsWhoInfo,senderName(more.id),num(more.chars-less.chars)):null));
            }else{
                ArrayList<ChatStatsViews.RankRow> rows=new ArrayList<>();
                int max=r.senders.get(0).messages;
                for(int i=0;i<Math.min(10,r.senders.size());i++){ChatStats.Sender s=r.senders.get(i);rows.add(new ChatStatsViews.RankRow(senderName(s.id),num(s.messages)+" · "+Math.round(s.messages*100f/r.total)+"%",s.messages/(float)max,ChatStatsViews.palette(i)));}
                resultItems.add(UItem.asCustom(new ChatStatsViews.Ranking(ctx,rows),-2));
                resultItems.add(UItem.asShadow(null));
            }
        }

        // hours
        resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsHours)));
        String[] hourTicks=new String[24];for(int h=0;h<24;h+=3)hourTicks[h]=String.format(Locale.ROOT,"%02d",h);
        resultItems.add(UItem.asCustom(new ChatStatsViews.Bars(ctx,r.hours,hourTicks,r.peakHour,ChatStatsViews.accent()),-2));
        resultItems.add(UItem.asShadow(chronotype(r)));

        // weekdays
        String[] wd=weekdays();
        resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsWeekdays)));
        resultItems.add(UItem.asCustom(new ChatStatsViews.Bars(ctx,r.weekdays,wd,r.peakWeekday,ChatStatsViews.palette(2)),-2));
        resultItems.add(UItem.asShadow(r.peakWeekday>=0?formatString(R.string.TetogramStatsPeakWeekday,weekdayLong(r.peakWeekday)):null));

        // heat map
        resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsHeatmap)));
        resultItems.add(UItem.asCustom(new ChatStatsViews.Heatmap(ctx,r.heat,wd),-2));
        resultItems.add(UItem.asShadow(getString(R.string.TetogramStatsHeatmapInfo)));

        // months
        if(r.months.length>=2){
            resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsMonths)));
            String[] shortMonths=new DateFormatSymbols(locale()).getShortMonths();
            String[] ticks=new String[r.months.length];int step=Math.max(1,(int)Math.ceil(r.months.length/6.0));
            int best=0;for(int i=0;i<r.months.length;i++){if(r.months[i]>r.months[best])best=i;int m=r.firstMonth+i;if(i%step==0)ticks[i]=(m%12==0||i==0)?String.valueOf(m/12):cap(shortMonths[m%12]);}
            resultItems.add(UItem.asCustom(new ChatStatsViews.Bars(ctx,r.months,ticks,best,ChatStatsViews.palette(3)),-2));
            int bm=r.firstMonth+best;SimpleDateFormat f=new SimpleDateFormat("LLLL yyyy",locale());java.util.Calendar c=java.util.Calendar.getInstance();c.clear();c.set(bm/12,bm%12,1);
            resultItems.add(UItem.asShadow(formatString(R.string.TetogramStatsPeakMonth,f.format(c.getTime()),formatPluralString("TetogramStatsMsgs",r.months[best]))));
        }

        // records
        resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsRecords)));
        ArrayList<String[]> tiles=new ArrayList<>();
        tiles.add(new String[]{"🔥",formatPluralString("TetogramStatsDays",r.longestStreak),formatString(R.string.TetogramStatsStreakCaption,day(r.streakStartDay))});
        tiles.add(new String[]{"📅",formatPluralString("TetogramStatsMsgs",r.busiestDayCount),formatString(R.string.TetogramStatsBusiestCaption,day(r.busiestDay))});
        if(r.currentStreak>0)tiles.add(new String[]{"⚡",formatPluralString("TetogramStatsDays",r.currentStreak),getString(R.string.TetogramStatsCurrentStreak)});
        int textMessages=0;for(ChatStats.Sender s:r.senders)textMessages+=s.messages;
        if(r.chars>0)tiles.add(new String[]{"💬",formatPluralString("TetogramStatsChars",(int)Math.round(r.chars/(double)Math.max(1,textMessages))),getString(R.string.TetogramStatsAvgLength)});
        if(r.longestMessage>0)tiles.add(new String[]{"📜",formatPluralString("TetogramStatsChars",r.longestMessage),getString(R.string.TetogramStatsLongest)+" · "+senderName(r.longestMessageSender)});
        if(r.questions>0)tiles.add(new String[]{"❓",num(r.questions),getString(R.string.TetogramStatsQuestions)});
        if(r.laughs>0)tiles.add(new String[]{"😂",num(r.laughs),getString(R.string.TetogramStatsLaughs)});
        if(r.voiceSeconds>0)tiles.add(new String[]{"🎙",duration(r.voiceSeconds),getString(R.string.TetogramStatsVoice)});
        if(r.night>0)tiles.add(new String[]{"🌙",num(r.night),getString(R.string.TetogramStatsNight)});
        if(r.forwarded>0)tiles.add(new String[]{"↪️",num(r.forwarded),getString(R.string.TetogramStatsForwarded)});
        resultItems.add(UItem.asCustom(new ChatStatsViews.Tiles(ctx,tiles),-2));
        resultItems.add(UItem.asShadow(null));

        // starters & response
        if(r.senders.size()>=2){
            ArrayList<ChatStatsViews.RankRow> starters=new ArrayList<>();int maxStarts=1;
            for(ChatStats.Sender s:r.senders)maxStarts=Math.max(maxStarts,s.starts);
            ArrayList<ChatStats.Sender> byStarts=new ArrayList<>(r.senders);java.util.Collections.sort(byStarts,(a,b)->Integer.compare(b.starts,a.starts));
            for(int i=0;i<Math.min(6,byStarts.size());i++){ChatStats.Sender s=byStarts.get(i);if(s.starts==0)break;starters.add(new ChatStatsViews.RankRow(senderName(s.id),num(s.starts),s.starts/(float)maxStarts,ChatStatsViews.palette(r.senders.indexOf(s))));}
            if(!starters.isEmpty()){
                resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsStarters)));
                resultItems.add(UItem.asCustom(new ChatStatsViews.Ranking(ctx,starters),-2));
                resultItems.add(UItem.asShadow(getString(R.string.TetogramStatsStartersInfo)));
            }
            ArrayList<ChatStats.Sender> responders=new ArrayList<>();
            for(ChatStats.Sender s:r.senders)if(s.medianResponse>=0&&s.messages>=5)responders.add(s);
            java.util.Collections.sort(responders,(a,b)->Integer.compare(a.medianResponse,b.medianResponse));
            if(responders.size()>=2){
                int fastest=Math.max(1,responders.get(0).medianResponse);
                ArrayList<ChatStatsViews.RankRow> rows=new ArrayList<>();
                for(int i=0;i<Math.min(6,responders.size());i++){ChatStats.Sender s=responders.get(i);rows.add(new ChatStatsViews.RankRow(senderName(s.id),"≈ "+duration(s.medianResponse),fastest/(float)Math.max(fastest,s.medianResponse),ChatStatsViews.palette(r.senders.indexOf(s))));}
                resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsResponse)));
                resultItems.add(UItem.asCustom(new ChatStatsViews.Ranking(ctx,rows),-2));
                resultItems.add(UItem.asShadow(getString(R.string.TetogramStatsResponseInfo)));
            }
        }

        // words & emoji
        if(!r.words.isEmpty()){
            ArrayList<String[]> chips=new ArrayList<>();for(int i=0;i<Math.min(24,r.words.size());i++)chips.add(new String[]{r.words.get(i).key,num(r.words.get(i).count)});
            resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsWords)));
            resultItems.add(UItem.asCustom(new ChatStatsViews.Chips(ctx,chips,false),-2));
            resultItems.add(UItem.asShadow(null));
        }
        if(!r.emoji.isEmpty()){
            ArrayList<String[]> chips=new ArrayList<>();for(int i=0;i<Math.min(15,r.emoji.size());i++)chips.add(new String[]{r.emoji.get(i).key,num(r.emoji.get(i).count)});
            resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsEmoji)));
            resultItems.add(UItem.asCustom(new ChatStatsViews.Chips(ctx,chips,true),-2));
            resultItems.add(UItem.asShadow(null));
        }

        // media
        ArrayList<String[]> media=new ArrayList<>();
        String[] icons={"🖼","🎬","🎤","⭕","🦄","🎞","📎","🎵","🔗","📊","📍",""};
        int[] labels={R.string.TetogramStatsPhotos,R.string.TetogramStatsVideos,R.string.TetogramStatsVoiceMessages,R.string.TetogramStatsRounds,R.string.TetogramStatsStickers,R.string.TetogramStatsGifs,R.string.TetogramStatsFiles,R.string.TetogramStatsMusic,R.string.TetogramStatsLinks,R.string.TetogramStatsPolls,R.string.TetogramStatsLocations};
        for(int i=0;i<labels.length;i++)if(r.media[i]>0)media.add(new String[]{icons[i],num(r.media[i]),getString(labels[i])});
        if(!media.isEmpty()){
            resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsMedia)));
            resultItems.add(UItem.asCustom(new ChatStatsViews.Tiles(ctx,media),-2));
            resultItems.add(UItem.asShadow(null));
        }
        if(counters!=null){
            ArrayList<String[]> server=new ArrayList<>();
            String[] ci={"🖼","🎬","🎤","⭕","🔗","📎","🎵","🎞"};
            int[] cl={R.string.TetogramStatsPhotos,R.string.TetogramStatsVideos,R.string.TetogramStatsVoiceMessages,R.string.TetogramStatsRounds,R.string.TetogramStatsLinks,R.string.TetogramStatsFiles,R.string.TetogramStatsMusic,R.string.TetogramStatsGifs};
            for(int i=0;i<ChatStatsLoader.COUNTER_COUNT;i++)if(counters[i]>0)server.add(new String[]{ci[i],num(counters[i]),getString(cl[i])});
            if(!server.isEmpty()){
                resultItems.add(UItem.asHeader(getString(R.string.TetogramStatsServer)));
                resultItems.add(UItem.asCustom(new ChatStatsViews.Tiles(ctx,server),-2));
                resultItems.add(UItem.asShadow(null));
            }
        }

        resultItems.add(UItem.asButton(SHARE,getString(R.string.TetogramStatsShare)));
        resultItems.add(UItem.asButton(AGAIN,getString(R.string.TetogramStatsAgain)));
        resultItems.add(UItem.asShadow(getString(R.string.TetogramStatsPrivacy)));
        if(list!=null)list.adapter.update(true);
    }

    private String chronotype(ChatStats.Result r){
        if(r.peakHour<0)return null;
        String peak=hour(r.peakHour);
        int nightShare=Math.round(r.night*100f/Math.max(1,r.total));
        if(r.peakHour<5||nightShare>=20)return formatString(R.string.TetogramStatsOwls,peak,nightShare);
        if(r.peakHour<12)return formatString(R.string.TetogramStatsLarks,peak);
        if(r.peakHour<18)return formatString(R.string.TetogramStatsDaytime,peak);
        return formatString(R.string.TetogramStatsEvening,peak);
    }

    // ---- share ----

    private String summary(){
        ChatStats.Result r=result;
        StringBuilder b=new StringBuilder();
        b.append("📊 ").append(formatString(R.string.TetogramStatsShareTitle,chatName())).append('\n');
        b.append(day(r.firstDay)).append(" — ").append(day(r.lastDay)).append("\n\n");
        b.append("💬 ").append(num(r.total)).append(' ').append(formatPluralString("TetogramStatsMessageWord",r.total)).append('\n');
        b.append("📅 ").append(num(r.activeDays)).append(' ').append(getString(R.string.TetogramStatsActiveDays)).append('\n');
        b.append("🔥 ").append(getString(R.string.TetogramStatsBestStreak)).append(": ").append(formatPluralString("TetogramStatsDays",r.longestStreak)).append('\n');
        if(r.senders.size()>=2){
            b.append('\n').append(getString(R.string.TetogramStatsWho)).append(":\n");
            for(int i=0;i<Math.min(5,r.senders.size());i++){ChatStats.Sender s=r.senders.get(i);b.append("• ").append(senderName(s.id)).append(" — ").append(Math.round(s.messages*100f/r.total)).append("%\n");}
        }
        String chrono=chronotype(r);if(chrono!=null)b.append('\n').append(chrono).append('\n');
        if(!r.emoji.isEmpty()){b.append('\n');for(int i=0;i<Math.min(5,r.emoji.size());i++)b.append(r.emoji.get(i).key);b.append('\n');}
        b.append('\n').append(getString(R.string.TetogramStatsMadeWith));
        return b.toString();
    }

    private void share(){
        if(result==null||getParentActivity()==null)return;
        String text=summary();
        Intent intent=new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT,text);
        intent.setType("text/plain");
        try{
            Uri image=renderCard();
            if(image!=null){intent.setType("image/png");intent.putExtra(Intent.EXTRA_STREAM,image);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        }catch(Throwable e){FileLog.e(e);}
        try{getParentActivity().startActivityForResult(Intent.createChooser(intent,getString(R.string.TetogramStatsShare)),500);}catch(Throwable e){FileLog.e(e);}
    }

    /** Renders the summary card plus the hourly chart into a PNG shared through the app FileProvider. */
    private Uri renderCard() throws Exception{
        if(hero==null||result==null)return null;
        int width=dp(380);
        ChatStatsViews.Hero card=new ChatStatsViews.Hero(getContext(),hero.title(),hero.subtitle(),hero.big(),hero.bigLabel(),hero.values(),hero.labels());
        card.progress=1f;
        String[] hourTicks=new String[24];for(int h=0;h<24;h+=3)hourTicks[h]=String.format(Locale.ROOT,"%02d",h);
        ChatStatsViews.Bars bars=new ChatStatsViews.Bars(getContext(),result.hours,hourTicks,result.peakHour,ChatStatsViews.accent());
        bars.progress=1f;
        int spec=View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),free=View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        card.measure(spec,free);card.layout(0,0,width,card.getMeasuredHeight());
        bars.measure(spec,free);bars.layout(0,0,width,bars.getMeasuredHeight());
        int footer=dp(36);
        int height=card.getMeasuredHeight()+bars.getMeasuredHeight()+footer;
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);bg.setColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhite));
        canvas.drawRect(new RectF(0,0,width,height),bg);
        card.draw(canvas);
        canvas.save();canvas.translate(0,card.getMeasuredHeight());bars.draw(canvas);canvas.restore();
        android.text.TextPaint tp=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);tp.setTextSize(dp(12));tp.setColor(ChatStatsViews.gray());tp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(getString(R.string.TetogramStatsMadeWith),width/2f,height-dp(14),tp);
        File dir=AndroidUtilities.getSharingDirectory();dir.mkdirs();
        File file=new File(dir,"tetogram_stats.png");
        try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();
        return androidx.core.content.FileProvider.getUriForFile(getParentActivity(),ApplicationLoader.getApplicationId()+".provider",file);
    }
}
