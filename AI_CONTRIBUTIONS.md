# AI Contributions & Verification Disclosure

**Language:** **English** | [Tiếng Việt](docs/vi/AI_CONTRIBUTIONS.md)

This document transparently discloses the scope of AI assistance (**Google Antigravity**) versus human ownership and real-hardware verification in **DeX 120Hz S22** (`com.dex120.s22`).

---

## 1. Identity & Git Attribution Policy (`ANTIGRAVITY_IDENTITY_UNVERIFIED`)

- **AI Assistant:** Google Antigravity.
- **Git Identity Status:** `ANTIGRAVITY_IDENTITY_UNVERIFIED`.
- **Commit Trailer Policy:** Commits on `main` do **NOT** include `Co-authored-by` trailers. All commits carry only the repository owner's author/committer identity (`hangothanhh <197389458+hangothanhh@users.noreply.github.com>`).
- **Technical & Governance Rationale:**
  - Checking `antigravity@google.com` via the GitHub API revealed that GitHub maps that email to an unverified regular user account named `antigravity-google-bot` (GitHub user ID `358880572`, `type: User`, created `2026-11-19T19:48:38Z`, lacking any profile bio or official Google verification).
  - The separate account `gemini-code-assist[bot]` (ID `176961590`) belongs to another GitHub App and was never used in this repository.
  - At the explicit instruction of the repository owner (`hangothanhh`), 4 post-`0b17242` commits that previously contained `Co-authored-by: Antigravity <antigravity@google.com>` were cleanly rewritten with `git commit-tree` to remove all links to `antigravity-google-bot`. Historical tag `v1.0.0-s22-a12` (`b10d6e9`) predates those commits and was untouched.

---

## 2. Division of Work

| Workstream | Google Antigravity Scope | Human Owner (`hangothanhh`) Scope |
| :--- | :--- | :--- |
| Xposed hooks & Android control app | Authored and refactored the 5 hook classes (`DpSysfsHook`, `LowRefreshTokenHook`, `DisplayModeHook`, `VirtualDisplaySyncHook`, `SdhmsDfsHook`), `MainActivity`, `BlackScreenActivity`, `RootShell` | Defined target device (`SM-S901B`), gathered real device diagnostics/disassembly evidence, tested and validated behavior |
| Diagnostic sanitizer & unit tests | Implemented `RootShell.sanitizeDiagnostics()` and `tests/SanitizerTest.java` | Reviewed redaction policy (Serial, IMEI, MAC, private IPs, Tokens) |
| Standalone build & verification scripts | Wrote `build.ps1`, `build.sh`, `scripts/verify_apk.py`, `stubs/de/robv/android/xposed/*` | Executed and verified builds locally on Windows |
| GitHub Actions CI/CD | Authored and hardened `.github/workflows/ci.yml` (`permissions: contents: read`, SHA-pinned actions) | Reviewed CI workflow, enabled repo SHA-pinning requirement and `Protect main branch` ruleset |
| Documentation & compliance governance | Authored bilingual documentation, `LICENSE_AUDIT_REPORT.md`, `SECURITY_AUDIT_REPORT.md`, `THIRD_PARTY_NOTICES.md`, `SBOM.cdx.json` | Chose `GNU GPL v3.0`, AI attribution rules, bilingual English-first priority, and `Pre-release` release policy |

---

## 3. Honest Verification Matrix & Boundaries

| Verification Area | Status | Factual Notes |
| :--- | :--- | :--- |
| APK Build (`aapt2` + `d8` + `zipalign` + `apksigner`) | **VERIFIED** | Verified cleanly on Windows host (JDK 17, Android SDK 34) and Ubuntu GitHub Actions |
| DEX Structure & Sanitizer Unit Tests (`scripts/verify_apk.py`, `tests/SanitizerTest.java`) | **VERIFIED** | Confirmed 0 Xposed stub classes in `classes.dex` and 100% pass on synthetic PII/token redaction tests |
| Live Samsung DeX High-Refresh Behavior | **HISTORICAL / PARTIAL** | Verified previously by owner on **1 Samsung Galaxy S22 Exynos (`SM-S901B`, Android 12 / One UI 4.1)** with a **75Hz external monitor (VSP `V2203H`)**. Physical 120Hz monitor output remains untested due to lack of 120Hz test hardware |
| On-Device Re-Test of `v1.0.1-s22-a12` (`ON_DEVICE_RETEST`) | **NOT_VERIFIED** | The `SM-S901B` test phone has been sold; `v1.0.1-s22-a12` only modified manifest version metadata, dynamic test fixture syntax, CI, and documentation without touching runtime Xposed hook logic |

---

## 4. Non-Endorsement Statement

Google LLC and the Google Antigravity team have not independently audited, do not warrant, and do not endorse this repository or its APK releases. Users assume full responsibility when installing Root / LSPosed system hooks into `system_server` and `com.sec.android.sdhms`.
