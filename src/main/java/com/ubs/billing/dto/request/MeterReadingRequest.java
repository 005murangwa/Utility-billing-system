package com.ubs.billing.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MeterReadingRequest {

    Long getMeterId();

    BigDecimal getPreviousReading();

    BigDecimal getCurrentReading();

    LocalDate getReadingDate();

    Integer getMonth();

    Integer getYear();
}
