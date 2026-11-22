# Hướng dẫn Tự Build từ Mã nguồn (Build from Source)

Tài liệu này dành cho lập trình viên hoặc người dùng muốn tự kiểm tra mã nguồn, tự biên dịch file `DeX120Hz-S22.apk` và chạy kiểm thử tự động — dựa chính xác trên các script build thực tế trong repository (`build.ps1` và `build.sh`).

---

## 1. Kiến trúc Build đặc biệt của dự án

**DeX 120Hz S22** **không dùng Gradle**. Quy trình biên dịch dùng trực tiếp các công cụ nhẹ từ JDK 17 và Android SDK:
1. Biên dịch compile-only stub trong `stubs/de/robv/android/xposed/` thành `build/xposed-stubs.jar` (chỉ để `javac` nhận biết API Xposed lúc compile, **không** đóng gói vào APK để tránh đụng độ class với LSPosed lúc runtime).
2. Biên dịch tài nguyên (`app/src/main/res/`, `AndroidManifest.xml`, `assets/xposed_init`) bằng **`aapt2`**.
3. Biên dịch mã nguồn Java (`app/src/main/java/`) với `--release 11`, **`minSdk 31` / `targetSdk 31`**.
4. Chuyển bytecode sang `classes.dex` bằng **`d8`** (chỉ từ `build/app-classes.jar` — **loại trừ hoàn toàn** `xposed-stubs.jar`).
5. Nhúng `classes.dex` vào APK bằng Python `zipfile`, căn chỉnh bằng **`zipalign`**, và ký APK bằng **`apksigner`**.

---

## 2. Yêu cầu Môi trường Build

| Thành phần | Yêu cầu thực tế theo script | Ghi chú |
|---|---|---|
| **Hệ điều hành** | Windows (`build.ps1`) hoặc Linux / macOS (`build.sh`) | CI của dự án chạy trên `ubuntu-latest`. |
| **Java (JDK)** | JDK **17** (hỗ trợ `--release 11`: `javac`, `jar`, `keytool`) | Đặt biến môi trường `JAVA_HOME` (trên Windows script cũng tự dò đường dẫn Microsoft JDK 17 mặc định hoặc `PATH`). |
| **Android SDK Platform** | `android-34` (hoặc bản `android-*` bất kỳ `>= 31` có `android.jar`) | Đặt `ANDROID_HOME` hoặc `ANDROID_SDK_ROOT`. |
| **Android SDK Build-Tools** | `34.0.0` (hoặc bản có `aapt2`, `d8`, `zipalign`, `apksigner`) | Tự dò phiên bản mới nhất trong `$ANDROID_HOME/build-tools/`. |
| **Python** | Python 3 (dùng `zipfile` chuẩn và chạy `scripts/verify_apk.py`) | Không cần `pip install` thư viện ngoài. |
| **Android NDK / Rust / CMake** | **Không dùng** | Dự án thuần Java + Android SDK. |
| **Dung lượng trống** | ~50 MB cho repo + output (chưa tính Android SDK & JDK có sẵn) | Build hoàn tất trong vài giây. |

---

## 3. Các bước Build & Kiểm thử

### Bước 1: Clone repository
*(Dự án không dùng Git submodule.)*
```bash
git clone https://github.com/hangothanhh/DeX120Hz-S22.git
cd DeX120Hz-S22
```

### Bước 2: Chạy Build

- **Trên Windows (PowerShell):**
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\build.ps1
  ```
- **Trên Linux / macOS:**
  ```bash
  chmod +x ./build.sh
  ./build.sh
  ```

**Kết quả thành công được ghi vào `out/`:**
- `out/DeX120Hz-S22.apk`
- `out/DeX120Hz-S22.apk.sha256`

### Bước 3: Chạy Kiểm thử & Xác minh Output

1. Kiểm tra chữ ký APK bằng `apksigner` *(với `build.sh`, bước này đã chạy tự động cuối script)*:
   - **Windows:**
     ```powershell
     & "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat" verify --verbose out\DeX120Hz-S22.apk
     ```
   - **Linux / macOS:**
     ```bash
     "$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose out/DeX120Hz-S22.apk
     ```
2. Chạy bộ kiểm thử tự động `scripts/verify_apk.py`:
   ```bash
   python scripts/verify_apk.py      # hoặc python3 scripts/verify_apk.py trên Linux/macOS
   ```
   Script này tự kiểm tra:
   - Cấu trúc ZIP của `out/DeX120Hz-S22.apk` có đủ `AndroidManifest.xml`, `classes.dex`, `assets/xposed_init`, `resources.arsc`.
   - `assets/xposed_init` trỏ đúng `com.dex120.s22.xposed.HookEntry`.
   - Parse header & bảng `class_defs` trong `classes.dex`: xác nhận có đủ `HookEntry`, `DpSysfsHook`, `LowRefreshTokenHook`, `DisplayModeHook`, `VirtualDisplaySyncHook`, `SdhmsDfsHook`, và **0 class `Lde/robv/android/xposed/...` bị lọt vào `classes.dex`**.
   - Đối chiếu SHA-256 thực tế của APK trùng khớp `out/DeX120Hz-S22.apk.sha256`.
   - Biên dịch và chạy `tests/SanitizerTest.java` trên host JVM để kiểm tra `RootShell.sanitizeDiagnostics()` ẩn đúng Serial, IMEI, MAC, IP nội bộ, Email, Token/Password mà không làm hỏng các dòng log kỹ thuật.

---

## 4. Quản lý Keystore ký APK (An toàn)

- Mặc định khi chưa có khóa riêng, `build.ps1` / `build.sh` tự sinh `debug.keystore` cục bộ (hoặc `build/debug.keystore` khi chạy trên CI) với mật khẩu debug nội bộ.
- File `*.keystore` và `*.jks` đã được `.gitignore` chặn — **tuyệt đối không** đưa lên Git.
- Nếu bạn muốn ký bằng khóa release cá nhân bên ngoài repo, truyền các biến môi trường sau trước khi chạy build:
  ```bash
  export DEX120_KEYSTORE="/duong/dan/ngoai/repo/my-release.jks"
  export DEX120_KEYSTORE_PASS="..."
  export DEX120_KEY_ALIAS="..."
  export DEX120_KEY_PASS="..."
  ./build.sh
  ```
- Lưu ý: khi thay khóa ký mới, người dùng cài bản trước bằng khóa cũ sẽ phải gỡ app cũ trước khi cài bản ký bằng khóa mới.
