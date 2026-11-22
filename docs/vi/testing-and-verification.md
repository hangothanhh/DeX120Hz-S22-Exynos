# Quy trình Kiểm tra & Xác minh Tần số quét thật

**Ngôn ngữ / Language:** [English](../testing-and-verification.md) | **Tiếng Việt**

## 1. Mục đích
Không tin cảm giác mắt nhìn hay thông số ảo của một layer đơn lẻ. Tài liệu này tổng hợp **từ cách dễ nhất (không cần máy tính)** tới **chuyên sâu qua ADB** để xác minh Samsung DeX có thật sự chạy trên 60Hz ở cả phần cứng lẫn phần mềm hay không.

---

## 2. Bạn cần chuẩn bị gì?
- Đã cài và bật đủ 3 scope LSPosed cho `DeX 120Hz S22` ([docs/installation.md](installation.md)).
- Cắm cáp/hub USB-C sang màn hình hỗ trợ >60Hz (ví dụ 75Hz, 120Hz, 144Hz).
- *(Tùy chọn cho bước nâng cao)* Máy tính có `adb` + `scrcpy` bật Gỡ lỗi USB (USB Debugging).

---

## 3. Các tầng kiểm tra (từ nhanh đến chi tiết)

### Tầng 1: Kiểm tra ngay trên màn hình rời (Không cần ADB)
1. **Menu OSD (On-Screen Display) của chính màn hình ngoài:**
   - Bấm nút cứng trên màn hình (Menu / Info / Signal).
   - **Kết quả mong đợi:** Phải hiển thị **`1920x1080 75Hz`** (hoặc `120Hz`, tùy màn hình của bạn), thay vì `60Hz` như stock DeX. Đây là bằng chứng **phần cứng nhận đúng tín hiệu >60Hz**.
2. **TestUFO trên trình duyệt trong DeX:**
   - Mở trình duyệt (Chrome / Samsung Internet) trên màn hình DeX, truy cập `https://www.testufo.com`.
   - Để tránh bị bóp VSync khi màn hình chính tắt hoặc khác nhịp (đặc biệt với màn 75Hz), mở **Màn đen AMOLED** trong app hoặc giữ màn hình chính đúng trạng thái cần test.
   - **Kết quả mong đợi:** Ô *Refresh Rate* / *Frame Rate* vượt 60fps (ví dụ ~`75 fps` trên màn 75Hz). Nếu rớt 24–45fps khi máy ấm, kiểm tra xem bạn đã tick **`com.sec.android.sdhms`** trong LSPosed để chặn `DFS=30` chưa.

### Tầng 2: Kiểm tra nhanh trong app `DeX 120Hz S22`
Mở app → mục **2. Trạng thái màn hình & Kernel DP** → bấm **Làm mới** → đối chiếu:
- Thẻ `LSPosed: ĐÃ KÍCH HOẠT` và `Root: Sẵn sàng` đều xanh lá.
- Màn `EXTERNAL` có `Active: ... @ 75.00 Hz` (hoặc `120.00 Hz`).
- Màn `VIRTUAL "Desktop"` có `Active` khớp 1:1 với màn `EXTERNAL`.
- `/sys/class/dp_sec/dex = 0`.
- `refresh_rate_mode (Secure) = 1 (Tương thích)` và `min/peak_refresh_rate = 120.0 / 120.0 Hz`.

### Tầng 3: Kiểm tra chuyên sâu qua ADB (yêu cầu Root)

Kết nối ADB qua USB hoặc Wi-Fi và chạy từng lệnh kiểm tra sau:

1. **Kiểm tra kernel DisplayPort/DPU có xuất đúng FPS không (`dmesg`):**
   ```bash
   adb shell su -c "dmesg | grep -E 'exynos-drmdecon.*bts\.fps|exynos-dp|dp_sec|best mode' | tail -n 20"
   ```
   - **Mong đợi:** Thấy `exynos-drmdecon[2]: decon_mode_update_bts_fps bts.fps:75` (hoặc `120`) cho màn ngoài!

2. **Kiểm tra trạng thái `DisplayModeDirector` & `VirtualDisplay "Desktop"` (`dumpsys display`):**
   ```bash
   adb shell su -c "dumpsys display | grep -E 'mDisplayId=|mActiveModeId=|mBaseModeId=|primaryRefreshRateRange|DisplayDeviceInfo|\"Desktop\"|type EXTERNAL' | head -n 40"
   ```
   - **Mong đợi:** Cả `EXTERNAL` và `"Desktop"` đều đang ở mode >60Hz; `mDesiredDisplayModeSpecs` của màn ngoài có `primaryRefreshRateRange=[... 120]` (không bị kẹt `[60 60]`).

3. **Kiểm tra chu kỳ VSync thật của `SurfaceFlinger` & `DFS`:**
   ```bash
   adb shell su -c "dumpsys SurfaceFlinger | grep -E 'DisplayDevice\{|activeMode=|VSYNC period|app duration|mIdealPeriod|workDuration' | head -n 25"
   adb shell su -c "logcat -d -s SurfaceFlinger | grep -E 'DFS =|Dynamic FPS' | tail -n 10"
   ```
   - **Mong đợi:** Không còn `DFS = 30` hay `VSYNC period: 33333333 ns` (30Hz). Trên màn 75Hz cần thấy `mIdealPeriod=13.33` / `workDuration: 13.33ms`; trên màn 120Hz là `8.33ms`.

4. **Kiểm tra log của chính 5 lớp hook (`logcat` / LSPosed log):**
   ```bash
   adb shell su -c "logcat -d -s DeX120Hz:I | tail -n 40"
   ```
   - Hoặc đọc log mới nhất trong `/data/adb/lspd/log/modules_*.log` (nếu `logcat` bị trôi):
     ```bash
     adb shell su -c "grep 'DeX120Hz' \$(ls -t /data/adb/lspd/log/modules_*.log | head -n 1) | tail -n 30"
     ```

### Tầng 4: Kiểm tra qua `scrcpy` (Lưu ý thông số `--display-id`)
Nếu muốn đo FPS của VirtualDisplay `"Desktop"` trên PC qua `scrcpy`:
1. Tìm đúng Display ID đang chạy:
   ```bash
   scrcpy --list-displays
   ```
2. Mirror đúng ID của màn DeX/`Desktop` (không phải ID 0 của điện thoại):
   ```bash
   scrcpy --display-id=<ID_DeX> --max-fps=120 --print-fps --no-audio
   ```
- **Lưu ý:** `scrcpy --print-fps` chỉ báo FPS khi trên màn hình đang có chuyển động liên tục (ví dụ khi kéo cuộn hoặc chạy TestUFO) và phụ thuộc tốc độ encode Wi-Fi/USB, vì vậy **OSD màn hình + `dmesg` + `dumpsys SurfaceFlinger`** luôn là tiêu chuẩn chính xác hơn.

---

## 4. Lỗi thường gặp khi kiểm tra & Cách xử lý
- `TestUFO` giữ đúng **60 fps** dù OSD báo **75Hz**: đọc mục *Màn 75Hz bị kẹt 60fps* trong [docs/troubleshooting.md](troubleshooting.md).
- `TestUFO` rớt xuống **24–45 fps**: đọc mục *TestUFO dưới 60fps / SDHMS DFS=30* trong [docs/troubleshooting.md](troubleshooting.md).
- Không khôi phục được cấu hình sau khi test: xem [docs/recovery.md](recovery.md).
