# Kiến trúc kỹ thuật & Cơ chế Hoạt động (S22 Exynos · Android 12 / One UI 4.1)

> [!NOTE]
> Tài liệu này giải thích chi tiết từng nút thắt 60Hz / 30Hz đã được phân tích thực tế trên **Galaxy S22 Exynos (`SM-S901B` / `s5e9925`, Android 12 / One UI 4.1)** và cách 5 lớp hook của `DeX 120Hz S22` xử lý chúng.

---

## 1. Mục đích
Giúp người đóng góp và người dùng nâng cao hiểu rõ **tại sao** Samsung DeX bị khóa ở 60Hz (và 30Hz khi nóng máy), từng file Java trong `app/src/main/java/com/dex120/s22/xposed/` móc vào đâu, và những giới hạn phần cứng/native nào không thể vượt qua chỉ bằng hook Java.

---

## 2. Đường đi của một khung hình trong Samsung DeX (Pipeline)

Trên Samsung DeX (Android 12), giao diện desktop không vẽ trực tiếp lên cổng HDMI mà đi qua chuỗi 4 tầng:

```text
[App / Trình duyệt / Game trong DeX]
        │  (VSync từ SurfaceFlinger / DisplayModeDirector + SDHMS)
        ▼
[VirtualDisplay "Desktop"]          ◄── Hook 4: VirtualDisplaySyncHook
        │  (mMode / renderFrameRate đồng bộ Hz thật)
        ▼
[DisplayModeDirector & DMS Tokens]  ◄── Hook 2 & 3: LowRefreshTokenHook + DisplayModeHook
        │  (baseModeId, RefreshRateRange, chặn HardwareManager & SiopLimiterHrr)
        ▼
[Kernel DisplayPort (dp_sec / decon2)] ◄── Hook 1: DpSysfsHook
        │  (sysfs /sys/class/dp_sec/dex giữ Mirror mode, không lọc 60Hz)
        ▼
[Cáp / Hub USB-C → HDMI/DP] → [Màn hình ngoài (75Hz / 120Hz)]

Nhánh song song gây bóp FPS khi nóng máy:
[com.sec.android.sdhms (SIOP)] ──(Binder tx 1122: DFS=30)──▶ [SurfaceFlinger]
        ▲
        └── Hook 5: SdhmsDfsHook (chặn trong SDHMS + ép reset DFS=60 từ system_server)
```

---

## 3. Chi tiết 5 lớp Hook

### Hook 1: `DpSysfsHook` (Kernel DisplayPort Mirror Bypass)
- **Target:** `com.android.server.desktopmode.Utils` (`writeFile`, `readFile`) và `com.android.server.desktopmode.DisplayPortStateManager` trong `system_server`.
- **Vấn đề gốc:** Khi DeX bật, Samsung `DesktopModeService` ghi cờ `DEX_ON` (`16`/`0x10` hoặc `17`/`0x11`) vào `/sys/class/dp_sec/dex`. Kernel driver DisplayPort của Exynos chuyển từ `MODE_FILTER_MIRROR` sang `MODE_FILTER_DEX`, lọc bỏ toàn bộ chế độ >60Hz hoặc >1080p60 không nằm trong whitelist DeX.
- **Cách xử lý:**
  - `handleWriteFileBefore`: nếu ghi giá trị số có bit `0xF0` (`16`, `17`...) vào `dp_sec/dex`, cắt bit cao (`oldVal & 0x0F` → `0` hoặc `1`) để kernel giữ `MODE_FILTER_MIRROR`, đánh dấu `sSimulatedDexOn = true`. Lệnh `SkipAdapterCheck` và `HMD` không bị đụng tới.
  - `handleReadFileAfter`: một số luồng kiểm tra đọc lại `/sys/class/dp_sec/dex`. Hook chỉ giả lập `"0" → "1"` khi **cả hai** điều kiện `sSimulatedDexOn == true` **và** `DisplayModeHook.hasActiveExternalDisplay() == true` cùng đúng. Nếu rút cáp (`extActive == false`), không giả lập nữa để tránh kẹt ~20 giây khi ngắt DeX.

### Hook 2: `LowRefreshTokenHook` (Chặn khóa ≤60Hz toàn cục)
- **Target:** `DisplayManagerService`, `$BinderService`, `$LocalService`: `acquireLowRefreshRateToken` (trả về `ILowRefreshRateToken`) và `acquireRefreshRateMaxLimitToken` (trả về `IRefreshRateMaxLimitToken`).
- **Vấn đề gốc:** Khi vào DeX, `[DMS]HardwareManager` xin `ILowRefreshRateToken` để khóa toàn bộ hệ thống về 60Hz; khi máy ấm lên trong DeX, `SiopLimiterHrr` cũng xin token khóa 60Hz.
- **Cách xử lý:**
  - Khi nguồn gọi thuộc DeX/Desktop (`isDexCaller`) **hoặc** đang có màn ngoài active (`DisplayModeHook.hasActiveExternalDisplay()`) và mức trần `<= 60Hz`, hook chặn lời gọi thật và trả về một `Proxy` giả riêng biệt theo **từng interface** lưu trong `ConcurrentHashMap<Class<?>, Object> sDummyTokens` (tránh `ClassCastException` giữa `ILowRefreshRateToken` và `IRefreshRateMaxLimitToken`).
  - Đồng thời kích hoạt `SdhmsDfsHook.scheduleDfsReset("token:" + tag)` vì `SiopLimiterHrr` thường xuất hiện cùng thời điểm với `DFS=30`.

### Hook 3: `DisplayModeHook` (`DisplayModeDirector` & Bảo vệ Display 0)
- **Target:** `DisplayModeDirector.getDesiredDisplayModeSpecs`, `updateVoteLocked`, `DisplayObserver`, `LogicalDisplay.getDisplayInfoLocked`, `RealInjector.getDisplayInfo`.
- **Vấn đề gốc:**
  1. `DisplayModeDirector` mặc định chọn `baseModeId` 60Hz và có vote giới hạn `RefreshRateRange.max = 60.0` cho màn ngoài (`type == TYPE_EXTERNAL`).
  2. Khi dùng chuột/bàn phím trên DeX mà không chạm màn điện thoại, LTPO trên Display 0 (`samsung lcd`) có thể hạ về 24Hz/60Hz nếu không khóa.
- **Cách xử lý:**
  - Theo dõi trạng thái màn hình ngoài chính xác (đồng bộ với `mSupportedModesByDisplay` và `DisplayObserver.onDisplayRemoved`).
  - Với màn ngoài (`isExternalDisplay(displayId)`): `findHighestModeSameRes` tìm `Display.Mode` cùng độ phân giải có `fps` cao nhất không vượt `Config.getTargetHz()` (mặc định 120Hz), nâng `baseModeId` và mở rộng `primaryRefreshRateRange` / `appRequestRefreshRateRange` lên tối đa 120Hz.
  - Với Display 0 khi `sExternalDisplayActive`: `protectInternalDisplaySpecs` cố gắng giữ `baseModeId` 120Hz và khóa `primaryRefreshRateRange` ở `[120, 120]`. *(Xem mục 5 về giới hạn native khi cắm màn 75Hz.)*

### Hook 4: `VirtualDisplaySyncHook` (`Desktop` VirtualDisplay)
- **Target:** `DisplayManagerService.configureDisplayLocked`, `VirtualDisplayAdapter$VirtualDisplayDevice` (`getDisplayDeviceInfoLocked`, `resizeLocked`, constructor).
- **Vấn đề gốc:** Màn hình ảo `"Desktop"` của Samsung DeX bị khởi tạo ở `60.0 Hz`, nên dù màn hình HDMI chạy 75Hz hoặc 120Hz, DeX vẫn vẽ ở 60Hz.
- **Cách xử lý:**
  - Đọc tần số quét thực của thiết bị màn ngoài trong `configureDisplayLocked` (`sLastExternalHz`).
  - Khi `isDesktopVirtualDevice` khớp `"Desktop"` (và `Config.isSyncVirtual()` bật): tạo `Display.Mode` mới với `targetHz = Math.min(sLastExternalHz, Config.getTargetHz())` (hoặc `Config.getTargetHz()` nếu bật Test Mode `dex120_force_virtual_120=1`), cập nhật `mMode`, `mRequestedRefreshRate`, `supportedModes`, `modeId`, `defaultModeId` và báo thay đổi thiết bị về adapter.

### Hook 5: `SdhmsDfsHook` (Chặn `DFS = 30` trong `com.sec.android.sdhms`)
- **Phát hiện gốc từ phân tích firmware thật:** Trên One UI 4.1 (`SamsungDeviceHealthManagerService.apk`), khi nhiệt độ máy tăng trong DeX (ví dụ mức `siop_level = 4`, pin ~44°C), `SDHMS` gọi Binder transaction **`1122` (`0x462`)** tới service `SurfaceFlinger` với interface descriptor `"android.ui.ISurfaceComposer"` và tham số `int 30` (`DFS = 30`), bật *Dynamic FPS VSyncThread* và kéo `VSYNC period` toàn hệ thống từ `8.33ms` / `13.33ms` xuống **`33.33ms` (30Hz)** — khiến TestUFO tụt thảm hại xuống 24–45fps!
- **Cách xử lý 2 lớp:**
  1. Trong `com.sec.android.sdhms`: hook `BinderProxy.transact`, nếu `code == 1122` và `Parcel` có interface `"android.ui.ISurfaceComposer"` với giá trị `0 < dfs < 60`, chặn transaction không cho gửi xuống `SurfaceFlinger` (luôn phục hồi `dataPosition` trong `finally` để không ảnh hưởng binder khác dùng trùng mã `1122`).
  2. Trong `system_server`: hàm `scheduleDfsReset` tự động gửi transaction `1122` với `dfs = 60` (`SurfaceFlinger` tắt Dynamic FPS VSyncThread) ở các mốc trễ `+150ms`, `+1000ms`, `+3000ms` khi màn ngoài bật hoặc khi `SiopLimiterHrr` kích hoạt.

---

## 4. Giải pháp giữ VSync khi muốn tắt màn điện thoại (`BlackScreenActivity`)

Trên Android 12 (`API 31`), khi bạn bấm nút nguồn để tắt hẳn màn hình chính (`Display 0` chuyển `powerMode=Off`), `SurfaceFlinger` có thể chuyển sang bộ tạo nhịp dự phòng (`SyntheticVSync` 60Hz) tùy điều kiện composer.

`BlackScreenActivity` giải quyết việc này bằng cách giữ Display 0 ở trạng thái `ON` (`FLAG_KEEP_SCREEN_ON`, `preferredRefreshRate=120f`, `screenBrightness=0.001f`) trong khi phủ kín màu đen `#000000` (trên tấm nền AMOLED, pixel đen `#000000` gần như không phát sáng, tiết kiệm pin và tránh burn-in mà vẫn giữ nhịp VSync cao cho DeX).

---

## 5. Giới hạn phần cứng & Native không thể né bằng Java Hook

Qua kiểm tra disassembly trực tiếp `/system/bin/surfaceflinger` trên `SM-S901B` (Android 12) và spec SoC Exynos 2200:

1. **Giới hạn riêng khi cắm màn 75Hz (bắt gặp trên VSP `V2203H`):**
   - Trong `SurfaceFlinger::performSetActiveConfig()`, mã native của Samsung kiểm tra nếu có màn ngoài với `vsync period` lớn hơn màn chính (ví dụ `133` cho 75Hz > `83` cho 120Hz), nó tự động ép Display 0 xuống mode có vsync >= 133 → **`configId=2` (60Hz)** (`desiredActiveMode.configId 0 is change to 2, because other display(vsync period = 133) is connected`).
   - Đồng thời trên ROM này `/vendor/build.prop` đặt `vendor.hwc.exynos.vsync_mode=0`, khiến khi cả 2 màn cùng bật (60Hz trên điện thoại + 75Hz trên màn ngoài), VSync ứng dụng chọn màn chậm hơn (`16.67ms` = 60Hz).
   - *Lưu ý:* Với **màn 120Hz thật**, vsync period của màn ngoài (`83`) không lớn hơn 120Hz của Display 0 (`83`), nên logic `performSetActiveConfig` sẽ **không** ép Display 0 về 60Hz.
2. **Băng thông DisplayPort HBR2 (5.4 Gbps/lane):**
   - Cáp/hub 2-lane (chia sẻ USB 3.0 Data): tối đa `1080p@120Hz` (~7.1 Gbps chưa mã hóa 8b/10b, sát ngưỡng 8.64 Gbps khả dụng của 2 lane HBR2). Không kéo nổi `1440p@120Hz`.
   - Cáp USB-C → DisplayPort/HDMI trực tiếp (4-lane HBR2, tối đa 17.28 Gbps): đủ cho `1440p@120Hz` (khoảng 11–13 Gbps tùy CVT/RB).
3. **Throttling CPU/GPU của `SDHMS`:**
   - Module chỉ chặn tụt **tần số quét (DFS 30Hz / token 60Hz)** chứ **không** mở khóa giới hạn nhiệt silicon hay tắt giảm xung CPU/GPU của `SDHMS`.
