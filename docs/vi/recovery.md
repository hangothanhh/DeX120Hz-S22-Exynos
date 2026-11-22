# Cập nhật, Rollback, Gỡ cài đặt & Khôi phục (Recovery)

**Ngôn ngữ / Language:** [English](../recovery.md) | **Tiếng Việt**

> [!IMPORTANT]
> Hãy đọc kỹ phần **Khôi phục theo từng mức độ lỗi** bên dưới trước khi thử nghiệm bất kỳ thay đổi nào liên quan tới `system_server` hoặc LSPosed.

---

## 1. Cập nhật (Update) lên phiên bản mới

**Cập nhật có giữ cấu hình không?**
Các công tắc hook của `DeX 120Hz S22` được lưu trong hệ thống Android (`Settings.Global` khóa `dex120_*`), **không** nằm trong bộ nhớ nội bộ bị xóa của app. Do đó khi cài đè APK bản mới (cùng chữ ký), các công tắc thường giữ nguyên; tuy nhiên do đây là bản Pre-release đang phát triển, bạn **vẫn nên ghi/chụp lại cấu hình hiện tại** trước khi cập nhật.

**Các bước cập nhật:**
1. Mở app `DeX 120Hz S22`, xem lại hoặc chụp ảnh các công tắc hiện tại (và nếu cần sao lưu qua ADB: `adb shell "settings list global | grep '^dex120_'"`).
2. Tải `DeX120Hz-S22.apk` và `.sha256` bản mới từ [Releases](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/releases) và đọc [CHANGELOG.md](CHANGELOG.md).
3. Kiểm tra lại **SHA-256** (xem [docs/installation.md](installation.md#bước-2-kiểm-tra-sha-256-bắt-buộc-để-tránh-file-hỏnggiả-mạo)).
4. Cài đè APK (`adb install -r DeX120Hz-S22.apk` hoặc bấm cài trực tiếp).
5. Mở **LSPosed Manager** → kiểm tra xem bản mới có thêm scope mới không (tối thiểu tick đủ 3 mục: `Hệ thống Android (android)`, `com.sec.android.sdhms`, `DeX 120Hz S22`).
6. **Soft Reboot** 1 lần *(không Reboot thường nếu dùng Temproot)*.
7. Mở lại app, kiểm tra 2 thẻ xanh lá và test DeX như lần đầu.

---

## 2. Rollback (Quay lại phiên bản cũ)

Thực hiện khi bản mới bị lỗi app, rớt hook, sập `system_server` hoặc nóng máy bất thường:

1. Mở **LSPosed Manager** → tắt công tắc module **DeX 120Hz S22** → **Soft Reboot**.
2. Gỡ cài đặt app `DeX 120Hz S22` hiện tại (Android không cho cài đè phiên bản `versionCode` thấp hơn trực tiếp, trừ khi gỡ trước).
3. Tải phiên bản cũ ổn định từ Releases và xác minh **SHA-256**.
4. Cài lại APK cũ → vào LSPosed bật lại module + chọn đủ scope của bản cũ → **Soft Reboot**.

---

## 3. Gỡ cài đặt hoàn toàn (Uninstall)

Thực hiện theo đúng thứ tự dưới đây để không để lại thiết lập Hz bị khóa trong Android:

1. Mở app **DeX 120Hz S22** → nhấn **Khôi phục Adaptive** *(để xóa `min_refresh_rate`, đưa `peak_refresh_rate=120.0` và `refresh_rate_mode=1`)*.
2. Mở **LSPosed Manager** → tắt công tắc module **DeX 120Hz S22** → **Soft Reboot**.
3. Gỡ ứng dụng **DeX 120Hz S22** như app bình thường (hoặc `adb uninstall com.dex120.s22`).
4. **Kiểm tra không còn hook chạy:**
   ```bash
   adb shell su -c "logcat -d -s DeX120Hz:I | tail -n 10"
   ```
   Sau Soft Reboot mà không thấy log mới `[DeX120Hz] Initializing...` nữa là sạch hook hoàn toàn.

**File nào được giữ lại vs bị xóa sau khi gỡ?**
- **Bị xóa:** APK và dữ liệu riêng của app `com.dex120.s22` tại `/data/app/...` và `/data/data/com.dex120.s22/`. Module không cài bất kỳ file nhị phân hay module `.zip` nào vào `/data/adb/modules/` và không sửa phân vùng `/system` / `/vendor`.
- **Còn giữ lại (vô hại khi chưa cài app):** Các khóa cài đặt `dex120_*` trong `Settings.Global` và `min_refresh_rate` / `peak_refresh_rate` trong `Settings.System`. Nếu muốn dọn sạch 100% các khóa `dex120_*` qua ADB:
  ```bash
  for k in dex120_enabled dex120_block_dp_dex dex120_block_token dex120_unlock_modes dex120_sync_virtual dex120_force_virtual_120 dex120_target_hz; do
    adb shell su -c "settings delete global $k"
  done
  adb shell su -c "settings delete system min_refresh_rate; settings put system peak_refresh_rate 120.0; settings put secure refresh_rate_mode 1"
  ```

> [!WARNING]
> **Không** tự ý xóa thủ công các file thuộc `/data/adb/lspd/`, `/data/adb/modules/` hoặc `/system/` nếu bạn không chắc — xóa nhầm sẽ hỏng LSPosed hoặc hệ điều hành.

---

## 4. Khôi phục sự cố (Recovery) theo từng tình huống

### Tình huống A: Chỉ Màn đen AMOLED bị kẹt hoặc App `DeX 120Hz S22` bị crash
- **Tác động tới dữ liệu:** **Không mất dữ liệu.**
- **Cách xử lý:**
  - Chạm nhanh **3 lần** hoặc bấm **Back 2 lần** để thoát Màn đen AMOLED; nếu vẫn kẹt, bấm nút nguồn hoặc rút cáp màn hình ngoài.
  - Nếu app crash liên tục: vào *Cài đặt Android → Ứng dụng → DeX 120Hz S22 → Buộc dừng / Xóa dữ liệu* hoặc chạy `adb shell su -c "settings put global dex120_enabled 0"`.

### Tình huống B: Hook không nạp (thẻ đỏ `LSPosed: CHƯA BẬT`)
- **Tác động tới dữ liệu:** **Không mất dữ liệu.**
- **Cách xử lý:** Kiểm tra đủ 3 scope trong LSPosed, đảm bảo ReZygisk hoạt động, rồi **Soft Reboot**. Nếu cần xem lỗi load, kiểm tra `/data/adb/lspd/log/modules_*.log`.

### Tình huống C: Máy bị treo / bootloop ngay sau khi Soft Reboot
- **Tác động tới dữ liệu:** **Không mất dữ liệu** nếu làm đúng cách bên dưới.
- **Cách 1 (Với máy dùng KernelSU Temproot):**
  1. Giữ nút **Nguồn + Giảm âm lượng** khoảng 7–10 giây để ép tắt và khởi động lại hoàn toàn (**Hard Reboot**).
  2. Vì Temproot không bền qua Hard Reboot, máy sẽ khởi động thẳng vào Android 12 gốc sạch sẽ, **không** nạp LSPosed hay hook nào cả — dữ liệu của bạn vẫn còn nguyên.
- **Cách 2 (Với máy Root vĩnh viễn — Bootloader đã mở khóa):**
  1. Dùng tính năng Safe Mode của **KernelSU** *(giữ nút Giảm âm lượng liên tục lúc logo khởi động xuất hiện — tùy phiên bản KernelSU bạn đang cài hỗ trợ)* để tắt toàn bộ module khi boot, **hoặc**:
  2. Trong lúc máy đang kẹt bootloop nhưng ADB đã kịp lên:
     ```bash
     adb wait-for-device shell su -c "settings put global dex120_enabled 0"
     ```
     Hoặc tạm tắt module LSPosed bằng CLI của LSPosed (nếu bản LSPosed của bạn hỗ trợ) hoặc gỡ gói app qua ADB:
     ```bash
     adb wait-for-device uninstall com.dex120.s22
     ```

### Tình huống D: Máy không thể vào Android (trường hợp rất hiếm, thường do sửa sai module hệ thống khác song song)
> [!CAUTION]
> Các phương án khôi phục mức thấp dưới đây **có thể gây mất toàn bộ dữ liệu** nếu bạn phải Factory Reset / Wipe Data!

1. Nếu vào được **Android Recovery gốc** hoặc **ADB sideload / custom recovery** (nếu đã cài):
   - Chỉ chọn **Wipe cache** trước nếu có — tuyệt đối chưa chọn *Wipe data/factory reset* vội.
   - Nếu là root vĩnh viễn do boot image lỗi: flash lại đúng `boot.img` / firmware gốc đúng mã **SM-S901B** Android 12 của bạn qua Download Mode (`HOME_CSC` để thử giữ dữ liệu trước; dùng `CSC` thông thường sẽ **xóa sạch dữ liệu**).
2. Không bao giờ flash firmware của model S22 khác hoặc phiên bản bootloader thấp hơn (rollback protection trên Samsung).
