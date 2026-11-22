# DeX 120Hz S22

**Ngôn ngữ / Language:** **Tiếng Việt** | [English](README.en.md)

Module **LSPosed** và ứng dụng điều khiển đi kèm giúp mở khóa tần số quét cao (trên 60Hz, tối đa 120Hz tùy màn hình và cáp) cho **Samsung DeX** — áp dụng riêng cho **Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`) chạy Android 12 / One UI 4.1 đã root**.

- **Phiên bản hiện tại:** `v1.0.1-s22-a12` (**Pre-release / Experimental**)
- **Gói ứng dụng:** `com.dex120.s22` (`minSdk 31`, `targetSdk 31` — Android 12)
- **Công nghệ sử dụng:** Java 11, Xposed/LSPosed Hooks (`system_server` + `com.sec.android.sdhms`), Android SDK thuần (`aapt2` + `d8` + `apksigner`, không phụ thuộc Gradle nặng)
- **Giấy phép & Tuân thủ:** [GNU GPL v3.0](LICENSE) · [NOTICE](NOTICE) · [Minh bạch AI](AI_CONTRIBUTIONS.md) · [Bên thứ ba](THIRD_PARTY_NOTICES.md)
- **Liên kết dự án:** [Phát hành (Releases)](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/releases) · [Báo lỗi (Issues)](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/issues) · [Đóng góp (Contributing)](CONTRIBUTING.md)

---

## Đọc kỹ trước khi cài — CẢNH BÁO

> [!CAUTION]
> **Minh bạch về đóng góp của AI & cảnh báo AI Slop:**
> Phần lớn mã nguồn Java, script build/kiểm thử và bộ tài liệu này **được tạo hoặc hỗ trợ viết bởi Google Antigravity** (chi tiết phạm vi & tình trạng kiểm thử tại [AI_CONTRIBUTIONS.md](AI_CONTRIBUTIONS.md)), và mới được con người kiểm thử thực tế trước đây trên **duy nhất 1 thiết bị Samsung Galaxy S22 Exynos (`SM-S901B`) chạy Android 12 / One UI 4.1 với màn hình ngoài VSP 75Hz (`V2203H`)** (thiết bị test đã được bán; bản `v1.0.1-s22-a12` chỉ mới kiểm chứng build/unit test trên host & CI).
> Người duy trì không thể kiểm toán thủ công toàn bộ từng nhánh mã nguồn cho mọi cấu hình ROM/màn hình khác nhau. Nếu bạn muốn đóng góp code, hãy đọc kỹ [quy định chống AI Slop](CONTRIBUTING.md#quy-định-về-ai-và-chống-ai-slop) trước khi gửi Pull Request.

> [!WARNING]
> **Rủi ro khi sử dụng công cụ Root & LSPosed:**
> 1. Dự án can thiệp trực tiếp vào tiến trình hệ thống (`system_server` và `com.sec.android.sdhms`) với quyền **Root**. Nếu cài sai phiên bản Android/One UI hoặc cấu hình không tương thích, máy có thể **văng giao diện, treo `system_server` hoặc bootloop (khởi động lặp lại)** — trong trường hợp xấu khi khôi phục có thể mất dữ liệu.
> 2. **Chỉ áp dụng cho Galaxy S22 Exynos (`SM-S901B`) Android 12 / One UI 4.1.** Không cài lên Android 13/14/15 (One UI 5/6/7), bản S22 Snapdragon hoặc các máy Samsung khác nếu bạn chưa tự kiểm chứng lại hook.
> 3. **Lưu ý đặc biệt cho KernelSU Temproot (máy bootloader chưa mở khóa):** Sau khi bật/sửa scope trong LSPosed, **chỉ dùng Soft Reboot (Khởi động mềm)** — không bấm Reboot thường nếu bạn không muốn mất quyền root tạm thời.
> 4. **Không đảm bảo 120Hz trên mọi màn hình, cáp hoặc game:** Màn hình ngoài 120Hz vật lý hiện ở trạng thái **PARTIAL / chưa test trực tiếp** (mới kiểm chứng thực tế vượt trần 60Hz lên **75Hz** trên màn VSP `V2203H`). Ngoài ra, băng thông cổng USB-C / hub HDMI, giới hạn riêng của từng game hoặc chế độ màn hình điện thoại bật/tắt đều ảnh hưởng tới FPS thật.
> 5. **Không vô hiệu hóa bảo vệ nhiệt silicon/CPU/GPU:** Hook chỉ gỡ giới hạn tụt tần số quét màn hình của Samsung (`SiopLimiterHrr` 60Hz và `DFS = 30`), **không** tắt giảm xung CPU/GPU hay bảo vệ quá nhiệt phần cứng. Chơi game nặng vẫn cần tản nhiệt ngoài.
> 6. **Không đảm bảo vượt qua Play Integrity** hay các cơ chế kiểm tra root/anti-cheat của ứng dụng ngân hàng hoặc game.
> 7. **Bạn tự chịu trách nhiệm hoàn toàn** khi cài đặt và sử dụng.

**6 việc bắt buộc trước khi cài đặt:**
1. Sao lưu toàn bộ dữ liệu quan trọng ra máy tính hoặc đám mây.
2. Kiểm tra đúng máy **Galaxy S22 Exynos (`SM-S901B`)**, đang ở **Android 12 / One UI 4.1**.
3. Đảm bảo đã có quyền **Root** (`KernelSU` hoặc tương đương), **Zygisk** (`ReZygisk`) và **LSPosed** đang hoạt động ổn định.
4. Tải đúng file `DeX120Hz-S22.apk` từ [trang Releases](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/releases) (không tải nhầm Source code ZIP).
5. Kiểm tra mã **SHA-256** trước khi cài.
6. Đọc trước [hướng dẫn khôi phục khi lỗi (Recovery)](docs/recovery.md) để biết cách gỡ/tắt module nếu màn hình hoặc `system_server` gặp trục trặc.

---

## Dự án này làm gì?

Trên Android 12 / One UI 4.1 gốc, Samsung khóa **Samsung DeX** tối đa **60Hz** ở nhiều tầng cùng lúc: từ driver DisplayPort trong kernel, dịch vụ quản lý hiển thị (`DisplayModeDirector`), cho đến dịch vụ nhiệt/hiệu năng (`SDHMS`). **DeX 120Hz S22** đặt 5 lớp hook (móc can thiệp thời gian chạy qua LSPosed) cùng một màn hình điều khiển gọn nhẹ:

| Thành phần | Chức năng & Cách hoạt động | Trạng thái | Điều kiện & Hạn chế | Bật / Tắt |
|---|---|---|---|---|
| **Hook 1: `DpSysfsHook`** | Chặn ghi cờ `DEX_ON` (`0x10`/`0x11`) vào `/sys/class/dp_sec/dex` để giữ kernel DP ở chế độ Mirror (không ép lọc mode về 60Hz), đồng thời giả lập giá trị đọc `"1"` chỉ khi đang có màn ngoài active để tránh trễ ~20s lúc rút cáp. | **VERIFIED** (đã test rút/cắm trên VSP 75Hz) | Cần scope `Hệ thống Android (android)` + Soft Reboot. | Công tắc **Hook 1** trong app (`dex120_block_dp_dex`) |
| **Hook 2: `LowRefreshTokenHook`** | Bỏ qua yêu cầu khóa ≤60Hz (`acquireLowRefreshRateToken` và `acquireRefreshRateMaxLimitToken`) từ `[DMS]HardwareManager` và `SiopLimiterHrr` khi có màn ngoài active; lưu proxy token riêng theo từng interface. | **VERIFIED** (đã xác nhận log chặn cả DMS lẫn SiopLimiterHrr) | Chặn trên toàn màn hình khi DeX đang cắm cáp. | Công tắc **Hook 2** trong app (`dex120_block_token`) |
| **Hook 3: `DisplayModeHook`** | Mở khóa `DisplayModeDirector.getDesiredDisplayModeSpecs`: nâng `baseModeId` của màn ngoài lên Hz cao nhất (tới trần cấu hình 120Hz), mở rộng `RefreshRateRange`, và giữ Display 0 ở 120Hz khi đang dùng màn ngoài. | **VERIFIED** ở 75Hz; **PARTIAL** ở 120Hz | Phụ thuộc EDID màn hình hỗ trợ Hz cao qua cáp/hub đang dùng. Không vượt qua được check cứng trong native `/system/bin/surfaceflinger` khi cắm màn 75Hz (xem [docs/architecture.md](docs/architecture.md)). | Công tắc **Hook 3** trong app (`dex120_unlock_modes`) |
| **Hook 4: `VirtualDisplaySyncHook`** | Đồng bộ `VirtualDisplay` tên `"Desktop"` (màn hình ảo nơi DeX vẽ giao diện) khớp 1:1 với Hz thật của màn ngoài (kèm tùy chọn Test Mode ép báo 120Hz trên màn 60Hz để thử nghiệm). | **VERIFIED** (75Hz đồng bộ 1:1; Test Mode 120Hz gây drop frame trên màn 75Hz nên tắt khi dùng thật) | Nếu màn ngoài chỉ 60Hz hoặc 75Hz, **nên tắt Test Mode** để tránh rớt khung hình. | Công tắc **Hook 4** (`dex120_sync_virtual`) & **Test Mode** (`dex120_force_virtual_120`) |
| **Hook 5: `SdhmsDfsHook`** | Hook `BinderProxy.transact` trong `com.sec.android.sdhms` để chặn giao dịch `SurfaceFlinger` mã `1122` (`DFS = 30` khi nóng ~44°C), đồng thời chủ động gửi lệnh `DFS = 60` từ `system_server` để không bị tụt VSync xuống 30Hz. | **VERIFIED** (đã xác nhận chặn `DFS=30` & phục hồi VSync 13.33ms trong `dumpsys SurfaceFlinger`) | Cần tick thêm scope **`com.sec.android.sdhms`** trong LSPosed. Không tắt hạ xung CPU/GPU của SDHMS. | Đi theo công tắc **Hook 2** (`dex120_block_token`) |
| **Màn đen AMOLED** | Mở một cửa sổ đen `#000000` phủ toàn màn hình điện thoại ở độ sáng cực thấp, giữ `Display 0` ở trạng thái **ON** để tránh Android 12 `SurfaceFlinger` rơi về `SyntheticVSync` 60Hz khi bạn bấm nút nguồn. | **VERIFIED** | Chạm nhanh 3 lần hoặc bấm Back 2 lần để thoát. Không phải tắt nguồn màn hình cấp kernel. | Nút **Mở Màn đen AMOLED** trong app |
| **Ép cứng 120Hz / Khôi phục** | Ghi `Settings.Secure.refresh_rate_mode=1`, `Settings.System` `min/peak_refresh_rate=120.0` (tránh lỗi `120.1` của SamHelper bị One UI 4.1 từ chối), tắt `low_power` và reset `DFS=60`. | **VERIFIED** | Cần cấp quyền Root (`su`). Nếu bạn cài thêm app/module quản lý game (như SamHelper hoặc module KSU khác) ghi đè lại `min/peak_refresh_rate`, các app đó có thể kéo máy về 60Hz trở lại. | Hai nút trong mục **3** của app |
| **Chẩn đoán & Tự ẩn dữ liệu nhạy cảm** | Thu thập nhanh thông tin máy, `/sys/class/dp_sec/dex`, settings, log `DeX120Hz`, `dumpsys display`, `dmesg`, đồng thời tự động ẩn Serial, IMEI, MAC, IP nội bộ và Token trước khi Copy Log. | **VERIFIED** (đã kiểm tra bằng unit test `SanitizerTest`) | Cần quyền Root để đọc `dmesg` và `dp_sec`. | Nút **Chạy chẩn đoán** → **Copy Log** |

Chi tiết cơ chế kỹ thuật từng hook và các phát hiện từ `/system/bin/surfaceflinger`: xem [docs/architecture.md](docs/architecture.md).

---

## Khả năng tương thích (Compatibility)

Mức phân loại sử dụng thống nhất trong dự án:
- **VERIFIED**: Đã chạy và xác minh trực tiếp trên máy thật.
- **PARTIAL**: Pipeline/mã nguồn đã mở khóa và chạy một phần, nhưng chưa thử hết kịch bản phần cứng đích.
- **EXPERIMENTAL**: Đang thử nghiệm, có thể cần bật/tắt tùy tình huống.
- **UNSUPPORTED**: Không hỗ trợ / đã biết không dùng được.
- **UNKNOWN**: Chưa có dữ liệu kiểm thử.

| Hạng mục | Thông tin | Trạng thái | Ghi chú thực tế |
|---|---|---|---|
| **Thiết bị** | Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`) | **VERIFIED** | Đã kiểm chứng trên duy nhất bản `SM-S901B` (`r0s`). Các biến thể S22 Snapdragon, S22+, S22 Ultra hoặc Galaxy khác: **UNKNOWN / UNSUPPORTED**. |
| **Hệ điều hành** | Android 12 (`API 31`) / One UI 4.1 | **VERIFIED** | Kiểm thử trên kernel `5.10.66`. Android 13 / 14 / 15 (One UI 5/6/7): **UNSUPPORTED**. |
| **Giải pháp Root & Hook** | KernelSU (Temproot) + ReZygisk + LSPosed | **VERIFIED** | Phải là bản Xposed/LSPosed hỗ trợ hook `android` (`system_server`) và `com.sec.android.sdhms`. Magisk + Zygisk + LSPosed: **UNKNOWN** (cùng chuẩn API nhưng chưa test thực tế). |
| **Màn hình ngoài 75Hz** | VSP `V2203H` (`1920x1080@75Hz`) qua USB-C hub | **VERIFIED** | OSD báo `75Hz`, `dmesg` báo `decon[2] bts.fps:75`, `TestUFO` vượt trần 60fps sau khi chặn `DFS=30`. Tuy nhiên với màn 75Hz, xem ghi chú bên dưới về việc bật sáng màn điện thoại. |
| **Màn hình ngoài 120Hz** | `1080p@120Hz` / `1440p@120Hz` | **PARTIAL** | Các nút thắt phần mềm 60Hz & 30Hz đều đã mở khóa tới 120Hz, nhưng **chưa có màn hình vật lý 120Hz để kiểm chứng cuối cùng**. |
| **Cáp / Hub USB-C → HDMI/DP** | Hub USB 3.0 2-lane (`048d:5212`) vs Cáp 4-lane | **VERIFIED** (2-lane ở 1080p@75Hz) | Exynos 2200 dùng DP HBR2 (5.4 Gbps/lane): hub 2-lane tối đa `1080p@120Hz`; muốn `1440p@120Hz` cần cáp trực tiếp 4-lane. |

> [!NOTE]
> **Lưu ý riêng khi cắm màn 75Hz (không áp dụng cho màn 120Hz):**
> Native `/system/bin/surfaceflinger` của Samsung có cơ chế tự ép màn chính (120Hz) xuống **60Hz** khi nối với màn ngoài chậm hơn (`75Hz`, do S22 không có mode 75Hz), và nếu property `vendor.hwc.exynos.vsync_mode=0`, app VSync sẽ lấy theo màn chậm hơn (`16.67ms` = 60Hz) khi màn điện thoại đang bật. Khi dùng màn **120Hz thật**, phép so sánh khoảng VSync không hạ màn chính xuống 60Hz. Chi tiết xem [docs/troubleshooting.md](docs/troubleshooting.md).

---

## Bạn cần chuẩn bị gì? (Dành cho người mới)

Nếu bạn chưa quen với khái niệm Android Root, hãy hiểu ngắn gọn 4 thành phần bạn bắt buộc phải có sẵn trên S22 Android 12 trước khi dùng app này:

1. **Quyền Root (KernelSU / Magisk):** Cho phép ứng dụng chạy lệnh hệ thống với quyền quản trị cao nhất (`su`), cần để ghi cài đặt hệ thống ẩn (`dex120_*`) và đọc thông số kernel `/sys/class/dp_sec/dex`.
2. **Zygisk (ví dụ ReZygisk):** Nền tảng nạp module vào tiến trình ứng dụng và hệ thống Android lúc khởi động (hoặc Soft Reboot).
3. **LSPosed:** Khung hook chạy trên Zygisk cho phép chặn/sửa hành vi hàm Java bên trong Android mà không cần sửa file APK hệ thống.
4. **Cáp hoặc Hub USB-C sang HDMI / DisplayPort** hỗ trợ xuất hình sang màn hình rời, cùng một màn hình có tần số quét **trên 60Hz** (75Hz, 100Hz, 120Hz, 144Hz...).

Chi tiết giải thích từng khái niệm và lưu ý Temproot xem tại [docs/installation.md](docs/installation.md).

---

## Cài đặt nhanh từ A đến Z

### Bước 1: Tải file APK chính thức
1. Mở trang [GitHub Releases](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/releases/latest).
2. Tải về **`DeX120Hz-S22.apk`** và file **`DeX120Hz-S22.apk.sha256`**.
3. **Không** tải file `Source code (zip)` để cài — đây chỉ là APK ứng dụng + module LSPosed (dự án này **không** dùng file ZIP flash trong KernelSU Manager).

### Bước 2: Kiểm tra tính toàn vẹn file (SHA-256)
Mở terminal tại thư mục vừa tải file và chạy đúng lệnh theo máy bạn:

- **Windows (PowerShell hoặc CMD):**
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

So sánh chuỗi 64 ký tự hiện ra với nội dung trong file `DeX120Hz-S22.apk.sha256`. Nếu **khác nhau dù 1 ký tự**, xóa ngay và tải lại — file có thể đã bị hỏng hoặc bị chỉnh sửa.

### Bước 3: Cài đặt & kích hoạt trong LSPosed
1. Cài đặt **`DeX120Hz-S22.apk`** như app Android bình thường.
2. Mở **LSPosed Manager** → tab **Modules** → chọn **DeX 120Hz S22** → gạt **Bật**.
3. Trong danh sách phạm vi (Scope), tick **đủ cả 3 mục** sau:
   - **`Hệ thống Android`** (`android` / *System Framework*) — để kích hoạt Hook 1–4.
   - **`com.sec.android.sdhms`** (nếu ẩn app hệ thống, bật *"Hiện ứng dụng hệ thống"* / *Show system apps* trong menu LSPosed) — để kích hoạt Hook 5 chặn `DFS=30`.
   - **`DeX 120Hz S22`** (`com.dex120.s22`) — để app kiểm tra được trạng thái hook.
4. Thực hiện **Soft Reboot** (Khởi động mềm — ví dụ trong KernelSU/LSPosed chọn *Soft Reboot* / *Khởi động lại hệ thống người dùng*, **không** chọn Reboot thường nếu máy bạn đang dùng **Temproot**).

### Bước 4: Mở app lần đầu & xác minh thực tế
1. Mở app **DeX 120Hz S22**.
2. Cấp quyền **Root** (`su`) khi KernelSU/Magisk hỏi.
3. Kiểm tra **hai thẻ trạng thái trên cùng** phải đều màu xanh:
   - `LSPosed: ĐÃ KÍCH HOẠT`
   - `Root: Sẵn sàng`
4. Cắm màn hình ngoài vào cổng USB-C để mở **Samsung DeX**.
5. Vào mục **4. Công tắc Hook**, đảm bảo 5 hook chính đang **Bật** và **Test Mode** đang **Tắt** (trừ khi bạn cố tình test trên màn 60Hz).
6. Bấm **Chạy chẩn đoán** → kiểm tra mục log `DeX120Hz` có dòng:
   - `DpSysfsHook`, `LowRefreshTokenHook`, `DisplayModeHook`, `VirtualDisplaySyncHook`
   - `Hook initialization complete: 4/4 modules installed.`
   - Khi cắm màn >60Hz: thấy `Upgraded external display ... baseModeId` và `Synced VirtualDisplay Desktop mMode -> ...@75.0Hz` (hoặc `@120.0Hz`).

Hướng dẫn chi tiết hơn cho người mới (kèm cách kiểm tra bằng OSD màn hình, `dumpsys`, `dmesg` và `testufo.com`) xem tại:
- [docs/installation.md](docs/installation.md)
- [docs/testing-and-verification.md](docs/testing-and-verification.md)

---

## Cập nhật, Rollback, Gỡ cài đặt & Khôi phục

Tóm tắt nhanh các thao tác an toàn — hướng dẫn từng bước đầy đủ nằm ở [docs/recovery.md](docs/recovery.md):

- **Cập nhật phiên bản mới:**
  1. Nếu đã chỉnh gì đặc biệt, vào app ghi lại trạng thái các công tắc (cấu hình lưu trong `Settings.Global` khóa `dex120_*`, nên cài đè bản mới thường vẫn giữ nguyên công tắc).
  2. Tải bản APK mới, kiểm tra SHA-256 và đọc [CHANGELOG.md](CHANGELOG.md).
  3. Cài đè APK → kiểm tra lại đủ **3 scope** trong LSPosed → **Soft Reboot**.
- **Rollback (quay về bản cũ):**
  1. Tải lại APK bản trước từ Releases và kiểm tra SHA-256.
  2. Tắt module trong LSPosed → gỡ app `DeX 120Hz S22` → cài bản APK cũ → bật lại scope → **Soft Reboot**.
- **Gỡ cài đặt hoàn toàn:**
  1. Trong app, bấm **Khôi phục Adaptive** (đưa tần số quét màn hình về mặc định).
  2. Tắt module **DeX 120Hz S22** trong LSPosed → **Soft Reboot**.
  3. Gỡ cài đặt app **DeX 120Hz S22**. (Các cờ `dex120_*` không gây hại, có thể xóa sạch bằng ADB nếu muốn — hướng dẫn có trong [docs/recovery.md](docs/recovery.md)).
- **Khôi phục khi máy treo/bootloop sau Soft Reboot:**
  1. Giữ nút **Nguồn + Giảm âm lượng** khoảng 7–10 giây để ép **Hard Reboot**.
  2. Nếu dùng **Temproot KernelSU**: sau Hard Reboot máy sẽ khởi động sạch hoàn toàn (không còn root/ReZygisk/LSPosed) và quay lại giao diện bình thường mà **không mất dữ liệu**.
  3. Với **Root vĩnh viễn**: dùng tổ hợp phím an toàn của KernelSU/LSPosed (Volume Down khi boot vào Safe Mode của module) hoặc tắt module qua `adb wait-for-device shell su`. Chi tiết xem [docs/recovery.md](docs/recovery.md).

---

## Tài liệu chi tiết (`docs/`)

| Tài liệu | Nội dung chính |
|---|---|
| [docs/installation.md](docs/installation.md) | Hướng dẫn cài đặt chi tiết từng bước cho người mới chưa rành Android Root |
| [docs/configuration.md](docs/configuration.md) | Ý nghĩa từng công tắc, bảng khóa `Settings.Global`, xung đột với SamHelper / module gaming khác |
| [docs/architecture.md](docs/architecture.md) | Cơ chế kỹ thuật 5 lớp hook, băng thông DisplayPort HBR2 và phân tích `SurfaceFlinger` |
| [docs/testing-and-verification.md](docs/testing-and-verification.md) | Cách đo và kiểm chứng tần số quét thật (OSD, `dmesg`, `dumpsys`, `scrcpy`, `testufo.com`) |
| [docs/troubleshooting.md](docs/troubleshooting.md) | FAQ & sửa các lỗi hay gặp (kẹt 60Hz, tụt <60fps do `DFS=30`, app không nhận LSPosed...) |
| [docs/recovery.md](docs/recovery.md) | Cập nhật, Rollback, Gỡ bỏ hoàn toàn và Khôi phục khi bootloop / crash `system_server` |
| [docs/development.md](docs/development.md) | Môi trường build từ mã nguồn (Windows & Linux/macOS), kiểm thử tự động và quy ước keystore |

---

## Tự build từ mã nguồn

<details>
<summary><b>Mở hướng dẫn build nhanh trên Windows và Linux/macOS</b> (chi tiết đầy đủ xem <a href="docs/development.md">docs/development.md</a>)</summary>

**Yêu cầu:** JDK 17 (`javac`, `jar`, `keytool`), Android SDK Platform `>= 31` (ưu tiên `android-34`), Android SDK `build-tools` (`aapt2`, `d8`, `zipalign`, `apksigner`), Python 3.

**Trên Windows (PowerShell):**
```powershell
git clone https://github.com/hangothanhh/DeX120Hz-S22-Exynos.git
cd DeX120Hz-S22-Exynos
powershell -ExecutionPolicy Bypass -File .\build.ps1
python .\scripts\verify_apk.py
```

**Trên Linux / macOS:**
```bash
git clone https://github.com/hangothanhh/DeX120Hz-S22-Exynos.git
cd DeX120Hz-S22-Exynos
chmod +x ./build.sh
./build.sh
python3 ./scripts/verify_apk.py
```

File thành phẩm và checksum được tạo tại:
- `out/DeX120Hz-S22.apk`
- `out/DeX120Hz-S22.apk.sha256`

Script ký bằng khóa debug nội bộ (`debug.keystore` tự sinh nếu chưa có và được `.gitignore` bỏ qua, **không bao giờ** commit lên Git).

</details>

---

## Hỗ trợ, Đóng góp & Giấy phép

- Gặp lỗi? Vui lòng bấm **Chạy chẩn đoán → Copy Log** trong app (log đã tự ẩn Serial, IMEI, MAC, IP nội bộ và Token — nhưng bạn vẫn nên đọc lướt lại trước khi dán) và mở Issue tại [GitHub Issues](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/issues/new/choose).
- Muốn đóng góp? Đọc [CONTRIBUTING.md](CONTRIBUTING.md) và [SECURITY.md](SECURITY.md) trước khi tạo Pull Request.
- Minh bạch AI & hồ sơ tuân thủ: [AI_CONTRIBUTIONS.md](AI_CONTRIBUTIONS.md) · [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) · [LICENSE_AUDIT_REPORT.md](LICENSE_AUDIT_REPORT.md) · [SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md) · [SBOM (CycloneDX)](SBOM.cdx.json).
- Giấy phép mã nguồn mở: [GNU GPL v3.0](LICENSE) · Ghi công & lưu ý bên thứ ba: [NOTICE](NOTICE).
- **Tuyên bố nhãn hiệu:** Samsung, Galaxy S22, Samsung DeX, One UI, Exynos là nhãn hiệu của Samsung Electronics Co., Ltd.; Android, Google, Antigravity là nhãn hiệu của Google LLC. Dự án này độc lập và không liên kết hay được bảo trợ bởi các đơn vị trên.
