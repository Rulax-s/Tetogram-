#!/usr/bin/env python3
"""Real SQLite statements from the account-store implementation; no Android UI or Telegram account."""
import json,re,sqlite3,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SOURCE=(ROOT/'TMessagesProj/src/main/java/org/telegram/messenger/tetogram/EditHistory.java').read_text()
def sql(name):return json.loads(re.search(r'public static final String '+name+r' = ("[^\n]+?");',SOURCE).group(1))
class EditStorage(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.file=Path(self.tmp.name)/'account.db';self.db=sqlite3.connect(self.file);self.db.execute(sql('SCHEMA'))
 def tearDown(self):self.db.close();self.tmp.cleanup()
 def save(self,text='A',edited=0,account=1,did=-10,mid=7,sender=22,observed=1000):self.db.execute(sql('INSERT'),(account,did,mid,sender,900,edited,observed,text,text,b'private TL fixture'))
 def test_old_and_new_text_and_author_survive_restart(self):
  self.save();self.save('B',100);self.db.commit();self.db.close();self.db=sqlite3.connect(self.file)
  self.assertEqual([('A',22,0),('B',22,100)],self.db.execute('SELECT text,sender,edited FROM tetogram_edits_v1 ORDER BY seq').fetchall())
 def test_same_ids_in_different_accounts_are_separate(self):
  self.save(account=1);self.save('B',100,account=2);self.assertEqual([(1,'A'),(2,'B')],self.db.execute('SELECT account,text FROM tetogram_edits_v1 ORDER BY seq').fetchall())
 def test_group_and_channel_dialog_namespaces_are_separate(self):
  self.save(did=-10);self.save('B',100,did=-20);self.assertEqual(2,self.db.execute('SELECT COUNT(DISTINCT did) FROM tetogram_edits_v1').fetchone()[0])
 def test_multiple_messages_and_authors_keep_own_payloads(self):
  self.save(mid=7,sender=22);self.save('B',100,mid=8,sender=23);self.assertEqual([(7,22,'A'),(8,23,'B')],self.db.execute('SELECT mid,sender,text FROM tetogram_edits_v1 ORDER BY seq').fetchall())
 def test_same_second_revert_is_not_lost_to_a_unique_hash_constraint(self):
  self.save('A',100);self.save('B',100);self.save('A',100);self.assertEqual(['A','B','A'],[r[0] for r in self.db.execute('SELECT text FROM tetogram_edits_v1 ORDER BY seq')])
 def test_retention_is_account_scoped(self):
  self.save(observed=1);self.save('B',100,account=2,observed=1);self.db.execute('DELETE FROM tetogram_edits_v1 WHERE account=? AND observed<?',(1,100));self.assertEqual([(2,)],self.db.execute('SELECT account FROM tetogram_edits_v1').fetchall())
 def test_clear_one_message_does_not_delete_other_messages(self):
  self.save(mid=7);self.save('B',100,mid=8);self.db.execute('DELETE FROM tetogram_edits_v1 WHERE account=? AND did=? AND mid=?',(1,-10,7));self.assertEqual([(8,)],self.db.execute('SELECT mid FROM tetogram_edits_v1').fetchall())
 def test_clearing_local_history_never_deletes_native_messages(self):
  self.db.execute('CREATE TABLE messages_v2(uid INTEGER,mid INTEGER,data BLOB)');self.db.execute('INSERT INTO messages_v2 VALUES(-10,7,?)',(b'native',));self.save();self.db.execute('DELETE FROM tetogram_edits_v1 WHERE account=1');self.assertEqual([(b'native',)],self.db.execute('SELECT data FROM messages_v2').fetchall())
 def test_capture_savepoint_is_atomic(self):
  self.db.execute('SAVEPOINT tetogram_edit_capture_v1');self.save();self.save('B',100);self.db.execute('ROLLBACK TO SAVEPOINT tetogram_edit_capture_v1');self.db.execute('RELEASE SAVEPOINT tetogram_edit_capture_v1');self.assertEqual(0,self.db.execute('SELECT COUNT(*) FROM tetogram_edits_v1').fetchone()[0])
 def test_initializing_new_table_preserves_deleted_history(self):
  self.db.execute('CREATE TABLE tetogram_deleted_v1(mid INTEGER,text TEXT)');self.db.execute("INSERT INTO tetogram_deleted_v1 VALUES(7,'retained')");self.db.execute(sql('SCHEMA'));self.assertEqual([('retained',)],self.db.execute('SELECT text FROM tetogram_deleted_v1').fetchall())
 def test_latest_marker_is_per_message(self):
  self.save(mid=7);self.save('B',100,mid=7);self.save('C',101,mid=8)
  rows=self.db.execute('SELECT r.text,r.seq=(SELECT MAX(q.seq) FROM tetogram_edits_v1 q WHERE q.account=r.account AND q.did=r.did AND q.mid=r.mid) FROM tetogram_edits_v1 r ORDER BY r.seq').fetchall();self.assertEqual([('A',0),('B',1),('C',1)],rows)
 def test_unicode_and_newlines_are_preserved(self):
  text='🙂\nё\n👨‍👩‍👧‍👦';self.save(text);self.assertEqual(text,self.db.execute('SELECT text FROM tetogram_edits_v1').fetchone()[0])
 def test_optimistic_write_does_not_destroy_retained_baseline(self):
  self.db.execute('CREATE TABLE messages_v2(uid INTEGER,mid INTEGER,send_state INTEGER,data BLOB)');self.db.execute('INSERT INTO messages_v2 VALUES(-10,7,0,?)',(b'A',));self.save('A',0)
  self.db.execute('UPDATE messages_v2 SET data=?,send_state=3',(b'B pending',));self.assertEqual([('A',)],self.db.execute('SELECT text FROM tetogram_edits_v1').fetchall())
  self.save('B confirmed',100);self.assertEqual([('A',),('B confirmed',)],self.db.execute('SELECT text FROM tetogram_edits_v1 ORDER BY seq').fetchall())
if __name__=='__main__':unittest.main(verbosity=2)
