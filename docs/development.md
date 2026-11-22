# Building from Source Guide

**Language:** **English** | [Tiếng Việt](vi/development.md)

This guide is for developers and auditors who want to inspect the source, compile `DeX120Hz-S22.apk` locally, and run automated verification — matching the actual build scripts in this repository (`build.ps1` and `build.sh`).

---

## 1. Standalone Build Pipeline (Zero Gradle)

**DeX 120Hz S22** does **not** use Gradle. Compilation invokes JDK 17 and Android SDK tools directly:
1. Compiles clean-room API stubs under `stubs/de/robv/android/xposed/` into `build/xposed-stubs.jar` (used strictly so `javac` resolves Xposed method signatures; **never** packaged into the APK so LSPosed provides them at runtime).
2. Compiles Android resources (`app/src/main/res/`, `AndroidManifest.xml`, `assets/xposed_init`) via **`aapt2`**.
3. Compiles Java application sources (`app/src/main/java/`) targeting `--release 11`, **`minSdk 31` / `targetSdk 31`**.
4. Compiles `build/app-classes.jar` to `classes.dex` via **`d8`** (**excluding** `xposed-stubs.jar` completely).
5. Bundles `classes.dex` using Python `zipfile`, aligns with **`zipalign`**, and signs with **`apksigner`**.

---

## 2. Build Requirements

| Component | Script Requirements | Notes |
|---|---|---|
| **Operating System** | Windows (`build.ps1`) or Linux / macOS (`build.sh`) | GitHub Actions CI runs on `ubuntu-latest`. |
| **Java (JDK)** | JDK **17** (`javac`, `jar`, `keytool` supporting `--release 11`) | Set `JAVA_HOME` (on Windows `build.ps1` also auto-discovers standard Microsoft JDK 17 paths or `PATH`). |
| **Android SDK Platform** | `android-34` (or any installed `android-* >= 31` with `android.jar`) | Set `ANDROID_HOME` or `ANDROID_SDK_ROOT`. |
| **Android SDK Build-Tools** | `34.0.0` (containing `aapt2`, `d8`, `zipalign`, `apksigner`) | Auto-detects the newest version under `$ANDROID_HOME/build-tools/`. |
| **Python** | Python 3 (standard `zipfile` + `scripts/verify_apk.py`) | Zero external `pip install` dependencies required. |
| **Android NDK / Rust / CMake** | **Not used** | Pure Java + Android SDK project. |
| **Disk Space** | ~50 MB for source + outputs (excluding existing JDK & Android SDK) | Clean build completes in seconds. |

---

## 3. Build & Verification Steps

### Step 1: Clone the Repository
*(No Git submodules are used.)*
```bash
git clone https://github.com/hangothanhh/DeX120Hz-S22-Exynos.git
cd DeX120Hz-S22-Exynos
```

### Step 2: Run the Build Script

- **Windows (PowerShell):**
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\build.ps1
  ```
- **Linux / macOS:**
  ```bash
  chmod +x ./build.sh
  ./build.sh
  ```

**Generated outputs under `out/`:**
- `out/DeX120Hz-S22.apk`
- `out/DeX120Hz-S22.apk.sha256`

### Step 3: Verify Signature, DEX Structure & Unit Tests

1. Verify APK signature with `apksigner` *(`build.sh` runs this automatically)*:
   - **Windows:**
     ```powershell
     & "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat" verify --verbose out\DeX120Hz-S22.apk
     ```
   - **Linux / macOS:**
     ```bash
     "$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose out/DeX120Hz-S22.apk
     ```
2. Run the automated verification suite `scripts/verify_apk.py`:
   ```bash
   python scripts/verify_apk.py      # or python3 scripts/verify_apk.py on Linux/macOS
   ```
   This script automatically checks:
   - `out/DeX120Hz-S22.apk` ZIP entries (`AndroidManifest.xml`, `classes.dex`, `assets/xposed_init`, `resources.arsc`).
   - `assets/xposed_init` equals `com.dex120.s22.xposed.HookEntry`.
   - Parses `classes.dex` headers & `class_defs` table: confirms presence of `HookEntry`, `DpSysfsHook`, `LowRefreshTokenHook`, `DisplayModeHook`, `VirtualDisplaySyncHook`, `SdhmsDfsHook`, and **0 `Lde/robv/android/xposed/...` stub classes** inside `classes.dex`.
   - Matches APK SHA-256 against `out/DeX120Hz-S22.apk.sha256`.
   - Compiles and runs `tests/SanitizerTest.java` on host JVM to confirm `RootShell.sanitizeDiagnostics()` redacts Serials, IMEIs, MACs, private IPs, Emails, and Tokens/Passwords while keeping benign diagnostic markers intact.

---

## 4. Signing Keystore Security

- By default, if no custom keystore is supplied, `build.ps1` / `build.sh` generates a local `debug.keystore` (or `build/debug.keystore` on CI) with standard debug credentials.
- `*.keystore` and `*.jks` are ignored by `.gitignore` — **never** commit signing keys to Git.
- To sign with a personal release keystore outside the repo, export these environment variables before running the build:
  ```bash
  export DEX120_KEYSTORE="/path/outside/repo/my-release.jks"
  export DEX120_KEYSTORE_PASS="..."
  export DEX120_KEY_ALIAS="..."
  export DEX120_KEY_PASS="..."
  ./build.sh
  ```
- Note: switching signing keys requires uninstalling any prior APK signed with the old key before installing the newly signed build.
