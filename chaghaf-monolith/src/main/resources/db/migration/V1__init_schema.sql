-- ═══════════════════════════════════════════════════════════════
-- V1 — Schéma initial Chaghaf
-- Idempotent (CREATE TABLE IF NOT EXISTS) pour permettre une
-- migration douce depuis ddl-auto=update vers Flyway.
-- ═══════════════════════════════════════════════════════════════

-- ── Users ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    phone       VARCHAR(30),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active      BOOLEAN      DEFAULT TRUE,
    fcm_token   VARCHAR(500),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role  ON users (role);

-- ── Subscriptions ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subscriptions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    pack_type   VARCHAR(30)  NOT NULL,
    duration    VARCHAR(20)  NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    price       NUMERIC(10,2),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subs_user_status ON subscriptions (user_id, status);

-- ── Day Accesses ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS day_accesses (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    qr_token     VARCHAR(100) NOT NULL UNIQUE,
    access_date  DATE         NOT NULL,
    access_type  VARCHAR(20)  NOT NULL DEFAULT 'DAY_PASS',
    used         BOOLEAN      DEFAULT FALSE,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dayaccess_token ON day_accesses (qr_token);
CREATE INDEX IF NOT EXISTS idx_dayaccess_user  ON day_accesses (user_id);

-- ── Subscription Change Requests ──────────────────────────────
CREATE TABLE IF NOT EXISTS subscription_change_requests (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    current_pack        VARCHAR(30),
    requested_pack      VARCHAR(30)  NOT NULL,
    requested_duration  VARCHAR(30)  NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    admin_note          VARCHAR(500),
    admin_id            BIGINT,
    processed_at        TIMESTAMP,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subreq_status ON subscription_change_requests (status);

-- ── Reservations ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reservations (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    salle_id          VARCHAR(10)  NOT NULL,
    salle_name        VARCHAR(100) NOT NULL,
    reservation_date  DATE         NOT NULL,
    duration          VARCHAR(20)  NOT NULL,
    price             NUMERIC(10,2),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resa_user_date ON reservations (user_id, reservation_date);
CREATE INDEX IF NOT EXISTS idx_resa_salle_date ON reservations (salle_id, reservation_date);

-- ── Catalog items (boissons & snacks) ─────────────────────────
CREATE TABLE IF NOT EXISTS catalog_items (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    price           NUMERIC(10,2) NOT NULL,
    emoji           VARCHAR(10),
    image_base64    TEXT,
    image_mime_type VARCHAR(50),
    available       BOOLEAN       DEFAULT TRUE,
    stock_quantity  INTEGER       DEFAULT 0,
    category        VARCHAR(60),
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_catalog_type      ON catalog_items (type);
CREATE INDEX IF NOT EXISTS idx_catalog_available ON catalog_items (available);

-- ── Boisson consumptions (1ère gratuite par jour) ─────────────
CREATE TABLE IF NOT EXISTS boisson_consumptions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    consumed_day  DATE         NOT NULL,
    boisson_name  VARCHAR(100) NOT NULL,
    price         NUMERIC(10,2),
    was_free      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_boisson_user_day ON boisson_consumptions (user_id, consumed_day);

-- ── Posts (social) ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS posts (
    id            BIGSERIAL PRIMARY KEY,
    author_id     BIGINT       NOT NULL,
    author_name   VARCHAR(100) NOT NULL,
    author_avatar VARCHAR(5),
    author_role   VARCHAR(20),
    content       TEXT         NOT NULL,
    likes         INTEGER      DEFAULT 0,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_posts_created ON posts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_author  ON posts (author_id);

-- ── Notifications ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        TEXT,
    type        VARCHAR(30),
    link        VARCHAR(500),
    read        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications (user_id, read);
CREATE INDEX IF NOT EXISTS idx_notif_created   ON notifications (created_at DESC);

-- ── Check-ins (présence physique) ─────────────────────────────
CREATE TABLE IF NOT EXISTS check_ins (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    user_name       VARCHAR(100) NOT NULL,
    checked_in_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    checked_out_at  TIMESTAMP,
    active          BOOLEAN      DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_checkins_active     ON check_ins (active);
CREATE INDEX IF NOT EXISTS idx_checkins_user_active ON check_ins (user_id, active);
