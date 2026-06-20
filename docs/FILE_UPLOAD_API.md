# File Upload & User Profile/Avatar API — Hướng dẫn tích hợp Frontend

> Cơ chế upload file dùng chung (base) + cập nhật hồ sơ & avatar của user hiện tại.

---

## 1. Tổng quan

- **Base URL:** `http://localhost:8080`
- **Auth:** endpoint upload và `/me` cần header `Authorization: Bearer <accessToken>`.
- **Envelope chung:** `{ "code": 1000, "message": "...", "data": ..., "timestamp": "..." }`.
- **Lưu trữ:** mặc định lưu ở local filesystem (`app.storage.location`, mặc định thư mục `uploads/`).
  File được phục vụ public (chỉ đọc) ở prefix **`/files/**`** — không cần token để xem ảnh.
- **URL trả về:** mặc định là **đường dẫn tương đối** dạng `/files/avatars/abc123.png`.
  FE tự ghép host (`http://localhost:8080` + url). Nếu backend cấu hình `app.storage.base-url` (CDN/host)
  thì URL trả về đã là tuyệt đối.
- **Giới hạn dung lượng:** mặc định **5MB/file**, 10MB/request (cấu hình `spring.servlet.multipart`).
  Vượt quá ⇒ trả `code 4004` (HTTP 413).

---

## 2. Upload file dùng chung

### `POST /api/files`

- **Content-Type:** `multipart/form-data`
- **Form field:** `file` — file cần upload (bất kỳ loại nào).
- **Ví dụ (JS):**
  ```js
  const fd = new FormData();
  fd.append("file", fileInput.files[0]);
  await fetch("/api/files", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` }, // KHÔNG tự set Content-Type, để browser tự thêm boundary
    body: fd,
  });
  ```
- **Response 200:**
  ```json
  {
    "code": 1000,
    "message": "Success",
    "data": {
      "url": "/files/uploads/9f2c8b1e4a7d4c0fb1e2.png",
      "filename": "9f2c8b1e4a7d4c0fb1e2.png",
      "contentType": "image/png",
      "size": 20480
    },
    "timestamp": "2026-06-20T08:00:00Z"
  }
  ```
- **Dùng để:** lấy `url` rồi gắn vào resource bất kỳ (vd gửi kèm khi cập nhật một bản ghi có ảnh).

---

## 3. Hồ sơ user hiện tại (`/me`)

### 3.1 `GET /api/users/me` — Lấy hồ sơ của tôi

- **Response 200:**
  ```json
  {
    "code": 1000, "message": "Success",
    "data": {
      "id": 1, "username": "admin", "email": "admin@springboot.vn",
      "fullName": "Administrator",
      "avatarUrl": "/files/avatars/abc123.png",
      "enabled": true,
      "roles": ["ADMIN"],
      "permissions": ["USER_READ", "USER_WRITE", "..."]
    },
    "timestamp": "..."
  }
  ```

### 3.2 `PUT /api/users/me` — Cập nhật hồ sơ (JSON)

- **Mô tả:** cập nhật các field hồ sơ. Chỉ field **khác null** mới được áp dụng.
- **Request body:**
  ```json
  { "fullName": "Nguyễn Văn A", "avatarUrl": "/files/avatars/abc123.png" }
  ```
- **Validation:** `fullName` ≤ 100 ký tự; `avatarUrl` ≤ 500 ký tự (cả hai optional).
- **Response 200:** trả về `UserResponse` đã cập nhật (cấu trúc như 3.1), `message = "Profile updated"`.

> Luồng đổi avatar kiểu 2 bước: `POST /api/files` lấy `url` → `PUT /api/users/me` với `avatarUrl = url`.

### 3.3 `POST /api/users/me/avatar` — Upload & đặt avatar (1 bước, khuyên dùng)

- **Content-Type:** `multipart/form-data`, field **`file`** = ảnh.
- **Chỉ chấp nhận ảnh:** `image/jpeg`, `image/png`, `image/webp`, `image/gif`. Loại khác ⇒ `code 4003`.
- **Hành vi:** lưu ảnh vào `avatars/`, set `avatarUrl` cho user hiện tại, **tự xoá avatar cũ**.
- **Response 200:** trả về `UserResponse` đã cập nhật (`avatarUrl` mới), `message = "Avatar updated"`.
  ```json
  {
    "code": 1000, "message": "Avatar updated",
    "data": { "id": 1, "username": "admin", "avatarUrl": "/files/avatars/new123.png", "...": "..." },
    "timestamp": "..."
  }
  ```

---

## 4. Bảng mã lỗi

| `code` | HTTP | Khi nào |
|--------|------|---------|
| 1000 | 200 | Thành công |
| 4001 | 400 | Validation failed (vd `fullName` quá dài) |
| 4002 | 400 | File rỗng / không có file |
| 4003 | 400 | Sai định dạng (avatar không phải ảnh hợp lệ) |
| 4004 | 413 | File vượt quá dung lượng cho phép |
| 4010 | 401 | Thiếu/sai access token |
| 9001 | 500 | Lỗi lưu file phía server |

---

## 5. Lưu ý vận hành (cho BE/DevOps)

- Thư mục `uploads/` đã được `.gitignore`. Production nên **mount volume** hoặc trỏ `APP_STORAGE_LOCATION` tới ổ lưu trữ bền vững.
- Muốn chuyển sang S3/MinIO sau này: chỉ cần thêm một bean `StorageService` mới, không phải sửa controller/service gọi nó.
- Đổi `app.storage.public-path` thì nhớ cập nhật danh sách public endpoint trong `SecurityConfig` cho khớp.
