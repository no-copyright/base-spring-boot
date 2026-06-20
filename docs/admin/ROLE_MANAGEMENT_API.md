# Role & Permission Management API (ADMIN) — Hướng dẫn tích hợp Frontend

> **Đối tượng:** **Admin Platform** (RBAC). Quản lý role và xem catalog permission.
> Envelope, error code đầy đủ, JWT: xem `../client/AUTH_API.md`. Mô hình RBAC tổng quan + gán role cho user:
> xem `USER_MANAGEMENT_API.md`.

---

## 1. Nguyên tắc (quan trọng)

- **Permission là catalog cố định**, được **seed** và ánh xạ 1:1 với các `@PreAuthorize(...)` trong code.
  → **KHÔNG có API tạo/sửa/xoá permission.** Chỉ có API **đọc** để admin chọn permission gắn vào role.
  (Tạo permission runtime mà code không kiểm tra là vô nghĩa.)
- **Role là động**: admin tạo role mới, gắn tập permission, sửa tập permission của role, xoá role.
- **Role built-in được bảo vệ phía server:**
  - `ADMIN`: **không cho sửa, không cho xoá** (luôn giữ quyền cao nhất).
  - `USER`: **không cho xoá** (role mặc định khi đăng ký) — vẫn cho sửa permission.
  - Vi phạm → `code 4095` (ROLE_PROTECTED).

Quyền: đọc = `ROLE_READ`, ghi (tạo/sửa/xoá) = `ROLE_WRITE`. Header: `Authorization: Bearer <accessToken>`.

---

## 2. Permission (chỉ đọc)

### `GET /api/permissions` — Danh sách permission catalog

**Quyền: `ROLE_READ`.** Dùng để render danh sách chọn khi tạo/sửa role.

```json
{
  "code": 1000, "message": "Success",
  "data": [
    { "id": 1, "name": "USER_READ",   "description": "user read" },
    { "id": 2, "name": "USER_WRITE",  "description": "user write" },
    { "id": 3, "name": "USER_DELETE", "description": "user delete" },
    { "id": 4, "name": "ROLE_READ",   "description": "role read" },
    { "id": 5, "name": "ROLE_WRITE",  "description": "role write" }
  ],
  "timestamp": "..."
}
```

---

## 3. Role (CRUD)

### 3.1 `GET /api/roles` — Danh sách role
**Quyền: `ROLE_READ`.**
```json
{
  "code": 1000, "data": [
    { "id": 1, "name": "ADMIN", "description": "Full system access",
      "permissions": ["USER_READ", "USER_WRITE", "USER_DELETE", "ROLE_READ", "ROLE_WRITE"] },
    { "id": 2, "name": "USER", "description": "Standard user", "permissions": ["USER_READ"] }
  ]
}
```

### 3.2 `GET /api/roles/{id}` — Chi tiết role
**Quyền: `ROLE_READ`.** `data` = `RoleResponse`. Lỗi `4041` không tìm thấy.

### 3.3 `POST /api/roles` — Tạo role + gắn permission
**Quyền: `ROLE_WRITE`.**
- **Request body:**
  ```json
  { "name": "CONTENT_MANAGER", "description": "Quản lý nội dung", "permissionIds": [1, 4] }
  ```
- **Validation:** `name` bắt buộc, ≤ 50; `description` ≤ 255; `permissionIds` optional (rỗng = role không permission).
- **Response 200:** `RoleResponse` vừa tạo, `message = "Role created"`.
- **Lỗi:** `4094` tên role đã tồn tại; `4045` có `permissionId` không tồn tại; `4001` validation.

### 3.4 `PUT /api/roles/{id}` — Sửa role (mô tả + tập permission)
**Quyền: `ROLE_WRITE`.** `name` **không đổi được** (vì backing cho `hasRole(...)`).
- **Request body** (các field optional; chỉ field gửi lên mới áp dụng):
  ```json
  { "description": "Mô tả mới", "permissionIds": [1, 2, 4] }
  ```
  > Gửi `permissionIds` ⇒ **thay thế toàn bộ** tập permission của role. Không gửi ⇒ giữ nguyên.
- **Response 200:** `RoleResponse` đã cập nhật, `message = "Role updated"`.
- **Lỗi:** `4041` không tìm thấy; `4095` role được bảo vệ (vd `ADMIN`); `4045` permissionId không tồn tại.

### 3.5 `DELETE /api/roles/{id}` — Xoá role
**Quyền: `ROLE_WRITE`.** Xoá role; liên kết user↔role và role↔permission tự gỡ (FK ON DELETE CASCADE).
- **Response 200:** `data = null`, `message = "Role deleted"`.
- **Lỗi:** `4041` không tìm thấy; `4095` role built-in (`ADMIN`/`USER`) không cho xoá.

---

## 4. Gắn permission cho role ↔ gán role cho user

- **Gắn permission cho role:** chính là `permissionIds` trong `POST`/`PUT /api/roles` ở trên.
- **Gán role cho user:** `PUT /api/users/{id}/roles` (quyền `USER_WRITE`) — xem `USER_MANAGEMENT_API.md` §2.3.

Luồng RBAC chuẩn cho admin UI:
1. `GET /api/permissions` → hiển thị bảng chọn permission.
2. `POST /api/roles` (hoặc `PUT`) → tạo/sửa role với `permissionIds`.
3. `PUT /api/users/{id}/roles` → gán role cho user.
4. User đăng nhập lại (hoặc refresh token) → JWT chứa `roles`/`permissions` mới.

> ⚠️ Quyền nằm trong access token (JWT). User đang đăng nhập **đổi role** sẽ chỉ có hiệu lực sau khi **token được cấp lại** (login/refresh). FE nên thông báo hoặc buộc refresh khi cần áp ngay.

---

## 5. DTO

```ts
interface PermissionResponse { id: number; name: string; description: string | null; }
interface RoleResponse { id: number; name: string; description: string | null; permissions: string[]; }
```

## 6. Mã lỗi (phần role/permission)

| `code` | HTTP | Khi nào |
|--------|------|---------|
| 4001 | 400 | Validation failed |
| 4030 | 403 | Thiếu quyền `ROLE_READ`/`ROLE_WRITE` |
| 4041 | 404 | Không tìm thấy role |
| 4045 | 404 | Không tìm thấy permission (id sai) |
| 4094 | 409 | Tên role đã tồn tại |
| 4095 | 409 | Role built-in không cho sửa/xoá |
