# Step-by-Step Installation Guide (For Beginners)

**Language:** **English** | [Tiếng Việt](vi/installation.md)

> [!WARNING]
> **Strictly scoped to a rooted Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`) running Android 12 / One UI 4.1.**
> Back up all important data first and read the [Recovery Guide](recovery.md) before proceeding.
> If your device runs **KernelSU Temproot** (locked bootloader), **NEVER perform a normal Reboot** — always use **Soft Reboot** to load LSPosed hooks.

---

## 1. Purpose
Walks first-time users through installing the APK, checking all 3 required LSPosed scopes, granting Root access, and confirming whether `DeX 120Hz S22` is actively hooking on-device.

---

## 2. Quick Glossary of Terms

- **Root (`su`):** Superuser privileges on Android allowing the control app to write hidden system refresh settings and read `/sys/class/dp_sec/dex`.
- **KernelSU / Magisk:** Root management apps/frameworks on Android.
- **Temproot:** Temporary root on a device with a locked bootloader. Root and Zygisk/LSPosed modules **only persist for the current boot session** — a normal *Hard Reboot* returns cleanly to stock unrooted Android.
- **Soft Reboot (Userspace Reboot):** Restarts only Android userspace (`zygote` + `system_server`) without resetting the kernel, preserving Temproot while loading newly enabled LSPosed modules.
- **Zygisk / ReZygisk:** Injection layer used to load Xposed/LSPosed into Android processes.
- **LSPosed:** Xposed framework running on Zygisk so **DeX 120Hz S22** can intercept Samsung's 60Hz limits in memory without modifying `/system` files.
- **Scope:** List of target processes/apps an LSPosed module is permitted to hook. Missing a required scope disables the corresponding hooks silently.

---

## 3. Prerequisites

1. **Matching phone & firmware:** Galaxy S22 Exynos (`SM-S901B`), **Android 12 / One UI 4.1**. *(Check Settings → About phone → Software information.)*
2. **Working Root:** KernelSU Manager shows root active (or working Magisk).
3. **Working Zygisk + LSPosed:** e.g., `ReZygisk` + `LSPosed` active in KernelSU Manager, and **LSPosed Manager** opens normally.
4. **USB-C Cable/Hub & >60Hz Monitor:**
   - Standard 2-lane USB 3.0 → HDMI hubs handle up to **`1080p@120Hz`**.
   - **`1440p@120Hz`** requires a direct 4-lane USB-C → DisplayPort/HDMI cable.

---

## 4. Installation Steps

### Step 1: Download the Correct Files
1. Open [GitHub Releases](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/releases/latest).
2. Download **both** files below (do NOT download `Source code (zip)`):
   - `DeX120Hz-S22.apk`
   - `DeX120Hz-S22.apk.sha256`

### Step 2: Verify SHA-256 (Required to Avoid Corrupted or Tampered Files)
Open a terminal in the folder containing both downloaded files:

- **Windows (PowerShell / CMD):**
  ```powershell
  certutil -hashfile DeX120Hz-S22.apk SHA256
  ```
- **Linux or Android (Termux):**
  ```bash
  sha256sum DeX120Hz-S22.apk
  ```
- **macOS:**
  ```bash
  shasum -a 256 DeX120Hz-S22.apk
  ```

Open `DeX120Hz-S22.apk.sha256` in any text editor and compare the 64-character hex digest (case-insensitive). Both digests must match **100%**.

### Step 3: Install the APK on Your Phone
- **Option A (Directly on phone):** Tap `DeX120Hz-S22.apk` and tap **Install** *(allow installing unknown apps if prompted)*.
- **Option B (Via ADB from computer):**
  ```bash
  adb install -r DeX120Hz-S22.apk
  ```
  Expected terminal output: `Success`.

### Step 4: Enable Module & Check All 3 LSPosed Scopes
> [!CAUTION]
> Verify that your **ReZygisk** build supports **Soft Reboot** cleanly before triggering it below. On **Temproot** setups, accidentally tapping normal **Reboot** discards temporary root until you re-run your temproot setup.

1. Open **LSPosed Manager**.
2. Go to **Modules**.
3. Tap **`DeX 120Hz S22`** and turn on **Enable module**.
4. Open the 3-dot overflow menu → enable **System apps** so hidden Samsung services appear.
5. Check **all 3 required targets** (display labels vary slightly across LSPosed builds; match by package name in parentheses):
   - **`System Framework`** (`android`) — required for Hooks 1, 2, 3, and 4.
   - **`com.sec.android.sdhms`** (*Samsung Device Health Manager Service*) — required for Hook 5 (blocking `DFS=30` thermal drops).
   - **`DeX 120Hz S22`** (`com.dex120.s22`) — allows the app to report `LSPosed: ĐÃ KÍCH HOẠT` (Active).

### Step 5: Soft Reboot
1. In **KernelSU Manager** or **LSPosed**'s reboot menu, tap **Soft Reboot (Userspace reboot)** — restarting Android userspace without rebooting the kernel.
2. Wait 20–40 seconds until the lock screen reappears.

### Step 6: First Launch & Grant Root Access
1. Open **DeX 120Hz S22** from your launcher.
2. Grant **Root (`su`)** permission when **KernelSU Manager / Magisk** prompts (or manually enable Superuser access for `DeX 120Hz S22` / `com.dex120.s22` inside **KernelSU Manager → Superuser**).
3. Return to the app and confirm both top cards turn **green**:
   - **`LSPosed: ĐÃ KÍCH HOẠT`** (Active)
   - **`Root: Sẵn sàng`** (Ready)
4. Verify toggles in **4. Công tắc Hook**:
   - Turn ON the 5 main switches (`Bật toàn bộ`, `Hook 1`, `Hook 2`, `Hook 3`, `Hook 4`).
   - Keep the last **Test Mode** switch (`dex120_force_virtual_120`) **OFF**.

---

## 5. Expected Results & Confirmation

1. Connect a >60Hz external monitor (75Hz, 120Hz, etc.) via USB-C to start **Samsung DeX**.
2. Tap **Làm mới thông số Display / dp_sec** (Refresh Display / `dp_sec` status) in the app:
   - `EXTERNAL` should display your high-refresh mode (e.g., `1920x1080 @ 75.00 Hz` or `120.00 Hz`).
   - `VIRTUAL "Desktop"` must match the external display's active Hz 1:1 (not stuck at `60.00 Hz`).
   - `/sys/class/dp_sec/dex` should remain `0` (Kernel DP Mirror mode instead of `16/17`).
3. Tap **Chạy chẩn đoán (Run Diagnostics)** → inspect `3. Logcat DeX120Hz` for `Hook initialization complete: 4/4 modules installed`, `Upgraded external display ... baseModeId`, and `Synced VirtualDisplay Desktop mMode`.

For hardware OSD, `dmesg`, and TestUFO verification, continue to [docs/testing-and-verification.md](testing-and-verification.md).

---

## 6. Common Post-Install Issues

- **Red `LSPosed: CHƯA BẬT` badge:** Forgot to check `DeX 120Hz S22` (and `android`) in LSPosed scopes, or forgot to **Soft Reboot**.
- **Red `Root: Chưa cấp quyền` badge:** Grant Superuser permission to `com.dex120.s22` in KernelSU Manager → tap *Làm mới*.
- **Monitor still stays at 60Hz:** Verify Test Mode is OFF, check cable/hub bandwidth, or consult [docs/troubleshooting.md](troubleshooting.md).

If `system_server` crashes after Soft Reboot, follow [docs/recovery.md](recovery.md).
