#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
# Copyright (c) 2026 hangothanhh
"""Verify DeX120Hz-S22 APK contents, DEX class definitions, SHA-256, and host unit tests."""

import hashlib
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "out"
BUILD_DIR = ROOT / "build"
APK_PATH = OUT_DIR / "DeX120Hz-S22.apk"
SHA_PATH = OUT_DIR / "DeX120Hz-S22.apk.sha256"

REQUIRED_APK_ENTRIES = (
    "AndroidManifest.xml",
    "classes.dex",
    "assets/xposed_init",
    "resources.arsc",
)

EXPECTED_XPOSED_INIT = "com.dex120.s22.xposed.HookEntry"
REQUIRED_HOOK_DESCRIPTORS = (
    "Lcom/dex120/s22/xposed/HookEntry;",
    "Lcom/dex120/s22/xposed/DpSysfsHook;",
    "Lcom/dex120/s22/xposed/LowRefreshTokenHook;",
    "Lcom/dex120/s22/xposed/DisplayModeHook;",
    "Lcom/dex120/s22/xposed/VirtualDisplaySyncHook;",
    "Lcom/dex120/s22/xposed/SdhmsDfsHook;",
)
FORBIDDEN_CLASS_DEF_PREFIX = "Lde/robv/android/xposed/"


def read_uleb128(buf: bytes, offset: int) -> tuple[int, int]:
    result = 0
    shift = 0
    while True:
        byte = buf[offset]
        offset += 1
        result |= (byte & 0x7F) << shift
        if (byte & 0x80) == 0:
            return result, offset
        shift += 7


def parse_dex_class_defs(dex_bytes: bytes) -> list[str]:
    if not dex_bytes.startswith(b"dex\n"):
        raise AssertionError("classes.dex does not start with valid DEX magic")
    string_ids_size, string_ids_off = struct.unpack_from("<II", dex_bytes, 56)
    type_ids_size, type_ids_off = struct.unpack_from("<II", dex_bytes, 64)
    class_defs_size, class_defs_off = struct.unpack_from("<II", dex_bytes, 96)

    def get_string(idx: int) -> str:
        str_data_off = struct.unpack_from("<I", dex_bytes, string_ids_off + idx * 4)[0]
        utf16_len, pos = read_uleb128(dex_bytes, str_data_off)
        _ = utf16_len
        end = dex_bytes.index(0, pos)
        return dex_bytes[pos:end].decode("utf-8", errors="replace")

    class_names = []
    for i in range(class_defs_size):
        class_idx = struct.unpack_from("<I", dex_bytes, class_defs_off + i * 32)[0]
        if class_idx >= type_ids_size:
            raise AssertionError(f"Invalid type_idx {class_idx}")
        descriptor_idx = struct.unpack_from("<I", dex_bytes, type_ids_off + class_idx * 4)[0]
        if descriptor_idx >= string_ids_size:
            raise AssertionError(f"Invalid string_idx {descriptor_idx}")
        class_names.append(get_string(descriptor_idx))
    return class_names


def verify_apk_and_dex() -> None:
    if not APK_PATH.is_file():
        raise AssertionError(f"APK missing: {APK_PATH}")

    with zipfile.ZipFile(APK_PATH, "r") as zf:
        names = set(zf.namelist())
        for req in REQUIRED_APK_ENTRIES:
            if req not in names:
                raise AssertionError(f"Missing required APK entry: {req}")

        xposed_init = zf.read("assets/xposed_init").decode("utf-8").strip()
        if xposed_init != EXPECTED_XPOSED_INIT:
            raise AssertionError(f"Unexpected assets/xposed_init: {xposed_init!r}")

        dex_bytes = zf.read("classes.dex")

    class_defs = parse_dex_class_defs(dex_bytes)
    class_set = set(class_defs)
    for req_desc in REQUIRED_HOOK_DESCRIPTORS:
        if req_desc not in class_set:
            raise AssertionError(f"Missing class_def in classes.dex: {req_desc}")

    stub_defs = [c for c in class_defs if c.startswith(FORBIDDEN_CLASS_DEF_PREFIX)]
    if stub_defs:
        raise AssertionError(f"Xposed stubs leaked into classes.dex: {stub_defs}")

    app_jar = BUILD_DIR / "app-classes.jar"
    if app_jar.is_file():
        with zipfile.ZipFile(app_jar, "r") as jf:
            leaked = [n for n in jf.namelist() if n.startswith("de/robv/android/xposed/")]
            if leaked:
                raise AssertionError(f"Xposed stubs leaked into app-classes.jar: {leaked}")

    print(f"[OK] APK structure & DEX verified ({len(class_defs)} class_defs, 0 Xposed stubs)")


def verify_sha256() -> None:
    if not SHA_PATH.is_file():
        raise AssertionError(f"Missing SHA-256 file: {SHA_PATH}")
    actual = hashlib.sha256(APK_PATH.read_bytes()).hexdigest().lower()
    recorded_line = SHA_PATH.read_text(encoding="utf-8").strip()
    recorded = recorded_line.split()[0].lower() if recorded_line else ""
    if actual != recorded:
        raise AssertionError(f"SHA-256 mismatch: file={recorded} actual={actual}")
    print(f"[OK] SHA-256 verified: {actual}")


def find_jdk_tool(name: str) -> str:
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        for ext in ("", ".exe"):
            cand = Path(java_home) / "bin" / f"{name}{ext}"
            if cand.is_file():
                return str(cand)
    win_default = Path(r"C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot\bin") / f"{name}.exe"
    if win_default.is_file():
        return str(win_default)
    found = shutil.which(name)
    if found:
        return found
    raise AssertionError(f"JDK tool '{name}' not found")


def run_sanitizer_tests() -> None:
    javac = find_jdk_tool("javac")
    java = find_jdk_tool("java")

    root_shell = ROOT / "app/src/main/java/com/dex120/s22/RootShell.java"
    mod_status = ROOT / "app/src/main/java/com/dex120/s22/ModuleStatus.java"
    test_src = ROOT / "tests/SanitizerTest.java"

    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(
            [javac, "--release", "11", "-encoding", "UTF-8", "-d", tmp, str(root_shell), str(mod_status), str(test_src)],
            check=True,
        )
        res = subprocess.run(
            [java, "-cp", tmp, "SanitizerTest"],
            check=True,
            capture_output=True,
            text=True,
        )
        for line in res.stdout.strip().splitlines():
            print(line)


def find_android_jar() -> str:
    candidates: list[Path] = []
    for env_key in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        val = os.environ.get(env_key)
        if val:
            candidates.append(Path(val))
    candidates.extend([
        Path(r"C:\Android\Sdk"),
        Path("/usr/local/lib/android/sdk"),
        Path.home() / "Android" / "Sdk",
    ])
    for sdk_root in candidates:
        platforms = sdk_root / "platforms"
        if not platforms.is_dir():
            continue
        jars: list[tuple[int, Path]] = []
        for entry in platforms.iterdir():
            if entry.is_dir() and entry.name.startswith("android-"):
                suffix = entry.name[len("android-"):]
                if suffix.isdigit() and int(suffix) >= 31:
                    jar = entry / "android.jar"
                    if jar.is_file():
                        jars.append((int(suffix), jar))
        if jars:
            jars.sort(key=lambda item: item[0], reverse=True)
            return str(jars[0][1])
    raise AssertionError("Android SDK android.jar (API >= 31) not found")


def run_hook_logic_tests() -> None:
    javac = find_jdk_tool("javac")
    java = find_jdk_tool("java")
    android_jar = find_android_jar()

    app_jar = BUILD_DIR / "app-classes.jar"
    stubs_jar = BUILD_DIR / "xposed-stubs.jar"
    test_src = ROOT / "tests/HookLogicTest.java"
    if not app_jar.is_file() or not stubs_jar.is_file():
        raise AssertionError("Missing build/app-classes.jar or build/xposed-stubs.jar (run build script first)")

    compile_cp = os.pathsep.join([str(app_jar), str(stubs_jar), android_jar])
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(
            [javac, "--release", "11", "-encoding", "UTF-8", "-cp", compile_cp, "-d", tmp, str(test_src)],
            check=True,
        )
        run_cp = os.pathsep.join([tmp, str(app_jar), str(stubs_jar), android_jar])
        res = subprocess.run(
            [java, "-cp", run_cp, "HookLogicTest"],
            check=True,
            capture_output=True,
            text=True,
        )
        for line in res.stdout.strip().splitlines():
            print(line)


def main() -> int:
    verify_apk_and_dex()
    verify_sha256()
    run_sanitizer_tests()
    run_hook_logic_tests()
    print("[PASS] All DeX120Hz-S22 checks succeeded.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
