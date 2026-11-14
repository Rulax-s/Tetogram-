package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class SectionIconPolicyTest {
 private final String id="b8348b88-aac9-4f3c-85f7-bb44b7e2b967";
 @Test public void ownedFilesAreUuidNamesNotArbitraryPaths(){assertTrue(SectionIconPolicy.sectionId(id));assertTrue(SectionIconPolicy.imageName(id+".jpg"));assertTrue(SectionIconPolicy.inputName(id+".input"));assertFalse(SectionIconPolicy.imageName("../"+id+".jpg"));assertFalse(SectionIconPolicy.imageName("/sdcard/photo.jpg"));assertFalse(SectionIconPolicy.sectionId(SectionUnreadPolicy.SYSTEM_ID));}
 @Test public void accountImageDirectoriesAreDifferent(){assertNotEquals(SectionIconPolicy.accountDirectory(100),SectionIconPolicy.accountDirectory(101));assertEquals("u100",SectionIconPolicy.accountDirectory(100));}
 @Test public void hugePortraitAndLandscapeDecodeIsBounded(){for(int[] size:new int[][]{{800,800},{48000,32000},{32000,48000},{Integer.MAX_VALUE,1}}){int sample=SectionIconPolicy.sample(size[0],size[1],512);assertTrue(sample>0);assertEquals(0,sample&(sample-1));assertTrue(Math.max(size[0],size[1])/(long)sample<=1024);}}
 @Test public void cancellationAndNewSelectionRejectOldPhotoResults(){SectionIconPolicy.Edit e=new SectionIconPolicy.Edit();long first=e.begin();e.cancel();assertFalse(e.current(first));long second=e.begin();assertTrue(e.current(second));long third=e.begin();assertFalse(e.current(second));assertTrue(e.current(third));}
 @Test public void composedEmojiIsKeptWithoutCodePointTruncation(){String emoji="👩🏽‍💻🏳️‍🌈";SectionConfig c=new SectionConfig();SectionConfig.Section s=c.add("Name");s.icon=emoji;assertEquals(emoji,SectionConfig.decode(c.encode()).section(s.id).icon);}
 @Test(expected=IllegalArgumentException.class) public void damagedImageBoundsAreRejected(){SectionIconPolicy.sample(-1,0,512);}
}
