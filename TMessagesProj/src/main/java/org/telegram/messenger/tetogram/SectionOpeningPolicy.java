/* Tetogram, 2026. GPL v2 or later. Opening mode is presentation, not section identity. */
package org.telegram.messenger.tetogram;
public final class SectionOpeningPolicy {
 public static final String FLOATING="floating",CHATS="chats";
 private SectionOpeningPolicy(){}
 public static String normalize(String value){return CHATS.equals(value)?CHATS:FLOATING;}
 public static boolean repeatToggles(String mode,boolean activeChats,boolean eligible,boolean resumed,boolean sameAccount){return CHATS.equals(normalize(mode))&&activeChats&&eligible&&resumed&&sameAccount;}
 public static int left(int center,int width,int viewport,int insetLeft,int insetRight){int min=Math.max(0,insetLeft),max=Math.max(min,viewport-Math.max(0,insetRight)-width);return Math.max(min,Math.min(max,center-width/2));}
 public static int height(int desired,int anchorTop,int minimumTop,int gap){return Math.max(0,Math.min(desired,anchorTop-minimumTop-gap));}
}
