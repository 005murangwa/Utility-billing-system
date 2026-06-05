CREATE TABLE meters (
    id                BIGSERIAL PRIMARY KEY,
    meter_number      VARCHAR(50)  NOT NULL UNIQUE,
    meter_type        VARCHAR(20)  NOT NULL,
    installation_date DATE         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    customer_id       BIGINT       NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_meters_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT
);

CREATE INDEX idx_meters_meter_number ON meters (meter_number);
CREATE INDEX idx_meters_meter_type ON meters (meter_type);
CREATE INDEX idx_meters_status ON meters (status);
CREATE INDEX idx_meters_customer_id ON meters (customer_id);
