CREATE TABLE bills (
    id              BIGSERIAL PRIMARY KEY,
    bill_reference  VARCHAR(50)    NOT NULL UNIQUE,
    customer_id     BIGINT         NOT NULL,
    meter_id        BIGINT         NOT NULL,
    tariff_id       BIGINT         NOT NULL,
    meter_reading_id BIGINT        NOT NULL,
    month           INT            NOT NULL,
    year            INT            NOT NULL,
    consumption     NUMERIC(12, 2) NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL,
    vat_amount      NUMERIC(14, 2) NOT NULL,
    penalty_amount  NUMERIC(14, 2) NOT NULL,
    service_charge  NUMERIC(14, 2) NOT NULL,
    total_amount    NUMERIC(14, 2) NOT NULL,
    balance         NUMERIC(14, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    approved        BOOLEAN        NOT NULL DEFAULT FALSE,
    generated_date  DATE           NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bills_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bills_meter FOREIGN KEY (meter_id) REFERENCES meters (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bills_tariff FOREIGN KEY (tariff_id) REFERENCES tariffs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bills_meter_reading FOREIGN KEY (meter_reading_id) REFERENCES meter_readings (id) ON DELETE RESTRICT,
    CONSTRAINT uk_bills_meter_month_year UNIQUE (meter_id, month, year),
    CONSTRAINT chk_bills_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_bills_amounts CHECK (
        consumption >= 0 AND amount >= 0 AND vat_amount >= 0
        AND penalty_amount >= 0 AND service_charge >= 0
        AND total_amount >= 0 AND balance >= 0
    )
);

CREATE INDEX idx_bills_bill_reference ON bills (bill_reference);
CREATE INDEX idx_bills_customer_id ON bills (customer_id);
CREATE INDEX idx_bills_meter_id ON bills (meter_id);
CREATE INDEX idx_bills_status ON bills (status);
CREATE INDEX idx_bills_month_year ON bills (month, year);
