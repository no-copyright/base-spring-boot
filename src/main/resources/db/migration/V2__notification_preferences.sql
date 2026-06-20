-- =====================================================================
-- V2 — Per-user, per-type notification preferences (opt-out).
-- Only explicit user choices are stored; a missing row means "enabled".
-- Relationship to users is enforced by FK here (entity stores user_id as a
-- plain column, no JPA mapping — see CLAUDE.md §4).
-- =====================================================================

CREATE TABLE notification_preferences (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    type           VARCHAR(50) NOT NULL,
    in_app_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    push_enabled   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)     NULL,
    updated_at     DATETIME(6)     NULL,
    created_by     VARCHAR(100)    NULL,
    updated_by     VARCHAR(100)    NULL,
    PRIMARY KEY (id),
    -- One preference row per (user, type); also the index that serves the
    -- "load this user's prefs" / "load (user, type)" lookups on the emit path.
    CONSTRAINT uk_notif_pref_user_type UNIQUE (user_id, type),
    CONSTRAINT fk_notif_pref_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
