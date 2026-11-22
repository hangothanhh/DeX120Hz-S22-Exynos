# Changelog

**Language:** **English** | [Tiếng Việt](docs/vi/CHANGELOG.md)

All notable changes to **DeX 120Hz S22** (`com.dex120.s22`) are documented in this file following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

### Added
- Added `SPDX-License-Identifier: GPL-3.0-only` copyright headers across all Java source files (`app/`, `stubs/`, `tests/`) and build/verification scripts (`build.ps1`, `build.sh`, `scripts/verify_apk.py`), enforced automatically in CI.
- Added pure-Java unit test suite `tests/HookLogicTest.java` (covering `DpSysfsHook.maskDexWriteValue`, `DpSysfsHook.isPassthroughDpCommand`, `DpSysfsHook.parseIntSafe`, `Config.sanitizeTargetHz`, reflection helpers, and `ModuleStatus`) and expanded `tests/SanitizerTest.java`, wired into `scripts/verify_apk.py`.
- Added community health and governance files: `CODE_OF_CONDUCT.md`, `.github/CODEOWNERS`, `.github/ISSUE_TEMPLATE/config.yml`, `.github/dependabot.yml`, repository status badges in `README*.md`, and an Inbound = Outbound `GPL-3.0-only` contributor clause in `CONTRIBUTING.md`.
- Added `.github/workflows/codeql.yml` (CodeQL SAST scanning for `java-kotlin` and `actions` using SHA-pinned `github/codeql-action` v4).
- Added `.github/workflows/release.yml` (`Release and Attest APK`: signs tag/manual release APKs with a dedicated off-repo RSA-4096 PKCS12 keystore stored in GitHub Secrets and generates SLSA Build Provenance Attestations via SHA-pinned `actions/attest-build-provenance` v4.2.2).
- Enabled local Ed25519 SSH commit/tag signing (`gpg.format=ssh`, `commit.gpgsign=true`, `tag.gpgsign=true`).

### Changed
- Promoted **English** to the primary default language across `README.md`, all root governance/audit documents, all 7 guides under `docs/`, all 4 `.github/` issue/PR templates, and release notes; relocated Vietnamese mirrors cleanly to `README.vi.md` and `docs/vi/`.
- Removed redundant `README.en.md` after promoting it to `README.md`.
- Set GitHub repository topics, `delete_branch_on_merge: true`, and added a `SUPERSEDED HISTORICAL SNAPSHOT` banner to the legacy `v1.0.0-s22-a12` release notes.

### Security
- Enabled GitHub **Private Vulnerability Reporting**, **Secret Scanning**, **Secret Scanning Push Protection**, **Dependabot Alerts & Security Updates**, Actions **`sha_pinning_required: true`**, and active Repository Ruleset **`Protect main branch`** (`#27848863`: blocks deletion and non-fast-forward pushes, requires PR + `build-and-verify` status check, with `RepositoryRole` admin bypass).

---

## [v1.0.1-s22-a12] - 2026-11-22 (Pre-release)

License compliance, supply-chain security, bilingual documentation, and CI/CD hardening release built on top of `v1.0.0-s22-a12` (no changes to core Xposed hook logic).

### Added
- Added beginner-friendly English documentation and language switcher bars across repository documentation.
- Added `.gitattributes` standardizing line endings (`LF` default, `CRLF` for `*.ps1`, binary rules for build outputs).
- Added compliance and governance deliverables: `AI_CONTRIBUTIONS.md`, `THIRD_PARTY_NOTICES.md`, `LICENSE_AUDIT_REPORT.md`, `SECURITY_AUDIT_REPORT.md`, and `SBOM.cdx.json` (CycloneDX `1.5`).
- Added **Supported Versions** and **APK Signing Policy** in `SECURITY.md`, plus **Trademark Disclaimers** in `NOTICE`, `README.md`, and `THIRD_PARTY_NOTICES.md`.

### Changed
- Switched project license from MIT to **`GNU GPL v3.0` (`GPL-3.0`)** and updated `NOTICE` for full license compatibility with `Kinue72/OnigiriDeX` (`GPL-3.0`), `LSPosed`, and `KernelSU`.
- Updated repository URLs to `hangothanhh/DeX120Hz-S22-Exynos`.
- Bumped `versionCode` to `2` and `versionName` to `1.0.1-s22-a12` in `AndroidManifest.xml` and `RootShell.java`; set executable mode (`100755`) on `build.sh` and `scripts/verify_apk.py`.
- Stripped `Co-authored-by: Antigravity <antigravity@google.com>` trailers from post-`0b17242` commit history and updated `CONTRIBUTING.md` / `AI_CONTRIBUTIONS.md` to prevent misattribution to the unverified `antigravity-google-bot` GitHub account.

### Security
- Hardened `.github/workflows/ci.yml`: explicit `permissions: contents: read`, commit-SHA pinning across all actions (`actions/checkout` v7.0.1, `setup-java` v6.3.0, `setup-python` v7.1.0, `upload-artifact` v7.0.2), automated `LICENSE` GPL-3.0 verification, `SBOM.cdx.json` schema/JSON check, and static token literal blocking.
- Constructed the synthetic GitHub token fixture dynamically via string concatenation in `tests/SanitizerTest.java`.

---

## [v1.0.0-s22-a12] - 2026-11-22 (Pre-release)

First experimental release strictly targeting **Samsung Galaxy S22 Exynos (`SM-S901B` / `s5e9925`) running Android 12 (`API 31`) / One UI 4.1 with Root + LSPosed**. *(Historical pre-GPL snapshot; superseded by `v1.0.1-s22-a12`.)*

### Added
- **5 Xposed/LSPosed hook layers:**
  - `DpSysfsHook`: Intercepts writes of `0x10`/`0x11` to `/sys/class/dp_sec/dex` to keep the kernel DisplayPort driver in Mirror mode, unlocking >60Hz modes; conditionally spoofs reads as `"1"` only when an external display is active.
  - `LowRefreshTokenHook`: Blocks `<=60Hz` lock tokens (`acquireLowRefreshRateToken`, `acquireRefreshRateMaxLimitToken`) from `[DMS]HardwareManager`, `SiopLimiterHrr`, and callers while an external display is active, caching safe interface-specific proxies (`ILowRefreshRateToken` and `IRefreshRateMaxLimitToken`).
  - `DisplayModeHook`: Upgrades external display `baseModeId` and expands `RefreshRateRange` up to `dex120_target_hz` (default 120Hz), while protecting Display 0 at 120Hz during external display sessions.
  - `VirtualDisplaySyncHook`: Synchronizes `VirtualDisplay "Desktop"` with the external display's active refresh rate (plus optional Test Mode `dex120_force_virtual_120`).
  - `SdhmsDfsHook`: Intercepts `SurfaceFlinger` Binder transaction `1122` (`DFS = 30`) from `com.sec.android.sdhms` under thermal load, and proactively resets `DFS = 60` from `system_server`.
- **`DeX 120Hz S22` control app (`MainActivity`, `BlackScreenActivity`, `RootShell`, `ModuleStatus`):**
  - Automatic detection of LSPosed module activation and Root status.
  - AMOLED Black Screen (`#000000`, `0.001f` brightness) keeping Display 0 `ON` so Android 12 `SurfaceFlinger` avoids `SyntheticVSync` 60Hz fallback when turning the screen off.
  - **Force 120Hz** toggle (`Settings.Secure.refresh_rate_mode=1`, `Settings.System` `min/peak_refresh_rate=120.0`, `low_power=0`, `SurfaceFlinger DFS=60` reset) and SamHelper `120.1` warning.
  - **Run Diagnostics** helper with automatic PII/secret redaction (`RootShell.sanitizeDiagnostics`) before copying logs (redacts Serial, IMEI, MAC, private IPs, Email, Tokens/Passwords).
- **Standalone build, verification & docs:**
  - Gradle-free build scripts (`build.ps1` for Windows, `build.sh` for Linux/macOS) and automated APK/DEX/SHA-256 + `SanitizerTest` suite (`scripts/verify_apk.py`, `tests/SanitizerTest.java`).
  - Initial guides (`docs/`), `NOTICE`, `SECURITY.md`, `CONTRIBUTING.md`, and GitHub Issue/PR templates.

### Compatibility & Verification Scope
- **Verified (`VERIFIED`):** `SM-S901B` (Exynos 2200), Android 12 / One UI 4.1 (`API 31`, kernel `5.10.66`), KernelSU temproot + ReZygisk + LSPosed, external monitor **VSP `V2203H` (`1920x1080@75Hz`)** over USB-C hub (`OSD 75Hz`, `decon[2] bts.fps:75`, `VSync 13.33ms` after `DFS=60`).
- **Partially verified (`PARTIAL`):** Software pipeline unlocked up to `120Hz`, **untested on physical 120Hz hardware**.
- **Unsupported (`UNSUPPORTED`):** Android 13/14/15 (One UI 5/6/7), Snapdragon S22, or other Galaxy models.

### Known Issues
- On **75Hz external monitors**, Samsung's native `/system/bin/surfaceflinger` forces Display 0 (120Hz) down to 60Hz due to incompatible VSync periods, and with default `vendor.hwc.exynos.vsync_mode=0`, application VSync follows the slower 60Hz display while Display 0 remains on.
- Long-term stability, thermals, and link bandwidth at `1080p@120Hz` / `1440p@120Hz` have not yet been verified on physical 120Hz monitors.
- Third-party refresh-rate/gaming apps or modules writing `120.1` (e.g., SamHelper) or `60` into `min/peak_refresh_rate` can override app settings.
