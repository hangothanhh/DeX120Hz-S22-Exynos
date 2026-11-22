# Security, Supply Chain & Release Readiness Audit Report

**Ngôn ngữ / Language:** [English](../../SECURITY_AUDIT_REPORT.md) | **Tiếng Việt**

Báo cáo kiểm toán bảo mật, quyền riêng tư, chuỗi cung ứng phần mềm (Phase 4), cấu hình quyền GitHub (Phase 5) và đánh giá sẵn sàng phát hành (Phase 16) cho repository **DeX120Hz-S22-Exynos** (`com.dex120.s22`).

---

## 1. Quét bí mật, dữ liệu riêng tư & tệp nhị phân trong toàn bộ lịch sử Git (Phase 4.1 & 4.2)

- **Phạm vi kiểm tra:** Toàn bộ cây làm việc + lịch sử mọi commit trên nhánh `main` (từ commit gốc `9e75c64` tới hiện tại).
- **Kết quả kiểm tra nhị phân & khóa ký:**
  - `*.apk`, `*.dex`, `*.jar`, `*.keystore`, `*.jks`, `*.pk8`, `*.pem`, `*.key`: **0 tệp từng xuất hiện trong lịch sử Git**.
  - Khóa cục bộ `debug.keystore` và thư mục `build/`, `out/` được `.gitignore` loại trừ và không bao giờ đưa lên remote.
- **Kết quả kiểm tra bí mật & PII:**
  - Personal Access Tokens, Private Keys, mật khẩu thật, IMEI, Serial Number máy thật (`SM-S901B` / A10), IP mạng nội bộ thật: **0 phát hiện rò rỉ**.
  - Phát hiện 1 chuỗi giả lập dạng `ghp_<32_ky_tu_gia_lap>` từng nằm nguyên chuỗi tĩnh trong `tests/SanitizerTest.java` (mục đích unit test bộ lọc `sanitizeDiagnostics`). Đã khắc phục bằng cách ghép chuỗi động trong Java (`"ghp_" + "..."`) và thêm bước kiểm tra tự động trong CI.
  - Chuỗi `"pass:$KS_PASS"` trong `build.ps1`/`build.sh` chỉ là tham số biến môi trường cho `apksigner` / `keytool` sinh `debug.keystore` cục bộ (không phải mật khẩu bí mật).
- **Lớp bảo vệ đầu ra log (`RootShell.sanitizeDiagnostics`):**
  - Đã kiểm thử tự động qua `tests/SanitizerTest.java` cho Samsung Serial, IMEI, MAC, Private IP (`10.x`, `172.16-31.x`, `192.168.x`, `127.x`), email, GitHub token (`ghp_`/`gho_`/`github_pat_`) và từ khóa bí mật trong khi vẫn giữ nguyên tag chẩn đoán kỹ thuật hợp lệ (`dex120_block_token`, `token:[DMS]HardwareManager`, `SiopLimiterHrr`).

---

## 2. Bảo mật CI/CD & Chuỗi cung ứng (Phase 4.3 & 4.4)

| Hạng mục | Trước kiểm toán | Sau khắc phục (`v1.0.1-s22-a12`) | Trạng thái |
| :--- | :--- | :--- | :--- |
| Workflow `GITHUB_TOKEN` permissions | Không khai báo tường minh trong `.github/workflows/ci.yml` (dựa vào mặc định repo `read`) | Khai báo rõ `permissions: contents: read` (least privilege) | **PASS** |
| Ghim phiên bản GitHub Actions | Dùng tag biến thiên (`@v4`, `@v5`, cảnh báo Node 20 deprecated) | Ghim toàn bộ theo **Commit SHA** (`checkout` v7.0.1, `setup-java` v6.3.0, `setup-python` v7.1.0, `upload-artifact` v7.0.2) | **PASS** |
| Kiểm tra tuân thủ tự động trong CI | Kiểm tra keystore + `bash -n build.sh` | Bổ sung kiểm tra `LICENSE` GPL-3.0, cú pháp JSON của `SBOM.cdx.json`, và chặn chuỗi `ghp_`/`gho_`/`github_pat_` tĩnh trong mã nguồn & test | **PASS** |
| Chuẩn hóa kết thúc dòng & quyền thực thi | `build.sh` & `scripts/verify_apk.py` mang mode `100644`, chưa có `.gitattributes` | Thêm `.gitattributes` (`LF` mặc định, `CRLF` riêng cho `*.ps1`) và bật bit `100755` cho `build.sh`, `scripts/verify_apk.py` | **PASS** |
| Third-party dependencies | 0 thư viện đóng gói trong APK | Ghi nhận đầy đủ build tools & CI actions trong `SBOM.cdx.json` (CycloneDX `1.5`) | **PASS** |

---

## 3. Kiểm tra quyền GitHub & Cấu hình Repository (Phase 5)

- **Chủ sở hữu:** `hangothanhh` (`admin: true`), số collaborator ngoài: **0**.
- **Deploy keys / Webhooks:** **0**.
- **GitHub Actions setting:** `enabled: true`, `allowed_actions: all`, `default_workflow_permissions: read`.
- **Gia cố GitHub đã kích hoạt (`Protect main branch` + SHA Pinning):**
  - **GitHub Actions `sha_pinning_required: true`** đã được bật ở cấp repository (`enabled: true`, `allowed_actions: all`, `sha_pinning_required: true`).
  - **Repository Ruleset `#27848863` (`Protect main branch`, `enforcement: active`)** đã được kích hoạt trên `~DEFAULT_BRANCH` (`main`): chặn xóa nhánh (`deletion`), chặn force-push (`non_fast_forward`), yêu cầu Pull Request và check **`build-and-verify`** pass (`RepositoryRole` admin `actor_id: 5` có quyền bypass phục vụ bảo trì trực tiếp của chủ repo).

---

## 4. Sổ rủi ro còn lại (Risk Register)

| Mức độ | Hạng mục | Mô tả & Cách xử lý hiện tại |
| :--- | :--- | :--- |
| **MEDIUM** | Ký APK bằng `debug.keystore` | Phù hợp cho bản `Pre-release / Experimental` mã nguồn mở; người dùng cuối phải kiểm tra `SHA-256` kèm theo Release hoặc tự build. Nếu sau này phát hành bản `Stable` diện rộng, cần tạo release keystore riêng và bảo vệ tuyệt đối ngoài repo. |
| **INFO** | Chưa tái test `v1.0.1-s22-a12` trên máy thật | Máy test `SM-S901B` đã được bán. Các hook Xposed giống hoàn toàn bản đã test `75Hz` trước đó; bản `v1.0.1-s22-a12` được xác nhận build sạch + ký hợp lệ + pass toàn bộ `verify_apk.py` & `SanitizerTest` + pass CI Ubuntu. |
| **INFO** | Kiến trúc đặc quyền `Root` + `system_server` | Đặc tính cố hữu của module LSPosed can thiệp display mode; đã được cảnh báo rõ đầu `README.md`, `README.en.md`, `SECURITY.md` và kèm hướng dẫn khôi phục `docs/recovery.md`. |

---

## 5. Đánh giá sẵn sàng phát hành (Phase 16 — 15-Point Checklist)

| # | Tiêu chí kiểm toán | Kết quả |
| :---: | :--- | :--- |
| 1 | Git status & working history sạch | **PASS** (đã làm sạch trailer `Co-authored-by` ánh xạ nhầm `antigravity-google-bot`; zero untracked secrets) |
| 2 | Nguồn gốc mã nguồn rõ ràng | **PASS** (0 `SOURCE_UNKNOWN`) |
| 3 | Quyền sở hữu & Contributor minh bạch | **PASS** (duy nhất `hangothanhh`, AI tách sang `AI_CONTRIBUTIONS.md`) |
| 4 | Không xung đột giấy phép | **PASS** (`GNU GPL v3.0` đồng bộ với `OnigiriDeX`, `LSPosed`, `KernelSU`) |
| 5 | Dependencies được kiểm toán | **PASS** (0 bundled runtime deps; có `THIRD_PARTY_NOTICES.md` & `SBOM.cdx.json`) |
| 6 | Non-code assets an toàn | **PASS** (0 tài sản bên thứ ba) |
| 7 | Bảo vệ nhãn hiệu (Trademarks) | **PASS** (đã thêm disclaimer tại `NOTICE`, `THIRD_PARTY_NOTICES.md`, `README*.md`) |
| 8 | Quét bí mật & lịch sử Git | **PASS** (0 rò rỉ trong working tree và toàn bộ lịch sử) |
| 9 | Bảo mật quyền riêng tư (PII) | **PASS** (`RootShell.sanitizeDiagnostics` + `SanitizerTest` kiểm chứng) |
| 10 | Bảo mật CI/CD & GitHub Ruleset | **PASS** (`permissions: contents: read`, SHA pinning bắt buộc cấp repo, Ruleset `#27848863` trên `main`) |
| 11 | Ghi nhận AI trung thực (Phase 6) | **PASS** (`ANTIGRAVITY_IDENTITY_UNVERIFIED` ghi rõ, không lạm dụng bot giả) |
| 12 | Chuẩn Conventional Commits & `.gitattributes` | **PASS** (commit chia nhóm chuẩn, `build.sh` & `verify_apk.py` có `+x`) |
| 13 | Cấu trúc repository & Tài liệu | **PASS** (đầy đủ tiếng Việt + tiếng Anh, kiến trúc, cài đặt, khôi phục, bảo mật) |
| 14 | Build & Unit Test xác minh được | **PASS (HOST & CI)** / **NOT_VERIFIED_ON_DEVICE** (máy test `SM-S901B` đã bán) |
| 15 | Changelog, SBOM & Tag phát hành | **PASS** (`v1.0.1-s22-a12` chuẩn bị đầy đủ APK, `.sha256` và `SBOM.cdx.json`) |

### Kết luận tổng thể: **`CONDITIONAL GO`**
- **Phê duyệt phát hành ở mức `Pre-release / Experimental` (`v1.0.1-s22-a12`, `GNU GPL v3.0`)**.
- **Điều kiện đi kèm (không tự nhận mức Stable):**
  1. Giữ nguyên nhãn **`Pre-release / Experimental`** và ghi rõ `v1.0.1-s22-a12` chưa được tái kiểm thử trực tiếp trên máy thật do máy `SM-S901B` đã bán (chỉ kế thừa kết quả test `75Hz` trước đây và xác minh build/unit test trên host + CI).
  2. APK phát hành sử dụng khóa debug nội bộ (`debug.keystore`) kèm `SHA-256` để đối chiếu.
  3. Tiếp tục duy trì Ruleset `#27848863` (`Protect main branch`) và `sha_pinning_required: true` đã kích hoạt.
