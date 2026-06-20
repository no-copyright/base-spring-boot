-- =====================================================================
-- Repeatable seed — sample notifications for the 'admin' user.
-- Runs only under the 'seed' profile. MUST be idempotent: re-running it
-- (Flyway re-applies repeatable scripts whenever their checksum changes)
-- must never create duplicates or fail.
--
-- Pattern: INSERT ... SELECT ... WHERE NOT EXISTS, keyed on a stable column.
-- =====================================================================

INSERT INTO notifications (recipient_id, type, title, content, is_read, created_at, updated_at)
SELECT u.id, 'SYSTEM', 'Chào mừng đến với hệ thống',
       'Đây là thông báo mẫu được seed cho môi trường dev.', FALSE, NOW(6), NOW(6)
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.recipient_id = u.id AND n.title = 'Chào mừng đến với hệ thống'
  );

INSERT INTO notifications (recipient_id, type, title, content, is_read, created_at, updated_at)
SELECT u.id, 'PROMOTION', 'Ưu đãi mẫu 50%',
       'Thông báo khuyến mãi mẫu để FE test giao diện.', FALSE, NOW(6), NOW(6)
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.recipient_id = u.id AND n.title = 'Ưu đãi mẫu 50%'
  );
