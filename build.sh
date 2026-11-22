#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only
# Copyright (c) 2026 hangothanhh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

resolve_tool() {
  local name="$1"
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/$name" ]]; then
    printf '%s\n' "$JAVA_HOME/bin/$name"
    return 0
  fi
  if command -v "$name" >/dev/null 2>&1; then
    command -v "$name"
    return 0
  fi
  echo "Error: '$name' not found. Install JDK 17 and set JAVA_HOME." >&2
  exit 1
}

resolve_android_sdk() {
  local cand
  for cand in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Android/Sdk" "/usr/local/lib/android/sdk" "/opt/android-sdk"; do
    if [[ -n "$cand" && -d "$cand/platforms" ]]; then
      printf '%s\n' "$cand"
      return 0
    fi
  done
  echo "Error: Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT." >&2
  exit 1
}

JAVAC="$(resolve_tool javac)"
JAR="$(resolve_tool jar)"
KEYTOOL="$(resolve_tool keytool)"
PYTHON_BIN="$(command -v python3 || command -v python)"
if [[ -z "$PYTHON_BIN" ]]; then
  echo "Error: python3 not found." >&2
  exit 1
fi

ANDROID_SDK="$(resolve_android_sdk)"

ANDROID_JAR=""
if [[ -f "$ANDROID_SDK/platforms/android-34/android.jar" ]]; then
  ANDROID_JAR="$ANDROID_SDK/platforms/android-34/android.jar"
else
  while IFS= read -r pdir; do
    if [[ -f "$pdir/android.jar" ]]; then
      ANDROID_JAR="$pdir/android.jar"
      break
    fi
  done < <(ls -1d "$ANDROID_SDK"/platforms/android-* 2>/dev/null | sort -Vr)
fi
if [[ -z "$ANDROID_JAR" ]]; then
  echo "Error: No android.jar found under $ANDROID_SDK/platforms" >&2
  exit 1
fi

BUILD_TOOLS=""
if [[ -d "$ANDROID_SDK/build-tools/34.0.0" ]]; then
  BUILD_TOOLS="$ANDROID_SDK/build-tools/34.0.0"
else
  while IFS= read -r btdir; do
    if [[ -x "$btdir/aapt2" && -x "$btdir/d8" ]]; then
      BUILD_TOOLS="$btdir"
      break
    fi
  done < <(ls -1d "$ANDROID_SDK"/build-tools/* 2>/dev/null | sort -Vr)
fi
if [[ -z "$BUILD_TOOLS" ]]; then
  echo "Error: No valid Android build-tools found under $ANDROID_SDK/build-tools" >&2
  exit 1
fi

AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/d8"
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"

BUILD_DIR="$ROOT/build"
OUT_DIR="$ROOT/out"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/stub-classes" "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$OUT_DIR"

echo "[1/7] Compiling Xposed compile-only stubs..."
STUB_SOURCES=()
while IFS= read -r line; do
  STUB_SOURCES+=("$line")
done < <(find "$ROOT/stubs" -type f -name "*.java" | sort)
"$JAVAC" --release 11 -encoding UTF-8 -cp "$ANDROID_JAR" -d "$BUILD_DIR/stub-classes" "${STUB_SOURCES[@]}"
STUBS_JAR="$BUILD_DIR/xposed-stubs.jar"
"$JAR" cf "$STUBS_JAR" -C "$BUILD_DIR/stub-classes" .

echo "[2/7] Compiling resources with aapt2..."
RES_ZIP="$BUILD_DIR/res.zip"
"$AAPT2" compile --dir "$ROOT/app/src/main/res" -o "$RES_ZIP"

echo "[3/7] Linking APK & generating R.java..."
UNALIGNED_APK="$BUILD_DIR/unaligned.apk"
"$AAPT2" link \
  -o "$UNALIGNED_APK" \
  -I "$ANDROID_JAR" \
  --manifest "$ROOT/app/src/main/AndroidManifest.xml" \
  -A "$ROOT/app/src/main/assets" \
  --min-sdk-version 31 \
  --target-sdk-version 31 \
  --java "$BUILD_DIR/gen" \
  "$RES_ZIP"

echo "[4/7] Compiling app + Xposed module classes..."
APP_SOURCES=()
while IFS= read -r line; do
  APP_SOURCES+=("$line")
done < <(find "$ROOT/app/src/main/java" "$BUILD_DIR/gen" -type f -name "*.java" | sort)
"$JAVAC" --release 11 -encoding UTF-8 -cp "$ANDROID_JAR:$STUBS_JAR" -d "$BUILD_DIR/classes" "${APP_SOURCES[@]}"
APP_JAR="$BUILD_DIR/app-classes.jar"
"$JAR" cf "$APP_JAR" -C "$BUILD_DIR/classes" .

echo "[5/7] Converting classes to classes.dex with d8 (excluding Xposed stubs)..."
"$D8" --min-api 31 --lib "$ANDROID_JAR" --output "$BUILD_DIR/dex" "$APP_JAR"

echo "[6/7] Packaging classes.dex & aligning APK..."
CLASSES_DEX="$BUILD_DIR/dex/classes.dex"
"$PYTHON_BIN" -c "import zipfile; z=zipfile.ZipFile(r'$UNALIGNED_APK','a',zipfile.ZIP_DEFLATED); z.write(r'$CLASSES_DEX','classes.dex'); z.close()"
ALIGNED_APK="$BUILD_DIR/aligned.apk"
"$ZIPALIGN" -f -p 4 "$UNALIGNED_APK" "$ALIGNED_APK"

echo "[7/7] Signing APK..."
if [[ -n "${DEX120_KEYSTORE:-}" ]]; then
  KEYSTORE="$DEX120_KEYSTORE"
elif [[ "${CI:-false}" == "true" ]]; then
  KEYSTORE="$BUILD_DIR/debug.keystore"
else
  KEYSTORE="$ROOT/debug.keystore"
fi
KS_PASS="${DEX120_KEYSTORE_PASS:-android}"
KS_ALIAS="${DEX120_KEY_ALIAS:-dex120}"
KEY_PASS="${DEX120_KEY_PASS:-android}"

if [[ ! -f "$KEYSTORE" ]]; then
  "$KEYTOOL" -genkeypair -v \
    -keystore "$KEYSTORE" \
    -storepass "$KS_PASS" \
    -alias "$KS_ALIAS" \
    -keypass "$KEY_PASS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=DeX120Hz, OU=S22, O=DeX120Hz-S22, C=VN"
fi

FINAL_APK="$OUT_DIR/DeX120Hz-S22.apk"
"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-pass "pass:$KS_PASS" \
  --key-pass "pass:$KEY_PASS" \
  --ks-key-alias "$KS_ALIAS" \
  --out "$FINAL_APK" \
  "$ALIGNED_APK"

"$APKSIGNER" verify --verbose "$FINAL_APK"

SHA_HASH="$("$PYTHON_BIN" -c "import hashlib; print(hashlib.sha256(open(r'$FINAL_APK','rb').read()).hexdigest())")"
printf '%s  DeX120Hz-S22.apk\n' "$SHA_HASH" > "$FINAL_APK.sha256"

echo "Build succeeded: $FINAL_APK"
echo "SHA-256 ($FINAL_APK.sha256): $SHA_HASH"
