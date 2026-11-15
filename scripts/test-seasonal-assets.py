#!/usr/bin/env python3
"""Prepared artwork parsing/copy tests. No pixel editing and no Android UI execution."""
from pathlib import Path
import struct,unittest
from seasonal_image import image_size
ROOT=Path(__file__).resolve().parents[1]
class SeasonalAssets(unittest.TestCase):
 def test_actual_png_and_resaved_jpeg_dimensions(self):
  png=(ROOT/'TMessagesProj/src/main/res/drawable-nodpi/tetogram_teto_spring_art.png').read_bytes()
  jpeg=(ROOT/'assets/launcher/seasonal/tetogram_summer.jpg').read_bytes()
  self.assertEqual((1254,1254),image_size(png));self.assertEqual((1536,1536),image_size(jpeg))
 def test_generated_summer_is_byte_identical_to_prepared_input(self):
  self.assertEqual((ROOT/'assets/launcher/seasonal/tetogram_summer.jpg').read_bytes(),(ROOT/'TMessagesProj/src/main/res/drawable-nodpi/tetogram_summer.jpg').read_bytes())
 def test_invalid_or_truncated_headers_fail_without_guessing(self):
  for data in (b'GIF89a',b'\xff\xd8\xff\xc0\x00\x00',b'\xff\xd8\xff\xc0\x00\x20abc',b'\x89PNG\r\n\x1a\n'):
   with self.assertRaises(ValueError):image_size(data)
 def test_exif_is_never_silently_rotated_or_location_metadata_published(self):
  def jpeg(tag,value):
   tiff=b'II'+struct.pack('<HIH',42,8,1)+struct.pack('<HHII',tag,3,1,value)+b'\0'*4
   app=b'Exif\0\0'+tiff
   frame=b'\x08'+struct.pack('>HH',1536,1536)+b'\x03'
   return b'\xff\xd8\xff\xe1'+struct.pack('>H',len(app)+2)+app+b'\xff\xc0'+struct.pack('>H',len(frame)+2)+frame+b'\xff\xda'
  self.assertEqual((1536,1536),image_size(jpeg(0x112,1)))
  for tag,value in ((0x112,6),(0x8825,0)):
   with self.assertRaises(ValueError):image_size(jpeg(tag,value))
if __name__=='__main__':unittest.main(verbosity=2)
