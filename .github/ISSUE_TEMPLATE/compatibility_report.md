---
name: Báo cáo Tương thích Phần cứng (Compatibility Report)
about: Chia sẻ kết quả test thực tế với màn hình rời, cáp/hub USB-C hoặc biến thể ROM/S22
title: "[Compat] <Model màn hình / Cáp hoặc Hub> @ <Độ phân giải & Hz>"
labels: compatibility
assignees: ''
---

> [!NOTE]
> Chỉ gửi kết quả bạn đã **chạy thực tế** kèm bằng chứng (`OSD màn hình`, `dumpsys display`, `dmesg` hoặc `TestUFO`). Không gửi suy đoán.

## 1. Thiết bị & Hệ điều hành
- **Model máy:** (ví dụ: `SM-S901B`)
- **Android / One UI:** (ví dụ: `Android 12 / One UI 4.1`)
- **Root / LSPosed:**

## 2. Màn hình & Cáp kết nối
- **Tên màn hình (theo OSD / `V2203H`...):**
- **Loại kết nối:** [Cáp USB-C → DP/HDMI trực tiếp (4-lane) / Hub USB 3.0 (2-lane)]
- **Độ phân giải & Hz tối đa màn hình hỗ trợ:**
- **Chế độ Test Mode (`dex120_force_virtual_120`):** [Bật / Tắt]
- **Màn hình điện thoại:** [Mở Màn đen AMOLED / Sáng bình thường / Tắt bằng nút nguồn]

## 3. Kết quả thực tế quan sát được
- **OSD trên màn hình báo:** (ví dụ: `1920x1080 120Hz` hoặc `75Hz`)
- **DeX Desktop trong `dumpsys display`:**
- **TestUFO / app kiểm tra FPS thực tế đạt:**
- **Trạng thái xếp hạng đề xuất:** [VERIFIED / PARTIAL / UNSUPPORTED]

## 4. Bằng chứng / Log đã ẩn danh
```text
(Dán kết quả 'Chạy chẩn đoán' đã xóa thông tin nhạy cảm tại đây)
```
