# Notification Preferences & FCM Push API — Hướng dẫn tích hợp Frontend

> Tài liệu bàn giao cho FE: **bật/tắt thông báo theo từng loại (type)** và **đăng ký thiết bị nhận push FCM**.
> Bổ sung cho `docs/WEBSOCKET_NOTIFICATION.md` (real-time) — tài liệu này tập trung phần cài đặt người dùng + push.

---

## 1. Tổng quan

- **Base URL:** `http://localhost:8080`
- **Auth:** mọi endpoint cần header `Authorization: Bearer <accessToken>`. Tất cả thao tác trên **user hiện tại**.
- **Envelope chung:** `{ "code": 1000, "message": "Success", "data": ..., "timestamp": "..." }`. `code = 1000` ⇒ thành công.
- **Thời gian:** ISO-8601 UTC.

### Mô hình thông báo theo loại (type)

Mỗi thông báo có một **`type`**, mỗi type thuộc một **`category`** (để FE gom nhóm) và có **`icon`** (key trừu tượng, FE tự map sang icon set của mình).

| `type` | `category` | `icon` |
|--------|-----------|--------|
| `PROMOTION` | `promotion` | `gift` |
| `ORDER_SUCCESS` | `order` | `shopping-bag` |
| `ORDER_CANCELLED` | `order` | `x-circle` |
| `SHIPPING` | `order` | `truck` |
| `DELIVERED` | `order` | `package-check` |
| `PAYMENT` | `order` | `credit-card` |
| `NEWS` | `news` | `newspaper` |
| `SECURITY` | `system` | `shield` |
| `ACCOUNT` | `account` | `user` |
| `SYSTEM` | `system` | `bell` |

> FE **không cần hard-code bảng này** — endpoint `GET /preferences` (mục 3.1) trả về đủ `type + category + icon + trạng thái bật/tắt` để dựng màn hình cài đặt.

### Hai kênh bật/tắt độc lập cho mỗi type

| Trường | Ý nghĩa khi bật | Khi tắt |
|--------|-----------------|---------|
| `inAppEnabled` | Thông báo được lưu vào "trung tâm thông báo" + đẩy real-time qua WebSocket | Không tạo bản ghi, không hiện in-app, không real-time |
| `pushEnabled`  | Đẩy push FCM tới thiết bị đã đăng ký | Không gửi push |

- **Mặc định cả 2 = bật.** Hệ thống chỉ lưu DB khi user **tự đổi** (mô hình opt-out) → user mới chưa cấu hình gì vẫn nhận đủ.
- Tắt cả 2 kênh của một type ⇒ type đó bị "câm" hoàn toàn với user.

---

## 2. Enum hợp lệ

- **`type`** (`NotificationType`): xem bảng mục 1.
- **`platform`** (`DevicePlatform`) cho đăng ký thiết bị: `ANDROID`, `IOS`, `WEB`.

---

## 3. Endpoint — Cài đặt thông báo theo type

### 3.1 `GET /api/notifications/preferences` — Lấy cài đặt hiện tại (dựng màn hình settings)

- **Mô tả:** trả về **toàn bộ** type kèm trạng thái bật/tắt hiệu lực của user. Dùng để render màn hình "Cài đặt thông báo".
- **Response 200:**
  ```json
  {
    "code": 1000,
    "message": "Success",
    "data": [
      { "type": "PROMOTION",     "category": "promotion", "icon": "gift",         "inAppEnabled": true,  "pushEnabled": false },
      { "type": "ORDER_SUCCESS", "category": "order",     "icon": "shopping-bag", "inAppEnabled": true,  "pushEnabled": true  },
      { "type": "SYSTEM",        "category": "system",    "icon": "bell",         "inAppEnabled": true,  "pushEnabled": true  }
    ],
    "timestamp": "2026-06-20T08:00:00Z"
  }
  ```
  > Mảng luôn chứa **đủ tất cả type** (kể cả type user chưa từng chỉnh — khi đó cả 2 cờ = `true`).

### 3.2 `PUT /api/notifications/preferences` — Lưu cài đặt

- **Mô tả:** cập nhật (upsert) cài đặt cho **một hoặc nhiều** type. Type không gửi lên thì **giữ nguyên**.
- **Request body:**
  ```json
  {
    "preferences": [
      { "type": "PROMOTION", "inAppEnabled": true,  "pushEnabled": false },
      { "type": "NEWS",      "inAppEnabled": false, "pushEnabled": false }
    ]
  }
  ```
- **Validation:** `preferences` không rỗng; mỗi item bắt buộc `type`, `inAppEnabled`, `pushEnabled` (không null).
- **Response 200:** trả về **toàn bộ** cài đặt sau khi lưu (giống cấu trúc 3.1), kèm `"message": "Preferences updated"`.

> Gợi ý UX cho FE: dùng đúng mảng `data` của 3.1 làm state; người dùng toggle rồi bấm "Lưu" → gửi 3.2 với các item đã đổi (hoặc gửi cả mảng cũng được).

---

## 4. Endpoint — Đăng ký thiết bị nhận Push (FCM)

Để nhận push, app/web lấy **FCM registration token** (qua Firebase SDK phía client) rồi đăng ký với backend.

### 4.1 `POST /api/notifications/devices` — Đăng ký / làm mới token

- **Request body:**
  ```json
  { "token": "<fcm-registration-token>", "platform": "ANDROID" }
  ```
- **Validation:** `token` không rỗng; `platform` ∈ {`ANDROID`, `IOS`, `WEB`}.
- **Hành vi:** token đã tồn tại thì cập nhật (gắn lại user hiện tại, bật enabled); chưa có thì tạo mới.
- **Response 200:** `{ "code": 1000, "message": "Device registered", "data": null, "timestamp": "..." }`

### 4.2 `DELETE /api/notifications/devices/{token}` — Gỡ thiết bị (khi logout)

- **Path param:** `token` — FCM token cần gỡ.
- **Response 200:** `{ "code": 1000, "message": "Device unregistered", "data": null, "timestamp": "..." }`

### Cấu trúc payload Push (FCM) mà thiết bị nhận

- **notification:** `title`, `body` (= content), `image` (nếu có `imageUrl`).
- **data:** `type`, `category`, `id` (nếu là noti đã lưu), `linkUrl` (deep-link để điều hướng khi tap).

> ⚙️ Phía backend: push FCM mặc định **tắt** ở môi trường base (`app.fcm.enabled=false`) → ghi log thay vì gửi thật.
> Bật bằng `APP_FCM_ENABLED=true` + cung cấp service-account JSON (`APP_FCM_CREDENTIALS`). FE không bị ảnh hưởng:
> luồng đăng ký token và cài đặt push vẫn hoạt động như trên.

---

## 5. Bảng mã lỗi

| `code` | HTTP | Khi nào |
|--------|------|---------|
| 1000 | 200 | Thành công |
| 4001 | 400 | Validation failed (thiếu field/sai enum) |
| 4010 | 401 | Thiếu/sai access token |
| 4040 | 404 | User không tồn tại (chỉ phát sinh ở luồng emit nội bộ) |

---

## 6. Liên quan

- Real-time qua WebSocket/STOMP: xem `docs/WEBSOCKET_NOTIFICATION.md` (kênh `/user/queue/notifications`).
- Danh sách / đếm chưa đọc / đánh dấu đã đọc: xem nhóm endpoint `GET /api/notifications`, `GET /api/notifications/unread-count`, `PATCH /api/notifications/{id}/read`, ...
