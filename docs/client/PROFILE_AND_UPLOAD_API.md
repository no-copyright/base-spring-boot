# Profile, Avatar & File Upload API (CLIENT) — Hướng dẫn tích hợp Frontend

> **Đối tượng:** **Client app** — user tự xem/sửa hồ sơ của **chính mình** (`/me`), đổi avatar, và upload file.
> Envelope response & bảng error code: xem `AUTH_API.md`.

---

## 1. Tổng quan

- **Base URL:** `http://localhost:8080`
- **Auth:** mọi endpoint cần header `Authorization: Bearer <accessToken>`; thao tác trên **user hiện tại**.
- **Lưu trữ file:** mặc định local filesystem (`app.storage.location`, mặc định `uploads/`). File phục vụ public (chỉ đọc) ở prefix **`/files/**`** — không cần token để xem ảnh.
- **URL trả về:** mặc định **đường dẫn tương đối** `/files/avatars/abc123.png` → FE ghép host (`http://localhost:8080` + url). Nếu backend đặt `app.storage.base-url` (CDN) thì URL đã tuyệt đối.
- **Giới hạn dung lượng:** mặc định **5MB/file**, 10MB/request. Vượt ⇒ `code 4004` (HTTP 413).

---

## 2. Upload file dùng chung — `POST /api/files`

- **Content-Type:** `multipart/form-data`; **field `file`** = file cần upload (bất kỳ loại).
- **Ví dụ (JS):**
  ```js
  const fd = new FormData();
  fd.append("file", fileInput.files[0]);
  await fetch("/api/files", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` }, // KHÔNG tự set Content-Type, để browser thêm boundary
    body: fd,
  });
  ```
- **Response 200:**
  ```json
  {
    "code": 1000, "message": "Success",
    "data": {
      "id": 42,
      "url": "/files/misc/9f2c8b1e4a7d4c0fb1e2.png",
      "filename": "photo.png",
      "contentType": "image/png",
      "size": 20480
    },
    "timestamp": "2026-06-20T08:00:00Z"
  }
  ```
- **`id`**: khóa bản ghi trong bảng `files` (registry — backend lưu metadata mọi upload để audit/cleanup). FE tham chiếu `id` **hoặc** `url`.
- **`filename`**: tên gốc (chỉ để hiển thị; tên thật trên đĩa là chuỗi ngẫu nhiên).

---

## 3. Hồ sơ user hiện tại (`/me`)

### 3.1 `GET /api/users/me` — Lấy hồ sơ của tôi

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
> Tương đương `GET /api/auth/me` (xem `AUTH_API.md` §3.5).

### 3.2 `PUT /api/users/me` — Cập nhật hồ sơ (JSON)

- Chỉ field **khác null** mới được áp dụng.
- **Request body:** `{ "fullName": "Nguyễn Văn A", "avatarUrl": "/files/avatars/abc123.png" }`
- **Validation:** `fullName` ≤ 100; `avatarUrl` ≤ 500 (cả hai optional).
- **Response 200:** `UserResponse` đã cập nhật, `message = "Profile updated"`.

> Đổi avatar kiểu 2 bước: `POST /api/files` lấy `url` → `PUT /api/users/me` với `avatarUrl = url`.

### 3.3 `POST /api/users/me/avatar` — Upload & đặt avatar (1 bước, khuyên dùng)

- **Content-Type:** `multipart/form-data`, field **`file`** = ảnh.
- **Chỉ chấp nhận ảnh:** `image/jpeg`, `image/png`, `image/webp`, `image/gif`. Loại khác ⇒ `code 4003`.
- **Hành vi:** lưu ảnh vào `avatars/`, set `avatarUrl` cho user hiện tại, **tự xoá avatar cũ**.
- **Response 200:** `UserResponse` đã cập nhật, `message = "Avatar updated"`.
  ```json
  { "code": 1000, "message": "Avatar updated",
    "data": { "id": 1, "username": "admin", "avatarUrl": "/files/avatars/new123.png", "...": "..." },
    "timestamp": "..." }
  ```

---

## 4. Bảng mã lỗi (phần upload/profile)

| `code` | HTTP | Khi nào |
|--------|------|---------|
| 1000 | 200 | Thành công |
| 4001 | 400 | Validation failed (vd `fullName` quá dài) |
| 4002 | 400 | File rỗng / không có file |
| 4003 | 400 | Sai định dạng (avatar không phải ảnh hợp lệ) |
| 4004 | 413 | File vượt quá dung lượng |
| 4010 | 401 | Thiếu/sai access token |
| 9001 | 500 | Lỗi lưu file phía server |

(Đầy đủ error code: xem `AUTH_API.md` §1.)

---

## 5. Lưu ý vận hành (BE/DevOps)

- Thư mục `uploads/` đã `.gitignore`. Production nên **mount volume** hoặc trỏ `APP_STORAGE_LOCATION` tới ổ bền vững.
- Bảng `files` (registry) lưu metadata mọi upload: `storage_key`, `original_filename`, `content_type`, `size_bytes`, `owner_id` + audit → nền cho job dọn file mồ côi / thống kê dung lượng.
- Tầng: `Controller / UserService → FileService` (lưu vật lý + ghi registry) `→ StorageService` + `FileRepository`.
- Chuyển S3/MinIO: chỉ cần thêm bean `StorageService` mới, không sửa `FileService`/controller.
- Đổi `app.storage.public-path` thì cập nhật public endpoint trong `SecurityConfig` cho khớp.
