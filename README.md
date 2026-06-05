<div align="center">

# 🚀 Spring Boot Starter — JWT Auth & RBAC

**A production-ready Spring Boot base with JWT authentication, refresh-token rotation, and role/permission-based authorization.**

<br/>

<!-- 🌐 Language switcher / Nút chuyển ngôn ngữ -->
<a href="README.md"><img src="https://img.shields.io/badge/🇬🇧_English-2C5BFF?style=for-the-badge" alt="English"/></a>
<a href="README.vi.md"><img src="https://img.shields.io/badge/🇻🇳_Tiếng_Việt-555?style=for-the-badge" alt="Tiếng Việt"/></a>

<br/><br/>

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-Database-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

</div>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [API Reference](#-api-reference)
- [Security Model](#-security-model)
- [Response Format](#-response-format)
- [License](#-license)

---

## 🧭 Overview

This project is a clean, layered Spring Boot starter that you can fork to bootstrap any REST API quickly. It ships with a complete authentication flow (register / login / refresh / logout / current-user), a **role-based access control (RBAC)** model with fine-grained permissions, JPA auditing, a unified JSON response envelope, and centralized exception handling.

The codebase follows **SOLID** principles: every service is coded against an interface, responsibilities are cleanly separated, and cross-cutting concerns (security, errors, auditing) live in their own packages.

---

## 🛠 Tech Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| **Language** | Java | 21 (LTS) | Main language, modern record/pattern-matching |
| **Framework** | Spring Boot | 3.5.10 | Auto-configuration & dependency injection |
| **Web** | Spring Web (MVC) | — | REST controllers, JSON serialization |
| **Persistence** | Spring Data JPA + Hibernate | — | ORM, repository abstraction, Specification API |
| **Database** | MariaDB | — | Relational DB (`mariadb-java-client` driver) |
| **Security** | Spring Security | — | Authentication, authorization, method security |
| **Token** | JJWT (io.jsonwebtoken) | 0.12.6 | Issue & validate JWT (HS512) |
| **Validation** | Jakarta Bean Validation | — | Request payload validation (`@Valid`) |
| **Mail** | Spring Boot Starter Mail | — | Transactional emails (`EmailService`) |
| **Boilerplate** | Lombok | — | Reduce boilerplate (getter/setter/builder) |
| **Build** | Maven (`mvnw` wrapper) | 3.9.x | Dependency management & packaging |
| **Test** | JUnit 5 + Spring Security Test | — | Integration testing |

---

## ✨ Features

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

---

## 🏗 Architecture

Classic layered architecture with a one-way dependency flow. The web layer never touches persistence directly; services depend on **interfaces**, not implementations.

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
┌──────────────────────┐        ┌──────────────┐                  │   MariaDB    │
│ GlobalExceptionHandler│◀──────│  AppException │                  └──────────────┘
└──────────────────────┘        └──────────────┘
            ▲
            │  Security filter chain (JWT)  ──▶  EntryPoint / AccessDeniedHandler
```

---

## 📂 Project Structure

```text
src/main/java/vn/springboot
├── Application.java                  # Entry point
├── common
│   ├── entity/BaseEntity.java        # Audit fields (id, created/updated by/at)
│   ├── exception/                    # ErrorCode, AppException, GlobalExceptionHandler
│   └── response/ApiResponse.java     # Unified response envelope
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

## 🚦 Getting Started

### 1. Prerequisites

- **JDK 21+**
- **MariaDB** (or MySQL) running locally
- No need to install Maven — use the bundled wrapper `./mvnw`

### 2. Create the database

```sql
CREATE DATABASE dev_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Hibernate `ddl-auto: update` will create the tables automatically on first run.

### 3. Run

```bash
# Development
./mvnw spring-boot:run

# Build a runnable jar
./mvnw clean package
java -jar target/spring-boot-0.0.1-SNAPSHOT.jar
```

The app starts on **`http://localhost:8080`**.

### 4. Default admin account

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Email | `admin@springboot.vn` |

> ⚠️ **Change this password before deploying!**

### 5. Run with Docker

```bash
docker build -t spring-boot-app .
docker run -p 8080:8080 \
  -e DB_URL="jdbc:mariadb://host.docker.internal:3306/dev_db" \
  -e DB_USERNAME=root -e DB_PASSWORD=root \
  -e APP_JWT_SECRET="<base64-512bit-secret>" \
  spring-boot-app
```

---

## ⚙️ Configuration

All settings live in `src/main/resources/application.yaml` and can be overridden by environment variables.

| Env variable | Default | Description |
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

> 🔒 **Production tip:** generate a fresh JWT secret and pass it via `APP_JWT_SECRET`; never commit real secrets.

---

## 🔌 API Reference

Base path: **`/api`**

### 🔑 Authentication — `/api/auth`

| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| `POST` | `/register` | ❌ | Register a new user |
| `POST` | `/login` | ❌ | Login (username **or** email) → access + refresh token |
| `POST` | `/refresh` | ❌ | Rotate refresh token → new token pair |
| `POST` | `/logout` | ✅ | Revoke a refresh token |
| `GET` | `/me` | ✅ | Get the current authenticated user |

### 👤 Users — `/api/users`

| Method | Endpoint | Permission | Description |
|---|---|:---:|---|
| `GET` | `/` | `USER_READ` | Search + paginate users |
| `GET` | `/{id}` | `USER_READ` | Get a user by id |

### 📥 Example requests

**Register**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@mail.com","password":"secret123","fullName":"John Doe"}'
```

**Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**Call a protected endpoint**
```bash
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## 🛡 Security Model

1. **Login** validates credentials via `AuthenticationManager` + `BCryptPasswordEncoder`.
2. The server issues a short-lived **JWT access token** (HS512) and a long-lived, DB-stored **refresh token**.
3. Each request passes through `JwtAuthenticationFilter`, which validates the token and populates the `SecurityContext`.
4. **Authorization** is enforced with `@PreAuthorize("hasAuthority('USER_READ')")` — permissions are derived from the user's roles.
5. Auth failures return a consistent JSON body: **401** (`JwtAuthenticationEntryPoint`) or **403** (`CustomAccessDeniedHandler`).

**Seeded roles & permissions**

| Role | Permissions |
|---|---|
| `ADMIN` | `USER_READ`, `USER_WRITE`, `USER_DELETE`, `ROLE_READ`, `ROLE_WRITE` |
| `USER` | `USER_READ` |

---

## 📦 Response Format

Every response uses the same envelope. Code `1000` means success.

**Success**
```json
{
  "code": 1000,
  "message": "Login successful",
  "data": { "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer", "expiresIn": 3600 },
  "timestamp": "2026-06-05T10:00:00Z"
}
```

**Error**
```json
{
  "code": 4011,
  "message": "Invalid username or password",
  "data": null,
  "timestamp": "2026-06-05T10:00:00Z"
}
```

**Error code ranges**

| Range | Meaning |
|---|---|
| `1000` | Success |
| `4000–4001` | Bad request / validation |
| `4010–4014` | Authentication |
| `4030` | Authorization (403) |
| `4040–4043` | Not found |
| `4090–4093` | Conflict (already exists, token revoked…) |
| `9000–9999` | Server / internal errors |

---

## 📄 License

Released under the **MIT License**. Free to use, modify, and distribute.

---

<div align="center">

**Built with ❤️ using Spring Boot**

⭐ If this project helps you, give it a star!

</div>
