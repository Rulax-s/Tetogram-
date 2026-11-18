package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.TimeZone;
public class ChatStatsTest {
 private static final TimeZone UTC=TimeZone.getTimeZone("UTC");
 // 2026-11-16 00:00 UTC, a Monday
 private static final int MONDAY=1794787200;
 private static int at(int day,int hour,int minute){return MONDAY+day*86400+hour*3600+minute*60;}
 private static ChatStats.Sender sender(ChatStats.Result r,long id){for(ChatStats.Sender s:r.senders)if(s.id==id)return s;throw new AssertionError(id);}
 @Test public void mondayConstantIsMonday(){java.util.Calendar c=java.util.Calendar.getInstance(UTC);c.setTimeInMillis(MONDAY*1000L);assertEquals(java.util.Calendar.MONDAY,c.get(java.util.Calendar.DAY_OF_WEEK));assertEquals(0,c.get(java.util.Calendar.HOUR_OF_DAY));}
 @Test public void emptyResultIsSafe(){ChatStats.Result r=new ChatStats(UTC).finish(at(0,12,0));assertEquals(0,r.total);assertEquals(-1,r.peakHour);assertEquals(0,r.months.length);assertEquals(0,r.perActiveDay(),0);}
 @Test public void hoursWeekdaysAndHeatUseLocalZone(){
  ChatStats s=new ChatStats(TimeZone.getTimeZone("GMT+03:00"));
  s.add(1,at(0,22,30),"a",ChatStats.MEDIA_NONE,0,false);// 01:30 Tuesday local
  ChatStats.Result r=s.finish(0);
  assertEquals(1,r.hours[1]);assertEquals(1,r.weekdays[1]);assertEquals(1,r.heat[1][1]);assertEquals(1,r.night);
 }
 @Test public void orderOfInsertionDoesNotMatter(){
  ChatStats a=new ChatStats(UTC),b=new ChatStats(UTC);
  int[] dates={at(0,10,0),at(0,10,5),at(1,9,0),at(3,20,0)};long[] who={1,2,1,2};
  for(int i=0;i<dates.length;i++)a.add(who[i],dates[i],"hi",ChatStats.MEDIA_NONE,0,false);
  for(int i=dates.length-1;i>=0;i--)b.add(who[i],dates[i],"hi",ChatStats.MEDIA_NONE,0,false);
  ChatStats.Result ra=a.finish(0),rb=b.finish(0);
  assertEquals(ra.activeDays,rb.activeDays);assertEquals(ra.longestStreak,rb.longestStreak);
  assertEquals(sender(ra,1).starts,sender(rb,1).starts);assertEquals(sender(ra,2).medianResponse,sender(rb,2).medianResponse);
 }
 @Test public void streaksAndBusiestDay(){
  ChatStats s=new ChatStats(UTC);
  for(int d:new int[]{0,1,2,5,6})s.add(1,at(d,12,0),"x",ChatStats.MEDIA_NONE,0,false);
  for(int i=0;i<4;i++)s.add(2,at(5,13,i),"y",ChatStats.MEDIA_NONE,0,false);
  ChatStats.Result r=s.finish(at(6,23,0));
  assertEquals(5,r.activeDays);assertEquals(3,r.longestStreak);assertEquals(s.localDay(at(0,0,0)),r.streakStartDay);
  assertEquals(2,r.currentStreak);assertEquals(5,r.busiestDayCount);assertEquals(s.localDay(at(5,0,0)),r.busiestDay);
  assertEquals(9/5.0,r.perActiveDay(),1e-9);
 }
 @Test public void currentStreakIsZeroWhenStale(){ChatStats s=new ChatStats(UTC);s.add(1,at(0,12,0),"x",-1,0,false);assertEquals(0,s.finish(at(5,0,0)).currentStreak);ChatStats t=new ChatStats(UTC);t.add(1,at(0,12,0),"x",-1,0,false);assertEquals(1,t.finish(at(1,8,0)).currentStreak);}
 @Test public void initiatorsAndResponseTimes(){
  ChatStats s=new ChatStats(UTC);
  s.add(1,at(0,10,0),"привет",-1,0,false);
  s.add(2,at(0,10,2),"привет!",-1,0,false);// 120 s
  s.add(1,at(0,10,6),"как ты?",-1,0,false);// 240 s
  s.add(2,at(0,20,0),"норм",-1,0,false);// gap 9h54m > 6h: new conversation started by 2
  s.add(1,at(0,20,10),"ок",-1,0,false);// 600 s
  ChatStats.Result r=s.finish(0);
  assertEquals(1,sender(r,1).starts);assertEquals(1,sender(r,2).starts);
  assertEquals(120,sender(r,2).medianResponse);assertEquals((240+600)/2,sender(r,1).medianResponse);
  assertEquals(1,r.questions);
 }
 @Test public void monthsAreContiguous(){
  ChatStats s=new ChatStats(UTC);
  s.add(1,at(0,12,0),"a",-1,0,false);// Nov 2026
  s.add(1,at(80,12,0),"b",-1,0,false);// Feb 2027
  ChatStats.Result r=s.finish(0);
  assertEquals(4,r.months.length);assertArrayEquals(new int[]{1,0,0,1},r.months);assertEquals(2026*12+10,r.firstMonth);
 }
 @Test public void wordsSkipStopwordsShortWordsNumbersAndLinks(){
  ChatStats s=new ChatStats(UTC);
  s.add(1,at(0,1,0),"Кот это КОТ, кот! и 12345 https://example.com котёнок котенок",-1,0,false);
  s.add(1,at(0,1,1),"the cat",-1,0,false);
  ChatStats.Result r=s.finish(0);
  assertEquals("кот",r.words.get(0).key);assertEquals(3,r.words.get(0).count);
  assertEquals("котенок",r.words.get(1).key);assertEquals(2,r.words.get(1).count);
  for(ChatStats.Entry e:r.words){assertNotEquals("это",e.key);assertNotEquals("12345",e.key);assertFalse(e.key.startsWith("http"));assertNotEquals("the",e.key);}
 }
 @Test public void apostrophesAndHyphensStayInsideWords(){ChatStats s=new ChatStats(UTC);s.add(1,at(0,1,0),"кое-что кое-что don't-care",-1,0,false);assertEquals("кое-что",s.finish(0).words.get(0).key);}
 @Test public void emojiClustersAreCountedWhole(){
  ChatStats s=new ChatStats(UTC);
  s.add(1,at(0,1,0),"😂😂 👍🏽 👨‍👩‍👧 🇷🇺 ❤️ ❤️",-1,0,false);
  ChatStats.Result r=s.finish(0);
  java.util.HashMap<String,Integer> m=new java.util.HashMap<>();for(ChatStats.Entry e:r.emoji)m.put(e.key,e.count);
  assertEquals(Integer.valueOf(2),m.get("😂"));assertEquals(Integer.valueOf(1),m.get("👍🏽"));assertEquals(Integer.valueOf(1),m.get("👨‍👩‍👧"));
  assertEquals(Integer.valueOf(1),m.get("🇷🇺"));assertEquals(Integer.valueOf(2),m.get("❤️"));assertEquals(5,m.size());
 }
 @Test public void laughterDetection(){
  for(String w:new String[]{"ахахах","хахаха","ахпхахп","hahaha","lol","ржу","хд"})assertTrue(w,ChatStats.isLaugh(w));
  for(String w:new String[]{"хорошо","ах","ха","hello","хата","ахах1"})assertFalse(w,ChatStats.isLaugh(w));
  ChatStats s=new ChatStats(UTC);s.add(1,at(0,1,0),"АХАХАХ ну ты даёшь",-1,0,false);s.add(1,at(0,1,1),"хаха хаха",-1,0,false);
  ChatStats.Result r=s.finish(0);assertEquals(2,r.laughs);for(ChatStats.Entry e:r.words)assertNotEquals("ахахах",e.key);
 }
 @Test public void mediaVoiceAndForwards(){
  ChatStats s=new ChatStats(UTC);
  s.add(1,at(0,1,0),null,ChatStats.MEDIA_VOICE,65,false);
  s.add(2,at(0,1,1),null,ChatStats.MEDIA_ROUND,15,false);
  s.add(2,at(0,1,2),"подпись подпись",ChatStats.MEDIA_PHOTO,0,true);
  ChatStats.Result r=s.finish(0);
  assertEquals(1,r.media[ChatStats.MEDIA_VOICE]);assertEquals(1,r.media[ChatStats.MEDIA_ROUND]);assertEquals(1,r.media[ChatStats.MEDIA_PHOTO]);
  assertEquals(80,r.voiceSeconds);assertEquals(65,sender(r,1).voiceSeconds);assertEquals(1,r.forwarded);
  assertTrue("forwarded captions are not the sender's words",r.words.isEmpty());
 }
 @Test public void sendersSortedAndCharactersCountedByCodePoint(){
  ChatStats s=new ChatStats(UTC);
  s.add(5,at(0,1,0),"🙂🙂",-1,0,false);s.add(7,at(0,1,1),"a",-1,0,false);s.add(7,at(0,1,2),"bb",-1,0,false);
  ChatStats.Result r=s.finish(0);
  assertEquals(7,r.senders.get(0).id);assertEquals(2,sender(r,5).chars);assertEquals(2,r.longestMessage);assertEquals(5,r.longestMessageSender);assertEquals(5,r.chars);
 }
 @Test public void invalidDatesAreIgnored(){ChatStats s=new ChatStats(UTC);s.add(1,0,"x",-1,0,false);s.add(1,-5,"x",-1,0,false);assertEquals(0,s.finish(0).total);}
 @Test public void peakHourAndWeekday(){ChatStats s=new ChatStats(UTC);s.add(1,at(2,23,0),"a",-1,0,false);s.add(1,at(9,23,5),"b",-1,0,false);s.add(1,at(3,8,0),"c",-1,0,false);ChatStats.Result r=s.finish(0);assertEquals(23,r.peakHour);assertEquals(2,r.peakWeekday);assertEquals(2,r.heatMax());}
}
