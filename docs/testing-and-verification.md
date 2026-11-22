# Refresh Rate Testing & Verification Procedure

**Language:** **English** | [Tiếng Việt](vi/testing-and-verification.md)

## 1. Purpose
Never rely on subjective visual impressions or a single synthetic metric alone. This checklist walks from **zero-PC quick checks** to **low-level ADB kernel & SurfaceFlinger probes** to confirm whether Samsung DeX actually drives hardware and software above 60Hz.

---

## 2. Requirements
- `DeX 120Hz S22` installed and all 3 LSPosed scopes enabled ([docs/installation.md](installation.md)).
- USB-C cable/hub connected to an external monitor supporting >60Hz (75Hz, 120Hz, 144Hz, etc.).
- *(Optional for advanced tiers)* Computer with `adb` + `scrcpy` and USB Debugging enabled.

---

## 3. Multi-Layer Verification Tiers (Quick to Deep)

### Tier 1: Direct External Monitor & Browser Checks (No ADB Needed)
1. **External Monitor Hardware OSD (On-Screen Display):**
   - Press your monitor's physical Info / Signal / Menu button.
   - **Expected:** Must report **`1920x1080 75Hz`** (or `120Hz`, per monitor), rather than `60Hz`. This is hardware proof that the panel receives a >60Hz signal.
2. **TestUFO inside Samsung DeX:**
   - Open Chrome / Samsung Internet inside DeX and navigate to `https://www.testufo.com`.
   - To avoid mixed-panel VSync throttling (especially on 75Hz monitors), use the app's **AMOLED Black Screen** or keep the phone screen in the state you wish to benchmark.
   - **Expected:** Frame Rate exceeds 60fps (e.g., ~`75 fps` on a 75Hz monitor). If it drops to 24–45fps as the phone warms up, verify **`com.sec.android.sdhms`** is checked in LSPosed to block `DFS=30`.

### Tier 2: In-App Status Check (`DeX 120Hz S22`)
Open the app → **2. Trạng thái màn hình & Kernel DP** → tap **Làm mới** → verify:
- Both `LSPosed: ĐÃ KÍCH HOẠT` and `Root: Sẵn sàng` badges are green.
- `EXTERNAL` shows `Active: ... @ 75.00 Hz` (or `120.00 Hz`).
- `VIRTUAL "Desktop"` shows `Active` matching `EXTERNAL` 1:1.
- `/sys/class/dp_sec/dex = 0`.
- `refresh_rate_mode (Secure) = 1 (Tương thích)` and `min/peak_refresh_rate = 120.0 / 120.0 Hz`.

### Tier 3: Low-Level Verification via Rooted ADB

Connect ADB over USB or Wi-Fi and run these checks:

1. **Verify Kernel DisplayPort / DPU Output FPS (`dmesg`):**
   ```bash
   adb shell su -c "dmesg | grep -E 'exynos-drmdecon.*bts\.fps|exynos-dp|dp_sec|best mode' | tail -n 20"
   ```
   - **Expected:** `exynos-drmdecon[2]: decon_mode_update_bts_fps bts.fps:75` (or `120`) for the external display!

2. **Verify `DisplayModeDirector` & `VirtualDisplay "Desktop"` (`dumpsys display`):**
   ```bash
   adb shell su -c "dumpsys display | grep -E 'mDisplayId=|mActiveModeId=|mBaseModeId=|primaryRefreshRateRange|DisplayDeviceInfo|\"Desktop\"|type EXTERNAL' | head -n 40"
   ```
   - **Expected:** Both `EXTERNAL` and `"Desktop"` report modes >60Hz; the external display's `mDesiredDisplayModeSpecs` shows `primaryRefreshRateRange=[... 120]` (not capped at `[60 60]`).

3. **Verify `SurfaceFlinger` VSync Period & `DFS` State:**
   ```bash
   adb shell su -c "dumpsys SurfaceFlinger | grep -E 'DisplayDevice\{|activeMode=|VSYNC period|app duration|mIdealPeriod|workDuration' | head -n 25"
   adb shell su -c "logcat -d -s SurfaceFlinger | grep -E 'DFS =|Dynamic FPS' | tail -n 10"
   ```
   - **Expected:** No `DFS = 30` or `VSYNC period: 33333333 ns` (30Hz). Expect `mIdealPeriod=13.33` / `workDuration: 13.33ms` at 75Hz, or `8.33ms` at 120Hz.

4. **Inspect Hook Logs (`logcat` / LSPosed module logs):**
   ```bash
   adb shell su -c "logcat -d -s DeX120Hz:I | tail -n 40"
   ```
   - Or inspect the latest LSPosed log file (if `logcat` rotated):
     ```bash
     adb shell su -c "grep 'DeX120Hz' \$(ls -t /data/adb/lspd/log/modules_*.log | head -n 1) | tail -n 30"
     ```

### Tier 4: `scrcpy` Check (Select the Exact `--display-id`)
To measure `"Desktop"` VirtualDisplay frame output on PC with `scrcpy`:
1. List active display IDs:
   ```bash
   scrcpy --list-displays
   ```
2. Mirror the DeX/`Desktop` ID specifically (not phone ID `0`):
   ```bash
   scrcpy --display-id=<DeX_ID> --max-fps=120 --print-fps --no-audio
   ```
- **Note:** `scrcpy --print-fps` only emits non-zero frames while pixels are continuously updating (e.g., scrolling or running TestUFO) and depends on USB/Wi-Fi encoder throughput, so **Monitor OSD + `dmesg` + `dumpsys SurfaceFlinger`** remain the authoritative ground truth.

---

## 4. Handling Common Test Anomalies
- `TestUFO` stays locked at **60 fps** despite Monitor OSD showing **75Hz**: see *75Hz Monitor Stuck at 60fps* in [docs/troubleshooting.md](troubleshooting.md).
- `TestUFO` drops to **24–45 fps**: see *TestUFO Dropping Below 60fps / SDHMS DFS=30* in [docs/troubleshooting.md](troubleshooting.md).
- Need to revert test settings afterward: see [docs/recovery.md](recovery.md).
