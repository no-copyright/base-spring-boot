-- =====================================================================
-- V3 — User avatar. Stores the public URL of the uploaded avatar image.
-- =====================================================================

ALTER TABLE users
    ADD COLUMN avatar_url VARCHAR(500) NULL AFTER full_name;
