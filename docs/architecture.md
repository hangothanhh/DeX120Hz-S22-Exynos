# Technical Architecture & How It Works (S22 Exynos · Android 12 / One UI 4.1)

**Language:** **English** | [Tiếng Việt](vi/architecture.md)

> [!NOTE]
> This document explains each 60Hz / 30Hz bottleneck observed directly on **Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`, Android 12 / One UI 4.1)** and how the 5 hook layers in `DeX 120Hz S22` address them.

---

## 1. Purpose
Help contributors and power users understand **why** stock Samsung DeX is capped at 60Hz (and throttled down to 30Hz under heat), where each Java file in `app/src/main/java/com/dex120/s22/xposed/` hooks into Android / Samsung frameworks, and which hardware/native limits cannot be bypassed via Java hooks alone.

---

## 2. Samsung DeX Frame Pipeline

On Samsung DeX (Android 12), the desktop UI does not render straight to the HDMI output; it flows through 4 stacked layers:

```text
[App / Browser / Game inside DeX]
        │  (VSync from SurfaceFlinger / DisplayModeDirector + SDHMS)
        ▼
[VirtualDisplay "Desktop"]             ◄── Hook 4: VirtualDisplaySyncHook
        │  (mMode / renderFrameRate synced to real external Hz)
        ▼
[DisplayModeDirector & DMS Tokens]     ◄── Hook 2 & 3: LowRefreshTokenHook + DisplayModeHook
        │  (baseModeId, RefreshRateRange, blocks HardwareManager & SiopLimiterHrr)
        ▼
[Kernel DisplayPort (dp_sec / decon2)] ◄── Hook 1: DpSysfsHook
        │  (sysfs /sys/class/dp_sec/dex stays in Mirror mode, skipping 60Hz filtering)
        ▼
[USB-C → HDMI/DP Cable or Hub] → [External Monitor (75Hz / 120Hz)]

Parallel thermal throttling path:
[com.sec.android.sdhms (SIOP)] ──(Binder tx 1122: DFS=30)──▶ [SurfaceFlinger]
        ▲
        └── Hook 5: SdhmsDfsHook (blocks in SDHMS + proactively resets DFS=60 from system_server)
```

---

## 3. Detailed Breakdown of the 5 Hook Layers

### Hook 1: `DpSysfsHook` (Kernel DisplayPort Mirror Bypass)
- **Targets:** `com.android.server.desktopmode.Utils` (`writeFile`, `readFile`) and `com.android.server.desktopmode.DisplayPortStateManager` inside `system_server`.
- **Root cause:** When DeX starts, Samsung's `DesktopModeService` writes the `DEX_ON` flag (`16`/`0x10` or `17`/`0x11`) to `/sys/class/dp_sec/dex`. The Exynos DisplayPort kernel driver switches from `MODE_FILTER_MIRROR` to `MODE_FILTER_DEX`, filtering out any resolution/refresh mode >60Hz or outside Samsung's DeX whitelist.
- **How it works:**
  - `handleWriteFileBefore`: if a numeric value with bit `0xF0` set (`16`, `17`, etc.) is written to `dp_sec/dex`, masks off the high bits (`oldVal & 0x0F` → `0` or `1`) so the kernel stays in `MODE_FILTER_MIRROR`, setting `sSimulatedDexOn = true`. `SkipAdapterCheck` and `HMD` writes are left untouched.
  - `handleReadFileAfter`: several system checks read `/sys/class/dp_sec/dex` back. The hook conditionally spoofs `"0" → "1"` **only when both** `sSimulatedDexOn == true` **and** `DisplayModeHook.hasActiveExternalDisplay() == true` hold. Once the cable is unplugged (`extActive == false`), it stops spoofing so DeX does not hang ~20 seconds while disconnecting.

### Hook 2: `LowRefreshTokenHook` (Global `<=60Hz` Token Blocking)
- **Targets:** `DisplayManagerService`, `$BinderService`, `$LocalService`: `acquireLowRefreshRateToken` (returns `ILowRefreshRateToken`) and `acquireRefreshRateMaxLimitToken` (returns `IRefreshRateMaxLimitToken`).
- **Root cause:** When DeX launches, `[DMS]HardwareManager` acquires an `ILowRefreshRateToken` locking the system to 60Hz; when the device warms up during DeX, `SiopLimiterHrr` also acquires a 60Hz cap token.
- **How it works:**
  - Whenever the caller belongs to DeX/Desktop (`isDexCaller`) **or** an external display is active (`DisplayModeHook.hasActiveExternalDisplay()`) and the requested cap is `<= 60Hz`, the hook blocks the real call and returns a dedicated dynamic `Proxy` **per interface**, cached in `ConcurrentHashMap<Class<?>, Object> sDummyTokens` (preventing `ClassCastException` between `ILowRefreshRateToken` and `IRefreshRateMaxLimitToken`).
  - Also triggers `SdhmsDfsHook.scheduleDfsReset("token:" + tag)` because `SiopLimiterHrr` typically triggers alongside `DFS=30`.

### Hook 3: `DisplayModeHook` (`DisplayModeDirector` & Display 0 Protection)
- **Targets:** `DisplayModeDirector.getDesiredDisplayModeSpecs`, `updateVoteLocked`, `DisplayObserver`, `LogicalDisplay.getDisplayInfoLocked`, `RealInjector.getDisplayInfo`.
- **Root cause:**
  1. `DisplayModeDirector` defaults to selecting a 60Hz `baseModeId` and votes `RefreshRateRange.max = 60.0` for external displays (`type == TYPE_EXTERNAL`).
  2. When using a mouse/keyboard on DeX without touching the phone screen, Display 0 (`samsung lcd`) LTPO can drop to 24Hz/60Hz unless locked.
- **How it works:**
  - Tracks exact active external display state (synchronized with `mSupportedModesByDisplay` and `DisplayObserver.onDisplayRemoved`).
  - For external displays (`isExternalDisplay(displayId)`): `findHighestModeSameRes` locates the highest-fps `Display.Mode` matching the current resolution up to `Config.getTargetHz()` (default 120Hz), upgrades `baseModeId`, and widens `primaryRefreshRateRange` / `appRequestRefreshRateRange` up to 120Hz.
  - For Display 0 while `sExternalDisplayActive`: `protectInternalDisplaySpecs` attempts to keep Display 0 `baseModeId` at 120Hz and locks `primaryRefreshRateRange` to `[120, 120]`. *(See Section 5 for native SurfaceFlinger limits when connected to 75Hz panels.)*

### Hook 4: `VirtualDisplaySyncHook` (`Desktop` VirtualDisplay)
- **Targets:** `DisplayManagerService.configureDisplayLocked`, `VirtualDisplayAdapter$VirtualDisplayDevice` (`getDisplayDeviceInfoLocked`, `resizeLocked`, constructor).
- **Root cause:** Samsung DeX's `"Desktop"` `VirtualDisplay` initializes at `60.0 Hz`, so even if HDMI outputs 75Hz or 120Hz, DeX still renders frames at 60Hz.
- **How it works:**
  - Reads the external physical display's actual refresh rate in `configureDisplayLocked` (`sLastExternalHz`).
  - When `isDesktopVirtualDevice` matches `"Desktop"` (and `Config.isSyncVirtual()` is enabled): creates a new `Display.Mode` at `targetHz = Math.min(sLastExternalHz, Config.getTargetHz())` (or `Config.getTargetHz()` if Test Mode `dex120_force_virtual_120=1` is enabled), updates `mMode`, `mRequestedRefreshRate`, `supportedModes`, `modeId`, `defaultModeId`, and notifies the adapter.

### Hook 5: `SdhmsDfsHook` (Blocking `DFS = 30` Inside `com.sec.android.sdhms`)
- **Finding from real firmware analysis:** On One UI 4.1 (`SamsungDeviceHealthManagerService.apk`), when phone thermals climb during DeX (e.g., `siop_level = 4`, battery ~44°C), `SDHMS` sends Binder transaction **`1122` (`0x462`)** to `SurfaceFlinger` with interface descriptor `"android.ui.ISurfaceComposer"` and argument `int 30` (`DFS = 30`), enabling *Dynamic FPS VSyncThread* and throttling global `VSYNC period` from `8.33ms` / `13.33ms` down to **`33.33ms` (30Hz)** — causing TestUFO to collapse to 24–45fps!
- **Two-layer mitigation:**
  1. Inside `com.sec.android.sdhms`: hooks `BinderProxy.transact`; if `code == 1122` and the `Parcel` carries interface `"android.ui.ISurfaceComposer"` with `0 < dfs < 60`, blocks the transaction from reaching `SurfaceFlinger` (always restoring `dataPosition` inside `finally` so unrelated binders reusing transaction code `1122` are unaffected).
  2. Inside `system_server`: `scheduleDfsReset` sends transaction `1122` with `dfs = 60` (disabling Dynamic FPS VSyncThread in `SurfaceFlinger`) at `+150ms`, `+1000ms`, and `+3000ms` delays when an external display activates or `SiopLimiterHrr` triggers.

---

## 4. Keeping VSync Alive When Turning Off the Phone Screen (`BlackScreenActivity`)

On Android 12 (`API 31`), pressing the power button to turn Display 0 off (`powerMode=Off`) can cause `SurfaceFlinger` to fall back to fallback software `SyntheticVSync` at 60Hz depending on composer state.

`BlackScreenActivity` avoids this by keeping Display 0 logically `ON` (`FLAG_KEEP_SCREEN_ON`, `preferredRefreshRate=120f`, `screenBrightness=0.001f`) while painting solid `#000000` black pixels (on AMOLED panels, pure `#000000` pixels draw virtually zero light/power and minimize burn-in while keeping high-refresh VSync active for DeX).

---

## 5. Hardware & Native Limits That Java Hooks Cannot Bypass

Confirmed via direct disassembly of `/system/bin/surfaceflinger` on `SM-S901B` (Android 12) and Exynos 2200 DisplayPort specifications:

1. **Native 75Hz external monitor downgrade (observed on VSP `V2203H`):**
   - Inside `SurfaceFlinger::performSetActiveConfig()`, Samsung's native code checks whether any connected external display has a larger `vsync period` than the primary panel (e.g., `133` for 75Hz > `83` for 120Hz), and forces Display 0 down to a mode with vsync >= 133 → **`configId=2` (60Hz)** (`desiredActiveMode.configId 0 is change to 2, because other display(vsync period = 133) is connected`).
   - Additionally, `/vendor/build.prop` sets `vendor.hwc.exynos.vsync_mode=0`, so while both screens are on (60Hz phone + 75Hz external), app VSync follows the slower display (`16.67ms` = 60Hz).
   - *Note:* With a **native 120Hz external monitor**, the external vsync period (`83`) does not exceed Display 0's `83`, so `performSetActiveConfig` does **not** force Display 0 down to 60Hz.
2. **DisplayPort HBR2 link bandwidth (5.4 Gbps/lane):**
   - 2-lane cables/hubs (sharing USB 3.0 data lanes): top out around `1080p@120Hz` (~7.1 Gbps raw before 8b/10b overhead, close to the 8.64 Gbps usable 2-lane HBR2 limit). Insufficient for `1440p@120Hz`.
   - Direct USB-C → DisplayPort/HDMI cables (4-lane HBR2, up to 17.28 Gbps): sufficient for `1440p@120Hz` (~11–13 Gbps depending on CVT/RB timings).
3. **`SDHMS` CPU/GPU throttling:**
   - This module only blocks **display refresh-rate throttling (`DFS 30Hz` / `60Hz` tokens)**; it does **not** override SoC thermal limits or prevent CPU/GPU clock scaling.
