#!/usr/bin/env python3
"""Inspect the built APK using the installed Android build-tools; never logs credentials."""
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET


def main():
    apk = Path(sys.argv[1]).resolve()
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
    tools = sdk / 'build-tools/36.0.0'
    badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)], text=True)
    assert "package: name='org.tetogram.messenger'" in badging, 'Wrong application ID'
    assert "versionName='0.1.0'" in badging, 'Wrong version'
    assert "application-label:'Tetogram'" in badging, 'Wrong label'
    assert "native-code: 'arm64-v8a'" in badging, 'ARM64 ABI missing'
    manifest = subprocess.check_output([str(tools / 'aapt2'), 'dump', 'xmltree', str(apk), '--file', 'AndroidManifest.xml'], text=True)
    authority_lines = [line for line in manifest.splitlines() if 'android:authorities' in line]
    assert authority_lines and all('org.tetogram.messenger.' in line for line in authority_lines), 'Provider authority conflict'
    assert 'tetogram_launcher' in manifest or 'android:icon' in manifest, 'Missing icon attribute'
    # The merged plain-text manifest verifies all resolved component identifiers and icon resource.
    manifests = list(Path('TMessagesProj_App/build/intermediates/merged_manifests').rglob('AndroidManifest.xml'))
    debug = [p for p in manifests if 'afatDebug' in str(p)]
    assert debug, 'Merged debug manifest not found'
    root = ET.parse(debug[0]).getroot()
    android = '{http://schemas.android.com/apk/res/android}'
    assert root.get('package') == 'org.tetogram.messenger'
    app = root.find('application')
    assert app.get(android + 'icon') == '@mipmap/tetogram_launcher', 'Wrong launcher icon'
    assert app.get(android + 'debuggable') == 'true', 'Expected debug APK'
    for permission in root.findall('permission'):
        assert permission.get(android + 'name').startswith('org.tetogram.messenger.'), 'Custom permission conflict'
    with zipfile.ZipFile(apk) as archive:
        libs = [name for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')]
        assert 'lib/arm64-v8a/libtmessages.49.so' in libs, 'MTProto/media/VoIP native library missing'
        assert 'lib/arm64-v8a/libtwidget.so' in libs, 'Widget native library missing'
        assert all(name.startswith('lib/arm64-v8a/') for name in libs), 'Unexpected native ABI'
        for name in libs:
            with archive.open(name) as library:
                header = library.read(64)
            assert header[:6] == b'\x7fELF\x02\x01', 'Expected little-endian ELF64: ' + name
            assert int.from_bytes(header[18:20], 'little') == 183, 'Expected AArch64 ELF: ' + name
        print('Native libraries:', ', '.join(libs))
    subprocess.run([str(tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], check=True)
    print('PASS: APK package, version, label, provider identifiers, signature, icon and ARM64 libraries.')
    print('Runtime behavior and account login still require manual testing.')


if __name__ == '__main__':
    main()
