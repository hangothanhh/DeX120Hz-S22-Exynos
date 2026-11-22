# Khắc phục Sự cố & Câu hỏi Thường gặp (Troubleshooting & FAQ)

Tài liệu tổng hợp các lỗi phổ biến cho người mới khi cài và chạy `DeX 120Hz S22` trên **Galaxy S22 Exynos (`SM-S901B`, Android 12 / One UI 4.1)**.

---

## 1. Thẻ `LSPosed: CHƯA BẬT` hoặc hook không chạy

1. **Biểu hiện:** App hiện thẻ đỏ `LSPosed: CHƯA BẬT`, hoặc log chẩn đoán báo `(Chưa có log...)`.
2. **Nguyên nhân có thể:**
   - Chưa gạt bật module `DeX 120Hz S22` trong **LSPosed Manager**.
   - Tick thiếu phạm vi (Scope) `DeX 120Hz S22` hoặc `Hệ thống Android` (`android`).
   - Vừa cài/cập nhật APK xong nhưng chưa **Soft Reboot**.
   - **ReZygisk** bị ngừng sau khi Soft Reboot (một số bản ReZygisk cũ trên KernelSU temproot có thể rớt injection sau Soft Reboot).
3. **Cách kiểm tra:**
   - Mở **LSPosed Manager** kiểm tra cột trạng thái LSPosed có báo *Activated* không, và vào **Modules → DeX 120Hz S22** xem đã tick đủ 3 mục (`android`, `com.sec.android.sdhms`, `com.dex120.s22`) chưa.
4. **Cách xử lý:**
   - Tick đủ cả **3 scope** → **Soft Reboot** 1 lần *(nhớ không bấm Reboot thường nếu đang dùng Temproot)*.
   - Nếu LSPosed chính nó báo chưa kích hoạt sau Soft Reboot, cần kiểm tra lại bản ReZygisk của bạn có hỗ trợ Soft Reboot trên Android 12 hay không.
5. **Khi nào cần gửi log:** Sau khi chắc chắn LSPosed *Activated*, tick đủ 3 scope và đã Soft Reboot mà app vẫn báo chưa bật.

---

## 2. Thẻ `Root: Chưa cấp quyền` hoặc nút bấm báo `Lỗi Root`

1. **Biểu hiện:** Mục Root màu đỏ, `/sys/class/dp_sec/dex` hiện `(cần root)`, bấm công tắc bị bật ngược lại.
2. **Nguyên nhân có thể:** Chưa cấp quyền `su` cho `com.dex120.s22` trong **KernelSU Manager** (hoặc Magisk), hoặc máy đã Hard Reboot làm mất **Temproot**.
3. **Cách kiểm tra:** Mở **KernelSU Manager** xem KernelSU còn báo *Working* và mục **Superuser** đã bật `DeX 120Hz S22` chưa.
4. **Cách xử lý:** Bật quyền Superuser cho `com.dex120.s22` trong KernelSU Manager → quay lại app bấm **Làm mới**.

---

## 3. TestUFO hoặc game trong DeX rớt xuống dưới 60fps (24–45fps)

1. **Biểu hiện:** Màn hình ngoài nhận 75Hz/120Hz nhưng chạy `testufo.com` chỉ ra ~24–45fps, cảm giác giật rõ khi máy ấm lên.
2. **Nguyên nhân có thể:**
   - Khi S22 nóng tới ngưỡng nhiệt SIOP (`siop_level = 4`, khoảng ~44°C pin), tiến trình `com.sec.android.sdhms` gửi lệnh Binder `1122` (`DFS = 30`) xuống `SurfaceFlinger`, khóa nhịp VSync toàn hệ thống xuống **30Hz (`33.33ms`)**.
   - Nếu bạn quên tick **`com.sec.android.sdhms`** trong LSPosed scope, **Hook 5 (`SdhmsDfsHook`)** chưa thể chặn trực tiếp trong `sdhms`.
3. **Cách kiểm tra:** Chạy chẩn đoán → xem phần `2. Settings` có dòng `SurfaceFlinger: DFS = 30` không, hoặc qua ADB:
   ```bash
   adb shell su -c "logcat -d -s SurfaceFlinger | grep 'DFS =' | tail -n 10"
   ```
4. **Cách xử lý:**
   - Tick thêm **`com.sec.android.sdhms`** trong LSPosed (*Hiện ứng dụng hệ thống* để tìm) → **Soft Reboot**.
   - Tạm thời trong app, nhấn nút **Ép cứng 120Hz** — nút này tự gọi `service call SurfaceFlinger 1122 i32 60` để xóa khóa `DFS=30` ngay lập tức.
   - Bổ sung tản nhiệt / quạt cho điện thoại nếu dùng DeX nặng hoặc chơi game lâu.

---

## 4. Màn hình 75Hz báo `OSD 75Hz` nhưng TestUFO & màn phản chiếu (View) kẹt đúng 60fps

1. **Biểu hiện:** Màn hình VSP 75Hz (hoặc màn 75Hz khác) báo 75Hz ở OSD và `Desktop` ở `75.0 Hz`, nhưng khi màn hình điện thoại bật sáng, `TestUFO` giữ đúng `60 fps`.
2. **Nguyên nhân có thể:**
   - Disassembly `/system/bin/surfaceflinger` trên `SM-S901B` cho thấy: khi cắm màn ngoài chậm hơn 120Hz (ví dụ `75Hz`), Samsung native `SurfaceFlinger::performSetActiveConfig` ép màn điện thoại từ 120Hz về 60Hz (`desiredActiveMode.configId 0 is change to 2`).
   - Kết hợp với `/vendor/build.prop` đặt `vendor.hwc.exynos.vsync_mode=0`, khiến `SurfaceFlinger` lấy chu kỳ VSync theo **màn chậm hơn** (`16.67ms` = 60Hz) chừng nào Display 0 còn bật ở 60Hz.
3. **Cách kiểm tra:**
   ```bash
   adb shell su -c "dumpsys SurfaceFlinger | grep -E 'VSYNC period|mIdealPeriod|powerMode=|activeMode='"
   ```
4. **Cách xử lý:**
   - Kiểm tra với **màn hình điện thoại tắt bằng nút nguồn** xem `mIdealPeriod` có về `13.33` (75Hz) không, hoặc trên màn **120Hz thật** (không bị lệch nhịp 75Hz vs 120Hz) vấn đề ép 60Hz ở `performSetActiveConfig` sẽ không kích hoạt.
   - Không chạy **Test Mode (`dex120_force_virtual_120`)** trên màn 75Hz (sẽ gây skip frame).

---

## 5. Sau khi thoát game hoặc khởi động lại, màn điện thoại bị tụt về 60Hz, bấm ép 120Hz không ăn

1. **Biểu hiện:** `settings get system min_refresh_rate` / `peak_refresh_rate` quay về `60`, hoặc kẹt ở `120.1`, hoặc `Cảnh báo 120.1 Hz (SamHelper)`.
2. **Nguyên nhân có thể:**
   - **Xung đột với SamHelper (`120.1`):** SamHelper ghi `120.1`, trong khi mode 120Hz thật của panel S22 là `120.00001 Hz` nên `DisplayModeDirector` từ chối khóa 120Hz.
   - **Xung đột với module KernelSU / app tối ưu game bên thứ ba** (như `ksu_ultimate_gaming_x` nếu file trạng thái bị detect nhầm `DISPLAY_MAX_HZ='60'` hoặc `orig_display.env` lưu `120.1`): app/module đó tự ghi đè `min=60` / `peak=60` vào `settings_system.xml`. Giá trị này được Android lưu xuống đĩa nên **tồn tại cả sau Soft Reboot lẫn Hard Reboot**!
3. **Cách kiểm tra:**
   ```bash
   adb shell "echo min=$(settings get system min_refresh_rate) peak=$(settings get system peak_refresh_rate) sec_rrm=$(settings get secure refresh_rate_mode)"
   ```
4. **Cách xử lý:**
   - Tạm tắt hoặc sửa module/app xung đột trước (nếu không nó sẽ ghi đè lại `60` khi mở game).
   - Vào app **DeX 120Hz S22** bấm **Ép cứng 120Hz** (đặt `secure.refresh_rate_mode=1` và `min/peak=120.0`), hoặc chạy qua ADB:
     ```bash
     adb shell settings put secure refresh_rate_mode 1
     adb shell settings put system min_refresh_rate 120.0
     adb shell settings put system peak_refresh_rate 120.0
     ```

---

## 6. Cắm màn hình 120Hz/144Hz nhưng không thấy mode 120Hz trong danh sách

1. **Biểu hiện:** Trong `Supported:` của màn `EXTERNAL` chỉ hiện tới `1920x1080@60` hoặc `2560x1440@60`.
2. **Nguyên nhân có thể:**
   - Cáp hoặc Hub USB-C đời cũ chỉ có băng thông **HDMI 1.4** hoặc **2-lane HBR2** (2-lane không đủ cho `1440p@120Hz`, chỉ kéo được `1080p@120Hz`).
   - Màn hình yêu cầu bật cổng HDMI 2.0/2.1 hoặc tắt chế độ tiết kiệm/tương thích trong menu OSD của chính màn hình.
   - Hook 1 (`DpSysfsHook`) chưa chạy nên kernel vẫn ở `MODE_FILTER_DEX`.
3. **Cách kiểm tra:** Kiểm tra `/sys/class/dp_sec/dex` trong app phải là `0` (không phải `16`/`17`), và kiểm tra `dmesg | grep -E "exynos-dp|filter out|best mode"` để xem kernel đọc được EDID nào từ màn hình.
4. **Cách xử lý:** Đổi sang độ phân giải `1080p` nếu dùng hub USB 3.0 2-lane, hoặc dùng cáp USB-C → DisplayPort/HDMI trực tiếp (4-lane) để thử `1440p@120Hz`.

---

## 7. Không thoát được khỏi "Màn đen AMOLED"

- Chạm nhanh liên tiếp **3 lần** vào màn hình (trong vòng 700ms), hoặc bấm nút **Back 2 lần** (trong vòng 1.2 giây). Nếu cần gấp, bấm nút nguồn 2 lần hoặc thao tác qua ADB `adb shell input keyevent KEYCODE_BACK KEYCODE_BACK`.

---

## 8. Rút cáp HDMI ra nhưng DeX chờ ~20 giây mới tắt hoàn toàn

- Đây là lỗi đã gặp ở bản hook sớm khi `dp_sec/dex` luôn trả `"1"` dù màn ngoài đã rút. Từ bản `v1.0.0-s22-a12`, `DpSysfsHook` chỉ giả lập `"0" → "1"` khi `DisplayModeHook.hasActiveExternalDisplay() == true`. Nếu vẫn bị, kiểm tra lại đã cập nhật đúng APK mới nhất và đã Soft Reboot sau khi cài chưa.

---

## 9. Vấn đề Play Integrity / Ngân hàng / Anti-cheat game

- **Quan trọng:** Không phải mọi lỗi Play Integrity hoặc bị app ngân hàng chặn đều do `DeX 120Hz S22`. Bản thân việc root máy, mở LSPosed hoặc cài module spoof thiết bị khác đã có thể khiến Play Integrity / app phát hiện root từ chối chạy.
- **DeX 120Hz S22 không chứa tính năng ẩn root, giả mạo chứng thực phần cứng (attestation) hay bypass anti-cheat game.** Nếu nghi xung đột trên một app cụ thể, hãy kiểm tra phạm vi (Scope) LSPosed của `DeX 120Hz S22` — chỉ nên tick đúng 3 scope (`android`, `com.sec.android.sdhms`, `com.dex120.s22`), **không** tick game hay app ngân hàng vào scope của module này.

---

## 10. Thu thập Diagnostic Report an toàn để báo lỗi

1. Cắm màn hình ngoài và tái hiện lỗi (nếu lỗi xảy ra khi chạy DeX).
2. Mở app **DeX 120Hz S22** → cuộn xuống mục **5. Chẩn đoán & Thu thập Log**.
3. Bấm **Chạy chẩn đoán** → chờ vài giây cho đến khi hộp terminal hiện đủ mục `=== 0` tới `=== 5`.
4. Bấm **Copy Log**. Ứng dụng sẽ chạy qua `RootShell.sanitizeDiagnostics()` để tự ẩn:
   - Serial (`ro.serialno`, Samsung serial)
   - IMEI, MAC, IP nội bộ (`10.x`, `172.16-31.x`, `192.168.x`, `127.0.0.1`)
   - Email, Token GitHub, Mật khẩu / Secret
5. **Tự kiểm tra lại bằng mắt 1 lần nữa** trước khi dán vào [mẫu Bug Report trên GitHub Issues](https://github.com/hangothanhh/DeX120Hz-S22/issues/new?template=bug_report.md).
