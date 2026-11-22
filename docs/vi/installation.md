# Hướng dẫn Cài đặt từ A đến Z (Cho người mới)

**Ngôn ngữ / Language:** [English](../installation.md) | **Tiếng Việt**

> [!WARNING]
> **Chỉ áp dụng cho Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`) chạy Android 12 / One UI 4.1 đã root.**
> Trước khi bắt đầu, hãy sao lưu toàn bộ dữ liệu quan trọng và đọc trước [hướng dẫn khôi phục (Recovery)](recovery.md).
> Nếu máy bạn đang dùng **KernelSU Temproot** (bootloader chưa mở khóa), **tuyệt đối không bấm Reboot thường** — chỉ dùng **Soft Reboot** để nạp hook.

---

## 1. Mục đích
Tài liệu này giúp bạn tự cài đặt, kích hoạt đúng 3 phạm vi (Scope) trong LSPosed, cấp quyền Root và xác minh xem `DeX 120Hz S22` đã hoạt động thật trên máy chưa — ngay cả khi bạn chưa từng dùng Xposed/LSPosed trước đây.

---

## 2. Giải thích nhanh các thuật ngữ bạn sẽ gặp

- **Root (`su`):** Quyền quản trị cao nhất trên Android, giúp app đọc/ghi các cài đặt hệ thống mà app thường không được phép chạm tới.
- **KernelSU / Magisk:** Công cụ cấp và quản lý quyền Root trên điện thoại.
- **Temproot:** Kiểu root tạm thời trên máy chưa mở khóa bootloader. Root và các module Zygisk/LSPosed **chỉ tồn tại trong phiên chạy hiện tại** — nếu bạn khởi động lại hoàn toàn (*Hard Reboot*), máy quay về Android gốc sạch sẽ (không root).
- **Soft Reboot (Khởi động mềm):** Chỉ khởi động lại tầng Android (`zygote` + `system_server`) mà **không** tắt kernel, nhờ đó giữ được Temproot trong khi vẫn nạp các module LSPosed mới cài.
- **Zygisk / ReZygisk:** Lớp nền giúp tiêm (inject) module vào tiến trình Android khi khởi động.
- **LSPosed:** Công cụ chạy trên Zygisk cho phép **DeX 120Hz S22** can thiệp vào những hàm kiểm soát 60Hz của Samsung mà không cần sửa file hệ thống ROM.
- **Scope (Phạm vi):** Danh sách các tiến trình/app mà module LSPosed được phép can thiệp. Tick thiếu Scope thì hook tương ứng sẽ **không chạy**.

---

## 3. Bạn cần chuẩn bị gì?

1. **Thiết bị đúng chuẩn:** Galaxy S22 Exynos (`SM-S901B`), **Android 12 / One UI 4.1**. *(Kiểm tra tại: Cài đặt → Thông tin điện thoại → Thông tin phần mềm.)*
2. **Root đang hoạt động:** KernelSU Manager báo đã có root (hoặc Magisk hoạt động bình thường).
3. **Zygisk + LSPosed đã bật:** Ví dụ module `ReZygisk` + `LSPosed` đang bật trong KernelSU Manager, và bạn mở được giao diện **LSPosed Manager**.
4. **Cáp / Hub xuất hình & màn hình ngoài >60Hz:**
   - Hub USB 3.0 → HDMI thông thường (2-lane) xuất được tới **`1080p@120Hz`**.
   - Muốn **`1440p@120Hz`**, cần cáp USB-C → DisplayPort/HDMI trực tiếp (4-lane).

---

## 4. Các bước thực hiện

### Bước 1: Tải đúng file APK
1. Mở [trang Releases trên GitHub](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/releases/latest).
2. Tải **2 file** sau (không tải `Source code (zip)`):
   - `DeX120Hz-S22.apk`
   - `DeX120Hz-S22.apk.sha256`

### Bước 2: Kiểm tra SHA-256 (bắt buộc để tránh file hỏng/giả mạo)
Mở terminal/shell tại thư mục chứa 2 file vừa tải:

- **Windows (PowerShell / CMD):**
  ```powershell
  certutil -hashfile DeX120Hz-S22.apk SHA256
  ```
- **Linux hoặc Android (Termux):**
  ```bash
  sha256sum DeX120Hz-S22.apk
  ```
- **macOS:**
  ```bash
  shasum -a 256 DeX120Hz-S22.apk
  ```

Mở file `DeX120Hz-S22.apk.sha256` bằng trình xem chữ bất kỳ (Notepad/TextEdit) và đối chiếu chuỗi 64 ký tự hex. Hai chuỗi phải **trùng khớp 100%** (không phân biệt chữ hoa/thường).

### Bước 3: Cài đặt APK lên điện thoại
- **Cách A (Trực tiếp trên điện thoại):** Nhấn vào file `DeX120Hz-S22.apk` vừa tải về và bấm **Cài đặt** *(bật "Cài đặt ứng dụng không rõ nguồn gốc" nếu Android hỏi)*.
- **Cách B (Qua ADB từ máy tính Windows/Linux/macOS):**
  ```bash
  adb install -r DeX120Hz-S22.apk
  ```
  Kết quả mong đợi trên màn hình lệnh: `Success`.

### Bước 4: Bật module và chọn đủ 3 Scope trong LSPosed
> [!CAUTION]
> Kiểm tra lại **ReZygisk** của bạn có hoạt động tốt với **Soft Reboot** chưa trước khi bấm Soft Reboot ở bước sau. Nếu bấm nhầm **Reboot (Khởi động lại)** thường trên máy **Temproot**, bạn sẽ mất temproot và phải chạy lại quy trình kích hoạt temproot của mình.

1. Mở ứng dụng **LSPosed** (hoặc mở qua thông báo LSPosed / trình quay số tùy phiên bản LSPosed bạn cài).
2. Vào mục **Modules (Mô-đun)**.
3. Nhấn vào **`DeX 120Hz S22`** và gạt công tắc **Enable module (Bật mô-đun)**.
4. Bấm vào nút menu góc trên (biểu tượng 3 chấm) → tick bật **System apps (Hiển thị ứng dụng hệ thống)** để hiện các dịch vụ Samsung ẩn.
5. Tick chọn **đủ 3 ứng dụng/tiến trình** sau (giao diện từng bản LSPosed có thể gọi hơi khác, hãy tìm theo tên gói bên trong ngoặc):
   - **`Hệ thống Android`** (`android` / *System Framework*) — bắt buộc cho Hook 1, 2, 3, 4.
   - **`com.sec.android.sdhms`** (*Samsung Device Health Manager Service*) — bắt buộc cho Hook 5 chống tụt VSync 30Hz khi nóng máy.
   - **`DeX 120Hz S22`** (`com.dex120.s22`) — để app tự nhận diện được `LSPosed: ĐÃ KÍCH HOẠT`.

### Bước 5: Soft Reboot (Khởi động mềm)
1. Trong **KernelSU Manager** hoặc menu khởi động của **LSPosed**, chọn **Soft Reboot (Khởi động lại nhanh / Userspace reboot tùy tên bản bạn dùng)** — tức là **khởi động lại phần Android, không sập kernel**.
2. Chờ khoảng 20–40 giây cho tới khi điện thoại vào lại màn hình khóa.

### Bước 6: Lần mở app đầu tiên & Cấp quyền Root
1. Mở app **DeX 120Hz S22** từ màn hình chính.
2. Nếu đây là lần đầu, **KernelSU Manager / Magisk** có thể hỏi quyền **Root (`su`)** — nếu không tự popup, hãy vào trực tiếp **KernelSU Manager → Superuser** và bật cấp quyền cho **`DeX 120Hz S22`** (`com.dex120.s22`).
3. Quay lại app, kiểm tra 2 thẻ trên cùng:
   - **`LSPosed: ĐÃ KÍCH HOẠT`** (nền xanh lá)
   - **`Root: Sẵn sàng`** (nền xanh lá)
4. Kiểm tra các công tắc ở mục **4. Công tắc Hook**:
   - Bật cả 5 công tắc chính (`Bật toàn bộ`, `Hook 1`, `Hook 2`, `Hook 3`, `Hook 4`).
   - **Tắt** mục cuối cùng (`Test Mode (màn 60Hz): Ép Desktop 120Hz`), trừ khi bạn đang cố tình thử trên màn 60Hz để kiểm tra log `VirtualDisplay`.

---

## 5. Kết quả mong đợi & Cách kiểm tra

1. Cắm màn hình ngoài >60Hz (75Hz, 120Hz...) qua USB-C để khởi động **Samsung DeX**.
2. Nhấn **Làm mới thông số Display / dp_sec** trong app:
   - Phải thấy dòng `EXTERNAL` hiển thị độ phân giải & tần số quét cao (ví dụ `1920x1080 @ 75.00 Hz` hoặc `120.00 Hz`).
   - Dòng `VIRTUAL "Desktop"` phải khớp 1:1 với Hz thật của màn ngoài (không bị kẹt `60.00 Hz`).
   - Dòng `/sys/class/dp_sec/dex` giữ ở `0` (Kernel DP ở chế độ Mirror thay vì `16/17`).
3. Nhấn **Chạy chẩn đoán** → xem mục `3. Logcat DeX120Hz` có đủ log cài hook (`Hook initialization complete: 4/4 modules installed`) và log nâng mode (`Upgraded external display ... baseModeId`, `Synced VirtualDisplay Desktop mMode`).

Để kiểm tra sâu hơn bằng **OSD màn hình**, **`dmesg`** và **TestUFO**, xem tiếp [docs/testing-and-verification.md](testing-and-verification.md).

---

## 6. Lỗi thường gặp ngay sau khi cài

- **Thẻ báo `LSPosed: CHƯA BẬT`:** Chưa tick `DeX 120Hz S22` (và `android`) trong LSPosed, hoặc bạn chưa **Soft Reboot**.
- **Thẻ báo `Root: Chưa cấp quyền`:** Vào **KernelSU Manager → Superuser** bật quyền root cho `com.dex120.s22`, rồi bấm *Làm mới*.
- **Cắm màn hình vẫn chỉ thấy 60Hz:** Kiểm tra lại `Test Mode`, kiểm tra cáp/hub, hoặc đối chiếu [docs/troubleshooting.md](troubleshooting.md).

Nếu gặp crash `system_server` sau khi Soft Reboot, làm theo [docs/recovery.md](recovery.md).
