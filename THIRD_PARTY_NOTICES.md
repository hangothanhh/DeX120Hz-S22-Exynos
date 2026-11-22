# Third-Party Notices & Dependency Inventory

**Language:** **English** | [Tiếng Việt](docs/vi/THIRD_PARTY_NOTICES.md)

This document inventories all upstream inspirations, compile-only API stubs, build tools, CI GitHub Actions, and runtime ecosystem components related to **DeX 120Hz S22** (`com.dex120.s22`).

---

## 1. Runtime & Bundled Dependencies Inside APK (`out/DeX120Hz-S22.apk`)

- **Bundled third-party Java or native libraries inside `classes.dex` or `lib/`:** **0 (None)**.
- The application only uses standard Android platform SDK APIs (`android.*`, `java.*`) and hooks into the on-device LSPosed runtime via reflection.

---

## 2. Clean-Room Compile-Only Stubs (Excluded from `classes.dex`)

| Component | Repository Path | Reference | Ecosystem License | Packaging Notes |
| :--- | :--- | :--- | :--- | :--- |
| Clean-room Xposed API signature stubs (`IXposedHookLoadPackage`, `XC_MethodHook`, `XposedBridge`, `XposedHelpers`, `XC_LoadPackage`) | `stubs/de/robv/android/xposed/` | [LSPosed / Xposed API](https://github.com/LSPosed/LSPosed) | `GPL-3.0-only` (LSPosed) | Compiled to an intermediate scratch JAR strictly so `javac` resolves method signatures; completely excluded from `classes.dex` by `d8` and verified via `scripts/verify_apk.py`. |

---

## 3. Upstream Design Inspirations & Device Runtime Ecosystem

| Project / Ecosystem | Author / Source | License | Relationship |
| :--- | :--- | :--- | :--- |
| **OnigiriDeX** | [Kinue72/OnigiriDeX](https://github.com/Kinue72/OnigiriDeX) | `GPL-3.0-only` | Studied its DeX high-refresh patches (`0005-enable-dex-high-refresh.patch`, `0025-Add-mirroring-high-refresh-rate-toggle.patch`) and beginner-oriented README layout during design (no ROM source files copied directly). This repository adopts `GNU GPL v3.0` for full license compatibility. |
| **LSPosed** | [LSPosed/LSPosed](https://github.com/LSPosed/LSPosed) | `GPL-3.0-only` | On-device Xposed hook framework (not bundled in repo or APK). |
| **KernelSU & ReZygisk** | KernelSU / Zygisk authors | `GPL-3.0-only` / Open Source | On-device Root and Zygisk injection stack used during live testing on `SM-S901B` (not bundled in repo or APK). |

---

## 4. Build Toolchain (Host / CI Build-Time Only — Not Distributed)

| Tool | Version Used | License | Purpose |
| :--- | :--- | :--- | :--- |
| Android SDK Platform (`android.jar`) | API 34 (targeting API 31) | `Apache-2.0` (AOSP / Android SDK Terms) | Provides Android framework compile-time stubs |
| Android SDK Build-Tools (`aapt2`, `d8`, `zipalign`, `apksigner`) | `34.0.0` | `Apache-2.0` (Android SDK) | Resource packaging, DEX compilation, APK alignment, and APK signing |
| Eclipse Temurin / Microsoft Build of OpenJDK | JDK 17 | `GPL-2.0-with-classpath-exception` | Compiles Java (`javac --release 11`) and executes `SanitizerTest` / `keytool` |
| Python | 3.x (CI: `3.11`) | `PSF-2.0` | Runs `scripts/verify_apk.py` |

---

## 5. CI/CD GitHub Actions (Pinned by Commit SHA)

| Action | Version / Tag | Pinned Commit SHA | License |
| :--- | :--- | :--- | :--- |
| `actions/checkout` | `v7.0.1` | `3d3c42e5aac5ba805825da76410c181273ba90b1` | `MIT` |
| `actions/setup-java` | `v6.3.0` | `d1374f8ac74474200d16ef8ecee22c7ae7a51880` | `MIT` |
| `actions/setup-python` | `v7.1.0` | `6d4fd22b20465ccc0f7354476588e8df7c9ca0e0` | `MIT` |
| `actions/upload-artifact` | `v7.0.2` | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` | `MIT` |
| `github/codeql-action` | `v4` | `1bd86f2fdb4b86187fa4059f16f31a3202acc9ea` | `MIT` |

---

## 6. Non-Code Assets

- **0 bundled third-party non-code assets:** The repository contains no images (`.png`, `.jpg`, `.webp`), third-party icons, commercial fonts, audio/video files, datasets, model weights, or extracted Samsung proprietary binaries/firmware.

---

## 7. Trademark Disclaimer

"Samsung", "Galaxy", "Galaxy S22", "Samsung DeX", "One UI", and "Exynos" are trademarks or registered trademarks of Samsung Electronics Co., Ltd.
"Android", "Google", and "Antigravity" are trademarks of Google LLC.
All brand names are mentioned solely for technical compatibility identification.
