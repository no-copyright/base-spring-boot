# User Management API (ADMIN) — Hướng dẫn tích hợp Frontend

> **Đối tượng:** **Admin Platform** (RBAC). Các endpoint ở đây yêu cầu **quyền quản trị**.
> Envelope response, bảng error code, cấu trúc JWT & các DTO chung (`UserResponse`, `PageResponse`):
> xem `../client/AUTH_API.md`.

---

## 1. Mô hình phân quyền (RBAC)

```
User  ──< many-to-many >──  Role  ──< many-to-many >──  Permission
```

- **User** có nhiều **Role**; **Role** có nhiều **Permission**.
- Quyền hiệu lực của user = hợp (union) permission từ các role của user.
- Backend kiểm tra bằng `@PreAuthorize("hasAuthority('PERMISSION_CODE')")` hoặc `hasRole('ROLE_NAME')`.
- FE ẩn/hiện menu theo `permissions` trong JWT (xem `../client/AUTH_API.md` §2).

### Permission seed sẵn

| Permission code | Mô tả |
|-----------------|-------|
| `USER_READ`   | Xem danh sách / chi tiết user |
| `USER_WRITE`  | Tạo / sửa user |
| `USER_DELETE` | Xoá user |
| `ROLE_READ`   | Xem role |
| `ROLE_WRITE`  | Tạo / sửa role |

### Role seed sẵn

| Role    | Permission | Ghi chú |
|---------|-----------|---------|
| `ADMIN` | Tất cả ở trên | Tài khoản admin mặc định: `admin` / `admin123` |
| `USER`  | `USER_READ` | Role mặc định khi đăng ký mới |

> ⚠️ Đổi mật khẩu admin mặc định trước khi lên production (`app.init.admin-password`).

---

## 2. User Management APIs

Base path: `/api/users` — **yêu cầu xác thực + quyền tương ứng**. Header: `Authorization: Bearer <accessToken>`.

### 2.1. Tìm kiếm / danh sách user — `GET /api/users`

**Quyền: `USER_READ`.** Phân trang + lọc + sắp xếp.

Query params (tất cả tuỳ chọn):

| Param | Mặc định | Ý nghĩa |
|-------|----------|---------|
| `username` | — | Lọc theo username (chứa) |
| `email` | — | Lọc theo email (chứa) |
| `page` | `0` | Trang (0-based) |
| `size` | `10` | Số phần tử/trang (tối đa 100) |
| `sortBy` | `id` | Trường sắp xếp (`id`,`username`,`email`,`fullName`,`createdAt`) |
| `sortDirection` | `ASC` | `ASC` / `DESC` |

Ví dụ: `GET /api/users?username=jo&page=0&size=20&sortBy=username&sortDirection=ASC`

Response `data` = `PageResponse<UserResponse>`:
```json
{
  "content": [
    { "id": 1, "username": "admin", "email": "admin@springboot.vn",
      "fullName": "Administrator", "avatarUrl": null, "enabled": true,
      "roles": ["ADMIN"], "permissions": ["USER_READ", "..."] }
  ],
  "pageNumber": 0, "pageSize": 20, "totalElements": 1, "totalPages": 1,
  "first": true, "last": true
}
```
Lỗi: `4010` chưa xác thực, `4030` thiếu quyền `USER_READ`.

### 2.2. Chi tiết user — `GET /api/users/{id}`

**Quyền: `USER_READ`.** Response `data` = `UserResponse`. Lỗi: `4040` không tìm thấy, `4030` thiếu quyền.

### 2.3. Gán role cho user — `PUT /api/users/{id}/roles`

**Quyền: `USER_WRITE`.** Thay thế **toàn bộ** tập role của user bằng danh sách gửi lên.
- **Request body:**
  ```json
  { "roleIds": [1, 2] }
  ```
- **Validation:** `roleIds` không rỗng.
- **Response 200:** `UserResponse` đã cập nhật (kèm `roles`/`permissions` mới), `message = "Roles updated"`.
- **Lỗi:** `4040` user không tồn tại; `4041` có `roleId` không tồn tại; `4030` thiếu quyền `USER_WRITE`.

> Quản lý role & gắn permission cho role: xem `ROLE_MANAGEMENT_API.md`.
> ⚠️ Đổi role chỉ áp dụng sau khi user được **cấp lại token** (login/refresh) vì quyền nằm trong JWT.

---

## 3. Bảng tổng hợp endpoint (admin)

| Method | Path | Quyền | Mô tả |
|--------|------|-------|-------|
| GET | `/api/users` | `USER_READ` | Danh sách / tìm kiếm user (phân trang) |
| GET | `/api/users/{id}` | `USER_READ` | Chi tiết user |
| PUT | `/api/users/{id}/roles` | `USER_WRITE` | Gán/đổi role của user |

> ℹ️ **Admin tự xem/sửa hồ sơ của chính mình** dùng các endpoint `/me` (`GET /api/users/me`,
> `PUT /api/users/me`, `POST /api/users/me/avatar`) — mọi user đăng nhập (gồm admin) đều gọi được.
> Mô tả đầy đủ ở `../client/PROFILE_AND_UPLOAD_API.md`. Đăng nhập admin dùng chung luồng auth — xem `../client/AUTH_API.md`.

> 📌 CRUD **tạo/sửa/xoá user** (ứng `USER_WRITE`/`USER_DELETE`) hiện chưa có controller — phối hợp backend bổ sung khi cần.
