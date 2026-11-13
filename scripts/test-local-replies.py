#!/usr/bin/env python3
"""Real SQLite mapping contracts; not Android UI or Telegram network tests."""
import json,re,sqlite3,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SOURCE=(ROOT/'TMessagesProj/src/main/java/org/telegram/messenger/tetogram/LocalReplies.java').read_text()
SCHEMA=json.loads(re.search(r'public static final String SCHEMA=("[^\n]+");',SOURCE).group(1))
class Links(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.path=Path(self.tmp.name)/'account.db';self.db=sqlite3.connect(self.path);self.db.execute(SCHEMA)
 def tearDown(self):self.db.close();self.tmp.cleanup()
 def put(self,owner=1,did=-50,random=123,mid=-1,mode=0,source=11,sender=101):self.db.execute('INSERT OR REPLACE INTO tetogram_reply_links_v1 VALUES(?,?,?,?,?,?,?,?,?)',(owner,did,random,mid,mode,source,sender,14,'hash fixture'))
 def rows(self):return self.db.execute('SELECT owner,did,mid,mode,source_mid,sender FROM tetogram_reply_links_v1 ORDER BY owner,did,mid').fetchall()
 def test_ack_and_restart(self):
  self.put();self.db.execute('UPDATE tetogram_reply_links_v1 SET mid=? WHERE owner=? AND did=? AND mode=? AND random_id=?',(42,1,-50,0,123));self.db.commit();self.db.close();self.db=sqlite3.connect(self.path);self.assertEqual(self.rows(),[(1,-50,42,0,11,101)])
 def test_account_chat_and_scheduled_isolation(self):
  self.put();self.put(owner=2);self.put(did=-51);self.put(mode=1);self.db.execute('UPDATE tetogram_reply_links_v1 SET mid=42 WHERE owner=1 AND did=-50 AND mode=0 AND random_id=123');self.assertEqual(sum(row[2]==42 for row in self.rows()),1);self.assertEqual(len(self.rows()),4)
 def test_scheduled_delivery_keeps_normal_message_with_same_id(self):
  self.put(random=123,mid=42,mode=1,source=11,sender=101);self.put(random=124,mid=42,mode=0,source=12,sender=102);self.db.execute("UPDATE tetogram_reply_links_v1 SET mid=99,mode=0 WHERE owner=1 AND did=-50 AND mode=1 AND random_id=123");self.assertEqual(self.rows(),[(1,-50,42,0,12,102),(1,-50,99,0,11,101)])
 def test_retry_is_idempotent(self):self.put();self.put();self.assertEqual(len(self.rows()),1)
 def test_two_authors_and_multiple_replies(self):
  self.put(random=123,mid=41,source=11,sender=101);self.put(random=124,mid=42,source=12,sender=102);self.put(random=125,mid=43,source=11,sender=101);self.assertEqual([row[4:] for row in self.rows()],[(11,101),(12,102),(11,101)])
 def test_deleted_original_falls_back_without_text_duplication(self):
  self.put();self.db.execute('CREATE TABLE originals(owner INTEGER,did INTEGER,mid INTEGER)');self.db.execute('INSERT INTO originals VALUES(1,-50,11)');query='SELECT l.mid FROM tetogram_reply_links_v1 l JOIN originals h ON h.owner=l.owner AND h.did=l.did AND h.mid=l.source_mid';self.assertEqual(len(self.db.execute(query).fetchall()),1);self.db.execute('DELETE FROM originals');self.assertEqual(self.db.execute(query).fetchall(),[]);self.assertNotIn('text',[row[1] for row in self.db.execute('PRAGMA table_info(tetogram_reply_links_v1)')])
if __name__=='__main__':unittest.main(verbosity=2)
