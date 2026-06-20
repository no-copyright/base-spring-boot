-- =====================================================================
-- Repeatable seed — sample notification preference for 'admin'.
-- Demonstrates the opt-out: admin mutes PROMOTION push (keeps it in-app).
-- Idempotent: keyed on (user_id, type), runs only under the 'seed' profile.
-- =====================================================================

INSERT INTO notification_preferences (user_id, type, in_app_enabled, push_enabled, created_at, updated_at)
SELECT u.id, 'PROMOTION', TRUE, FALSE, NOW(6), NOW(6)
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM notification_preferences p
      WHERE p.user_id = u.id AND p.type = 'PROMOTION'
  );
