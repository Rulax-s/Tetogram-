#!/usr/bin/env python3
"""Static identity/configuration checks; does not claim an APK has been built."""
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def read(path):
    return (ROOT / path).read_text()


def main():
    props = read('gradle.properties')
    require('APP_PACKAGE=org.tetogram.messenger' in props, 'Wrong application ID')
    require('APP_VERSION_NAME=0.4.0' in props, 'Wrong version')
    app = read('TMessagesProj_App/build.gradle')
    require('applicationIdSuffix ".beta"' not in app, 'Debug package suffix conflicts with required ID')
    require('abiFilters "armeabi-v7a"' not in app and 'abiFilters "arm64-v8a"' in app, 'Expected ARM64 target')
    debug_signing = app.split('signingConfigs {', 1)[1].split('release {', 1)[0]
    require('release.keystore' not in debug_signing, 'Debug must not use upstream signing key')
    for path in ('ui/IntroActivity.java', 'ui/DialogsActivity.java', 'ui/Stories/DialogStoriesCell.java'):
        source = read('TMessagesProj/src/main/java/org/telegram/' + path)
        require('R.drawable.telegram_logo' not in source, 'Official wordmark remains in app branding')
    for path in (ROOT / 'TMessagesProj/src/main/res').glob('values*/styles.xml'):
        require('@drawable/tg_splash_320' not in path.read_text(), 'Official splash logo remains')
    intro = read('TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java')
    require('R.string.TetogramIntro' in intro and 'R.drawable.intro_tg_plane' not in intro, 'Intro branding/disclosure missing')
    buildvars = read('TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java')
    require('APP_ID = BuildConfig.TETOGRAM_API_ID' in buildvars, 'API ID must come from owner configuration')
    require('APP_HASH = BuildConfig.TETOGRAM_API_HASH' in buildvars, 'API hash must come from owner configuration')
    manifests = [ROOT / 'TMessagesProj/src/main/AndroidManifest.xml', * (ROOT / 'TMessagesProj/config').glob('*/AndroidManifest*.xml')]
    for path in manifests:
        root = ET.parse(path).getroot()
        for node in root.iter():
            authority = node.get(ANDROID + 'authorities')
            if authority:
                require(authority.startswith('${applicationId}.'), f'Non-unique authority in {path.name}')
            if node.tag == 'permission':
                require(node.get(ANDROID + 'name', '').startswith('${applicationId}.'), 'Non-unique custom permission')
            for name in ('icon', 'roundIcon'):
                icon = node.get(ANDROID + name)
                if icon:
                    require(icon == '@mipmap/tetogram_launcher', f'Official launcher icon in {path.name}')
    for name in ('auth.xml', 'sync_contacts.xml'):
        root = ET.parse(ROOT / 'TMessagesProj/src/main/res/xml' / name).getroot()
        require(root.get(ANDROID + 'accountType') == 'org.tetogram.messenger', 'Account type conflict')
    manifest_text = read('TMessagesProj/src/main/AndroidManifest.xml')
    contacts_xml = read('TMessagesProj/src/main/res/xml/contacts.xml')
    launch = read('TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java')
    for kind in ('profile', 'call', 'call.video'):
        mime = 'vnd.android.cursor.item/vnd.org.tetogram.messenger.android.' + kind
        require(mime in manifest_text and mime in contacts_xml, 'Contact MIME routing mismatch')
    require('"org.tetogram.messenger.OPEN_ACCOUNT"' in launch, 'Account settings intent routing mismatch')
    require('vnd.org.telegram.messenger.android.' not in manifest_text, 'Official contact MIME handler remains')
    contacts = read('TMessagesProj/src/main/java/org/telegram/messenger/ContactsController.java')
    require('"org.telegram.messenger"' not in contacts, 'Official Android accounts would be accessed')
    for path in (ROOT / 'TMessagesProj/src/main/res').glob('values*/strings.xml'):
        root = ET.parse(path).getroot()
        for name in ('AppName', 'AppNameBeta'):
            entry = root.find(f"string[@name='{name}']")
            require(entry is not None and entry.text == 'Tetogram', f'Wrong label: {path}')
    tracked = subprocess.check_output(['git', 'ls-files', '--cached'], cwd=ROOT, text=True).splitlines()
    # Removed upstream samples must stay removed; generated/local secrets must stay ignored.
    for path in tracked:
        require(not path.endswith('google-services.json'), 'Firebase configuration is tracked')
        require(path not in ('tetogram.properties', '.env'), 'Local secrets file is tracked')
    for name in ('README.md', 'BUILD.md', 'ARCHITECTURE.md', 'CUSTOMIZATION.md', 'TROUBLESHOOTING.md', 'CHANGELOG.md'):
        path = ROOT / name if name == 'README.md' else ROOT / 'docs' / name
        require(path.is_file(), f'Missing documentation: {name}')
    for path in (ROOT / 'TMessagesProj/src/main/res').glob('**/tetogram*.xml'):
        ET.parse(path)
    if '--submodules' in sys.argv:
        status = subprocess.check_output(['git', 'submodule', 'status', '--recursive'], cwd=ROOT, text=True)
        optional_tests = 'TMessagesProj/jni/twidget/widget-sandbox/packages/ios/Sources/CQuickJS/quickjs/test262'
        checked = 0
        for line in status.splitlines():
            path = line[1:].split()[1]
            if line[0] == '-' and path == optional_tests:
                print('Upstream update=none: optional QuickJS test262 test suite skipped.')
                continue
            require(line.startswith(' '), 'Submodule missing or at wrong SHA: ' + path)
            checked += 1
        require(checked >= 22, 'Required submodules not initialized')
        print('PASS: required submodule checkouts match pinned commits:', checked)
    print('PASS: source identity, account isolation, branding, config and documentation.')
    print('APK compilation, installation, login and Telegram features are NOT tested by this check.')


if __name__ == '__main__':
    main()
