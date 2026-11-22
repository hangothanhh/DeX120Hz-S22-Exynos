# Hướng dẫn Cấu hình & Ý nghĩa các Tùy chọn

## 1. Mục đích
Giải thích toàn bộ công tắc, nút bấm và các khóa `Settings.Global` mà `DeX 120Hz S22` đọc/ghi trên S22 Android 12, đồng thời chỉ rõ những xung đột phổ biến với SamHelper hoặc module gaming bên thứ ba.

---

## 2. Bảng khóa `Settings.Global` điều khiển Hook

Mỗi công tắc ở mục **4. Công tắc Hook** trong ứng dụng ghi thẳng một số nguyên (`0` hoặc `1`, trừ `dex120_target_hz`) vào bảng `Settings.Global`. `Config.java` trong `system_server` (và `com.sec.android.sdhms`) tự làm mới bộ nhớ đệm mỗi **2000 ms (2 giây)** — bạn **không** cần Soft Reboot lại khi thay đổi công tắc.

| Khóa `Settings.Global` | Mặc định | Ý nghĩa & Khuyến nghị dùng |
|---|---|---|
| `dex120_enabled` | `1` (Bật) | Công tắc tổng cho tất cả 5 lớp hook. Tắt về `0` nếu muốn tạm ngưng toàn bộ can thiệp mà chưa cần tắt LSPosed. |
| `dex120_block_dp_dex` | `1` (Bật) | Điều khiển **Hook 1 (`DpSysfsHook`)**: chặn ghi `16/17` vào `/sys/class/dp_sec/dex` để kernel không lọc mất mode >60Hz. |
| `dex120_block_token` | `1` (Bật) | Điều khiển **Hook 2 (`LowRefreshTokenHook`)** và **Hook 5 (`SdhmsDfsHook`)**: chặn khóa 60Hz từ `HardwareManager`/`SiopLimiterHrr` và chặn `SDHMS DFS=30`. |
| `dex120_unlock_modes` | `1` (Bật) | Điều khiển **Hook 3 (`DisplayModeHook`)**: nâng `baseModeId` & `RefreshRateRange` màn ngoài vượt 60Hz. |
| `dex120_sync_virtual` | `1` (Bật) | Điều khiển **Hook 4 (`VirtualDisplaySyncHook`)**: đồng bộ `VirtualDisplay "Desktop"` theo Hz thật của màn ngoài. |
| `dex120_force_virtual_120` | `0` (Tắt) | **Test Mode (EXPERIMENTAL):** Ép `VirtualDisplay "Desktop"` luôn báo `120Hz` ngay cả khi màn ngoài chỉ chạy `60Hz` hoặc `75Hz`. **Hãy TẮT (`0`) khi dùng thật** vì nếu không khớp Hz vật lý của màn hình, DeX sẽ rớt khung hình (skip frames). |
| `dex120_target_hz` | `120` | Trần tần số quét tối đa (từ `60` đến `240`) được dùng trong `DisplayModeHook`, `VirtualDisplaySyncHook` và `BlackScreenActivity`. Giá trị ngoài `[60, 240]` tự quay về `120`. |

Nếu muốn chỉnh hoặc xem trực tiếp từ ADB (root):
```bash
adb shell su -c "settings list global | grep '^dex120_'"
adb shell su -c "settings put global dex120_force_virtual_120 0"
```

---

## 3. Công tắc Ép cứng 120Hz vs Khôi phục Adaptive (Mục 3 trong app)

- **Nút `Ép cứng 120Hz`:** Chạy qua `RootShell.applySamHelper120Hz()`:
  ```sh
  settings put secure refresh_rate_mode 1
  settings put system refresh_rate_mode 1
  settings put system min_refresh_rate 120.0
  settings put system peak_refresh_rate 120.0
  settings put global low_power 0
  service call SurfaceFlinger 1122 i32 60
  ```
  - *Lưu ý quan trọng về One UI 4.1:* Trên S22 Android 12, `DisplayModeDirector` đọc **`Settings.Secure.refresh_rate_mode`** (`0` = Chuẩn 60Hz, `1` = Tương thích Seamless 10–120Hz), và panel 120Hz của S22 báo tần số chính xác là **`120.00001 Hz`**. Do đó app đặt **`120.0`** — tuyệt đối tránh giá trị `120.1` của một số công cụ cũ.
- **Nút `Khôi phục Adaptive`:** Chạy `RootShell.resetRefreshRate()` — xóa `min_refresh_rate` và đưa `peak_refresh_rate = 120.0`, `refresh_rate_mode = 1` để điện thoại quay lại dải adaptive gốc khi không cắm DeX.

---

## 4. Cảnh báo xung đột với App / Module khác (Quan trọng!)

> [!WARNING]
> Trong quá trình kiểm thử thực tế trên S22, 2 xung đột sau đây **đã gây rớt về 60Hz** dù hook của `DeX 120Hz S22` vẫn hoạt động bình thường:

1. **SamHelper đặt `min/peak_refresh_rate = 120.1`:**
   - Vì panel S22 có `fps = 120.00001`, mức `120.1` lớn hơn `120.00001` nên bị `DisplayModeDirector` từ chối khóa 120Hz và rơi về `baseModeId=3 (60Hz)` / `[10 120]`.
   - `MainActivity` sẽ hiện cảnh báo nếu phát hiện `min/peak > 120.05` — chỉ cần nhấn **Ép cứng 120Hz** trong app để sửa về `120.0`.
2. **Module quản lý hiệu năng/gaming tự ghi `min/peak_refresh_rate = 60`:**
   - Một số module KernelSU gaming (ví dụ từng quan sát thực tế trên `ksu_ultimate_gaming_x` khi detect lỗi `DISPLAY_MAX_HZ='60'`) tự động ghi `settings put system min_refresh_rate 60` và `peak_refresh_rate 60` mỗi khi mở game, và nếu reboot trước khi restore thì mức `60/60` kẹt vĩnh viễn trong `settings_system.xml`.
   - Nếu thấy máy bị kẹt 60Hz bất thường, hãy kiểm tra lại các khóa:
     ```bash
     adb shell "echo min=$(settings get system min_refresh_rate) peak=$(settings get system peak_refresh_rate) sec_rrm=$(settings get secure refresh_rate_mode)"
     ```
     và kiểm tra log của các module Root khác bạn đang cài.

---

## 5. Khôi phục cấu hình về mặc định
Xem chi tiết cách xóa toàn bộ cấu hình và đưa hệ thống về nguyên trạng tại [docs/recovery.md](recovery.md).
