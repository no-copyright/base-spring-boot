<div align="center">

# 🚀 Spring Boot Starter — JWT Auth & RBAC

**A production-ready Spring Boot base with JWT authentication, refresh-token rotation, and role/permission-based authorization.**

**Một bộ khung Spring Boot sẵn sàng cho production với xác thực JWT, xoay vòng refresh-token và phân quyền theo vai trò/quyền hạn.**

<br/>

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-Database-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

</div>

---

> 🌐 **Bilingual / Song ngữ** — Each section is written in **English** first, then **Tiếng Việt** (🇻🇳).

---

## 📑 Table of Contents / Mục lục

- [Overview / Tổng quan](#-overview--tổng-quan)
- [Tech Stack / Công nghệ sử dụng](#-tech-stack--công-nghệ-sử-dụng)
- [Features / Tính năng](#-features--tính-năng)
- [Architecture / Kiến trúc](#-architecture--kiến-trúc)
- [Project Structure / Cấu trúc dự án](#-project-structure--cấu-trúc-dự-án)
- [Getting Started / Bắt đầu](#-getting-started--bắt-đầu)
- [Configuration / Cấu hình](#-configuration--cấu-hình)
- [API Reference / Tài liệu API](#-api-reference--tài-liệu-api)
- [Security Model / Mô hình bảo mật](#-security-model--mô-hình-bảo-mật)
- [Response Format / Định dạng phản hồi](#-response-format--định-dạng-phản-hồi)
- [License / Giấy phép](#-license--giấy-phép)

---

## 🧭 Overview / Tổng quan

**EN** — This project is a clean, layered Spring Boot starter that you can fork to bootstrap any REST API quickly. It ships with a complete authentication flow (register / login / refresh / logout / current-user), a **role-based access control (RBAC)** model with fine-grained permissions, JPA auditing, a unified JSON response envelope, and centralized exception handling. The codebase follows **SOLID** principles: every service is coded against an interface, responsibilities are cleanly separated, and cross-cutting concerns (security, errors, auditing) live in their own packages.

**VI** — Đây là một bộ khung Spring Boot phân tầng, sạch sẽ, bạn có thể fork để khởi tạo nhanh bất kỳ REST API nào. Dự án đi kèm luồng xác thực hoàn chỉnh (đăng ký / đăng nhập / làm mới token / đăng xuất / lấy thông tin người dùng hiện tại), mô hình **phân quyền theo vai trò (RBAC)** với quyền hạn chi tiết, JPA auditing, một lớp vỏ phản hồi JSON thống nhất và xử lý ngoại lệ tập trung. Mã nguồn tuân thủ nguyên tắc **SOLID**: mọi service đều lập trình theo interface, trách nhiệm được tách bạch rõ ràng, và các mối quan tâm xuyên suốt (bảo mật, lỗi, audit) được đặt trong các package riêng.

---

## 🛠 Tech Stack / Công nghệ sử dụng

| Layer / Tầng | Technology / Công nghệ | Version | Purpose / Mục đích |
|---|---|---|---|
| **Language** | Java | 21 (LTS) | Ngôn ngữ chính, dùng record/pattern-matching hiện đại |
| **Framework** | Spring Boot | 3.5.10 | Nền tảng auto-configuration & dependency injection |
| **Web** | Spring Web (MVC) | — | REST controllers, JSON serialization |
| **Persistence** | Spring Data JPA + Hibernate | — | ORM, repository abstraction, Specification API |
| **Database** | MariaDB | — | CSDL quan hệ (driver `mariadb-java-client`) |
| **Security** | Spring Security | — | Xác thực, phân quyền, method-level security |
| **Token** | JJWT (io.jsonwebtoken) | 0.12.6 | Phát hành & xác thực JWT (HS512) |
| **Validation** | Jakarta Bean Validation | — | Kiểm tra dữ liệu đầu vào (`@Valid`) |
| **Mail** | Spring Boot Starter Mail | — | Gửi email giao dịch (`EmailService`) |
| **Boilerplate** | Lombok | — | Giảm code lặp (getter/setter/builder) |
| **Build** | Maven (`mvnw` wrapper) | 3.9.x | Quản lý phụ thuộc & đóng gói |
| **Test** | JUnit 5 + Spring Security Test | — | Kiểm thử tích hợp |

---

## ✨ Features / Tính năng

**EN**

- 🔐 **JWT Authentication** — stateless access tokens (HS512), no server-side session.
- 🔄 **Refresh-token rotation** — DB-backed, opaque refresh tokens that support real logout and revocation; each refresh replaces the previous token.
- 👥 **RBAC with permissions** — users → roles → permissions; authorize with `hasRole(...)` *and* `hasAuthority(PERMISSION)`.
- 🧾 **Unified API envelope** — every response (success or error) has the same `{ code, message, data, timestamp }` shape.
- 🧯 **Global exception handling** — one `@RestControllerAdvice` maps every exception to a stable business error code.
- 🕵️ **JPA auditing** — `createdAt / updatedAt / createdBy / updatedBy` populated automatically.
- 🔎 **Dynamic search & pagination** — JPA Specification with whitelisted, safe sorting.
- 🌱 **Idempotent data seeding** — bootstrap default permissions, `ADMIN`/`USER` roles and an admin account on startup.
- ✉️ **Email service** — pluggable behind an interface (DIP).
- 🌍 **CORS configured** & login by **username or email**.

**VI**

- 🔐 **Xác thực JWT** — access token stateless (HS512), không lưu session phía server.
- 🔄 **Xoay vòng refresh-token** — refresh token dạng opaque lưu trong DB, cho phép đăng xuất & thu hồi thật; mỗi lần làm mới sẽ thay token cũ.
- 👥 **RBAC theo quyền hạn** — người dùng → vai trò → quyền; kiểm tra bằng `hasRole(...)` *và* `hasAuthority(PERMISSION)`.
- 🧾 **Vỏ API thống nhất** — mọi phản hồi (thành công hay lỗi) đều cùng cấu trúc `{ code, message, data, timestamp }`.
- 🧯 **Xử lý ngoại lệ tập trung** — một `@RestControllerAdvice` ánh xạ mọi ngoại lệ về mã lỗi nghiệp vụ ổn định.
- 🕵️ **JPA auditing** — tự động điền `createdAt / updatedAt / createdBy / updatedBy`.
- 🔎 **Tìm kiếm động & phân trang** — JPA Specification kèm sắp xếp an toàn (whitelist).
- 🌱 **Seed dữ liệu idempotent** — tạo sẵn quyền mặc định, vai trò `ADMIN`/`USER` và tài khoản admin khi khởi động.
- ✉️ **Dịch vụ email** — ẩn sau interface để dễ thay thế (DIP).
- 🌍 **Đã cấu hình CORS** & đăng nhập bằng **username hoặc email**.

---

## 🏗 Architecture / Kiến trúc

**EN** — Classic layered architecture with a one-way dependency flow. The web layer never touches persistence directly; services depend on **interfaces**, not implementations.

**VI** — Kiến trúc phân tầng cổ điển với luồng phụ thuộc một chiều. Tầng web không bao giờ chạm trực tiếp vào tầng lưu trữ; service phụ thuộc vào **interface**, không phụ thuộc implementation.

```text
HTTP Request
     │
     ▼
┌─────────────┐   @Valid DTO     ┌──────────────┐   Interface     ┌──────────────┐
│ Controller  │ ───────────────▶ │   Service    │ ──────────────▶ │  Repository  │
│  (REST API) │                  │ (impl)       │                 │ (Spring Data)│
└─────────────┘                  └──────────────┘                 └──────────────┘
     ▲                                  │                                 │
     │ ApiResponse<T>                   │ Mapper / Specification          ▼
     │                                  ▼                          ┌──────────────┐
┌─────────────────────┐         ┌──────────────┐                  │   MariaDB    │
│ GlobalExceptionHandler│◀──────│  AppException │                  └──────────────┘
└─────────────────────┘         └──────────────┘
            ▲
            │  Security filter chain (JWT)  ──▶  EntryPoint / AccessDeniedHandler
```

---

## 📂 Project Structure / Cấu trúc dự án

```text
src/main/java/vn/springboot
├── Application.java                  # Entry point / Điểm khởi động
├── common
│   ├── entity/BaseEntity.java        # Audit fields (id, created/updated by/at)
│   ├── exception/                    # ErrorCode, AppException, GlobalExceptionHandler
│   └── response/ApiResponse.java     # Unified response envelope / Vỏ phản hồi chung
├── config
│   ├── DataInitializer.java          # Seed roles/permissions/admin
│   └── JpaAuditingConfig.java        # AuditorAware (current principal)
├── controller                        # AuthController, UserController
├── dto
│   ├── request/                      # Login/Register/RefreshToken/UserSearch
│   └── response/                     # Auth/User/Page responses
├── entity/user                       # User, Role, Permission, RefreshToken
├── mapper/UserMapper.java            # Entity → DTO
├── repository                        # Spring Data repositories + Specification
├── security
│   ├── SecurityConfig.java           # Filter chain, CORS, password encoder
│   ├── jwt/                          # JwtService, JwtAuthenticationFilter
│   ├── CustomUserDetails(Service)    # UserEntity → Spring Security UserDetails
│   └── *Handler / *EntryPoint        # Unified 401/403 JSON responses
└── service                           # AuthService, UserService, EmailService (+ impl)
```

---

## 🚦 Getting Started / Bắt đầu

### 1. Prerequisites / Yêu cầu

- **JDK 21+**
- **MariaDB** (or MySQL) running locally / đang chạy cục bộ
- No need to install Maven — use the bundled wrapper `./mvnw` / Không cần cài Maven — dùng wrapper `./mvnw` có sẵn

### 2. Create the database / Tạo CSDL

```sql
CREATE DATABASE dev_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Hibernate `ddl-auto: update` will create the tables automatically on first run.
> Hibernate `ddl-auto: update` sẽ tự tạo bảng ở lần chạy đầu tiên.

### 3. Run / Chạy ứng dụng

```bash
# Development / Phát triển
./mvnw spring-boot:run

# Build a runnable jar / Đóng gói jar chạy được
./mvnw clean package
java -jar target/spring-boot-0.0.1-SNAPSHOT.jar
```

The app starts on **`http://localhost:8080`**. / Ứng dụng khởi động tại **`http://localhost:8080`**.

### 4. Default admin account / Tài khoản admin mặc định

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Email | `admin@springboot.vn` |

> ⚠️ **Change this password before deploying!** / **Hãy đổi mật khẩu này trước khi triển khai!**

---

## ⚙️ Configuration / Cấu hình

**EN** — All settings live in `src/main/resources/application.yaml` and can be overridden by environment variables.
**VI** — Mọi cấu hình nằm trong `src/main/resources/application.yaml` và có thể ghi đè bằng biến môi trường.

| Env variable / Biến môi trường | Default / Mặc định | Description / Mô tả |
|---|---|---|
| `DB_URL` | `jdbc:mariadb://localhost:3306/dev_db` | JDBC connection string |
| `DB_USERNAME` | `root` | Database user |
| `DB_PASSWORD` | `root` | Database password |
| `APP_JWT_SECRET` | *(dev default)* | Base64-encoded 512-bit signing key — **must override in production** |
| `app.jwt.access-token-expiration` | `3600000` | Access token lifetime (ms) — 1h |
| `app.jwt.refresh-token-expiration` | `604800000` | Refresh token lifetime (ms) — 7d |
| `app.mail.from` | `no-reply@springboot.vn` | "From" address for outgoing mail |
| `app.init.enabled` | `true` | Toggle startup data seeding |
| `app.init.admin-username` / `-email` / `-password` | `admin` / … | Seeded admin credentials |

> 🔒 **Production tip / Lưu ý production:** generate a fresh JWT secret and pass it via `APP_JWT_SECRET`; never commit real secrets. / Hãy sinh JWT secret mới và truyền qua `APP_JWT_SECRET`; đừng commit secret thật.

---

## 🔌 API Reference / Tài liệu API

Base path: **`/api`**

### 🔑 Authentication / Xác thực — `/api/auth`

| Method | Endpoint | Auth | Description / Mô tả |
|---|---|:---:|---|
| `POST` | `/register` | ❌ | Register a new user / Đăng ký người dùng mới |
| `POST` | `/login` | ❌ | Login (username **or** email) → access + refresh token |
| `POST` | `/refresh` | ❌ | Rotate refresh token → new token pair / Làm mới & xoay token |
| `POST` | `/logout` | ✅ | Revoke a refresh token / Thu hồi refresh token |
| `GET` | `/me` | ✅ | Get the current authenticated user / Lấy người dùng hiện tại |

### 👤 Users / Người dùng — `/api/users`

| Method | Endpoint | Permission | Description / Mô tả |
|---|---|:---:|---|
| `GET` | `/` | `USER_READ` | Search + paginate users / Tìm kiếm & phân trang |
| `GET` | `/{id}` | `USER_READ` | Get a user by id / Lấy người dùng theo id |

### 📥 Example requests / Ví dụ

**Register / Đăng ký**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@mail.com","password":"secret123","fullName":"John Doe"}'
```

**Login / Đăng nhập**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**Call a protected endpoint / Gọi endpoint cần xác thực**
```bash
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## 🛡 Security Model / Mô hình bảo mật

**EN**

1. **Login** validates credentials via `AuthenticationManager` + `BCryptPasswordEncoder`.
2. The server issues a short-lived **JWT access token** (HS512) and a long-lived, DB-stored **refresh token**.
3. Each request passes through `JwtAuthenticationFilter`, which validates the token and populates the `SecurityContext`.
4. **Authorization** is enforced with `@PreAuthorize("hasAuthority('USER_READ')")` — permissions are derived from the user's roles.
5. Auth failures return a consistent JSON body: **401** (`JwtAuthenticationEntryPoint`) or **403** (`CustomAccessDeniedHandler`).

**VI**

1. **Đăng nhập** xác thực thông tin qua `AuthenticationManager` + `BCryptPasswordEncoder`.
2. Server phát hành **JWT access token** (HS512) thời gian ngắn và **refresh token** thời gian dài lưu trong DB.
3. Mỗi request đi qua `JwtAuthenticationFilter` để xác thực token và nạp `SecurityContext`.
4. **Phân quyền** thực thi bằng `@PreAuthorize("hasAuthority('USER_READ')")` — quyền được suy ra từ vai trò của người dùng.
5. Khi xác thực thất bại, trả về JSON nhất quán: **401** (`JwtAuthenticationEntryPoint`) hoặc **403** (`CustomAccessDeniedHandler`).

**Seeded roles & permissions / Vai trò & quyền mặc định**

| Role | Permissions |
|---|---|
| `ADMIN` | `USER_READ`, `USER_WRITE`, `USER_DELETE`, `ROLE_READ`, `ROLE_WRITE` |
| `USER` | `USER_READ` |

---

## 📦 Response Format / Định dạng phản hồi

**EN** — Every response uses the same envelope. Code `1000` means success.
**VI** — Mọi phản hồi dùng chung một lớp vỏ. Mã `1000` nghĩa là thành công.

**Success / Thành công**
```json
{
  "code": 1000,
  "message": "Login successful",
  "data": { "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer", "expiresIn": 3600 },
  "timestamp": "2026-06-05T10:00:00Z"
}
```

**Error / Lỗi**
```json
{
  "code": 4011,
  "message": "Invalid username or password",
  "data": null,
  "timestamp": "2026-06-05T10:00:00Z"
}
```

**Error code ranges / Dải mã lỗi**

| Range | Meaning / Ý nghĩa |
|---|---|
| `1000` | Success / Thành công |
| `4000–4001` | Bad request / validation |
| `4010–4014` | Authentication / xác thực |
| `4030` | Authorization / phân quyền (403) |
| `4040–4043` | Not found / không tìm thấy |
| `4090–4093` | Conflict / xung đột (đã tồn tại, token bị thu hồi…) |
| `9000–9999` | Server / internal errors |

---

## 📄 License / Giấy phép

**EN** — Released under the **MIT License**. Free to use, modify, and distribute.
**VI** — Phát hành theo **Giấy phép MIT**. Tự do sử dụng, chỉnh sửa và phân phối.

---

<div align="center">

**Built with ❤️ using Spring Boot / Xây dựng bằng ❤️ với Spring Boot**

⭐ If this project helps you, give it a star! / Nếu dự án hữu ích, hãy tặng một sao nhé!

</div>
