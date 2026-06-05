CREATE TABLE meter_readings (
    id                BIGSERIAL PRIMARY KEY,
    meter_id          BIGINT         NOT NULL,
    previous_reading  NUMERIC(12, 2) NOT NULL,
    current_reading   NUMERIC(12, 2) NOT NULL,
    reading_date      DATE           NOT NULL,
    month             INT            NOT NULL,
    year              INT            NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_meter_readings_meter FOREIGN KEY (meter_id) REFERENCES meters (id) ON DELETE RESTRICT,
    CONSTRAINT uk_meter_readings_meter_month_year UNIQUE (meter_id, month, year),
    CONSTRAINT chk_meter_readings_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_meter_readings_year CHECK (year >= 2000),
    CONSTRAINT chk_meter_readings_values CHECK (current_reading > previous_reading)
);

CREATE INDEX idx_meter_readings_meter_id ON meter_readings (meter_id);
CREATE INDEX idx_meter_readings_reading_date ON meter_readings (reading_date);
CREATE INDEX idx_meter_readings_month_year ON meter_readings (month, year);
