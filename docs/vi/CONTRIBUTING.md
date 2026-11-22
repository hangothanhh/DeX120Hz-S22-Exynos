# Hướng dẫn Đóng góp (Contributing to DeX 120Hz S22)

**Ngôn ngữ / Language:** [English](../../CONTRIBUTING.md) | **Tiếng Việt**

Cảm ơn bạn đã quan tâm tới **DeX 120Hz S22**! Vì dự án can thiệp sâu vào `system_server` và `com.sec.android.sdhms` bằng quyền Root + LSPosed, mọi đóng góp đều cần sự cẩn trọng và trung thực cao.

---

## Quy định về AI và Chống "AI Slop"

> [!WARNING]
> **Chính sách BẮT BUỘC đối với mọi Pull Request:**
> Bản thân dự án này có **phần lớn code và tài liệu ban đầu do AI hỗ trợ tạo ra**, nên người duy trì hiểu rất rõ điểm mạnh và rủi ro của code do AI sinh ra. Chính vì vậy, dự án áp dụng quy tắc nghiêm ngặt chống **"AI slop"** (mã rác/bịa đặt do AI tạo ra mà người gửi không kiểm tra):

1. **Bắt buộc khai báo trung thực:** Nếu bạn dùng AI hỗ trợ viết code, phân tích disassembly hay soạn tài liệu, phải tick rõ và mô tả mức độ kiểm tra trong Pull Request template.
2. **Không được gửi code chưa tự đọc và hiểu:** Bạn phải giải thích được *vì sao* hook/sửa đổi đó hoạt động trên One UI 4.1 / Android 12, tác động tới phương thức nào, và kịch bản nào khiến nó lỗi.
3. **Không bịa đặt thông tin kỹ thuật hoặc tương thích:**
   - Cấm thêm model máy khác (S22 Snapdragon, S22+, S23...) hay Android 13/14/15 vào danh sách "VERIFIED" nếu bạn chưa chạy thực tế trên máy đó kèm bằng chứng log (`dmesg` / `dumpsys display` / `dumpsys SurfaceFlinger` / OSD).
   - Cấm bịa kết quả benchmark, mã SHA-256 hoặc tên class/method không tồn tại trong firmware thật.
4. **Cấm gửi tweak phản tác dụng / "placebo":** Không thêm prop rác, không tắt bảo vệ nhiệt silicon/CPU/GPU hay giả lập tính năng không có thật.
5. **Bắt buộc chạy build + kiểm thử cục bộ trước khi mở PR.**

Các PR có dấu hiệu copy-paste từ AI hàng loạt, sai tên API thật trên Android 12, chứa code thừa/lặp hoặc chưa build/test sẽ bị đóng thẳng.

---

## Quy tắc an toàn bắt buộc

- **KHÔNG** commit khóa ký APK (`*.keystore`, `*.jks`, `*.pk8`, `*.pem`).
- **KHÔNG** commit thông tin nhạy cảm hoặc log chưa ẩn danh (Serial, IMEI, MAC, IP cá nhân, Token, dữ liệu Play Integrity).
- **KHÔNG** thêm thư viện/file nhị phân bên thứ ba không rõ nguồn gốc hoặc không ghi nhận bản quyền trong [NOTICE](../../NOTICE).
- **KHÔNG** sửa trực tiếp file build sinh ra (`build/`, `out/`); hãy sửa nguồn trong `app/`, `stubs/`, `scripts/`, `build.ps1`, `build.sh`.
- **KHÔNG** xóa copyright notice hay ghi nhận nguồn tham khảo sẵn có.
- Giữ phạm vi sửa đổi gọn gàng, không phá cấu trúc build hiện tại (`aapt2` + `d8` + `apksigner`, `minSdk 31`).

---

## Quy trình đóng góp từng bước

### 1. Fork và Clone
1. Nhấn **Fork** repository `hangothanhh/DeX120Hz-S22-Exynos` về tài khoản GitHub của bạn (nếu bạn có quyền truy cập).
2. Clone bản fork về máy:
   ```bash
   git clone https://github.com/<your-username>/DeX120Hz-S22-Exynos.git
   cd DeX120Hz-S22-Exynos
   ```

### 2. Cài đặt môi trường Build
Xem chi tiết yêu cầu (JDK 17, Android SDK Platform `>= 31`, Build-Tools, Python 3) tại [docs/development.md](development.md).

### 3. Tạo nhánh (Branch) mới
Không commit trực tiếp lên `main`. Hãy tạo nhánh riêng cho từng việc:
```bash
git checkout -b fix/dp-disconnect-delay
```

### 4. Sửa code & kiểm tra chuẩn code
- Tuân thủ `.editorconfig` (UTF-8, 4 khoảng trắng cho Java/Python/XML, LF cho các file không phải `.ps1`).
- Nếu sửa hook trong `app/src/main/java/com/dex120/s22/xposed/`, nhớ kiểm tra:
  - Không làm leak các class stub trong `stubs/de/robv/android/xposed/` vào `classes.dex`.
  - Không để lỗi reflection làm crash `system_server` (luôn bọc `try/catch (Throwable)` hợp lý và log bằng `Config.logError`).
  - Bảo toàn nút bật/tắt runtime qua `Config` (`Settings.Global` `dex120_*`) để người dùng có thể vô hiệu hóa nhanh từng lớp hook nếu cần.

### 5. Build và Chạy Kiểm thử bắt buộc

Trên **Windows (PowerShell)**:
```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
python .\scripts\verify_apk.py
```

Trên **Linux / macOS**:
```bash
bash -n ./build.sh
./build.sh
python3 ./scripts/verify_apk.py
```

Nếu thay đổi ảnh hưởng hành vi runtime (hook DeX, UI, RootShell), bạn **phải** thử cài lên máy thật (hoặc nói rõ phần nào mới kiểm thử ở host JVM và phần nào chưa test trên thiết bị thật).

### 6. Quy ước Commit Message

Sử dụng chuẩn [Conventional Commits](https://www.conventionalcommits.org/) ngắn gọn, rõ nghĩa:

- `feat: add refresh rate toggle for <scenario>` — thêm tính năng mới
- `fix: prevent DFS 30Hz cap when Display 0 sleeps` — sửa lỗi
- `docs: clarify temproot soft reboot steps` — cập nhật tài liệu
- `refactor: extract mode selection helper` — tái cấu trúc không đổi hành vi
- `perf: reduce polling overhead in Config` — tối ưu hiệu năng
- `test: add sanitization test case for serial format` — thêm hoặc sửa unit test
- `ci: validate shell syntax in workflow` — thay đổi GitHub Actions CI
- `chore: update gitignore patterns` — việc bảo trì repo, build script

> [!NOTE]
> **Quy định ghi nhận AI trong commit:** KHÔNG gắn trailer `Co-authored-by` với email chưa được xác minh hoặc trỏ nhầm tới tài khoản/bot không liên quan (như `antigravity-google-bot` hoặc `gemini-code-assist[bot]`). Hãy khai báo trung thực sự hỗ trợ của AI trong mẫu Pull Request và cập nhật [AI_CONTRIBUTIONS.md](AI_CONTRIBUTIONS.md) / [NOTICE](../../NOTICE) khi cần.

### 7. Tạo Pull Request
Điền đầy đủ tất cả các mục trong `.github/PULL_REQUEST_TEMPLATE.md`, bao gồm thiết bị test thật (model, phiên bản Android/One UI, kernel, LSPosed/Root stack), log đã ẩn danh và đánh giá khả năng rollback.
