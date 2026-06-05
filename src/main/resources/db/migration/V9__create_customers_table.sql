CREATE TABLE customers (
    id           BIGSERIAL PRIMARY KEY,
    full_name    VARCHAR(200) NOT NULL,
    national_id  VARCHAR(20)  NOT NULL UNIQUE,
    email        VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20)  NOT NULL UNIQUE,
    address      VARCHAR(500) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_full_name ON customers (full_name);
CREATE INDEX idx_customers_national_id ON customers (national_id);
CREATE INDEX idx_customers_email ON customers (email);
CREATE INDEX idx_customers_status ON customers (status);
