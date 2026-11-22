# Troubleshooting & FAQ

**Language:** **English** | [Tiếng Việt](vi/troubleshooting.md)

Covers common issues encountered when installing and running `DeX 120Hz S22` on **Samsung Galaxy S22 Exynos (`SM-S901B`, Android 12 / One UI 4.1)**.

---

## 1. Red `LSPosed: CHƯA BẬT` Badge or Hooks Do Not Run

1. **Symptoms:** Companion app displays red badge `LSPosed: CHƯA BẬT`, or diagnostic log reads `(Chưa có log...)`.
2. **Possible causes:**
   - `DeX 120Hz S22` module switch is OFF inside **LSPosed Manager**.
   - Forgot to check `DeX 120Hz S22` or `System Framework` (`android`) scope.
   - Installed/updated APK without performing a **Soft Reboot**.
   - **ReZygisk** failed or unloaded across Soft Reboot (older ReZygisk builds under KernelSU temproot can drop injection after Soft Reboot).
3. **How to check:**
   - Open **LSPosed Manager**, verify LSPosed reports *Activated*, and open **Modules → DeX 120Hz S22** to ensure all 3 scopes (`android`, `com.sec.android.sdhms`, `com.dex120.s22`) are checked.
4. **Fix:**
   - Check all **3 scopes** → **Soft Reboot** once *(do NOT perform a normal Reboot if using Temproot)*.
   - If LSPosed itself shows unactivated after Soft Reboot, verify your ReZygisk version supports Android 12 Soft Reboots.
5. **When to file an issue:** Only if LSPosed reports *Activated*, all 3 scopes are checked, and Soft Reboot completed yet the badge remains red.

---

## 2. Red `Root: Chưa cấp quyền` Badge or Root Actions Fail

1. **Symptoms:** Root badge is red, `/sys/class/dp_sec/dex` reads `(cần root)`, switches revert when tapped.
2. **Possible causes:** Missing `su` grant for `com.dex120.s22` in **KernelSU Manager** (or Magisk), or device performed a Hard Reboot and lost **Temproot**.
3. **How to check:** Open **KernelSU Manager** to verify KernelSU is *Working* and **Superuser** permits `DeX 120Hz S22`.
4. **Fix:** Grant Superuser permission to `com.dex120.s22` → return to app and tap **Làm mới** (Refresh).

---

## 3. TestUFO or DeX Games Drop Below 60fps (24–45fps)

1. **Symptoms:** External monitor locks 75Hz/120Hz, yet `testufo.com` drops to ~24–45fps and stutters badly as the phone warms up.
2. **Possible causes:**
   - When S22 hits thermal SIOP level (`siop_level = 4`, ~44°C battery), `com.sec.android.sdhms` fires Binder transaction `1122` (`DFS = 30`) to `SurfaceFlinger`, forcing global VSync down to **30Hz (`33.33ms`)**.
   - Forgot to check **`com.sec.android.sdhms`** in LSPosed scopes, leaving **Hook 5 (`SdhmsDfsHook`)** inactive inside `sdhms`.
3. **How to check:** Run diagnostics → inspect section `2. Settings` for `SurfaceFlinger: DFS = 30`, or run via ADB:
   ```bash
   adb shell su -c "logcat -d -s SurfaceFlinger | grep 'DFS =' | tail -n 10"
   ```
4. **Fix:**
   - Check **`com.sec.android.sdhms`** in LSPosed (*enable Show system apps* to locate it) → **Soft Reboot**.
   - As an immediate workaround in the app, tap **Ép cứng 120Hz** — this executes `service call SurfaceFlinger 1122 i32 60` to clear `DFS=30`.
   - Use an external cooler/fan during sustained DeX gaming or heavy desktop workloads.

---

## 4. 75Hz Monitor Shows `OSD 75Hz`, Yet TestUFO & Screen Mirroring Stay at 60fps

1. **Symptoms:** A 75Hz panel (e.g., VSP `V2203H`) displays `75Hz` on OSD and `Desktop` reports `75.0 Hz`, yet while the phone screen is awake, `TestUFO` stays pinned at `60 fps`.
2. **Root cause:**
   - Disassembly of `/system/bin/surfaceflinger` on `SM-S901B` shows Samsung's native `SurfaceFlinger::performSetActiveConfig` downgrades Display 0 from 120Hz to 60Hz whenever a slower external display (such as `75Hz`) is connected (`desiredActiveMode.configId 0 is change to 2`).
   - Combined with `vendor.hwc.exynos.vsync_mode=0` in `/vendor/build.prop`, `SurfaceFlinger` picks the **slower screen's** VSync period (`16.67ms` = 60Hz) whenever Display 0 stays awake at 60Hz.
3. **How to check:**
   ```bash
   adb shell su -c "dumpsys SurfaceFlinger | grep -E 'VSYNC period|mIdealPeriod|powerMode=|activeMode='"
   ```
4. **Fix:**
   - Test with Display 0 off or AMOLED Black Screen state depending on composer VSync behavior; on a **native 120Hz monitor**, Display 0 and the external monitor share matching 120Hz timing (`83` vs `83`) so the native downgrade in `performSetActiveConfig` never triggers.
   - Keep **Test Mode (`dex120_force_virtual_120`) OFF** on 75Hz panels (enabling it causes frame skipping).

---

## 5. Phone Screen Gets Stuck at 60Hz After Gaming or Reboot; Force 120Hz Fails to Stick

1. **Symptoms:** `min_refresh_rate` / `peak_refresh_rate` reverts to `60`, gets stuck at `120.1`, or app warns `120.1 Hz (SamHelper)`.
2. **Possible causes:**
   - **SamHelper conflict (`120.1`):** SamHelper writes `120.1`, exceeding S22 panel's exact `120.00001 Hz` mode and causing `DisplayModeDirector` to reject 120Hz.
   - **Third-party KernelSU / game-booster modules** (such as `ksu_ultimate_gaming_x` misdetecting `DISPLAY_MAX_HZ='60'` or storing `orig_display.env` with `120.1`): overwrite `min=60` / `peak=60` into `settings_system.xml`, persisting across both Soft and Hard Reboots!
3. **How to check:**
   ```bash
   adb shell "echo min=$(settings get system min_refresh_rate) peak=$(settings get system peak_refresh_rate) sec_rrm=$(settings get secure refresh_rate_mode)"
   ```
4. **Fix:**
   - Disable or fix the conflicting module/app first so it stops overwriting `60` on game launch.
   - Open **DeX 120Hz S22** and tap **Ép cứng 120Hz** (writes `secure.refresh_rate_mode=1` and `min/peak=120.0`), or run over ADB:
     ```bash
     adb shell settings put secure refresh_rate_mode 1
     adb shell settings put system min_refresh_rate 120.0
     adb shell settings put system peak_refresh_rate 120.0
     ```

---

## 6. 120Hz/144Hz Monitor Plugged In, Only 60Hz Modes Listed

1. **Symptoms:** `EXTERNAL` `Supported:` list stops at `1920x1080@60` or `2560x1440@60`.
2. **Possible causes:**
   - USB-C hub/cable bandwidth capped at **HDMI 1.4** or **2-lane HBR2** (2-lane is too narrow for `1440p@120Hz` and maxes around `1080p@120Hz`).
   - Monitor OSD requires enabling HDMI 2.0/2.1 or disabling legacy compatibility/eco mode on that port.
   - Hook 1 (`DpSysfsHook`) failed to load, leaving kernel DisplayPort in `MODE_FILTER_DEX`.
3. **How to check:** Confirm `/sys/class/dp_sec/dex` reads `0` (not `16`/`17`), and inspect `dmesg | grep -E "exynos-dp|filter out|best mode"` to check EDID parsing.
4. **Fix:** Lower external resolution to `1080p` if using a 2-lane USB 3.0 hub, or switch to a direct 4-lane USB-C → DisplayPort/HDMI cable for `1440p@120Hz`.

---

## 7. Cannot Exit "AMOLED Black Screen"

- **Triple-tap** rapidly on the screen (within 700ms) or press **Back twice** (within 1.2 seconds). As an emergency fallback, press Power twice or run `adb shell input keyevent KEYCODE_BACK KEYCODE_BACK`.

---

## 8. DeX Takes ~20 Seconds to Close After Unplugging HDMI

- Early prototype hooks returned `"1"` unconditionally for `/sys/class/dp_sec/dex` reads even after unplugging HDMI. Since `v1.0.0-s22-a12`, `DpSysfsHook` only spoofs `"0" → "1"` while `DisplayModeHook.hasActiveExternalDisplay() == true`. Confirm you installed the current APK and performed a Soft Reboot afterward.

---

## 9. Play Integrity / Banking Apps / Game Anti-Cheat Detections

- **Important:** Play Integrity or banking app blocks are usually triggered by Root, Zygisk, LSPosed itself, or spoofing modules — not specifically `DeX 120Hz S22`.
- **`DeX 120Hz S22` contains zero root-hiding, hardware attestation spoofing, or game anti-cheat bypass features.** Keep LSPosed scopes strictly limited to `android`, `com.sec.android.sdhms`, and `com.dex120.s22` — **never** enable banking apps or games inside this module's scope.

---

## 10. Collecting a Sanitized Diagnostic Report Safely

1. Connect the external display and reproduce the issue.
2. Open **DeX 120Hz S22** → scroll to **5. Chẩn đoán & Thu thập Log**.
3. Tap **Chạy chẩn đoán** → wait a few seconds until sections `=== 0` through `=== 5` appear.
4. Tap **Copy Log**. `RootShell.sanitizeDiagnostics()` automatically redacts:
   - Serials (`ro.serialno`, Samsung serials)
   - IMEI, MAC addresses, private/loopback IPv4 (`10.x`, `172.16-31.x`, `192.168.x`, `127.0.0.1`)
   - Emails, GitHub tokens, Passwords / Secrets
5. **Review the entire copied log manually before pasting** into the [GitHub Bug Report template](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/issues/new?template=bug_report.md).
