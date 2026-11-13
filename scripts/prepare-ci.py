#!/usr/bin/env python3
"""Validate CI secrets without printing them; materialize only the ignored Firebase file."""
import base64
import json
import os
from pathlib import Path
import re
import sys


def main():
    api_id = os.environ.get('TELEGRAM_API_ID', '').strip()
    api_hash = os.environ.get('TELEGRAM_API_HASH', '').strip()
    if not re.fullmatch(r'[1-9][0-9]{0,8}', api_id) or api_id == '4':
        raise ValueError('Set your own TELEGRAM_API_ID repository secret.')
    if not re.fullmatch(r'[a-fA-F0-9]{32}', api_hash):
        raise ValueError('Set TELEGRAM_API_HASH repository secret.')
    encoded = os.environ.get('GOOGLE_SERVICES_JSON_BASE64', '')
    if not encoded:
        raise ValueError('Set GOOGLE_SERVICES_JSON_BASE64 repository secret.')
    try:
        raw = base64.b64decode(''.join(encoded.split()), validate=True)
        config = json.loads(raw)
        clients = config['client']
        packages = [c['client_info']['android_client_info']['package_name'] for c in clients]
        number = str(config['project_info']['project_number'])
    except Exception:
        raise ValueError('Firebase secret must contain base64 of a valid google-services.json.') from None
    if 'org.tetogram.messenger' not in packages or number == '760348033671':
        raise ValueError('Use your own Firebase project with Android package org.tetogram.messenger.')
    path = Path(__file__).resolve().parents[1] / 'TMessagesProj_App/google-services.json'
    path.write_bytes(raw)
    path.chmod(0o600)
    # A stable optional key lets test APKs update without uninstalling account data.
    key = os.environ.get('DEBUG_KEYSTORE_BASE64', '')
    if key:
        try:
            key_bytes = base64.b64decode(key, validate=True)
        except Exception:
            raise ValueError('DEBUG_KEYSTORE_BASE64 is invalid.') from None
        key_path = Path(__file__).resolve().parents[1] / 'TMessagesProj/config/tetogram-debug.keystore'
        if key_path.exists() and key_path.read_bytes() != key_bytes:
            raise ValueError('Refusing to replace an existing Tetogram debug keystore.')
        key_path.parent.mkdir(parents=True, exist_ok=True)
        key_path.write_bytes(key_bytes)
        key_path.chmod(0o600)
    else:
        raise ValueError('Set DEBUG_KEYSTORE_BASE64 to keep Tetogram update signatures stable.')
    print('Owner configuration validated; Firebase package matches org.tetogram.messenger. Secret values were not printed.')


if __name__ == '__main__':
    try:
        main()
    except ValueError as exc:
        print(f'Configuration error: {exc}', file=sys.stderr)
        sys.exit(1)
