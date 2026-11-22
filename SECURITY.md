# Chính sách Bảo mật (Security Policy)

## Phạm vi bảo mật & rủi ro cố hữu

**DeX 120Hz S22** là công cụ dành cho thiết bị **Samsung Galaxy S22 Exynos (`SM-S901B`, Android 12 / One UI 4.1) đã root**. Về bản chất kiến trúc, dự án:

1. Chạy lệnh quản trị hệ thống qua `su` trong `MainActivity` (`RootShell.exec`) để đọc `/sys/class/dp_sec/dex`, ghi `Settings.Global` (`dex120_*`), ghi `Settings.System` / `Settings.Secure` (`refresh_rate_mode`, `min_refresh_rate`, `peak_refresh_rate`), gọi `service call SurfaceFlinger 1122 i32 60` và thu thập `dumpsys` / `logcat` / `dmesg`.
2. Tiêm mã Java thông qua LSPosed vào hai tiến trình hệ thống đặc quyền:
   - `android` (`system_server`, UID `1000`)
   - `com.sec.android.sdhms` (Samsung Device Health Manager Service, UID `1000`)

Do đặc quyền `system` (UID `1000`) và `root`, mọi lỗ hổng trong code hook hoặc script đều có thể ảnh hưởng trực tiếp tới tính ổn định và bảo mật của thiết bị. Hãy kiểm tra kỹ mã nguồn trước khi cài đặt.

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

Phần lớn mã nguồn và bộ lọc bảo mật trong dự án được viết với sự hỗ trợ của AI và mới được kiểm thử cục bộ trên một máy `SM-S901B` cùng bộ unit test `tests/SanitizerTest.java`. Không mặc định coi đây là phần mềm đã qua kiểm toán bảo mật chuyên sâu bởi con người.

## Báo cáo lỗ hổng bảo mật

Vì đây là dự án thử nghiệm cho thiết bị cụ thể và không thu thập dữ liệu máy chủ từ xa:

- Nếu phát hiện lỗ hổng trong repository công khai/riêng tư của chủ dự án hoặc script build (ví dụ rò rỉ khóa, lỗi thực thi lệnh shell không an toàn trong `RootShell` hoặc lỗi injection trong `build.ps1`/`build.sh`):
  - Vui lòng liên hệ trực tiếp chủ repository (`hangothanhh`) qua GitHub Security Advisory (nếu bật) hoặc mở Issue **không kèm payload khai thác chi tiết / dữ liệu riêng tư**.
  - Không gửi kèm tệp khóa riêng hoặc thông tin máy thật của người khác khi báo cáo.
