#!/bin/bash
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="$HOME/android-sdk"
BT="$SDK/build-tools/34.0.0"
OUT="/tmp/yy-ui-probe"
rm -rf "$OUT/classes" "$OUT/dex"
mkdir -p "$OUT/classes" "$OUT/dex"
mkdir -p "$OUT/assets/formats"
if [ -d /tmp/yy-effects-formats ]; then cp /tmp/yy-effects-formats/tone.* "$OUT/assets/formats/"; fi
"$BT/aapt2" link -A "$OUT/assets" -o "$OUT/base.apk" -I "$SDK/platforms/android-34/android.jar" --manifest "$ROOT/AndroidManifest.xml"
javac -encoding UTF-8 -source 8 -target 8 -nowarn -bootclasspath "$SDK/platforms/android-34/android.jar:$BT/core-lambda-stubs.jar" -d "$OUT/classes" "$ROOT/Probe.java" "$ROOT/MotionProbe.java" "$ROOT/FocusMotionProbe.java" "$ROOT/NasOnlyProbe.java" "$ROOT/SoundChecks.java" "$ROOT/SoundProbe.java" "$ROOT/MetadataProbe.java" "$ROOT/../../java/com/yunying/music/MusicLibrary.java" "$ROOT/../../java/com/yunying/music/OnlineMeta.java" "$ROOT/../../java/com/yunying/music/SoundEffects.java" "$ROOT/../../java/com/yunying/music/"{Http,B64,Config,Entry}.java
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" --min-api 21 --lib "$SDK/platforms/android-34/android.jar" --output "$OUT/dex" @"$OUT/classes.txt"
python3 - "$OUT" <<'PY'
import sys,zipfile,shutil
from pathlib import Path
p=Path(sys.argv[1]);shutil.copy(p/'base.apk',p/'unsigned.apk')
with zipfile.ZipFile(p/'unsigned.apk','a') as z:z.write(p/'dex/classes.dex','classes.dex')
PY
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$ROOT/../../keystore/yunying.jks" --ks-pass pass:yunying123 --out "$OUT/probe.apk" "$OUT/aligned.apk"
PROBE_ADB_ARGS=()
if [ -n "${YY_ADB_PORT:-}" ]; then PROBE_ADB_ARGS+=(-P "$YY_ADB_PORT"); fi
if [ -n "${YY_ADB_SERIAL:-}" ]; then PROBE_ADB_ARGS+=(-s "$YY_ADB_SERIAL"); fi
"$SDK/platform-tools/adb" "${PROBE_ADB_ARGS[@]}" install -r "$OUT/probe.apk"
