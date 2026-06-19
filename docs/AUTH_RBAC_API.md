# Auth / User / Role / Permission API — Hướng dẫn tích hợp Frontend

> Tài liệu base cho FE xây dựng **Admin Platform** (RBAC) dùng chung cho các hệ thống sau này.
> Backend: Spring Boot + Spring Security + JWT (stateless access token + DB-backed refresh token).

---

## 1. Tổng quan mô hình phân quyền (RBAC)

```
User  ──< many-to-many >──  Role  ──< many-to-many >──  Permission
```

- **User** có nhiều **Role**.
- **Role** có nhiều **Permission**.
- Quyền hiệu lực của user = hợp (union) của tất cả permission từ các role mà user sở hữu.
- Backend kiểm tra quyền bằng `@PreAuthorize("hasAuthority('PERMISSION_CODE')")` hoặc `hasRole('ROLE_NAME')`.

### Permission seed sẵn (DataInitializer)

| Permission code | Mô tả |
|-----------------|-------|
| `USER_READ`   | Xem danh sách / chi tiết user |
| `USER_WRITE`  | Tạo / sửa user |
| `USER_DELETE` | Xoá user |
| `ROLE_READ`   | Xem role |
| `ROLE_WRITE`  | Tạo / sửa role |

### Role seed sẵn

| Role    | Permission được gán | Ghi chú |
|---------|---------------------|---------|
| `ADMIN` | Tất cả permission ở trên | Tài khoản admin mặc định: `admin` / `admin123` |
| `USER`  | `USER_READ`         | Role mặc định khi đăng ký mới |

> ⚠️ Đổi mật khẩu admin mặc định trước khi lên production (`app.init.admin-password`).

---

## 2. JWT Access Token — cấu trúc payload sau khi decode

Token là **JWT HS512**. FE có thể `base64url`-decode phần payload (giữa 2 dấu `.`) để đọc claim mà **không cần verify chữ ký** (việc verify do backend làm). Đừng tin token để ra quyết định bảo mật phía server, nhưng **dùng tốt cho render menu/guard phía FE**.

Payload mẫu sau khi decode:

```json
{
  "iss": "spring-boot",
  "sub": "admin",
  "uid": 1,
  "email": "admin@springboot.vn",
  "fullName": "Administrator",
  "roles": ["ADMIN"],
  "permissions": ["ROLE_READ", "ROLE_WRITE", "USER_DELETE", "USER_READ", "USER_WRITE"],
  "authorities": ["ROLE_ADMIN", "USER_READ", "USER_WRITE", "USER_DELETE", "ROLE_READ", "ROLE_WRITE"],
  "iat": 1750300000,
  "exp": 1750303600
}
```

| Claim | Kiểu | Ý nghĩa |
|-------|------|---------|
| `iss` | string | Issuer, mặc định `spring-boot` |
| `sub` | string | Username |
| `uid` | number | ID của user |
| `email` | string | Email |
| `fullName` | string | Họ tên |
| `roles` | string[] | **Danh sách role (đã bỏ prefix `ROLE_`)** — dùng cái này để check role |
| `permissions` | string[] | **Danh sách permission** — dùng cái này để check quyền / render menu |
| `authorities` | string[] | Gộp role (prefix `ROLE_`) + permission — phục vụ Spring Security interop |
| `iat` | number (epoch giây) | Thời điểm phát hành |
| `exp` | number (epoch giây) | Thời điểm hết hạn |

> ✅ **Khuyến nghị cho FE:** dùng claim `roles` và `permissions` (đã tách sẵn). Tránh phải tự strip prefix `ROLE_` từ `authorities`.

### Ví dụ decode + guard (TypeScript)

```ts
type JwtPayload = {
  sub: string;
  uid: number;
  email: string;
  fullName: string;
  roles: string[];
  permissions: string[];
  exp: number;
};

function decodeJwt(token: string): JwtPayload {
  const payload = token.split('.')[1];
  const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(json);
}

const claims = decodeJwt(accessToken);

const hasPermission = (p: string) => claims.permissions.includes(p);
const hasRole = (r: string) => claims.roles.includes(r);
const isExpired = () => Date.now() >= claims.exp * 1000;

// Render menu "Quản lý user" chỉ khi có quyền đọc user
if (hasPermission('USER_READ')) { /* show menu */ }
```

---

## 3. Quy ước response chung

Mọi response (thành công lẫn lỗi) đều bọc trong envelope:

```json
{
  "code": 1000,
  "message": "Success",
  "data": { },
  "timestamp": "2026-06-19T10:00:00Z"
}
```

| Field | Ý nghĩa |
|-------|---------|
| `code` | Mã nghiệp vụ. **`1000` = thành công.** Khác 1000 = lỗi (xem bảng error code). |
| `message` | Thông điệp |
| `data` | Payload (có thể `null`) |
| `timestamp` | Thời điểm phản hồi (ISO-8601) |

### Bảng Error Code

| code | HTTP | Ý nghĩa |
|------|------|---------|
| 1000 | 200 | Thành công |
| 4000 | 400 | Request không hợp lệ |
| 4001 | 400 | Lỗi validation (xem `data` để biết field lỗi) |
| 4010 | 401 | Chưa xác thực / thiếu token |
| 4011 | 401 | Sai username hoặc mật khẩu |
| 4012 | 401 | Token không hợp lệ / sai định dạng |
| 4013 | 401 | Token hết hạn |
| 4014 | 401 | Tài khoản bị vô hiệu hoá |
| 4030 | 403 | Không đủ quyền truy cập resource |
| 4040 | 404 | Không tìm thấy user |
| 4041 | 404 | Không tìm thấy role |
| 4042 | 404 | Không tìm thấy resource |
| 4043 | 404 | Không tìm thấy refresh token |
| 4090 | 409 | Username đã tồn tại |
| 4091 | 409 | Email đã tồn tại |
| 4092 | 401 | Refresh token đã bị thu hồi |
| 4093 | 401 | Refresh token đã hết hạn |
| 9000 | 500 | Gửi email thất bại |
| 9999 | 500 | Lỗi không xác định |

---

## 4. Authentication APIs

Base path: `/api/auth`
Header cho endpoint cần xác thực: `Authorization: Bearer <accessToken>`

### 4.1. Đăng ký — `POST /api/auth/register`

**Public.** Tạo user mới với role mặc định `USER`.

Request:
```json
{
  "username": "john",        // bắt buộc, 3-50 ký tự
  "email": "john@mail.com",  // bắt buộc, email hợp lệ, <=100
  "password": "secret123",   // bắt buộc, 6-100 ký tự
  "fullName": "John Doe"      // tuỳ chọn, <=100
}
```

Response `data` (`UserResponse`):
```json
{
  "id": 5,
  "username": "john",
  "email": "john@mail.com",
  "fullName": "John Doe",
  "enabled": true,
  "roles": ["USER"],
  "permissions": ["USER_READ"]
}
```
Lỗi có thể: `4090` username tồn tại, `4091` email tồn tại, `4001` validation.

---

### 4.2. Đăng nhập — `POST /api/auth/login`

**Public.** `username` nhận username **hoặc** email.

Request:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response `data` (`AuthResponse`):
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@springboot.vn",
    "fullName": "Administrator",
    "enabled": true,
    "roles": ["ADMIN"],
    "permissions": ["USER_READ", "USER_WRITE", "USER_DELETE", "ROLE_READ", "ROLE_WRITE"]
  }
}
```

| Field | Ý nghĩa |
|-------|---------|
| `accessToken` | JWT, đính kèm header `Authorization` cho các request sau |
| `refreshToken` | UUID, dùng để xin cặp token mới |
| `expiresIn` | Số **giây** access token còn sống (mặc định 3600 = 1h) |
| `user` | Thông tin user kèm roles & permissions |

Lỗi: `4011` sai thông tin đăng nhập, `4014` tài khoản bị khoá.

---

### 4.3. Làm mới token — `POST /api/auth/refresh`

**Public.** Refresh token **xoay vòng** (rotation): token cũ bị thu hồi, trả về cặp `accessToken` + `refreshToken` mới.

Request:
```json
{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }
```
Response: giống `AuthResponse` ở mục 4.2.

Lỗi: `4043` không tìm thấy, `4092` đã thu hồi, `4093` hết hạn.

> 💡 FE flow gợi ý: khi gọi API gặp `4013` (access token hết hạn) → tự động gọi `/refresh` → lưu token mới → retry request. Nếu refresh cũng lỗi (`4092/4093`) → đẩy user về màn login.

---

### 4.4. Đăng xuất — `POST /api/auth/logout`

**Public** (chỉ cần refresh token). Thu hồi refresh token. Access token (stateless) vẫn còn hiệu lực tới khi `exp` — FE nên xoá token khỏi storage.

Request:
```json
{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }
```
Response: `data = null`, `message = "Logout successful"`.

---

### 4.5. Thông tin user hiện tại — `GET /api/auth/me`

**Yêu cầu xác thực.** Trả về `UserResponse` của user đang đăng nhập (đọc từ token).

Header: `Authorization: Bearer <accessToken>`
Response `data`: `UserResponse` (giống mục 4.1).
Lỗi: `4010` chưa xác thực.

---

## 5. User Management APIs

Base path: `/api/users` — **tất cả yêu cầu xác thực + quyền tương ứng.**

### 5.1. Tìm kiếm / danh sách user — `GET /api/users`

**Quyền: `USER_READ`.** Phân trang + lọc + sắp xếp.

Query params (tất cả tuỳ chọn):

| Param | Mặc định | Ý nghĩa |
|-------|----------|---------|
| `username` | — | Lọc theo username (chứa) |
| `email` | — | Lọc theo email (chứa) |
| `page` | `0` | Trang (0-based) |
| `size` | `10` | Số phần tử mỗi trang |
| `sortBy` | `id` | Trường sắp xếp |
| `sortDirection` | `ASC` | `ASC` / `DESC` |

Ví dụ: `GET /api/users?username=jo&page=0&size=20&sortBy=username&sortDirection=ASC`

Response `data` (`PageResponse<UserResponse>`):
```json
{
  "content": [
    { "id": 1, "username": "admin", "email": "admin@springboot.vn",
      "fullName": "Administrator", "enabled": true,
      "roles": ["ADMIN"], "permissions": ["USER_READ", "..."] }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```
Lỗi: `4010` chưa xác thực, `4030` thiếu quyền `USER_READ`.

---

### 5.2. Chi tiết user — `GET /api/users/{id}`

**Quyền: `USER_READ`.**
Response `data`: `UserResponse`.
Lỗi: `4040` không tìm thấy, `4030` thiếu quyền.

---

## 6. Các DTO dùng chung

### UserResponse
```ts
interface UserResponse {
  id: number;
  username: string;
  email: string;
  fullName: string;
  enabled: boolean;
  roles: string[];        // ví dụ ["ADMIN"]
  permissions: string[];  // ví dụ ["USER_READ", "USER_WRITE"]
}
```

### AuthResponse
```ts
interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;      // giây
  user: UserResponse;
}
```

### PageResponse<T>
```ts
interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
```

---

## 7. Bảng tổng hợp tất cả endpoint

| Method | Path | Auth | Quyền yêu cầu | Mô tả |
|--------|------|------|---------------|-------|
| POST | `/api/auth/register` | Public | — | Đăng ký user mới (role `USER`) |
| POST | `/api/auth/login` | Public | — | Đăng nhập, lấy token |
| POST | `/api/auth/refresh` | Public | — | Làm mới token (rotation) |
| POST | `/api/auth/logout` | Public | — | Thu hồi refresh token |
| GET  | `/api/auth/me` | Bearer | đã đăng nhập | Thông tin user hiện tại |
| GET  | `/api/users` | Bearer | `USER_READ` | Danh sách / tìm kiếm user (phân trang) |
| GET  | `/api/users/{id}` | Bearer | `USER_READ` | Chi tiết user |

> 📌 **Lưu ý cho FE base admin:** Backend hiện mới expose phần *đọc* user và toàn bộ luồng auth. Các CRUD cho **tạo/sửa/xoá user**, **quản lý role**, **quản lý permission** (tương ứng các permission `USER_WRITE`, `USER_DELETE`, `ROLE_READ`, `ROLE_WRITE` đã seed sẵn) **chưa có controller** — FE nên thiết kế UI dựa trên ma trận permission ở Mục 1 và phối hợp backend bổ sung endpoint khi cần. Permission đã có sẵn trong token nên việc ẩn/hiện menu có thể làm ngay phía FE.

---

## 8. Cấu hình liên quan (application.yaml)

| Key | Mặc định | Ý nghĩa |
|-----|----------|---------|
| `app.jwt.secret` | (env `APP_JWT_SECRET`) | Khoá ký HS512, base64 |
| `app.jwt.issuer` | `spring-boot` | Claim `iss` |
| `app.jwt.access-token-expiration` | `3600000` (ms) | Tuổi access token = 1h |
| `app.jwt.refresh-token-expiration` | `604800000` (ms) | Tuổi refresh token = 7 ngày |
| `app.init.enabled` | `true` | Seed role/permission/admin lúc khởi động |

CORS: cho phép mọi origin (`allowedOriginPatterns: *`), method `GET/POST/PUT/PATCH/DELETE/OPTIONS`, `allowCredentials: true`.

Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs` (public).
