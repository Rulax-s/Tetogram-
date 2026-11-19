#!/usr/bin/env python3
"""Source integration checks. No claim of Android/MTProto device testing."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/'TMessagesProj/src/main/java/org/telegram'
def src(name):return (ROOT/name).read_text()
class EditIntegration(unittest.TestCase):
 def test_both_native_write_paths_capture_before_deleted_snapshot(self):
  storage=src('messenger/MessagesStorage.java')
  for argument in ('messages','messages.messages'):
   self.assertIn('EditHistory.capture(currentAccount, '+argument+');\n'+('            ' if argument=='messages' else '                ')+'org.telegram.messenger.tetogram.DeletedHistory.capture(currentAccount, '+argument+');',storage)
  self.assertIn('SELECT data,send_state FROM messages_v2 WHERE uid=? AND mid=?',src('messenger/tetogram/EditHistory.java'))
 def test_consent_defaults_off_and_is_not_reused_from_deleted_history(self):
  self.assertIn('getBoolean("save_edits",false)',src('messenger/tetogram/LabsSettings.java'))
  ui=src('ui/tetogram/LabsHistoryActivity.java');self.assertIn('edit_consent_v1',ui);self.assertIn('TetogramEditsConsent',ui);self.assertNotIn('getString(R.string.TetogramEditUnavailable)',ui)
 def test_existing_privacy_rules_and_account_scope_are_reused(self):
  store=src('messenger/tetogram/EditHistory.java');self.assertIn('DeletedHistory.eligible(a,m,true)',store);self.assertIn('getStorageQueue().postRunnable',store);self.assertIn('uid!=owner(a)',store)
  self.assertIn('account=? AND did=? AND mid=?',store);self.assertNotIn('new SQLiteDatabase',store)
 def test_versions_have_no_message_send_or_native_delete_actions(self):
  ui=src('ui/tetogram/EditHistoryActivity.java');self.assertIn('extends LabsPage',ui)
  for forbidden in ('SendMessagesHelper','deleteMessages(', 'sendMessage('):self.assertNotIn(forbidden,ui)
  self.assertIn('TetogramEditNew',ui);self.assertIn('TetogramEditOld',ui)
  self.assertIn('new MessageObject(a,message,true,false)',src('messenger/tetogram/EditHistory.java'))
 def test_live_and_restored_rows_have_same_metadata_path(self):
  chat=src('ui/ChatActivity.java');self.assertIn('.add(NotificationCenter.tetogramEditHistoryChanged)',chat);self.assertIn('new org.telegram.ui.tetogram.EditHistoryActivity',chat)
  self.assertIn('EditHistory.hydrate(a,result',src('messenger/tetogram/DeletedHistory.java'))
  self.assertIn('EditHistory.cachedCount(messageObject)',src('ui/Cells/ChatMessageCell.java'))
 def test_fingerprint_ignores_volatile_metadata(self):
  content=src('messenger/tetogram/EditHistoryContent.java');self.assertIn('SerializedData',content)
  for field in ('.views','.reactions','.file_reference'):self.assertNotIn(field,content)
  self.assertIn('message.entities',content);self.assertIn('TL_messageMediaWebPage',content)
 def test_profile_media_uses_the_viewers_account_not_the_viewed_users_uid(self):
  profile=src('ui/ProfileActivity.java');self.assertIn('tetogramBanner.configure(getUserConfig().getClientUserId(),style)',profile);self.assertNotIn('tetogramBanner.configure(userId,style)',profile)
  self.assertIn('AppearanceProfilePolicy.target(userId,chatId)',profile);self.assertIn('R.string.TetogramLocalProfileAppearance',profile)
  self.assertIn('AppearanceRuntime.effectDuration(playProfileAnimation == 2 ? 250 : 180)',profile)
 def test_optimistic_edit_preserves_baseline_without_archiving_attempted_content(self):
  store=src('messenger/tetogram/EditHistory.java');pending=store.split('if(next.send_state==MessageObject.MESSAGE_SEND_STATE_EDITING){',1)[1].split('if(next.send_state!=0)continue;',1)[0]
  self.assertIn('save(db,uid,did,previous,',pending);self.assertNotIn('save(db,uid,did,next,',pending);self.assertIn('pendingNative',store)
if __name__=='__main__':unittest.main(verbosity=2)
