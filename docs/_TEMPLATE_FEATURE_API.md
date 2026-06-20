# <Tên chức năng> API — Hướng dẫn tích hợp Frontend

> Template bàn giao API cho FE. Copy file này thành `docs/<FEATURE>_API.md`, điền đầy đủ.
> Yêu cầu: viết **đủ chi tiết để FE tự call được mà không cần hỏi lại BE**.

---

## 1. Tổng quan

- Mô tả ngắn chức năng làm gì, dành cho đối tượng nào.
- **Base URL:** `http://localhost:8080`
- **Auth:** Hầu hết endpoint cần header `Authorization: Bearer <accessToken>`.
  Quyền yêu cầu (nếu có): `XXX_READ`, `XXX_WRITE`, ...
- **Envelope chung:** mọi response bọc trong:
  ```json
  { "code": 1000, "message": "Success", "data": { }, "timestamp": "2026-06-20T08:00:00Z" }
  ```
  `code = 1000` ⇒ thành công. `code` khác ⇒ lỗi (xem bảng mã lỗi mục cuối).
- **Thời gian:** ISO-8601 UTC (`Instant`), vd `2026-06-20T08:00:00Z`.

---

## 2. Enum / giá trị hợp lệ (nếu có)

| Enum | Giá trị | Ý nghĩa |
|------|---------|---------|
| `ExampleType` | `A`, `B`, `C` | ... |

---

## 3. Danh sách endpoint

### 3.1 `GET /api/<resource>` — Danh sách (phân trang)

- **Mô tả:** ...
- **Quyền:** `XXX_READ`
- **Query params:**

| Param | Kiểu | Bắt buộc | Mặc định | Mô tả |
|-------|------|----------|----------|-------|
| `page` | int | không | 0 | Trang (bắt đầu 0) |
| `size` | int | không | 20 | Số phần tử/trang (tối đa 100) |
| `sortDirection` | string | không | `DESC` | `ASC` / `DESC` |
| `<filter>` | ... | không | | ... |

- **Response 200:**
  ```json
  {
    "code": 1000,
    "message": "Success",
    "data": {
      "content": [ { "id": 1, "...": "..." } ],
      "pageNumber": 0,
      "pageSize": 20,
      "totalElements": 42,
      "totalPages": 3,
      "first": true,
      "last": false
    },
    "timestamp": "2026-06-20T08:00:00Z"
  }
  ```

### 3.2 `POST /api/<resource>` — Tạo mới

- **Quyền:** `XXX_WRITE`
- **Request body:**
  ```json
  { "field1": "value", "field2": 123 }
  ```
- **Validation:** `field1` bắt buộc, ≤ 100 ký tự; `field2` > 0; ...
- **Response 200:**
  ```json
  { "code": 1000, "message": "Success", "data": { "id": 10, "field1": "value" }, "timestamp": "..." }
  ```

### 3.3 `PATCH /api/<resource>/{id}` · `DELETE /api/<resource>/{id}` ...

- Lặp lại cấu trúc trên cho từng endpoint: path/query params, body mẫu, response mẫu.

---

## 4. Bảng mã lỗi có thể trả về

| `code` | HTTP | `message` | Khi nào |
|--------|------|-----------|---------|
| 1000 | 200 | Success | Thành công |
| 4000 | 400 | Invalid request | Body/params sai |
| 4001 | 400 | Validation failed | Vi phạm ràng buộc field |
| 4010 | 401 | Authentication required | Thiếu/invalid token |
| 4030 | 403 | ... | Không đủ quyền |
| 404x | 404 | ... not found | Không tìm thấy resource |
| ...  | ... | ... | ... |

> Bổ sung các `ErrorCode` cụ thể của chức năng này (xem `common/exception/ErrorCode.java`).
