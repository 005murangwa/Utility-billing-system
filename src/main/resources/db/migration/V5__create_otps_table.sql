CREATE TABLE otps (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    otp_type   VARCHAR(50)  NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otps_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_otps_user_id ON otps (user_id);
CREATE INDEX idx_otps_expires_at ON otps (expires_at);
