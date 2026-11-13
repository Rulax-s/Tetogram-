#!/usr/bin/env python3
"""SQLite contract tests using the exact schema and marking SQL from the Java store.
No Telegram account, SDK, credentials or network. UI tests remain a separate device checklist.
"""
import json
import re
import sqlite3
import tempfile
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / 'TMessagesProj/src/main/java/org/telegram/messenger/tetogram/DeletedHistory.java').read_text()
def sql(name):
    return json.loads(re.search(r'public static final String ' + name + r' = ("[^\n]+?");', SOURCE).group(1))
INSERT = json.loads(re.search(r'db.executeFast\(("INSERT OR IGNORE[^\n]+?")\)', SOURCE).group(1))

class StorageContract(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(prefix='tetogram-history-test-')
        self.path=Path(self.temp.name)/'account.db'
        self.db=sqlite3.connect(self.path)
        self.db.execute(sql('SCHEMA'))
    def tearDown(self):
        self.db.close();self.temp.cleanup()
    def save(self,account=100,did=200,mid=1,channel=0,text='Привет',received=1000,topic=0,sender=300,sent=900):
        self.db.execute(INSERT,(account,did,mid,channel,sender,text,text.lower(),topic,sent,received,b'local TL payload fixture'))
    def mark(self,ids,account=100,channel=0,time=2000):
        for mid in ids:
            self.db.execute(sql('MARK_CHANNEL') if channel else sql('MARK_GLOBAL'),
                (time,account,mid,-channel,channel) if channel else (time,account,mid))
    def deleted(self):
        return self.db.execute('SELECT account,did,mid,deleted FROM tetogram_deleted_v1 WHERE deleted>0 ORDER BY mid').fetchall()
    def test_two_authors_remain_distinct_after_delete_reopen_and_new_message(self):
        self.save(mid=10,sender=301,text='fixture A',sent=917)
        self.save(mid=11,sender=302,text='fixture B',sent=918)
        self.mark([10,11]);self.mark([10,11],time=3000)
        self.save(mid=12,sender=303,text='new fixture',sent=922)
        self.db.commit();self.db.close();self.db=sqlite3.connect(self.path)
        rows=self.db.execute('SELECT account,did,mid,sender,text,sent,deleted FROM tetogram_deleted_v1 WHERE deleted>0 ORDER BY mid').fetchall()
        self.assertEqual(rows,[(100,200,10,301,'fixture A',917,2000),(100,200,11,302,'fixture B',918,2000)])
    def test_same_channel_message_id_does_not_cross_accounts(self):
        self.save(account=100,did=-50,channel=50,mid=10,sender=301,text='first account')
        self.save(account=101,did=-50,channel=50,mid=10,sender=302,text='second account')
        self.mark([10],account=100,channel=50)
        self.assertEqual(self.deleted(),[(100,-50,10,2000)])
    def test_received_then_remote_deleted(self):
        self.save();self.mark([1]);self.assertEqual(self.deleted(),[(100,200,1,2000)])
    def test_multiple_messages_and_original_order(self):
        for mid in [4,1,3,2]:self.save(mid=mid)
        self.mark([3,1,4]);self.assertEqual([r[2] for r in self.deleted()],[1,3,4])
    def test_basic_group(self):
        self.save(did=-50);self.mark([1]);self.assertEqual(self.deleted()[0][1],-50)
    def test_channel_namespace_collision(self):
        self.save(did=-50,channel=50);self.save(did=-60,channel=60);self.save()
        self.mark([1],channel=50);self.assertEqual(self.deleted(),[(100,-50,1,2000)])
    def test_closed_app_and_restart(self):
        self.save();self.db.commit();self.db.close();self.db=sqlite3.connect(self.path)
        self.mark([1]);self.db.commit();self.db.close();self.db=sqlite3.connect(self.path)
        self.assertEqual(self.deleted(),[(100,200,1,2000)])
    def test_unknown_deletion_does_not_invent_content(self):
        self.mark([99]);self.assertEqual(self.deleted(),[])
    def test_clear_is_local(self):
        self.db.execute('CREATE TABLE stock_server_cache (mid INTEGER)');self.db.execute('INSERT INTO stock_server_cache VALUES(1)')
        self.save();self.mark([1]);self.db.execute('DELETE FROM tetogram_deleted_v1 WHERE account=100')
        self.assertEqual(self.deleted(),[]);self.assertEqual(self.db.execute('SELECT mid FROM stock_server_cache').fetchone()[0],1)
    def test_two_accounts(self):
        self.save();self.save(account=101);self.mark([1]);self.assertEqual(self.deleted(),[(100,200,1,2000)])
    def test_repeated_delete_and_receive_have_no_duplicates(self):
        self.save();self.mark([1]);self.mark([1],time=3000);self.save()
        self.assertEqual(self.deleted(),[(100,200,1,2000)])
        self.assertEqual(self.db.execute('SELECT count(*) FROM tetogram_deleted_v1').fetchone()[0],1)
    def test_retention_and_casefolded_russian_search(self):
        self.save(mid=1,received=1);self.save(mid=2,received=9999999)
        self.db.execute('DELETE FROM tetogram_deleted_v1 WHERE received<?',(9999999-7*86400,))
        self.assertEqual(self.db.execute('SELECT mid FROM tetogram_deleted_v1 WHERE instr(search_text,?)>0',('ПРИВЕТ'.lower(),)).fetchall(),[(2,)])
    def test_forum_topic_separation(self):
        self.save(mid=1,channel=50,did=-50,topic=7);self.save(mid=2,channel=50,did=-50,topic=8)
        self.mark([1,2],channel=50)
        self.assertEqual(self.db.execute('SELECT mid FROM tetogram_deleted_v1 WHERE topic=7').fetchall(),[(1,)])

if __name__=='__main__':unittest.main(verbosity=2)
