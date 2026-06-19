# Notification System — Hướng dẫn tích hợp Frontend

> Hệ thống thông báo chuẩn prod: **lưu DB per-user**, **real-time qua STOMP/WebSocket**, **REST API** cho notification center (list / unread badge / mark read / read all / delete), và **dựng sẵn đường cho FCM push**.
> Hiện chỉ là hạ tầng — **chưa gắn trigger nghiệp vụ cụ thể** (khuyến mãi, đặt hàng, vận chuyển...).
> Xác thực dùng chung JWT với REST API (xem [AUTH_RBAC_API.md](AUTH_RBAC_API.md)).

---

## 1. Kiến trúc tổng quan

```
Business code → NotificationService.notifyUser(username, command)
                       │
                       ├── lưu NotificationEntity (DB, per-user)
                       ├── đẩy real-time STOMP → /user/queue/notifications
                       └── PushSender → thiết bị (FCM/APNs)  [hiện no-op, log]
```

- **Lưu DB**: mỗi notification là 1 bản ghi gắn với 1 user → hỗ trợ notification center, đếm chưa đọc, xem lại.
- **Real-time**: user đang online nhận ngay qua WebSocket.
- **Push**: user offline → đẩy FCM/APNs (đang để no-op, chỉ log; sẽ thay bằng adapter FCM sau).

---

## 2. Loại thông báo & icon chuẩn (`NotificationType`)

Backend trả về `type`, `category` và `icon` (key trừu tượng) để FE render đồng nhất. **`icon` là key, không phải đường dẫn file** — FE map sang bộ icon của mình. Giữ ổn định các key này khi đã lên prod.

| `type` | `icon` (key) | `category` | Gợi ý dùng |
|--------|--------------|-----------|------------|
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

> Backend có thể override icon trên từng notification (field `icon`); nếu null FE dùng icon mặc định của `type`. Response luôn trả `icon` đã resolve sẵn — FE chỉ cần đọc `icon`.

---

## 3. Payload thông báo (`NotificationResponse`)

Dùng chung cho cả REST và WebSocket:

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
  "createdAt": "2026-06-19T10:00:00Z"
}
```

| Field | Kiểu | Ý nghĩa |
|-------|------|---------|
| `id` | number | ID notification (null nếu là broadcast không lưu) |
| `type` | enum | Loại (bảng mục 2) |
| `category` | string | Nhóm để gom/lọc |
| `icon` | string | Icon key đã resolve |
| `title` | string | Tiêu đề |
| `content` | string | Nội dung |
| `imageUrl` | string\|null | Ảnh rich (banner khuyến mãi...) |
| `linkUrl` | string\|null | Deep link FE điều hướng khi bấm |
| `data` | object\|null | Payload tuỳ type (id đơn, số tiền...) |
| `read` | boolean | Đã đọc chưa |
| `readAt` | string\|null | Thời điểm đọc |
| `createdAt` | string | Thời điểm tạo (ISO-8601) |

> Các field null bị lược bỏ khỏi JSON (`NON_NULL`).

---

## 4. REST API — Notification Center

Base path: `/api/notifications` — **tất cả yêu cầu xác thực** (`Authorization: Bearer <accessToken>`), thao tác trên notification của **chính user đang đăng nhập**. Envelope & error code: xem [AUTH_RBAC_API.md](AUTH_RBAC_API.md) mục 3.

### 4.1. Danh sách — `GET /api/notifications`

Query params (tuỳ chọn):

| Param | Mặc định | Ý nghĩa |
|-------|----------|---------|
| `read` | — | `true`/`false` lọc theo trạng thái đọc; bỏ trống = tất cả |
| `type` | — | Lọc theo `NotificationType` (vd `PROMOTION`) |
| `page` | `0` | Trang (0-based) |
| `size` | `20` | Số phần tử/trang (tối đa 100) |
| `sortDirection` | `DESC` | Sắp theo `createdAt`: `DESC`/`ASC` |

Ví dụ: `GET /api/notifications?read=false&type=ORDER_SUCCESS&page=0&size=20`

`data` = `PageResponse<NotificationResponse>` (xem cấu trúc `PageResponse` ở [AUTH_RBAC_API.md](AUTH_RBAC_API.md) mục 6).

### 4.2. Đếm chưa đọc (badge) — `GET /api/notifications/unread-count`
```json
{ "code": 1000, "data": { "count": 5 } }
```

### 4.3. Đánh dấu đã đọc 1 cái — `PATCH /api/notifications/{id}/read`
`data = null`. Lỗi `4044` nếu không tìm thấy / không thuộc về user.

### 4.4. Đánh dấu đã đọc tất cả — `PATCH /api/notifications/read-all`
`data = null`. Set toàn bộ notification chưa đọc của user thành đã đọc.

### 4.5. Xoá 1 thông báo — `DELETE /api/notifications/{id}`
`data = null`. Lỗi `4044` nếu không tìm thấy.

### 4.6. Đăng ký device token (FCM) — `POST /api/notifications/devices`
```json
{ "token": "<fcm-registration-token>", "platform": "ANDROID" }  // ANDROID | IOS | WEB
```
Idempotent: token đã tồn tại sẽ được cập nhật (user/platform/enabled). `data = null`.

### 4.7. Huỷ device token — `DELETE /api/notifications/devices/{token}`
Gọi khi logout / tắt nhận push trên thiết bị. `data = null`.

### Bảng tổng hợp endpoint

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/notifications` | Danh sách (phân trang, lọc read/type) |
| GET | `/api/notifications/unread-count` | Số chưa đọc (badge) |
| PATCH | `/api/notifications/{id}/read` | Đánh dấu đã đọc 1 |
| PATCH | `/api/notifications/read-all` | Đánh dấu đã đọc tất cả |
| DELETE | `/api/notifications/{id}` | Xoá 1 |
| POST | `/api/notifications/devices` | Đăng ký FCM token |
| DELETE | `/api/notifications/devices/{token}` | Huỷ FCM token |

---

## 5. Real-time qua WebSocket / STOMP

| Mục | Giá trị |
|-----|---------|
| Handshake endpoint | `/ws` (SockJS) — vd `http://localhost:8080/ws` |
| Xác thực | STOMP header `Authorization: Bearer <accessToken>` ở frame **CONNECT** |
| Tin riêng user | subscribe `/user/queue/notifications` |
| Tin broadcast | subscribe `/topic/notifications` |

Khi `notifyUser(...)` chạy, server đẩy `NotificationResponse` (mục 3) tới `/user/queue/notifications` của đúng user đó.

### Ví dụ FE (`@stomp/stompjs` + `sockjs-client`)
```bash
npm i @stomp/stompjs sockjs-client
```
```ts
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
  onStompError: (frame) => console.error('STOMP error:', frame.headers['message']),
});
client.activate();
// logout/unmount: client.deactivate();
```

> Gợi ý flow chuẩn FE:
> 1. Load app → `GET /unread-count` + `GET /api/notifications` (trang đầu).
> 2. Mở WebSocket → nghe `/user/queue/notifications`, có tin mới thì tăng badge + prepend list.
> 3. User mở panel → hiển thị list; bấm 1 item → `PATCH /{id}/read` + điều hướng `linkUrl`.
> 4. Nút "Đọc tất cả" → `PATCH /read-all`.
> 5. Access token hết hạn (1h): refresh rồi `deactivate()` + `activate()` lại với token mới.

---

## 6. Phía Backend — phát thông báo (cho dev sau này)

Inject `NotificationService`, build `SendNotificationCommand`:

```java
@RequiredArgsConstructor
public class OrderService {
    private final NotificationService notificationService;

    void onOrderPaid(Order order) {
        notificationService.notifyUser(order.getUsername(), SendNotificationCommand.builder()
                .type(NotificationType.ORDER_SUCCESS)
                .title("Đặt hàng thành công")
                .content("Đơn #" + order.getCode() + " đã được tiếp nhận.")
                .linkUrl("/orders/" + order.getCode())
                .data(Map.of("orderId", order.getCode(), "amount", order.getAmount()))
                .build());
    }
}
```

- `notifyUser(username, cmd)` → lưu DB + đẩy WebSocket + (tương lai) FCM.
- `broadcast(cmd)` → chỉ đẩy WebSocket `/topic/notifications`, **không lưu DB** (dùng cho thông báo tức thời toàn hệ thống).

---

## 7. FCM Push — đã dựng sẵn đường, chưa kích hoạt

| Thành phần | Trạng thái |
|------------|-----------|
| `DeviceTokenEntity` + `/api/notifications/devices` | ✅ Lưu & quản lý token thiết bị theo user |
| `PushSender` (interface) | ✅ Cổng gửi push |
| `LoggingPushSender` (mặc định) | ✅ No-op, chỉ log — kích hoạt khi chưa có bean FCM (`PushConfig`) |
| `FcmPushSender` (FCM thật) | ⬜ **Chưa làm** |

**Khi muốn bật FCM thật:**
1. Thêm dependency `com.google.firebase:firebase-admin`.
2. Khởi tạo `FirebaseApp` từ service-account credentials (biến môi trường).
3. Tạo bean `FcmPushSender implements PushSender` gọi `FirebaseMessaging.send(...)` với danh sách token.
4. Xong — `LoggingPushSender` tự nhường chỗ (`@ConditionalOnMissingBean`), `NotificationServiceImpl.dispatch()` đã gọi `pushSender.sendToDevices(...)` sẵn.

---

## 8. Thành phần đã thêm (tham chiếu mã nguồn)

| File | Vai trò |
|------|---------|
| `enums/NotificationType.java` | Loại thông báo + icon + category |
| `enums/DevicePlatform.java` | ANDROID / IOS / WEB |
| `entity/notification/NotificationEntity.java` | Bản ghi thông báo per-user (read, readAt, metadata...) |
| `entity/notification/DeviceTokenEntity.java` | Token thiết bị cho FCM |
| `repository/NotificationRepository.java` · `DeviceTokenRepository.java` | Truy vấn (đếm chưa đọc, mark-all-read...) |
| `repository/specification/NotificationSpecification.java` | Lọc theo recipient/type/read |
| `dto/request/notification/*` | `SendNotificationCommand`, `NotificationSearchRequest`, `DeviceTokenRequest` |
| `dto/response/notification/*` | `NotificationResponse`, `UnreadCountResponse` |
| `mapper/NotificationMapper.java` | Entity → Response, resolve icon, (de)serialize JSON `data` |
| `service/NotificationService.java` + `impl/NotificationServiceImpl.java` | Facade: emit + notification center + device token |
| `notification/push/PushSender.java` + `LoggingPushSender.java` + `config/PushConfig.java` | Cổng push FCM (mặc định no-op) |
| `controller/NotificationController.java` | REST API notification center |
| `config/WebSocketConfig.java` + `websocket/StompAuthChannelInterceptor.java` | STOMP broker + auth JWT lúc CONNECT |
| `security/SecurityUtils.java` | Lấy user hiện tại từ SecurityContext |

---

## 9. Ghi chú vận hành / mở rộng

- **Broker in-memory** (`enableSimpleBroker`) — đủ cho 1 instance. Scale nhiều node → dùng external broker relay (RabbitMQ/ActiveMQ STOMP).
- **Broadcast hiện không lưu DB.** Nếu cần thông báo toàn hệ thống mà user offline vẫn xem lại được, cần fan-out lưu bản ghi cho từng user (cân nhắc job nền) — chưa làm.
- **DB schema** tạo tự động (`ddl-auto: update`) — entity mới sẽ tự sinh bảng `notifications`, `device_tokens`.
- Chưa có `@MessageMapping` cho tin client→server; luồng hiện tại chỉ server→client.
