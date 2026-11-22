# Security, Supply Chain & Release Readiness Audit Report

**Language:** **English** | [Tiếng Việt](docs/vi/SECURITY_AUDIT_REPORT.md)

Security, privacy, software supply-chain audit (Phase 4), GitHub ownership/access-control review (Phase 5), and 15-point release readiness assessment (Phase 16) for **DeX120Hz-S22-Exynos** (`com.dex120.s22`).

---

## 1. Secret, PII & Binary Scanning Across Git History (Phase 4.1 & 4.2)

- **Scope inspected:** Full working tree plus every historical commit on `main` (from root commit `9e75c64` to current `HEAD`).
- **Signing keys & binaries check:**
  - `*.apk`, `*.dex`, `*.jar`, `*.keystore`, `*.jks`, `*.pk8`, `*.pem`, `*.key`: **0 files ever committed in Git history**.
  - Ephemeral `debug.keystore` and `build/`, `out/` artifacts are strictly ignored by `.gitignore`.
- **Secrets & personal data check:**
  - Real Personal Access Tokens, private keys, passwords, hardware IMEIs, device serials (`SM-S901B` / secondary devices), and real LAN/ADB IPs: **0 leaks found**.
  - A previously static synthetic fixture token in `tests/SanitizerTest.java` was refactored into dynamic string concatenation (`"ghp_" + "..."`) and is now guarded by an automated CI check against static `ghp_`/`gho_`/`github_pat_` literals.
  - CLI syntax strings such as `"pass:$KS_PASS"` in `build.ps1`/`build.sh` reference temporary build-time variables only, not real secrets.
- **Output redaction layer (`RootShell.sanitizeDiagnostics`):**
  - Covered by `tests/SanitizerTest.java` for Samsung serials, IMEI, MAC, private/loopback IPv4 (`10.x`, `172.16-31.x`, `192.168.x`, `127.x`), email addresses, GitHub tokens (`ghp_`/`gho_`/`github_pat_`), and secret key-value pairs, while preserving benign technical markers (`dex120_block_token`, `token:[DMS]HardwareManager`, `SiopLimiterHrr`).

---

## 2. CI/CD & Supply Chain Hardening (Phase 4.3 & 4.4)

| Control | Before Audit | After Remediation (`v1.0.1-s22-a12`+) | Status |
| :--- | :--- | :--- | :--- |
| Workflow `GITHUB_TOKEN` permissions | Implicit default (`read`) without workflow declaration | Explicit top-level `permissions: contents: read` (least privilege) | **PASS** |
| GitHub Actions pinning | Mutable version tags (`@v4`, `@v5`) | All actions pinned to immutable **Commit SHAs** (`actions/checkout` v7.0.1, `setup-java` v6.3.0, `setup-python` v7.1.0, `upload-artifact` v7.0.2) + repo-level **`sha_pinning_required: true`** | **PASS** |
| Automated compliance checks | Keystore check + `bash -n build.sh` | Added `LICENSE` GPL-3.0 check, `SBOM.cdx.json` JSON validation, static token literal rejection, and `SPDX-License-Identifier: GPL-3.0-only` header verification across all `.java` & build/test scripts | **PASS** |
| SAST & Dependency Automation | None | Added `CodeQL` workflow (`java-kotlin` + `actions`) & `.github/dependabot.yml` (`github-actions` monthly) | **PASS** |
| Line endings & file modes | `build.sh` & `scripts/verify_apk.py` tracked as `100644` without `.gitattributes` | Added `.gitattributes` (`LF` default, `CRLF` for `*.ps1`) and set executable mode `100755` on `build.sh` & `scripts/verify_apk.py` | **PASS** |
| Third-party SBOM | None | Added CycloneDX `1.5` `SBOM.cdx.json` (`0` bundled runtime dependencies; build/CI tools recorded as `excluded`) | **PASS** |

---

## 3. GitHub Ownership & Repository Access Controls (Phase 5)

- **Owner:** `hangothanhh` (`admin: true`), outside collaborators: **0** (`.github/CODEOWNERS`: `* @hangothanhh`).
- **Deploy keys / Webhooks:** **0**.
- **GitHub Security & Analysis features enabled:**
  - **Private Vulnerability Reporting:** Enabled (`enabled: true`).
  - **Secret Scanning & Push Protection:** Both `enabled` at repository level.
  - **Dependabot Alerts & Automated Security Updates:** Both `enabled` (`status: enabled`) + `.github/dependabot.yml`.
  - **GitHub Actions repository settings:** `enabled: true`, `allowed_actions: all`, **`sha_pinning_required: true`**, `default_workflow_permissions: read`, `delete_branch_on_merge: true`.
- **Active Branch Protection / Ruleset (`Protect main branch` — Ruleset `#27848863`):**
  - Target: `~DEFAULT_BRANCH` (`main`), `enforcement: active`.
  - Rules enforced: `deletion` blocked, `non_fast_forward` (force-push) blocked, `pull_request` required, and status check **`build-and-verify`** required (`RepositoryRole` admin `actor_id: 5` allowed `always` bypass for owner maintenance).

---

## 4. Residual Risk Register

| Severity | Item | Honest Assessment & Mitigation |
| :--- | :--- | :--- |
| **INFO** | Release keystore & SLSA Build Provenance configured | Dedicated PKCS12 release keystore (`dex120-release`, RSA-4096, public SHA-256 `07:3F:BF:73:65:90:0B:03:1F:5D:2D:6C:8A:1C:1A:9A:29:FE:28:7F:F0:6C:17:41:91:DB:FF:EE:8C:07:34:B1`) stored outside repo and encrypted in GitHub Secrets for `.github/workflows/release.yml` + `actions/attest-build-provenance`. Local commits use Ed25519 SSH signing (`commit.gpgsign=true`). Historical `v1.0.1-s22-a12` used debug signing. |
| **INFO** | `v1.0.1-s22-a12` not re-tested on device | The `SM-S901B` test phone has been sold. Runtime Xposed hooks are identical to the previously live-verified 75Hz build; `v1.0.1-s22-a12` and later pure-Java helper tests (`HookLogicTest`) are verified via host build, `apksigner`, `verify_apk.py`, `SanitizerTest`, and Ubuntu CI only. |
| **INFO** | Privileged `Root` + `system_server` architecture | Inherent to LSPosed display-mode hooking; prominently warned in `README.md`, `SECURITY.md`, and `docs/recovery.md`. |

---

## 5. Phase 16 Release Readiness Checklist (15 Items)

| # | Audit Criterion | Status |
| :---: | :--- | :--- |
| 1 | Clean Git status & commit history | **PASS** (`Co-authored-by` misattribution to `antigravity-google-bot` removed cleanly; zero untracked secrets) |
| 2 | Verified source code provenance | **PASS** (0 `SOURCE_UNKNOWN`) |
| 3 | Ownership & contributor clarity | **PASS** (`hangothanhh` sole commit author; `.github/CODEOWNERS`; AI scope separated into `AI_CONTRIBUTIONS.md`) |
| 4 | Open-source license compatibility | **PASS** (`GNU GPL v3.0`, 100% SPDX headers on `.java` & scripts, Inbound=Outbound clause) |
| 5 | Dependency audit & SBOM | **PASS** (0 bundled runtime deps; `THIRD_PARTY_NOTICES.md` & `SBOM.cdx.json`) |
| 6 | Non-code asset rights | **PASS** (0 third-party media/binaries) |
| 7 | Trademark disclaimers | **PASS** (`NOTICE`, `THIRD_PARTY_NOTICES.md`, `README.md`, `README.vi.md`) |
| 8 | Secret & history scan | **PASS** (0 leaks in history + GitHub Secret Scanning & Push Protection enabled) |
| 9 | Privacy & PII protection | **PASS** (`RootShell.sanitizeDiagnostics` verified via `SanitizerTest` + Private Vulnerability Reporting enabled) |
| 10 | CI/CD & GitHub Ruleset security | **PASS** (`permissions: contents: read`, repo `sha_pinning_required: true`, active Ruleset `#27848863`, `CodeQL`, Dependabot) |
| 11 | Authentic AI attribution (Phase 6) | **PASS** (`ANTIGRAVITY_IDENTITY_UNVERIFIED` recorded transparently; no unverified bot attribution) |
| 12 | Conventional Commits & `.gitattributes` | **PASS** (atomic commits, `100755` executable bits on `build.sh` & `verify_apk.py`) |
| 13 | Bilingual repository & Community Profile | **PASS** (`CODE_OF_CONDUCT.md`, `.github/ISSUE_TEMPLATE/config.yml`, English-first docs + `README.vi.md` & `docs/vi/`) |
| 14 | Build & test verification | **PASS (HOST & CI)** / **NOT_VERIFIED_ON_DEVICE** (`SanitizerTest` + `HookLogicTest` passing on host & CI; test device `SM-S901B` sold) |
| 15 | Changelog, SBOM & Pre-release tags | **PASS** (`v1.0.1-s22-a12` with APK, `.sha256`, `SBOM.cdx.json`) |

### Overall Verdict: **`CONDITIONAL GO`**
- Approved for distribution strictly as **`Pre-release / Experimental` (`v1.0.1-s22-a12`, `GNU GPL v3.0`)**.
- Release conditions (do not represent as `Stable`):
  1. Clearly preserve the **`Pre-release / Experimental`** label and disclose that `v1.0.1-s22-a12` was not re-tested on physical hardware because the `SM-S901B` test device was sold.
  2. Distribute APK with matching `SHA-256` checksum due to debug-keystore signing.
  3. Generate a dedicated offline release keystore outside Git before any future broad `Stable` distribution.
