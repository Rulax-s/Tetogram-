#!/usr/bin/env python3
"""Inspect the built APK using the installed Android build-tools; never logs credentials."""
import os
from pathlib import Path
import json
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET


ANDROID = '{http://schemas.android.com/apk/res/android}'
PACKAGE = 'org.tetogram.messenger'


def verify_manifest(root):
    """Check decoded APK XML, not the presentation format of aapt2's tree dump."""
    assert root.get('package') == PACKAGE, 'Wrong APK manifest package'
    app = root.find('application')
    assert app is not None and app.get(ANDROID + 'icon'), 'Missing APK application icon'
    assert app.get(ANDROID + 'debuggable') == 'true', 'Expected debug APK'
    providers = [
        {'name': p.get(ANDROID + 'name'), 'authorities': p.get(ANDROID + 'authorities', '')}
        for p in app.findall('provider')
    ]
    # Only component identifiers are logged, never metadata or the full manifest.
    print('APK providers:', json.dumps(providers, ensure_ascii=True))
    assert providers, 'No providers in APK manifest'
    for provider in providers:
        authorities = provider['authorities'].split(';')
        assert all(a.startswith(PACKAGE + '.') for a in authorities), (
            'Provider authority conflict: ' + json.dumps(provider))
    for permission in root.findall('permission'):
        name = permission.get(ANDROID + 'name', '')
        assert name.startswith(PACKAGE + '.'), 'Custom permission conflict: ' + name


def main():
    apk = Path(sys.argv[1]).resolve()
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
    tools = sdk / 'build-tools/36.0.0'
    badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)], text=True)
    assert "package: name='org.tetogram.messenger'" in badging, 'Wrong application ID'
    assert "versionName='0.3.0'" in badging, 'Wrong version'
    assert "application-label:'Tetogram'" in badging, 'Wrong label'
    assert "native-code: 'arm64-v8a'" in badging, 'ARM64 ABI missing'
    analyzers = [sdk / 'cmdline-tools/latest/bin/apkanalyzer', *sdk.glob('cmdline-tools/*/bin/apkanalyzer')]
    analyzer = next((p for p in analyzers if p.is_file()), None)
    assert analyzer is not None, 'Install Android SDK Command-Line Tools (apkanalyzer)'
    manifest = subprocess.check_output([str(analyzer), 'manifest', 'print', str(apk)], text=True)
    verify_manifest(ET.fromstring(manifest))
    # The merged plain-text manifest verifies all resolved component identifiers and icon resource.
    manifests = list(Path('TMessagesProj_App/build/intermediates/merged_manifests').rglob('AndroidManifest.xml'))
    debug = [p for p in manifests if 'afatDebug' in str(p)]
    assert debug, 'Merged debug manifest not found'
    root = ET.parse(debug[0]).getroot()
    android = '{http://schemas.android.com/apk/res/android}'
    assert root.get('package') == 'org.tetogram.messenger'
    app = root.find('application')
    assert app.get(android + 'icon') == '@mipmap/tetogram_launcher', 'Wrong launcher icon'
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
