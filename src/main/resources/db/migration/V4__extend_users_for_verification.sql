ALTER TABLE users
    ADD COLUMN full_name      VARCHAR(200),
    ADD COLUMN phone_number   VARCHAR(20),
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX idx_users_phone_number ON users (phone_number) WHERE phone_number IS NOT NULL;

UPDATE users SET email_verified = TRUE WHERE email_verified = FALSE;
