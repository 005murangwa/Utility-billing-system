ALTER TABLE users
    ADD COLUMN first_login BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET first_login = FALSE WHERE first_login IS NULL;
