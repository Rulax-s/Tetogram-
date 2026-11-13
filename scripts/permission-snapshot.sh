#!/usr/bin/env bash
# Read-only ADB diagnostics. Filters system output to version/permission booleans only.
set -euo pipefail
package=org.tetogram.messenger
adb shell getprop ro.build.version.sdk
adb shell dumpsys package "$package" | rg 'versionCode=|versionName=|android.permission.(READ_MEDIA_IMAGES|READ_MEDIA_VIDEO|READ_MEDIA_VISUAL_USER_SELECTED|READ_EXTERNAL_STORAGE|POST_NOTIFICATIONS): granted='
for permission_op in READ_MEDIA_IMAGES READ_MEDIA_VIDEO READ_MEDIA_VISUAL_USER_SELECTED POST_NOTIFICATION; do
  adb shell cmd appops get "$package" "$permission_op" 2>/dev/null | rg 'allow|deny|ignore|foreground|No operations' || true
done
