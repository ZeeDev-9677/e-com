CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ
);

-- Functional unique index: case-insensitive uniqueness AND fast lookup for lower(email) = lower(?)
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

-- Partial index: the dormant-user sweep only ever scans active rows
CREATE INDEX ix_users_active_last_login ON users (last_login_at) WHERE active;

CREATE TABLE user_roles (
    user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);
