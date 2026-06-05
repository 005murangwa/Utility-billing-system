CREATE TABLE tariffs (
    id             BIGSERIAL PRIMARY KEY,
    meter_type     VARCHAR(20)    NOT NULL,
    rate           NUMERIC(12, 4) NOT NULL,
    service_charge NUMERIC(12, 4) NOT NULL,
    vat            NUMERIC(5, 2)  NOT NULL,
    penalty_rate   NUMERIC(12, 4) NOT NULL,
    version        INT            NOT NULL,
    effective_date DATE           NOT NULL,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tariffs_meter_type_version UNIQUE (meter_type, version),
    CONSTRAINT chk_tariffs_rate CHECK (rate >= 0),
    CONSTRAINT chk_tariffs_service_charge CHECK (service_charge >= 0),
    CONSTRAINT chk_tariffs_vat CHECK (vat >= 0),
    CONSTRAINT chk_tariffs_penalty_rate CHECK (penalty_rate >= 0)
);

CREATE INDEX idx_tariffs_meter_type ON tariffs (meter_type);
CREATE INDEX idx_tariffs_active ON tariffs (active);
CREATE INDEX idx_tariffs_effective_date ON tariffs (effective_date);
