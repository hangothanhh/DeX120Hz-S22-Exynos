# Update, Rollback, Uninstall & Recovery Guide

**Language:** **English** | [Tiếng Việt](vi/recovery.md)

> [!IMPORTANT]
> Read the **Tiered Incident Recovery** section below carefully before experimenting with `system_server` or LSPosed modifications.

---

## 1. Updating to a Newer Version

**Does updating preserve my settings?**
All hook toggles are stored in Android system settings (`Settings.Global` `dex120_*` keys), **not** inside app-private storage. Installing an updated APK signed with the same key usually preserves your settings, though due to Pre-release status you should **still back up your settings before updating**.

**Update procedure:**
1. Open `DeX 120Hz S22`, review or screenshot your toggles (or back up via ADB: `adb shell "settings list global | grep '^dex120_'"`).
2. Download the new `DeX120Hz-S22.apk` and `.sha256` from [Releases](https://github.com/hangothanhh/DeX120Hz-S22-Exynos/releases) and read [CHANGELOG.md](../CHANGELOG.md).
3. Verify **SHA-256** ([docs/installation.md](installation.md#step-2-verify-sha-256-required-to-avoid-corrupted-or-tampered-files)).
4. Install over the existing app (`adb install -r DeX120Hz-S22.apk` or tap to install).
5. Open **LSPosed Manager** → confirm any updated/required scopes (`android`, `com.sec.android.sdhms`, `DeX 120Hz S22`).
6. **Soft Reboot** once *(avoid normal Reboot if on Temproot)*.
7. Re-open the app, confirm both green status cards, and test DeX.

---

## 2. Rolling Back to an Older Version

Follow these steps if a new release triggers app crashes, broken hooks, `system_server` restarts, or thermal issues:

1. Open **LSPosed Manager** → disable **DeX 120Hz S22** → **Soft Reboot**.
2. Uninstall the current `DeX 120Hz S22` APK (Android blocks downgrading `versionCode` in place without uninstalling first).
3. Download the older release from GitHub Releases and verify its **SHA-256**.
4. Install the older APK → re-enable the module & required scopes in LSPosed → **Soft Reboot**.

---

## 3. Clean Uninstall

Follow this order so Android does not retain any locked refresh-rate values:

1. Open **DeX 120Hz S22** → tap **Khôi phục Adaptive** (Reset Adaptive) *(deletes `min_refresh_rate`, sets `peak_refresh_rate=120.0` and `refresh_rate_mode=1`)*.
2. Open **LSPosed Manager** → disable **DeX 120Hz S22** → **Soft Reboot**.
3. Uninstall **DeX 120Hz S22** normally (or `adb uninstall com.dex120.s22`).
4. **Confirm no hooks remain loaded:**
   ```bash
   adb shell su -c "logcat -d -s DeX120Hz:I | tail -n 10"
   ```
   No new `[DeX120Hz] Initializing...` entries after Soft Reboot confirms hooks are completely unloaded.

**What gets removed vs kept after uninstall?**
- **Removed:** The APK and private app data for `com.dex120.s22` under `/data/app/...` and `/data/data/com.dex120.s22/`. This module installs no Magisk/KernelSU `.zip` binaries under `/data/adb/modules/` and never modifies `/system` or `/vendor`.
- **Retained harmlessly:** `dex120_*` keys in `Settings.Global` and refresh rate settings in `Settings.System`. To wipe all `dex120_*` settings cleanly via ADB:
  ```bash
  for k in dex120_enabled dex120_block_dp_dex dex120_block_token dex120_unlock_modes dex120_sync_virtual dex120_force_virtual_120 dex120_target_hz; do
    adb shell su -c "settings delete global $k"
  done
  adb shell su -c "settings delete system min_refresh_rate; settings put system peak_refresh_rate 120.0; settings put secure refresh_rate_mode 1"
  ```

> [!WARNING]
> Do **NOT** manually delete files under `/data/adb/lspd/`, `/data/adb/modules/`, or `/system/` unless you are certain — deleting shared runtime files can break LSPosed or Android itself.

---

## 4. Tiered Incident Recovery

### Case A: AMOLED Black Screen Stuck or Companion App Crashes Only
- **Data impact:** **Zero data loss.**
- **Recovery:**
  - **Triple-tap** quickly or press **Back twice** to close AMOLED Black Screen; if needed, press the power button or unplug the external display cable.
  - If the control app crashes repeatedly: *Android Settings → Apps → DeX 120Hz S22 → Force stop / Clear data* or run `adb shell su -c "settings put global dex120_enabled 0"`.

### Case B: Hooks Not Loading (`LSPosed: CHƯA BẬT` Red Badge)
- **Data impact:** **Zero data loss.**
- **Recovery:** Confirm all 3 LSPosed scopes are checked, ReZygisk is active, and perform a **Soft Reboot**. Check `/data/adb/lspd/log/modules_*.log` for module load errors.

### Case C: Freeze or Bootloop Immediately After Soft Reboot
- **Data impact:** **Zero data loss** using the steps below.
- **Method 1 (KernelSU Temproot devices):**
  1. Hold **Power + Volume Down** for 7–10 seconds to force a **Hard Reboot**.
  2. Because Temproot does not survive a Hard Reboot, the phone boots cleanly into stock unrooted Android 12 with zero LSPosed hooks loaded — all user data stays intact.
- **Method 2 (Permanent Root / Unlocked Bootloader):**
  1. Trigger **KernelSU Safe Mode** *(hold Volume Down continuously while the boot logo appears, if supported by your KernelSU version)* to disable modules on boot, **or**:
  2. If ADB comes online during bootloop:
     ```bash
     adb wait-for-device shell su -c "settings put global dex120_enabled 0"
     ```
     Or disable the module via LSPosed CLI (if supported) or uninstall the APK over ADB:
     ```bash
     adb wait-for-device uninstall com.dex120.s22
     ```

### Case D: Unable to Boot Android (Rare; Typically Caused by Conflicting System Modifications)
> [!CAUTION]
> Low-level recovery options below **can erase user data** if a Factory Reset / Wipe Data becomes necessary!

1. If you can access **stock Android Recovery** or **ADB sideload / custom recovery** (if installed):
   - Try **Wipe cache** first if available — never jump straight to *Wipe data/factory reset*.
   - For permanent root boot-image corruption: flash the exact stock `boot.img` or stock **SM-S901B** Android 12 firmware via Download Mode (use `HOME_CSC` first to try preserving user data; regular `CSC` **erases all user data**).
2. Never flash firmware for a different S22 variant or older bootloader binary revision (Samsung rollback protection).
