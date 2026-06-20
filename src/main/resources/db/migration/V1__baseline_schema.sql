-- =====================================================================
-- V1 — Baseline schema (single source of truth for a fresh database).
-- From here on, every schema change MUST be a new V{n}__*.sql migration.
--
-- Conventions:
--   * InnoDB + utf8mb4
--   * Every table carries the BaseEntity audit columns
--     (created_at, updated_at, created_by, updated_by)
--   * Instant  -> datetime(6)   |  boolean -> boolean (tinyint(1))
--   * Foreign keys are declared here (entities store *_id as plain columns,
--     no JPA relationship mapping — see CLAUDE.md §4)
--   * Join tables (M2M) own a composite PK and FKs to both sides
-- =====================================================================

-- ---------------------------------------------------------------------
-- permissions
-- ---------------------------------------------------------------------
CREATE TABLE permissions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255)     NULL,
    created_at  DATETIME(6)      NULL,
    updated_at  DATETIME(6)      NULL,
    created_by  VARCHAR(100)     NULL,
    updated_by  VARCHAR(100)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permissions_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- roles
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(255)    NULL,
    created_at  DATETIME(6)     NULL,
    updated_at  DATETIME(6)     NULL,
    created_by  VARCHAR(100)    NULL,
    updated_by  VARCHAR(100)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- roles_permissions (M2M: roles <-> permissions)
-- ---------------------------------------------------------------------
CREATE TABLE roles_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles (id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100)     NULL,
    avatar_url VARCHAR(500)     NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)      NULL,
    updated_at DATETIME(6)      NULL,
    created_by VARCHAR(100)     NULL,
    updated_by VARCHAR(100)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- users_roles (M2M: users <-> roles)
-- ---------------------------------------------------------------------
CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- refresh_tokens
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    token      VARCHAR(100) NOT NULL,
    user_id    BIGINT       NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)      NULL,
    updated_at DATETIME(6)      NULL,
    created_by VARCHAR(100)     NULL,
    updated_by VARCHAR(100)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT       NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    title        VARCHAR(200) NOT NULL,
    content      VARCHAR(1000)    NULL,
    icon         VARCHAR(100)     NULL,
    image_url    VARCHAR(500)     NULL,
    link_url     VARCHAR(500)     NULL,
    metadata     TEXT             NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at      DATETIME(6)      NULL,
    created_at   DATETIME(6)      NULL,
    updated_at   DATETIME(6)      NULL,
    created_by   VARCHAR(100)     NULL,
    updated_by   VARCHAR(100)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_notification_recipient_read    (recipient_id, is_read),
    INDEX idx_notification_recipient_created (recipient_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- device_tokens (push-notification groundwork)
-- ---------------------------------------------------------------------
CREATE TABLE device_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(512) NOT NULL,
    platform   VARCHAR(20)  NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)      NULL,
    updated_at DATETIME(6)      NULL,
    created_by VARCHAR(100)     NULL,
    updated_by VARCHAR(100)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_device_tokens_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- notification_preferences (per-user, per-type opt-out)
-- Missing row = enabled. Unique (user_id, type) doubles as the lookup index.
-- ---------------------------------------------------------------------
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
    CONSTRAINT uk_notif_pref_user_type UNIQUE (user_id, type),
    CONSTRAINT fk_notif_pref_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- files (upload registry: metadata for audit / ownership / cleanup)
-- Bytes live in the storage backend; this table tracks them.
-- ---------------------------------------------------------------------
CREATE TABLE files (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    storage_key       VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255)     NULL,
    content_type      VARCHAR(100)     NULL,
    size_bytes        BIGINT       NOT NULL DEFAULT 0,
    owner_id          BIGINT           NULL,
    created_at        DATETIME(6)      NULL,
    updated_at        DATETIME(6)      NULL,
    created_by        VARCHAR(100)     NULL,
    updated_by        VARCHAR(100)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_files_storage_key UNIQUE (storage_key),
    INDEX idx_files_owner (owner_id),
    CONSTRAINT fk_files_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
