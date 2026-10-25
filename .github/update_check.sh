#!/bin/bash
# THE FORGE  |  Copyright (c) 2026 Yannie D. Forest. All rights reserved.
# NO LICENCE IS GRANTED. See LICENCE.txt.
#
# 647: the Forge on a tablet, updated as a person updates it. The earlier APK (from the Releases page, signed with the old
# key) is installed and opened; this build's APK is installed over it and must be taken as an update of the same app (its
# first install time kept, so what the app stored stays); then the earlier APK must be refused over the new one, because the
# new key took over and the old key alone can no longer replace it.
#   bash .github/update_check.sh <earlier.apk> <new.apk>
set -u
PREV="$1"; NEW="$2"; PKG=com.yannieforest.theforge; FAIL=0
first() { adb shell dumpsys package "$PKG" | grep -m1 firstInstallTime | tr -d '\r' | sed 's/^ *//'; }
adb wait-for-device
if [ -f "$PREV" ]; then
  adb install "$PREV" || { echo "::error::the earlier APK did not install on the emulator"; exit 1; }
  adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1; sleep 12
  T0=$(first); echo "the earlier APK installed and opened: $T0"
  OUT=$(adb install -r "$NEW" 2>&1); echo "$OUT"
  echo "$OUT" | grep -q Success || { echo "::error::this build did not install over the earlier one (as on the tablet)"; FAIL=1; }
  T1=$(first); echo "after the update: $T1"
  [ -n "$T0" ] && [ "$T0" = "$T1" ] || { echo "::error::the app was installed afresh rather than updated, so what it stored would be lost"; FAIL=1; }
  OUT=$(adb install -r -d "$PREV" 2>&1); echo "$OUT"
  if echo "$OUT" | grep -q Success; then echo "::error::an APK signed with the old key alone still installs over the new one"; FAIL=1;
  else echo "the old key alone is refused now: $(echo "$OUT" | grep -o 'INSTALL_FAILED[A-Z_]*' | head -1)"; fi
else
  echo "no earlier APK: this build is installed on its own"
  adb install "$NEW" | grep -q Success || { echo "::error::this build did not install on the emulator"; FAIL=1; }
fi
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1; sleep 12
adb shell pidof "$PKG" > /dev/null && echo "the Forge is running after the update" || { echo "::error::the Forge did not stay open after the update"; FAIL=1; }
exit $FAIL
