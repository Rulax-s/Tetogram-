/* Tetogram, 2026. GNU GPL v2 or later. Animation completion tokens fence stale callbacks. */
package org.telegram.messenger.tetogram;
public final class SectionMenuState {
 public enum Phase{CLOSED,OPENING,OPEN,CLOSING}
 public Phase phase=Phase.CLOSED;private long generation;
 public boolean targetOpen(){return phase==Phase.OPEN || phase==Phase.OPENING;}
 public long request(boolean open){generation++;phase=open?Phase.OPENING:Phase.CLOSING;return generation;}
 public boolean current(long token){return token==generation;}
 public boolean finish(long token){if(!current(token))return false;phase=targetOpen()?Phase.OPEN:Phase.CLOSED;return true;}
 public void cancel(){generation++;phase=Phase.CLOSED;}
 public static long delay(int rank,int count){return Math.min(80,Math.round(Math.max(0,rank)*Math.min(18d,80d/Math.max(1,count-1))));}
 public static boolean canEdit(long owner,long current,SectionConfig config,String id){return owner!=0 && owner==current && config!=null && (SectionUnreadPolicy.SYSTEM_ID.equals(id)?config.unreadEnabled:config.section(id)!=null);}
 public static final class ScrollGate{private int amount,direction;public Boolean scroll(int dy,int threshold){int sign=Integer.signum(dy);if(sign==0)return null;if(sign!=direction){direction=sign;amount=0;}amount+=Math.abs(dy);if(amount<threshold)return null;amount=0;return sign>0;}public void reset(){amount=direction=0;}}
}
