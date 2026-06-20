# API Docs — Index

Doc bàn giao FE **tách theo đối tượng** (quy ước tại `CLAUDE.md` §8):

- **`client/`** — API cho **Client app** (user cuối): chỉ cần đăng nhập, thao tác trên tài nguyên của chính mình.
- **`admin/`** — API cho **Admin Platform**: yêu cầu quyền quản trị (`@PreAuthorize`), quản lý toàn hệ thống.

## Client (`docs/client/`)

| Doc | Nội dung | Endpoint chính |
|-----|----------|----------------|
| [AUTH_API.md](client/AUTH_API.md) | Auth (dùng chung cả admin) + **envelope, error code, JWT, DTO chung** | `/api/auth/*` |
| [PROFILE_AND_UPLOAD_API.md](client/PROFILE_AND_UPLOAD_API.md) | Hồ sơ `/me`, avatar, upload file | `/api/users/me`, `/api/users/me/avatar`, `/api/files` |
| [NOTIFICATION_API.md](client/NOTIFICATION_API.md) | Notification center, preferences, device FCM, WebSocket | `/api/notifications/*`, `/ws` |

## Admin (`docs/admin/`)

| Doc | Nội dung | Endpoint chính |
|-----|----------|----------------|
| [USER_MANAGEMENT_API.md](admin/USER_MANAGEMENT_API.md) | RBAC + quản lý user + gán role cho user | `/api/users`, `/api/users/{id}`, `/api/users/{id}/roles` |
| [ROLE_MANAGEMENT_API.md](admin/ROLE_MANAGEMENT_API.md) | Quản lý role (CRUD) + đọc catalog permission | `/api/roles`, `/api/permissions` |

## Khác

- [_TEMPLATE_FEATURE_API.md](_TEMPLATE_FEATURE_API.md) — khung viết doc bàn giao cho feature mới.

> Đăng nhập và upload file dùng chung cho cả 2 phía → mô tả đầy đủ trong `client/` và được `admin/` link tham chiếu (không lặp lại).
