#!/bin/bash
# 云影音乐 · 构建脚本（不依赖 Gradle/AGP：aapt2 + javac + d8 + zipalign + apksigner）
set -e

SDK="$HOME/android-sdk"
BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="${YY_BUILD_DIR:-$ROOT/build}"
PKG="com.yunying.music"
APK_NAME="YunYingMusic-1.9.1.apk"

[ -x "$BT/aapt2" ] || { echo "缺少 build-tools：$BT"; exit 1; }
[ -f "$PLATFORM" ] || { echo "缺少 platform android-34：$PLATFORM"; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT/flat" "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "==> 1/6 编译资源"
"$BT/aapt2" compile --dir "$ROOT/res" -o "$OUT/flat/res.zip"

echo "==> 2/6 链接资源 + 生成 R.java"
"$BT/aapt2" link \
  -o "$OUT/base.apk" \
  -I "$PLATFORM" \
  --manifest "$ROOT/AndroidManifest.xml" \
  -R "$OUT/flat/res.zip" \
  --java "$OUT/gen" \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  --version-code 11 --version-name 1.9.1 \
  --auto-add-overlay

echo "==> 3/6 编译 Java"
find "$OUT/gen" "$ROOT/java" -name '*.java' > "$OUT/sources.txt"
if ! javac -encoding UTF-8 -source 8 -target 8 -nowarn \
  -bootclasspath "$PLATFORM:$BT/core-lambda-stubs.jar" \
  -d "$OUT/classes" @"$OUT/sources.txt"; then
  echo "!! Java 编译失败，终止"; exit 1
fi

echo "==> 4/6 dex"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" --min-api 21 --lib "$PLATFORM" --release \
  --output "$OUT/dex" @"$OUT/classes.txt"

echo "==> 5/6 打包 + 对齐"
python3 - "$OUT/base.apk" "$OUT/dex/classes.dex" "$OUT/unsigned.apk" "$ROOT/../tools" <<'PY'
import sys, zipfile, shutil
base, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
shutil.copyfile(base, out)
with zipfile.ZipFile(out, 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(dex, 'classes.dex')
PY
"$BT/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "==> 6/6 签名"
KS="$ROOT/keystore/yunying.jks"
if [ ! -f "$KS" ]; then
  mkdir -p "$ROOT/keystore"
  keytool -genkeypair -v -keystore "$KS" -alias yunying \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass yunying123 -keypass yunying123 \
    -dname "CN=YunYing Music, OU=App, O=YunYing, L=Local, C=CN" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:yunying123 --key-pass pass:yunying123 \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$ROOT/$APK_NAME" "$OUT/aligned.apk"
"$BT/apksigner" verify --print-certs "$ROOT/$APK_NAME" | head -3

echo ""
echo "构建完成：$ROOT/$APK_NAME"
ls -lh "$ROOT/$APK_NAME"
