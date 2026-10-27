#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SDK="$HOME/android-sdk"
BT="$SDK/build-tools/34.0.0"
APP_OUT="${YY_BUILD_DIR:-/tmp/yy-cloud-playback-build}"
OUT=/tmp/yy-cloud-probe
rm -rf "$OUT/classes" "$OUT/dex"
mkdir -p "$OUT/classes" "$OUT/dex"
cp -R "$APP_OUT/classes/." "$OUT/classes/"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$SDK/platforms/android-34/android.jar" --manifest "$ROOT/test/cloud-playback/AndroidManifest.xml" -R "$APP_OUT/flat/res.zip" --auto-add-overlay
javac -encoding UTF-8 -source 8 -target 8 -nowarn -bootclasspath "$SDK/platforms/android-34/android.jar:$BT/core-lambda-stubs.jar" -classpath "$OUT/classes" -d "$OUT/classes" "$ROOT/test/cloud-playback/CloudPlaybackProbe.java"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" --min-api 23 --lib "$SDK/platforms/android-34/android.jar" --output "$OUT/dex" @"$OUT/classes.txt"
python3 - "$OUT" <<'PY'
import sys,zipfile,shutil
from pathlib import Path
p=Path(sys.argv[1]);shutil.copy(p/'base.apk',p/'unsigned.apk')
with zipfile.ZipFile(p/'unsigned.apk','a') as z:z.write(p/'dex/classes.dex','classes.dex')
PY
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$ROOT/keystore/yunying.jks" --ks-pass pass:yunying123 --out "$OUT/probe.apk" "$OUT/aligned.apk"
"$SDK/platform-tools/adb" install -r "$OUT/probe.apk"
