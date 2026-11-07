CREATE TABLE payments (
    id             UUID          PRIMARY KEY,
    order_id       UUID          NOT NULL,
    user_id        UUID          NOT NULL,
    amount         NUMERIC(19,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    reference      VARCHAR(100),
    failure_reason VARCHAR(255),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- The database, not application code, is the final guard against double charging
    CONSTRAINT ux_payments_order UNIQUE (order_id)
);
CREATE INDEX ix_payments_user ON payments (user_id);
