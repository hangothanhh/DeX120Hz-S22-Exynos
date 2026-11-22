# Contributing to DeX 120Hz S22

**Language:** **English** | [Tiếng Việt](docs/vi/CONTRIBUTING.md)

Thank you for your interest in **DeX 120Hz S22**! Because this project injects hooks into privileged Android processes (`system_server` and `com.sec.android.sdhms`) using Root + LSPosed, every contribution requires strict accuracy and engineering discipline.

---

## AI Contribution and Anti-AI-Slop Policy

> [!WARNING]
> **MANDATORY policy for every Pull Request:**
> Because a large portion of the initial codebase and documentation in this repository was **created with AI assistance**, the maintainer is acutely aware of both the strengths and failure modes of AI-generated code. Therefore, this project enforces a strict **anti-"AI slop"** rule:

1. **Honest AI disclosure is mandatory:** If you use AI tools for coding, disassembly analysis, or documentation, declare it accurately in the Pull Request template and describe how you verified each change.
2. **Do NOT submit code you have not read and understood:** You must be able to explain *why* a hook or patch works on One UI 4.1 / Android 12, which exact Android methods/signatures it targets, and what edge cases could trigger regressions.
3. **Zero fabricated compatibility or benchmark claims:**
   - Do NOT add untested devices (S22 Snapdragon, S22+, S23, etc.) or Android 13/14/15 to "VERIFIED" tables without real-device hardware execution backed by logs (`dmesg` / `dumpsys display` / `dumpsys SurfaceFlinger` / monitor OSD).
   - Never fabricate benchmark numbers, SHA-256 hashes, or class/method names that do not exist in actual Samsung firmware.
4. **No placebo or unsafe tweaks:** Never add meaningless `build.prop` props, disable SoC/CPU/GPU thermal safety protections, or fake unsupported features.
5. **Local build and verification are mandatory before opening any PR.**

Pull Requests exhibiting unreviewed AI copy-paste, invalid Android 12 API signatures, bloated boilerplate, or missing build/test verification will be closed immediately.

---

## Mandatory Safety Rules

- **NEVER** commit APK signing keys (`*.keystore`, `*.jks`, `*.pk8`, `*.pem`).
- **NEVER** commit sensitive data or unsanitized logs (Serial numbers, IMEIs, MAC addresses, private/local IPs, Tokens, Play Integrity hardware attestation artifacts).
- **NEVER** bundle third-party libraries or binaries of unknown origin or without proper license attribution in [NOTICE](NOTICE) / [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- **NEVER** hand-edit generated artifacts (`build/`, `out/`); modify sources under `app/`, `stubs/`, `scripts/`, `build.ps1`, or `build.sh`.
- **NEVER** remove existing copyright notices or upstream attributions.
- Keep edits focused and preserve the standalone build toolchain (`aapt2` + `d8` + `apksigner`, `minSdk 31`).

---

## Step-by-Step Contribution Workflow

### 1. Fork and Clone
1. Click **Fork** on `hangothanhh/DeX120Hz-S22-Exynos` to your GitHub account.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/DeX120Hz-S22-Exynos.git
   cd DeX120Hz-S22-Exynos
   ```

### 2. Set Up Build Environment
See [docs/development.md](docs/development.md) for exact requirements (JDK 17, Android SDK Platform `>= 31`, Build-Tools, Python 3).

### 3. Create a Dedicated Branch
Never commit directly to `main` (which is protected by Repository Ruleset `#27848863`). Create a topic branch per change:
```bash
git checkout -b fix/dp-disconnect-delay
```

### 4. Code Standards & Hook Guidelines
- Follow `.editorconfig` and `.gitattributes` (UTF-8, 4-space indentation for Java/Python/XML, LF line endings for non-`.ps1` files).
- When editing hooks under `app/src/main/java/com/dex120/s22/xposed/`:
  - Ensure compile-only stubs in `stubs/de/robv/android/xposed/` never leak into `classes.dex`.
  - Guard against reflection failures crashing `system_server` (wrap safely with `try/catch (Throwable)` and log via `Config.logError`).
  - Preserve runtime toggle control via `Config` (`Settings.Global` `dex120_*` keys) so users can disable individual hooks immediately.

### 5. Mandatory Build & Verification

On **Windows (PowerShell)**:
```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
python .\scripts\verify_apk.py
```

On **Linux / macOS**:
```bash
bash -n ./build.sh
./build.sh
python3 ./scripts/verify_apk.py
```

If your change affects runtime behavior (DeX hooks, UI, `RootShell`), test it on an actual device (or state explicitly which checks only ran on host/CI JVM and what remains untested on physical hardware).

### 6. Commit Message Convention

Use concise [Conventional Commits](https://www.conventionalcommits.org/):

- `feat: add refresh rate toggle for <scenario>` — new feature
- `fix: prevent DFS 30Hz cap when Display 0 sleeps` — bug fix
- `docs: clarify temproot soft reboot steps` — documentation update
- `refactor: extract mode selection helper` — non-behavioral refactor
- `perf: reduce polling overhead in Config` — performance improvement
- `test: add sanitization test case for serial format` — unit test additions/fixes
- `ci: validate shell syntax in workflow` — GitHub Actions CI changes
- `chore: update gitignore patterns` — repository maintenance and build scripts

> [!NOTE]
> **AI Commit Attribution Policy:** Do **NOT** attach `Co-authored-by` trailers with unverified emails or unrelated GitHub accounts/bots (such as `antigravity-google-bot` or `gemini-code-assist[bot]`). Disclose AI assistance honestly in the Pull Request template and update [AI_CONTRIBUTIONS.md](AI_CONTRIBUTIONS.md) / [NOTICE](NOTICE) instead.

### 7. Open a Pull Request
Complete every section of `.github/PULL_REQUEST_TEMPLATE.md`, including physical test hardware details (device model, Android/One UI build, kernel, LSPosed/Root stack), sanitized logs, and rollback instructions.
