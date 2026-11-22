## 1. Tóm tắt thay đổi
- **Nội dung thay đổi:**
- **Lý do:**
- **Các file bị chỉnh sửa:**

## 2. Khai báo sử dụng AI & Chính sách chống AI Slop (Bắt buộc)
- [ ] Tôi xác nhận đã đọc [CONTRIBUTING.md](../CONTRIBUTING.md) (bao gồm quy định về AI Slop).
- **Mức độ AI hỗ trợ trong PR này:** [ ] Không dùng AI / [ ] AI hỗ trợ một phần, tôi đã đọc và kiểm tra từng dòng / [ ] AI tạo phần lớn code (vui lòng giải thích kỹ bên dưới).
- **Tôi hiểu rõ từng hàm/hook mình sửa làm gì** và không thêm tweak chưa kiểm chứng hoặc thông tin tương thích bịa đặt.

## 3. Kiểm thử đã thực hiện
- [ ] `build.ps1` hoặc `build.sh` build thành công sạch sẽ.
- [ ] `apksigner verify --verbose out/DeX120Hz-S22.apk` đạt `Verifies`.
- [ ] `python scripts/verify_apk.py` (bao gồm `SanitizerTest`) chạy pass 100%.
- [ ] Đã kiểm thử thực tế trên thiết bị thật (nếu sửa runtime/hook):
  - **Thiết bị & ROM:** (ví dụ: `SM-S901B, Android 12 / One UI 4.1`)
  - **Root / LSPosed:**
  - **Màn hình ngoài / Kịch bản test:**

## 4. Ảnh chụp hoặc Log đã ẩn danh (nếu có)
```text
(Đảm bảo đã xóa Serial, IMEI, MAC, IP cá nhân và Token)
```

## 5. Đánh giá Tương thích & Khả năng Rollback
- **Tác động tương thích:** (có đổi scope LSPosed, khóa `Settings.Global` hoặc `minSdk` không?)
- **Cách rollback nếu lỗi:**
