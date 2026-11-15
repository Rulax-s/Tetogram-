package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class EditHistoryPolicyTest {
 @Test public void firstActualEditIsDetected(){assertTrue(EditHistoryPolicy.changed(0,100,"old","new"));}
 @Test public void reactionAndViewUpdatesAreNotEdits(){assertFalse(EditHistoryPolicy.changed(100,100,"same","same"));}
 @Test public void sameSecondDifferentTextIsAnotherVersion(){assertTrue(EditHistoryPolicy.changed(100,100,"A","B"));}
 @Test public void oldHistoryPageCannotRollBackLatestVersion(){assertFalse(EditHistoryPolicy.changed(200,100,"new","old"));}
 @Test public void unseenOriginalIsNotFabricated(){assertFalse(EditHistoryPolicy.changed(0,100,null,"known"));}
 @Test public void uneditedMessagesAreNotVersions(){assertFalse(EditHistoryPolicy.changed(0,0,"A","B"));}
 @Test public void oneKnownVersionHasNoPreviousVersionBadge(){assertFalse(EditHistoryPolicy.visible(1,true,true));assertTrue(EditHistoryPolicy.visible(2,true,true));}
 @Test public void markPreferenceAndPrivacyAreIndependentGates(){assertFalse(EditHistoryPolicy.visible(3,false,true));assertFalse(EditHistoryPolicy.visible(3,true,false));}
 @Test public void keysSeparateAccountsChatsAndMessages(){assertNotEquals(EditHistoryPolicy.key(1,-100,8),EditHistoryPolicy.key(2,-100,8));assertNotEquals(EditHistoryPolicy.key(1,-100,8),EditHistoryPolicy.key(1,-101,8));assertNotEquals(EditHistoryPolicy.key(1,-100,8),EditHistoryPolicy.key(1,-100,9));}
 @Test public void groupAndDirectNamespacesCannotCollide(){assertNotEquals(EditHistoryPolicy.key(1,100,8),EditHistoryPolicy.key(1,-100,8));}
 @Test public void repeatedUpdateIsNotAppended(){assertFalse(EditHistoryPolicy.append(100,"B",100,"B"));}
 @Test public void sameSecondReturnToEarlierTextIsARealTransition(){assertTrue(EditHistoryPolicy.append(100,"A",100,"B"));assertTrue(EditHistoryPolicy.append(100,"B",100,"A"));}
 @Test public void firstKnownSnapshotCanBeAppendedWithoutInventingOldText(){assertTrue(EditHistoryPolicy.append(0,null,100,"known"));}
 @Test public void delayedVersionCannotBeAppendedAfterNewerOne(){assertFalse(EditHistoryPolicy.append(200,"new",100,"old"));}
 @Test public void timestampOnlyChangeIsNotAnotherContentVersion(){assertFalse(EditHistoryPolicy.append(100,"same",101,"same"));}
}
