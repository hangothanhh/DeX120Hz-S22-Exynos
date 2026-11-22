# Chính sách Bảo mật (Security Policy)

**Ngôn ngữ / Language:** [English](../../SECURITY.md) | **Tiếng Việt**

## Phạm vi bảo mật & rủi ro cố hữu

**DeX 120Hz S22** là công cụ dành cho thiết bị **Samsung Galaxy S22 Exynos (`SM-S901B`, Android 12 / One UI 4.1) đã root**. Về bản chất kiến trúc, dự án:

1. Chạy lệnh quản trị hệ thống qua `su` trong `MainActivity` (`RootShell.exec`) để đọc `/sys/class/dp_sec/dex`, ghi `Settings.Global` (`dex120_*`), ghi `Settings.System` / `Settings.Secure` (`refresh_rate_mode`, `min_refresh_rate`, `peak_refresh_rate`), gọi `service call SurfaceFlinger 1122 i32 60` và thu thập `dumpsys` / `logcat` / `dmesg`.
2. Tiêm mã Java thông qua LSPosed vào hai tiến trình hệ thống đặc quyền:
   - `android` (`system_server`, UID `1000`)
   - `com.sec.android.sdhms` (Samsung Device Health Manager Service, UID `1000`)

Do đặc quyền `system` (UID `1000`) và `root`, mọi lỗ hổng trong code hook hoặc script đều có thể ảnh hưởng trực tiếp tới tính ổn định và bảo mật của thiết bị. Hãy kiểm tra kỹ mã nguồn trước khi cài đặt.

## Phiên bản được hỗ trợ (Supported Versions)

| Phiên bản | Giấy phép | Trạng thái hỗ trợ | Ghi chú kiểm chứng |
| :--- | :--- | :--- | :--- |
| `v1.0.2-s22-a12` (`Pre-release`) | `GNU GPL v3.0` | Đang duy trì (Experimental) | Bản đầu tiên ký bằng khóa `dex120-release` qua `release.yml`, kèm chứng thực SLSA. Chỉ xác minh host + CI; chưa test lại trên máy thật (máy test đã bán). |
| `v1.0.1-s22-a12` (`Pre-release`) | `GNU GPL v3.0` | Đang duy trì (Experimental) | Build & unit test đã xác minh trên host Windows + Ubuntu CI; máy test `SM-S901B` đã bán nên chưa test lại trực tiếp trên máy thật. |
| `v1.0.0-s22-a12` (`b10d6e9`) | `MIT` (snapshot cũ) | Ngừng hỗ trợ / Đã thay thế | Snapshot lịch sử trước khi chuyển giấy phép sang `GPL-3.0`; giữ nguyên tag để bảo toàn lịch sử Git. |

## Chính sách ký APK & Chứng thực nguồn gốc Build (SLSA Provenance)

- **Khóa ký Release riêng ngoài Repository (dành cho các bản phát hành từ tag qua `.github/workflows/release.yml`):**
  - Khóa PKCS12 (`dex120-release`) được lưu độc lập ngoài repository và mã hóa an toàn trong GitHub Repository Secrets (`DEX120_KEYSTORE_B64`, `DEX120_KEYSTORE_PASS`, `DEX120_KEY_ALIAS`, `DEX120_KEY_PASS`).
  - **Dấu vân tay chứng thư công khai SHA-256 (`dex120-release`):**
    ```text
    07:3F:BF:73:65:90:0B:03:1F:5D:2D:6C:8A:1C:1A:9A:29:FE:28:7F:F0:6C:17:41:91:DB:FF:EE:8C:07:34:B1
    ```
  - **SLSA Build Provenance Attestation:** Các bản build từ workflow `Release and Attest APK` (`release.yml`) tự động sinh chứng thực SLSA có thể kiểm tra bằng lệnh:
    ```bash
    gh attestation verify DeX120Hz-S22.apk -R hangothanhh/DeX120Hz-S22-Exynos
    ```
- **Khóa Debug (`v1.0.1-s22-a12` & Build phát triển trên CI/Local):**
  - Bản `v1.0.1-s22-a12` và các lượt push CI thông thường sử dụng `debug.keystore` tạm thời (luôn nằm trong `.gitignore` và **không bao giờ** commit lên Git).
  - **Lưu ý nâng cấp:** Vì Android yêu cầu chữ ký khớp khi cài đè, khi chuyển từ `v1.0.1-s22-a12` (ký debug) lên các bản tag phát hành mới ký bằng `dex120-release`, người dùng cần gỡ bản cũ trước khi cài bản mới, đồng thời luôn đối chiếu mã băm `SHA-256` hoặc tự biên dịch từ mã nguồn.

## Những dữ liệu TUYỆT ĐỐI KHÔNG đưa lên Repository / Issues / PR

Người dùng và cộng tác viên tuyệt đối không commit hoặc dán công khai:

- Keystore riêng, khóa ký APK (`.keystore`, `.jks`, `.pk8`, `.pem`) hoặc mật khẩu ký APK.
- API keys, GitHub Personal Access Tokens (`ghp_...`, `gho_...`, `github_pat_...`).
- Số nhận dạng cá nhân hoặc định danh phần cứng của máy: **IMEI**, **Serial Number** (`ro.serialno`, `ro.boot.serialno`), địa chỉ **MAC** Wi-Fi/Bluetooth, địa chỉ **IP** mạng riêng/ADB cá nhân.
- File backup `settings_*.xml`, `efs`, khóa **Play Integrity** / Play Integrity tokens / hardware attestation certificates chưa ẩn danh.
- Log thô (`logcat`, `dumpsys`, `dmesg`) chưa qua bước loại bỏ thông tin cá nhân.

## Cơ chế tự ẩn danh thông tin khi thu thập log lỗi

Hàm `RootShell.sanitizeDiagnostics(String raw)` trong ứng dụng tự động thay thế các mẫu thông tin nhạy cảm trước khi hiển thị và cho phép Copy Log:

- Token GitHub (`ghp_...`, `gho_...`, `github_pat_...`) → `[REDACTED_TOKEN]`
- Cặp key-value nhạy cảm (`token`, `password`, `passwd`, `secret`, `api_key`, `authorization`, `bearer`) → giữ tên khóa, che giá trị thành `[REDACTED_SECRET]` (bỏ qua đúng tên khóa `dex120_block_token` và tag kỹ thuật như `token:[DMS]HardwareManager` / `SiopLimiterHrr`)
- `ro.serialno` / `ro.boot.serialno` / serial Samsung 11 ký tự → `[REDACTED_SERIAL]`
- IMEI → `[REDACTED_IMEI]`
- Địa chỉ MAC → `[REDACTED_MAC]`
- Địa chỉ IP mạng nội bộ (`10.x.x.x`, `172.16-31.x.x`, `192.168.x.x`, `127.x.x.x`) → `[REDACTED_IP]`
- Địa chỉ email → `[REDACTED_EMAIL]`

> [!IMPORTANT]
> Bộ lọc tự động chỉ là lớp bảo vệ hỗ trợ. **Bạn vẫn phải tự đọc lại toàn bộ log đã copy trước khi dán lên GitHub Issues**, vì `dmesg` hoặc `logcat` từ app bên thứ ba đôi khi có thể chứa tên Wi-Fi SSID hoặc định dạng ID đặc thù ngoài các mẫu regex trên.

## Minh bạch về đóng góp của AI

Phần lớn mã nguồn, script và bộ lọc bảo mật trong dự án được viết với sự hỗ trợ của Google Antigravity dưới sự điều phối và kiểm duyệt của chủ dự án (`hangothanhh`). Trước đây hành vi thực tế 75Hz đã được kiểm chứng trên một máy `SM-S901B` + màn hình VSP `V2203H`; hiện máy test đã được bán nên bản `v1.0.1-s22-a12` chỉ mới kiểm chứng build trên host Windows, unit test `tests/SanitizerTest.java` và Ubuntu CI. Không mặc định coi đây là phần mềm đã qua kiểm toán bảo mật chuyên sâu bởi bên thứ ba. Xem chi tiết tại [AI_CONTRIBUTIONS.md](AI_CONTRIBUTIONS.md) và [SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md).

## Báo cáo lỗ hổng bảo mật

Vì đây là dự án thử nghiệm cho thiết bị cụ thể và không thu thập dữ liệu máy chủ từ xa:

- Nếu phát hiện lỗ hổng trong repository công khai của chủ dự án hoặc script build (ví dụ rò rỉ khóa, lỗi thực thi lệnh shell không an toàn trong `RootShell` hoặc lỗi injection trong `build.ps1`/`build.sh`):
  - Vui lòng liên hệ trực tiếp chủ repository (`hangothanhh`) qua GitHub Security Advisory (nếu bật) hoặc mở Issue **không kèm payload khai thác chi tiết / dữ liệu riêng tư**.
  - Không gửi kèm tệp khóa riêng hoặc thông tin máy thật của người khác khi báo cáo.
