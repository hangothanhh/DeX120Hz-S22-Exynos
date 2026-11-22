# Third-Party Notices & Dependency Inventory

**Ngôn ngữ / Language:** [English](../../THIRD_PARTY_NOTICES.md) | **Tiếng Việt**

Tài liệu này liệt kê toàn bộ dự án tham khảo, giao diện API, công cụ biên dịch, CI Actions và thành phần môi trường chạy liên quan tới **DeX 120Hz S22** (`com.dex120.s22`).

---

## 1. Runtime & Bundled Dependencies inside APK (`out/DeX120Hz-S22.apk`)

- **Bundled third-party Java/native libraries inside `classes.dex` or `lib/`:** **0 (Không có)**.
- Ứng dụng chỉ sử dụng API chuẩn của nền tảng Android (`android.*`, `java.*`) và cơ chế reflection/hook tới khung LSPosed được cung cấp sẵn lúc chạy trên thiết bị.

---

## 2. Clean-Room Compile-Only Stubs (Excluded from `classes.dex`)

| Thành phần | Vị trí trong Repo | Nguồn tham chiếu | Giấy phép hệ sinh thái | Ghi chú đóng gói |
| :--- | :--- | :--- | :--- | :--- |
| Clean-room Xposed API signature stubs (`IXposedHookLoadPackage`, `XC_MethodHook`, `XposedBridge`, `XposedHelpers`, `XC_LoadPackage`) | `stubs/de/robv/android/xposed/` | [LSPosed / Xposed API](https://github.com/LSPosed/LSPosed) | `GPL-3.0-only` (LSPosed) | Chỉ biên dịch vào thư mục tạm trung gian để `javac` nhận chữ ký hàm; bị loại trừ hoàn toàn trước khi tạo `classes.dex` bởi `d8` và được kiểm tra bằng `scripts/verify_apk.py`. |

---

## 3. Design Inspirations & Device Runtime Ecosystem

| Dự án / Hệ sinh thái | Tác giả / Nguồn | Giấy phép | Mức độ liên quan |
| :--- | :--- | :--- | :--- |
| **OnigiriDeX** | [Kinue72/OnigiriDeX](https://github.com/Kinue72/OnigiriDeX) | `GPL-3.0-only` | Tham khảo hướng tiếp cận kỹ thuật (`0005-enable-dex-high-refresh.patch`, `0025-Add-mirroring-high-refresh-rate-toggle.patch`) và cấu trúc README thân thiện; không sao chép trực tiếp file mã nguồn ROM. Dự án này chọn `GNU GPL v3.0` để tương thích đầy đủ. |
| **LSPosed** | [LSPosed/LSPosed](https://github.com/LSPosed/LSPosed) | `GPL-3.0-only` | Framework tiêm hook Xposed trên thiết bị người dùng (không đóng gói trong repo/APK). |
| **KernelSU & ReZygisk** | Cộng đồng KernelSU / Zygisk | `GPL-3.0-only` / Open Source | Môi trường cấp quyền Root và Zygisk được sử dụng khi test thực tế trên `SM-S901B` (không đóng gói trong repo/APK). |

---

## 4. Build Toolchain (Build-Time Only — Not Distributed in Repo/APK)

| Công cụ | Phiên bản sử dụng | Giấy phép | Vai trò |
| :--- | :--- | :--- | :--- |
| Android SDK Platform (`android.jar`) | API 34 (targeting API 31) | `Apache-2.0` (AOSP / Android SDK Terms) | Cung cấp chữ ký lớp hệ thống Android khi biên dịch |
| Android SDK Build-Tools (`aapt2`, `d8`, `zipalign`, `apksigner`) | `34.0.0` | `Apache-2.0` (Android SDK) | Đóng gói tài nguyên, biên dịch DEX, căn chỉnh & ký APK |
| Eclipse Temurin / Microsoft Build of OpenJDK | JDK 17 | `GPL-2.0-with-classpath-exception` | Biên dịch Java (`javac`, `--release 11`) và chạy `SanitizerTest` / `keytool` |
| Python | 3.x (CI: `3.11`) | `PSF-2.0` | Chạy `scripts/verify_apk.py` |

---

## 5. CI/CD GitHub Actions (Pinned by Commit SHA)

| Action | Version / Tag | Pinned Commit SHA | Giấy phép |
| :--- | :--- | :--- | :--- |
| `actions/checkout` | `v7.0.1` | `3d3c42e5aac5ba805825da76410c181273ba90b1` | `MIT` |
| `actions/setup-java` | `v6.3.0` | `d1374f8ac74474200d16ef8ecee22c7ae7a51880` | `MIT` |
| `actions/setup-python` | `v7.1.0` | `6d4fd22b20465ccc0f7354476588e8df7c9ca0e0` | `MIT` |
| `actions/upload-artifact` | `v7.0.2` | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` | `MIT` |
| `github/codeql-action` | `v4` | `1bd86f2fdb4b86187fa4059f16f31a3202acc9ea` | `MIT` |
| `actions/attest-build-provenance` | `v4.2.2` | `4d101475d8b20a2381f78447822ac1eab6504dd8` | `MIT` |

---

## 6. Non-Code Assets

- **0 tài sản phi mã nguồn của bên thứ ba:** Repository không chứa file ảnh (`.png`, `.jpg`, `.webp`), icon bản quyền, font chữ thương mại, âm thanh, video, dataset, mô hình AI hoặc nhị phân/firmware Samsung trích xuất từ máy thật.

---

## 7. Trademark Disclaimer

"Samsung", "Galaxy", "Galaxy S22", "Samsung DeX", "One UI", và "Exynos" là nhãn hiệu đã đăng ký hoặc nhãn hiệu của Samsung Electronics Co., Ltd.
"Android", "Google", và "Antigravity" là nhãn hiệu của Google LLC.
Việc nhắc tên thiết bị và hệ điều hành chỉ nhằm mục đích mô tả khả năng tương thích kỹ thuật.
