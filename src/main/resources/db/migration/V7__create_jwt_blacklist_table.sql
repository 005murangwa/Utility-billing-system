CREATE TABLE jwt_blacklist (
    id              BIGSERIAL PRIMARY KEY,
    jti             VARCHAR(64)  NOT NULL UNIQUE,
    expires_at      TIMESTAMP    NOT NULL,
    blacklisted_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jwt_blacklist_expires_at ON jwt_blacklist (expires_at);
