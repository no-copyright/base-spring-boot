# Auth API (Client + Admin dùng chung) — Hướng dẫn tích hợp Frontend

> **Đối tượng:** dùng chung cho **cả Client app lẫn Admin Platform** (đăng nhập/đăng ký/refresh/logout).
> Đây cũng là doc gốc định nghĩa **envelope response**, **bảng error code** và **cấu trúc JWT** mà các doc khác tham chiếu.
> Backend: Spring Boot + Spring Security + JWT (access token stateless + refresh token lưu DB).

---

## 1. Quy ước response chung (mọi API toàn hệ thống)

Mọi response (thành công lẫn lỗi) đều bọc trong envelope:

```json
{
  "code": 1000,
  "message": "Success",
  "data": { },
  "timestamp": "2026-06-20T10:00:00Z"
}
```

| Field | Ý nghĩa |
|-------|---------|
| `code` | Mã nghiệp vụ. **`1000` = thành công.** Khác 1000 = lỗi (xem bảng error code). |
| `message` | Thông điệp |
| `data` | Payload (có thể `null`) |
| `timestamp` | Thời điểm phản hồi (ISO-8601 UTC) |

### Bảng Error Code (toàn hệ thống)

| code | HTTP | Ý nghĩa |
|------|------|---------|
| 1000 | 200 | Thành công |
| 4000 | 400 | Request không hợp lệ |
| 4001 | 400 | Lỗi validation (xem `data` để biết field lỗi) |
| 4002 | 400 | File upload rỗng |
| 4003 | 400 | Sai định dạng file (vd avatar không phải ảnh) |
| 4004 | 413 | File vượt quá dung lượng |
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
| 4044 | 404 | Không tìm thấy notification |
| 4045 | 404 | Không tìm thấy permission |
| 4090 | 409 | Username đã tồn tại |
| 4091 | 409 | Email đã tồn tại |
| 4092 | 401 | Refresh token đã bị thu hồi |
| 4093 | 401 | Refresh token đã hết hạn |
| 4094 | 409 | Tên role đã tồn tại |
| 4095 | 409 | Role built-in không cho sửa/xoá |
| 9000 | 500 | Gửi email thất bại |
| 9001 | 500 | Lỗi lưu file phía server |
| 9999 | 500 | Lỗi không xác định |

---

## 2. JWT Access Token — cấu trúc payload sau khi decode

Token là **JWT HS512**. FE có thể `base64url`-decode phần payload (giữa 2 dấu `.`) để đọc claim mà **không cần verify chữ ký** (việc verify do backend làm). Dùng tốt cho render menu/guard phía FE.

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
| `roles` | string[] | **Role (đã bỏ prefix `ROLE_`)** — dùng để check role |
| `permissions` | string[] | **Permission** — dùng để check quyền / render menu |
| `authorities` | string[] | Gộp role (prefix `ROLE_`) + permission (Spring Security interop) |
| `iat` / `exp` | number (epoch giây) | Phát hành / hết hạn |

> ✅ **Khuyến nghị FE:** dùng claim `roles` & `permissions` (đã tách sẵn), tránh tự strip prefix từ `authorities`.

### Ví dụ decode + guard (TypeScript)

```ts
type JwtPayload = {
  sub: string; uid: number; email: string; fullName: string;
  roles: string[]; permissions: string[]; exp: number;
};

function decodeJwt(token: string): JwtPayload {
  const payload = token.split('.')[1];
  return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
}

const claims = decodeJwt(accessToken);
const hasPermission = (p: string) => claims.permissions.includes(p);
const hasRole = (r: string) => claims.roles.includes(r);
const isExpired = () => Date.now() >= claims.exp * 1000;

if (hasPermission('USER_READ')) { /* show "Quản lý user" menu */ }
```

---

## 3. Authentication APIs

Base path: `/api/auth`. Header cho endpoint cần xác thực: `Authorization: Bearer <accessToken>`.

### 3.1. Đăng ký — `POST /api/auth/register`

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

Response `data` = `UserResponse` (xem §4):
```json
{
  "id": 5, "username": "john", "email": "john@mail.com",
  "fullName": "John Doe", "avatarUrl": null, "enabled": true,
  "roles": ["USER"], "permissions": ["USER_READ"]
}
```
Lỗi: `4090` username tồn tại, `4091` email tồn tại, `4001` validation.

### 3.2. Đăng nhập — `POST /api/auth/login`

**Public.** `username` nhận username **hoặc** email.

Request:
```json
{ "username": "admin", "password": "admin123" }
```

Response `data` = `AuthResponse`:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": 1, "username": "admin", "email": "admin@springboot.vn",
            "fullName": "Administrator", "avatarUrl": null, "enabled": true,
            "roles": ["ADMIN"], "permissions": ["USER_READ", "USER_WRITE", "USER_DELETE", "ROLE_READ", "ROLE_WRITE"] }
}
```

| Field | Ý nghĩa |
|-------|---------|
| `accessToken` | JWT, đính header `Authorization` cho request sau |
| `refreshToken` | UUID, dùng xin cặp token mới |
| `expiresIn` | Số **giây** access token còn sống (mặc định 3600 = 1h) |
| `user` | `UserResponse` kèm roles & permissions |

Lỗi: `4011` sai thông tin đăng nhập, `4014` tài khoản bị khoá.

### 3.3. Làm mới token — `POST /api/auth/refresh`

**Public.** Refresh token **xoay vòng** (rotation): token cũ bị thu hồi, trả về cặp mới.

Request: `{ "refreshToken": "550e8400-..." }` → Response: `AuthResponse` (như 3.2).
Lỗi: `4043` không tìm thấy, `4092` đã thu hồi, `4093` hết hạn.

> 💡 FE flow: gọi API gặp `4013` (access token hết hạn) → tự gọi `/refresh` → lưu token mới → retry. Refresh cũng lỗi (`4092/4093`) → về màn login.

### 3.4. Đăng xuất — `POST /api/auth/logout`

Thu hồi refresh token. Access token (stateless) vẫn sống tới `exp` — FE nên xoá token khỏi storage.

Request: `{ "refreshToken": "550e8400-..." }` → Response: `data = null`, `message = "Logout successful"`.

### 3.5. Thông tin user hiện tại — `GET /api/auth/me`

**Yêu cầu xác thực.** Trả `UserResponse` của user đang đăng nhập (đọc từ token). Lỗi `4010` chưa xác thực.

> Ghi chú: có endpoint tương đương `GET /api/users/me` cho client (xem `../client/PROFILE_AND_UPLOAD_API.md`).

---

## 4. DTO dùng chung

```ts
interface UserResponse {
  id: number;
  username: string;
  email: string;
  fullName: string;
  avatarUrl: string | null;   // URL ảnh đại diện
  enabled: boolean;
  roles: string[];            // ["ADMIN"]
  permissions: string[];      // ["USER_READ", "USER_WRITE"]
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;          // giây
  user: UserResponse;
}

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

## 5. Cấu hình liên quan

| Key | Mặc định | Ý nghĩa |
|-----|----------|---------|
| `app.jwt.secret` | env `APP_JWT_SECRET` | Khoá ký HS512 (base64) |
| `app.jwt.access-token-expiration` | `3600000` ms | Tuổi access token = 1h |
| `app.jwt.refresh-token-expiration` | `604800000` ms | Tuổi refresh token = 7 ngày |

CORS: cho phép mọi origin (`allowedOriginPatterns: *`), method `GET/POST/PUT/PATCH/DELETE/OPTIONS`, `allowCredentials: true`.
Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs` (public).
