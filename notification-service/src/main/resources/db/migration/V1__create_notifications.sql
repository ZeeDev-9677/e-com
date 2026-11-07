CREATE TABLE notifications (
    id         UUID         PRIMARY KEY,
    dedupe_key VARCHAR(150) NOT NULL,
    user_id    UUID         NOT NULL,
    recipient  VARCHAR(255) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    sent_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_notifications_dedupe UNIQUE (dedupe_key)
);
CREATE INDEX ix_notifications_user_sent ON notifications (user_id, sent_at DESC);
