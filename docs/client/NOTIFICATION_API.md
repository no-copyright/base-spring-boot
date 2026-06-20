# Notification API (CLIENT) — Hướng dẫn tích hợp Frontend

> **Đối tượng:** **Client app** — thông báo của **chính user đang đăng nhập**: notification center (list / badge /
> mark read / delete), real-time WebSocket, bật/tắt theo loại, và đăng ký thiết bị nhận push FCM.
> Envelope response & bảng error code: xem `AUTH_API.md`.

---

## 1. Kiến trúc tổng quan

```
Business code → NotificationService.notifyUser(username, command)
                       │  (tôn trọng preference bật/tắt của user theo type)
                       ├── lưu NotificationEntity (DB, per-user)   [nếu inApp bật]
                       ├── đẩy real-time STOMP → /user/queue/notifications   [nếu inApp bật]
                       └── PushSender → thiết bị (FCM)             [nếu push bật]
```

- **Lưu DB**: mỗi notification gắn 1 user → notification center, đếm chưa đọc, xem lại.
- **Real-time**: user online nhận ngay qua WebSocket.
- **Push FCM**: đẩy tới thiết bị đã đăng ký (mặc định môi trường base **tắt FCM** → chỉ log; xem §7).

---

## 2. Loại thông báo & icon (`NotificationType`)

Backend trả `type`, `category`, `icon` (key trừu tượng) để FE render đồng nhất. **`icon` là key, không phải path** — FE map sang bộ icon của mình. Giữ ổn định key khi đã lên prod.

| `type` | `icon` | `category` | Gợi ý dùng |
|--------|--------|-----------|------------|
| `PROMOTION` | `gift` | `promotion` | Khuyến mãi, ưu đãi |
| `ORDER_SUCCESS` | `shopping-bag` | `order` | Đặt hàng thành công |
| `ORDER_CANCELLED` | `x-circle` | `order` | Đơn bị huỷ |
| `SHIPPING` | `truck` | `order` | Đang vận chuyển |
| `DELIVERED` | `package-check` | `order` | Đã giao |
| `PAYMENT` | `credit-card` | `order` | Thanh toán |
| `NEWS` | `newspaper` | `news` | Tin tức |
| `SECURITY` | `shield` | `system` | Cảnh báo bảo mật |
| `ACCOUNT` | `user` | `account` | Tài khoản |
| `SYSTEM` | `bell` | `system` | Hệ thống chung |

> Response luôn trả `icon` đã resolve sẵn — FE chỉ cần đọc `icon`. `platform` thiết bị: `ANDROID` | `IOS` | `WEB`.

---

## 3. Payload thông báo (`NotificationResponse`)

Dùng chung cho REST và WebSocket:

```json
{
  "id": 123,
  "type": "ORDER_SUCCESS",
  "category": "order",
  "icon": "shopping-bag",
  "title": "Đặt hàng thành công",
  "content": "Đơn #SO-2026-001 đã được tiếp nhận.",
  "imageUrl": "https://cdn.example.com/banner.png",
  "linkUrl": "/orders/SO-2026-001",
  "data": { "orderId": "SO-2026-001", "amount": 250000 },
  "read": false,
  "readAt": null,
  "createdAt": "2026-06-20T10:00:00Z"
}
```

| Field | Kiểu | Ý nghĩa |
|-------|------|---------|
| `id` | number\|null | ID (null nếu broadcast không lưu) |
| `type` / `category` / `icon` | string | Loại / nhóm / icon key (xem §2) |
| `title` / `content` | string | Tiêu đề / nội dung |
| `imageUrl` / `linkUrl` | string\|null | Ảnh rich / deep-link điều hướng khi bấm |
| `data` | object\|null | Payload tuỳ type (id đơn, số tiền...) |
| `read` / `readAt` | boolean / string\|null | Trạng thái đọc |
| `createdAt` | string | ISO-8601 |

> Field null bị lược khỏi JSON (`NON_NULL`).

---

## 4. REST API — Notification Center

Base path: `/api/notifications` — **yêu cầu xác thực**, thao tác trên thông báo của **chính user đang đăng nhập**.

### 4.1. Danh sách — `GET /api/notifications`

Query (tuỳ chọn): `read` (`true`/`false`), `type` (vd `PROMOTION`), `page` (0), `size` (20, tối đa 100), `sortDirection` (`DESC`/`ASC` theo `createdAt`).

Ví dụ: `GET /api/notifications?read=false&type=ORDER_SUCCESS&page=0&size=20`
`data` = `PageResponse<NotificationResponse>` (cấu trúc `PageResponse`: xem `AUTH_API.md` §4).

### 4.2. Đếm chưa đọc (badge) — `GET /api/notifications/unread-count`
```json
{ "code": 1000, "data": { "count": 5 } }
```

### 4.3. Đánh dấu đã đọc 1 — `PATCH /api/notifications/{id}/read`
`data = null`. Lỗi `4044` nếu không tìm thấy / không thuộc user.

### 4.4. Đánh dấu đã đọc tất cả — `PATCH /api/notifications/read-all`
`data = null`.

### 4.5. Xoá 1 thông báo — `DELETE /api/notifications/{id}`
`data = null`. Lỗi `4044` nếu không tìm thấy.

### Bảng tổng hợp

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/notifications` | Danh sách (phân trang, lọc read/type) |
| GET | `/api/notifications/unread-count` | Số chưa đọc (badge) |
| PATCH | `/api/notifications/{id}/read` | Đánh dấu đã đọc 1 |
| PATCH | `/api/notifications/read-all` | Đánh dấu đã đọc tất cả |
| DELETE | `/api/notifications/{id}` | Xoá 1 |

---

## 5. Bật/tắt thông báo theo loại (Preferences)

Hai kênh **độc lập** cho mỗi `type`:

| Trường | Khi bật | Khi tắt |
|--------|---------|---------|
| `inAppEnabled` | Lưu vào notification center + đẩy real-time WebSocket | Không tạo bản ghi, không real-time |
| `pushEnabled` | Đẩy push FCM tới thiết bị | Không gửi push |

- **Mặc định cả 2 = bật** (opt-out): chỉ lưu DB khi user tự đổi → user mới vẫn nhận đủ.
- Tắt cả 2 của một type ⇒ type đó bị "câm" hoàn toàn.

### 5.1 `GET /api/notifications/preferences` — Lấy cài đặt (dựng màn hình settings)

Trả **đủ tất cả type** kèm trạng thái hiệu lực (type chưa chỉnh → cả 2 cờ `true`):
```json
{
  "code": 1000, "message": "Success",
  "data": [
    { "type": "PROMOTION",     "category": "promotion", "icon": "gift",         "inAppEnabled": true, "pushEnabled": false },
    { "type": "ORDER_SUCCESS", "category": "order",     "icon": "shopping-bag", "inAppEnabled": true, "pushEnabled": true  }
  ],
  "timestamp": "..."
}
```
> FE dựng nguyên màn hình settings từ mảng này — không cần hard-code danh sách type.

### 5.2 `PUT /api/notifications/preferences` — Lưu cài đặt

Upsert cho một/nhiều type; type không gửi thì giữ nguyên.
```json
{ "preferences": [
    { "type": "PROMOTION", "inAppEnabled": true,  "pushEnabled": false },
    { "type": "NEWS",      "inAppEnabled": false, "pushEnabled": false }
] }
```
- **Validation:** `preferences` không rỗng; mỗi item bắt buộc `type`, `inAppEnabled`, `pushEnabled`.
- **Response 200:** toàn bộ cài đặt sau khi lưu (như 5.1), `message = "Preferences updated"`.

---

## 6. Đăng ký thiết bị nhận Push (FCM)

App/web lấy **FCM registration token** (qua Firebase SDK client) rồi đăng ký với backend.

### 6.1 `POST /api/notifications/devices` — Đăng ký / làm mới token
```json
{ "token": "<fcm-registration-token>", "platform": "ANDROID" }
```
- **Validation:** `token` không rỗng; `platform` ∈ {`ANDROID`,`IOS`,`WEB`}.
- Idempotent: token đã tồn tại → cập nhật (user/platform/enabled). `data = null`, `message = "Device registered"`.

### 6.2 `DELETE /api/notifications/devices/{token}` — Gỡ thiết bị (khi logout)
`data = null`, `message = "Device unregistered"`.

### Payload Push (FCM) thiết bị nhận
- **notification:** `title`, `body` (= content), `image` (nếu có `imageUrl`).
- **data:** `type`, `category`, `id` (nếu noti đã lưu), `linkUrl` (deep-link khi tap).

---

## 7. Real-time qua WebSocket / STOMP

| Mục | Giá trị |
|-----|---------|
| Handshake | `/ws` (SockJS) — vd `http://localhost:8080/ws` |
| Xác thực | STOMP header `Authorization: Bearer <accessToken>` ở frame **CONNECT** |
| Tin riêng user | subscribe `/user/queue/notifications` |
| Tin broadcast | subscribe `/topic/notifications` |

```ts
// npm i @stomp/stompjs sockjs-client
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  connectHeaders: { Authorization: `Bearer ${accessToken}` },  // bắt buộc
  reconnectDelay: 5000,
  onConnect: () => {
    client.subscribe('/user/queue/notifications', (msg) => {
      const n = JSON.parse(msg.body);   // NotificationResponse
      pushToInbox(n);                    // tăng badge, toast theo n.type/n.icon
    });
    client.subscribe('/topic/notifications', (msg) => {
      const n = JSON.parse(msg.body);    // broadcast (không lưu DB)
    });
  },
});
client.activate();
// logout/unmount: client.deactivate();
```

> Flow chuẩn FE:
> 1. Load app → `GET /unread-count` + `GET /api/notifications` (trang đầu).
> 2. Mở WebSocket → nghe `/user/queue/notifications`, tin mới thì tăng badge + prepend list.
> 3. Mở panel → list; bấm item → `PATCH /{id}/read` + điều hướng `linkUrl`.
> 4. "Đọc tất cả" → `PATCH /read-all`. 5. Access token hết hạn (1h) → refresh rồi `deactivate()`+`activate()` lại.

---

## 8. Ghi chú vận hành (BE)

- **FCM:** đã có `FcmPushSender` thật + `FirebaseConfig`. Mặc định **tắt** (`app.fcm.enabled=false`) → dùng `LoggingPushSender` (no-op, chỉ log), app chạy không cần credentials. Bật: `APP_FCM_ENABLED=true` + service-account JSON (`APP_FCM_CREDENTIALS`). FE không bị ảnh hưởng — luồng đăng ký token & preference vẫn như trên.
- **Phát thông báo (dev sau này):** inject `NotificationService` → `notifyUser(username, SendNotificationCommand.builder()...build())` (lưu DB + WS + push, có áp preference); `broadcast(cmd)` chỉ đẩy `/topic/notifications`, **không lưu DB**.
- **Broker in-memory** (1 instance). Scale nhiều node → external broker relay (RabbitMQ/ActiveMQ STOMP).
- **Schema** do Flyway quản (`db/migration`); preference lưu bảng `notification_preferences` (opt-out).

---

## 9. Bảng mã lỗi (phần notification)

| `code` | HTTP | Khi nào |
|--------|------|---------|
| 1000 | 200 | Thành công |
| 4001 | 400 | Validation failed (thiếu field/sai enum) |
| 4010 | 401 | Thiếu/sai access token |
| 4044 | 404 | Không tìm thấy notification (mark-read/delete) |

(Đầy đủ error code: xem `AUTH_API.md` §1.)
