# Changelog

**Ngôn ngữ / Language:** [English](../../CHANGELOG.md) | **Tiếng Việt**

Mọi thay đổi đáng chú ý của dự án **DeX 120Hz S22** (`com.dex120.s22`) đều được ghi lại tại đây theo chuẩn [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

### Changed
- Chuyển ngôn ngữ mặc định (primary language) sang **tiếng Anh** cho `README.md`, toàn bộ tài liệu gốc, 7 hướng dẫn trong `docs/`, 4 mẫu `.github/` và ghi chú phát hành; lưu bản dịch tiếng Việt đầy đủ tại `README.vi.md` và `docs/vi/`.
- Chuyển `README.en.md` thành `README.md` chính.

### Security
- Bật **`sha_pinning_required: true`** trên GitHub Actions của repository và kích hoạt Repository Ruleset **`Protect main branch`** (`#27848863`: chặn xóa nhánh/force-push, yêu cầu PR + status check `build-and-verify`).

---

## [v1.0.1-s22-a12] - 2026-11-22 (Pre-release)

Phiên bản hoàn thiện tuân thủ giấy phép, bảo mật chuỗi cung ứng, tài liệu song ngữ và CI/CD trên nền `v1.0.0-s22-a12` (không thay đổi logic Xposed hook cốt lõi).

### Added
- Thêm tài liệu tiếng Anh thân thiện cho người mới tại `README.en.md` và thanh chuyển ngôn ngữ trên `README.md`.
- Thêm `.gitattributes` chuẩn hóa line ending (`LF` mặc định, `CRLF` cho `*.ps1`, đánh dấu file nhị phân).
- Thêm bộ hồ sơ kiểm toán & quản trị mã nguồn mở: `AI_CONTRIBUTIONS.md`, `THIRD_PARTY_NOTICES.md`, `LICENSE_AUDIT_REPORT.md`, `SECURITY_AUDIT_REPORT.md` và `SBOM.cdx.json` (CycloneDX `1.5`).
- Bổ sung mục **Supported Versions**, **APK Signing Policy** trong `SECURITY.md` và **Trademark Disclaimer** trong `NOTICE`, `README.md`, `README.en.md`.

### Changed
- Chuyển giấy phép mã nguồn mở từ MIT sang **`GNU GPL v3.0` (`GPL-3.0`)** và cập nhật `NOTICE` để tương thích giấy phép đầy đủ với `Kinue72/OnigiriDeX` (`GPL-3.0`), `LSPosed` và `KernelSU`.
- Cập nhật URL repository sang `hangothanhh/DeX120Hz-S22-Exynos`.
- Tăng `versionCode` lên `2`, `versionName` lên `1.0.1-s22-a12` trong `AndroidManifest.xml` và `RootShell.java`; bật quyền thực thi (`100755`) cho `build.sh` và `scripts/verify_apk.py`.
- Loại bỏ trailer `Co-authored-by: Antigravity <antigravity@google.com>` khỏi lịch sử commit sau `0b17242` và cập nhật `CONTRIBUTING.md` / `AI_CONTRIBUTIONS.md` để tránh gán nhầm tài khoản GitHub không xác minh `antigravity-google-bot`.

### Security
- Gia cố `.github/workflows/ci.yml`: giới hạn `permissions: contents: read`, ghim toàn bộ GitHub Actions theo commit SHA (`actions/checkout` v7.0.1, `setup-java` v6.3.0, `setup-python` v7.1.0, `upload-artifact` v7.0.2), kiểm tra tồn tại `LICENSE` GPL-3.0, tính hợp lệ của `SBOM.cdx.json` và không để lọt chuỗi token tĩnh trong `tests/SanitizerTest.java`.
- Xây dựng chuỗi token giả lập trong `tests/SanitizerTest.java` bằng phép ghép chuỗi động thay cho literal tĩnh.

---

## [v1.0.0-s22-a12] - 2026-11-22 (Pre-release)

Phiên bản phát hành thử nghiệm đầu tiên nhắm riêng **Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`) chạy Android 12 (`API 31`) / One UI 4.1 với quyền Root + LSPosed**.

### Added
- **5 lớp Xposed/LSPosed hook:**
  - `DpSysfsHook`: Chặn ghi `0x10`/`0x11` vào `/sys/class/dp_sec/dex` để giữ kernel DisplayPort ở chế độ Mirror, mở khóa danh sách độ phân giải/tần số quét >60Hz; giả lập kết quả đọc `"1"` có điều kiện khi màn hình ngoài đang active.
  - `LowRefreshTokenHook`: Chặn các token khóa ≤60Hz (`acquireLowRefreshRateToken`, `acquireRefreshRateMaxLimitToken`) từ `[DMS]HardwareManager`, `SiopLimiterHrr` và các caller khi có màn hình ngoài active, cache proxy an toàn theo từng interface (`ILowRefreshRateToken` và `IRefreshRateMaxLimitToken`).
  - `DisplayModeHook`: Nâng `baseModeId` và `RefreshRateRange` của màn ngoài lên Hz cao nhất (tối đa theo `dex120_target_hz`, mặc định 120Hz), đồng thời bảo vệ Display 0 ở 120Hz khi đang cắm màn ngoài.
  - `VirtualDisplaySyncHook`: Tự động đồng bộ `VirtualDisplay "Desktop"` theo đúng Hz thực của màn hình ngoài (kèm tùy chọn Test Mode `dex120_force_virtual_120`).
  - `SdhmsDfsHook`: Chặn giao dịch Binder `SurfaceFlinger` mã `1122` (`DFS = 30`) từ `com.sec.android.sdhms` khi nóng máy, đồng thời tự động gửi lại lệnh `DFS = 60` từ `system_server`.
- **Ứng dụng điều khiển `DeX 120Hz S22` (`MainActivity`, `BlackScreenActivity`, `RootShell`, `ModuleStatus`):**
  - Tự kiểm tra trạng thái kích hoạt LSPosed và quyền Root.
  - Màn đen AMOLED (`#000000`, độ sáng tối thiểu `0.001f`) giữ Display 0 ở trạng thái `ON` để SurfaceFlinger Android 12 không rơi về `SyntheticVSync` 60Hz khi tắt màn hình bằng nút nguồn.
  - Nút **Ép cứng 120Hz** (đặt `Settings.Secure.refresh_rate_mode=1`, `Settings.System` `min/peak_refresh_rate=120.0`, `low_power=0`, reset `SurfaceFlinger DFS=60`) và cảnh báo giá trị `120.1` của SamHelper.
  - Công cụ **Chạy chẩn đoán** kèm bộ lọc tự ẩn dữ liệu nhạy cảm (`RootShell.sanitizeDiagnostics`) trước khi người dùng sao chép log (ẩn Serial, IMEI, MAC, IP nội bộ, Email, Token/Password).
- **Công cụ build, kiểm thử & tài liệu mở:**
  - Script build độc lập `build.ps1` (Windows) và `build.sh` (Linux/macOS), tự kiểm chứng APK/DEX/SHA-256 và unit test sanitizer (`scripts/verify_apk.py`, `tests/SanitizerTest.java`).
  - Bộ tài liệu hướng dẫn người mới hoàn chỉnh (`README.md` và thư mục `docs/`), `LICENSE` (MIT), `NOTICE`, `SECURITY.md`, `CONTRIBUTING.md`, và GitHub Issue/PR templates.

### Compatibility & Verification Scope
- **Đã kiểm chứng thực tế (`VERIFIED`):** `SM-S901B` (Exynos 2200), Android 12 / One UI 4.1 (`API 31`, kernel `5.10.66`), KernelSU temproot + ReZygisk + LSPosed, màn hình ngoài **VSP `V2203H` (`1920x1080@75Hz`)** qua USB-C hub (`OSD 75Hz`, `decon[2] bts.fps:75`, `VSync 13.33ms` sau `DFS=60`).
- **Kiểm thử một phần (`PARTIAL`):** Pipeline phần mềm đã mở khóa tới `120Hz`, nhưng **chưa test vật lý trên màn hình ngoài 120Hz thật**.
- **Không hỗ trợ (`UNSUPPORTED`):** Android 13/14/15 (One UI 5/6/7), S22 Snapdragon hoặc các model Galaxy khác chưa từng được kiểm thử.

### Known Issues
- Trên màn hình ngoài **75Hz**, `/system/bin/surfaceflinger` gốc của Samsung sẽ tự hạ Display 0 (120Hz) xuống 60Hz khi hai màn khác nhịp không tương thích, và với property mặc định `vendor.hwc.exynos.vsync_mode=0`, VSync ứng dụng có thể bị kéo theo 60Hz khi màn điện thoại đang bật. (Tắt màn điện thoại hoặc dùng màn 120Hz khớp nhịp tránh được giới hạn này.)
- Chưa có màn hình vật lý 120Hz để đo độ ổn định dài hạn, nhiệt độ và băng thông ở `1080p@120Hz` / `1440p@120Hz`.
- Nếu cài đồng thời ứng dụng hoặc module quản lý tần số quét/game bên thứ ba (ví dụ SamHelper ghi `120.1` hoặc module tự hạ về `60Hz`), có thể ghi đè lên `min/peak_refresh_rate`.
