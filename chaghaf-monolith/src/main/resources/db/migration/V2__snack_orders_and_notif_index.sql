-- ═══════════════════════════════════════════════════════════════
-- V2 — Persistance des commandes Snacks + index notifications
-- ═══════════════════════════════════════════════════════════════

-- ── Snack orders ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS snack_orders (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    items_json   TEXT         NOT NULL,
    note         TEXT,
    total_price  NUMERIC(10,2) NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_snack_orders_user    ON snack_orders (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_snack_orders_status  ON snack_orders (status, created_at DESC);
