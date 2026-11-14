#!/usr/bin/env python3
"""Tetogram XML, build inputs, and optional APK language-pack/binding contract."""
from pathlib import Path
import re,sys,struct,zipfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
def values(locale):
 result={}
 for p in (ROOT/'TMessagesProj/src/main/res'/locale).glob('*.xml'):
  for item in ET.parse(p).getroot():
   name=item.get('name','')
   if item.tag=='string' and name.startswith('Tetogram'):
    assert name not in result,('duplicate',locale,name)
    result[name]=''.join(item.itertext()).strip()
 return result
en,ru=values('values'),values('values-ru')
assert en and en.keys()==ru.keys(),('RU/EN mismatch',en.keys()-ru.keys(),ru.keys()-en.keys())
assert all(en.values()) and all(ru.values())
for p in (ROOT/'TMessagesProj/src/main/java').rglob('*.java'):
 for key in re.findall(r'R\.string\.(Tetogram\w+)',p.read_text()):assert key in en,('missing',p,key)
plugin=(ROOT/'buildSrc/src/main/kotlin/org/telegram/plugin/TelegramBuildAppPlugin.kt').read_text()
assert 'include("strings.xml", "tetogram*.xml")' in plugin
assert 'include("values-*/strings.xml", "values-*/tetogram*.xml")' in plugin
def hashcode(key):
 h=0
 for c in key:h=(h*31+ord(c))&0xffffffff
 return h
def pack(data):
 count=struct.unpack_from('<I',data)[0];at=4;out={}
 for _ in range(count):
  h=struct.unpack_from('<I',data,at)[0];at+=4;n=data[at];at+=1;head=1
  if n==254:n=int.from_bytes(data[at:at+3],'little');at+=3;head=4
  text=data[at:at+n].decode('utf8');at+=n;at+=(-(n+head))%4;out[h]=text
 assert at==len(data)
 return out
if len(sys.argv)>1:
 with zipfile.ZipFile(sys.argv[1]) as z:
  ep=pack(z.read('assets/localization_en.bin'));rp=pack(z.read('assets/localization_ru.bin'))
  b=z.read('assets/string_resource_ids.bin');count=struct.unpack_from('<I',b)[0];assert len(b)==4+count*8
  bound={struct.unpack_from('<II',b,4+i*8)[1] for i in range(count)}
  for k in en:
   h=hashcode(k);assert h in bound,('unbound resource',k)
   assert ep.get(h) and not ep[h].startswith('LOC_ERR'),('English pack',k)
   assert rp.get(h) and not rp[h].startswith('LOC_ERR'),('Russian pack',k)
 print('PASS: all',len(en),'Tetogram strings exist in actual APK EN/RU language packs and resource bindings.')
else:print('PASS: all',len(en),'Tetogram resource keys have nonempty EN/RU text and are included in pack generation.')
