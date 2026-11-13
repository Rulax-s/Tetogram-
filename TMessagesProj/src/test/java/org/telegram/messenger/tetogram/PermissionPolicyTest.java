package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class PermissionPolicyTest {
 @Test public void fullGrantSurvivesUpdateWithoutRequest() {for(int sdk:new int[]{33,34,35,36}){assertTrue(PermissionPolicy.canOpenGallery(sdk,false,true,true,false));assertFalse(PermissionPolicy.requestNotifications(sdk,true,true));}}
 @Test public void partialAccessIsUsableOnAndroid14AndLater(){for(int sdk:new int[]{34,35,36}){assertTrue(PermissionPolicy.canReadImages(sdk,false,false,true));assertTrue(PermissionPolicy.canReadVideos(sdk,false,false,true));}}
 @Test public void singleMediaTypeDoesNotBlockGallery(){assertTrue(PermissionPolicy.canOpenGallery(34,false,true,false,false));assertFalse(PermissionPolicy.canReadVideos(34,false,false,false));assertTrue(PermissionPolicy.canOpenGallery(33,false,false,true,false));}
 @Test public void revokeAndRegrantUsesCurrentState(){assertFalse(PermissionPolicy.canOpenGallery(34,false,false,false,false));assertTrue(PermissionPolicy.canOpenGallery(34,false,false,false,true));assertFalse(PermissionPolicy.canOpenGallery(34,false,false,false,false));}
 @Test public void legacyPermissionsAndSdkBoundaries(){assertTrue(PermissionPolicy.canOpenGallery(22,false,false,false,false));assertFalse(PermissionPolicy.canOpenGallery(32,false,true,true,true));assertTrue(PermissionPolicy.canOpenGallery(32,true,false,false,false));assertFalse(PermissionPolicy.canOpenGallery(33,false,false,false,true));}
 @Test public void notificationsRecheckedImmediatelyBeforePrompt(){assertTrue(PermissionPolicy.requestNotifications(34,false,false));assertFalse(PermissionPolicy.requestNotifications(34,true,false));assertFalse(PermissionPolicy.requestNotifications(34,false,true));assertFalse(PermissionPolicy.requestNotifications(32,false,false));}
}
