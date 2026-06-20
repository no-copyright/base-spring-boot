# CLAUDE.md — Quy trình & quy ước phát triển cho AI

> File này là **bản hợp đồng làm việc** cho bất kỳ AI/dev nào thêm hoặc sửa một chức năng
> trong codebase này. Đọc kỹ và làm **đúng từng bước**. Mục tiêu: code sạch, đồng nhất với
> cấu trúc hiện có, query hiệu quả (không N+1), có migration + seeder + unit test + doc bàn giao FE.

---

## 0. Tóm tắt stack & kiến trúc

- **Spring Boot 3.5.x · Java 21 · MariaDB · Maven.**
- **Flyway** quản lý schema bằng file SQL (`src/main/resources/db/migration`). Hibernate chạy ở chế độ `validate` — **không** tự sửa schema.
- **JWT** (access token stateless + refresh token lưu DB) + **RBAC** (User ⇄ Role ⇄ Permission).
- **MapStruct** + mapper viết tay cho map DTO ⇄ Entity.
- **WebSocket/STOMP** cho real-time notification.
- Base package: `vn.springboot`.

### Kiến trúc phân lớp (bắt buộc tuân theo)

```
controller/      → REST endpoint, nhận DTO request, trả ApiResponse<T>. KHÔNG chứa business logic.
service/         → interface nghiệp vụ (vd UserService).
service/impl/    → implement nghiệp vụ (vd UserServiceImpl). Đặt @Transactional ở đây.
repository/      → Spring Data JPA interface. Query JPA derived + JPQL.
repository/specification/ → Specification cho query động (filter/search nhiều điều kiện).
entity/          → JPA entity, kế thừa BaseEntity. Chia theo domain (user/, notification/...).
dto/request/<feature>/   · dto/response/<feature>/  → DTO, chia theo feature.
mapper/          → MapStruct hoặc mapper tay (khi cần xử lý JSON, logic đặc biệt).
common/          → response (ApiResponse), exception (AppException, ErrorCode), entity (BaseEntity).
enums/ · config/ · security/ · websocket/
```

**Luồng chuẩn 1 request:** `Controller → Service(interface) → ServiceImpl → Repository → DB`,
trả về `ApiResponse<PageResponse<...>>` hoặc `ApiResponse<XxxResponse>`.

---

## 1. QUY TRÌNH PHÁT TRIỂN 1 CHỨC NĂNG MỚI (checklist bắt buộc)

Khi được yêu cầu thêm/sửa một chức năng, làm **tuần tự** các bước sau. Đừng bỏ bước nào.

1. **Hiểu yêu cầu & rà soát cái đã có.** Tìm code/pattern tương tự (vd: làm CRUD mới thì soi `User*`/`Notification*`). Tái sử dụng convention sẵn có, đừng phát minh kiểu mới.
2. **Entity** (nếu cần bảng/cột mới): tạo/sửa entity kế thừa `BaseEntity`. **Không** map quan hệ — khóa ngoại để dạng cột `Long`. → đọc [§4 Không map quan hệ JPA].
3. **Migration Flyway**: viết file `V{n}__<mô_tả>.sql` cho mọi thay đổi schema. → [§5].
4. **Seeder**: viết/cập nhật seed cho bảng vừa đụng tới. → [§6].
5. **Repository**: thêm query. Đơn giản → JPA derived; hơi phức tạp → JPQL; filter động → Specification. → [§3].
6. **DTO**: tạo request (kèm validation) + response trong `dto/request/<feature>` và `dto/response/<feature>`.
7. **Mapper**: MapStruct cho map thuần; mapper tay khi có JSON/logic.
8. **Service interface + impl**: đặt business logic & `@Transactional` ở impl. Ném `AppException(ErrorCode.X)` cho lỗi nghiệp vụ.
9. **Controller**: endpoint mỏng, trả `ApiResponse`. Phân quyền bằng `@PreAuthorize` khi cần.
10. **ErrorCode**: thêm mã lỗi mới vào enum `ErrorCode` (đúng dải số) nếu phát sinh lỗi mới.
11. **Unit test**: vài case cho service/logic chính. → [§7].
12. **Doc bàn giao FE**: viết/cập nhật `docs/<FEATURE>_API.md`. → [§8].
13. **Build & kiểm tra**: `./mvnw test` phải xanh trước khi coi là xong. → [§9 Definition of Done].

---

## 2. Quy ước code sạch

- Java 21, dùng `var` hợp lý, `record` cho DTO bất biến nếu phù hợp (nhưng giữ đồng nhất: hiện DTO đang dùng class + Lombok `@Data/@Builder` → theo style đó).
- Lombok: `@RequiredArgsConstructor` cho injection (constructor injection, **không** `@Autowired` field).
- Đặt tên: Entity = `XxxEntity`, repo = `XxxRepository`, service = `XxxService`/`XxxServiceImpl`, request = `XxxRequest`/`XxxCommand`, response = `XxxResponse`.
- Mỗi class/method công khai có Javadoc ngắn gọn nêu *mục đích* (xem các file hiện có để bắt chước mật độ comment — vừa phải, tiếng Anh).
- Không nuốt exception. Lỗi nghiệp vụ → `AppException(ErrorCode.X)`; để `GlobalExceptionHandler` xử lý.
- `@Transactional(readOnly = true)` cho thao tác đọc, `@Transactional` cho ghi — đặt ở **ServiceImpl**, không đặt ở controller/repository.
- Hằng số (page size tối đa, tên topic...) đặt `private static final` đầu class như `NotificationServiceImpl`.

---

## 3. QUY TẮC TRUY VẤN DATABASE (JPA vs JPQL) — RẤT QUAN TRỌNG

Nguyên tắc lõi của dự án:

> **Lệnh cơ bản dùng JPA derived query là được. Hễ query *hơi phức tạp một tí* → DÙNG JPQL NGAY.**

### 3.1 Dùng JPA derived query (đặt tên method) — CHỈ cho truy vấn đơn giản

Được phép khi query ngắn, 1–2 điều kiện, không join thủ công, không tính toán:

```java
Optional<UserEntity> findByUsername(String username);
boolean existsByEmail(String email);
long countByRecipientIdAndReadFalse(Long recipientId);   // lọc theo cột *_id, không theo entity
```

Giới hạn: nếu tên method bắt đầu dài dòng (`findByAAndBAndCOrderByD...`), join nhiều bảng,
cần chọn cột cụ thể, cần aggregate → **dừng lại, chuyển sang JPQL**.

### 3.2 Dùng JPQL (`@Query`) — cho mọi thứ phức tạp hơn

Bắt buộc dùng JPQL khi: join, `GROUP BY`/aggregate, update/delete hàng loạt, projection ra DTO,
fetch join để tránh N+1, hoặc điều kiện đủ rối khiến derived-name khó đọc.

```java
@Query("UPDATE NotificationEntity n SET n.read = true, n.readAt = :now "
        + "WHERE n.recipient = :recipient AND n.read = false")
@Modifying
int markAllReadByRecipient(@Param("recipient") UserEntity recipient, @Param("now") Instant now);
```

- `@Modifying` cho UPDATE/DELETE; method phải nằm trong `@Transactional` (ở service).
- **Projection DTO bằng JPQL constructor expression** khi chỉ cần vài cột (nhanh + tránh kéo cả entity):
  ```java
  @Query("SELECT new vn.springboot.dto.response.user.UserResponse(u.id, u.username, u.email) "
          + "FROM UserEntity u WHERE u.enabled = true")
  List<UserResponse> findActiveUsers();
  ```
- Đặt tham số bằng **named param** (`:name`) + `@Param`, không dùng `?1`.
- **Không** viết native SQL trừ khi buộc phải dùng tính năng riêng của MariaDB; ưu tiên JPQL để độc lập DB.

### 3.3 Query động (nhiều filter optional) → Specification

Khi search/list có nhiều bộ lọc tùy chọn (FE truyền cái nào lọc cái đó), dùng `JpaSpecificationExecutor`
+ một class trong `repository/specification/` (xem `NotificationSpecification`, `UserSpecification`):

```java
Specification<NotificationEntity> spec = NotificationSpecification.build(me, request);
Page<NotificationEntity> page = notificationRepository.findAll(spec, pageable);
```

### 3.4 Phân trang

Mọi API list **phải** phân trang: nhận page/size từ request, **clamp size** (vd tối đa 100),
trả `PageResponse<T>`. Xem `NotificationServiceImpl.getMyNotifications`.

---

## 4. KHÔNG MAP QUAN HỆ JPA — RẤT QUAN TRỌNG

> **TUYỆT ĐỐI không dùng `@OneToMany`, `@ManyToMany`, `@ManyToOne`, `@OneToOne`, `@JoinColumn`, `@JoinTable`, `@ElementCollection`.**
> Quan hệ object-graph của JPA là nguồn gốc chính của N+1 query và load thừa dữ liệu.
> Trong dự án này, **khóa ngoại lưu thẳng dạng cột `Long`** và **mọi quan hệ ràng buộc bằng FK trong file migration**, không map ở tầng entity.

### Quy tắc

1. **Khóa ngoại = cột `Long` thường.** Thay vì map object, lưu id:
   ```java
   @Column(name = "recipient_id", nullable = false)
   private Long recipientId;          // KHÔNG: @ManyToOne UserEntity recipient
   ```
   Entity chỉ là ảnh xạ phẳng của 1 bảng (các cột vô hướng + cột `*_id`). Không chứa entity khác bên trong.

2. **Quan hệ & toàn vẹn dữ liệu đặt ở migration**, không ở entity: khai báo `FOREIGN KEY (...) REFERENCES ...`
   trong file `V{n}__*.sql` (xem `V1__baseline_schema.sql`). DB lo ràng buộc; code lo query.

3. **Cần dữ liệu bảng liên quan → query tay theo id**, chọn 1 trong các cách (ưu tiên từ trên xuống):
   - **Projection DTO bằng JPQL có join tường minh** (nhanh nhất, lấy đúng cột, 1 query):
     ```java
     @Query("SELECT new vn.springboot.dto.response.notification.NotificationResponse(n.id, n.title, u.username) "
             + "FROM NotificationEntity n JOIN UserEntity u ON u.id = n.recipientId "
             + "WHERE n.id = :id")
     Optional<NotificationResponse> findDetail(@Param("id") Long id);
     ```
     (Hibernate 6 hỗ trợ *ad-hoc entity join* `JOIN XxxEntity x ON x.id = a.xId` dù entity không map quan hệ.)
   - **Hai truy vấn rồi ghép ở service** khi danh sách cha nhỏ: lấy danh sách A, gom tập `aIds`,
     query B `WHERE b.aId IN :aIds` **một lần**, rồi map theo id ở Java. **Không** query trong vòng lặp (đó chính là N+1).
   - Lấy lẻ 1 bản ghi liên quan → gọi repository tương ứng `findById`.

4. **Tự kiểm N+1:** `show-sql: true` đã bật. Chạy endpoint list, nếu log lặp 1 query chính + N query con
   → bạn đang query trong vòng lặp; gộp lại bằng `IN (:ids)` hoặc JPQL join.

5. **Entity hiện tại đang map quan hệ là LEGACY.** `UserEntity`/`RoleEntity` (`@ManyToMany`),
   `NotificationEntity`/`DeviceTokenEntity`/`RefreshTokenEntity` (`@ManyToOne`) viết theo lối cũ —
   **không lấy làm khuôn mẫu.** Code MỚI tuân theo quy tắc 1–3 ở trên. Khi refactor được thì gỡ dần annotation
   về cột `Long` (cần migration/đảm bảo không vỡ RBAC trước khi đổi).

---

## 5. MIGRATION (Flyway) — bắt buộc cho MỌI thay đổi schema

- Vị trí: `src/main/resources/db/migration/`.
- Đặt tên: `V{n}__<mô_tả_ngắn>.sql` — vd `V2__add_orders_table.sql`, `V3__add_phone_to_users.sql`.
  `{n}` tăng dần liên tục, **không trùng**, không sửa file migration đã merge/đã chạy (Flyway check checksum).
- **Quy tắc bất biến:** file migration đã commit/đã chạy là **đóng băng**. Cần đổi → tạo `V{n+1}` mới.
- Mỗi bảng mới phải có đủ cột audit của `BaseEntity`: `created_at DATETIME(6)`, `updated_at DATETIME(6)`,
  `created_by VARCHAR(100)`, `updated_by VARCHAR(100)`.
- Map kiểu: `Long id` → `BIGINT AUTO_INCREMENT PK`; `Instant` → `DATETIME(6)`; `boolean` → `BOOLEAN`;
  `String` → `VARCHAR(n)` (n khớp `length` trong `@Column`, mặc định 255); enum `@Enumerated(STRING)` → `VARCHAR`;
  `@Lob`/text dài → `TEXT`. Engine `InnoDB`, charset `utf8mb4`.
- **Quan hệ khai báo bằng `FOREIGN KEY (...) REFERENCES ...`** ngay trong migration (vì entity không map quan hệ — xem [§4]).
  Mỗi cột `*_id` nên có FK constraint tương ứng.
- `UNIQUE` cho cột có ràng buộc duy nhất (username, email, token...).
- Sau khi viết migration, **schema phải khớp entity** (vì `ddl-auto: validate`). Lệch là app không boot.
- Tham khảo `V1__baseline_schema.sql` làm khuôn mẫu.

### 5.1 Đánh INDEX — bắt buộc cân nhắc, nhưng KHÔNG lạm dụng

Index sai/thiếu là nguyên nhân chậm phổ biến nhất; index thừa làm phình DB và chậm ghi (INSERT/UPDATE).
Quy tắc khi tạo bảng hoặc thêm tính năng tìm kiếm/lọc:

- **PHẢI đánh index** cho:
  - Cột khóa ngoại `*_id` (gần như luôn dùng để join/lọc).
  - Cột hay xuất hiện trong `WHERE` (filter), `ORDER BY` (sort), hoặc tra cứu (vd `status`, `created_at`, `email`).
  - **Composite index** cho bộ lọc đi cùng nhau thường xuyên — đặt cột theo **thứ tự selectivity / cách dùng**
    (cột lọc bằng `=` đứng trước, cột range/sort đứng sau). Vd: lọc theo recipient rồi sort theo thời gian
    → `(recipient_id, created_at)` (xem 2 index trên bảng `notifications`).
- **KHÔNG đánh index** cho: cột ít dùng để tìm/lọc, cột selectivity thấp (vd boolean đứng một mình),
  bảng nhỏ/ít bản ghi, hay đánh trùng (cột đầu của composite index đã phục vụ truy vấn chỉ-cột-đó rồi → khỏi đánh thêm index đơn).
- **Nguyên tắc vàng:** chỉ thêm index khi có **truy vấn thực tế** cần nó. Mỗi index thêm vào migration nên trả lời được
  câu hỏi *"câu query nào sẽ dùng index này?"*. Không thêm cho "phòng xa".
- Đặt tên index: `idx_<bảng>_<cột1>[_<cột2>]`, vd `idx_orders_user_status`.

> Nếu `validate` báo lỗi kiểu dữ liệu (boolean/datetime) do khác biệt dialect, có thể tạm chuyển
> `spring.jpa.hibernate.ddl-auto: none` — Flyway vẫn là nguồn chân lý của schema.

---

## 6. SEEDER — viết/cập nhật khi đụng tới bảng

Có **2 loại** seed, dùng đúng chỗ:

### 6.1 Seed thiết yếu (bootstrap hệ thống) → `DataInitializer.java`

Dữ liệu bắt buộc để hệ thống chạy: roles, permissions, tài khoản admin. Để trong `config/DataInitializer`
(Java) vì **cần BCrypt hash mật khẩu** và phải idempotent. Khi thêm permission/role mới cho RBAC → cập nhật ở đây.

### 6.2 Seed dữ liệu mẫu/demo (cho FE test, môi trường dev) → `src/main/resources/db/seed/`

- Mỗi khi làm/sửa một chức năng, **viết hoặc cập nhật file seed cho bảng đó** trong `db/seed/`.
- Đặt tên: `R__seed_<bảng>.sql` (repeatable migration). Chỉ chạy khi bật profile `seed`.
- **Bắt buộc idempotent**: dùng `INSERT ... SELECT ... WHERE NOT EXISTS` (hoặc `INSERT IGNORE`), keyed trên cột ổn định —
  chạy lại nhiều lần không tạo trùng, không lỗi. Xem `R__seed_sample_notifications.sql` làm mẫu.
- Chạy seed:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
  # hoặc
  SPRING_PROFILES_ACTIVE=seed ./mvnw spring-boot:run
  ```
- **Tuyệt đối không** bật profile `seed` trên production.

---

## 7. UNIT TEST — vài case cho mỗi chức năng

- Vị trí: `src/test/java/vn/springboot/...` (mirror package của class được test).
- **Trọng tâm: test logic ở ServiceImpl** bằng Mockito (mock repository/mapper). Không cần full `@SpringBootTest` cho mỗi test.
- Mỗi chức năng viết **vài case là đủ**: 1 happy path + 1–2 case lỗi/biên (vd not found → ném `AppException`,
  điều kiện rẽ nhánh quan trọng).
- Khung mẫu:
  ```java
  @ExtendWith(MockitoExtension.class)
  class NotificationServiceImplTest {
      @Mock NotificationRepository notificationRepository;
      @Mock UserRepository userRepository;
      @InjectMocks NotificationServiceImpl service;

      @Test
      void markAsRead_whenNotFound_throwsNotFound() {
          when(notificationRepository.findByIdAndRecipient(any(), any())).thenReturn(Optional.empty());
          AppException ex = assertThrows(AppException.class, () -> service.markAsRead(1L));
          assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
      }
  }
  ```
- Test repository (JPQL/Specification phức tạp) khi cần: dùng `@DataJpaTest` (H2 đã thêm sẵn ở scope test) hoặc Testcontainers nếu cần đúng MariaDB.
- **`./mvnw test` phải xanh** mới được coi là hoàn thành.

---

## 8. DOC BÀN GIAO API CHO FE — bắt buộc cho mỗi chức năng có endpoint

- Vị trí: `docs/<FEATURE>_API.md` (tiếng Việt, theo phong cách `docs/AUTH_RBAC_API.md` & `docs/WEBSOCKET_NOTIFICATION.md`).
- Dùng **template** `docs/_TEMPLATE_FEATURE_API.md` làm khung.
- Mô tả **đủ chi tiết để FE tự call được, không cần hỏi lại**, gồm:
  - Tổng quan chức năng + yêu cầu auth (header `Authorization: Bearer <token>`, quyền cần có).
  - Mỗi endpoint: method + path, mô tả, query/path params, **request body mẫu (JSON)**, **response mẫu (JSON đầy đủ envelope `ApiResponse`)**, các mã lỗi (`code`) có thể trả về.
  - Enum/giá trị hợp lệ (vd các `NotificationType`), quy tắc phân trang, định dạng thời gian (ISO-8601/`Instant`).
- Nhắc FE: response luôn bọc trong `{ code, message, data, timestamp }`; `code = 1000` là thành công.

---

## 9. DEFINITION OF DONE (kiểm trước khi báo xong)

- [ ] Entity (nếu có) kế thừa `BaseEntity`, **không** map quan hệ JPA — khóa ngoại để cột `Long` (`*_id`).
- [ ] Có migration `V{n}__*.sql` khớp entity; quan hệ khai bằng `FOREIGN KEY`; không sửa migration cũ.
- [ ] Đã đánh **index** cho cột `*_id` và cột hay lọc/sort; **không** đánh index thừa.
- [ ] Có/đã cập nhật seeder cho bảng liên quan (`db/seed/R__seed_*.sql` và/hoặc `DataInitializer`).
- [ ] Query: đơn giản dùng JPA, phức tạp dùng JPQL; lấy bảng liên quan bằng join/`IN (:ids)`, **không** query trong vòng lặp; list có phân trang; đã soi log SQL để chắc không N+1.
- [ ] DTO có validation; Service đặt `@Transactional` đúng chỗ; lỗi qua `AppException`/`ErrorCode`.
- [ ] Controller mỏng, trả `ApiResponse`, có `@PreAuthorize` nếu cần.
- [ ] Có unit test vài case; `./mvnw test` xanh.
- [ ] Có doc bàn giao FE `docs/<FEATURE>_API.md`.
- [ ] Code đồng nhất style hiện tại (đặt tên, Javadoc, Lombok, package).

---

## 10. Lệnh hay dùng

```bash
./mvnw clean test          # chạy unit test
./mvnw spring-boot:run     # chạy app (Flyway tự migrate khi khởi động)
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed   # chạy kèm seed dữ liệu mẫu
# Swagger UI: http://localhost:8080/swagger-ui.html
```
