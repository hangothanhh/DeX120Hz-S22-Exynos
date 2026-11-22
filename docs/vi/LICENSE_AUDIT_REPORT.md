# License, Copyright & Provenance Audit Report

**Ngôn ngữ / Language:** [English](../../LICENSE_AUDIT_REPORT.md) | **Tiếng Việt**

Báo cáo kiểm toán kỹ thuật về nguồn gốc mã nguồn (Phase 2) và tuân thủ giấy phép mã nguồn mở (Phase 3) cho repository **DeX120Hz-S22-Exynos** (`com.dex120.s22`).

> [!IMPORTANT]
> **Tuyên bố miễn trừ pháp lý (Legal Disclaimer):** Báo cáo này là kết quả kiểm toán kỹ thuật phục vụ quản trị dự án mã nguồn mở, **không phải tư vấn pháp lý chính thức (not formal legal counsel)**. Nếu dự án được sử dụng trong bối cảnh thương mại hoặc phân phối rộng rãi, chủ sở hữu nên tham vấn luật sư chuyên về sở hữu trí tuệ / mã nguồn mở.

---

## 1. Phân loại nguồn gốc mã nguồn (Phase 2 — Source Provenance)

| Đường dẫn / Thành phần | Phân loại | Tác giả / Nguồn gốc | Giấy phép | Tình trạng xác minh |
| :--- | :--- | :--- | :--- | :--- |
| `app/src/main/java/com/dex120/s22/*.java` & `xposed/*.java` | Original + AI-assisted | `hangothanhh` với hỗ trợ của Google Antigravity | `GPL-3.0-only` | Đã kiểm tra: chỉ dùng Java chuẩn + Android API công khai; khái niệm kỹ thuật lấy cảm hứng từ phân tích hành vi `SM-S901B` và kiến trúc patch của `Kinue72/OnigiriDeX` (không sao chép file nguồn) |
| `stubs/de/robv/android/xposed/*.java` | Clean-room API signature stubs | Viết tối giản theo chữ ký API công khai của Xposed/LSPosed | `GPL-3.0-only` (tương thích runtime LSPosed) | Loại trừ hoàn toàn khỏi `classes.dex` khi build; được `scripts/verify_apk.py` xác nhận 0 lớp `de/robv/android/xposed/*` trong APK |
| `app/src/main/AndroidManifest.xml`, `res/`, `assets/xposed_init` | Original | `hangothanhh` | `GPL-3.0-only` | Tự viết cho gói `com.dex120.s22` |
| `build.ps1`, `build.sh`, `scripts/verify_apk.py`, `tests/SanitizerTest.java` | Original + AI-assisted | `hangothanhh` + Google Antigravity | `GPL-3.0-only` | Không nhúng nhị phân bên thứ ba; gọi công cụ Android SDK & JDK từ môi trường host/CI |
| `README*.md`, `docs/*.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md`, `NOTICE`, báo cáo tuân thủ | Original + AI-assisted | `hangothanhh` + Google Antigravity | `GPL-3.0-only` | Minh bạch vai trò AI và thiết bị test thật tại `AI_CONTRIBUTIONS.md` |
| `LICENSE` | Verbatim upstream license text | Free Software Foundation (`GNU GPL v3.0`, 35.149 bytes) | `GPL-3.0-only` | Toàn văn nguyên bản GPL-3.0 |
| `.github/workflows/ci.yml` | Original + GitHub Actions | `hangothanhh` / `actions/*` (`MIT`) | `GPL-3.0-only` (workflow file); Actions dùng `MIT` | Tất cả 4 official actions đều đã ghim commit SHA |

---

## 2. Xác minh tương thích giấy phép (Phase 3 — License Compatibility)

1. **Giấy phép dự án:** **`GNU General Public License v3.0` (`GPL-3.0-only`)**, khớp với metadata trên GitHub (`licenseInfo: gpl-3.0`).
2. **Xung đột lịch sử trước đó (ĐÃ XỬ LÝ):**
   - Bản nháp đầu dùng giấy phép `MIT` trong khi thiết kế tham khảo `Kinue72/OnigiriDeX` (`GPL-3.0`) và chạy trong hệ sinh thái Xposed/LSPosed (`GPL-3.0`) + KernelSU (`GPL-3.0`).
   - Chủ sở hữu (`hangothanhh`) đã chủ động quyết định chuyển toàn bộ repository sang `GNU GPL v3.0` (commit `ba065a0`, nguyên gốc `8509725`), loại bỏ nguy cơ bất tương thích giấy phép về sau.
3. **Mốc lịch sử `v1.0.0-s22-a12` (`b10d6e9`):**
   - Tag/Pre-release `v1.0.0-s22-a12` được tạo tại commit `b10d6e9` khi repository còn đang mang file `LICENSE` bản `MIT`.
   - Theo quyết định của chủ sở hữu và nguyên tắc bảo toàn lịch sử Git (không xóa tag hay viết lại commit phát hành đã công bố), tag `v1.0.0-s22-a12` được giữ nguyên như một snapshot lịch sử, đã bổ sung banner **`SUPERSEDED HISTORICAL SNAPSHOT`** ngay đầu trang Release hướng dẫn người dùng tải `v1.0.1-s22-a12` (`GPL-3.0`), và được đánh dấu `Superseded` trong `SECURITY.md`.
4. **Header SPDX trong mã nguồn & Điều khoản đóng góp:** Toàn bộ `18` tệp `.java` (`app/`, `stubs/`, `tests/`) và `3` script (`build.ps1`, `build.sh`, `scripts/verify_apk.py`) đều đã gắn header `SPDX-License-Identifier: GPL-3.0-only` (được CI kiểm tra tự động), và `CONTRIBUTING.md` quy định rõ nguyên tắc **Inbound = Outbound (`GPL-3.0-only`)**.
5. **Dependency đóng gói bên trong APK:** **0 thư viện bên thứ ba** (`dependencies: []` trong `SBOM.cdx.json`). Không có vấn đề static/dynamic linking với mã độc quyền hay thư viện trái phép.
6. **Thử nghiệm & Xác minh biên dịch:** `scripts/verify_apk.py` xác nhận APK đầu ra chỉ chứa `com/dex120/s22/` trong `classes.dex` và chứa đầy đủ `AndroidManifest.xml`, `assets/xposed_init`, `resources.arsc`, `META-INF/*`.

---

## 3. Kiểm tra tài sản phi mã nguồn & Nhãn hiệu

- **Non-code assets:** **0** ảnh, icon ngoài, font thương mại, video, audio, dataset hay nhị phân trích xuất từ ROM/firmware Samsung được lưu trong repository.
- **Trademarks:** Tên sản phẩm `Samsung`, `Galaxy S22`, `Samsung DeX`, `One UI`, `Exynos`, `Android`, `Google`, `Antigravity` chỉ xuất hiện nhằm mô tả phạm vi kỹ thuật; đã có tuyên bố miễn trừ liên kết (Trademark Disclaimer) tại `NOTICE`, `THIRD_PARTY_NOTICES.md`, `README.md` và `README.en.md`.

---

## 4. Kết luận Phase 2–3

- `SOURCE_UNKNOWN`: **Không có (0 thành phần)**.
- `OWNERSHIP_UNVERIFIED`: **Không có thành phần mã nguồn bên thứ ba chưa rõ quyền**; riêng định danh Git của AI mang nhãn **`ANTIGRAVITY_IDENTITY_UNVERIFIED`** và đã được xử lý minh bạch bằng cách không dùng `Co-authored-by` trên Git mà ghi nhận tại `AI_CONTRIBUTIONS.md`.
- `LICENSE_COMPATIBILITY`: **PASS** dưới `GNU GPL v3.0` (`GPL-3.0-only`).
