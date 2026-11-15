"""Read prepared PNG/JPEG geometry without decoding, converting or editing pixels."""
import struct

def image_size(data):
    if data.startswith(b'\x89PNG\r\n\x1a\n'):
        if len(data)<24 or data[12:16]!=b'IHDR':raise ValueError('Invalid PNG header')
        return struct.unpack('>II',data[16:24])
    if not data.startswith(b'\xff\xd8'):raise ValueError('Prepared PNG or JPEG required')
    at=2;size=None
    while at<len(data):
        if data[at]!=255:raise ValueError('Invalid JPEG marker')
        while at<len(data) and data[at]==255:at+=1
        if at>=len(data):raise ValueError('Truncated JPEG marker')
        marker=data[at];at+=1
        if marker in (0xda,0xd9):break
        if marker==1 or 0xd0<=marker<=0xd8:continue
        if at+2>len(data):raise ValueError('Truncated JPEG length')
        length=int.from_bytes(data[at:at+2],'big')
        if length<2 or at+length>len(data):raise ValueError('Invalid JPEG segment')
        payload=data[at+2:at+length]
        if marker in (0xc0,0xc1,0xc2,0xc3,0xc5,0xc6,0xc7,0xc9,0xca,0xcb,0xcd,0xce,0xcf):
            if len(payload)<5:raise ValueError('Invalid JPEG frame')
            size=(int.from_bytes(payload[3:5],'big'),int.from_bytes(payload[1:3],'big'))
        if marker==0xe1 and payload.startswith(b'Exif\0\0'):
            tiff=payload[6:];order='little' if tiff[:2]==b'II' else 'big' if tiff[:2]==b'MM' else None
            if order is None or len(tiff)<8:raise ValueError('Invalid JPEG EXIF')
            offset=int.from_bytes(tiff[4:8],order)
            if offset+2>len(tiff):raise ValueError('Invalid JPEG EXIF directory')
            count=int.from_bytes(tiff[offset:offset+2],order)
            for n in range(count):
                p=offset+2+n*12
                if p+12>len(tiff):raise ValueError('Truncated JPEG EXIF')
                tag=int.from_bytes(tiff[p:p+2],order)
                if tag==0x8825:raise ValueError('Remove GPS metadata from prepared artwork before publishing')
                if tag==0x112 and int.from_bytes(tiff[p+8:p+10],order)!=1:raise ValueError('Prepared JPEG must have normal orientation')
        at+=length
    if size is None:raise ValueError('JPEG frame missing')
    return size
