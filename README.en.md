# DeX 120Hz S22

**Language:** [Tiếng Việt](README.md) | **English**

An **LSPosed** module and companion control app that unlocks high-refresh-rate (>60Hz, up to 120Hz depending on monitor and cable) output for **Samsung DeX** — specifically built for a rooted **Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`) running Android 12 / One UI 4.1**.

- **Current version:** `v1.0.0-s22-a12` (**Pre-release / Experimental**)
- **Package name:** `com.dex120.s22` (`minSdk 31`, `targetSdk 31` — Android 12)
- **Tech stack:** Java 11, Xposed/LSPosed Hooks (`system_server` + `com.sec.android.sdhms`), standalone Android SDK toolchain (`aapt2` + `d8` + `apksigner`, zero Gradle dependency)
- **License:** [MIT License](LICENSE) · [Credits & AI Notice](NOTICE)
- **Project links:** [Releases](https://github.com/hangothanhh/DeX120Hz-S22/releases) · [Issues](https://github.com/hangothanhh/DeX120Hz-S22/issues) · [Contributing](CONTRIBUTING.md)

---

## Read Before Installing — WARNINGS

> [!CAUTION]
> **AI Contribution Transparency & AI Slop Warning:**
> Most of the Java code, build/verification scripts, and documentation in this repository were **written or assisted by AI** (`Co-authored-by: Antigravity <antigravity@google.com>`), and have only been partially verified by a human on a **single Samsung Galaxy S22 Exynos (`SM-S901B`) device running Android 12 / One UI 4.1 with a 75Hz external monitor (VSP `V2203H`)**.
> The maintainer cannot manually audit every execution path across all firmware revisions or monitor models. If you plan to contribute code, read the [AI contribution & anti-AI-slop rules](CONTRIBUTING.md#quy-định-về-ai-và-chống-ai-slop) before opening a Pull Request.

> [!WARNING]
> **Risks of Root & LSPosed System Hooks:**
> 1. This module hooks directly into privileged Android system processes (`system_server` and `com.sec.android.sdhms`) using **Root**. Incompatible Android/One UI versions or conflicting configurations can cause **UI crashes, `system_server` restarts, or bootloops** — and improper recovery attempts can cause data loss.
> 2. **Target scope is strictly Galaxy S22 Exynos (`SM-S901B`) on Android 12 / One UI 4.1.** Do not install on Android 13/14/15 (One UI 5/6/7), Snapdragon S22 variants, or other Galaxy phones without independently auditing and testing every hook.
> 3. **Important note for KernelSU Temproot users (locked bootloader):** After enabling the module or changing LSPosed scopes, **only use Soft Reboot** — performing a normal/hard Reboot will discard your temporary root.
> 4. **120Hz is NOT guaranteed on every monitor, cable, or game:** Physical 120Hz external monitor testing is currently **`PARTIAL` / untested on real 120Hz hardware** (real hardware testing so far verified breaking the 60Hz cap up to **75Hz** on a VSP `V2203H`). USB-C/HDMI adapter lane count, game engine frame caps, and phone screen power state all affect real output FPS.
> 5. **Does NOT disable silicon/CPU/GPU thermal protection:** Hooks only block display refresh-rate throttling (`SiopLimiterHrr` 60Hz tokens and `SDHMS DFS = 30`). `SDHMS` still throttles CPU/GPU clocks when hot — use external cooling for sustained heavy loads.
> 6. **Does NOT guarantee passing Play Integrity** or bypassing banking/game anti-cheat or root checks.
> 7. **You assume full responsibility** when installing and running this project.

**6 mandatory steps before installing:**
1. Back up all important data to a computer or cloud storage.
2. Confirm your phone is **Galaxy S22 Exynos (`SM-S901B`)** on **Android 12 / One UI 4.1**.
3. Ensure **Root** (`KernelSU` or equivalent), **Zygisk** (`ReZygisk`), and **LSPosed** are already working.
4. Download the exact `DeX120Hz-S22.apk` from [GitHub Releases](https://github.com/hangothanhh/DeX120Hz-S22/releases) (do not download the Source code ZIP as an installer).
5. Verify the **SHA-256** checksum before installing.
6. Read the [Recovery Guide](docs/recovery.md) in advance so you know how to disable or remove the module if anything goes wrong.

---

## What This Project Does

On stock Android 12 / One UI 4.1, Samsung enforces a **60Hz cap** on **Samsung DeX** across multiple independent layers: kernel DisplayPort mode filtering, Android's `DisplayModeDirector`, and thermal telemetry (`SDHMS`). **DeX 120Hz S22** coordinates 5 runtime LSPosed hooks together with a lightweight control app:

| Component | Purpose & How It Works | Status | Requirements & Limits | Toggle |
|---|---|---|---|---|
| **Hook 1: `DpSysfsHook`** | Intercepts writes of `DEX_ON` flags (`0x10`/`0x11`) to `/sys/class/dp_sec/dex` so the Exynos DP kernel driver stays in Mirror mode (avoiding 60Hz whitelist filtering), while conditionally spoofing reads as `"1"` only when an external display is active (fixing ~20s DeX disconnect delays). | **VERIFIED** (tested plug/unplug on 75Hz monitor) | Requires `android` (`System Framework`) scope + Soft Reboot. | **Hook 1** switch (`dex120_block_dp_dex`) |
| **Hook 2: `LowRefreshTokenHook`** | Blocks `<=60Hz` lock tokens (`acquireLowRefreshRateToken` and `acquireRefreshRateMaxLimitToken`) requested by `[DMS]HardwareManager` and `SiopLimiterHrr` whenever an external display is active, caching separate proxy tokens per interface. | **VERIFIED** (confirmed live logs blocking DMS + SiopLimiterHrr) | Active globally while an external display is connected. | **Hook 2** switch (`dex120_block_token`) |
| **Hook 3: `DisplayModeHook`** | Patches `DisplayModeDirector.getDesiredDisplayModeSpecs`: upgrades external display `baseModeId` to the highest matching resolution refresh rate (up to `dex120_target_hz`, default 120Hz), expands `RefreshRateRange`, and keeps Display 0 locked at 120Hz while external display is active. | **VERIFIED** at 75Hz; **PARTIAL** at 120Hz | Requires monitor EDID to expose >60Hz modes over your cable/hub. Cannot override native `/system/bin/surfaceflinger` checks on mismatched 75Hz panels (see Compatibility note). | **Hook 3** switch (`dex120_unlock_modes`) |
| **Hook 4: `VirtualDisplaySyncHook`** | Synchronizes the DeX `"Desktop"` `VirtualDisplay` mode/refresh rate 1:1 with the real external display's active Hz (with an optional Test Mode to force 120Hz reporting on 60Hz monitors for pipeline testing). | **VERIFIED** (1:1 at 75Hz; Test Mode disabled in normal use to avoid skipped frames) | Keep **Test Mode OFF** (`0`) on 60Hz/75Hz monitors to prevent frame pacing mismatches. | **Hook 4** (`dex120_sync_virtual`) & **Test Mode** (`dex120_force_virtual_120`) |
| **Hook 5: `SdhmsDfsHook`** | Hooks `BinderProxy.transact` inside `com.sec.android.sdhms` to block `SurfaceFlinger` transaction `1122` (`DFS = 30` thermal cap around ~44°C battery), and proactively resets `DFS = 60` from `system_server`. | **VERIFIED** (confirmed blocking `DFS=30` & restoring 13.33ms VSync) | Requires ticking **`com.sec.android.sdhms`** in LSPosed scope. Does not stop CPU/GPU thermal throttling. | Controlled by **Hook 2** (`dex120_block_token`) |
| **AMOLED Black Screen** | Launches a pure `#000000` full-screen window at minimum brightness (`0.001f`) keeping Display 0 `ON` so Android 12 `SurfaceFlinger` does not fall back to `SyntheticVSync` 60Hz when the power button turns the panel off. | **VERIFIED** | Triple-tap quickly or press Back twice to exit. Display remains logically `ON` at `#000000`. | **AMOLED Black Screen** button in app |
| **Force 120Hz / Reset Adaptive** | Sets `Settings.Secure.refresh_rate_mode=1`, `Settings.System` `min/peak_refresh_rate=120.0` (avoiding SamHelper's invalid `120.1` rejected by S22's `120.00001Hz` mode), `low_power=0`, and resets `SurfaceFlinger DFS=60`. | **VERIFIED** | Requires Root (`su`). Third-party gaming/refresh modules or SamHelper may overwrite `min/peak_refresh_rate` back to `60` or `120.1`. | Two buttons in Card 3 |
| **Sanitized Diagnostics** | Collects device summary, `/sys/class/dp_sec/dex`, settings, `DeX120Hz` logs, `dumpsys display`, and `dmesg`, automatically redacting Serial, IMEI, MAC, private IP, Email, and Tokens before copying logs. | **VERIFIED** (`SanitizerTest` host unit test; new text edits not yet re-run on phone) | Needs Root for `dmesg` & `dp_sec`. Always review logs before posting publicly. | **Run Diagnostics** → **Copy Log** |

Technical deep-dive: [docs/architecture.md](docs/architecture.md) *(Vietnamese)*.

---

## Compatibility

Status definitions used across this project:
- **VERIFIED**: Tested directly on real hardware and confirmed with logs/measurements.
- **PARTIAL**: Software pipeline unlocked and verified at intermediate rates, but target hardware not yet physically tested.
- **EXPERIMENTAL**: Work-in-progress behavior or diagnostic option.
- **UNSUPPORTED**: Known incompatible / out of scope.
- **UNKNOWN**: No test data available.

| Item | Details | Status | Notes |
|---|---|---|---|
| **Device** | Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`) | **VERIFIED** | Verified only on a single `SM-S901B` (`r0s`). Snapdragon S22, S22+, S22 Ultra, and other Galaxy models: **UNKNOWN / UNSUPPORTED**. |
| **Android / One UI** | Android 12 (`API 31`) / One UI 4.1 | **VERIFIED** | Tested on kernel `5.10.66`. Android 13 / 14 / 15 (One UI 5/6/7): **UNSUPPORTED**. |
| **Root & Hook Stack** | KernelSU (Temproot) + ReZygisk + LSPosed | **VERIFIED** | Must support hooking `android` (`system_server`) and `com.sec.android.sdhms`. Magisk + Zygisk + LSPosed: **UNKNOWN** (compatible API in theory, untested here). |
| **75Hz External Monitor** | VSP `V2203H` (`1920x1080@75Hz`) via USB-C hub | **VERIFIED** | Monitor OSD showed `75Hz`, `dmesg` showed `decon[2] bts.fps:75`, and TestUFO exceeded 60fps once `DFS=30` was blocked (see native 75Hz caveat below). |
| **120Hz External Monitor** | `1080p@120Hz` / `1440p@120Hz` | **PARTIAL** | All 60Hz/30Hz software bottlenecks are unlocked up to 120Hz, but **no physical 120Hz monitor was available for final hardware confirmation**. |
| **USB-C Cable / Hub** | 2-lane USB 3.0 HDMI hub (`048d:5212`) vs 4-lane cable | **VERIFIED** (2-lane at 1080p@75Hz) | Exynos 2200 supports DP HBR2 (5.4 Gbps/lane): 2-lane USB 3.0 hubs cap around `1080p@120Hz`; `1440p@120Hz` requires a direct 4-lane USB-C to DP/HDMI cable. |

> [!NOTE]
> **Native SurfaceFlinger caveat on 75Hz monitors (does not affect native 120Hz monitors):**
> Disassembly of `/system/bin/surfaceflinger` on `SM-S901B` shows that Samsung's native `performSetActiveConfig()` forces Display 0 (120Hz) down to **60Hz** whenever connected to a slower external panel such as `75Hz` (because S22 has no 75Hz panel mode), and `/vendor/build.prop` ships `vendor.hwc.exynos.vsync_mode=0`, which picks the slower display's VSync (`16.67ms` = 60Hz) while both screens are on. On a **120Hz monitor**, the VSync periods match (`83` vs `83`), so this native downgrade is not triggered.

---

## Prerequisites (For Beginners)

If you are new to Android rooting, here is what the 4 required components mean:

1. **Root Access (`su`, e.g., KernelSU / Magisk):** Administrator privileges needed to write hidden `Settings.Global` flags (`dex120_*`), adjust refresh rate settings (`min/peak_refresh_rate`), and read `/sys/class/dp_sec/dex`.
2. **Zygisk (e.g., ReZygisk):** Injection framework that loads Xposed/LSPosed into Android processes during startup or Soft Reboot.
3. **LSPosed:** Module manager running on top of Zygisk that intercepts specific Samsung Java methods in memory without modifying `/system` or `/vendor` partitions.
4. **USB-C to HDMI / DisplayPort Cable or Hub** and an external monitor supporting **>60Hz** (75Hz, 120Hz, 144Hz, etc.).

---

## Step-by-Step Installation

### Step 1: Download the APK
1. Open [GitHub Releases](https://github.com/hangothanhh/DeX120Hz-S22/releases/latest).
2. Download both **`DeX120Hz-S22.apk`** and **`DeX120Hz-S22.apk.sha256`**.
3. **Do NOT** download `Source code (zip)` — this project installs as an APK + LSPosed module, **not** a KernelSU flashable module ZIP.

### Step 2: Verify SHA-256 Checksum
Open a terminal in your download folder and run the command for your OS:

- **Windows (PowerShell or CMD):**
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

Compare the 64-character hex string against `DeX120Hz-S22.apk.sha256` (case-insensitive). If even **one character differs**, delete the file and download it again.

### Step 3: Install APK & Enable All 3 Scopes in LSPosed
1. Install **`DeX120Hz-S22.apk`** normally (or via `adb install -r DeX120Hz-S22.apk`).
2. Open **LSPosed Manager** → **Modules** → tap **DeX 120Hz S22** → toggle **Enable module**.
3. Open LSPosed's menu and make sure **System apps** are visible.
4. Check **all 3 scopes**:
   - **`System Framework`** (`android`) — required for Hooks 1–4.
   - **`com.sec.android.sdhms`** — required for Hook 5 (blocking `DFS=30` thermal drops).
   - **`DeX 120Hz S22`** (`com.dex120.s22`) — allows the app to detect that LSPosed hooks are active.
5. Perform a **Soft Reboot** *(if you use KernelSU Temproot on a locked bootloader, do **NOT** perform a normal/hard Reboot)*.

### Step 4: First Launch & Verification
1. Open **DeX 120Hz S22** and grant **Root** (`su`) access when prompted (or enable `com.dex120.s22` in KernelSU Manager → Superuser).
2. Confirm both top badges turn **green**:
   - `LSPosed: ĐÃ KÍCH HOẠT` (Active)
   - `Root: Sẵn sàng` (Ready)
3. Connect your external monitor via USB-C to start **Samsung DeX**.
4. In Card 4, verify the 5 main hook switches are **ON** and **Test Mode** (`dex120_force_virtual_120`) is **OFF**.
5. Tap **Chạy chẩn đoán (Run Diagnostics)** and confirm:
   - `Hook initialization complete: 4/4 modules installed.`
   - External display mode upgraded (`Upgraded external display ... baseModeId`) and `Desktop` VirtualDisplay synced (`Synced VirtualDisplay Desktop mMode`).
   - Monitor OSD reports your monitor's high refresh rate (>60Hz).

---

## Update, Rollback, Uninstall & Recovery

Full step-by-step recovery procedures are in [docs/recovery.md](docs/recovery.md):

- **Update:** Hook switches persist in `Settings.Global` (`dex120_*`), so installing a newer APK over the same signature keeps them, though you should still back up your toggle states first. Verify SHA-256 → install APK → re-check all **3 LSPosed scopes** → **Soft Reboot**.
- **Rollback:** Disable **DeX 120Hz S22** in LSPosed → Soft Reboot → uninstall current APK → install previous verified APK → re-enable scopes → Soft Reboot.
- **Uninstall:** Tap **Khôi phục Adaptive** in the app → disable module in LSPosed → Soft Reboot → uninstall `DeX 120Hz S22`. (Optional ADB cleanup of `dex120_*` keys is documented in [docs/recovery.md](docs/recovery.md).)
- **Recovery from Soft-Reboot hang / bootloop:**
  - Hold **Power + Volume Down** for 7–10 seconds for a **Hard Reboot**.
  - On **Temproot setups**, a Hard Reboot returns cleanly to stock unrooted Android 12 **without losing data**.
  - On permanent root setups, use KernelSU/LSPosed module Safe Mode (or `adb wait-for-device shell su -c "settings put global dex120_enabled 0"` / `adb uninstall com.dex120.s22`).

---

## Detailed Documentation (`docs/`)

*(Detailed guides in `docs/` are currently written in Vietnamese.)*

| Document | Topic |
|---|---|
| [docs/installation.md](docs/installation.md) | Step-by-step beginner installation guide & root glossary |
| [docs/configuration.md](docs/configuration.md) | Toggle switches, `Settings.Global` (`dex120_*`) keys, SamHelper/module conflicts |
| [docs/architecture.md](docs/architecture.md) | 5-hook pipeline, DP HBR2 lane bandwidth, native `SurfaceFlinger` findings |
| [docs/testing-and-verification.md](docs/testing-and-verification.md) | Multi-layer verification via OSD, `dmesg`, `dumpsys`, `scrcpy`, and TestUFO |
| [docs/troubleshooting.md](docs/troubleshooting.md) | Troubleshooting common issues & sanitized Diagnostic Report guide |
| [docs/recovery.md](docs/recovery.md) | Update, rollback, clean uninstall, and tiered bootloop recovery |
| [docs/development.md](docs/development.md) | Building from source on Windows/Linux/macOS, keystore handling, and verification |

---

## Building from Source

<details>
<summary><b>Quick build commands for Windows and Linux/macOS</b> (see <a href="docs/development.md">docs/development.md</a> for details)</summary>

**Requirements:** JDK 17 (`javac`, `jar`, `keytool`), Android SDK Platform `>= 31` (`android-34` preferred), Android SDK `build-tools` (`aapt2`, `d8`, `zipalign`, `apksigner`), Python 3.

**Windows (PowerShell):**
```powershell
git clone https://github.com/hangothanhh/DeX120Hz-S22.git
cd DeX120Hz-S22
powershell -ExecutionPolicy Bypass -File .\build.ps1
python .\scripts\verify_apk.py
```

**Linux / macOS:**
```bash
git clone https://github.com/hangothanhh/DeX120Hz-S22.git
cd DeX120Hz-S22
chmod +x ./build.sh
./build.sh
python3 ./scripts/verify_apk.py
```

Outputs are generated under `out/`:
- `out/DeX120Hz-S22.apk`
- `out/DeX120Hz-S22.apk.sha256`

By default, builds sign with a local ephemeral/debug keystore (`debug.keystore`, ignored by `.gitignore` and never committed). Set `DEX120_KEYSTORE`, `DEX120_KEYSTORE_PASS`, `DEX120_KEY_ALIAS`, and `DEX120_KEY_PASS` to sign with an external keystore.

</details>

---

## Support, Contributing & License

- **Found a bug?** Run **Chạy chẩn đoán → Copy Log** in the app (Serial, IMEI, MAC, private IPs, and Tokens are auto-redacted — still review manually before sharing) and open a [GitHub Issue](https://github.com/hangothanhh/DeX120Hz-S22/issues/new/choose).
- **Want to contribute?** Read [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md) before opening a Pull Request.
- **AI Co-author:** `Co-authored-by: Antigravity <antigravity@google.com>` (see [NOTICE](NOTICE)).
- **License:** [MIT License](LICENSE) · Third-party credits & notices: [NOTICE](NOTICE).
