# Configuration & Options Reference

**Language:** **English** | [Tiếng Việt](vi/configuration.md)

## 1. Purpose
Explains all UI switches, buttons, and `Settings.Global` keys read/written by `DeX 120Hz S22` on Galaxy S22 Android 12, along with known conflicts involving SamHelper or third-party gaming modules.

---

## 2. Hook Control `Settings.Global` Keys

Each toggle in Card **4. Công tắc Hook (Hook Switches)** writes an integer (`0` or `1`, except `dex120_target_hz`) directly into Android's `Settings.Global`. `Config.java` inside `system_server` (and `com.sec.android.sdhms`) refreshes its cached state every **2000 ms (2 seconds)** — **no Soft Reboot is needed** when flipping runtime switches.

| `Settings.Global` Key | Default | Meaning & Recommended Setting |
|---|---|---|
| `dex120_enabled` | `1` (ON) | Master kill switch across all 5 hook layers. Set to `0` to pause all hooks temporarily without disabling LSPosed. |
| `dex120_block_dp_dex` | `1` (ON) | Controls **Hook 1 (`DpSysfsHook`)**: blocks writing `16/17` to `/sys/class/dp_sec/dex` so the kernel preserves >60Hz modes. |
| `dex120_block_token` | `1` (ON) | Controls **Hook 2 (`LowRefreshTokenHook`)** and **Hook 5 (`SdhmsDfsHook`)**: blocks 60Hz cap tokens (`HardwareManager`/`SiopLimiterHrr`) and blocks `SDHMS DFS=30`. |
| `dex120_unlock_modes` | `1` (ON) | Controls **Hook 3 (`DisplayModeHook`)**: raises external `baseModeId` & expands `RefreshRateRange` above 60Hz. |
| `dex120_sync_virtual` | `1` (ON) | Controls **Hook 4 (`VirtualDisplaySyncHook`)**: syncs `VirtualDisplay "Desktop"` 1:1 with the real external display Hz. |
| `dex120_force_virtual_120` | `0` (OFF) | **Test Mode (EXPERIMENTAL):** Forces `VirtualDisplay "Desktop"` to report `120Hz` even on a `60Hz` or `75Hz` monitor. **Keep OFF (`0`) for normal use**, otherwise mismatched panel pacing drops frames. |
| `dex120_target_hz` | `120` | Target ceiling refresh rate (`60` to `240`) used across `DisplayModeHook`, `VirtualDisplaySyncHook`, and `BlackScreenActivity`. Out-of-range values fall back to `120`. |

Inspect or adjust keys directly via rooted ADB:
```bash
adb shell su -c "settings list global | grep '^dex120_'"
adb shell su -c "settings put global dex120_force_virtual_120 0"
```

---

## 3. Force 120Hz vs Reset Adaptive (Card 3 in App)

- **`Ép cứng 120Hz` (Force 120Hz) button:** Runs `RootShell.applySamHelper120Hz()`:
  ```sh
  settings put secure refresh_rate_mode 1
  settings put system refresh_rate_mode 1
  settings put system min_refresh_rate 120.0
  settings put system peak_refresh_rate 120.0
  settings put global low_power 0
  service call SurfaceFlinger 1122 i32 60
  ```
  - *Important One UI 4.1 note:* On S22 Android 12, `DisplayModeDirector` reads **`Settings.Secure.refresh_rate_mode`** (`0` = Standard 60Hz, `1` = Adaptive 10–120Hz), and Samsung's 120Hz display mode reports exact fps **`120.00001 Hz`**. Therefore the app writes **`120.0`** — strictly avoid `120.1` used by older third-party tools.
- **`Khôi phục Adaptive` (Reset Adaptive) button:** Runs `RootShell.resetRefreshRate()` — deletes `min_refresh_rate` and sets `peak_refresh_rate = 120.0`, `refresh_rate_mode = 1` so the phone resumes stock adaptive refresh behavior outside DeX.

---

## 4. Known Conflicts With Other Apps & Modules (Important!)

> [!WARNING]
> During real-device testing on S22, these 2 conflicts **dropped refresh rates back to 60Hz** even while `DeX 120Hz S22` hooks were running properly:

1. **SamHelper setting `min/peak_refresh_rate = 120.1`:**
   - Because S22's 120Hz panel reports `fps = 120.00001`, `120.1` exceeds `120.00001` and causes `DisplayModeDirector` to reject the 120Hz vote, falling back to `baseModeId=3 (60Hz)` / `[10 120]`.
   - `MainActivity` warns automatically if `min/peak > 120.05` is detected — tap **Ép cứng 120Hz (Force 120Hz)** in the app to restore `120.0`.
2. **Performance / gaming modules forcing `min/peak_refresh_rate = 60`:**
   - Some KernelSU gaming modules (observed live with `ksu_ultimate_gaming_x` when misdetecting `DISPLAY_MAX_HZ='60'`) write `settings put system min_refresh_rate 60` and `peak_refresh_rate 60` on game launch; rebooting before cleanup leaves `60/60` persisted across reboots inside `settings_system.xml`.
   - If the phone remains stuck at 60Hz unexpectedly, inspect current values:
     ```bash
     adb shell "echo min=$(settings get system min_refresh_rate) peak=$(settings get system peak_refresh_rate) sec_rrm=$(settings get secure refresh_rate_mode)"
     ```
     and audit any other installed Root modules.

---

## 5. Resetting Everything Back to Stock Defaults
See step-by-step cleanup instructions in [docs/recovery.md](recovery.md).
