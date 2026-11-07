CREATE TABLE orders (
    id           UUID          PRIMARY KEY,
    user_id      UUID          NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE order_items (
    id         UUID          PRIMARY KEY,
    order_id   UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    sku        VARCHAR(64)   NOT NULL,
    quantity   INT           NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19,2) NOT NULL
);

-- "my orders, newest first": equality column first, sort column second => index-only ordering, no sort step
CREATE INDEX ix_orders_user_created ON orders (user_id, created_at DESC);
-- FK columns are NOT indexed automatically in PostgreSQL
CREATE INDEX ix_order_items_order_id ON order_items (order_id);
-- stale-order sweep touches only PENDING rows => tiny partial index
CREATE INDEX ix_orders_pending_created ON orders (created_at) WHERE status = 'PENDING';

CREATE TABLE outbox_event (
    id           UUID         PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    topic        VARCHAR(100) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_outbox_unpublished ON outbox_event (created_at) WHERE published = FALSE;
