CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    bill_id        BIGINT         NOT NULL,
    amount_paid    NUMERIC(14, 2) NOT NULL,
    payment_method VARCHAR(20)    NOT NULL,
    payment_date   DATE           NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_bill FOREIGN KEY (bill_id) REFERENCES bills (id) ON DELETE RESTRICT,
    CONSTRAINT chk_payments_amount CHECK (amount_paid > 0)
);

CREATE INDEX idx_payments_bill_id ON payments (bill_id);
CREATE INDEX idx_payments_payment_date ON payments (payment_date);
CREATE INDEX idx_payments_payment_method ON payments (payment_method);
