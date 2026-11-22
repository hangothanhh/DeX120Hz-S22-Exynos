# License, Copyright & Provenance Audit Report

**Language:** **English** | [Tiếng Việt](docs/vi/LICENSE_AUDIT_REPORT.md)

Technical compliance audit covering source provenance (Phase 2) and open-source license compatibility (Phase 3) for **DeX120Hz-S22-Exynos** (`com.dex120.s22`).

> [!IMPORTANT]
> **Legal Disclaimer:** This report summarizes a technical software-compliance audit conducted for open-source repository governance and **does not constitute formal legal counsel**. If you plan commercial distribution or derivative licensing beyond standard open-source reuse, consult qualified intellectual property counsel.

---

## 1. Source Origin & Provenance Inventory (Phase 2)

| Repository Path / Component | Origin Classification | Author / Source | License | Verification Notes |
| :--- | :--- | :--- | :--- | :--- |
| `app/src/main/java/com/dex120/s22/*.java` & `xposed/*.java` | Original + AI-assisted | `hangothanhh` with Google Antigravity assistance | `GPL-3.0-only` | Uses standard Java + public Android APIs only; design informed by `SM-S901B` firmware analysis and `Kinue72/OnigiriDeX` patch concepts without copying upstream files verbatim |
| `stubs/de/robv/android/xposed/*.java` | Clean-room API signature stubs | Minimal method signatures matching public Xposed/LSPosed API | `GPL-3.0-only` (LSPosed runtime compatibility) | Excluded from `classes.dex` during packaging; verified by `scripts/verify_apk.py` (`0` Xposed stub classes in APK) |
| `app/src/main/AndroidManifest.xml`, `res/`, `assets/xposed_init` | Original | `hangothanhh` | `GPL-3.0-only` | Authored specifically for `com.dex120.s22` |
| `build.ps1`, `build.sh`, `scripts/verify_apk.py`, `tests/SanitizerTest.java` | Original + AI-assisted | `hangothanhh` + Google Antigravity | `GPL-3.0-only` | No bundled third-party binaries; invokes local/CI Android SDK & JDK toolchain |
| `README*.md`, `docs/**/*.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md`, `NOTICE`, compliance reports | Original + AI-assisted | `hangothanhh` + Google Antigravity | `GPL-3.0-only` | AI scope & physical verification limits documented in `AI_CONTRIBUTIONS.md` |
| `LICENSE` | Verbatim upstream text | Free Software Foundation (`GNU GPL v3.0`, 35,149 bytes) | `GPL-3.0-only` | Exact verbatim GNU GPL v3.0 license text |
| `.github/workflows/ci.yml` | Original + GitHub Actions | `hangothanhh` / `actions/*` (`MIT`) | `GPL-3.0-only` (workflow file); Actions are `MIT` | All 4 official GitHub Actions pinned to exact commit SHAs |

---

## 2. Open-Source License Compatibility Analysis (Phase 3)

1. **Primary Project License:** **`GNU General Public License v3.0` (`GPL-3.0-only`)**, matching GitHub repository metadata (`licenseInfo: gpl-3.0`).
2. **Historical Conflict Resolution (RESOLVED):**
   - Initial commits started under `MIT`, while the module shares architectural design concepts with `Kinue72/OnigiriDeX` (`GPL-3.0`) and runs inside the `LSPosed` (`GPL-3.0`) + `KernelSU` (`GPL-3.0`) ecosystem.
   - At owner direction, the repository switched cleanly to `GNU GPL v3.0` (`ba065a0`, originally `8509725`), eliminating copyleft compatibility ambiguity.
3. **Historical Tag `v1.0.0-s22-a12` (`b10d6e9`):**
   - Created before the `MIT` → `GPL-3.0` transition.
   - To preserve Git history integrity without deleting published release tags, `v1.0.0-s22-a12` remains untouched as an archived pre-GPL snapshot, now carries an explicit `SUPERSEDED HISTORICAL SNAPSHOT` banner at the top of its release notes redirecting users to `v1.0.1-s22-a12` (`GPL-3.0`), and is marked `Superseded` in `SECURITY.md` and `CHANGELOG.md`.
4. **Source File SPDX Headers & Contributor Terms:** All 18 `.java` files (`app/`, `stubs/`, `tests/`) and all 3 build/verify scripts (`build.ps1`, `build.sh`, `scripts/verify_apk.py`) carry explicit `SPDX-License-Identifier: GPL-3.0-only` headers (enforced automatically by CI), and `CONTRIBUTING.md` specifies **Inbound = Outbound (`GPL-3.0-only`)** licensing for contributions.
5. **Bundled APK Dependencies:** **0 third-party libraries** (`dependencies: []` in `SBOM.cdx.json`). No static or dynamic linking conflicts with proprietary or incompatible open-source code exist.
6. **DEX & Packaging Verification:** `scripts/verify_apk.py` confirms that `out/DeX120Hz-S22.apk` contains only `com/dex120/s22/` classes (`59 class_defs`, `0` Xposed stubs) alongside `AndroidManifest.xml`, `assets/xposed_init`, `resources.arsc`, and `META-INF/*`.

---

## 3. Non-Code Assets & Trademarks

- **Non-code assets:** **0** third-party images, icons, commercial fonts, audio, video, datasets, or extracted Samsung firmware/vendor binaries are stored in this repository.
- **Trademarks:** Product names (`Samsung`, `Galaxy S22`, `Samsung DeX`, `One UI`, `Exynos`, `Android`, `Google`, `Antigravity`) are used solely for technical compatibility reference and carry explicit non-affiliation disclaimers in `NOTICE`, `THIRD_PARTY_NOTICES.md`, `README.md`, and `README.vi.md`.

---

## 4. Phase 2–3 Summary Verdict

- `SOURCE_UNKNOWN`: **0 components**.
- `OWNERSHIP_UNVERIFIED`: **0 third-party code ownership gaps**; AI Git identity is explicitly classified `ANTIGRAVITY_IDENTITY_UNVERIFIED` and excluded from `Co-authored-by` commit trailers (documented transparently in `AI_CONTRIBUTIONS.md`).
- `LICENSE_COMPATIBILITY`: **PASS** under `GNU GPL v3.0` (`GPL-3.0-only`).
