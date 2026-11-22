# AI Contributions & Verification Disclosure

Tài liệu này ghi nhận minh bạch phạm vi tham gia của AI (**Google Antigravity**) và phạm vi kiểm chứng thực tế của con người trong dự án **DeX 120Hz S22** (`com.dex120.s22`).

---

## 1. Identity & Git Attribution Policy (`ANTIGRAVITY_IDENTITY_UNVERIFIED`)

- **Công cụ hỗ trợ:** Google Antigravity.
- **Trạng thái định danh Git:** `ANTIGRAVITY_IDENTITY_UNVERIFIED`.
- **Chính sách commit trailer:** Các commit mới trên nhánh `main` **KHÔNG** gắn dòng trailer `Co-authored-by`. Toàn bộ commit mang author/committer là chủ sở hữu repository (`hangothanhh <197389458+hangothanhh@users.noreply.github.com>`).
- **Lý do kỹ thuật và quản trị:**
  - Địa chỉ email `antigravity@google.com` khi kiểm tra qua GitHub API ánh xạ tới một tài khoản người dùng phổ thông không xác minh là `antigravity-google-bot` (GitHub user ID `358880572`, `type: User`, tạo ngày `2026-11-19T19:48:38Z`, không có hồ sơ/xác thực chính thức từ Google).
  - Tài khoản `gemini-code-assist[bot]` (ID `176961590`) là một GitHub App khác, không phải Antigravity và không được sử dụng trong dự án này.
  - Theo yêu cầu trực tiếp của chủ repository (`hangothanhh`), 4 commit từng có trailer `Co-authored-by: Antigravity <antigravity@google.com>` sau `0b17242` đã được làm sạch bằng `git commit-tree` để xóa hoàn toàn liên kết contributor nhầm tới `antigravity-google-bot` trên GitHub. Tag lịch sử `v1.0.0-s22-a12` (`b10d6e9`) nằm trước 4 commit đó và không bị ảnh hưởng.

---

## 2. Phạm vi đóng góp của Google Antigravity

Google Antigravity tham gia hỗ trợ các hạng mục sau dưới yêu cầu, bối cảnh thực nghiệm và phê duyệt của chủ dự án (`hangothanhh`):

| Hạng mục | Vai trò của Google Antigravity | Vai trò của chủ dự án (`hangothanhh`) |
| :--- | :--- | :--- |
| Kiến trúc Xposed hook & Java UI | Soạn thảo, tái cấu trúc 5 lớp hook (`DpSysfsHook`, `LowRefreshTokenHook`, `DisplayModeHook`, `VirtualDisplaySyncHook`, `SdhmsDfsHook`), `MainActivity`, `BlackScreenActivity`, `RootShell` | Xác định mục tiêu thiết bị (`SM-S901B`), cung cấp log thật lúc phát triển, kiểm thử và xác nhận hoạt động |
| Bộ lọc ẩn danh log (`Sanitizer`) & Test | Thiết kế `RootShell.sanitizeDiagnostics()` và unit test `tests/SanitizerTest.java` | Rà soát tiêu chí ẩn danh Serial/IMEI/MAC/IP/Token |
| Toolchain build & verify không Gradle | Viết `build.ps1`, `build.sh`, `scripts/verify_apk.py`, `stubs/de/robv/android/xposed/*` | Chạy và kiểm chứng trên máy Windows cá nhân |
| GitHub Actions CI/CD | Viết và gia cố `.github/workflows/ci.yml` (least privilege + SHA-pinned actions) | Phê duyệt quy trình CI và chính sách phát hành |
| Tài liệu & Tuân thủ | Viết `README.md`, `README.en.md`, `docs/*`, `CHANGELOG.md`, `SECURITY.md`, `CONTRIBUTING.md`, `NOTICE`, báo cáo kiểm toán và `SBOM.cdx.json` | Quyết định chọn giấy phép `GNU GPL v3.0`, chính sách AI attribution và phát hành `Pre-release` |

---

## 3. Phạm vi kiểm chứng thực tế & Giới hạn trách nhiệm

| Loại kiểm chứng | Trạng thái | Ghi chú trung thực |
| :--- | :--- | :--- |
| Build APK (`aapt2` + `d8` + `zipalign` + `apksigner`) | **VERIFIED** | Xác minh trên host Windows (JDK 17, Android SDK 34) và Ubuntu GitHub Actions |
| Kiểm tra DEX & Unit Test (`scripts/verify_apk.py`, `tests/SanitizerTest.java`) | **VERIFIED** | Đã xác minh không đóng gói stub Xposed vào `classes.dex` và vượt qua 100% test case lọc PII/Token giả lập |
| Chạy thực tế Samsung DeX tần số quét cao | **HISTORICAL / PARTIAL** | Đã được chủ sở hữu xác minh thực tế ở mức **75Hz** trên **1 máy Samsung Galaxy S22 Exynos (`SM-S901B`, Android 12 / One UI 4.1)** với màn hình **VSP `V2203H`**. 120Hz chưa có màn hình/cáp thử nghiệm để khẳng định |
| Tái kiểm thử bản `v1.0.1-s22-a12` trên máy thật (`ON_DEVICE_RETEST`) | **NOT_VERIFIED** | Thiết bị test `SM-S901B` đã được chủ sở hữu bán trước khi kiểm toán/đóng gói `v1.0.1-s22-a12` (`v1.0.1` không sửa logic hook, chỉ cập nhật version, test fixture, CI và tài liệu tuân thủ) |

---

## 4. Tuyên bố không bảo trợ (Non-Endorsement)

Google LLC và nhóm phát triển Google Antigravity không kiểm toán độc lập, không chịu trách nhiệm pháp lý và không bảo trợ cho repository này hoặc các bản phát hành APK của dự án. Người dùng tự chịu toàn bộ rủi ro khi cài đặt công cụ Root / LSPosed vào tiến trình hệ thống (`system_server`, `com.sec.android.sdhms`).
